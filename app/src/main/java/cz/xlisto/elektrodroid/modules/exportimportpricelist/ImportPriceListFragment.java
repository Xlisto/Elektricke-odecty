package cz.xlisto.elektrodroid.modules.exportimportpricelist;


import static cz.xlisto.elektrodroid.dialogs.YesNoDialogRecyclerViewFragment.SELECTED_ARRAYLIST;
import static cz.xlisto.elektrodroid.shp.ShPFilter.AREA;
import static cz.xlisto.elektrodroid.shp.ShPFilter.COMPANY;
import static cz.xlisto.elektrodroid.shp.ShPFilter.DATE_START;
import static cz.xlisto.elektrodroid.shp.ShPFilter.PRODUKT;
import static cz.xlisto.elektrodroid.shp.ShPFilter.RADA;
import static cz.xlisto.elektrodroid.shp.ShPFilter.SAZBA;
import static cz.xlisto.elektrodroid.shp.ShPMainActivity.ACTUAL_FRAGMENT;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.ALPHA;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.backup.RecoverData;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListFragment;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.permission.Files;
import cz.xlisto.elektrodroid.shp.ShPBackup;
import cz.xlisto.elektrodroid.shp.ShPFilter;
import cz.xlisto.elektrodroid.shp.ShPMainActivity;
import cz.xlisto.elektrodroid.utils.FragmentChange;


/**
 * Fragment pro import ceníků z externího adresáře (JSON zálohy).
 * <p>
 * Úel:
 * - Poskytuje UI pro výběr složky se zálohami, zobrazení dostupných souborů a výběr ceníků k importu.
 * - Řídí proces importu: vložení nových záznamů, přípravu přepsání existujících záznamů
 * a řešení konfliktů při více verzích téhož ceníku.
 * <p>
 * Hlavní závislosti a komponenty:
 * - ImportPriceListViewModel: načítání souborů a stav oprávnění.
 * - ImportPriceListAdapter: zobrazení a správa výběru souborů v RecyclerView.
 * - DataPriceListSource: operace nad lokální databází (insert/update/count/id).
 * - Files: kontrola a vyžádání oprávnění k přístupu k adresáři (Storage Access Framework).
 * - YesNoDialogFragment: potvrzovací dialogy pro přepsání/řešení konfliktů.
 * - ShPBackup / ShPMainActivity / ShPFilter: uložené preference a nastavení navigace/filtru.
 * <p>
 * Chování a životní cyklus:
 * - V onViewCreated nastavuje pozorovatele LiveData, posluchače výsledků dialogů a UI akce.
 * - V onResume kontroluje a případně obnovuje oprávnění pro zvolený URI a spouští načtení souborů.
 * <p>
 * Vedlejší efekty:
 * - Zapisuje/aktualizuje záznamy v databázi, mění shared preferences (aktuální fragment/filtr),
 * naviguje uživatele a zobrazuje Snackbar/Toast pro informace a výzvy k akci.
 * <p>
 * Poznámky k implementaci:
 * - Statické proměnné `singlePriceLists` a `doublePriceList` slouží k předání dat mezi callbacky dialogů.
 * - Importní logika používá kontrolu počtu záznamů v DB: 0 = vložit, 1 = připravit přepsání, >1 = řešit konflikt.
 */
public class ImportPriceListFragment extends Fragment {

    private static final String TAG = "ExportImportPriceListFragment";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_1_PRICES = "exportDialogFragment1Prices";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_2_PRICES = "exportDialogFragment2Prices";
    private RecyclerView recyclerView;
    private LinearLayout lnProgressBar;
    private ImportPriceListAdapter importExportAdapter;
    private ImportPriceListViewModel importPriceListViewModel;
    private Uri uri;
    private ShPBackup shPBackup;
    private Button btnSelectFolder;
    private TextView tvDescriptionPermition;
    private static PriceListModel doublePriceList;//ceníky, který je v databázi vícekrát
    private static ArrayList<PriceListModel> singlePriceLists;//ceníky, které jsou v databázi jednou

    /**
     * Callback pro výběr adresáře přes Storage Access Framework.
     * <p>
     * Volá se po návratu z intentu spuštěného metodou `Files.openTree`.
     * - Zpracuje pouze výsledek `Activity.RESULT_OK`.
     * - Použije `Files.activityResult(Intent, Activity)` pro uložení oprávnění k URI.
     * - Poté vyvolá `loadFiles()` pro načtení souborů z nově vybraného adresáře.
     * <p>
     * Poznámky:
     * - Registrováno přes `ActivityResultContracts.StartActivityForResult`.
     * - Spouští se na UI vlákně.
     * - Používá `assert` že `result.getData()` není `null`.
     */
    private final ActivityResultLauncher<Intent> resultTree = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    Files.activityResult(result.getData(), requireActivity());
                    loadFiles();
                }
            }
    );


    /**
     * Vytvoří novou instanci ImportPriceListFragment.
     * <p>
     * Tovární metoda vrací nový objekt fragmentu. Pokud bude potřeba
     * předat parametry, lze je sem vložit do Bundle a nastavit přes
     * setArguments(bundle) před návratem instance.
     *
     * @return nová instance ImportPriceListFragment
     */
    public static ImportPriceListFragment newInstance() {
        return new ImportPriceListFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    /**
     * Vytvoří a vrátí kořenové View pro tento fragment.
     * <p>
     * Metoda inflatesuje layout `R.layout.fragment_import_price_list` pomocí předaného
     * `LayoutInflater` a vrací výsledné View. Inicializace a nastavení UI komponent
     * nejsou prováděny zde, ale v `onViewCreated`.
     *
     * @param inflater           LayoutInflater použitý k rozbalení XML layoutu
     * @param container          Rodičovský ViewGroup, do kterého může být layout vložen (může být null)
     * @param savedInstanceState Uložený stav fragmentu, pokud existuje
     * @return Kořenové View fragmentu (inflated layout)
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_price_list, container, false);
    }


    /**
     * Inicializuje UI komponenty, ViewModel a chování fragmentu po vytvoření View.
     * <p>
     * V metodě se provádí:
     * - invalidace menu aktivity,
     * - inicializace referencí na View (RecyclerView, tlačítka, popisky, progress),
     * - vytvoření a připojení ImportPriceListViewModel a jeho LiveData observerů (seznam souborů, loading, oprávnění),
     * - registrace fragment result listenerů pro mazání souborů, potvrzení importu a řešení přepisů/konfliktů,
     * - nastavení onClick listeneru pro výběr složky a volání načtení souborů pokud je `savedInstanceState == null`.
     *
     * @param view               kořenové View fragmentu
     * @param savedInstanceState uložený stav fragmentu, může být null
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        shPBackup = new ShPBackup(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewImportExport);
        lnProgressBar = view.findViewById(R.id.lnProgressBar);
        btnSelectFolder = view.findViewById(R.id.btnSelectFolder);
        tvDescriptionPermition = view.findViewById(R.id.tvDescriptionPermission);

        importPriceListViewModel = new ViewModelProvider(this).get(ImportPriceListViewModel.class);
        importPriceListViewModel.getDocumentFiles().observe(getViewLifecycleOwner(), documentFiles -> {
            if (documentFiles != null) {
                importExportAdapter = new ImportPriceListAdapter(requireActivity(), documentFiles, recyclerView);
                recyclerView.setAdapter(importExportAdapter);
                recyclerView.scheduleLayoutAnimation();
                recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
            }
        });
        importPriceListViewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLnProgressBar);
        importPriceListViewModel.getHasPermission().observe(getViewLifecycleOwner(), has -> {
            if (Boolean.TRUE.equals(has)) {
                btnSelectFolder.setVisibility(View.GONE);
                tvDescriptionPermition.setVisibility(View.GONE);
            } else {
                btnSelectFolder.setVisibility(View.VISIBLE);
                tvDescriptionPermition.setVisibility(View.VISIBLE);
                if (importExportAdapter != null)
                    importExportAdapter.clear();
                Snackbar.make(view, getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                        .setAction(requireActivity().getResources().getString(R.string.select), v -> Files.openTree(false, requireActivity(), resultTree))
                        .show();
            }
        });

        if (savedInstanceState == null) {
            loadFiles();
        }

        //listener dialogového okna na smazání souboru JSON zálohy
        requireActivity().getSupportFragmentManager().setFragmentResultListener(ImportPriceListAdapter.FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                importExportAdapter.deleteFile();
            }
        });

        //listener při seznamu vybraných ceníků k importu
        //zde je seznam zatržených ceníků připravených k importu do databáze
        requireActivity().getSupportFragmentManager().setFragmentResultListener(ImportPriceListAdapter.FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_BACKUP, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                ArrayList<PriceListModel> priceLists = (ArrayList<PriceListModel>) result.getSerializable(SELECTED_ARRAYLIST);
                assert priceLists != null;
                saveToDatabase(priceLists);
            }
        });

        //listener při potvrzení přepisu jednoho záznamu v databázi
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_1_PRICES, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
                dataPriceListSource.open();
                for (PriceListModel priceList : singlePriceLists) {
                    dataPriceListSource.updatePriceList(priceList, priceList.getId());
                }
                dataPriceListSource.close();
                Toast.makeText(requireContext(), requireContext().getResources().getString(R.string.rewrite_prices_list), Toast.LENGTH_SHORT).show();
            }
        });

        //listener při potvrzení existence více než dvou ceníků
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_2_PRICES, this,
                ((requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        if (mainActivity != null && !isDetached()) {
                            mainActivity.onCheckedNavigationItem(R.id.meni_prices);
                        }
                        ShPMainActivity shPMainActivity = new ShPMainActivity(requireContext());
                        shPMainActivity.set(ACTUAL_FRAGMENT, R.id.meni_prices);

                        //nastavení filtru ceníku na hledaný ceník
                        ShPFilter shpFilter = new ShPFilter(requireContext());
                        shpFilter.set(RADA, doublePriceList.getRada());
                        shpFilter.set(PRODUKT, doublePriceList.getProdukt());
                        shpFilter.set(SAZBA, doublePriceList.getSazba());
                        shpFilter.set(COMPANY, doublePriceList.getFirma());
                        shpFilter.set(AREA, doublePriceList.getDistribuce());
                        shpFilter.set(DATE_START, ViewHelper.convertLongToDate(doublePriceList.getPlatnostOD()));//tady je potřeba převádět long číslo na datum ve stringu, jinak při načítání následuje pád aplikace
                        Fragment priceListFragment = PriceListFragment.newInstance(false, -1L);
                        FragmentChange.replace(requireActivity(), priceListFragment, ALPHA);
                    }
                }));

        btnSelectFolder.setOnClickListener(v -> Files.openTree(false, requireActivity(), resultTree));
    }


    /**
     * Obnoví stav fragmentu při návratu do popředí.
     * <p>
     * - Načte uložené URI složky záloh ze `ShPBackup` a uloží ho do proměnné `uri`.
     * - Pošle tento `uri` do `ImportPriceListViewModel` pro kontrolu oprávnění k přístupu.
     * <p>
     * Poznámka: Implementace by měla volat `super.onResume()` před nebo po vlastní logice.
     */
    @Override
    public void onResume() {
        super.onResume();
        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        importPriceListViewModel.checkPermission(requireActivity(), uri);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        requireActivity().getMenuInflater().inflate(R.menu.menu_export, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_item_select_folder) {
            Files.openTree(false, requireActivity(), resultTree);
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Zobrazí nebo skryje kontejner s ProgressBar pomocí jednoduché fade animace.
     * <p>
     * Pokud je parametr `true`, nastaví `lnProgressBar` na VISIBLE a spustí animaci
     * `android.R.anim.fade_in`. Pokud je `false`, spustí `android.R.anim.fade_out`
     * a po animaci nastaví `lnProgressBar` na GONE.
     *
     * @param b true = zobrazit progress, false = skrýt progress
     */
    private void showLnProgressBar(boolean b) {
        int visibility, animation;
        if (b) {
            visibility = View.VISIBLE;
            animation = android.R.anim.fade_in;
        } else {
            visibility = View.GONE;
            animation = android.R.anim.fade_out;
        }
        lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), animation));
        lnProgressBar.setVisibility(visibility);
    }


    /**
     * Zpracuje seznam vybraných ceníků pro import do lokální databáze.
     * <p>
     * Pro každý ceník:
     * - zjistí počet odpovídajících záznamů v DB pomocí `DataPriceListSource`,
     * - pokud není žádný (\=0) — vloží nový záznam,
     * - pokud je přesně jeden (\=1) — nastaví jeho `id`, přidá do `singlePriceLists` a po skončení vyvolá dialog pro potvrzení přepisu,
     * - pokud je více než jeden (\>1) — uloží tento ceník do `doublePriceList` a vyvolá dialog pro řešení více verzí (navigace/filtrace).
     * <p>
     * Metoda provádí databázové operace (`open`/`close`/`insert`/`update`/`count`), zobrazuje Toast/YesNoDialogFragment
     * a může změnit navigaci nebo SharedPreferences (přes dialogy nebo filtry).
     *
     * @param selectedPriceLists seznam vybraných ceníků k importu
     */
    private void saveToDatabase(ArrayList<PriceListModel> selectedPriceLists) {
        singlePriceLists = new ArrayList<>();
        for (int i = 0; i < selectedPriceLists.size(); i++) {
            PriceListModel priceList = selectedPriceLists.get(i);
            DataPriceListSource dataPriceListSource = new DataPriceListSource(requireContext());
            dataPriceListSource.open();
            int countPriceListInDatabase = dataPriceListSource.countPriceListItems(priceList.getRada(), priceList.getProdukt(), priceList.getSazba(),
                    priceList.getFirma(), String.valueOf(priceList.getPlatnostOD()), priceList.getDistribuce());
            dataPriceListSource.close();
            if (countPriceListInDatabase == 0) {
                //přidá ceník do databáze, pokud neexistuje
                dataPriceListSource.open();
                dataPriceListSource.insertPriceList(priceList);
                dataPriceListSource.close();
                if (i == selectedPriceLists.size() - 1) {
                    Toast.makeText(requireContext(), requireContext().getResources().getString(R.string.saved_prices_list), Toast.LENGTH_SHORT).show();
                }
            } else if (countPriceListInDatabase == 1) {
                //upraví ceník v databázi, pokud existuje jen jeden
                dataPriceListSource.open();
                long idPriceListInDatabase = dataPriceListSource.idPriceListItem(priceList.getRada(), priceList.getProdukt(), priceList.getSazba(),
                        priceList.getFirma(), String.valueOf(priceList.getPlatnostOD()), priceList.getDistribuce());
                dataPriceListSource.close();
                priceList.setId(idPriceListInDatabase);
                singlePriceLists.add(priceList);
                if (i == selectedPriceLists.size() - 1) {
                    String title = getResources().getString(R.string.price_list_exists);
                    String message = getResources().getString(R.string.rewrite_price_list, priceList.getProdukt(), priceList.getSazba());
                    if (selectedPriceLists.size() > 1) {
                        title = getResources().getString(R.string.price_lists_exist);
                        message = getResources().getString(R.string.found_price_list);
                    }
                    YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(title, FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_1_PRICES, message);
                    yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                }

            } else {
                //zobrazí upozornění, že ceník již existuje ve více verzích
                doublePriceList = priceList;
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(getResources().getString(R.string.price_list_exists), FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_2_PRICES,
                        getResources().getString(R.string.price_list_found_multiple_variants, priceList.getProdukt(), priceList.getSazba()));
                yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                break;
            }

        }
    }


    /**
     * Načte uložené URI složky záloh ze `ShPBackup` a spustí načtení souborů, pokud jsou dostupná oprávnění.
     * <p>
     * Postup:
     * - Načte `uri` z preferencí `ShPBackup`.
     * - Zavolá `importPriceListViewModel.checkPermission(requireActivity(), uri)` pro ověření oprávnění.
     * - Pokud `Files.permissions(requireActivity(), uri)` vrátí `true`, zavolá
     * `importPriceListViewModel.loadFiles(requireActivity(), uri, resultTree)`.
     * <p>
     * Vedlejší efekty: čtení SharedPreferences, volání metod ViewModelu a případné zahájení načítání souborů
     * přes ActivityResult (`resultTree`).
     */
    private void loadFiles() {
        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        importPriceListViewModel.checkPermission(requireActivity(), uri);
        if (Files.permissions(requireActivity(), uri))
            importPriceListViewModel.loadFiles(requireActivity(), uri, resultTree);
    }

}
