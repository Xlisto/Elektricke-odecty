package cz.xlisto.odecty.modules.exportimportpricelist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.modules.backup.FilterNameFile;
import cz.xlisto.odecty.permission.Files;
import cz.xlisto.odecty.permission.Permissions;
import cz.xlisto.odecty.modules.backup.RecoverData;
import cz.xlisto.odecty.modules.backup.SortFile;
import cz.xlisto.odecty.shp.ShPBackup;

/**
 * Xlisto 06.12.2023 18:40
 */
public class ExportImportPriceListFragment extends Fragment {
    private static final String TAG = "ExportImportPriceListFragment";
    private ShPBackup shPBackup;
    private Uri uri;
    private RecyclerView recyclerView;
    private ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ExportImportPriceListAdapter importExportAdapter;
    //private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String[] filtersFileName = {".json"};

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

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            recyclerView.setAdapter(new ExportImportPriceListAdapter(requireActivity(), (ArrayList<DocumentFile>) msg.obj, recyclerView));//nastavení prázdného adaptéru kvůli warning: No adapter attached; skipping layout
            recyclerView.scheduleLayoutAnimation();
        }
    };


    public static ExportImportPriceListFragment newInstance() {
        return new ExportImportPriceListFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_import_export_price_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPBackup = new ShPBackup(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewImportExport);
    }


    @Override
    public void onResume() {
        super.onResume();

        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        documentFiles.clear();
        recyclerView.setAdapter(new ExportImportPriceListAdapter(requireActivity(), documentFiles, recyclerView));//nastavení prázdného adaptéru kvůli warning: No adapter attached; skipping layout
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        Files.loadFiles(requireActivity(), uri, filtersFileName, handler, resultTree);

    }
}
