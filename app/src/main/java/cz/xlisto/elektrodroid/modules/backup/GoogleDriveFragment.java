package cz.xlisto.elektrodroid.modules.backup;


import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.api.services.drive.model.File;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.dialogs.GoogleDriveDeleteProgressDialogFragment;
import cz.xlisto.elektrodroid.dialogs.GoogleDriveSaveProgressDialogFragment;
import cz.xlisto.elektrodroid.dialogs.PendingBackupUploadProgressDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.permission.Files;
import cz.xlisto.elektrodroid.shp.ShPBackup;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;
import cz.xlisto.elektrodroid.utils.MainActivityHelper;
import cz.xlisto.elektrodroid.utils.NetworkCallbackImpl;
import cz.xlisto.elektrodroid.utils.NetworkUtil;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


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
    private static final String FLAG_DIALOG_FRAGMENT_SAVE_TO_LOCAL = "googleDriveFragmentSaveToLocal";
    private static final String STATE_PENDING_SAVE_ALL_FILES = "statePendingSaveAllFiles";
    private static final String STATE_PENDING_SAVE_NON_DUPLICATE_FILES = "statePendingSaveNonDuplicateFiles";
    private static final String DRIVE_FILE_STATE_SEPARATOR = "\u0001";

    private TextView tvAlertNoInternet;
    private TextView tvPendingUploadAlert;
    private CredentialHelper credentialHelper;
    private ShPGoogleDrive shPGoogleDrive;
    private GoogleDriveService googleDriveService;
    private RecyclerView recyclerView;
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;
    private ConnectivityManager connectivityManager;
    private NetworkCallbackImpl networkCallback;
    private boolean internetAvailable;
    private boolean hasPendingWifiUpload;
    private boolean hadPendingWifiUpload;
    private int pendingWifiUploadFilesCount;
    private int selectedFilesCount;
    private boolean multiSelectMode;
    private boolean suppressSelectionCanceledSnackbar;
    private MenuItem menuItemDeleteSelected;
    private MenuItem menuItemSaveSelected;
    private MenuItem menuItemToastMenu;
    private GoogleDriveDeleteProgressDialogFragment deleteProgressDialog;
    private GoogleDriveSaveProgressDialogFragment saveProgressDialog;
    private PendingBackupUploadProgressDialogFragment pendingUploadProgressDialog;
    private GoogleDriveViewModel googleDriveViewModel;
    private ArrayList<File> pendingSaveAllFiles = new ArrayList<>();
    private ArrayList<File> pendingSaveNonDuplicateFiles = new ArrayList<>();

    private final ActivityResultLauncher<Intent> resultTree = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null)
                    return;

                Files.activityResult(result.getData(), requireActivity());
                continuePendingSaveToLocal();
            }
    );

    // handler pro zobrazení výsledku obnovení databáze
    /**
     * Handler pro zpracování výsledku obnovení databáze z Google Drive zálohy.
     *
     * <p>Zavolá se po úspěšném nebo neúspěšném stažení a importu záložního souboru z Google Drive.
     * Handler zajišťuje persistenci aktuálně vybraného odběrného místa během procesu obnovy.</p>
     *
     * <p>Postup při úspěšné obnově (msg.obj == true):</p>
     * <ol>
     *   <li>Skryje progress bar</li>
     *   <li>Zobrazí Snackbar se zprávou "Obnova OK"</li>
     *   <li>Pokusí se obnovit aktuálně vybrané odběrné místo pomocí
     *       {@link cz.xlisto.elektrodroid.utils.SubscriptionPoint#applyCurrentFromSettings(Context)}</li>
     *   <li>Pokud se obnova podařila:
     *       <ul>
     *         <li>Aktualizuje toolbar a znovu načte data</li>
     *       </ul>
     *   </li>
     *   <li>Pokud se obnova nepodařila (místo není v uloženém nastavení):
     *       <ul>
     *         <li>Zobrazí dialog {@link SubscriptionPointDialogFragment} pro výběr místa</li>
     *       </ul>
     *   </li>
     * </ol>
     * </p>
     *
     * <p>Při neúspěšné obnově (msg.obj == false):</p>
     * <ul>
     *   <li>Skryje progress bar</li>
     *   <li>Zobrazí Snackbar se zprávou chyby "Obnova selhala"</li>
     * </ul>
     * </p>
     *
     * @see cz.xlisto.elektrodroid.utils.SubscriptionPoint#applyCurrentFromSettings(Context)
     * @see SubscriptionPointDialogFragment
     */
    private final Handler handlerResultRecoveryDatabase = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            boolean b = (boolean) msg.obj;
            showLnProgressBar(false);
            if (b) {
                Snackbar.make(requireView(), getResources().getString(R.string.recovery_ok), Snackbar.LENGTH_LONG).show();
                if (SubscriptionPoint.applyCurrentFromSettings(requireContext())) {
                    MainActivityHelper.updateToolbarAndLoadData(requireActivity());
                } else {
                    SubscriptionPointDialogFragment.newInstance().show(
                            requireActivity().getSupportFragmentManager(),
                            SubscriptionPointDialogFragment.class.getSimpleName()
                    );
                }
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
        internetAvailable = NetworkUtil.isInternetAllowedBySettings(requireContext());
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
        tvPendingUploadAlert = view.findViewById(R.id.tvPendingUploadAlert);

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

        if (savedInstanceState != null) {
            pendingSaveAllFiles = restoreDriveFilesFromState(savedInstanceState.getStringArrayList(STATE_PENDING_SAVE_ALL_FILES));
            pendingSaveNonDuplicateFiles = restoreDriveFilesFromState(savedInstanceState.getStringArrayList(STATE_PENDING_SAVE_NON_DUPLICATE_FILES));
        }

        // Inicializace ViewModelu pro sledování stavu mazání přes rotaci
        googleDriveViewModel = new ViewModelProvider(this).get(GoogleDriveViewModel.class);

        // Po rotaci obrazovky může být dialog stále zobrazen ve FragmentManageru.
        // Znovu se k němu připojíme, aby bylo možné ho korektně zavřít.
        androidx.fragment.app.Fragment existingDialog = requireActivity().getSupportFragmentManager()
                .findFragmentByTag(GoogleDriveDeleteProgressDialogFragment.TAG);
        if (existingDialog instanceof GoogleDriveDeleteProgressDialogFragment) {
            deleteProgressDialog = (GoogleDriveDeleteProgressDialogFragment) existingDialog;
        }

        androidx.fragment.app.Fragment existingSaveDialog = requireActivity().getSupportFragmentManager()
                .findFragmentByTag(GoogleDriveSaveProgressDialogFragment.TAG);
        if (existingSaveDialog instanceof GoogleDriveSaveProgressDialogFragment) {
            saveProgressDialog = (GoogleDriveSaveProgressDialogFragment) existingSaveDialog;
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

        googleDriveViewModel.getSaveState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            switch (state.status()) {
                case IN_PROGRESS:
                    saveProgressDialog = ensureSaveProgressDialog(state.totalCount());
                    saveProgressDialog.showProgress(state.processedCount(), state.totalCount());
                    break;
                case FINISHED:
                    dismissSaveProgressDialog();
                    if (backupAdapter != null) {
                        suppressSelectionCanceledSnackbar = true;
                        backupAdapter.cancelMultiSelect();
                    }
                    View saveRoot = getView();
                    if (saveRoot != null) {
                        String message;
                        if (state.success()) {
                            message = getString(R.string.saved_selected_google_drive_files);
                        } else if (state.savedCount() > 0) {
                            message = getString(R.string.partially_saved_selected_google_drive_files, state.savedCount(), state.totalCount());
                        } else {
                            message = getString(R.string.not_saved_selected_google_drive_files);
                        }
                        Snackbar.make(saveRoot, message, Snackbar.LENGTH_LONG).show();
                    }
                    googleDriveViewModel.resetSaveToIdle();
                    break;
                case FAILED:
                    dismissSaveProgressDialog();
                    View saveFailRoot = getView();
                    if (saveFailRoot != null && state.errorMessage() != null)
                        Snackbar.make(saveFailRoot, state.errorMessage(), Snackbar.LENGTH_LONG).show();
                    googleDriveViewModel.resetSaveToIdle();
                    break;
                case IDLE:
                default:
                    break;
            }
        });

        observePendingWifiUploadWork();

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

        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DIALOG_FRAGMENT_SAVE_TO_LOCAL, this, (requestKey, result) -> {
            boolean overwrite = result.getBoolean(YesNoDialogFragment.RESULT);
            if (overwrite) {
                startPendingSaveToLocal(new ArrayList<>(pendingSaveAllFiles), true);
            } else {
                if (pendingSaveNonDuplicateFiles.isEmpty()) {
                    clearPendingSaveToLocal();
                    View root = getView();
                    if (root != null)
                        Snackbar.make(root, getString(R.string.save_selected_all_files_already_exist), Snackbar.LENGTH_SHORT).show();
                } else {
                    startPendingSaveToLocal(new ArrayList<>(pendingSaveNonDuplicateFiles), false);
                }
            }
        });
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!pendingSaveAllFiles.isEmpty())
            outState.putStringArrayList(STATE_PENDING_SAVE_ALL_FILES, serializeDriveFiles(pendingSaveAllFiles));
        if (!pendingSaveNonDuplicateFiles.isEmpty())
            outState.putStringArrayList(STATE_PENDING_SAVE_NON_DUPLICATE_FILES, serializeDriveFiles(pendingSaveNonDuplicateFiles));
    }


    /**
     * Uklidí systémové zdroje fragmentu (odregistruje callback změny konektivity).
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissPendingUploadProgressDialog();
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
                menuItemSaveSelected = menu.findItem(R.id.menu_action_google_save_selected);
                menuItemToastMenu = menu.findItem(R.id.menu_action_toast_menu);
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

                if (menuItemSaveSelected != null) {
                    menuItemSaveSelected.setVisible(internetAvailable && multiSelectMode);
                    menuItemSaveSelected.setEnabled(internetAvailable && multiSelectMode && selectedFilesCount > 0);
                    menuItemSaveSelected.setTitle(getString(R.string.save_selected_google_drive_files_button, selectedFilesCount));
                }

                if (menuItemToastMenu != null) {
                    menuItemToastMenu.setVisible(false);
                    menuItemToastMenu.setEnabled(false);
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
                if (item.getItemId() == R.id.menu_action_google_save_selected) {
                    saveSelectedFilesToLocal();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).updateToolbarForSelection(0, false, null);
        }
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
        dismissSaveProgressDialog();
        clearPendingSaveToLocal();
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
     * Uloží vybrané soubory z Google Drive do lokální záložní složky.
     */
    private void saveSelectedFilesToLocal() {
        if (backupAdapter == null || selectedFilesCount == 0)
            return;

        startSaveFilesWorkflow(backupAdapter.getSelectedGoogleDriveFiles());
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
            backupAdapter = new BackupAdapter(requireActivity(), files, recyclerView, handlerResultRecoveryDatabase, googleDriveService, this, this, this::handleSaveGoogleDriveFileRequest);
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
        internetAvailable = NetworkUtil.isInternetAllowedBySettings(requireContext());
        toggleButtonsAndRecyclerViewVisibility();
        updateAppBarSignMenu();
    }


    /**
     * Callback při ztrátě internetového připojení.
     */
    @Override
    public void onNetworkLost() {
        Log.w(TAG, "onNetworkLost");
        internetAvailable = NetworkUtil.isInternetAllowedBySettings(requireContext());
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
                updateConnectionAlert(isUserSignedIn);
                updatePendingUploadAlert();
                if (isUserSignedIn)
                    loadGoogleFiles();
            } else {
                recyclerView.setVisibility(View.INVISIBLE);
                updateConnectionAlert(isUserSignedIn);
                updatePendingUploadAlert();
            }
            updateAppBarSignMenu();
        });
    }


    /**
     * Průběžně sleduje, zda existuje čekající upload na WiFi naplánovaný z lokálních záloh.
     */
    private void observePendingWifiUploadWork() {
        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData(PendingBackupUploadWorker.WORK_TAG_PENDING_UPLOAD_WIFI)
                .observe(getViewLifecycleOwner(), workInfos -> {
                    boolean previousPendingState = hasPendingWifiUpload;
                    hasPendingWifiUpload = false;
                    pendingWifiUploadFilesCount = 0;
                    boolean isRunningUpload = false;
                    if (workInfos != null) {
                        for (WorkInfo workInfo : workInfos) {
                            WorkInfo.State state = workInfo.getState();
                            if (state == WorkInfo.State.ENQUEUED
                                    || state == WorkInfo.State.BLOCKED
                                    || state == WorkInfo.State.RUNNING) {
                                hasPendingWifiUpload = true;
                            }
                            if (state == WorkInfo.State.RUNNING)
                                isRunningUpload = true;
                        }
                    }

                    if (hasPendingWifiUpload)
                        pendingWifiUploadFilesCount = loadPersistedPendingWifiUploadCount();

                    hadPendingWifiUpload = previousPendingState;
                    final boolean runningUploadNow = isRunningUpload;

                    if (!isAdded())
                        return;

                    requireActivity().runOnUiThread(() -> {
                        boolean isUserSignedIn = shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false);
                        updateConnectionAlert(isUserSignedIn);
                        updatePendingUploadAlert();
                        if (runningUploadNow)
                            showPendingUploadProgressDialog();
                        else
                            dismissPendingUploadProgressDialog();
                        if (hadPendingWifiUpload && !hasPendingWifiUpload && isUserSignedIn && internetAvailable)
                            loadGoogleFiles();
                    });
                });
    }


    /**
     * Aktualizuje centrální text upozornění podle síťového stavu a přihlášení.
     */
    private void updateConnectionAlert(boolean isUserSignedIn) {
        if (!internetAvailable) {
            if (NetworkUtil.shouldWarnWifiRequired(requireContext())) {
                tvAlertNoInternet.setText(getString(R.string.wifi_required_mobile_disabled_message));
            } else {
                tvAlertNoInternet.setText(getString(R.string.internet_is_not_available));
            }
            tvAlertNoInternet.setVisibility(View.VISIBLE);
            return;
        }

        if (!isUserSignedIn) {
            tvAlertNoInternet.setText(getString(R.string.google_drive_signed_out_warning));
            tvAlertNoInternet.setVisibility(View.VISIBLE);
            return;
        }

        tvAlertNoInternet.setVisibility(View.GONE);
    }


    /**
     * Zobrazí/skryje horní banner s informací o čekajícím uploadu na WiFi.
     */
    private void updatePendingUploadAlert() {
        if (tvPendingUploadAlert == null)
            return;

        if (!hasPendingWifiUpload) {
            tvPendingUploadAlert.setVisibility(View.GONE);
            return;
        }

        int count = Math.max(pendingWifiUploadFilesCount, 1);
        tvPendingUploadAlert.setText(getString(R.string.pending_upload_waiting_wifi_alert_count, count));
        tvPendingUploadAlert.setVisibility(View.VISIBLE);
    }


    /**
     * Načte počet čekajících souborů z perzistentních metadat pending uploadu.
     */
    private int loadPersistedPendingWifiUploadCount() {
        String fileNamesJson = new ShPBackup(requireContext()).get(ShPBackup.PENDING_WIFI_UPLOAD_FILE_NAMES, "");
        if (fileNamesJson.isEmpty())
            return 0;

        try {
            JSONArray jsonArray = new JSONArray(fileNamesJson);
            return jsonArray.length();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse pending WiFi upload count", e);
            return 0;
        }
    }


    /**
     * Bezpečně zobrazí dialog průběhu nahrávání čekajících záloh.
     */
    private void showPendingUploadProgressDialog() {
        if (!isAdded())
            return;

        if (pendingUploadProgressDialog != null && pendingUploadProgressDialog.isAdded())
            return;

        androidx.fragment.app.Fragment existing = requireActivity().getSupportFragmentManager()
                .findFragmentByTag(PendingBackupUploadProgressDialogFragment.TAG);
        if (existing instanceof PendingBackupUploadProgressDialogFragment) {
            pendingUploadProgressDialog = (PendingBackupUploadProgressDialogFragment) existing;
            return;
        }

        int count = Math.max(pendingWifiUploadFilesCount, 1);
        pendingUploadProgressDialog = PendingBackupUploadProgressDialogFragment.newInstance(
                getString(R.string.pending_upload_in_progress_count, count)
        );
        pendingUploadProgressDialog.show(requireActivity().getSupportFragmentManager(), PendingBackupUploadProgressDialogFragment.TAG);
    }


    /**
     * Bezpečně zavře dialog průběhu nahrávání čekajících záloh.
     */
    private void dismissPendingUploadProgressDialog() {
        if (pendingUploadProgressDialog == null)
            return;

        if (pendingUploadProgressDialog.isAdded())
            pendingUploadProgressDialog.dismissAllowingStateLoss();
        pendingUploadProgressDialog = null;
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

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).updateToolbarForSelection(selectedCount, isMultiSelectMode,
                    isMultiSelectMode ? this::cancelSelectionFromToolbar : null);
        }

        updateAppBarSignMenu();
    }


    private void cancelSelectionFromToolbar() {
        if (backupAdapter == null)
            return;

        suppressSelectionCanceledSnackbar = true;
        backupAdapter.cancelMultiSelect();
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


    /**
     * Zahájí workflow uložení vybraného souboru z Google Drive do lokální záložní složky.
     */
    private void handleSaveGoogleDriveFileRequest(@NonNull File driveFile) {
        ArrayList<File> filesToSave = new ArrayList<>();
        filesToSave.add(driveFile);
        startSaveFilesWorkflow(filesToSave);
    }


    /**
     * Pokračuje v ukládání souboru po ověření oprávnění ke složce a případně zobrazí konflikt názvu.
     */
    private void continuePendingSaveToLocal() {
        if (!isAdded() || pendingSaveAllFiles.isEmpty())
            return;

        Uri backupFolderUri = Uri.parse(new ShPBackup(requireContext()).get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        if (!Files.permissions(requireActivity(), backupFolderUri))
            return;

        DocumentFile backupFolder = DocumentFile.fromTreeUri(requireContext(), backupFolderUri);
        if (backupFolder == null || !backupFolder.canWrite()) {
            View root = getView();
            if (root != null)
                Snackbar.make(root, getString(R.string.no_folder), Snackbar.LENGTH_LONG).show();
            clearPendingSaveToLocal();
            return;
        }

        ArrayList<File> duplicateFiles = new ArrayList<>();
        ArrayList<File> nonDuplicateFiles = new ArrayList<>();
        Set<String> existingNames = new HashSet<>();
        for (DocumentFile localFile : backupFolder.listFiles()) {
            if (localFile != null && localFile.getName() != null)
                existingNames.add(localFile.getName());
        }

        for (File driveFile : pendingSaveAllFiles) {
            if (driveFile == null || driveFile.getName() == null)
                continue;
            if (existingNames.contains(driveFile.getName()))
                duplicateFiles.add(driveFile);
            else
                nonDuplicateFiles.add(driveFile);
        }

        if (!duplicateFiles.isEmpty()) {
            pendingSaveNonDuplicateFiles = new ArrayList<>(nonDuplicateFiles);
            YesNoDialogFragment.newInstance(
                    getString(R.string.save_selected_google_drive_files_exists_title),
                    FLAG_DIALOG_FRAGMENT_SAVE_TO_LOCAL,
                    getString(R.string.save_selected_google_drive_files_exists_message),
                    getString(R.string.upload_existing_files_overwrite),
                    getString(R.string.upload_existing_files_skip)
            ).show(requireActivity().getSupportFragmentManager(), FLAG_DIALOG_FRAGMENT_SAVE_TO_LOCAL);
            return;
        }

        startPendingSaveToLocal(new ArrayList<>(pendingSaveAllFiles), false);
    }


    /**
     * Spustí uložení do lokálního úložiště na background vlákně.
     */
    private void startPendingSaveToLocal(@NonNull List<File> filesToSave, boolean overwriteExisting) {
        if (googleDriveService == null || filesToSave.isEmpty())
            return;

        Uri backupFolderUri = Uri.parse(new ShPBackup(requireContext()).get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        ArrayList<File> filesSnapshot = new ArrayList<>(filesToSave);
        clearPendingSaveToLocal();
        if (googleDriveViewModel != null)
            googleDriveViewModel.setSaveInProgress(filesSnapshot.size());

        new Thread(() -> {
            int savedCount = 0;
            int totalCount = filesSnapshot.size();
            for (int i = 0; i < filesSnapshot.size(); i++) {
                File fileToSave = filesSnapshot.get(i);
                GoogleDriveService.ResultAction result = googleDriveService.saveFileToLocalStorage(backupFolderUri, fileToSave, overwriteExisting);
                if (result.result == GoogleDriveService.ResultAction.RESULT_OK)
                    savedCount++;
                if (googleDriveViewModel != null)
                    googleDriveViewModel.setSaveProgress(i + 1, savedCount, totalCount);
            }

            final int finalSavedCount = savedCount;
            final int finalTotalCount = totalCount;

            android.app.Activity activity = getActivity();
            if (activity == null)
                return;

            activity.runOnUiThread(() -> {
                if (googleDriveViewModel != null) {
                    googleDriveViewModel.setSaveFinished(finalSavedCount == finalTotalCount, finalSavedCount, finalTotalCount);
                } else {
                    View root = getView();
                    if (root != null) {
                        String message;
                        if (finalSavedCount == finalTotalCount) {
                            message = getString(R.string.saved_selected_google_drive_files);
                        } else if (finalSavedCount > 0) {
                            message = getString(R.string.partially_saved_selected_google_drive_files, finalSavedCount, finalTotalCount);
                        } else {
                            message = getString(R.string.not_saved_selected_google_drive_files);
                        }
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        }).start();
    }


    /**
     * Připraví workflow pro uložení souborů z Google Drive do lokální zálohy.
     */
    private void startSaveFilesWorkflow(@NonNull List<File> driveFiles) {
        if (!isAdded())
            return;

        if (!internetAvailable) {
            View root = getView();
            if (root != null)
                Snackbar.make(root, getString(R.string.internet_is_not_available), Snackbar.LENGTH_LONG).show();
            return;
        }

        if (googleDriveService == null) {
            View root = getView();
            if (root != null)
                Snackbar.make(root, getString(R.string.google_drive_service_init_failed), Snackbar.LENGTH_LONG).show();
            return;
        }

        pendingSaveAllFiles = new ArrayList<>(driveFiles);
        pendingSaveNonDuplicateFiles.clear();

        Uri backupFolderUri = Uri.parse(new ShPBackup(requireContext()).get(ShPBackup.FOLDER_BACKUP, RecoverData.DEF_URI));
        if (!Files.permissions(requireActivity(), backupFolderUri)) {
            Files.openTree(false, requireActivity(), resultTree);
            return;
        }

        continuePendingSaveToLocal();
    }


    /**
     * Vyčistí dočasná metadata pro lokální uložení souboru.
     */
    private void clearPendingSaveToLocal() {
        pendingSaveAllFiles.clear();
        pendingSaveNonDuplicateFiles.clear();
    }


    /**
     * Bezpečně otevře nebo vrátí existující dialog průběhu ukládání.
     */
    private GoogleDriveSaveProgressDialogFragment ensureSaveProgressDialog(int totalCount) {
        if (saveProgressDialog != null && saveProgressDialog.isAdded())
            return saveProgressDialog;

        androidx.fragment.app.Fragment existing = requireActivity().getSupportFragmentManager()
                .findFragmentByTag(GoogleDriveSaveProgressDialogFragment.TAG);
        if (existing instanceof GoogleDriveSaveProgressDialogFragment) {
            saveProgressDialog = (GoogleDriveSaveProgressDialogFragment) existing;
            return saveProgressDialog;
        }

        saveProgressDialog = GoogleDriveSaveProgressDialogFragment.newInstance(totalCount);
        saveProgressDialog.show(requireActivity().getSupportFragmentManager(), GoogleDriveSaveProgressDialogFragment.TAG);
        return saveProgressDialog;
    }


    /**
     * Bezpečně zavře dialog průběhu ukládání a uvolní referenci.
     */
    private void dismissSaveProgressDialog() {
        if (saveProgressDialog == null)
            return;

        if (saveProgressDialog.isAdded())
            saveProgressDialog.dismissAllowingStateLoss();
        saveProgressDialog = null;
    }


    /**
     * Převede seznam Drive souborů na serializovatelný stav pro obnovu po rotaci.
     */
    private ArrayList<String> serializeDriveFiles(List<File> files) {
        ArrayList<String> result = new ArrayList<>();
        if (files == null)
            return result;

        for (File file : files) {
            if (file == null || file.getId() == null || file.getName() == null)
                continue;
            long modifiedTime = file.getModifiedTime() != null
                    ? file.getModifiedTime().getValue()
                    : (file.getCreatedTime() != null ? file.getCreatedTime().getValue() : -1L);
            result.add(file.getId() + DRIVE_FILE_STATE_SEPARATOR + file.getName() + DRIVE_FILE_STATE_SEPARATOR + modifiedTime);
        }
        return result;
    }


    /**
     * Obnoví seznam Drive souborů ze serializovaného stavu po změně konfigurace.
     */
    private ArrayList<File> restoreDriveFilesFromState(@Nullable ArrayList<String> serializedFiles) {
        ArrayList<File> restoredFiles = new ArrayList<>();
        if (serializedFiles == null)
            return restoredFiles;

        for (String serializedFile : serializedFiles) {
            if (serializedFile == null)
                continue;
            String[] parts = serializedFile.split(DRIVE_FILE_STATE_SEPARATOR, 3);
            if (parts.length < 3)
                continue;

            File file = new File();
            file.setId(parts[0]);
            file.setName(parts[1]);
            try {
                long modifiedTime = Long.parseLong(parts[2]);
                if (modifiedTime > 0)
                    file.setModifiedTime(new com.google.api.client.util.DateTime(modifiedTime));
            } catch (NumberFormatException ignored) {
            }
            restoredFiles.add(file);
        }
        return restoredFiles;
    }

}