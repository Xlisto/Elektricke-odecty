package cz.xlisto.elektrodroid.modules.backup;


import android.accounts.Account;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.api.services.drive.model.File;

import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;
import cz.xlisto.elektrodroid.utils.NetworkCallbackImpl;
import cz.xlisto.elektrodroid.utils.NetworkUtil;


/**
 * UI vrstva pro správu Google Drive záloh.
 *
 * <p>Fragment zajišťuje přihlášení/odhlášení přes {@link CredentialHelper}, načtení seznamu
 * dostupných záloh z Google Drive, reakci na změny konektivity a obsluhu akcí uživatele
 * (obnovení nebo smazání vybrané zálohy).</p>
 *
 * <p>Stav hromadného mazání je synchronizován přes {@link GoogleDriveViewModel}, takže průběh
 * a výsledek operace zůstává konzistentní i po změně konfigurace (např. rotace obrazovky).</p>
 */
public class GoogleDriveFragment extends Fragment implements CredentialHelper.CredentialListener, GoogleDriveService.OnFilesLoadedListener, NetworkCallbackImpl.NetworkChangeListener, BackupAdapter.SelectionChangeListener, BackupAdapter.DeleteStateListener {

    private static final String TAG = "GoogleDriveFragment";
    private static final int MENU_ACTION_GOOGLE_SIGN = R.id.menu_action_google_sign;

    private TextView tvAlertNoInternet;
    private CredentialHelper credentialHelper;
    private ShPGoogleDrive shPGoogleDrive;
    private GoogleDriveService googleDriveService;
    private RecyclerView recyclerView;
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;
    private ConnectivityManager connectivityManager;
    private NetworkCallbackImpl networkCallback;
    private boolean internetAvailable;
    private int selectedFilesCount;
    private boolean multiSelectMode;
    private boolean suppressSelectionCanceledSnackbar;
    private MenuItem menuItemDeleteSelected;
    private MenuItem menuItemCancelSelection;
    private GoogleDriveDeleteProgressDialogFragment deleteProgressDialog;
    private GoogleDriveViewModel googleDriveViewModel;

    // handler pro zobrazení výsledku obnovení databáze
    private final Handler handlerResultRecoveryDatabase = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            boolean b = (boolean) msg.obj;
            showLnProgressBar(false);
            if (b) {
                Snackbar.make(requireView(), getResources().getString(R.string.recovery_ok), Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(requireView(), getResources().getString(R.string.recovery_fail), Snackbar.LENGTH_LONG).show();
            }
        }
    };


    /**
     * Vyžadovaný prázdný veřejný konstruktor.
     */
    public GoogleDriveFragment() {
    }


    /**
     * Vytvoří novou instanci GoogleDriveFragment.
     *
     * @return Nová instance GoogleDriveFragment.
     */
    public static GoogleDriveFragment newInstance() {
        return new GoogleDriveFragment();
    }


    /**
     * Inicializuje stav fragmentu, zejména první detekci dostupnosti internetu.
     *
     * @param savedInstanceState dříve uložený stav fragmentu, může být {@code null}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        internetAvailable = NetworkUtil.isInternetAvailable(requireContext());
    }


    /**
     * Vytvoří layout, inicializuje závislosti a registruje síťový callback.
     *
     * @param inflater           inflater pro vytvoření layoutu
     * @param container          rodičovský kontejner fragmentu
     * @param savedInstanceState dříve uložený stav fragmentu, může být {@code null}
     * @return kořenový {@link View} fragmentu
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_drive, container, false);
        recyclerView = view.findViewById(R.id.rvGoogleDriveFiles);
        lnProgressBar = view.findViewById(R.id.lnProgressBar);
        tvAlertNoInternet = view.findViewById(R.id.tvAlertNoInternet);

        credentialHelper = new CredentialHelper(requireContext());
        credentialHelper.setCredentialListener(this);

        shPGoogleDrive = new ShPGoogleDrive(requireContext());

        connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new NetworkCallbackImpl(this);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        toggleButtonsAndRecyclerViewVisibility();
        updateAppBarSignMenu();

        loadGoogleFiles();

        return view;
    }


    /**
     * Dokončí inicializaci po vytvoření view a přidá posluchače výsledků dialogů.
     *
     * @param view               kořenový view fragmentu
     * @param savedInstanceState dříve uložený stav fragmentu, může být {@code null}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializace ViewModelu pro sledování stavu mazání přes rotaci
        googleDriveViewModel = new ViewModelProvider(this).get(GoogleDriveViewModel.class);

        // Po rotaci obrazovky může být dialog stále zobrazen ve FragmentManageru.
        // Znovu se k němu připojíme, aby bylo možné ho korektně zavřít.
        androidx.fragment.app.Fragment existingDialog = requireActivity().getSupportFragmentManager()
                .findFragmentByTag(GoogleDriveDeleteProgressDialogFragment.TAG);
        if (existingDialog instanceof GoogleDriveDeleteProgressDialogFragment) {
            deleteProgressDialog = (GoogleDriveDeleteProgressDialogFragment) existingDialog;
        }

        // Sledování stavu mazání – po rotaci reagujeme na výsledek z ViewModelu
        googleDriveViewModel.getDeleteState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            switch (state.status()) {
                case FINISHED:
                    dismissDeleteProgressDialog();
                    View root = getView();
                    if (root != null) {
                        String message;
                        if (state.success()) {
                            message = getString(R.string.deleted_selected_google_drive_files);
                        } else if (state.deletedCount() > 0) {
                            message = getString(R.string.partially_deleted_selected_google_drive_files, state.deletedCount(), state.totalCount());
                        } else {
                            message = getString(R.string.not_deleted_selected_google_drive_files);
                        }
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                    }
                    googleDriveViewModel.resetToIdle();
                    loadGoogleFiles();
                    break;
                case FAILED:
                    dismissDeleteProgressDialog();
                    View rootFail = getView();
                    if (rootFail != null && state.errorMessage() != null)
                        Snackbar.make(rootFail, state.errorMessage(), Snackbar.LENGTH_LONG).show();
                    googleDriveViewModel.resetToIdle();
                    break;
                case IN_PROGRESS:
                    // dialog je již zobrazen, nic neprovádíme
                    break;
                case IDLE:
                default:
                    break;
            }
        });

        setupMenuProvider();

        // posluchač výsledku dialogového okna pro obnovení databáze
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_BACKUP, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                showLnProgressBar(true);
                backupAdapter.downloadAndRecoveryFile();
                Log.w(TAG, "obnovit databázi");
            }
        });

        // posluchač výsledku dialogového okna pro smazání záložního souboru
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.deleteFile();
                Log.w(TAG, "smazat soubor");
            }
        });
    }


    /**
     * Uklidí systémové zdroje fragmentu (odregistruje callback změny konektivity).
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }


    /**
     * Zaregistruje menu pro app bar pomocí {@link MenuProvider} API navázaného na lifecycle view.
     */
    private void setupMenuProvider() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_google_drive, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                menuItemDeleteSelected = menu.findItem(R.id.menu_action_google_delete_selected);
                menuItemCancelSelection = menu.findItem(R.id.menu_action_google_cancel_selection);
                MenuItem signItem = menu.findItem(MENU_ACTION_GOOGLE_SIGN);
                if (signItem == null || shPGoogleDrive == null)
                    return;

                boolean isUserSignedIn = shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false);
                signItem.setVisible(internetAvailable);
                signItem.setIcon(isUserSignedIn ? R.drawable.ic_logout_24 : R.drawable.ic_login_24);
                signItem.setTitle(isUserSignedIn ? R.string.sign_out : R.string.sign_in);

                if (menuItemDeleteSelected != null) {
                    menuItemDeleteSelected.setVisible(internetAvailable && multiSelectMode);
                    menuItemDeleteSelected.setEnabled(internetAvailable && multiSelectMode && selectedFilesCount > 0);
                    menuItemDeleteSelected.setTitle(getString(R.string.delete_selected_google_drive_files_button, selectedFilesCount));
                }

                if (menuItemCancelSelection != null) {
                    menuItemCancelSelection.setVisible(multiSelectMode);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == MENU_ACTION_GOOGLE_SIGN) {
                    if (credentialHelper == null || shPGoogleDrive == null)
                        return true;

                    if (shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false))
                        credentialHelper.signOutWithCredentialManager();
                    else
                        credentialHelper.signInWithCredentialManager();
                    return true;
                }
                if (item.getItemId() == R.id.menu_action_google_delete_selected) {
                    deleteSelectedFiles();
                    return true;
                }
                if (item.getItemId() == R.id.menu_action_google_cancel_selection) {
                    if (backupAdapter != null) {
                        suppressSelectionCanceledSnackbar = true;
                        backupAdapter.cancelMultiSelect();
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    /**
     * Callback po úspěšném přihlášení uživatele k Google účtu.
     *
     * @param account přihlášený Google účet
     */
    @Override
    public void onSignInSuccess(Account account) {
        Log.w(TAG, "onSignInSuccess: " + account.name);
        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, true);
        shPGoogleDrive.set(ShPGoogleDrive.USER_NAME, account.name);
        toggleButtonsAndRecyclerViewVisibility();
        updateAppBarSignMenu();
        loadGoogleFiles();
    }


    /**
     * Callback při chybě přihlášení uživatele.
     *
     * @param noCredentials {@code true}, pokud v zařízení nejsou dostupné přihlašovací údaje
     * @param errorMessage  technický popis chyby pro logování
     */
    @Override
    public void onSignInError(boolean noCredentials, String errorMessage) {
        if (!isAdded())
            return;

        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, false);
        String message = noCredentials
                ? getString(R.string.sign_in_no_credentials)
                : getString(R.string.sign_in_failed);
        updateAppBarSignMenu();

        View root = getView();
        if (root != null)
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();

        Log.w(TAG, "onSignInError: " + errorMessage);
    }


    /**
     * Callback po úspěšném odhlášení uživatele.
     */
    @Override
    public void onSignOutSuccess() {
        Log.w(TAG, "onSignOutSuccess");
        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, false);
        shPGoogleDrive.set(ShPGoogleDrive.USER_NAME, "");
        if (backupAdapter != null) {
            suppressSelectionCanceledSnackbar = true;
            backupAdapter.cancelMultiSelect();
        }
        updateSelectionActions(0, false);
        dismissDeleteProgressDialog();
        toggleButtonsAndRecyclerViewVisibility();
        updateAppBarSignMenu();
        onFilesLoaded(List.of());
    }


    /**
     * Načte soubory z Google Drive, pokud je uživatel přihlášen.
     */
    public void loadGoogleFiles() {
        Log.w(TAG, "loadGoogleFiles: internetAvailable: " + internetAvailable);
        if (!internetAvailable)
            return;

        if (shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, "").isEmpty())
            return;

        if (backupAdapter != null)
            backupAdapter.clearData();

        showLnProgressBar(true);
        googleDriveService = new GoogleDriveService(requireContext(), shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, ""));
        googleDriveService.setOnFilesLoadedListener(this);
    }


    /**
     * Smaže vybrané soubory z Google Drive.
     */
    private void deleteSelectedFiles() {
        if (backupAdapter == null || selectedFilesCount == 0)
            return;

        if (!internetAvailable) {
            View root = getView();
            if (root != null)
                Snackbar.make(root, getString(R.string.internet_is_not_available), Snackbar.LENGTH_LONG).show();
            return;
        }

        suppressSelectionCanceledSnackbar = true;
        backupAdapter.deleteSelectedFiles();
    }


    /**
     * Zobrazí nebo skryje LinearLayout s ProgressBar.
     *
     * @param show {@code true} pro zobrazení, {@code false} pro skrytí
     */
    private void showLnProgressBar(boolean show) {
        if (show) {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(requireActivity(), android.R.anim.fade_in));
            lnProgressBar.setVisibility(View.VISIBLE);
        } else {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
            lnProgressBar.setVisibility(View.GONE);
        }
    }


    /**
     * Callback s načtenými soubory z Google Drive.
     *
     * @param files seznam záloh načtených ze služby
     */
    @Override
    public void onFilesLoaded(List<File> files) {
        Log.w(TAG, "onFilesLoaded: " + files.size());
        requireActivity().runOnUiThread(() -> {
            showLnProgressBar(false);
            backupAdapter = new BackupAdapter(requireActivity(), files, recyclerView, handlerResultRecoveryDatabase, googleDriveService, this, this);
            recyclerView.setAdapter(backupAdapter);
            updateSelectionActions(0, false);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        });
    }


    /**
     * Callback při obnovení internetového připojení.
     */
    @Override
    public void onNetworkAvailable() {
        Log.w(TAG, "onNetworkAvailable");
        internetAvailable = true;
        toggleButtonsAndRecyclerViewVisibility();
        updateAppBarSignMenu();
    }


    /**
     * Callback při ztrátě internetového připojení.
     */
    @Override
    public void onNetworkLost() {
        Log.w(TAG, "onNetworkLost");
        internetAvailable = false;
        toggleButtonsAndRecyclerViewVisibility();
        updateAppBarSignMenu();
    }


    /**
     * Nastaví viditelnost obsahu a stavových hlášek podle internetu a přihlášení.
     */
    private void toggleButtonsAndRecyclerViewVisibility() {
        requireActivity().runOnUiThread(() -> {
            boolean isUserSignedIn = shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false);

            if (internetAvailable) {
                recyclerView.setVisibility(isUserSignedIn ? View.VISIBLE : View.INVISIBLE);
                if (isUserSignedIn) {
                    tvAlertNoInternet.setVisibility(View.GONE);
                } else {
                    tvAlertNoInternet.setText(getString(R.string.google_drive_signed_out_warning));
                    tvAlertNoInternet.setVisibility(View.VISIBLE);
                }
                if (isUserSignedIn)
                    loadGoogleFiles();
            } else {
                recyclerView.setVisibility(View.INVISIBLE);
                tvAlertNoInternet.setText(getString(R.string.internet_is_not_available));
                tvAlertNoInternet.setVisibility(View.VISIBLE);
            }
            updateAppBarSignMenu();
        });
    }


    /**
     * Synchronizuje stav výběru v recycler view s menu aplikace.
     *
     * @param selectedCount počet vybraných položek
     * @param isMultiSelectMode {@code true}, pokud je aktivní režim více výběru
     */
    private void updateSelectionActions(int selectedCount, boolean isMultiSelectMode) {
        boolean wasMultiSelectMode = multiSelectMode;
        selectedFilesCount = selectedCount;
        multiSelectMode = isMultiSelectMode;

        if (wasMultiSelectMode && !isMultiSelectMode && selectedCount == 0) {
            if (!suppressSelectionCanceledSnackbar && isAdded()) {
                View root = getView();
                if (root != null)
                    Snackbar.make(root, getString(R.string.selection_canceled), Snackbar.LENGTH_SHORT).show();
            }
            suppressSelectionCanceledSnackbar = false;
        }

        updateAppBarSignMenu();
    }


    /**
     * Callback změny výběru z adapteru.
     *
     * @param selectedCount počet vybraných položek
     * @param isMultiSelectMode {@code true}, pokud je aktivní režim více výběru
     */
    @Override
    public void onSelectionChanged(int selectedCount, boolean isMultiSelectMode) {
        updateSelectionActions(selectedCount, isMultiSelectMode);
    }


    /**
     * Zobrazení progress dialogu pro mazání.
     *
     * @param totalCount celkový počet souborů ke smazání
     */
    @Override
    public void onDeleteStarted(int totalCount) {
        if (googleDriveViewModel != null)
            googleDriveViewModel.setInProgress(totalCount);

        if (!isAdded())
            return;

        androidx.fragment.app.Fragment existing = requireActivity().getSupportFragmentManager().findFragmentByTag(GoogleDriveDeleteProgressDialogFragment.TAG);
        if (existing instanceof GoogleDriveDeleteProgressDialogFragment) {
            deleteProgressDialog = (GoogleDriveDeleteProgressDialogFragment) existing;
        } else {
            deleteProgressDialog = GoogleDriveDeleteProgressDialogFragment.newInstance(totalCount);
            deleteProgressDialog.show(requireActivity().getSupportFragmentManager(), GoogleDriveDeleteProgressDialogFragment.TAG);
        }
        deleteProgressDialog.showProgress(0, totalCount);
    }


    /**
     * Aktualizace průběhu mazání.
     *
     * @param processedCount počet již smazaných souborů
     * @param totalCount celkový počet souborů
     */
    @Override
    public void onDeleteProgress(int processedCount, int totalCount) {
        if (!isAdded())
            return;

        if (deleteProgressDialog != null)
            deleteProgressDialog.showProgress(processedCount, totalCount);
    }


    /**
     * Dokončení mazání vybraných souborů.
     *
     * @param success {@code true}, pokud byly smazány všechny soubory
     * @param deletedCount počet úspěšně smazaných souborů
     * @param totalCount celkový počet souborů
     */
    @Override
    public void onDeleteFinished(boolean success, int deletedCount, int totalCount) {
        // Výsledek uložíme do ViewModelu – fragment ho zpracuje (i po rotaci přes observer)
        if (googleDriveViewModel != null) {
            googleDriveViewModel.setFinished(success, deletedCount, totalCount);
            return;
        }

        // Fallback – pokud ViewModel není dostupný (neočekávaný stav)
        if (!isAdded())
            return;

        dismissDeleteProgressDialog();
        View root = getView();
        if (root != null) {
            String message;
            if (success) {
                message = getString(R.string.deleted_selected_google_drive_files);
            } else if (deletedCount > 0) {
                message = getString(R.string.partially_deleted_selected_google_drive_files, deletedCount, totalCount);
            } else {
                message = getString(R.string.not_deleted_selected_google_drive_files);
            }
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
        loadGoogleFiles();
    }


    /**
     * Chyba při mazání souborů.
     *
     * @param message text chyby
     */
    @Override
    public void onDeleteFailed(String message) {
        // Výsledek uložíme do ViewModelu – fragment ho zpracuje (i po rotaci přes observer)
        if (googleDriveViewModel != null) {
            googleDriveViewModel.setFailed(message);
            return;
        }

        // Fallback
        if (!isAdded())
            return;

        dismissDeleteProgressDialog();
        View root = getView();
        if (root != null)
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Bezpečně zavře dialog průběhu mazání a uvolní na něj referenci.
     *
     * <p>Metoda toleruje stav, kdy dialog není vytvořen nebo není připojen k FragmentManageru.</p>
     */
    private void dismissDeleteProgressDialog() {
        if (deleteProgressDialog == null)
            return;

        if (deleteProgressDialog.isAdded()) {
            deleteProgressDialog.dismissAllowingStateLoss();
        }
        deleteProgressDialog = null;
    }


    /**
     * Vynutí překreslení položky přihlášení/odhlášení v app baru.
     */
    private void updateAppBarSignMenu() {
        if (!isAdded())
            return;
        requireActivity().invalidateOptionsMenu();
    }

}