package cz.xlisto.elektrodroid.utils;


import static cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG;

import android.content.Context;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;


/**
 * Centrální utility třída pro správu a synchronizaci aktuálně vybraného odběrného místa.
 *
 * <p>Tato třída koordinuje dual-layer persistenci odběrného místa:
 * <ul>
 *   <li><strong>SharedPreferences</strong> - pro rychlý přístup v runtime (výkon)</li>
 *   <li><strong>Databází (tabulka nastaveni)</strong> - pro persistenci přes backup/restore (data integrity)</li>
 * </ul>
 * </p>
 *
 * <p>Klíčové metody:</p>
 * <ul>
 *   <li>{@link #load(Context)} - Načte aktuálně vybrané místo z SharedPreferences, s fallback na databázi</li>
 *   <li>{@link #setCurrentSelection(Context, long)} - Uloží vybrané místo do obou storage vrstev</li>
 *   <li>{@link #applyCurrentFromSettings(Context)} - Obnoví stav z databáze (po importu zálohy)</li>
 * </ul>
 *
 * <p>Typické scénáře použití:</p>
 * <pre>
 *     // 1. Uživatel vybere místo ve fragmentu
 *     SubscriptionPoint.setCurrentSelection(context, selectedId);
 *
 *     // 2. Po importu zálohy se obnoví stav
 *     if (!SubscriptionPoint.applyCurrentFromSettings(context)) {
 *         // Žádné místo nebolo uloženo - zobrazit dialog
 *         showSelectionDialog();
 *     }
 *
 *     // 3. App startup: načte poslední vybrané místo
 *     SubscriptionPointModel current = SubscriptionPoint.load(context);
 * </pre>
 *
 * @author Created for subscription point restoration feature
 * @see cz.xlisto.elektrodroid.databaze.DataSettingsSource
 * @see cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint
 */
public class SubscriptionPoint {

    /**
     * Načte aktuálně vybrané odběrné místo.
     *
     * <p>Priorita načítání:</p>
     * <ol>
     *   <li>SharedPreferences (nejrychlejší)</li>
     *   <li>Databází settings table (fallback, např. po obnovení)</li>
     *   <li>null (pokud nic není dostupné)</li>
     * </ol>
     *
     * @param context Kontext aplikace
     * @return {@link cz.xlisto.elektrodroid.models.SubscriptionPointModel SubscriptionPointModel} - aktuálně vybrané místo,
     *         nebo {@code null} pokud není dostupné
     *
     * @see #applyCurrentFromSettings(Context)
     * @see #loadById(Context, long)
     */
    static public SubscriptionPointModel load(Context context) {
        long id;
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
        id = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);

        SubscriptionPointModel subscriptionPoint = loadById(context, id);
        if (subscriptionPoint != null)
            return subscriptionPoint;

        if (applyCurrentFromSettings(context)) {
            long selectedId = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);
            return loadById(context, selectedId);
        }

        return null;
    }


    /**
     * Načte počet odběrných míst v databázi
     *
     * @param context Kontext aplikace
     * @return int - počet odběrných míst v databázi
     */
    static public int count(Context context) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        int count = dataSubscriptionPointSource.countSubscriptionPoints();
        dataSubscriptionPointSource.close();
        return count;
    }


    /**
     * Načte všechna odběrná místa z databáze.
     *
     * @param context Kontext aplikace
     * @return ArrayList<SubscriptionPointModel> Seznam odběrných míst
     */
    public static ArrayList<SubscriptionPointModel> getAllSubscriptionPoints(Context context) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPoints = dataSubscriptionPointSource.loadSubscriptionPoints();
        dataSubscriptionPointSource.close();
        return subscriptionPoints;
    }


    /**
     * Uloží aktuálně vybrané odběrné místo do obou úložišť (SharePreferences i databáze).
     *
     * <p>Tato metoda je zavolávána každý čas, když uživatel vybere odběrné místo:
     * <ul>
     *   <li>V {@link cz.xlisto.elektrodroid.modules.subscriptionpoint.SubscriptionPointFragment SubscriptionPointFragment} - změna spinneru</li>
     *   <li>V {@link cz.xlisto.elektrodroid.modules.subscriptionpoint.SubscriptionPointAddFragment SubscriptionPointAddFragment} - vytvoření nového místa</li>
     *   <li>V {@link cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment SubscriptionPointDialogFragment} - dialog po obnovení</li>
     *   <li>V {@link cz.xlisto.elektrodroid.MainActivity MainActivity} - HDO notifikace</li>
     * </ul>
     * </p>
     *
     * <p>Dual-write zajišťuje, že:</p>
     * <ul>
     *   <li>SharedPreferences - pro okamžitý přístup během běhu aplikace</li>
     *   <li>Databáze - pro persistenci přes backup/restore</li>
     * </ul>
     * </p>
     *
     * @param context aplikační kontext
     * @param idSubscriptionPoint id odběrného místa k uložení
     *
     * @see cz.xlisto.elektrodroid.databaze.DataSettingsSource#setCurrentSubscriptionPoint(long)
     */
    public static void setCurrentSelection(Context context, long idSubscriptionPoint) {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
        shPSubscriptionPoint.set(ID_SUBSCRIPTION_POINT_LONG, idSubscriptionPoint);

        DataSettingsSource dataSettingsSource = new DataSettingsSource(context);
        dataSettingsSource.open();
        dataSettingsSource.setCurrentSubscriptionPoint(idSubscriptionPoint);
        dataSettingsSource.close();
    }


    /**
     * Obnoví aktuálně vybrané odběrné místo ze uloženého nastavení v databázi.
     *
     * <p>Tato metoda je zavolávána po importu zálohy (z lokálního souboru nebo Google Drive).
     * Pokusí se načíst ID odběrného místa z tabulky nastavení a aplikuje ho do SharedPreferences.</p>
     *
     * <p>Typické používání v reverse flow:</p>
     * <pre>
     *     // Após importu zálohy
     *     if (SubscriptionPoint.applyCurrentFromSettings(context)) {
     *         // Úspěšně obnoven - aktualizovat UI
     *         updateToolbarAndLoadData();
     *     } else {
     *         // Žádné uložené místo nebolo nalezeno - zobrazit dialog
     *         showSubscriptionPointSelectionDialog();
     *     }
     * </pre>
     * </p>
     *
     * <p>Proces ověřuje, že načtené ID odpovídá validnímu odběrnému místu v databázi.
     * Pokud se načte ID, ale místo s tímto ID neexistuje, vrací {@code false}.</p>
     *
     * @param context aplikační kontext
     * @return {@code true} pokud se podařilo obnovit validní odběrné místo,
     *         {@code false} pokud nastavení neexistuje nebo neobsahuje validní místo
     *
     * @see cz.xlisto.elektrodroid.modules.backup.BackupFragment
     * @see cz.xlisto.elektrodroid.modules.backup.GoogleDriveFragment
     */
    public static boolean applyCurrentFromSettings(Context context) {
        DataSettingsSource dataSettingsSource = new DataSettingsSource(context);
        dataSettingsSource.open();
        long subscriptionPointId = dataSettingsSource.loadCurrentSubscriptionPoint();
        dataSettingsSource.close();

        SubscriptionPointModel subscriptionPoint = loadById(context, subscriptionPointId);
        if (subscriptionPoint == null)
            return false;

        new ShPSubscriptionPoint(context).set(ID_SUBSCRIPTION_POINT_LONG, subscriptionPointId);
        return true;
    }


    /**
     * Načte odběrné místo podle jeho ID z databáze.
     *
     * <p>Toto je vnitřní helper metoda pro faktorizaci opakovaného kódu.
     * Ověří, že ID je validní (> 0), a pokud není, vrátí {@code null}.</p>
     *
     * @param context aplikační kontext
     * @param id id odběrného místa k načtení
     * @return {@link cz.xlisto.elektrodroid.models.SubscriptionPointModel SubscriptionPointModel} nebo {@code null}
     *         pokud je id nevalidní nebo místo neexistuje
     *
     * @see cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource#loadSubscriptionPoint(long)
     */
    private static SubscriptionPointModel loadById(Context context, long id) {
        if (id <= 0)
            return null;

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(id);
        dataSubscriptionPointSource.close();
        return subscriptionPoint;
    }

}
