package cz.xlisto.elektrodroid.modules.backup;


import android.content.Context;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.R;


/**
 * Třída CredentialHelper poskytuje metody pro přihlášení a odhlášení uživatele pomocí CredentialManager.
 * Obsahuje také rozhraní CredentialListener pro zpracování událostí přihlášení a odhlášení.
 */
public class CredentialHelper {

    private static final String TAG = "CredentialHelper";
    private final CredentialManager credentialManager;
    private final Context context;
    private final GetCredentialRequest credentialRequest;
    private CredentialListener credentialListener;


    /**
     * Konstruktor pro CredentialHelper.
     * Inicializuje CredentialManager a nastaví GetCredentialRequest.
     *
     * @param context Aplikační kontext.
     */
    public CredentialHelper(Context context) {
        this.context = context;

        credentialManager = CredentialManager.create(context);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build();

        credentialRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

    }


    /**
     * Nastaví CredentialListener pro zpracování událostí přihlášení a odhlášení.
     *
     * @param listener CredentialListener, který má být nastaven.
     */
    public void setCredentialListener(CredentialListener listener) {
        this.credentialListener = listener;
    }


    /**
     * Zahájí proces přihlášení pomocí CredentialManager.
     * Při úspěchu volá metodu onSignInSuccess listeneru.
     */
    public void signInWithCredentialManager() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                CancellationSignal cancellationSignal = new CancellationSignal();
                credentialManager.getCredentialAsync(
                        context,
                        credentialRequest,
                        cancellationSignal,
                        executor,
                        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(GetCredentialResponse getCredentialResponse) {
                                if (credentialListener != null) {
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            credentialListener.onSignInSuccess());
                                }
                            }


                            @Override
                            public void onError(@NonNull GetCredentialException e) {
                                Log.e(TAG, "onError: " + e);
                            }

                        }
                );
            } catch (Exception e) {
                Log.e(TAG, "signInWithCredentialManager: ", e);
            }
        });
    }


    /**
     * Zahájí proces odhlášení pomocí CredentialManager.
     * Při úspěchu volá metodu onSignOutSuccess listeneru.
     */
    public void signOutWithCredentialManager() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                CancellationSignal cancellationSignal = new CancellationSignal();
                credentialManager.clearCredentialStateAsync(
                        new ClearCredentialStateRequest(),
                        cancellationSignal,
                        executor,
                        new CredentialManagerCallback<Void, ClearCredentialException>() {
                            @Override
                            public void onResult(Void result) {
                                if (credentialListener != null) {
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            credentialListener.onSignOutSuccess());
                                }
                            }


                            @Override
                            public void onError(@NonNull ClearCredentialException e) {
                                Log.e(TAG, "onError: Sign out failed", e);
                            }
                        }
                );
            } catch (Exception e) {
                Log.e(TAG, "signOutWithCredentialManager: ", e);
            }
        });
    }


    /**
     * Rozhraní pro zpracování událostí přihlášení a odhlášení.
     */
    public interface CredentialListener {

        void onSignInSuccess();

        void onSignOutSuccess();

    }

}