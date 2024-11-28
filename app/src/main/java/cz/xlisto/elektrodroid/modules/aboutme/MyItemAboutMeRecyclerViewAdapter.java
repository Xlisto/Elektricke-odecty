package cz.xlisto.elektrodroid.modules.aboutme;


import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databinding.ItemAboutMeBinding;
import cz.xlisto.elektrodroid.models.VersionModel;


/**
 * Adapter pro RecyclerView, který zobrazuje seznam verzí.
 */
public class MyItemAboutMeRecyclerViewAdapter extends RecyclerView.Adapter<MyItemAboutMeRecyclerViewAdapter.ViewHolder> {

    private final List<VersionModel> items;


    /**
     * Konstruktor adapteru.
     *
     * @param items Seznam verzí, které mají být zobrazeny.
     */
    public MyItemAboutMeRecyclerViewAdapter(List<VersionModel> items) {
        this.items = items;
    }


    /**
     * Vytváří nový ViewHolder při vytvoření nové položky v RecyclerView.
     *
     * @param parent   Rodičovský ViewGroup, ke kterému bude ViewHolder připojen.
     * @param viewType Typ pohledu.
     * @return Nový ViewHolder.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAboutMeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }


    /**
     * Připojuje data k ViewHolderu na zadané pozici.
     *
     * @param holder   ViewHolder, který má být aktualizován.
     * @param position Pozice položky v adapteru.
     */
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.versionModel = items.get(position);
        holder.tvDate.setText(holder.versionModel.getDate());
        holder.tvVersion.setText(holder.itemView.getContext().getString(R.string.version, holder.versionModel.getVersion()));
        // Převede pole změn na HTML
        StringBuilder changesHtml = new StringBuilder("<ul>");
        for (int i = 0; i < holder.versionModel.getChanges().length; i++) {
            String change = holder.versionModel.getChanges()[i];
            changesHtml.append(change);
            if (i < holder.versionModel.getChanges().length - 1)
                changesHtml.append("<br><br>");
        }
        // Nastaví HTML řetězec do TextView
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvContent.setText(Html.fromHtml(changesHtml.toString(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvContent.setText(Html.fromHtml(changesHtml.toString()));
        }
        holder.itemView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
    }


    /**
     * Vrací počet položek v adapteru.
     *
     * @return Počet položek.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }


    /**
     * ViewHolder, který obsahuje pohledy pro jednotlivé položky v RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvDate;
        public final TextView tvVersion;
        public final TextView tvContent;
        public VersionModel versionModel;


        /**
         * Konstruktor ViewHolderu.
         *
         * @param binding DataBinding objekt pro položku.
         */
        public ViewHolder(ItemAboutMeBinding binding) {
            super(binding.getRoot());
            tvDate = binding.itemDate;
            tvVersion = binding.itemVersion;
            tvContent = binding.itemContent;
        }


        /**
         * Vrací řetězcovou reprezentaci ViewHolderu.
         *
         * @return Řetězcová reprezentace ViewHolderu.
         */
        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + tvContent.getText() + "'";
        }

    }

}