package cz.xlisto.elektrodroid.modules.widgets;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * Centrální správce aktualizací všech instancí domovských widgetů aplikace ElektroDroid.
 * <p>
 * Zajišťuje:
 * <ul>
 *     <li>zpracování každé instance widgetu výhradně podle jejího vlastního {@code appWidgetId},</li>
 *     <li>asynchronní načítání dat z SQLite a vykreslování bitmap na jednom pracovním vlákně na pozadí,</li>
 *     <li>slučování (debounce) rychlých databázových změn do jedné úsporné aktualizace dotčených widgetů,</li>
 *     <li>šetrné plánování inexact alarmu pro přepnutí HDO a posun ručičky hodin (pouze pokud jsou příslušné widgety umístěny).</li>
 * </ul>
 */
public final class WidgetUpdateManager {

    private static final long DEBOUNCE_DELAY_MS = 250L;
    private static final long CLOCK_REFRESH_INTERVAL_MS = 15L * 60L * 1000L;
    private static final long MIN_ALARM_DELAY_MS = 10_000L;
    private static final int HDO_ALARM_REQUEST_CODE = 47812;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ElektroDroid-Widgets");
        thread.setDaemon(true);
        return thread;
    });

    private static final Object PENDING_LOCK = new Object();

    private static final Set<WidgetType> pendingGlobalTypes = EnumSet.noneOf(WidgetType.class);
    private static final Set<Long> pendingPointIds = new HashSet<>();
    private static final Set<Long> pendingPointMilins = new HashSet<>();
    private static boolean pendingFullRefresh = false;
    private static boolean debounceScheduled = false;


    private WidgetUpdateManager() {
    }


    /**
     * Okamžitě aktualizuje jednu konkrétní instanci widgetu na pracovním vlákně (např. po uložení konfigurace).
     *
     * @param context     kontext aplikace
     * @param widgetType  typ widgetu
     * @param appWidgetId systémové ID instance
     */
    public static void updateSingleWidgetAsync(@NonNull Context context,
                                               @NonNull WidgetType widgetType,
                                               int appWidgetId) {
        updateWidgetsAsync(context, widgetType, new int[]{appWidgetId}, null);
    }


    /**
     * Synchronně aktualizuje jednu instanci widgetu (určeno pro volání z vlákna na pozadí, např. při dokončení konfigurace).
     *
     * @param context     kontext aplikace
     * @param widgetType  typ widgetu
     * @param appWidgetId systémové ID instance
     */
    public static void updateSingleWidgetNow(@NonNull Context context,
                                             @NonNull WidgetType widgetType,
                                             int appWidgetId) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        WidgetConfigStore store = new WidgetConfigStore(appContext);
        updateInstanceInternal(appContext, manager, store, widgetType, appWidgetId);
        rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
    }


    /**
     * Asynchronně aktualizuje předaná {@code appWidgetIds} jednoho typu widgetu.
     *
     * @param context      kontext aplikace
     * @param widgetType   typ widgetu
     * @param appWidgetIds pole ID instancí k aktualizaci
     * @param onComplete   volitelný callback po dokončení (např. pro {@code PendingResult.finish()})
     */
    public static void updateWidgetsAsync(@NonNull Context context,
                                          @NonNull WidgetType widgetType,
                                          @Nullable int[] appWidgetIds,
                                          @Nullable Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        int[] idsCopy = appWidgetIds != null ? appWidgetIds.clone() : new int[0];
        EXECUTOR.execute(() -> {
            try {
                AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
                WidgetConfigStore store = new WidgetConfigStore(appContext);
                for (int appWidgetId : idsCopy) {
                    updateInstanceInternal(appContext, manager, store, widgetType, appWidgetId);
                }
                if (widgetType == WidgetType.CURRENT_PRICE || widgetType == WidgetType.HDO_CLOCK) {
                    rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
                }
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }


    /**
     * Asynchronně aktualizuje všechny aktivní instance zadaných typů widgetů.
     *
     * @param context    kontext aplikace
     * @param types      množina typů widgetů k aktualizaci
     * @param onComplete volitelný callback po dokončení
     */
    public static void updateWidgetTypesAsync(@NonNull Context context,
                                              @NonNull Set<WidgetType> types,
                                              @Nullable Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        Set<WidgetType> typesCopy = EnumSet.copyOf(types);
        EXECUTOR.execute(() -> {
            try {
                AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
                WidgetConfigStore store = new WidgetConfigStore(appContext);
                for (WidgetType type : typesCopy) {
                    int[] ids = getActiveWidgetIds(appContext, manager, type);
                    for (int id : ids) {
                        updateInstanceInternal(appContext, manager, store, type, id);
                    }
                }
                if (typesCopy.contains(WidgetType.CURRENT_PRICE) || typesCopy.contains(WidgetType.HDO_CLOCK)) {
                    rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
                }
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }


    /**
     * Asynchronně aktualizuje všechny aktivní widgety všech typů a přeplánuje HDO alarm.
     *
     * @param context    kontext aplikace
     * @param onComplete volitelný callback po dokončení
     */
    public static void updateAllWidgetsAsync(@NonNull Context context, @Nullable Runnable onComplete) {
        updateWidgetTypesAsync(context, EnumSet.allOf(WidgetType.class), onComplete);
    }


    /**
     * Odstraní konfigurace smazaných instancí widgetů a případně zruší nepotřebný HDO alarm.
     *
     * @param context      kontext aplikace
     * @param appWidgetIds pole smazaných ID widgetů
     */
    public static void onWidgetsDeleted(@NonNull Context context, @Nullable int[] appWidgetIds) {
        Context appContext = context.getApplicationContext();
        int[] idsCopy = appWidgetIds != null ? appWidgetIds.clone() : new int[0];
        EXECUTOR.execute(() -> {
            WidgetConfigStore store = new WidgetConfigStore(appContext);
            store.deleteConfigs(idsCopy);
            AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
            rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
        });
    }


    /**
     * Přemapuje konfigurace při systémové obnově widgetů ({@code onRestored}) a okamžitě aktualizuje nové instance.
     *
     * @param context      kontext aplikace
     * @param widgetType   typ widgetu
     * @param oldWidgetIds původní ID widgetů
     * @param newWidgetIds nová ID widgetů
     * @param onComplete   volitelný callback po dokončení
     */
    public static void onWidgetsRestored(@NonNull Context context,
                                         @NonNull WidgetType widgetType,
                                         @Nullable int[] oldWidgetIds,
                                         @Nullable int[] newWidgetIds,
                                         @Nullable Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        int[] oldCopy = oldWidgetIds != null ? oldWidgetIds.clone() : null;
        int[] newCopy = newWidgetIds != null ? newWidgetIds.clone() : null;
        EXECUTOR.execute(() -> {
            try {
                WidgetConfigStore store = new WidgetConfigStore(appContext);
                store.remapWidgetIds(oldCopy, newCopy);
                AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
                if (newCopy != null) {
                    for (int newId : newCopy) {
                        updateInstanceInternal(appContext, manager, store, widgetType, newId);
                    }
                }
                rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }


    /**
     * Oznámí změnu v dynamické databázové tabulce odběrného místa ({@code O*}, {@code HDO*},
     * {@code FAK*}, {@code TED*}, {@code PLATBY*}). Aktualizace se sloučí (debounce) a cílí
     * pouze na widgety dotčeného odběrného místa.
     *
     * @param context   kontext aplikace
     * @param tableName název změněné tabulky
     */
    public static void notifyTableChanged(@Nullable Context context, @Nullable String tableName) {
        if (context == null) {
            return;
        }
        long milins = parseMilinsFromTableName(tableName);
        if (milins > 0L) {
            scheduleDebouncedUpdate(context, null, -1L, milins, false);
        } else {
            scheduleDebouncedUpdate(context, EnumSet.of(
                    WidgetType.CURRENT_PRICE,
                    WidgetType.HDO_CLOCK,
                    WidgetType.CONSUMPTION,
                    WidgetType.FINANCIAL_BALANCE
            ), -1L, -1L, false);
        }
    }


    /**
     * Oznámí úpravu nebo smazání konkrétního odběrného místa (aktualizuje všechny widgety vázané na toto místo).
     *
     * @param context             kontext aplikace
     * @param subscriptionPointId databázové ID odběrného místa (nebo {@code <= 0})
     */
    public static void notifySubscriptionPointChanged(@Nullable Context context, long subscriptionPointId) {
        if (context == null) {
            return;
        }
        if (subscriptionPointId > 0L) {
            scheduleDebouncedUpdate(context, null, subscriptionPointId, -1L, false);
        } else {
            scheduleDebouncedUpdate(context, EnumSet.allOf(WidgetType.class), -1L, -1L, true);
        }
    }


    /**
     * Oznámí úpravu nebo smazání ceníku (dotýká se ceny, spotřeby i finanční bilance).
     *
     * @param context kontext aplikace
     */
    public static void notifyPriceListChanged(@Nullable Context context) {
        if (context == null) {
            return;
        }
        scheduleDebouncedUpdate(context, EnumSet.of(
                WidgetType.CURRENT_PRICE,
                WidgetType.CONSUMPTION,
                WidgetType.FINANCIAL_BALANCE
        ), -1L, -1L, false);
    }


    /**
     * Oznámí změnu uživatelských barev VT/NT (dotýká se widgetu ceny a spotřeby).
     *
     * @param context kontext aplikace
     */
    public static void notifyVtNtColorsChanged(@Nullable Context context) {
        if (context == null) {
            return;
        }
        scheduleDebouncedUpdate(context, EnumSet.of(
                WidgetType.CURRENT_PRICE,
                WidgetType.CONSUMPTION
        ), -1L, -1L, false);
    }


    /**
     * Oznámí změnu motivu aplikace (přepnutí dynamických barev Material You, změna tapety
     * nebo světlého/tmavého režimu) a obnoví všechny umístěné widgety.
     *
     * @param context kontext aplikace
     */
    public static void notifyThemeChanged(@Nullable Context context) {
        if (context == null) {
            return;
        }
        scheduleDebouncedUpdate(context, EnumSet.allOf(WidgetType.class), -1L, -1L, false);
    }


    /**
     * Oznámí změnu nastavení HDO (např. hlavní relé nebo časový posun elektroměru).
     *
     * @param context             kontext aplikace
     * @param subscriptionPointId ID odběrného místa (nebo {@code <= 0} pro globální změnu hlavního relé)
     */
    public static void notifyHdoSettingsChanged(@Nullable Context context, long subscriptionPointId) {
        if (context == null) {
            return;
        }
        if (subscriptionPointId > 0L) {
            scheduleDebouncedUpdate(context, null, subscriptionPointId, -1L, false);
        } else {
            scheduleDebouncedUpdate(context, EnumSet.of(
                    WidgetType.CURRENT_PRICE,
                    WidgetType.HDO_CLOCK
            ), -1L, -1L, false);
        }
    }


    /**
     * Oznámí dokončení obnovy databáze ze zálohy – znovu ověří všechny vazby widgetů podle
     * {@code _id} a {@code milins} a obnoví všechny umístěné instance.
     *
     * @param context kontext aplikace
     */
    public static void onDatabaseRestored(@Nullable Context context) {
        if (context == null) {
            return;
        }
        scheduleDebouncedUpdate(context, EnumSet.allOf(WidgetType.class), -1L, -1L, true);
    }


    /**
     * Přeplánuje nebo zruší inexact alarm pro aktualizaci widgetů závislých na čase HDO.
     *
     * @param context kontext aplikace
     */
    public static void rescheduleHdoWidgetsRefreshAsync(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
            WidgetConfigStore store = new WidgetConfigStore(appContext);
            rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
        });
    }


    /**
     * Extrahuje číselnou příponu {@code milins} z názvu dynamické tabulky odběrného místa
     * ({@code O*}, {@code HDO*}, {@code FAK*}, {@code TED*}, {@code PLATBY*}).
     *
     * @param tableName název databázové tabulky
     * @return hodnota {@code milins}, nebo {@code -1L}, pokud název neodpovídá konvenci
     */
    static long parseMilinsFromTableName(@Nullable String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return -1L;
        }
        String suffix;
        if (tableName.startsWith("PLATBY")) {
            suffix = tableName.substring(6);
        } else if (tableName.startsWith("HDO") || tableName.startsWith("FAK") || tableName.startsWith("TED")) {
            suffix = tableName.substring(3);
        } else if (tableName.startsWith("O")) {
            suffix = tableName.substring(1);
        } else {
            return -1L;
        }
        if (suffix.isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(suffix);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }


    private static void scheduleDebouncedUpdate(@NonNull Context context,
                                                @Nullable Set<WidgetType> types,
                                                long pointId,
                                                long pointMilins,
                                                boolean fullRefresh) {
        Context appContext = context.getApplicationContext();
        synchronized (PENDING_LOCK) {
            if (fullRefresh) {
                pendingFullRefresh = true;
            }
            if (types != null) {
                pendingGlobalTypes.addAll(types);
            }
            if (pointId > 0L) {
                pendingPointIds.add(pointId);
            }
            if (pointMilins > 0L) {
                pendingPointMilins.add(pointMilins);
            }
            if (debounceScheduled) {
                return;
            }
            debounceScheduled = true;
        }
        EXECUTOR.schedule(() -> flushPendingUpdates(appContext), DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }


    private static void flushPendingUpdates(@NonNull Context appContext) {
        final boolean fullRefresh;
        final Set<WidgetType> types;
        final Set<Long> pointIds;
        final Set<Long> pointMilins;

        synchronized (PENDING_LOCK) {
            fullRefresh = pendingFullRefresh;
            types = EnumSet.copyOf(pendingGlobalTypes);
            pointIds = new HashSet<>(pendingPointIds);
            pointMilins = new HashSet<>(pendingPointMilins);

            pendingFullRefresh = false;
            pendingGlobalTypes.clear();
            pendingPointIds.clear();
            pendingPointMilins.clear();
            debounceScheduled = false;
        }

        EXECUTOR.execute(() -> {
            AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
            WidgetConfigStore store = new WidgetConfigStore(appContext);

            Set<Integer> activeIdsAll = collectAllActiveWidgetIds(appContext, manager);
            if (activeIdsAll.isEmpty()) {
                cancelHdoWidgetsAlarm(appContext);
                return;
            }
            store.pruneOrphanedConfigs(activeIdsAll);

            Set<Integer> updatedIds = new HashSet<>();
            boolean touchedHdoOrPrice = false;

            if (fullRefresh) {
                for (WidgetType type : WidgetType.values()) {
                    for (int id : getActiveWidgetIds(appContext, manager, type)) {
                        updateInstanceInternal(appContext, manager, store, type, id);
                        updatedIds.add(id);
                    }
                }
                touchedHdoOrPrice = true;
            } else {
                for (WidgetType type : types) {
                    for (int id : getActiveWidgetIds(appContext, manager, type)) {
                        if (updatedIds.add(id)) {
                            updateInstanceInternal(appContext, manager, store, type, id);
                        }
                    }
                    if (type == WidgetType.CURRENT_PRICE || type == WidgetType.HDO_CLOCK) {
                        touchedHdoOrPrice = true;
                    }
                }

                if (!pointIds.isEmpty() || !pointMilins.isEmpty()) {
                    List<WidgetConfig> allConfigs = store.loadAllConfigs();
                    for (WidgetConfig config : allConfigs) {
                        int id = config.appWidgetId();
                        if (!activeIdsAll.contains(id) || updatedIds.contains(id)) {
                            continue;
                        }
                        boolean match = pointIds.contains(config.subscriptionPointId())
                                || pointMilins.contains(config.subscriptionPointMilins());
                        if (match) {
                            updateInstanceInternal(appContext, manager, store, config.widgetType(), id);
                            updatedIds.add(id);
                            if (config.widgetType() == WidgetType.CURRENT_PRICE
                                    || config.widgetType() == WidgetType.HDO_CLOCK) {
                                touchedHdoOrPrice = true;
                            }
                        }
                    }
                }
            }

            if (touchedHdoOrPrice) {
                rescheduleHdoWidgetsRefreshInternal(appContext, manager, store);
            }
        });
    }


    private static void updateInstanceInternal(@NonNull Context appContext,
                                               @NonNull AppWidgetManager manager,
                                               @NonNull WidgetConfigStore store,
                                               @NonNull WidgetType widgetType,
                                               int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        WidgetConfig config = store.loadConfig(appWidgetId);
        WidgetPointResolver.ResolutionResult resolution = WidgetPointResolver.resolveAndRefreshBinding(
                appContext, store, config);
        // Znovu načteme případně aktualizovanou konfiguraci (po znovunapojení milins či přejmenování)
        WidgetConfig refreshedConfig = (config != null) ? store.loadConfig(appWidgetId) : null;
        Bundle options = manager.getAppWidgetOptions(appWidgetId);
        RemoteViews views = WidgetRemoteViewsBuilder.build(
                appContext, widgetType, appWidgetId, refreshedConfig, resolution, options);
        manager.updateAppWidget(appWidgetId, views);
    }


    private static void rescheduleHdoWidgetsRefreshInternal(@NonNull Context appContext,
                                                            @NonNull AppWidgetManager manager,
                                                            @NonNull WidgetConfigStore store) {
        int[] priceIds = getActiveWidgetIds(appContext, manager, WidgetType.CURRENT_PRICE);
        int[] clockIds = getActiveWidgetIds(appContext, manager, WidgetType.HDO_CLOCK);

        if (priceIds.length == 0 && clockIds.length == 0) {
            cancelHdoWidgetsAlarm(appContext);
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long earliestTrigger = Long.MAX_VALUE;
        List<SubscriptionPointModel> allPoints = WidgetPointResolver.loadAllSubscriptionPoints(appContext);

        for (int id : priceIds) {
            earliestTrigger = Math.min(earliestTrigger, computeNextTriggerForWidget(appContext, store, WidgetType.CURRENT_PRICE, id, allPoints, nowMillis));
        }
        for (int id : clockIds) {
            earliestTrigger = Math.min(earliestTrigger, computeNextTriggerForWidget(appContext, store, WidgetType.HDO_CLOCK, id, allPoints, nowMillis));
        }

        // Pro analogové hodiny HDO omezíme interval obnovy ručičky na max. 15 minut
        if (clockIds.length > 0) {
            earliestTrigger = Math.min(earliestTrigger, nowMillis + CLOCK_REFRESH_INTERVAL_MS);
        }

        if (earliestTrigger == Long.MAX_VALUE) {
            cancelHdoWidgetsAlarm(appContext);
            return;
        }

        long triggerAtMillis = Math.max(nowMillis + MIN_ALARM_DELAY_MS, earliestTrigger + 2_000L);
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Inexact non-wakeup alarm: šetrný k baterii a nevyžaduje oprávnění pro přesné alarmy
            alarmManager.set(AlarmManager.RTC, triggerAtMillis, buildHdoRefreshPendingIntent(appContext));
        }
    }


    private static long computeNextTriggerForWidget(@NonNull Context appContext,
                                                    @NonNull WidgetConfigStore store,
                                                    @NonNull WidgetType widgetType,
                                                    int appWidgetId,
                                                    @NonNull List<SubscriptionPointModel> allPoints,
                                                    long nowMillis) {
        WidgetConfig config = store.loadConfig(appWidgetId);
        WidgetPointResolver.ResolutionResult resolution = WidgetPointResolver.resolve(config, allPoints);
        if (resolution.status() != WidgetPointResolver.ResolutionStatus.RESOLVED || resolution.point() == null) {
            return Long.MAX_VALUE;
        }
        WidgetDataService.HdoSnapshot hdo = WidgetDataService.loadHdoSnapshot(appContext, resolution.point(), nowMillis);
        if (!hdo.hasHdo()) {
            return Long.MAX_VALUE;
        }
        long nextTrigger = (widgetType == WidgetType.HDO_CLOCK)
                ? hdo.earliestNextTriggerMillis(nowMillis)
                : hdo.nextTriggerMillis();
        if (nextTrigger > nowMillis) {
            return nextTrigger;
        }
        return Long.MAX_VALUE;
    }


    private static void cancelHdoWidgetsAlarm(@NonNull Context appContext) {
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(buildHdoRefreshPendingIntent(appContext));
        }
    }


    @NonNull
    private static PendingIntent buildHdoRefreshPendingIntent(@NonNull Context appContext) {
        Intent intent = new Intent(appContext, WidgetHdoRefreshReceiver.class);
        intent.setAction(WidgetHdoRefreshReceiver.ACTION_REFRESH_HDO_WIDGETS);
        return PendingIntent.getBroadcast(
                appContext,
                HDO_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }


    @NonNull
    private static int[] getActiveWidgetIds(@NonNull Context appContext,
                                            @NonNull AppWidgetManager manager,
                                            @NonNull WidgetType type) {
        ComponentName componentName = new ComponentName(appContext, type.getProviderClass());
        int[] ids = manager.getAppWidgetIds(componentName);
        return ids != null ? ids : new int[0];
    }


    @NonNull
    private static Set<Integer> collectAllActiveWidgetIds(@NonNull Context appContext,
                                                          @NonNull AppWidgetManager manager) {
        Set<Integer> result = new HashSet<>();
        for (WidgetType type : WidgetType.values()) {
            for (int id : getActiveWidgetIds(appContext, manager, type)) {
                result.add(id);
            }
        }
        return result;
    }

}
