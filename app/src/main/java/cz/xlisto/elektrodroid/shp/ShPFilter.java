package cz.xlisto.elektrodroid.shp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.xlisto.elektrodroid.databaze.DataSettingsSource;


/**
 * Nastavení jednoho typu filtru ceníků pro odběrné místo. Databáze nastavení je součástí zálohy.
 */
public class ShPFilter {

    private static final String TAG = "ShPFilter";

    public static final String RADA = "filtrRada";
    public static final String PRODUKT = "filtrProdukt";
    public static final String SAZBA = "filtrSazba";
    public static final String COMPANY = "filtrDodavatel";
    public static final String AREA = "filtrUzemi";
    public static final String ORIGIN = "filtrPuvod";
    public static final String DATE_START = "filtrDateStart";
    public static final String DATE_END = "filtrDateEnd";
    public static final String DEFAULT = "%";
    public static final String WITHOUT_SUBSCRIPTION_POINT = "";

    private static final String[] FILTER_KEYS = {
            RADA, PRODUKT, SAZBA, COMPANY, AREA, ORIGIN, DATE_START, DATE_END
    };

    public enum FilterType {
        GENERAL(""),
        MONTHLY_READING("_monthly"),
        INVOICE("_invoice");

        private final String legacySuffix;


        FilterType(String legacySuffix) {
            this.legacySuffix = legacySuffix;
        }
    }


    private final Context context;
    private final FilterType filterType;
    private final String pointMilins;
    private JSONObject values;


    public ShPFilter(Context context, FilterType filterType, String pointMilins) {
        this.context = context.getApplicationContext();
        this.filterType = filterType;
        this.pointMilins = pointMilins;
        DataSettingsSource settings = new DataSettingsSource(this.context);
        settings.open();
        try {
            values = settings.loadPriceListFilter(pointMilins, filterType.name());
        } finally {
            settings.close();
        }
    }


    public String get(String key, String defaultValue) {
        return values.optString(key, defaultValue);
    }


    /**
     * Uloží změny v jedné databázové operaci a zachová zbývající kritéria filtru.
     */
    public void save(Map<String, String> changes) {
        DataSettingsSource settings = new DataSettingsSource(context);
        settings.open();
        try {
            values = settings.savePriceListFilter(pointMilins, filterType.name(), changes);
        } finally {
            settings.close();
        }
    }


    public boolean isFilterActive() {
        for (String key : FILTER_KEYS) {
            String value = get(key, DEFAULT);
            if (!value.trim().isEmpty() && !value.equals(DEFAULT) && !value.equalsIgnoreCase("[VŠE]")) {
                return true;
            }
        }
        return false;
    }


    /**
     * Jednorázově zkopíruje původní filtry ze SharedPreferences do existujících odběrných míst.
     * Staré předvolby se odstraňují až po úspěšném zápisu do databáze.
     */
    public static void migrateLegacyFilters(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> oldValues = preferences.getAll();
        Map<String, String> filters = new HashMap<>();
        List<String> migratedKeys = new ArrayList<>();
        for (FilterType type : FilterType.values()) {
            Map<String, String> filter = new HashMap<>();
            for (String key : FILTER_KEYS) {
                String legacyKey = key + type.legacySuffix;
                if (oldValues.get(legacyKey) instanceof String value) {
                    filter.put(key, value);
                    migratedKeys.add(legacyKey);
                }
            }
            if (!filter.isEmpty()) {
                filters.put(type.name(), new JSONObject(filter).toString());
            }
        }
        if (filters.isEmpty()) {
            return;
        }

        DataSettingsSource settings = new DataSettingsSource(context);
        settings.open();
        if (settings.getDatabase() == null) {
            return;
        }
        try {
            settings.migrateLegacyPriceListFilters(filters);
        } catch (SQLException | NumberFormatException e) {
            Log.w(TAG, "Převod filtrů ceníků se zopakuje při dalším spuštění", e);
            return;
        } finally {
            settings.close();
        }

        SharedPreferences.Editor editor = preferences.edit();
        for (String key : migratedKeys) {
            editor.remove(key);
        }
        editor.apply();
    }

}
