package cz.xlisto.odecty.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;

/**
 * Xlisto 07.03.2023 15:12
 */

public class Permissions {
    private static final String TAG = "Permissions";

    private static final int REQUEST_WRITE_STORAGE = 0;//může být jakékoliv číslo typu int, slouží pro oddělení jednotlivých oprávnění, klidně můžu používat rovnou čísla int, ale takto je to přehlednější


    private Permissions() {
    }


    private static Permissions instance = null;


    public static Permissions getInstance() {
        if (instance == null)
            instance = new Permissions();
        return instance;
    }


    /**
     * Dotaz na oprávnění na čtení složky
     * @param activity - aktuální aktivita
     * @param uri - uri složky
     * @return - true má oprávnění, false nemá oprávnění
     */
    public boolean isPermissionStorageRead(final Activity activity, Uri uri) {
        return activity.checkUriPermission(uri, android.os.Process.myPid(), activity.getApplicationInfo().uid, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Dotaz na oprávnění na zápis složky
     * @param activity - aktuální aktivita
     * @param uri - uri složky
     * @return - true má oprávnění, false nemá oprávnění
     */
    public boolean isPermissionStorageWrite(final Activity activity, Uri uri) {
        return activity.checkUriPermission(uri, android.os.Process.myPid(), activity.getApplicationInfo().uid, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Dotaz na oprávnění na zápis a čtení složky současně
     * @param activity - aktuální aktivita
     * @param uri - uri složky
     * @return - true má oprávnění, false nemá oprávnění
     */
    public boolean isPermissionStorage(final Activity activity, Uri uri) {
        return isPermissionStorageRead(activity,uri) && isPermissionStorageWrite(activity,uri);
    }


    /**
     * Otevře aktivitu na výběr a povolení složky
     *
     * @param showRoot     true zobrazí root složku, false zobrazí poslední vybranou složku
     */
    public void openTree(boolean showRoot, Fragment fragment, int request_code) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:");

        if (showRoot) intent.putExtra("android.provider.extra.INITIAL_URI", uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        fragment.startActivityForResult(intent, request_code);
    }


    public boolean permissions(final Activity activity, final Fragment fragment, View view, Uri treeUri, final int request_code) {
        //od API 23
        //kontrola nebezpečného oprávnění: -1 nemá oprávnění; 0 má oprávnění
        int permissionCheck1 = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //přidělení oprávnění pro čtení do API 29
        if (Build.VERSION.SDK_INT < 30) {
            if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
                //když není povolený zápis, zobrazí se snackbar s tlačítkem, které odkazuje na nastavení aplikace s žádostí o povolení oprávnění
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, activity.getResources().getString(R.string.allow_write_to_storage), Snackbar.LENGTH_LONG).setAction(activity.getResources().getString(R.string.allow),
                            v -> {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:" + activity.getApplication().getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                activity.startActivity(intent);
                            }).show();
                } else {
                    //Zobrazení dialogového okna pro povolení oprávnění
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                    //REQUEST_WRITE_STORAGE představuje statickou proměnnou typu int, ke každému oprávnění si přiřadím jeden int, když jich bude víc
                }

            } else {//provádí se když má oprávnění
                return true;
            }
        } else {
            if (activity.checkUriPermission(treeUri, android.os.Process.myPid(), activity.getApplicationInfo().uid, Intent.FLAG_GRANT_READ_URI_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(view, activity.getResources().getString(R.string.add_permissions), Snackbar.LENGTH_LONG)
                        .setAction(activity.getResources().getString(R.string.select), v -> openTree(true, fragment, request_code))
                        .show();
                return false;
            } else
                return true;
        }
        return false;
    }

}

