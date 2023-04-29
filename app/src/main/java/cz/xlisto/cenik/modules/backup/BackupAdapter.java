package cz.xlisto.cenik.modules.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.cenik.R;
import cz.xlisto.cenik.dialogs.YesNoDialogFragment;
import cz.xlisto.cenik.ownview.ViewHelper;

/**
 * Adaptér zobrazení souboru záloh
 * Xlisto 24.04.2023 20:06
 */
public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.MyViewHolder>{
    private static final String TAG = "BackupAdapter";
    private List<DocumentFile> documentFiles;
    private Context context;
    private RecyclerView recyclerView;
    private int showButtons = -1;
    private OnListenerFile onListenerFile;
    private DocumentFile selectedFile;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        DocumentFile object;
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

    public BackupAdapter(Context context,List<DocumentFile> documentFiles, RecyclerView recyclerView) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
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
        String text = "";
        text = "Text "+object;
        holder.tvName.setText(text);



        boolean cenik = object.getName().contains(".cenik");
        boolean odecet = object.getName().contains(".odecet");
        boolean zip = object.getName().contains("ElektroDroid.zip");
        if (cenik) text = object.getName().replace(".cenik", "");
        if (odecet) text = object.getName().replace(".odecet", "");
        if (zip) text = object.getName().replace("ElektroDroid.zip", "");
        Calendar cl = Calendar.getInstance();
        holder.tvName.setText(text);
        try {
            if (!zip) {
                long textLong = Long.parseLong(text);
                cl.setTimeInMillis(textLong);
                Date date = cl.getTime();
                holder.tvName.setText(ViewHelper.convertLongToTime(date.getTime()));
            }

        } catch (
                Exception e) {
            e.printStackTrace();
        }

        if (cenik) {
            holder.iconFile.setImageResource(R.mipmap.ic_cenik);
            holder.tvTyp.setText("záloha ceník");
        }
        if (odecet) {
            holder.iconFile.setImageResource(R.mipmap.ic_odecet);
            holder.tvTyp.setText("Záloha odečet");
        }
        if (zip) {
            holder.iconFile.setImageResource(R.mipmap.ic_odecet);
            holder.tvTyp.setText("Záloha komprimovaná");
        }

        showButtons( holder, position);

        holder.rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                showButtons( holder, position);
            }
        });

        holder.btnRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFile = object;
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(new YesNoDialogFragment.OnDialogResult() {
                    @Override
                    public void onResult(boolean b) {
                        if (b) {
                            onListenerFile.onListenerFile(selectedFile);
                            selectedFile = null;
                        }
                    }
                }, "Obnovit zálohu");
                yesNoDialogFragment.show(((FragmentActivity)context).getSupportFragmentManager(), TAG);
            }
        });
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedFile = object;
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(new YesNoDialogFragment.OnDialogResult() {
                    @Override
                    public void onResult(boolean b) {
                       if (b) {
                            object.delete();
                            documentFiles.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, documentFiles.size());
                            selectedFile = null;
                            showButtons = -1;
                        }
                    }
                }, "Smazat zálohu");
                yesNoDialogFragment.show(((FragmentActivity)context).getSupportFragmentManager(), TAG);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (documentFiles == null)
            return 0;
        return documentFiles.size();
    }

    private void showButtons( MyViewHolder holder, int position) {
        if (showButtons == position)
            holder.ln.setVisibility(View.VISIBLE);
        else
            holder.ln.setVisibility(View.GONE);
    }

    public void setOnListenerFile(OnListenerFile onListenerFile) {
        this.onListenerFile = onListenerFile;
    }

    public interface OnListenerFile {
        void onListenerFile(DocumentFile file);
    }

}
