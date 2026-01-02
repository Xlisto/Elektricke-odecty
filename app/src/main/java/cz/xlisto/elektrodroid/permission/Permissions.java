package cz.xlisto.elektrodroid.permission;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;


/**
 * Utility třída pro kontrolu a správu oprávnění k úložišti a práci se Storage Access Framework (SAF).
 * <p>
 * Poskytuje metody pro ověření READ/WRITE oprávnění nad zadaným `Uri`, otevření systému pro výběr
 * adresářového stromu a získání persistovaného přístupu, a pro vyžádání runtime oprávnění na
 * starších verzích Androidu.
 * <p>
 * Třída je navržena jako jednoduchý singleton pro sdílené použití v aplikaci.
 */

public class Permissions {

    private static final String TAG = "Permissions";

    public static final int REQUEST_WRITE_STORAGE = 0;//může být jakékoliv číslo typu int, slouží pro oddělení jednotlivých oprávnění, klidně můžu používat rovnou čísla int, ale takto je to přehlednější

    private static Permissions instance = null;


    /**
     * Privátní konstruktor třídy Permissions.
     * <p>
     * Zabraňuje přímé instanciaci třídy z vnějšího kódu a vynucuje použití
     * `getInstance()` pro získání jediné sdílené instance (singleton).
     */
    private Permissions() {
    }


    /**
     * Vrátí sdílenou instanci třídy Permissions (singleton).
     * <p>
     * Pokud instance ještě nebyla vytvořena, vytvoří ji při prvním volání.
     *
     * @return jediná sdílená instance Permissions
     */
    public static Permissions getInstance() {
        if (instance == null)
            instance = new Permissions();
        return instance;
    }


    /**
     * Zkontroluje, zda má aplikace oprávnění pro čtení nad zadaným `Uri`.
     *
     * @param activity aktuální `Activity` použitá pro kontrolu oprávnění a UID procesu
     * @param uri      `Uri` adresářového stromu nebo dokumentu, pro který se kontroluje přístup
     * @return `true` pokud je nad daným `uri` přiděleno oprávnění READ; jinak `false`
     */
    public boolean isPermissionStorageRead(final Activity activity, Uri uri) {
        return activity.checkUriPermission(uri, android.os.Process.myPid(), activity.getApplicationInfo().uid, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Zkontroluje, zda má aplikace oprávnění pro zápis nad zadaným `Uri`.
     *
     * @param activity aktuální `Activity` použitá pro kontrolu oprávnění a UID procesu
     * @param uri      `Uri` adresářového stromu nebo dokumentu, pro který se kontroluje přístup
     * @return `true` pokud je nad daným `uri` přiděleno oprávnění WRITE; jinak `false`
     */
    public boolean isPermissionStorageWrite(final Activity activity, Uri uri) {
        return activity.checkUriPermission(uri, android.os.Process.myPid(), activity.getApplicationInfo().uid, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Zkontroluje současně oprávnění pro čtení i zápis nad zadaným `Uri`.
     *
     * @param activity aktuální `Activity` používaná pro kontrolu oprávnění a UID procesu
     * @param uri      `Uri` adresářového stromu nebo dokumentu, pro který se kontroluje přístup
     * @return `true` pokud má aplikace jak READ, tak WRITE oprávnění k zadanému `uri`; jinak `false`
     */
    public boolean isPermissionStorage(final Activity activity, Uri uri) {
        return isPermissionStorageRead(activity, uri) && isPermissionStorageWrite(activity, uri);
    }

}

