package cz.xlisto.odecty.modules.exportimportpricelist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Document;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;

/**
 * Xlisto 06.12.2023 18:57
 */
public class ExportImportPriceListAdapter extends RecyclerView.Adapter<ExportImportPriceListAdapter.MyViewHolder>{
    private static final String TAG = "ExportImportPriceListAdapter";
    private final List<DocumentFile> documentFiles;
    private final Context context;
    private final RecyclerView recyclerView;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName,tvTyp;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public ExportImportPriceListAdapter(Context context, List<DocumentFile> documentFiles, RecyclerView recyclerView) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_export_price_list, parent, false);
        MyViewHolder vh = new MyViewHolder(view);
        vh.tvName = view.findViewById(R.id.tvNameImportExportFile);
        vh.tvTyp = view.findViewById(R.id.tvTypeImportExportFile);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
    DocumentFile documentFile = documentFiles.get(position);

    holder.tvName.setText(documentFile.getName().replace(".json",""));

    }

    @Override
    public int getItemCount() {
        if (documentFiles == null)
            return 0;
        return documentFiles.size();
    }


}
