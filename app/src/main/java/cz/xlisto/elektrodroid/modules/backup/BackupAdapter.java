package cz.xlisto.elektrodroid.modules.backup;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;
import cz.xlisto.elektrodroid.utils.NetworkUtil;


/**
 * Adaptér pro zobrazení a obsluhu souborů záloh.
 *
 * <p>Podporuje dva režimy dat:</p>
 * <ul>
 *   <li>místní soubory ({@link DocumentFile})</li>
 *   <li>soubory Google Drive ({@link File})</li>
 * </ul>
 *
 * <p>V lokálním režimu zajišťuje rozbalovací akce nad položkou, obnovu dat,
 * mazání a také vícenásobný výběr se synchronizací stavu do fragmentu přes
 * {@link SelectionChangeListener}. V režimu Drive zajišťuje akce nad složkami
 * a soubory včetně mazání a obnovy.</p>
 */
public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.MyViewHolder> {

    private static final String TAG = "BackupAdapter";
    public static final String FLAG_DIALOG_FRAGMENT_BACKUP = "backupDialogFragmentBackup";
    public static final String FLAG_DIALOG_FRAGMENT_DELETE = "backupDialogFragmentDelete";
    // FLAG_DIALOG_RENAME_FOLDER je skryt, protože DRIVE_APPDATA neumožňuje práci se složkami
    private List<DocumentFile> documentFiles;
    private List<File> files;
    private final Context context;
    private final RecyclerView recyclerView;
    private static int showButtons = -1;
    private int selectedPosition;
    private static DocumentFile selectedDocumentFile;
    private final boolean isGoogleDrive;
    private GoogleDriveService googleDriveService;
    private String selectedFileId;
    private final ShPGoogleDrive shPGoogleDrive;
    private final Handler handlerRecovery;
    private final Handler handlerShowProgressBar;
    private final SelectionChangeListener selectionChangeListener;
    private final DeleteStateListener deleteStateListener;
    private final SingleUploadRequestListener singleUploadRequestListener;
    private final SaveGoogleDriveFileListener saveGoogleDriveFileListener;
    private final Set<Integer> selectedPositions = new TreeSet<>();
    private boolean multiSelectMode = false;
    //handler smazání souboru z GoogleDrive
    private final Handler handlerReloadFiles = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            GoogleDriveService.ResultAction result = (GoogleDriveService.ResultAction) msg.obj;
            if (result.result == GoogleDriveService.ResultAction.RESULT_OK) {
                files.remove(selectedPosition);
                notifyItemRemoved(selectedPosition);
                notifyItemRangeChanged(selectedPosition, files.size());
                selectedDocumentFile = null;
                showButtons = -1;
                Snackbar.make(recyclerView, context.getString(R.string.deleted_file), Snackbar.LENGTH_SHORT).show();
            } else if (result.result == GoogleDriveService.ResultAction.RESULT_ERROR)
                Snackbar.make(recyclerView, result.message, Snackbar.LENGTH_SHORT).show();
            else
                Snackbar.make(recyclerView, context.getString(R.string.not_deleted_file), Snackbar.LENGTH_SHORT).show();
        }
    };


    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvTyp;
        ImageView iconFile;
        ImageView iconMoreFolderAction;
        RelativeLayout rl;
        LinearLayout ln;
        Button btnRestore;
        Button btnDelete;
        Button btnUpload;
        android.widget.CheckBox cbSelect;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

    /**
     * Callback pro předání stavu vícenásobného výběru do nadřazené vrstvy (fragmentu).
     */
    public interface SelectionChangeListener {
        /**
         * Volá se při každé změně výběru.
         *
         * @param selectedCount     počet aktuálně vybraných položek
         * @param isMultiSelectMode {@code true}, pokud je aktivní režim vícenásobného výběru
         */
        void onSelectionChanged(int selectedCount, boolean isMultiSelectMode);
    }


    /**
     * Callback pro mazání vybraných záloh.
     */
    public interface DeleteStateListener {

        void onDeleteStarted(int totalCount);

        void onDeleteProgress(int processedCount, int totalCount);

        void onDeleteFinished(boolean success, int deletedCount, int totalCount);

        void onDeleteFailed(String message);
    }

    /**
     * Callback pro požadavek na upload jedné lokální zálohy z tlačítka položky.
     */
    public interface SingleUploadRequestListener {
        void onSingleUploadRequested(@NonNull DocumentFile documentFile);
    }


    /**
     * Callback pro požadavek na uložení souboru z Google Drive do lokálního úložiště.
     */
    public interface SaveGoogleDriveFileListener {
        void onSaveGoogleDriveFileRequested(@NonNull File file);
    }


    /**
     * Konstruktor pro vytvoření instance BackupAdapter pro místní soubory.
     *
     * @param context         kontext aplikace
     * @param documentFiles   seznam dokumentových souborů
     * @param recyclerView    RecyclerView pro zobrazení souborů
     * @param handlerRecovery handler pro obnovu souborů
     */
    public BackupAdapter(Context context, List<DocumentFile> documentFiles, RecyclerView recyclerView, Handler handlerRecovery, Handler handlerShowProgressBar, SelectionChangeListener selectionChangeListener, SingleUploadRequestListener singleUploadRequestListener) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
        this.handlerRecovery = handlerRecovery;
        this.handlerShowProgressBar = handlerShowProgressBar;
        this.selectionChangeListener = selectionChangeListener;
        this.deleteStateListener = null;
        this.singleUploadRequestListener = singleUploadRequestListener;
        this.saveGoogleDriveFileListener = null;
        this.isGoogleDrive = false;
        shPGoogleDrive = new ShPGoogleDrive(context);
        showButtons = -1;
    }


    /**
     * Konstruktor pro vytvoření instance BackupAdapter pro soubory na Google Drive
     * s podporou vícenásobného výběru a callbacků mazání.
     *
     * @param context            kontext aplikace
     * @param files              seznam souborů na Google Drive
     * @param recyclerView       RecyclerView pro zobrazení souborů
     * @param handlerRecovery    handler pro obnovu souborů
     * @param googleDriveService služba pro práci s Google Drive
     * @param selectionChangeListener callback změny výběru
     * @param deleteStateListener callback průběhu mazání
     */
    public BackupAdapter(Context context, List<File> files, RecyclerView recyclerView, Handler handlerRecovery, GoogleDriveService googleDriveService, SelectionChangeListener selectionChangeListener, DeleteStateListener deleteStateListener, SaveGoogleDriveFileListener saveGoogleDriveFileListener) {
        this.files = files;
        this.context = context;
        this.recyclerView = recyclerView;
        this.handlerRecovery = handlerRecovery;
        this.handlerShowProgressBar = null;
        this.selectionChangeListener = selectionChangeListener;
        this.deleteStateListener = deleteStateListener;
        this.singleUploadRequestListener = null;
        this.saveGoogleDriveFileListener = saveGoogleDriveFileListener;
        this.isGoogleDrive = true;
        this.googleDriveService = googleDriveService;
        shPGoogleDrive = new ShPGoogleDrive(context);
        showButtons = -1;
    }


    /**
     * Vytvoří a napojí {@link MyViewHolder} pro položku seznamu záloh.
     *
     * @param parent   rodičovský kontejner RecyclerView
     * @param viewType typ položky
     * @return inicializovaný view holder
     */
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.tvName = v.findViewById(R.id.tvNameBackup);
        vh.tvTyp = v.findViewById(R.id.tvTypeBackup);
        vh.iconFile = v.findViewById(R.id.imgIconFile);
        vh.iconMoreFolderAction = v.findViewById(R.id.ivMoreOptions);
        vh.rl = v.findViewById(R.id.rlBackupItem);
        vh.ln = v.findViewById(R.id.lnButtonsBackup);
        vh.btnRestore = v.findViewById(R.id.btnRestoreBackup);
        vh.btnDelete = v.findViewById(R.id.btnDeleteBackup);
        vh.btnUpload = v.findViewById(R.id.btnUploadBackup);
        vh.cbSelect = v.findViewById(R.id.cbSelectBackup);
        return vh;
    }


    /**
     * Naváže data na položku seznamu a nastaví obsluhu akcí.
     *
     * <p>V lokálním režimu řeší zobrazení checkboxu pro multi-výběr,
     * rozbalovací tlačítka a akce obnovy/smazání. V režimu Drive řeší
     * navigaci do složek a akce nad soubory/složkami.</p>
     *
     * @param holder   holder položky
     * @param position pozice položky v adapteru
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DocumentFile documentFile;
        File file;

        if (isGoogleDrive) {
            documentFile = null;
            file = files.get(position);
            holder.tvName.setText(IconFileHelper.getName(file.getName()));
            holder.iconFile.setImageResource(IconFileHelper.getIcon(file.getName(), file.getMimeType()));
            holder.tvTyp.setText(IconFileHelper.getType(file.getName(), context));
            holder.btnUpload.setVisibility(View.VISIBLE);
            holder.btnUpload.setText(R.string.save_to_local_storage);
            holder.iconMoreFolderAction.setVisibility(View.GONE);
        } else {
            file = null;
            documentFile = documentFiles.get(position);
            holder.tvName.setText(IconFileHelper.getName(documentFile.getName()));
            holder.iconFile.setImageResource(IconFileHelper.getIcon(documentFile.getName()));
            holder.tvTyp.setText(IconFileHelper.getType(documentFile.getName(), context));
            holder.btnUpload.setText(R.string.google_drive);
            boolean isUserLoggedIn = !shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, "").isEmpty();
            if (isUserLoggedIn && NetworkUtil.isInternetAvailable(context))
                holder.btnUpload.setVisibility(View.VISIBLE);
            else
                holder.btnUpload.setVisibility(View.GONE);
            holder.iconMoreFolderAction.setVisibility(View.GONE);
        }

        holder.cbSelect.setVisibility(multiSelectMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedPositions.contains(position));


        showButtons(holder, position);

        holder.rl.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION)
                return;

            if (multiSelectMode) {
                toggleSelection(adapterPosition);
                if (multiSelectMode) {
                    notifyItemChanged(adapterPosition);
                } else {
                    selectedPosition = -1;
                    showButtons = -1;
                    selectedDocumentFile = null;
                    selectedFileId = null;
                    notifyAllItemsChanged();
                }
                return;
            }

            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsBackup).setVisibility(View.GONE);
            }

            // Check if the item is a folder and display its contents
            if (isGoogleDrive) {

                selectedFileId = file.getId();
                toggleButtons(adapterPosition, holder);
            } else {
                toggleButtons(adapterPosition, holder);

            }
        });

        holder.rl.setOnLongClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION)
                return false;

            if (!multiSelectMode) {
                multiSelectMode = true;
                showButtons = -1;
                selectedPosition = -1;
                selectedDocumentFile = null;
                selectedFileId = null;
            }
            toggleSelection(adapterPosition);
            notifyAllItemsChanged();
            return true;
        });

        holder.btnRestore.setOnClickListener(v -> {
            selectedDocumentFile = documentFile;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getString(R.string.recover_backup), FLAG_DIALOG_FRAGMENT_BACKUP);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.btnUpload.setOnClickListener(v -> {
            if (isGoogleDrive) {
                if (saveGoogleDriveFileListener != null)
                    saveGoogleDriveFileListener.onSaveGoogleDriveFileRequested(file);
                return;
            }

            if (singleUploadRequestListener != null) {
                singleUploadRequestListener.onSingleUploadRequested(documentFile);
                return;
            }

            Message msg = new Message();
            msg.obj = true;
            if (handlerShowProgressBar != null)
                handlerShowProgressBar.sendMessage(msg);
            selectedDocumentFile = documentFile;
            ShPGoogleDrive shPGoogleDrive = new ShPGoogleDrive(context);
            googleDriveService = new GoogleDriveService(context,
                    shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, ""));
            googleDriveService.setOnDriveServiceListener(this::uploadFile);
        });

        holder.btnDelete.setOnClickListener(v -> {
            selectedDocumentFile = documentFile;
            selectedPosition = holder.getBindingAdapterPosition();
            if (selectedPosition == RecyclerView.NO_POSITION)
                return;

            if (isGoogleDrive)
                selectedFileId = file.getId();

            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getString(R.string.delete_backup), FLAG_DIALOG_FRAGMENT_DELETE);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.iconMoreFolderAction.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_more_folder_options, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (file == null)
                    return false;
                selectedFileId = file.getId();
                selectedPosition = holder.getBindingAdapterPosition();
                if (selectedPosition == RecyclerView.NO_POSITION)
                    return false;

                // rename_folder je skryta v menu, protože DRIVE_APPDATA neumožňuje práci se složkami
                if (R.id.delete_folder == item.getItemId()) {
                    YesNoDialogFragment.newInstance(context.getString(R.string.delete_folder), FLAG_DIALOG_FRAGMENT_DELETE).show(((FragmentActivity) context).getSupportFragmentManager(), YesNoDialogFragment.TAG);
                    return true;
                }

                return false;
            });
            popupMenu.show();
        });

    }


    /**
     * Nahraje vybraný dokumentový soubor na Google Drive.
     * <p>
     * Tato metoda získá výchozí ID složky z preferencí a zavolá metodu `uploadFile`
     * služby `GoogleDriveService` pro nahrání vybraného dokumentového souboru na Google Drive.
     * </p>
     */
    private void uploadFile() {
        boolean b = googleDriveService.uploadFile(selectedDocumentFile);
        Message msg = new Message();
        msg.obj = false;
        if (handlerShowProgressBar != null)
            handlerShowProgressBar.sendMessage(msg);
        if (b)
            Snackbar.make(recyclerView, context.getString(R.string.uploaded_file), Snackbar.LENGTH_SHORT).show();
        else
            Snackbar.make(recyclerView, context.getString(R.string.not_uploaded_file), Snackbar.LENGTH_SHORT).show();
    }



    /**
     * Vrátí seznam aktuálně vybraných lokálních souborů záloh.
     *
     * @return seznam {@link DocumentFile} odpovídajících označeným položkám
     */
    public List<DocumentFile> getSelectedDocumentFiles() {
        List<DocumentFile> selectedFiles = new ArrayList<>();
        if (documentFiles == null)
            return selectedFiles;

        for (Integer position : new TreeSet<>(selectedPositions)) {
            if (position < 0 || position >= documentFiles.size())
                continue;
            DocumentFile documentFile = documentFiles.get(position);
            if (documentFile != null)
                selectedFiles.add(documentFile);
        }
        return selectedFiles;
    }


    /**
     * Vrátí seznam aktuálně vybraných souborů z Google Drive.
     *
     * @return seznam {@link File} odpovídajících označeným položkám
     */
    public List<File> getSelectedGoogleDriveFiles() {
        List<File> selectedFiles = new ArrayList<>();
        if (!isGoogleDrive || files == null)
            return selectedFiles;

        for (Integer position : new TreeSet<>(selectedPositions)) {
            if (position < 0 || position >= files.size())
                continue;
            File file = files.get(position);
            if (file != null)
                selectedFiles.add(file);
        }
        return selectedFiles;
    }


    /**
     * Přepíná viditelnost tlačítek pro obnovu a smazání zálohy.
     *
     * @param position pozice vybrané položky
     * @param holder   view holder
     */
    private void toggleButtons(int position, MyViewHolder holder) {
        if (multiSelectMode)
            return;

        if (showButtons == position) {
            showButtons = -1;
        } else {
            showButtons = position;
        }
        selectedPosition = position;
        showButtons(holder, position);
    }


    /**
     * Stáhne a obnoví data ze záložního souboru na Google Drive.
     */
    public void downloadAndRecoveryFile() {
        RecoverDataFromBackupFile.setDriveService(googleDriveService.getDrive());
        RecoverDataFromBackupFile.recoverDatabaseFromZipGoogleDrive(context, selectedFileId, "fileName.zip", result -> {
            Message msg = new Message();
            msg.obj = result;
            handlerRecovery.sendMessage(msg);
        });
    }


    /**
     * Vrátí počet položek v seznamu.
     *
     * @return počet položek v seznamu
     */
    @Override
    public int getItemCount() {
        if (isGoogleDrive)
            return files != null ? files.size() : 0;
        else
            return documentFiles != null ? documentFiles.size() : 0;
    }


    /**
     * Smaže vybraný záložní soubor (případně složku na GoogleDrive)
     */
    public void deleteFile() {
        if (isGoogleDrive) {
            Message msg = new Message();

            new Thread(() -> {
                msg.obj = googleDriveService.deleteFile(selectedFileId);
                handlerReloadFiles.sendMessage(msg);
            }).start();
        } else {
            selectedDocumentFile.delete();
            documentFiles.remove(selectedPosition);
            notifyItemRemoved(selectedPosition);
            notifyItemRangeChanged(selectedPosition, documentFiles.size());
            selectedDocumentFile = null;
            showButtons = -1;
        }

    }


    public void deleteSelectedFiles() {
        if (selectedPositions.isEmpty())
            return;

        if (isGoogleDrive) {
            if (googleDriveService == null) {
                dispatchDeleteFailed(context.getString(R.string.not_deleted_file));
                return;
            }

            if (!NetworkUtil.isInternetAvailable(context)) {
                dispatchDeleteFailed(context.getString(R.string.internet_is_not_available));
                return;
            }

            List<File> selectedFiles = new ArrayList<>();
            for (Integer index : new TreeSet<>(selectedPositions)) {
                if (index < 0 || index >= files.size())
                    continue;
                File file = files.get(index);
                if (file != null)
                    selectedFiles.add(file);
            }

            if (selectedFiles.isEmpty())
                return;

            dispatchDeleteStarted(selectedFiles.size());
            new Thread(() -> {
                int deletedCount = 0;
                int totalCount = selectedFiles.size();
                for (int i = 0; i < selectedFiles.size(); i++) {
                    File file = selectedFiles.get(i);
                    GoogleDriveService.ResultAction result = googleDriveService.deleteFile(file.getId());
                    if (result != null && result.result == GoogleDriveService.ResultAction.RESULT_OK)
                        deletedCount++;
                    dispatchDeleteProgress(i + 1, totalCount);
                }

                final int finalDeletedCount = deletedCount;
                final int finalTotalCount = totalCount;
                runOnMainThread(() -> {
                    cancelMultiSelect();
                    dispatchDeleteFinished(finalDeletedCount == finalTotalCount, finalDeletedCount, finalTotalCount);
                });
            }).start();
            return;
        }

        int deletedCount = 0;
        for (Integer index : new TreeSet<>(selectedPositions).descendingSet()) {
            if (index < 0 || index >= documentFiles.size())
                continue;
            DocumentFile file = documentFiles.get(index);
            if (file != null && file.delete()) {
                documentFiles.remove((int) index);
                notifyItemRemoved(index);
                deletedCount++;
            }
        }

        clearSelection();
        showButtons = -1;
        selectedDocumentFile = null;
        notifyAllItemsChanged();

        if (deletedCount > 0)
            Snackbar.make(recyclerView, context.getString(R.string.deleted_file), Snackbar.LENGTH_SHORT).show();
        else
            Snackbar.make(recyclerView, context.getString(R.string.not_deleted_file), Snackbar.LENGTH_SHORT).show();
    }


    /**
     * Vrátí počet aktuálně vybraných položek v lokálním multi-výběru.
     *
     * @return počet vybraných položek
     */
    public int getSelectedItemCount() {
        return selectedPositions.size();
    }


    /**
     * Vrátí URI vybraných lokálních záloh pro možnost obnovení stavu po rotaci.
     *
     * @return seznam URI vybraných položek
     */
    public ArrayList<String> getSelectedDocumentFileUris() {
        ArrayList<String> selectedUris = new ArrayList<>();
        if (documentFiles == null)
            return selectedUris;

        for (Integer position : selectedPositions) {
            if (position < 0 || position >= documentFiles.size())
                continue;
            DocumentFile documentFile = documentFiles.get(position);
            if (documentFile != null)
                selectedUris.add(documentFile.getUri().toString());
        }
        return selectedUris;
    }



    /**
     * Vymaže výběr položek a vypne režim vícenásobného výběru.
     */
    public void clearSelection() {
        selectedPositions.clear();
        multiSelectMode = false;
        notifySelectionChanged();
    }


    /**
     * Zruší multi-výběr a zároveň resetuje interní stav výběru/rozbalení položky.
     */
    public void cancelMultiSelect() {
        clearSelection();
        selectedPosition = -1;
        showButtons = -1;
        selectedDocumentFile = null;
        notifyAllItemsChanged();
    }


    /**
     * Obnoví výběr položek podle jejich URI (typicky po obnově stavu fragmentu).
     *
     * @param selectedUris URI položek, které mají být označeny
     * @return počet úspěšně obnovených vybraných položek
     */
    public int restoreSelectionByUris(List<String> selectedUris) {
        if (isGoogleDrive || documentFiles == null) {
            cancelMultiSelect();
            return 0;
        }

        selectedPositions.clear();
        if (selectedUris == null || selectedUris.isEmpty()) {
            cancelMultiSelect();
            return 0;
        }

        Set<String> uriSet = new HashSet<>(selectedUris);
        for (int i = 0; i < documentFiles.size(); i++) {
            DocumentFile documentFile = documentFiles.get(i);
            if (documentFile != null && uriSet.contains(documentFile.getUri().toString()))
                selectedPositions.add(i);
        }

        multiSelectMode = !selectedPositions.isEmpty();
        selectedPosition = -1;
        showButtons = -1;
        selectedDocumentFile = null;
        notifySelectionChanged();
        notifyAllItemsChanged();
        return selectedPositions.size();
    }


    /**
     * Obnoví data ze záložního vybraného souboru zip
     */
    public void recoverDatabaseFromZip() {
        boolean b = RecoverDataFromBackupFile.recoverDatabaseFromZip(context, selectedDocumentFile);
        Message msg = new Message();
        msg.obj = b;
        handlerRecovery.sendMessage(msg);
    }


    /**
     * Zobrazí tlačítka pro obnovu a smazání zálohy
     *
     * @param holder   view holder
     * @param position pozice
     */
    private void showButtons(MyViewHolder holder, int position) {
        if (multiSelectMode) {
            holder.ln.setVisibility(View.GONE);
            return;
        }

        if (showButtons == position)
            holder.ln.setVisibility(View.VISIBLE);
        else
            holder.ln.setVisibility(View.GONE);
    }


    /**
     * Posune index vybrané položky a jednu pozici a nastaví aktuálně vybraný soubor
     */
    public void moveToPosition() {
        if (selectedPosition >= 0 && showButtons >= 0) {
            selectedPosition = selectedPosition + 1;
            selectedDocumentFile = documentFiles.get(selectedPosition);
            showButtons = selectedPosition;
        }
    }


    /**
     * Aktualizuje data v adapteru a informuje RecyclerView o změně rozsahu položek.
     * Tato metoda by měla být volána, když se změní data, která adapter zobrazuje.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void notifyDataChanged() {
        notifyDataSetChanged();
    }


    /**
     * Vymaže data v adapteru a informuje RecyclerView o změně.
     * Tato metoda vymaže seznam souborů a aktualizuje adapter.
     */
    public void clearData() {
        if (documentFiles != null)
            documentFiles.clear();
        if (files != null)
            files.clear();
        clearSelection();
        notifyDataChanged();
    }


    /**
     * Přepne výběr konkrétní položky v režimu vícenásobného výběru.
     *
     * @param position pozice položky
     */
    private void toggleSelection(int position) {
        if (selectedPositions.contains(position))
            selectedPositions.remove(position);
        else
            selectedPositions.add(position);

        if (selectedPositions.isEmpty()) {
            multiSelectMode = false;
        }
        notifySelectionChanged();
    }


    /**
     * Odešle callback o změně výběru do posluchače, pokud je registrován.
     */
    private void notifySelectionChanged() {
        if (selectionChangeListener != null)
            selectionChangeListener.onSelectionChanged(selectedPositions.size(), multiSelectMode);
    }



    private void dispatchDeleteStarted(int totalCount) {
        if (deleteStateListener == null)
            return;

        runOnMainThread(() -> deleteStateListener.onDeleteStarted(totalCount));
    }


    private void dispatchDeleteProgress(int processedCount, int totalCount) {
        if (deleteStateListener == null)
            return;

        runOnMainThread(() -> deleteStateListener.onDeleteProgress(processedCount, totalCount));
    }


    private void dispatchDeleteFinished(boolean success, int deletedCount, int totalCount) {
        if (deleteStateListener == null)
            return;

        runOnMainThread(() -> deleteStateListener.onDeleteFinished(success, deletedCount, totalCount));
    }


    private void dispatchDeleteFailed(String message) {
        if (deleteStateListener == null)
            return;

        runOnMainThread(() -> deleteStateListener.onDeleteFailed(message));
    }


    private void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }


    private void notifyAllItemsChanged() {
        int itemCount = getItemCount();
        if (itemCount > 0)
            notifyItemRangeChanged(0, itemCount);
    }

}
