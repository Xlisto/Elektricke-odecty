package cz.xlisto.odecty.modules.exportimportpricelist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.MainActivity;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.modules.pricelist.PriceListFragment;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.permission.Files;
import cz.xlisto.odecty.modules.backup.RecoverData;
import cz.xlisto.odecty.shp.ShPBackup;
import cz.xlisto.odecty.shp.ShPFilter;
import cz.xlisto.odecty.shp.ShPMainActivity;
import cz.xlisto.odecty.utils.FragmentChange;

import static cz.xlisto.odecty.dialogs.YesNoDialogRecyclerViewFragment.SELECTED_ARRAYLIST;
import static cz.xlisto.odecty.shp.ShPFilter.DATUM;
import static cz.xlisto.odecty.shp.ShPFilter.DODAVATEL;
import static cz.xlisto.odecty.shp.ShPFilter.PRODUKT;
import static cz.xlisto.odecty.shp.ShPFilter.RADA;
import static cz.xlisto.odecty.shp.ShPFilter.SAZBA;
import static cz.xlisto.odecty.shp.ShPFilter.UZEMI;
import static cz.xlisto.odecty.shp.ShPMainActivity.ACTUAL_FRAGMENT;
import static cz.xlisto.odecty.utils.FragmentChange.Transaction.ALPHA;

/**
 * Xlisto 06.12.2023 18:40
 */
public class ImportPriceListFragment extends Fragment {
    private static final String TAG = "ExportImportPriceListFragment";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_1_PRICES = "exportDialogFragment1Prices";
    public static final String FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_2_PRICES = "exportDialogFragment2Prices";
    private ShPBackup shPBackup;
    private RecyclerView recyclerView;
    private LinearLayout lnProgressBar;
    private ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
    private ImportPriceListAdapter importExportAdapter;
    private static final String[] filtersFileName = {".json"};
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
                }
            }
    );

    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            documentFiles.clear();
            documentFiles = (ArrayList<DocumentFile>) msg.obj;
            importExportAdapter = new ImportPriceListAdapter(requireActivity(), documentFiles, recyclerView);
            recyclerView.setAdapter(importExportAdapter);
            recyclerView.scheduleLayoutAnimation();
            recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

            showLnProgressBar(false);
            handlerLoadFile.removeCallbacksAndMessages(null);
            handlerLoadFile.removeMessages(22);
        }
    };


    public static ImportPriceListFragment newInstance() {
        return new ImportPriceListFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_price_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPBackup = new ShPBackup(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewImportExport);
        lnProgressBar = view.findViewById(R.id.lnProgressBar);
        Button btnSelectDir = view.findViewById(R.id.btnSelectFolderImportExport);

        btnSelectDir.setOnClickListener(v -> Files.openTree(false, requireActivity(), resultTree));


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
                        shpFilter.set(DODAVATEL, doublePriceList.getFirma());
                        shpFilter.set(UZEMI, doublePriceList.getDistribuce());
                        shpFilter.set(DATUM, ViewHelper.convertLongToDate(doublePriceList.getPlatnostOD()));//tady je potřeba převádět long číslo na datum ve stringu, jinak při načítání následuje pád aplikace
                        Fragment priceListFragment = PriceListFragment.newInstance(false, -1L);
                        FragmentChange.replace(requireActivity(), priceListFragment, ALPHA);
                    }
                }));
    }


    @Override
    public void onResume() {
        super.onResume();
        Uri uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        if (Files.permissions(requireActivity(), uri)) {
            new Files().loadFiles(requireActivity(), uri, filtersFileName, handlerLoadFile, resultTree, 22);
        }

        documentFiles.clear();
        recyclerView.setAdapter(new ImportPriceListAdapter(requireActivity(), documentFiles, recyclerView));//nastavení prázdného adaptéru kvůli warning: No adapter attached; skipping layout
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        showLnProgressBar(true);
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
                    String title = "Ceník již existuje";
                    String message = "Ceník " + priceList.getProdukt() + ", " + priceList.getSazba() + "\nbyl nalezen.\n\nPřejete si jej přepsat?";
                    if (selectedPriceLists.size() > 1) {
                        title = "Ceníky již existují";
                        message = "Několik ceníků již bylo nalezeno.\nPřejete si je všechny přepsat?";
                    }
                    YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(title, FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_1_PRICES, message);
                    yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                }


            } else {
                //zobrazí upozornění, že ceník již existuje ve více verzích
                doublePriceList = priceList;
                YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance("Ceník již existuje", FLAG_DIALOG_FRAGMENT_EXPORT_IMPORT_2_PRICES,
                        "Ceník " + priceList.getProdukt() + ", " + priceList.getSazba() + "\nbyl nalezen ve více shodných variantách. Nepoužívanou variantu odstraňte ručně.\n\nPřepnout se do seznamu s ceníky?");
                yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                break;
            }

        }
    }

}
