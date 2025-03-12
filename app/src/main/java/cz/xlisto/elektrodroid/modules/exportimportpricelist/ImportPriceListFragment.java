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
 * Fragment pro importování ceníků.
 * <p>
 * Tento fragment poskytuje uživatelské rozhraní pro importování ceníků z vybraného adresáře.
 * Obsahuje metody pro načítání souborů, sledování stavu načítání a správu vybraných ceníků.
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
    private View view;
    private Button btnSelectFolder;
    private TextView tvDescriptionPermition;
    private static PriceListModel doublePriceList;//ceníky, který je v databázi vícekrát
    private static ArrayList<PriceListModel> singlePriceLists;//ceníky, které jsou v databázi jednou

    /**
     * Callback z výběru složky
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


    public static ImportPriceListFragment newInstance() {
        return new ImportPriceListFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view =  inflater.inflate(R.layout.fragment_import_price_list, container, false);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        shPBackup = new ShPBackup(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewImportExport);
        lnProgressBar = view.findViewById(R.id.lnProgressBar);
        btnSelectFolder = view.findViewById(R.id.btnSelectFolder);
        tvDescriptionPermition = view.findViewById(R.id.tvDescriptionPermition);

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


    @Override
    public void onResume() {
        super.onResume();
        if (Files.permissions(requireActivity(), uri)) {
            btnSelectFolder.setVisibility(View.GONE);
            tvDescriptionPermition.setVisibility(View.GONE);
        } else {
            btnSelectFolder.setVisibility(View.VISIBLE);
            tvDescriptionPermition.setVisibility(View.VISIBLE);
            Snackbar.make(view, getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                    .setAction(requireActivity().getResources().getString(R.string.select), v -> Files.openTree(false, requireActivity(), resultTree))
                    .show();
        }
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
     * Zobrazí nebo skryje LinearLayout s ProgressBar
     */
    private void showLnProgressBar(boolean b) {
        if (b) {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
            lnProgressBar.setVisibility(View.VISIBLE);
        } else {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
            lnProgressBar.setVisibility(View.GONE);
        }
    }


    /**
     * Uloží/upraví nebo se zeptá na další akci s vybranými ceníky pro import do databáze
     *
     * @param selectedPriceLists seznam vybraných ceníků pro import do databáze
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
     * Načte soubory z určeného URI, pokud jsou k dispozici potřebná oprávnění.
     * <p>
     * Tato metoda kontroluje, zda má aplikace oprávnění k přístupu k URI.
     * Pokud oprávnění existují, zavolá metodu `loadFiles` ve ViewModelu `importPriceListViewModel`,
     * která načte soubory z daného URI.
     */
    private void loadFiles() {
        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        if (Files.permissions(requireActivity(), uri))
            importPriceListViewModel.loadFiles(requireActivity(), uri, resultTree);
    }

}
