package cz.xlisto.elektrodroid.modules.backup;


import static cz.xlisto.elektrodroid.permission.Permissions.REQUEST_WRITE_STORAGE;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.permission.Files;
import cz.xlisto.elektrodroid.shp.ShPBackup;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.MainActivityHelper;
import cz.xlisto.elektrodroid.utils.NetworkCallbackImpl;


/**
 * Fragment pro správu zálohování a obnovy dat aplikace.
 *
 * <p>Zajišťuje UI pro vytváření ZIP záloh, výběr složky pro ukládání a zobrazení
 * existujících záložních souborů v {@code RecyclerView}. Spolupracuje s
 * {@code BackupViewModel} pro asynchronní načítání souborů a udržování stavu
 * (loading, dostupnost oprávnění). Obsahuje handlery pro zpracování výsledků
 * ukládání a obnovy a používá {@code ActivityResultLauncher} pro výběr adresáře.</p>
 *
 * <p>Implementuje {@code NetworkCallbackImpl.NetworkChangeListener} pro reakci na
 * změny síťové dostupnosti (aktualizace dat). Kontroluje a vyžaduje oprávnění
 * k přístupu k URI přes utilitu {@code Files} a spravuje registraci síťového
 * callbacku v cyklu života fragmentu.</p>
 *
 * @see BackupViewModel
 * @see Files
 * @see NetworkCallbackImpl.NetworkChangeListener
 */
public class BackupFragment extends Fragment implements NetworkCallbackImpl.NetworkChangeListener {

    private static final String TAG = "BackupFragment";
    private Button btnBackup;
    private RelativeLayout rlDescriptionPermission;
    private RecyclerView recyclerView;
    private ArrayList<DocumentFile> documentFiles = new ArrayList<>(); //seznam souborů
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;
    private ConnectivityManager connectivityManager;
    private NetworkCallbackImpl networkCallback;
    private BackupViewModel backupViewModel;
    private Uri uri;
    private ShPBackup shPBackup;

    //Callback z výběru složky
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

    //handler po uložení souboru
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
                if (recyclerViewHeight < itemHeight * (backupAdapter.getItemCount() + 1))
                    animator.start();
            }

            btnBackup.setEnabled(true);
        }
    };

    //handler pro výsledek obnovení databáze
    private final Handler handlerResultRecovery = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            boolean b = (boolean) msg.obj;
            if (b) {
                Snackbar.make(requireView(), getResources().getString(R.string.recovery_ok), Snackbar.LENGTH_LONG).show();

                ShPBackup shPSubscriptionPoint = new ShPBackup(requireContext());
                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.loadFirstIdSubscriptionPoint();
                shPSubscriptionPoint.set(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, dataSubscriptionPointSource.loadFirstIdSubscriptionPoint());
                dataSubscriptionPointSource.close();

                MainActivityHelper.updateToolbarAndLoadData(requireActivity());
            } else {
                Snackbar.make(requireView(), getResources().getString(R.string.recovery_fail), Snackbar.LENGTH_LONG).show();
            }
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
        return inflater.inflate(R.layout.fragment_backup, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();

        shPBackup = new ShPBackup(requireContext());

        btnBackup = view.findViewById(R.id.btnZalohuj);
        Button btnSelectDir = view.findViewById(R.id.btnVyberSlozkuBackup);
        rlDescriptionPermission = view.findViewById(R.id.rlDescriptionPermission);
        recyclerView = view.findViewById(R.id.recyclerViewBackup);
        btnBackup.setOnClickListener(v -> saveToZip());
        btnSelectDir.setOnClickListener((v) -> Files.openTree(false, requireActivity(), resultTree));
        lnProgressBar = view.findViewById(R.id.lnProgressBar);

        requireContext();

        connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new NetworkCallbackImpl(this);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        backupViewModel = new ViewModelProvider(this).get(BackupViewModel.class);
        backupViewModel.getDocumentFiles().observe(getViewLifecycleOwner(), documentFiles -> {
            this.documentFiles = documentFiles;
            backupAdapter = new BackupAdapter(requireActivity(), documentFiles, recyclerView, handlerResultRecovery, null);
            recyclerView.setAdapter(backupAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
            showLnProgressBar(false);
        });
        backupViewModel.getIsLoading().observe(getViewLifecycleOwner(), this::showLnProgressBar);
        backupViewModel.getHasPermission().observe(getViewLifecycleOwner(), has -> {
            btnBackup.setEnabled(Boolean.TRUE.equals(has));
            if (Boolean.TRUE.equals(has)) {
                rlDescriptionPermission.setVisibility(View.GONE);
            } else {
                rlDescriptionPermission.setVisibility(View.VISIBLE);
                if (backupAdapter != null)
                    backupAdapter.clearData();
                View root = requireActivity().findViewById(android.R.id.content);
                if (root != null) {
                    Snackbar.make(root, getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                            .setAction(getResources().getString(R.string.select), v -> Files.openTree(false, requireActivity(), resultTree))
                            .show();
                }
            }
        });

        if (savedInstanceState == null)
            loadFiles();

        //posluchač výsledku dialogového okna pro obnovení databáze
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_BACKUP, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.recoverDatabaseFromZip();
            }
        });
        //posluchač výsledku dialogového okna pro smazání záložního souboru
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.deleteFile();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        backupViewModel.checkPermission(requireActivity(), uri);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }


    /**
     * Uloží databáze do ZIPu a aktualizuje RecyclerView.
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
     * Zobrazí nebo skryje `LinearLayout` s ProgressBar pomocí jednoduché fade animace.
     *
     * <p>Pokud je parametr {@code b} {@code true}, nastaví se viditelnost `lnProgressBar`
     * na {@code View.VISIBLE} a spustí se animace {@code android.R.anim.fade_in}.
     * Pokud je {@code b} {@code false}, nastaví se viditelnost na {@code View.GONE}
     * a spustí se animace {@code android.R.anim.fade_out}.</p>
     *
     * <p>Metoda předpokládá, že běží na UI vlákně a že pole {@code lnProgressBar}
     * je inicializované (není {@code null}).</p>
     *
     * @param b {@code true} pro zobrazení progress baru, {@code false} pro jeho skrytí
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
     * Metoda volaná při dostupnosti sítě.
     * Tato metoda je volána, když je detekováno, že zařízení má přístup k síti.
     * Aktualizuje data v adapteru a informuje RecyclerView o změně rozsahu položek.
     */
    @Override
    public void onNetworkAvailable() {
        Log.w(TAG, "Network is available");
        notifyDataChanged();
    }


    /**
     * Metoda volaná při ztrátě sítě.
     * Tato metoda je volána, když je detekováno, že zařízení ztratilo přístup k síti.
     * Aktualizuje data v adapteru a informuje RecyclerView o změně rozsahu položek.
     */
    @Override
    public void onNetworkLost() {
        Log.w(TAG, "Network is lost");
        notifyDataChanged();
    }


    /**
     * Aktualizuje data v adapteru a informuje RecyclerView o změně rozsahu položek.
     * Tato metoda by měla být volána, když se změní data, která adapter zobrazuje.
     */
    private void notifyDataChanged() {
        requireActivity().runOnUiThread(() -> {
            if (backupAdapter != null)
                backupAdapter.notifyDataChanged();
        });
    }


    /**
     * Načte soubory z vybraného URI.
     * <p>
     * Tato metoda zkontroluje oprávnění k přístupu k souborům a pokud jsou oprávnění udělena,
     * spustí načítání souborů pomocí `backupViewModel`.
     */
    private void loadFiles() {
        uri = Uri.parse(shPBackup.get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        backupViewModel.checkPermission(requireActivity(), uri);
        if (Files.permissions(requireActivity(), uri)) {
            backupViewModel.loadFiles(requireActivity(), uri, resultTree);
        }
    }

}
