package cz.xlisto.elektrodroid.modules.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
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

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;


/**
 * Adaptér zobrazení souboru záloh
 * Xlisto 24.04.2023 20:06
 */
public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.MyViewHolder> {
    private static final String TAG = "BackupAdapter";
    public static final String FLAG_DIALOG_FRAGMENT_BACKUP = "backupDialogFragmentBackup";
    public static final String FLAG_DIALOG_FRAGMENT_DELETE = "backupDialogFragmentDelete";
    private final List<DocumentFile> documentFiles;
    private final Context context;
    private final RecyclerView recyclerView;
    private static int showButtons = -1;
    private int selectedPosition;
    private static DocumentFile selectedFile;
    private final Handler handler;


    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvTyp;
        ImageView iconFile;
        RelativeLayout rl;
        LinearLayout ln;
        Button btnRestore;
        Button btnDelete;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    public BackupAdapter(Context context, List<DocumentFile> documentFiles, RecyclerView recyclerView, Handler handler) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
        this.handler = handler;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        vh.tvName = v.findViewById(R.id.tvNameBackup);
        vh.tvTyp = v.findViewById(R.id.tvTypeBackup);
        vh.iconFile = v.findViewById(R.id.imgIconFile);
        vh.rl = v.findViewById(R.id.rlBackupItem);
        vh.ln = v.findViewById(R.id.lnButtonsBackup);
        vh.btnRestore = v.findViewById(R.id.btnRestoreBackup);
        vh.btnDelete = v.findViewById(R.id.btnDeleteBackup);
        return vh;
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DocumentFile object = documentFiles.get(position);
        String text = object.toString();
        holder.tvName.setText(text);

        boolean cenik = Objects.requireNonNull(object.getName()).contains(".cenik");
        boolean odecet = Objects.requireNonNull(object.getName()).contains(".odecet");
        boolean zip_old = Objects.requireNonNull(object.getName()).contains("ElektroDroid.zip");
        boolean zip_now = Objects.requireNonNull(object.getName()).contains("El odecet.zip");


        if (cenik) text = object.getName().replace(".cenik", "");
        if (odecet) text = object.getName().replace(".odecet", "");
        if (zip_old) {
            text = object.getName().replace(" ElektroDroid.zip", "");
        }
        if (zip_now) {
            text = object.getName().replace(" El odecet.zip", "");
        }
        holder.tvName.setText(text);


        if (cenik) {
            holder.iconFile.setImageResource(R.mipmap.ic_cenik);
            holder.tvTyp.setText(context.getResources().getString(R.string.backup_pricelist));
        }
        if (odecet) {
            holder.iconFile.setImageResource(R.mipmap.ic_odecet);
            holder.tvTyp.setText(context.getResources().getString(R.string.backup_readings));
        }
        if (zip_old || zip_now) {
            if (zip_old)
                holder.iconFile.setImageResource(R.mipmap.ic_odecet);
            if (zip_now)
                holder.iconFile.setImageResource(R.mipmap.ic_odecet_new);
            holder.tvTyp.setText(context.getResources().getString(R.string.backup_zip));
        }

        showButtons(holder, position);

        holder.rl.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsBackup).setVisibility(View.GONE);
            }

            if (showButtons == position)
                showButtons = -1;
            else
                showButtons = position;
            selectedPosition = position;

            showButtons(holder, position);
        });

        holder.btnRestore.setOnClickListener(v -> {
            selectedFile = object;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance("Obnovit zálohu", FLAG_DIALOG_FRAGMENT_BACKUP);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.btnDelete.setOnClickListener(v -> {
            selectedFile = object;
            selectedPosition = position;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance("Smazat zálohu", FLAG_DIALOG_FRAGMENT_DELETE);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });
    }


    @Override
    public int getItemCount() {
        if (documentFiles == null)
            return 0;
        return documentFiles.size();
    }


    /**
     * Smaže vybraný záložní soubor
     */
    public void deleteFile() {
        selectedFile.delete();
        documentFiles.remove(selectedPosition);
        notifyItemRemoved(selectedPosition);
        notifyItemRangeChanged(selectedPosition, documentFiles.size());
        selectedFile = null;
        showButtons = -1;
    }


    /**
     * Obnoví data ze záložního vybraného souboru zip
     */
    public void recoverDatabaseFromZip() {
        boolean b = RecoverDataFromBackupFile.recoverDatabaseFromZip(context, selectedFile);
        Message msg = new Message();
        msg.obj = b;
        handler.sendMessage(msg);
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
            selectedFile = documentFiles.get(selectedPosition);
            showButtons = selectedPosition;
        }
    }
}
