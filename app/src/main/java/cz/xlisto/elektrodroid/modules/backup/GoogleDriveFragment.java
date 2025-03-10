package cz.xlisto.elektrodroid.modules.backup;


import android.accounts.Account;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.api.services.drive.model.File;

import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.dialogs.FolderDialog;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;
import cz.xlisto.elektrodroid.utils.NetworkCallbackImpl;
import cz.xlisto.elektrodroid.utils.NetworkUtil;


/**
 * Fragment pro správu přihlášení a odhlášení uživatele pomocí Google Drive.
 * Implementuje CredentialListener pro zpracování událostí přihlášení a odhlášení
 * a OnFilesLoadedListener pro zpracování načtených souborů z Google Drive.
 */
public class GoogleDriveFragment extends Fragment implements CredentialHelper.CredentialListener, GoogleDriveService.OnFilesLoadedListener, NetworkCallbackImpl.NetworkChangeListener {

    private static final String TAG = "GoogleDriveFragment";
    private static final String FLAG_DIALOG_NEW_FOLDER = "NewFolderDialog";
    private static final String ROOT = "root";
    private Button btnGoogleSign;
    private Button btnSetDefaultFolder;
    private Button btnCreateFolder;
    private TextView tvAlertNoInternet;
    private ShPGoogleDrive shPGoogleDrive;
    private GoogleDriveService googleDriveService;
    private RecyclerView recyclerView;
    private LinearLayout lnProgressBar;
    private BackupAdapter backupAdapter;
    private String parentFolderId = ROOT;
    private ConnectivityManager connectivityManager;
    private NetworkCallbackImpl networkCallback;
    private boolean internetAvailable;
    //handler pro načtení obsahu složky
    private final Handler handlerResultOpenFolder = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            String folderId = (String) msg.obj;
            parentFolderId = folderId;
            btnSetDefaultFolder.setEnabled(!folderId.equals(shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, ROOT)));
            showLnProgressBar(true);
            btnCreateFolder.setEnabled(false);
            googleDriveService = new GoogleDriveService(requireContext(), shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, ""), folderId);
            googleDriveService.setOnFilesLoadedListener(GoogleDriveFragment.this);
        }
    };
    //handler pro zobrazení výsledku obnovení databáze
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        internetAvailable = NetworkUtil.isInternetAvailable(requireContext());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_drive, container, false);
        recyclerView = view.findViewById(R.id.rvGoogleDriveFiles);
        lnProgressBar = view.findViewById(R.id.lnProgressBar);
        tvAlertNoInternet = view.findViewById(R.id.tvAlertNoInternet);

        CredentialHelper credentialHelper = new CredentialHelper(requireContext());
        credentialHelper.setCredentialListener(this);

        shPGoogleDrive = new ShPGoogleDrive(requireContext());
        btnGoogleSign = view.findViewById(R.id.btnGoogleSign);
        btnSetDefaultFolder = view.findViewById(R.id.btnSetDefaultFolder);
        btnCreateFolder = view.findViewById(R.id.btnCreateFolder);

        btnCreateFolder.setEnabled(false);

        btnGoogleSign.setOnClickListener(v -> {
            if (shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false)) {
                credentialHelper.signOutWithCredentialManager();
            } else {
                credentialHelper.signInWithCredentialManager();
            }
        });

        btnSetDefaultFolder.setOnClickListener(v -> {
            shPGoogleDrive.set(ShPGoogleDrive.DEFAULT_FOLDER_ID, parentFolderId);
            btnSetDefaultFolder.setEnabled(false);
        });

        btnCreateFolder.setOnClickListener(v ->
                FolderDialog.newInstance(FLAG_DIALOG_NEW_FOLDER).show(requireActivity().getSupportFragmentManager(), FolderDialog.TAG)
        );

        connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new NetworkCallbackImpl(this);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        toggleButtonsAndRecyclerViewVisibility();

        loadGoogleFiles();

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //posluchač výsledku dialogového okna pro obnovení databáze
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_BACKUP, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                showLnProgressBar(true);
                backupAdapter.downloadAndRecoveryFile();
                Log.w("GoogleDriveFragment", "onCreate: " + "obnovit databázi");
            }

        });
        //posluchač výsledku dialogového okna pro smazání záložního souboru
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_FRAGMENT_DELETE, this, (requestKey, result) -> {
            if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                backupAdapter.deleteFile();
                Log.w("GoogleDriveFragment", "onCreate: " + "smazat soubor");
            }
        });
        //posluchač výsledku dialogového okna pro vytvoření nové složky
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DIALOG_NEW_FOLDER, this, (requestKey, result) -> {
            if (result.getBoolean(FolderDialog.RESULT)) {
                String folderName = result.getString(FolderDialog.FLAG_FOLDER_NAME);
                new Thread(() -> {
                    boolean b = googleDriveService.createFolder(folderName);
                    Message message = new Message();
                    if (b) {
                        message.obj = parentFolderId;
                        handlerResultOpenFolder.sendMessage(message);
                    }
                }).start();

                Log.w("GoogleDriveFragment", "onCreate: " + "vytvořit složku: " + folderName);
            }
        });
        //posluchač výsledku dialogového okna pro přejmenování složky
        requireActivity().getSupportFragmentManager().setFragmentResultListener(BackupAdapter.FLAG_DIALOG_RENAME_FOLDER, this, (requestKey, result) -> {
            if (result.getBoolean(FolderDialog.RESULT)) {
                String folderName = result.getString(FolderDialog.FLAG_FOLDER_NAME);
                Log.w("GoogleDriveFragment", "onCreate: " + "přejmenovat složku: " + folderName + ", " + backupAdapter.getSelectedFileId());
                new Thread(() -> {
                    boolean b = googleDriveService.renameFolder(backupAdapter.getSelectedFileId(), folderName);
                    if (b) {
                        requireActivity().runOnUiThread(() -> backupAdapter.updateFile(backupAdapter.getSelectedFileId(), folderName));
                    }
                }).start();

            }
        });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }


    /**
     * Metoda volaná při úspěšném přihlášení uživatele.
     * Nastaví stav přihlášení uživatele na true, uloží uživatelské jméno a aktualizuje stav tlačítka.
     * Načte soubory z Google Drive.
     *
     * @param account Účet, který byl úspěšně přihlášen.
     */
    @Override
    public void onSignInSuccess(Account account) {
        Log.w("GoogleDriveFragment", "onSignInSuccess: " + account.name);
        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, true);
        shPGoogleDrive.set(ShPGoogleDrive.USER_NAME, account.name);
        toggleButtonsAndRecyclerViewVisibility();
        loadGoogleFiles();
    }


    /**
     * Metoda volaná při úspěšném odhlášení uživatele.
     * Nastaví stav přihlášení uživatele na false, vymaže uživatelské jméno a nastaví výchozí ID složky na "root".
     * Aktualizuje stav tlačítka a načte prázdný seznam souborů.
     */
    @Override
    public void onSignOutSuccess() {
        Log.w("GoogleDriveFragment", "onSignOutSuccess: ");
        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, false);
        shPGoogleDrive.set(ShPGoogleDrive.USER_NAME, "");
        shPGoogleDrive.set(ShPGoogleDrive.DEFAULT_FOLDER_ID, ROOT);
        toggleButtonsAndRecyclerViewVisibility();
        List<File> fileList = List.of();
        onFilesLoaded(fileList);
    }


    /**
     * Načte soubory z Google Drive, pokud je uživatel přihlášen.
     * Zobrazí ProgressBar a inicializuje službu GoogleDriveService s aktuálním ID složky.
     */
    public void loadGoogleFiles() {
        Log.w("GoogleDriveFragment", "loadGoogleFiles: internetAvailable: " + internetAvailable);
        if (internetAvailable) {
            if (!shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, "").isEmpty()) {
                if (backupAdapter != null)
                    backupAdapter.clearData();
                showLnProgressBar(true);
                btnCreateFolder.setEnabled(false);
                Log.w("GoogleDriveFragment", "onCreate: " + shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, ""));
                String folderId = shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, ROOT);
                parentFolderId = folderId;
                btnSetDefaultFolder.setEnabled(!folderId.equals(shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, ROOT)));
                googleDriveService = new GoogleDriveService(requireContext(), shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, ""), folderId);
                googleDriveService.setOnFilesLoadedListener(this);
            }
        }
    }


    /**
     * Zobrazí nebo skryje LinearLayout s ProgressBar
     */
    private void showLnProgressBar(boolean b) {
        if (b) {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(requireActivity(), android.R.anim.fade_in));
            lnProgressBar.setVisibility(View.VISIBLE);
        } else {
            lnProgressBar.setAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
            lnProgressBar.setVisibility(View.GONE);
        }
    }


    /**
     * Metoda volaná při načtení souborů z Google Drive.
     * Aktualizuje uživatelské rozhraní s načtenými soubory a nastaví adapter pro RecyclerView.
     *
     * @param files Seznam souborů načtených z Google Drive.
     */
    @Override
    public void onFilesLoaded(List<File> files) {
        Log.w("GoogleDriveFragment", "onFilesLoaded: " + files.size());
        requireActivity().runOnUiThread(() -> {
            showLnProgressBar(false);
            btnCreateFolder.setEnabled(true);
            backupAdapter = new BackupAdapter(requireActivity(), files, recyclerView, handlerResultRecoveryDatabase, handlerResultOpenFolder, googleDriveService);
            recyclerView.setAdapter(backupAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

            for (File file : files) {
                Log.w("GoogleDriveFragment", "onFilesLoaded: " + file.getName() + ", " + file.getId() + ", " + file.getParents());
            }
        });
    }


    @Override
    public void onNetworkAvailable() {
        Log.w("GoogleDriveFragment", "onNetworkAvailable: ");
        internetAvailable = true;
        toggleButtonsAndRecyclerViewVisibility();
    }


    @Override
    public void onNetworkLost() {
        Log.w("GoogleDriveFragment", "onNetworkLost: ");
        internetAvailable = false;
        toggleButtonsAndRecyclerViewVisibility();
    }


    /**
     * Nastaví viditelnost tlačítek a RecyclerView na základě dostupnosti internetu a stavu přihlášení uživatele.
     */
    private void toggleButtonsAndRecyclerViewVisibility() {
        requireActivity().runOnUiThread(() -> {
            boolean isUserSignedIn = shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false);
            btnGoogleSign.setText(isUserSignedIn ? R.string.sign_out : R.string.sign_in);

            if (internetAvailable) {
                recyclerView.setVisibility(isUserSignedIn ? View.VISIBLE : View.INVISIBLE);
                btnCreateFolder.setVisibility(isUserSignedIn ? View.VISIBLE : View.GONE);
                btnSetDefaultFolder.setVisibility(isUserSignedIn ? View.VISIBLE : View.GONE);
                btnGoogleSign.setVisibility(View.VISIBLE);
                tvAlertNoInternet.setVisibility(View.GONE);
                if (isUserSignedIn) {
                    loadGoogleFiles();
                }
            } else {
                recyclerView.setVisibility(View.INVISIBLE);
                btnCreateFolder.setVisibility(View.GONE);
                btnSetDefaultFolder.setVisibility(View.GONE);
                btnGoogleSign.setVisibility(View.GONE);
                tvAlertNoInternet.setVisibility(View.VISIBLE);
            }
        });
    }

}