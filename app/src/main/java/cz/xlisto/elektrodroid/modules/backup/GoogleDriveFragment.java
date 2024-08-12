package cz.xlisto.elektrodroid.modules.backup;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;


/**
 * Fragment pro správu přihlášení a odhlášení uživatele pomocí Google Drive.
 * Implementuje CredentialListener pro zpracování událostí přihlášení a odhlášení.
 */
public class GoogleDriveFragment extends Fragment implements CredentialHelper.CredentialListener {

    private Button btnGoogleSign;
    private ShPGoogleDrive shPGoogleDrive;


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

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_drive, container, false);
        CredentialHelper credentialHelper = new CredentialHelper(requireContext());
        credentialHelper.setCredentialListener(this);

        shPGoogleDrive = new ShPGoogleDrive(requireContext());
        btnGoogleSign = view.findViewById(R.id.btnGoogleSign);

        btnGoogleSign.setOnClickListener(v -> {
            if (shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false))
                credentialHelper.signOutWithCredentialManager();
            else
                credentialHelper.signInWithCredentialManager();
        });

        setButtonState();
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    /**
     * Nastaví stav tlačítka podle stavu přihlášení uživatele.
     */
    private void setButtonState() {
        if (shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false))
            btnGoogleSign.setText(R.string.sign_out);
        else
            btnGoogleSign.setText(R.string.sign_in);
    }


    @Override
    public void onSignInSuccess() {
        Log.w("GoogleDriveFragment", "onSignInSuccess: ");
        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, true);
        setButtonState();
    }


    @Override
    public void onSignOutSuccess() {
        Log.w("GoogleDriveFragment", "onSignOutSuccess: ");
        shPGoogleDrive.set(ShPGoogleDrive.USER_SIGNED, false);
        setButtonState();
    }

}