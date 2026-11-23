package cz.xlisto.elektrodroid.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.hdo.Connections;
import cz.xlisto.elektrodroid.shp.ShPSettings;

/**
 * Plánování alarmů pro upozornění na začátek/konec HDO.
 * <p>
 * Třída poskytuje utility pro plánování, přeplánování a rušení alarmů
 * nad konkrétním HDO záznamem, tabulkou i všemi odběrnými místy.
 */
public final class HdoAlarmScheduler {

    public static final String ACTION_HDO_ALARM = "cz.xlisto.elektrodroid.action.HDO_ALARM";
    public static final String EXTRA_TABLE = "extra_table";
    public static final String EXTRA_HDO_ID = "extra_hdo_id";
    public static final String EXTRA_TYPE = "extra_type";

    public static final int TYPE_START = 1;
    public static final int TYPE_END = 2;

    public static final String EXTRA_SCHEDULED_TIME = "extra_scheduled_time";

    private static final String LOG_TAG = "HdoAlarmScheduler";
    private static final int MAX_DAYS_LOOKAHEAD = 400;
    /**
     * Týdenní horizont pro výpočet nejbližšího přepnutí HDO v přehledu (ve dnech).
     */
    private static final int WEEKLY_LOOKAHEAD_DAYS = 7;
    /**
     * Bezpečnostní limit pro navazování souvislých intervalů HDO přes půlnoc.
     */
    private static final int MAX_CONTIGUOUS_EXTENSIONS = 16;

    private HdoAlarmScheduler() {
    }

    /**
     * Přeplánuje alarmy pro všechna odběrná místa.
     *
     * @param context kontext aplikace
     */
    public static void rescheduleAll(Context context) {
        DataSubscriptionPointSource source = new DataSubscriptionPointSource(context);
        source.open();
        ArrayList<SubscriptionPointModel> points = source.loadSubscriptionPoints();
        source.close();

        for (SubscriptionPointModel point : points) {
            rescheduleForTable(context, point.getTableHDO());
        }
    }

    /**
     * Přeplánuje alarmy pro všechny HDO záznamy v dané tabulce.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     */
    public static void rescheduleForTable(Context context, String table) {
        DataHdoSource source = new DataHdoSource(context);
        source.open();
        ArrayList<HdoModel> models = source.loadHdo(table);
        source.close();

        for (HdoModel model : models) {
            rescheduleForModel(context, table, model);
        }
    }

    /**
     * Přeplánuje oba typy alarmu (začátek/konec) pro konkrétní HDO záznam.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @param model   HDO záznam
     */
    public static void rescheduleForModel(Context context, String table, HdoModel model) {
        scheduleNextForType(context, table, model, TYPE_START);
        scheduleNextForType(context, table, model, TYPE_END);
    }

    /**
     * Naplánuje nejbližší alarm zadaného typu pro konkrétní HDO záznam.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @param model   HDO záznam
     * @param type    typ alarmu ({@link #TYPE_START} nebo {@link #TYPE_END})
     */
    public static void scheduleNextForType(Context context, String table, HdoModel model, int type) {
        if (!isTypeEnabled(model, type)) {
            cancelForType(context, table, model.getId(), type);
            return;
        }

        long timeShift = findTimeShiftByTable(context, table);
        long triggerAtMillis = findNextTrigger(model, type, System.currentTimeMillis(), timeShift);
        if (triggerAtMillis <= 0L) {
            cancelForType(context, table, model.getId(), type);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                canExact = alarmManager.canScheduleExactAlarms();
            } catch (Exception e) {
                Log.w(LOG_TAG, "canScheduleExactAlarms() check failed, assuming exact allowed", e);
            }
        }

        PendingIntent pendingIntent = createAlarmPendingIntent(context, table, model.getId(), type, triggerAtMillis, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean useSetAlarmClock = new ShPSettings(context).get(ShPSettings.USE_HDO_SET_ALARM, false);

        if (useSetAlarmClock) {
            try {
                alarmManager.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(triggerAtMillis, createAlarmClockInfoIntent(context, table, model.getId(), type)),
                        pendingIntent
                );
                Log.d(LOG_TAG, "Scheduled alarm via setAlarmClock for " + table + ":" + model.getId() + ", type=" + type + " at " + triggerAtMillis);
                return;
            } catch (SecurityException e) {
                Log.w(LOG_TAG, "setAlarmClock failed, falling back to default HDO scheduling", e);
            }
        }

        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(LOG_TAG, "Scheduled exact alarm for " + table + ":" + model.getId() + ", type=" + type + " at " + triggerAtMillis);
            } catch (SecurityException e) {
                Log.w(LOG_TAG, "setExactAndAllowWhileIdle failed, falling back to setAndAllowWhileIdle", e);
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else {
            Log.w(LOG_TAG, "Exact alarms not allowed, scheduling inexact alarm for " + table + ":" + model.getId() + ", type=" + type + " at " + triggerAtMillis);
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    /**
     * Zruší oba alarmy (začátek/konec) pro jeden HDO záznam.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @param hdoId   ID HDO záznamu
     */
    public static void cancelForModel(Context context, String table, long hdoId) {
        cancelForType(context, table, hdoId, TYPE_START);
        cancelForType(context, table, hdoId, TYPE_END);
    }

    /**
     * Zruší alarmy pro seznam ID HDO záznamů.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @param ids     seznam ID HDO záznamů
     */
    public static void cancelForIds(Context context, String table, List<Long> ids) {
        for (Long id : ids) {
            cancelForModel(context, table, id);
        }
    }

    /**
     * Zruší jeden konkrétní alarm podle typu.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @param hdoId   ID HDO záznamu
     * @param type    typ alarmu ({@link #TYPE_START} nebo {@link #TYPE_END})
     */
    public static void cancelForType(Context context, String table, long hdoId, int type) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = createAlarmPendingIntent(context, table, hdoId, type, 0L, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    /**
     * Vytvoří PendingIntent pro alarm konkrétního HDO záznamu a typu události.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @param hdoId   ID HDO záznamu
     * @param type    typ alarmu ({@link #TYPE_START} nebo {@link #TYPE_END})
     * @param flags   flagy pro PendingIntent
     * @return vytvořený PendingIntent
     */
    public static PendingIntent createAlarmPendingIntent(Context context, String table, long hdoId, int type, long scheduledAtMillis, int flags) {
        Intent intent = new Intent(context, HdoAlarmReceiver.class);
        intent.setAction(ACTION_HDO_ALARM);
        intent.putExtra(EXTRA_TABLE, table);
        intent.putExtra(EXTRA_HDO_ID, hdoId);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_SCHEDULED_TIME, scheduledAtMillis);

        int requestCode = (table + ":" + hdoId + ":" + type).hashCode();
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    /**
     * PendingIntent pro otevření aplikace z případné systémové informace alarmu.
     */
    private static PendingIntent createAlarmClockInfoIntent(Context context, String table, long hdoId, int type) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_TABLE, table);
        intent.putExtra(EXTRA_HDO_ID, hdoId);
        intent.putExtra(EXTRA_TYPE, type);

        int requestCode = ("info:" + table + ":" + hdoId + ":" + type).hashCode();
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Dohledá název odběrného místa podle názvu HDO tabulky.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @return název odběrného místa nebo prázdný řetězec
     */
    public static String findSubscriptionPointNameByTable(Context context, String table) {
        DataSubscriptionPointSource source = new DataSubscriptionPointSource(context);
        source.open();
        ArrayList<SubscriptionPointModel> points = source.loadSubscriptionPoints();
        source.close();

        for (SubscriptionPointModel point : points) {
            if (table.equals(point.getTableHDO())) {
                return point.getName();
            }
        }
        return "";
    }

    /**
     * Dohledá ID odběrného místa podle názvu HDO tabulky.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @return ID odběrného místa nebo -1
     */
    public static long findSubscriptionPointIdByTable(Context context, String table) {
        DataSubscriptionPointSource source = new DataSubscriptionPointSource(context);
        source.open();
        ArrayList<SubscriptionPointModel> points = source.loadSubscriptionPoints();
        source.close();

        for (SubscriptionPointModel point : points) {
            if (table.equals(point.getTableHDO())) {
                return point.getId();
            }
        }
        return -1L;
    }


    /**
     * Dohledá časový posun (time shift) pro zadanou HDO tabulku.
     *
     * @param context kontext aplikace
     * @param table   název HDO tabulky
     * @return časový posun v milisekundách nebo 0
     */
    public static long findTimeShiftByTable(Context context, String table) {
        long subscriptionPointId = findSubscriptionPointIdByTable(context, table);
        if (subscriptionPointId <= 0) {
            return 0L;
        }
        DataSettingsSource source = new DataSettingsSource(context);
        source.open();
        long timeShift = source.loadTimeShift(subscriptionPointId);
        source.close();
        return timeShift;
    }


    /**
     * Najde nejbližší čas spuštění ze seznamu HDO modelů (např. pro dané relé),
     * s plynulým navazováním souvislých aktivních intervalů přes půlnoc.
     * <p>
     * Pokud je pro aktuální datum platný alespoň jeden HDO záznam, respektuje datum platnosti
     * ({@code dateFrom}–{@code dateUntil}) v týdenním horizontu. Jestliže jsou uložená data HDO
     * neaktuální/prošlá a žádnému záznamu dnes platnost nesedí, použije záložní týdenní rozvrh
     * podle dnů v týdnu a svátků (shodně s vyhodnocením aktivního tarifu a 24h hodin HDO),
     * aby se nezobrazovalo přepnutí za desítky či stovky dní.
     *
     * @param models    seznam HDO modelů (např. pro zvolené relé)
     * @param type      typ události ({@link #TYPE_START} nebo {@link #TYPE_END})
     * @param nowMillis aktuální reálný čas v milisekundách
     * @param timeShift časový posun elektroměru v milisekundách
     * @return reálný čas nejbližšího přepnutí v milisekundách, nebo {@code -1L} při neplatných datech
     */
    public static long findNextTriggerForModels(List<HdoModel> models, int type, long nowMillis, long timeShift) {
        if (models == null || models.isEmpty()) return -1L;

        Date meterToday = new Date(nowMillis + timeShift);
        long result = -1L;
        if (hasAnyModelValidOnDate(models, meterToday)) {
            result = findNextTriggerForModelsInternal(models, type, nowMillis, timeShift, false);
        }
        if (result <= 0L) {
            result = findNextTriggerForModelsInternal(models, type, nowMillis, timeShift, true);
        }
        return result;
    }


    /**
     * Ověří, zda alespoň jeden nesváteční HDO záznam v seznamu spadá svým rozsahem platnosti
     * ({@code dateFrom}–{@code dateUntil}) do zadaného data (nebo datum platnosti neomezuje).
     *
     * @param models seznam HDO modelů
     * @param date   datum, vůči kterému se platnost vyhodnocuje
     * @return {@code true}, pokud existuje alespoň jeden záznam platný pro dané datum
     */
    private static boolean hasAnyModelValidOnDate(List<HdoModel> models, Date date) {
        for (HdoModel model : models) {
            boolean modelIsHoliday = model.getSv() == 1 || "SVÁTEK".equalsIgnoreCase(model.getDateFrom());
            if (modelIsHoliday) {
                continue;
            }
            Date[] range = cz.xlisto.elektrodroid.modules.hdo.HdoUpdateHelper.parseDateRange(
                    model.getDateFrom(), model.getDateUntil(), date);
            Date dateFrom = range != null ? range[0] : null;
            Date dateUntil = range != null ? range[1] : null;
            if (isDateInRange(date, dateFrom, dateUntil)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Interní výpočet nejbližšího přepnutí HDO nad seznamem modelů v týdenním horizontu
     * ({@link #WEEKLY_LOOKAHEAD_DAYS}) s volbou, zda ignorovat kalendářní rozmezí platnosti
     * ({@code dateFrom}–{@code dateUntil}).
     *
     * @param models          seznam HDO modelů
     * @param type            typ události ({@link #TYPE_START} nebo {@link #TYPE_END})
     * @param nowMillis       aktuální reálný čas v milisekundách
     * @param timeShift       časový posun elektroměru v milisekundách
     * @param ignoreDateRange {@code true} pro záložní vyhodnocení podle dnů v týdnu bez kontroly kalendářního rozmezí
     * @return reálný čas nejbližšího přepnutí v milisekundách, nebo {@code -1L}
     */
    private static long findNextTriggerForModelsInternal(List<HdoModel> models,
                                                         int type,
                                                         long nowMillis,
                                                         long timeShift,
                                                         boolean ignoreDateRange) {
        if (type == TYPE_END) {
            HdoModel activeModel = null;
            long activeEnd = -1L;

            for (HdoModel model : models) {
                long endTrigger = findNextTrigger(model, TYPE_END, nowMillis, timeShift, WEEKLY_LOOKAHEAD_DAYS, ignoreDateRange);
                long startTrigger = findNextTrigger(model, TYPE_START, nowMillis, timeShift, WEEKLY_LOOKAHEAD_DAYS, ignoreDateRange);

                if (endTrigger > 0L && (startTrigger <= 0L || endTrigger < startTrigger)) {
                    if (activeEnd < 0L || endTrigger > activeEnd) {
                        activeModel = model;
                        activeEnd = endTrigger;
                    }
                }
            }

            if (activeModel != null) {
                long currentEnd = activeEnd;
                boolean extended = true;
                int extensions = 0;

                while (extended && extensions < MAX_CONTIGUOUS_EXTENSIONS) {
                    extended = false;
                    for (HdoModel model : models) {
                        long startTrigger = findNextTrigger(model, TYPE_START, currentEnd - 60000L, timeShift, WEEKLY_LOOKAHEAD_DAYS, ignoreDateRange);
                        if (startTrigger > 0L && Math.abs(startTrigger - currentEnd) <= 60000L) {
                            long nextEnd = findNextTrigger(model, TYPE_END, currentEnd + 1000L, timeShift, WEEKLY_LOOKAHEAD_DAYS, ignoreDateRange);
                            if (nextEnd > currentEnd) {
                                currentEnd = nextEnd;
                                extended = true;
                                extensions++;
                                break;
                            }
                        }
                    }
                }
                return currentEnd;
            }
        }

        long earliest = -1L;
        for (HdoModel model : models) {
            long trigger = findNextTrigger(model, type, nowMillis, timeShift, WEEKLY_LOOKAHEAD_DAYS, ignoreDateRange);
            if (trigger > 0L) {
                if (earliest < 0L || trigger < earliest) {
                    earliest = trigger;
                }
            }
        }
        return earliest;
    }

    /**
     * Vrátí, zda je požadovaný typ notifikace pro záznam aktivní.
     */
    private static boolean isTypeEnabled(HdoModel model, int type) {
        return type == TYPE_START ? model.getNotifyStart() == 1 : model.getNotifyEnd() == 1;
    }

    /**
     * Najde nejbližší čas spuštění alarmu od aktuálního času, zohledňující časový posun (time shift) elektroměru.
     */
    private static long findNextTrigger(HdoModel model, int type, long nowMillis, long timeShift) {
        return findNextTrigger(model, type, nowMillis, timeShift, MAX_DAYS_LOOKAHEAD, false);
    }


    /**
     * Najde nejbližší čas spuštění nebo ukončení HDO pro jeden model v zadaném horizontu dní.
     * Pro {@link #TYPE_END} kontroluje i předchozí den ({@code offset = -1}), aby správně zachytil
     * intervaly přecházející přes půlnoc (např. {@code 22:00–06:00} po půlnoci).
     *
     * @param model            HDO záznam
     * @param type             typ události ({@link #TYPE_START} nebo {@link #TYPE_END})
     * @param nowMillis        aktuální reálný čas v milisekundách
     * @param timeShift        časový posun elektroměru v milisekundách
     * @param maxDaysLookahead maximální počet dní dopředu pro hledání
     * @param ignoreDateRange  {@code true}, pokud se nemá filtrovat podle {@code dateFrom}–{@code dateUntil}
     * @return reálný čas přepnutí v milisekundách, nebo {@code -1L}
     */
    private static long findNextTrigger(HdoModel model,
                                        int type,
                                        long nowMillis,
                                        long timeShift,
                                        int maxDaysLookahead,
                                        boolean ignoreDateRange) {
        long meterNowMillis = nowMillis + timeShift;

        Calendar startOfToday = Calendar.getInstance();
        startOfToday.setTimeInMillis(meterNowMillis);
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        int startOffset = (type == TYPE_END) ? -1 : 0;
        for (int offset = startOffset; offset <= maxDaysLookahead; offset++) {
            Calendar day = (Calendar) startOfToday.clone();
            day.add(Calendar.DAY_OF_YEAR, offset);

            boolean isHolidayDay = Connections.isCzechHoliday(day);
            boolean modelIsHoliday = model.getSv() == 1 || "SVÁTEK".equalsIgnoreCase(model.getDateFrom());

            if (!modelIsHoliday || !isHolidayDay) {
                if (modelIsHoliday) continue;
                if (!ignoreDateRange) {
                    Date[] range = cz.xlisto.elektrodroid.modules.hdo.HdoUpdateHelper.parseDateRange(model.getDateFrom(), model.getDateUntil(), day.getTime());
                    Date dateFrom = range != null ? range[0] : null;
                    Date dateUntil = range != null ? range[1] : null;
                    if (!isDateInRange(day.getTime(), dateFrom, dateUntil)) {
                        continue;
                    }
                }
                if (!isDayEnabled(model, day.get(Calendar.DAY_OF_WEEK))) {
                    continue;
                }
            }

            Calendar start = applyTime(day, model.getTimeFrom());
            Calendar candidate;
            if (type == TYPE_START) {
                candidate = start;
            } else {
                candidate = applyTime(day, model.getTimeUntil());
                if (!candidate.after(start)) {
                    candidate.add(Calendar.DAY_OF_YEAR, 1);
                }
            }

            long meterCandidateMillis = candidate.getTimeInMillis();
            if (meterCandidateMillis > meterNowMillis) {
                return meterCandidateMillis - timeShift;
            }
        }
        return -1L;
    }

    /**
     * Ověří, zda je HDO aktivní pro zadaný den v týdnu.
     */
    private static boolean isDayEnabled(HdoModel model, int dayOfWeek) {
        return switch (dayOfWeek) {
            case Calendar.MONDAY -> model.getMon() == 1;
            case Calendar.TUESDAY -> model.getTue() == 1;
            case Calendar.WEDNESDAY -> model.getWed() == 1;
            case Calendar.THURSDAY -> model.getThu() == 1;
            case Calendar.FRIDAY -> model.getFri() == 1;
            case Calendar.SATURDAY -> model.getSat() == 1;
            case Calendar.SUNDAY -> model.getSun() == 1;
            default -> false;
        };
    }

    /**
     * Aplikuje čas HH:mm na zadaný den a vrátí nový kalendář.
     */
    private static Calendar applyTime(Calendar day, String hhmm) {
        Calendar out = (Calendar) day.clone();
        int hour = 0;
        int minute = 0;
        try {
            String[] parts = hhmm.split(":");
            if (parts.length == 2) {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {
        }

        if (hour == 24) {
            hour = 0;
            out.add(Calendar.DAY_OF_YEAR, 1);
        }

        out.set(Calendar.HOUR_OF_DAY, hour);
        out.set(Calendar.MINUTE, minute);
        out.set(Calendar.SECOND, 0);
        out.set(Calendar.MILLISECOND, 0);
        return out;
    }

    /**
     * Ověří, že datum spadá do intervalu platnosti HDO.
     */
    private static boolean isDateInRange(Date date, Date from, Date until) {
        if (from != null && date.before(startOfDay(from))) {
            return false;
        }
        return until == null || !date.after(endOfDay(until));
    }

    /**
     * Vrátí začátek dne pro zadané datum.
     */
    private static Date startOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Vrátí konec dne pro zadané datum.
     */
    private static Date endOfDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }
}
