package cz.xlisto.elektrodroid.modules.exportimportpricelist;


import android.annotation.SuppressLint;
import android.content.Context;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogRecyclerViewFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.JSONPriceList;

/**
 * Xlisto 06.12.2023 18:57
 */
public class ImportPriceListAdapter extends RecyclerView.Adapter<ImportPriceListAdapter.MyViewHolder> {
    private static final String TAG = "ExportImportPriceListAdapter";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_BACKUP = "exportDialogFragmentBackup";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_DELETE = "exportDialogFragmentDelete";
    private final List<DocumentFile> documentFiles;
    private final Context context;
    private final RecyclerView recyclerView;
    private static int showButtons = -1;
    private int selectedPosition;
    private static DocumentFile selectedFile;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTyp, tvTyp2;
        RelativeLayout rl;
        LinearLayout ln;
        Button btnRestore, btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public ImportPriceListAdapter(Context context, List<DocumentFile> documentFiles, RecyclerView recyclerView) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
        showButtons = -1;
        selectedPosition = -1;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_price_list, parent, false);
        MyViewHolder vh = new MyViewHolder(view);
        vh.tvName = view.findViewById(R.id.tvNameImportExportFile);
        vh.tvTyp = view.findViewById(R.id.tvTypeImportExportFile);
        vh.tvTyp2 = view.findViewById(R.id.tvType2ImportExportFile);
        vh.rl = view.findViewById(R.id.rlImportItem);
        vh.ln = view.findViewById(R.id.lnButtonsExportImport);
        vh.btnRestore = view.findViewById(R.id.btnRestoreImportExport);
        vh.btnDelete = view.findViewById(R.id.btnDeleteImportExport);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DocumentFile documentFile = documentFiles.get(position);
        holder.tvName.setText(Objects.requireNonNull(documentFile.getName()).replace(".json", ""));
        Runnable runnable = () -> {
                ArrayList<PriceListModel> priceList = (new JSONPriceList().getPriceList(context, documentFile));
                if(priceList.size()>0) {
                    holder.tvName.setText(priceList.get(0).getRada());
                    holder.tvTyp.setText(context.getResources().getString(R.string.import_price_list_typ1,priceList.get(0).getFirma(),priceList.get(0).getDistribuce()));
                    holder.tvTyp2.setText(context.getResources().getString(R.string.import_price_list_typ2,priceList.size(),ViewHelper.convertLongToDate(priceList.get(0).getPlatnostOD())));
                }
        };
        runnable.run();

        holder.rl.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(recyclerView);
            if (showButtons >= 0) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(showButtons);
                if (viewHolder != null)
                    viewHolder.itemView.findViewById(R.id.lnButtonsExportImport).setVisibility(View.GONE);
            }

            if (showButtons == position)
                showButtons = -1;
            else
                showButtons = position;
            selectedPosition = position;

            showButtons(holder, position);
        });

        holder.btnRestore.setOnClickListener(v -> {
            selectedFile = documentFile;
            YesNoDialogRecyclerViewFragment yesNoDialogRecyclerViewFragment = YesNoDialogRecyclerViewFragment
                    .newInstance("Nahrát ceník do aplikace", FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_BACKUP, documentFile);
            yesNoDialogRecyclerViewFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.btnDelete.setOnClickListener(v -> {
            selectedFile = documentFile;
            selectedPosition = position;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance("Smazat soubor s ceníkem", FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_DELETE);
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
        selectedPosition = -1;
        showButtons = -1;
    }


    /**
     * Načte JSON soubor s ceníkem
     */
    public void loadFilePriceList() {
        JSONPriceList jsonPriceList = new JSONPriceList();
        jsonPriceList.getPriceList(context, selectedFile);
        //selectedFile = null;
    }


    /**
     * Zobrazí tlačítka pro nahrání nebo smazání souboru s ceníkem
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
}
