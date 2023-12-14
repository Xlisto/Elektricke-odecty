package cz.xlisto.odecty.modules.exportimportpricelist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.PriceListSumModel;
import cz.xlisto.odecty.modules.backup.RecoverData;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.permission.Files;
import cz.xlisto.odecty.shp.ShPBackup;
import cz.xlisto.odecty.utils.JSONPriceList;

import static cz.xlisto.odecty.modules.exportimportpricelist.ExportPriceListAdapter.FLAG_DIALOG_FRAGMENT_EXPORT;
import static cz.xlisto.odecty.modules.exportimportpricelist.ExportPriceListAdapter.FLAG_DIALOG_FRAGMENT_EXPORT_REWRITE;

/**
 * Export ceníků do JSON souboru
 * Xlisto 12.12.2023 11:17
 */
public class ExportPriceListFragment extends Fragment {
    private static final String TAG = "ExportPriceListFragment";
    private RecyclerView recyclerView;
    private ExportPriceListAdapter exportPriceListAdapter;
    private String fileName, json;
    public static ExportPriceListFragment newInstance() {
        return new ExportPriceListFragment();
    }


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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_export_price_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerViewExport);

        //posluchač dialogu pro vytvoření zálohy
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DIALOG_FRAGMENT_EXPORT, this, (requestKey, result) -> {

            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                ShPBackup shPBackup = new ShPBackup(requireActivity());
                PriceListSumModel priceListSumModel = exportPriceListAdapter.getSelectedPriceListSumModel();

                DataPriceListSource dataPriceListSource = new DataPriceListSource(requireActivity());
                dataPriceListSource.open();
                ArrayList<PriceListModel> priceListModels = dataPriceListSource.readPriceList(priceListSumModel.getRada(), "%", "%",
                        priceListSumModel.getFirma(), priceListSumModel.getArea(), String.valueOf(priceListSumModel.getDatum()));
                dataPriceListSource.close();

                JSONPriceList jsonPriceList = new JSONPriceList();
                json = jsonPriceList.buildJSON(priceListSumModel, priceListModels);
                Files files = new Files();
                fileName = priceListSumModel.getFirma() + " "
                        + priceListSumModel.getRada() + " "
                        + ViewHelper.getSimpleDateFormat().format(priceListSumModel.getDatum()) + " "
                        + priceListModels.get(0).getDistribuce() + ".json";

                if(files.existJSONFile(requireActivity(), fileName, Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI)))){
                    YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(getString(R.string.export_price_list_title),FLAG_DIALOG_FRAGMENT_EXPORT_REWRITE
                            ,getResources().getString(R.string.export_price_list_rewrite_file_message, fileName));
                    yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), YesNoDialogFragment.TAG);
                    return;
                }
                files.saveJSONFile(requireActivity(), json, fileName, Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI)), resultTree);
            }
        });

        //posluchač dialogu pro přepsání souboru
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DIALOG_FRAGMENT_EXPORT_REWRITE, this, (requestKey, result) -> {

            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                ShPBackup shPBackup = new ShPBackup(requireActivity());
                Files files = new Files();
                files.saveJSONFile(requireActivity(), json, fileName, Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI)), resultTree);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        DataPriceListSource dataPriceListSource = new DataPriceListSource(getContext());
        dataPriceListSource.open();
        ArrayList<PriceListSumModel> priceListSumModels = dataPriceListSource.readSumPriceList();
        dataPriceListSource.close();

        exportPriceListAdapter = new ExportPriceListAdapter(getContext(), priceListSumModels);
        recyclerView.setAdapter(exportPriceListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }
}
