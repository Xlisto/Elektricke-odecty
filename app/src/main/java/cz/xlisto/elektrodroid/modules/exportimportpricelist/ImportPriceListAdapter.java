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
 * Adapter pro RecyclerView zobrazující záložní JSON soubory s ceníky a nabízející akce (nahrát / smazat).
 * <p>
 * Funkce:
 * - Zobrazuje seznam {@code DocumentFile} položek (souborů \".json\") s názvem, typem a datem platnosti.
 * - Parsuje metadata ceníku pomocí {@link cz.xlisto.elektrodroid.utils.JSONPriceList} a zobrazuje je v položce.
 * - Po kliknutí na položku přepíná viditelnost tlačítek pro akce se základní animací pomocí {@code TransitionManager}.
 * - Umožňuje spustit dialogy pro potvrzení nahrání ({@link cz.xlisto.elektrodroid.dialogs.YesNoDialogRecyclerViewFragment})
 * a smazání ({@link cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment}) souboru.
 * <p>
 * Konstruktor:
 * - Očekává {@code Context}, seznam {@code List<DocumentFile>} a cílový {@code RecyclerView}.
 * - Kontext by měl být instancí {@code FragmentActivity}, protože adapter otevírá dialogy přes fragment manager.
 * <p>
 * Veřejné metody:
 * - {@code deleteFile()} – smaže aktuálně vybraný soubor, upraví seznam a notifikace pro RecyclerView.
 * - {@code loadFilePriceList()} – načte vybraný soubor přes {@code JSONPriceList}.
 * - {@code clear()} – vyčistí seznam položek.
 * <p>
 * Poznámky a omezení:
 * - Parsování JSON probíhá synchronně v {@code onBindViewHolder} (může blokovat UI pro velké soubory).
 * - Používá statické proměnné ({@code showButtons}, {@code selectedFile}) pro uchování stavu, což sdílí stav mezi instancemi adapteru a může vést k nežádoucím vedlejším efektům.
 * - Předpokládá se, že soubory jsou validní JSON ve struktuře očekávané {@code JSONPriceList}.
 * <p>
 * Konstanty:
 * - {@code FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_BACKUP} – flag pro dialog nahrání ceníku.
 * - {@code FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_DELETE} – flag pro dialog smazání souboru.
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


    /**
     * Adapter pro RecyclerView, který zobrazuje záložní JSON soubory s ceníky a nabízí akce (nahrát / smazat).
     * <p>
     * Hlavní funkce:
     * - Zobrazuje seznam {@code DocumentFile} položek (souborů \".json\") s názvem, typem a datumem platnosti.
     * - Parsuje metadata ceníku pomocí {@link cz.xlisto.elektrodroid.utils.JSONPriceList} a zobrazuje je v položce.
     * - Po kliknutí na položku přepíná viditelnost akčních tlačítek s animací pomocí {@link android.transition.TransitionManager}.
     * - Spouští potvrzovací dialogy pro nahrání ({@link cz.xlisto.elektrodroid.dialogs.YesNoDialogRecyclerViewFragment})
     * a smazání ({@link cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment}) vybraného souboru.
     * <p>
     * Konstruktor:
     * - Očekává {@code Context}, {@code List<DocumentFile>} a cílový {@code RecyclerView}.
     * - Kontext by měl být instancí {@code FragmentActivity}, protože adapter otevírá dialogy přes fragment manager.
     * <p>
     * Veřejné metody:
     * - {@code deleteFile()} – smaže aktuálně vybraný soubor, upraví seznam a notifikace pro RecyclerView.
     * - {@code loadFilePriceList()} – načte vybraný soubor přes {@code JSONPriceList}.
     * - {@code clear()} – vyčistí seznam položek.
     * <p>
     * Implementační poznámky a omezení:
     * - Parsování JSON probíhá synchronně v {@code onBindViewHolder} (může blokovat UI u velkých souborů) — zvážit přesun do background threadu.
     * - Použití statických proměnných ({@code showButtons}, {@code selectedFile}) sdílí stav mezi instancemi adapteru a může vést k vedlejším efektům.
     * - Předpokládá se, že soubory jsou validní JSON ve struktuře očekávané {@code JSONPriceList}.
     * <p>
     * Konstanty:
     * - {@code FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_BACKUP} – flag pro dialog nahrání ceníku.
     * - {@code FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_DELETE} – flag pro dialog smazání souboru.
     */
    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvTyp, tvTyp2;
        RelativeLayout rl;
        LinearLayout ln;
        Button btnRestore, btnDelete;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }


    /**
     * Inicializuje adaptér pro zobrazení záložních JSON souborů s ceníky.
     *
     * @param context       Kontext aplikace; očekává se instance {@link androidx.fragment.app.FragmentActivity}
     *                      pro zobrazování potvrzovacích dialogů.
     * @param documentFiles Seznam {@link androidx.documentfile.provider.DocumentFile} souborů k zobrazení
     *                      (může být null, adaptér s tím počítá).
     * @param recyclerView  Cílový {@link androidx.recyclerview.widget.RecyclerView} používaný pro
     *                      animace a vyhledávání ViewHolderů.
     *                      <p>
     *                      Inicializace:
     *                      - Uloží předané parametry do interních polí adaptéru.
     *                      - Resetuje stav viditelnosti akčních tlačítek (\@code showButtons) a vybranou pozici (\@code selectedPosition)
     *                      na výchozí hodnoty.
     *                      <p>
     *                      Poznámky:
     *                      - Kontext musí být {@code FragmentActivity}, jinak volání dialogů bude selhávat.
     */
    public ImportPriceListAdapter(Context context, List<DocumentFile> documentFiles, RecyclerView recyclerView) {
        this.documentFiles = documentFiles;
        this.context = context;
        this.recyclerView = recyclerView;
        showButtons = -1;
        selectedPosition = -1;
    }


    /**
     * Vytvoří a inicializuje nový {@link MyViewHolder} pro položku RecyclerView.
     * <p>
     * Inflatuje layout `R.layout.item_import_price_list`, najde a přiřadí potřebné View
     * (tvName, tvTyp, tvTyp2, rl, ln, btnRestore, btnDelete) a vrátí inicializovaný ViewHolder.
     *
     * @param parent   ViewGroup do kterého bude nový item vložen
     * @param viewType Typ view (v této implementaci se nevyužívá)
     * @return nově vytvořený a inicializovaný {@link MyViewHolder}
     */
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


    /**
     * Propojí data s view pro položku na zadané pozici a nastaví chování položky.
     * <p>
     * Naplní {@code holder} informacemi z {@code documentFiles.get(position)}, synchronně
     * parsuje JSON přes {@link cz.xlisto.elektrodroid.utils.JSONPriceList} a aktualizuje názvy,
     * typy a datum platnosti. Dále nastaví click listenery:
     * - klik na řádek přepíná viditelnost akčních tlačítek s animací ({@link android.transition.TransitionManager}),
     * - tlačítko obnovit otevře {@link cz.xlisto.elektrodroid.dialogs.YesNoDialogRecyclerViewFragment},
     * - tlačítko smazat otevře {@link cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment}.
     * <p>
     * Poznámky:
     * - Parsování JSON probíhá synchronně v UI vlákně a může blokovat UI u velkých souborů.
     * - Metoda manipuluje se sdíleným stavem pomocí statických polí {@code showButtons} a {@code selectedFile}.
     *
     * @param holder   ViewHolder obsahující view položky
     * @param position Pozice položky v adapteru
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DocumentFile documentFile = documentFiles.get(position);
        holder.tvName.setText(Objects.requireNonNull(documentFile.getName()).replace(".json", ""));
        Runnable runnable = () -> {
            ArrayList<PriceListModel> priceList = (new JSONPriceList().getPriceList(context, documentFile));
            if (!priceList.isEmpty()) {
                holder.tvName.setText(priceList.get(0).getRada());
                holder.tvTyp.setText(context.getResources().getString(R.string.import_price_list_typ1, priceList.get(0).getFirma(), priceList.get(0).getDistribuce()));
                holder.tvTyp2.setText(context.getResources().getString(R.string.import_price_list_typ2, priceList.size(), ViewHelper.convertLongToDate(priceList.get(0).getPlatnostOD())));
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
                    .newInstance(context.getString(R.string.upload_file_with_pricelist), FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_BACKUP, documentFile);
            yesNoDialogRecyclerViewFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

        holder.btnDelete.setOnClickListener(v -> {
            selectedFile = documentFile;
            selectedPosition = position;
            YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(context.getString(R.string.delete_file_with_pricelist), FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_DELETE);
            yesNoDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), TAG);
        });

    }


    /**
     * Vrací počet položek v adapteru.
     *
     * @return počet položek zobrazených adapterem; 0 pokud je interní seznam {@code documentFiles} null
     */
    @Override
    public int getItemCount() {
        if (documentFiles == null)
            return 0;
        return documentFiles.size();
    }


    /**
     * Smaže aktuálně vybraný záložní soubor, odstraní jej ze seznamu a notifikuje RecyclerView.
     * <p>
     * Provádí:
     * - volání {@code selectedFile.delete()},
     * - odstranění položky z {@code documentFiles} na pozici {@code selectedPosition},
     * - volání {@code notifyItemRemoved} a {@code notifyItemRangeChanged},
     * - reset {@code selectedFile}, {@code selectedPosition} a {@code showButtons} na výchozí hodnoty.
     * <p>
     * Poznámka: mazání může selhat (metoda {@code DocumentFile.delete()} vrací {@code boolean}) nebo vyžadovat oprávnění;
     * tato metoda současně nekontroluje návratovou hodnotu ani výjimky.
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
     * Vyčistí interní seznam záložních souborů a notifikuje adapter o změně dat.
     * <p>
     * Odstraní všechny položky z {@code documentFiles} a zavolá {@code notifyItemRangeChanged(0, documentFiles.size())}
     * aby RecyclerView aktualizoval zobrazení.
     * <p>
     * Poznámka: po vyprázdnění seznamu může být voláno {@code notifyItemRangeChanged} s velikostí 0;
     * zvážit použití {@code notifyDataSetChanged()} nebo {@code notifyItemRangeRemoved} pro konzistentní notifikace.
     */
    public void clear() {
        documentFiles.clear();
        notifyItemRangeChanged(0, documentFiles.size());
    }


    /**
     * Nastaví viditelnost akčních tlačítek v položce podle aktuální hodnoty {@code showButtons}.
     * <p>
     * Pokud je {@code showButtons} shodné s {@code position}, nastaví {@code holder.ln} na
     * {@link android.view.View#VISIBLE}, jinak na {@link android.view.View#GONE}.
     *
     * @param holder   ViewHolder obsahující layout tlačítek
     * @param position Pozice položky v adapteru, pro kterou se nastavuje viditelnost
     */
    private void showButtons(MyViewHolder holder, int position) {
        int visibility;
        if (showButtons == position)
            visibility = View.VISIBLE;
        else
            visibility = View.GONE;
        holder.ln.setVisibility(visibility);
    }

}
