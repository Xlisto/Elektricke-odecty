package cz.xlisto.elektrodroid.modules.backup;


import static androidx.credentials.GetCredentialRequest.Builder;

import android.accounts.Account;
import android.content.Context;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.R;


/**
 * Třída CredentialHelper poskytuje metody pro přihlášení a odhlášení uživatele pomocí CredentialManager.
 * Tato třída zapouzdřuje logiku pro správu přihlašovacích údajů pomocí Android Credential Manager API.
 * Umožňuje:
 * <ul>
 *     <li>Přihlášení uživatele pomocí Google ID nebo přihlášení přes Google</li>
 *     <li>Automatické zpracování fallback scénářů, když nejsou dostupné přihlašovací údaje</li>
 *     <li>Odhlášení uživatele a vymazání uloženého stavu přihlašovacích údajů</li>
 *     <li>Asynchronní zpracování všech operací pomocí ExecutorService</li>
 * </ul>
 *
 * Třída vyžaduje posluchače {@link CredentialListener} pro zpracování výsledků operací.
 *
 * @see CredentialListener
 * @see CredentialManager
 * @see androidx.credentials.GetCredentialRequest
 */
public class CredentialHelper {

    private static final String TAG = "CredentialHelper";
    private final CredentialManager credentialManager;
    private final Context context;
    private final GetCredentialRequest credentialRequest;
    private final GetCredentialRequest signInWithGoogleRequest;
    private CredentialListener credentialListener;


    /**
     * Konstruktor pro CredentialHelper.
     * <p>
     * Inicializuje CredentialManager a připravuje požadavky na získání přihlašovacích údajů.
     * Konfiguruje dvě možnosti přihlášení:
     * <ul>
     *     <li>Google ID - pro automatické přihlášení bez interakce uživatele</li>
     *     <li>Sign In with Google - pro interaktivní výběr účtu</li>
     * </ul>
     *
     * @param context Aplikační kontext, který se používá pro inicializaci CredentialManager
     *                a pro přístup ke zdrojům aplikace (client ID).
     */
    public CredentialHelper(Context context) {
        this.context = context;

        credentialManager = CredentialManager.create(context);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build();

        GetSignInWithGoogleOption signInWithGoogleOption = new GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id))
                .build();

        credentialRequest = new Builder()
                .addCredentialOption(googleIdOption)
                .build();

        signInWithGoogleRequest = new Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build();

    }


    /**
     * Nastaví CredentialListener pro zpracování událostí přihlášení a odhlášení.
     * <p>
     * Posluchač bude volán v hlavním vlákně (Main Thread) v případě úspěchu nebo chyby
     * operací přihlášení a odhlášení.
     *
     * @param listener {@link CredentialListener}, který bude obsluhovat callback события.
     *                 Pokud je nastaven na null, budou callback ignorovány.
     */
    public void setCredentialListener(CredentialListener listener) {
        this.credentialListener = listener;
    }


    /**
     * Zahájí proces přihlášení pomocí CredentialManager.
     * <p>
     * Metoda provede:
     * <ol>
     *     <li>Vytvoření nového vlákna (ExecutorService) pro asynchronní zpracování</li>
     *     <li>Odeslání požadavku na přihlašovací údaje</li>
     *     <li>V případě úspěchu volá {@link CredentialListener#onSignInSuccess(Account)}</li>
     *     <li>V případě neúspěchu volá {@link CredentialListener#onSignInError(boolean, String)}</li>
     * </ol>
     *
     * Metoda se spouští asynchronně, takže se vrátí okamžitě bez čekání na výsledek.
     * Výsledek je vrácen prostřednictvím nastavené {@link CredentialListener}.
     */
    public void signInWithCredentialManager() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                requestCredential(executor, credentialRequest, false);
            } catch (Exception e) {
                Log.e(TAG, "signInWithCredentialManager: ", e);
            }
        });
    }


    /**
     * Vnitřní metoda pro asynchronní žádost o přihlašovací údaje.
     * <p>
     * Tato metoda:
     * <ul>
     *     <li>Odesílá požadavek na přihlašovací údaje přes CredentialManager</li>
     *     <li>Zpracovává úspěšné přihlášení a extrahuje email účtu</li>
     *     <li>Implementuje fallback logiku pro případ, kdy nejsou dostupné uložené přihlašovací údaje</li>
     *     <li>Volá odpovídající callback metody listeneru v hlavním vlákně</li>
     * </ul>
     *
     * Fallback mechanismus se spouští pouze jednou, a pokud selže, vrátí chybou.
     *
     * @param executor {@link ExecutorService} pro spouštění asynchronní operace.
     * @param request {@link GetCredentialRequest} obsahující možnosti přihlášení.
     * @param fallbackAttempt {@code true} pokud se jedná o fallback pokus (Sign In with Google),
     *                        {@code false} pro iniciální pokus (Google ID).
     */
    private void requestCredential(ExecutorService executor, GetCredentialRequest request, boolean fallbackAttempt) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        credentialManager.getCredentialAsync(
                context,
                request,
                cancellationSignal,
                executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse getCredentialResponse) {
                        if (credentialListener != null) {
                            Credential credential = getCredentialResponse.getCredential();
                            String accountEmail = credential.getData().getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID");
                            if (accountEmail != null) {
                                Account account = new Account(accountEmail, "com.google");
                                new Handler(Looper.getMainLooper()).post(() ->
                                        credentialListener.onSignInSuccess(account));
                            } else {
                                Log.e("TokenError", "Account ID is missing");
                                new Handler(Looper.getMainLooper()).post(() ->
                                        credentialListener.onSignInError(false, "Account ID is missing"));
                            }
                        }
                    }


                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "onError: " + e);
                        if (e instanceof NoCredentialException && !fallbackAttempt) {
                            // Fallback otevře interaktivní výběr Google účtu.
                            requestCredential(executor, signInWithGoogleRequest, true);
                            return;
                        }
                        if (credentialListener != null) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    credentialListener.onSignInError(e instanceof NoCredentialException, e.getMessage()));
                        }
                    }
                }
        );
    }


    /**
     * Zahájí proces odhlášení pomocí CredentialManager.
     * <p>
     * Metoda provede:
     * <ol>
     *     <li>Vytvoření nového vlákna (ExecutorService) pro asynchronní zpracování</li>
     *     <li>Vymazání stavu přihlašovacích údajů z CredentialManager</li>
     *     <li>V případě úspěchu volá {@link CredentialListener#onSignOutSuccess()}</li>
     *     <li>V případě neúspěchu zaznamenává chybu (bez volání listeneru)</li>
     * </ol>
     *
     * Metoda se spouští asynchronně a chyby odhlášení jsou pouze zaznamenávány.
     * Úspěšné odhlášení je vráceno prostřednictvím nastavené {@link CredentialListener}.
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
                        new CredentialManagerCallback<>() {
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
     * <p>
     * Implementátor tohoto rozhraní obdrží callback volání pro:
     * <ul>
     *     <li>Úspěšné přihlášení s účtem uživatele</li>
     *     <li>Chyby během procesu přihlášení</li>
     *     <li>Úspěšné odhlášení</li>
     * </ul>
     *
     * Všechny callback metody jsou volány v hlavním vlákně (Main Thread).
     */
    public interface CredentialListener {

        /**
         * Volán při úspěšném přihlášení uživatele.
         *
         * @param account {@link Account} obsahující informace o přihlášeném uživateli.
         *                Typ účtu je vždy "com.google".
         */
        void onSignInSuccess(Account account);

        /**
         * Volán v případě chyby během procesu přihlášení.
         *
         * @param noCredentials {@code true} pokud chyba nastala proto, že nejsou dostupné
         *                      žádné přihlašovací údaje ({@link NoCredentialException}),
         *                      {@code false} pro ostatní typy chyb.
         * @param errorMessage Popis chyby, která nastala. Může obsahovat informace
         *                     o důvodu selhání operace.
         */
        void onSignInError(boolean noCredentials, String errorMessage);

         /**
         * Volán při úspěšném odhlášení uživatele.
         * Po zavolání této metody jsou všechny uložené přihlašovací údaje vymazány
         * z CredentialManager.
         */
        void onSignOutSuccess();

    }

}

