package cz.xlisto.odecty.modules.backup;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.permission.Files;
import cz.xlisto.odecty.shp.ShPBackup;

/**
 * Xlisto 07.03.2023 12:36
 */
public class BackupFragment extends Fragment {
    private static final String TAG = "BackupFragment";
    private Button btnBackup;
    private RecyclerView recyclerView;
    private ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
    private static final int REQUEST_WRITE_STORAGE = 0;//může být jakékoliv číslo typu int, slouží pro oddělení jednotlivých oprávnění
    private ShPBackup shPBackup;
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;
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
    //handler pro načtení souborů
    private final Handler handlerLoadFile = new Handler(Looper.getMainLooper()) {
        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            documentFiles =  (ArrayList<DocumentFile>) msg.obj;
            backupAdapter = new BackupAdapter(requireActivity(), documentFiles, recyclerView);
            recyclerView.setAdapter(backupAdapter);
            recyclerView.scheduleLayoutAnimation();
            recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

            showLnProgressBar(false);
        }
    };
    //handler pro uložení souboru
    private final Handler handlerSaveZip = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            documentFiles.add((DocumentFile) msg.obj);
            SortFile.quickSortDate(documentFiles);
            backupAdapter.moveToPosition();
            backupAdapter.notifyItemInserted(0);
            backupAdapter.notifyItemRangeChanged(0, documentFiles.size());

            if (recyclerView.getChildCount() > 0) {

                // Získání výšky jednoho prvku v RecyclerView (pokud je pevná výška)
                int itemHeight = recyclerView.getChildAt(0).getHeight();
                int recyclerViewHeight = recyclerView.getHeight();

                // Vytvoření ValueAnimator pro animovaný posun
                ValueAnimator animator = ValueAnimator.ofInt(0, itemHeight);
                animator.setDuration(1000); // Dobu trvání

                animator.addUpdateListener(animation -> {
                    int animatedValue = (int) animation.getAnimatedValue();
                    recyclerView.scrollBy(0, -animatedValue); // Záporné znaménko posune o výšku jedné položky nahoru
                });

                // Spuštění animace
                if (recyclerViewHeight < itemHeight * (backupAdapter.getItemCount()+1))
                    animator.start();
            }

            btnBackup.setEnabled(true);
        }
    };


    /**
     * Vytvoření instance fragmentu
     *
     * @return instance fragmentu
     */
    public static BackupFragment newInstance() {
        return new BackupFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_backup, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shPBackup = new ShPBackup(requireContext());
        btnBackup = view.findViewById(R.id.btnZalohuj);
        Button btnSelectDir = view.findViewById(R.id.btnVynerSlozku);
        recyclerView = view.findViewById(R.id.recyclerViewBackup);
        btnBackup.setOnClickListener(v -> saveToZip());
        btnSelectDir.setOnClickListener((v) -> Files.openTree(false, requireActivity(), resultTree));
        lnProgressBar = view.findViewById(R.id.lnProgressBar);

        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_BACKUP, this, (requestKey, result) -> {


            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.recoverDatabaseFromZip();
            }

        });

        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_DELETE, this, (requestKey, result) -> {

            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.deleteFile();
            }

        });
    }


    @Override
    public void onResume() {
        super.onResume();
        Uri uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        if (Files.permissions(requireActivity(), uri))
            showLnProgressBar(true);


        documentFiles.clear();
        recyclerView.setAdapter(new BackupAdapter(requireActivity(), documentFiles, recyclerView));//nastavení prázdného adaptéru kvůli warning: No adapter attached; skipping layout
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        btnBackup.setEnabled(Files.permissions(requireActivity(), uri));
        Files.loadFiles(requireActivity(), uri, RecoverData.getFiltersFileName(), handlerLoadFile, resultTree);
    }


    /**
     * Uloží databáze do ZIPu a aktualizuje recyclerview
     */
    private void saveToZip() {
        btnBackup.setEnabled(false);
        SaveDataToBackupFile.saveToZip(requireActivity(), handlerSaveZip);
    }


    //TODO: DEPRECATED!!!
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {// If request is cancelled, the fakturyArrayList arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(requireView(), "Přístup povolen", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(requireView(), "Přístup zamítnut", Snackbar.LENGTH_LONG).show();
            }
        }
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
}
