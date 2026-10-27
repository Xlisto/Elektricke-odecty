package cz.xlisto.elektrodroid.utils;


import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import cz.xlisto.elektrodroid.R;


/**
 * Pomocná třída pro kontrolu a instalaci aktualizací aplikace přes Google Play In-App Update API.
 */
public class AppUpdateHelper {

    private static final String TAG = "AppUpdateHelper";

    private final Activity activity;
    private AppUpdateManager appUpdateManager;
    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher;
    private InstallStateUpdatedListener installStateUpdatedListener;


    /**
     * Konstruktor pro AppUpdateHelper.
     *
     * @param activity       Aktivita
     * @param updateLauncher Launcher pro spuštění dialogu aktualizace
     */
    public AppUpdateHelper(@NonNull Activity activity,
                           @NonNull ActivityResultLauncher<IntentSenderRequest> updateLauncher) {
        this.activity = activity;
        this.updateLauncher = updateLauncher;
        try {
            this.appUpdateManager = AppUpdateManagerFactory.create(activity.getApplicationContext());
            setupInstallListener();
        } catch (Exception e) {
            Log.w(TAG, "Google Play App Update manager nelze vytvořit: " + e.getMessage());
        }
    }


    /**
     * Nastaví listener pro sledování stavu stahování na pozadí.
     */
    private void setupInstallListener() {
        if (appUpdateManager == null) return;
        try {
            installStateUpdatedListener = state -> {
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    showUpdateDownloadedSnackbar();
                }
            };
            appUpdateManager.registerListener(installStateUpdatedListener);
        } catch (Exception e) {
            Log.w(TAG, "Nelze zaregistrovat posluchače aktualizací: " + e.getMessage());
        }
    }


    /**
     * Odregistruje posluchače stavu instalace (volat v onDestroy).
     */
    public void unregisterListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null) {
            try {
                appUpdateManager.unregisterListener(installStateUpdatedListener);
            } catch (Exception e) {
                Log.w(TAG, "Chyba při odregistrování posluchače: " + e.getMessage());
            }
        }
    }


    /**
     * Zkontroluje, zda je k dispozici nová verze aplikace.
     *
     * @param isManualCheck {@code true} pokud kontrolu vyvolal uživatel tlačítkem (zobrazí informace/chyby)
     */
    public void checkForUpdate(boolean isManualCheck) {
        if (appUpdateManager == null) {
            if (isManualCheck) {
                Toast.makeText(activity, R.string.update_check_error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showUpdateDownloadedSnackbar();
                    return;
                }

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    startFlexibleUpdate(appUpdateInfo);
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startFlexibleUpdate(appUpdateInfo);
                } else if (isManualCheck) {
                    Toast.makeText(activity, R.string.app_is_up_to_date, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Služba Google Play App Update není k dispozici: " + e.getMessage());
                if (isManualCheck) {
                    Toast.makeText(activity, R.string.update_check_error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Chyba při spuštění kontroly aktualizací: " + e.getMessage());
            if (isManualCheck) {
                Toast.makeText(activity, R.string.update_check_error, Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Kontroluje v onResume, zda nebyl stažen update během běhu aplikace na pozadí.
     */
    public void checkResumeUpdate() {
        if (appUpdateManager == null) return;
        try {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showUpdateDownloadedSnackbar();
                }
            }).addOnFailureListener(e -> Log.w(TAG, "CheckResumeUpdate nebylo možné dokončit: " + e.getMessage()));
        } catch (Exception e) {
            Log.w(TAG, "Chyba v checkResumeUpdate: " + e.getMessage());
        }
    }


    /**
     * Spustí kaskádu stažení aktualizace na pozadí (Flexible Update).
     */
    private void startFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        if (appUpdateManager == null) return;
        try {
            AppUpdateOptions options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build();
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateLauncher, options);
        } catch (Exception e) {
            Log.w(TAG, "Chyba při spuštění aktualizace: " + e.getMessage());
        }
    }


    /**
     * Zobrazí Snackbar informující uživatele, že nová verze je stažena a připravena k instalaci.
     */
    private void showUpdateDownloadedSnackbar() {
        if (activity.isFinishing()) {
            return;
        }
        View rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        Snackbar snackbar = Snackbar.make(
                rootView,
                R.string.update_downloaded_message,
                Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction(R.string.update_restart_button, v -> {
            if (appUpdateManager != null) {
                try {
                    appUpdateManager.completeUpdate();
                } catch (Exception e) {
                    Log.w(TAG, "Chyba při dokončení aktualizace: " + e.getMessage());
                }
            }
        });
        snackbar.show();
    }

}