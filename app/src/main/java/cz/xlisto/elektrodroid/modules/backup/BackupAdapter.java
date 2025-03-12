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

import java.util.List;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.FolderDialog;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;
import cz.xlisto.elektrodroid.utils.NetworkUtil;


/**
 * Adaptér zobrazení souboru záloh
 * Xlisto 24.04.2023 20:06
 */
public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.MyViewHolder> {

    private static final String TAG = "BackupAdapter";
    public static final String FLAG_DIALOG_FRAGMENT_BACKUP = "backupDialogFragmentBackup";
    public static final String FLAG_DIALOG_FRAGMENT_DELETE = "backupDialogFragmentDelete";
    public static final String FLAG_DIALOG_RENAME_FOLDER = "RenameFolderDialog";
    private List<DocumentFile> documentFiles;
    private List<File> files;
    private final Context context;
    private final RecyclerView recyclerView;
    private static int showButtons = -1;
    private int selectedPosition;
    private static DocumentFile selectedDocumentFile;
    private boolean isGoogleDrive;
    private GoogleDriveService googleDriveService;
    private String selectedFileId;
    private final ShPGoogleDrive shPGoogleDrive;
    private final Handler handlerOpenFolder;
    private final Handler handlerRecovery;
    private final Handler handlerShowProgressBar;
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


    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvTyp;
        ImageView iconFile;
        ImageView iconMoreFolderAction;
        RelativeLayout rl;
        LinearLayout ln;
        Button btnRestore;
        Button btnDelete;
        Button btnUpload;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    /**
     * Konstruktor pro vytvoření instance BackupAdapter pro místní soubory.
     *
     * @param context         kontext aplikace
     * @param documentFiles   seznam dokumentových souborů
     * @param recyclerView    RecyclerView pro zobrazení souborů
     * @param handlerRecovery handler pro obnovu souborů
     */
    public BackupAdapter(Context context, List<DocumentFile> documentFiles, RecyclerView recyclerView, Handler handlerRecovery, Handler handlerShowProgressBar) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
        this.handlerRecovery = handlerRecovery;
        this.handlerShowProgressBar = handlerShowProgressBar;
        this.handlerOpenFolder = null;
        this.isGoogleDrive = false;
        shPGoogleDrive = new ShPGoogleDrive(context);
        showButtons = -1;
    }


    /**
     * Konstruktor pro vytvoření instance BackupAdapter pro soubory na Google Drive.
     *
     * @param context            kontext aplikace
     * @param files              seznam souborů na Google Drive
     * @param recyclerView       RecyclerView pro zobrazení souborů
     * @param handlerRecovery    handler pro obnovu souborů
     * @param handlerOpenFolder  handler pro otevření složky
     * @param googleDriveService služba pro práci s Google Drive
     */
    public BackupAdapter(Context context, List<File> files, RecyclerView recyclerView, Handler handlerRecovery, Handler handlerOpenFolder, GoogleDriveService googleDriveService) {
        this.files = files;
        this.context = context;
        this.recyclerView = recyclerView;
        this.handlerOpenFolder = handlerOpenFolder;
        this.handlerRecovery = handlerRecovery;
        this.handlerShowProgressBar = null;
        this.isGoogleDrive = true;
        this.googleDriveService = googleDriveService;
        shPGoogleDrive = new ShPGoogleDrive(context);
        showButtons = -1;
    }


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
        return vh;
    }


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
            holder.btnUpload.setVisibility(View.GONE);
            if ("application/vnd.google-apps.folder".equals(file.getMimeType()) && !file.getName().equals("...")) {
                holder.iconMoreFolderAction.setVisibility(View.VISIBLE);
            } else {
                holder.iconMoreFolderAction.setVisibility(View.GONE);
            }
        } else {
            file = null;
            documentFile = documentFiles.get(position);
            holder.tvName.setText(IconFileHelper.getName(documentFile.getName()));
            holder.iconFile.setImageResource(IconFileHelper.getIcon(documentFile.getName()));
            holder.tvTyp.setText(IconFileHelper.getType(documentFile.getName(), context));
            if (NetworkUtil.isInternetAvailable(context))
                holder.btnUpload.setVisibility(View.VISIBLE);
            else
                holder.btnUpload.setVisibility(View.GONE);
            holder.iconMoreFolderAction.setVisibility(View.GONE);
        }


        //TODO: skrytí Drive
        isGoogleDrive = false;
        holder.btnUpload.setVisibility(View.GONE);

        showButtons(holder, position);

        holder.rl.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsBackup).setVisibility(View.GONE);
            }

            // Check if the item is a folder and display its contents
            if (isGoogleDrive) {
                selectedFileId = file.getId();
                if ("application/vnd.google-apps.folder".equals(file.getMimeType())) {
                    openFolder(file.getId());
                } else {
                    toggleButtons(position, holder);
                }
            } else {
                toggleButtons(position, holder);

            }
        });

        holder.btnRestore.setOnClickListener(v -> {
            selectedDocumentFile = documentFile;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getString(R.string.recover_backup), FLAG_DIALOG_FRAGMENT_BACKUP);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.btnUpload.setOnClickListener(v -> {
            Message msg = new Message();
            msg.obj = true;
            if (handlerShowProgressBar != null)
                handlerShowProgressBar.sendMessage(msg);
            selectedDocumentFile = documentFile;
            ShPGoogleDrive shPGoogleDrive = new ShPGoogleDrive(context);
            googleDriveService = new GoogleDriveService(context,
                    shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, ""),
                    shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, ""));
            googleDriveService.setOnDriveServiceListener(this::uploadFile);
        });

        holder.btnDelete.setOnClickListener(v -> {
            selectedDocumentFile = documentFile;
            selectedPosition = position;
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
                selectedPosition = position;

                if (R.id.rename_folder == item.getItemId()) {
                    FolderDialog.newInstance(FLAG_DIALOG_RENAME_FOLDER, file.getName()).show(((FragmentActivity) context).getSupportFragmentManager(), FolderDialog.TAG);
                    return true;
                } else if (R.id.delete_folder == item.getItemId()) {
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
        boolean b = googleDriveService.uploadFile(selectedDocumentFile, shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, ""));
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
     * Přepíná viditelnost tlačítek pro obnovu a smazání zálohy.
     *
     * @param position pozice vybrané položky
     * @param holder   view holder
     */
    private void toggleButtons(int position, MyViewHolder holder) {
        if (showButtons == position) {
            showButtons = -1;
        } else {
            showButtons = position;
        }
        selectedPosition = position;
        showButtons(holder, position);
    }


    /**
     * Otevře složku na základě zadaného ID složky.
     *
     * @param folderId ID složky, která se má otevřít
     */
    private void openFolder(String folderId) {
        Message msg = new Message();
        msg.obj = folderId;
        if (handlerOpenFolder != null) {
            handlerOpenFolder.sendMessage(msg);
        }
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
            if (Objects.equals(selectedFileId, shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, ""))) {
                msg.obj = new GoogleDriveService.ResultAction(GoogleDriveService.ResultAction.RESULT_ERROR, context.getString(R.string.cannot_delete_default_folder));
                handlerReloadFiles.sendMessage(msg);
                return;
            }

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
     * Vrátí ID vybraného souboru.
     * <p>
     * Tato metoda vrací ID souboru, který je aktuálně vybrán v adapteru.
     *
     * @return ID vybraného souboru
     */
    public String getSelectedFileId() {
        return selectedFileId;
    }


    /**
     * Aktualizuje data v adapteru a informuje RecyclerView o změně rozsahu položek.
     * Tato metoda by měla být volána, když se změní data, která adapter zobrazuje.
     */
    public void notifyDataChanged() {
        notifyItemRangeChanged(0, getItemCount());
    }


    /**
     * Aktualizuje název souboru v seznamu souborů na Google Drive.
     *
     * @param fileId  ID souboru, který má být aktualizován
     * @param newName nový název, který má být přiřazen souboru
     */
    public void updateFile(String fileId, String newName) {
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (file.getId().equals(fileId)) {
                file.setName(newName);
                notifyItemChanged(i);
                break;
            }
        }
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
        notifyDataChanged();
    }

}
