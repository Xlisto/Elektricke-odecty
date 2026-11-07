package cz.xlisto.elektrodroid.modules.aboutme;


import androidx.core.text.HtmlCompat;
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
 * Adaptér pro {@link RecyclerView}, který zobrazuje historii verzí a seznam změn aplikace v obrazovce O aplikaci.
 */
public class MyItemAboutMeRecyclerViewAdapter extends RecyclerView.Adapter<MyItemAboutMeRecyclerViewAdapter.ViewHolder> {

    private final List<VersionModel> items;

    /**
     * Vytvoří novou instanci adaptéru se seznamem změn v jednotlivých verzích.
     *
     * @param items seznam modelů verzí ({@link VersionModel}) určených k zobrazení
     */
    public MyItemAboutMeRecyclerViewAdapter(List<VersionModel> items) {
        this.items = items;
    }

    /**
     * Vytváří novou instanci {@link ViewHolder} nafouknutím rozvržení položky seznamu verzí.
     *
     * @param parent   rodičovský {@link ViewGroup}, do kterého bude nový pohled vložen
     * @param viewType typ pohledu pro novou položku
     * @return nový {@link ViewHolder} s navázaným {@link ItemAboutMeBinding}
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAboutMeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    /**
     * Bindle data z modelu verze na zadané pozici do zobrazení položky.
     * Formátuje seznam změn do HTML struktury pro zobrazení v {@link TextView}.
     *
     * @param holder   {@link ViewHolder}, který má být aktualizován
     * @param position pozice položky v adaptéru
     */
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.versionModel = items.get(position);
        holder.tvDate.setText(holder.versionModel.date());
        holder.tvVersion.setText(holder.itemView.getContext().getString(R.string.version, holder.versionModel.version()));
        // Převede pole změn na HTML
        StringBuilder changesHtml = new StringBuilder("<ul>");
        for (int i = 0; i < holder.versionModel.changes().length; i++) {
            String change = holder.versionModel.changes()[i];
            changesHtml.append(change);
            if (i < holder.versionModel.changes().length - 1)
                changesHtml.append("<br><br>");
        }
        // Nastaví HTML řetězec do TextView
        holder.tvContent.setText(HtmlCompat.fromHtml(changesHtml.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.itemView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /**
     * Vrací celkový počet zobrazených verzí v adaptéru.
     *
     * @return počet položek v seznamu
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Držitel pohledů ({@link RecyclerView.ViewHolder}) pro jednotlivou položku historie verzí.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvDate;
        public final TextView tvVersion;
        public final TextView tvContent;
        public VersionModel versionModel;

        /**
         * Vytvoří držitel pohledů navázaný na zobrazení z {@link ItemAboutMeBinding}.
         *
         * @param binding objekt zobrazení položky historie verzí
         */
        public ViewHolder(ItemAboutMeBinding binding) {
            super(binding.getRoot());
            tvDate = binding.itemDate;
            tvVersion = binding.itemVersion;
            tvContent = binding.itemContent;
        }

        /**
         * Vrací řetězcovou reprezentaci držitele pohledů s obsahem textu změn.
         *
         * @return řetězcová reprezentace objektu {@link ViewHolder}
         */
        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + tvContent.getText() + "'";
        }

    }

}