package cz.xlisto.elektrodroid.modules.exportimportpricelist;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListSumModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

/**
 * Xlisto 12.12.2023 18:21
 */
public class ExportPriceListAdapter extends RecyclerView.Adapter<ExportPriceListAdapter.MyViewHolder> {

    public final static String FLAG_DIALOG_FRAGMENT_EXPORT = "dialogFragmentExport";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_REWRITE = "backupDialogFragmentExportRewrite";
    private final Context context;
    private final ArrayList<PriceListSumModel> priceListSumModels;
    private PriceListSumModel selectedPriceListSumModel;


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvRada, tvArea, tvDate, tvFirma;
        RelativeLayout rl;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvExportDate);
            tvFirma = itemView.findViewById(R.id.tvExportFirma);
            tvArea = itemView.findViewById(R.id.tvExportArea);
            tvRada = itemView.findViewById(R.id.tvExportRada);
            rl = itemView.findViewById(R.id.rlExportItem);
        }
    }


    public ExportPriceListAdapter(Context context, ArrayList<PriceListSumModel> priceListSumModels) {
        this.context = context;
        this.priceListSumModels = priceListSumModels;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_export_price_list, parent, false);
        return new MyViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PriceListSumModel priceListSumModel = priceListSumModels.get(position);
        holder.tvRada.setText(priceListSumModel.getRada());
        holder.tvArea.setText(context.getResources().getString(R.string.dist_area, priceListSumModel.getArea()));
        holder.tvDate.setText(context.getResources().getString(R.string.validity, ViewHelper.convertLongToDate(priceListSumModel.getDatum())));
        holder.tvFirma.setText(priceListSumModel.getFirma());

        holder.rl.setOnClickListener(v -> {
            selectedPriceListSumModel = priceListSumModel;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getResources().getString(R.string.export_price_list_title), FLAG_DIALOG_FRAGMENT_EXPORT
                    , context.getResources().getString(R.string.export_price_list_title_message, priceListSumModel.getRada(), priceListSumModel.getCount()));
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), YesNoDialogFragment.TAG);
        });
    }


    @Override
    public int getItemCount() {
        if (priceListSumModels == null) {
            return 0;
        }
        return priceListSumModels.size();
    }


    /**
     * Vrátí vybraný ceník
     * @return vybraný ceník
     */
    public PriceListSumModel getSelectedPriceListSumModel() {
        return selectedPriceListSumModel;
    }
}
