package cz.xlisto.elektrodroid.modules.widgets;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Úložiště nezávislých konfigurací jednotlivých instancí widgetů klíčovaných výhradně podle {@code appWidgetId}.
 * <p>
 * Každá instance widgetu má svůj samostatný záznam v dedikovaném souboru {@link SharedPreferences}
 * ({@code elektrodroid_home_widgets}), takže více instancí téhož widgetu pro různá odběrná místa
 * (např. Domov, Chata, Garáž) se nikdy vzájemně neovlivňuje.
 */
public class WidgetConfigStore {

    static final String PREFS_NAME = "elektrodroid_home_widgets";
    static final String KEY_PREFIX = "widget_cfg_";

    @NonNull
    private final KeyValueStorage storage;


    /**
     * Jednoduchá abstrakce klíč–hodnota nad {@link SharedPreferences},
     * umožňující snadné jednotkové testování bez závislosti na Android frameworku.
     */
    public interface KeyValueStorage {

        @Nullable
        String getString(@NonNull String key);

        void putString(@NonNull String key, @NonNull String value);

        void remove(@NonNull String key);

        @NonNull
        Map<String, ?> getAll();

    }


    /**
     * Vytvoří úložiště konfigurací widgetů nad systémovými {@link SharedPreferences}.
     *
     * @param context aplikační kontext
     */
    public WidgetConfigStore(@NonNull Context context) {
        this(new SharedPrefsStorage(context.getApplicationContext()));
    }


    /**
     * Vytvoří úložiště konfigurací widgetů nad vlastní implementací {@link KeyValueStorage} (pro testy).
     *
     * @param storage implementace úložiště klíč–hodnota
     */
    public WidgetConfigStore(@NonNull KeyValueStorage storage) {
        this.storage = storage;
    }


    /**
     * Uloží konfiguraci výhradně pro zadané {@code appWidgetId}.
     *
     * @param config konfigurace instance widgetu
     */
    public void saveConfig(@NonNull WidgetConfig config) {
        storage.putString(buildKey(config.getAppWidgetId()), config.toJson());
    }


    /**
     * Načte konfiguraci konkrétní instance widgetu podle jejího {@code appWidgetId}.
     *
     * @param appWidgetId systémové ID instance widgetu
     * @return konfigurace instance, nebo {@code null}, pokud instance není nakonfigurována
     */
    @Nullable
    public WidgetConfig loadConfig(int appWidgetId) {
        String raw = storage.getString(buildKey(appWidgetId));
        return WidgetConfig.fromJson(appWidgetId, raw);
    }


    /**
     * Smaže konfiguraci pouze pro zadané {@code appWidgetId}, ostatní instance ponechá beze změny.
     *
     * @param appWidgetId systémové ID odstraňované instance widgetu
     */
    public void deleteConfig(int appWidgetId) {
        storage.remove(buildKey(appWidgetId));
    }


    /**
     * Smaže konfigurace pro všechna předaná {@code appWidgetIds}.
     *
     * @param appWidgetIds pole ID odstraňovaných widgetů
     */
    public void deleteConfigs(@Nullable int[] appWidgetIds) {
        if (appWidgetIds == null) {
            return;
        }
        for (int id : appWidgetIds) {
            deleteConfig(id);
        }
    }


    /**
     * Načte všechny platné uložené konfigurace všech instancí widgetů.
     *
     * @return seznam všech uložených konfigurací
     */
    @NonNull
    public List<WidgetConfig> loadAllConfigs() {
        List<WidgetConfig> result = new ArrayList<>();
        Map<String, ?> all = storage.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith(KEY_PREFIX)) {
                continue;
            }
            Object value = entry.getValue();
            if (!(value instanceof String rawJson)) {
                continue;
            }
            Integer widgetId = parseWidgetId(key);
            if (widgetId == null) {
                continue;
            }
            WidgetConfig config = WidgetConfig.fromJson(widgetId, rawJson);
            if (config != null) {
                result.add(config);
            }
        }
        return result;
    }


    /**
     * Načte všechny uložené konfigurace widgetů daného typu.
     *
     * @param type požadovaný typ widgetu
     * @return seznam konfigurací odpovídajících zadanému typu
     */
    @NonNull
    public List<WidgetConfig> loadConfigsByType(@NonNull WidgetType type) {
        List<WidgetConfig> result = new ArrayList<>();
        for (WidgetConfig config : loadAllConfigs()) {
            if (config.getWidgetType() == type) {
                result.add(config);
            }
        }
        return result;
    }


    /**
     * Vrátí všechny konfigurace navázané na zadané odběrné místo (podle databázového ID nebo stabilního {@code milins}).
     *
     * @param subscriptionPointId databázové ID odběrného místa (nebo {@code <= 0}, pokud není známé)
     * @param milins              stabilní časová značka tabulek odběrného místa (nebo {@code <= 0}, pokud není známá)
     * @return seznam dotčených konfigurací widgetů
     */
    @NonNull
    public List<WidgetConfig> loadConfigsForSubscriptionPoint(long subscriptionPointId, long milins) {
        List<WidgetConfig> result = new ArrayList<>();
        for (WidgetConfig config : loadAllConfigs()) {
            boolean matchesId = subscriptionPointId > 0L && config.getSubscriptionPointId() == subscriptionPointId;
            boolean matchesMilins = milins > 0L && config.getSubscriptionPointMilins() == milins;
            if (matchesId || matchesMilins) {
                result.add(config);
            }
        }
        return result;
    }


    /**
     * Bezpečně přemapuje uložené konfigurace při systémové obnově widgetů ({@code AppWidgetProvider.onRestored}).
     * <p>
     * Nejprve načte všechny původní konfigurace do paměti, poté odstraní staré klíče a zapíše nové,
     * takže korektně funguje i v případě překryvu mezi poli {@code oldWidgetIds} a {@code newWidgetIds}.
     *
     * @param oldWidgetIds pole původních ID widgetů
     * @param newWidgetIds pole nových ID widgetů v odpovídajícím pořadí
     */
    public void remapWidgetIds(@Nullable int[] oldWidgetIds, @Nullable int[] newWidgetIds) {
        if (oldWidgetIds == null || newWidgetIds == null) {
            return;
        }
        int count = Math.min(oldWidgetIds.length, newWidgetIds.length);
        Map<Integer, WidgetConfig> remapped = new HashMap<>();
        for (int i = 0; i < count; i++) {
            int oldId = oldWidgetIds[i];
            int newId = newWidgetIds[i];
            WidgetConfig existing = loadConfig(oldId);
            if (existing != null) {
                remapped.put(newId, existing.withAppWidgetId(newId));
            }
        }
        for (int i = 0; i < count; i++) {
            deleteConfig(oldWidgetIds[i]);
        }
        for (WidgetConfig updated : remapped.values()) {
            saveConfig(updated);
        }
    }


    /**
     * Odstraní osiřelé konfigurace widgetů, jejichž {@code appWidgetId} již systémový {@code AppWidgetManager} nezná.
     *
     * @param activeWidgetIds množina všech aktivních {@code appWidgetId} registrovaných v systému
     */
    public void pruneOrphanedConfigs(@NonNull Set<Integer> activeWidgetIds) {
        for (WidgetConfig config : loadAllConfigs()) {
            if (!activeWidgetIds.contains(config.getAppWidgetId())) {
                deleteConfig(config.getAppWidgetId());
            }
        }
    }


    @NonNull
    static String buildKey(int appWidgetId) {
        return KEY_PREFIX + appWidgetId;
    }


    @Nullable
    private static Integer parseWidgetId(@NonNull String key) {
        try {
            return Integer.parseInt(key.substring(KEY_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private static final class SharedPrefsStorage implements KeyValueStorage {

        private final SharedPreferences prefs;


        private SharedPrefsStorage(@NonNull Context appContext) {
            this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }


        @Nullable
        @Override
        public String getString(@NonNull String key) {
            return prefs.getString(key, null);
        }


        @Override
        public void putString(@NonNull String key, @NonNull String value) {
            prefs.edit().putString(key, value).apply();
        }


        @Override
        public void remove(@NonNull String key) {
            prefs.edit().remove(key).apply();
        }


        @NonNull
        @Override
        public Map<String, ?> getAll() {
            return prefs.getAll();
        }

    }

}
