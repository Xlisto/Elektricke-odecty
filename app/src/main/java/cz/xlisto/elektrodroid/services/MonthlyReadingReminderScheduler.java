package cz.xlisto.elektrodroid.services;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Scheduler připomínek pro měsíční odečty.
 */
public final class MonthlyReadingReminderScheduler {

    public static final String ACTION_MONTHLY_READING_REMINDER = "cz.xlisto.elektrodroid.action.MONTHLY_READING_REMINDER";
    public static final String EXTRA_TABLE = "extra_table";
    public static final String EXTRA_SUBSCRIPTION_POINT_ID = "extra_subscription_point_id";
    public static final String EXTRA_SCHEDULED_TIME = "extra_scheduled_time";
    
    public static final int FREQUENCY_WEEKLY = 1;

    private static final String TAG = "MonthlyReadReminder";
    private static final int REQUEST_CODE = 41001;
    private static final int LOOKAHEAD_MONTHS = 48;
    private static final int LOOKAHEAD_WEEKS = 104;


    private MonthlyReadingReminderScheduler() {
    }


    /**
     * Přeplánuje připomínku pro aktuálně vybrané odběrné místo mimo UI vlákno.
     */
    public static void rescheduleCurrentAsync(Context context) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> rescheduleCurrent(appContext), "monthly-reading-reminder").start();
    }


    /**
     * Přeplánuje připomínku pro aktuálně vybrané odběrné místo.
     */
    public static void rescheduleCurrent(Context context) {
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(context);
        if (subscriptionPoint == null) {
            cancel(context);
            return;
        }
        rescheduleForSubscriptionPoint(context, subscriptionPoint);
    }


    /**
     * Přeplánuje připomínku pro konkrétní odběrné místo.
     */
    public static void rescheduleForSubscriptionPoint(Context context, SubscriptionPointModel subscriptionPoint) {
        if (subscriptionPoint == null) {
            cancel(context);
            return;
        }

        long subscriptionPointId = subscriptionPoint.getId();
        DataSettingsSource settingsSource = new DataSettingsSource(context);
        settingsSource.open();
        int frequency;
        int dayOfMonth;
        int dayOfWeek;
        int[] time;
        try {
            if (!settingsSource.loadReadingNotificationEnabled(subscriptionPointId)) {
                cancel(context);
                return;
            }

            frequency = settingsSource.loadReadingNotificationFrequency(subscriptionPointId);
            dayOfMonth = parseInt(settingsSource.loadReadingNotificationDayOfMonth(subscriptionPointId));
            dayOfWeek = settingsSource.loadReadingNotificationDayOfWeek(subscriptionPointId);
            time = parseTime(settingsSource.loadReadingNotificationTime(subscriptionPointId));
        } finally {
            settingsSource.close();
        }

        long triggerAtMillis = findNextTrigger(context, subscriptionPoint.getTableO(), frequency, dayOfMonth, dayOfWeek, time[0], time[1], System.currentTimeMillis());
        if (triggerAtMillis <= 0L) {
            cancel(context);
            return;
        }

        scheduleAlarm(context, subscriptionPoint, triggerAtMillis);
    }


    /**
     * Zjistí, zda už pro aktuální relevantní období existuje zapsaný odečet.
     */
    public static boolean hasReadingForCurrentPeriod(Context context, SubscriptionPointModel subscriptionPoint) {
        if (subscriptionPoint == null) {
            return false;
        }

        DataMonthlyReadingSource source = new DataMonthlyReadingSource(context);
        source.open();
        try {
            Calendar from = Calendar.getInstance();
            from.set(Calendar.DAY_OF_MONTH, 1);
            from.set(Calendar.HOUR_OF_DAY, 0);
            from.set(Calendar.MINUTE, 0);
            from.set(Calendar.SECOND, 0);
            from.set(Calendar.MILLISECOND, 0);

            Calendar to = (Calendar) from.clone();
            to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
            to.set(Calendar.HOUR_OF_DAY, 23);
            to.set(Calendar.MINUTE, 59);
            to.set(Calendar.SECOND, 59);
            to.set(Calendar.MILLISECOND, 999);

            return source.hasMonthlyReadingInPeriod(subscriptionPoint.getTableO(), from.getTimeInMillis(), to.getTimeInMillis());
        } finally {
            source.close();
        }
    }


    /**
     * Zruší připomínku měsíčního odečtu.
     */
    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, null, -1L, 0L, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }


    static long findNextTrigger(Context context, String table, int frequency, int dayOfMonth, int dayOfWeek, int hourOfDay, int minute, long nowMillis) {
        if (frequency == FREQUENCY_WEEKLY) {
            return findNextWeeklyTrigger(context, table, dayOfWeek, hourOfDay, minute, nowMillis);
        }
        return findNextMonthlyTrigger(context, table, dayOfMonth, hourOfDay, minute, nowMillis);
    }


    private static long findNextMonthlyTrigger(Context context, String table, int requestedDayOfMonth, int hourOfDay, int minute, long nowMillis) {
        DataMonthlyReadingSource source = new DataMonthlyReadingSource(context);
        source.open();
        try {
            Calendar cursor = Calendar.getInstance();
            cursor.setTimeInMillis(nowMillis);
            cursor.set(Calendar.DAY_OF_MONTH, 1);
            setStartOfDay(cursor);

            for (int i = 0; i < LOOKAHEAD_MONTHS; i++) {
                Calendar trigger = createMonthlyCandidate(cursor, requestedDayOfMonth, hourOfDay, minute);

                if (trigger.getTimeInMillis() <= nowMillis || hasReadingInMonth(source, table, trigger)) {
                    cursor.add(Calendar.MONTH, 1);
                    cursor.set(Calendar.DAY_OF_MONTH, 1);
                    setStartOfDay(cursor);
                    continue;
                }
                return trigger.getTimeInMillis();
            }
            return -1L;
        } finally {
            source.close();
        }
    }


    private static long findNextWeeklyTrigger(Context context, String table, int dayOfWeekIndex, int hourOfDay, int minute, long nowMillis) {
        DataMonthlyReadingSource source = new DataMonthlyReadingSource(context);
        source.open();
        try {
            Calendar trigger = Calendar.getInstance();
            trigger.setTimeInMillis(nowMillis);
            trigger.set(Calendar.HOUR_OF_DAY, hourOfDay);
            trigger.set(Calendar.MINUTE, minute);
            trigger.set(Calendar.SECOND, 0);
            trigger.set(Calendar.MILLISECOND, 0);

            int wanted = mapDayOfWeek(dayOfWeekIndex);
            int current = trigger.get(Calendar.DAY_OF_WEEK);
            int delta = wanted - current;
            if (delta < 0 || (delta == 0 && trigger.getTimeInMillis() <= nowMillis)) {
                delta += 7;
            }
            trigger.add(Calendar.DAY_OF_YEAR, delta);

            for (int i = 0; i < LOOKAHEAD_WEEKS; i++) {
                if (trigger.getTimeInMillis() <= nowMillis || hasReadingInMonth(source, table, trigger)) {
                    trigger.add(Calendar.DAY_OF_YEAR, 7);
                    continue;
                }
                return trigger.getTimeInMillis();
            }
            return -1L;
        } finally {
            source.close();
        }
    }


    private static boolean hasReadingInMonth(DataMonthlyReadingSource source, String table, Calendar trigger) {
        Calendar from = (Calendar) trigger.clone();
        from.set(Calendar.DAY_OF_MONTH, 1);
        setStartOfDay(from);

        Calendar to = (Calendar) from.clone();
        to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
        to.set(Calendar.HOUR_OF_DAY, 23);
        to.set(Calendar.MINUTE, 59);
        to.set(Calendar.SECOND, 59);
        to.set(Calendar.MILLISECOND, 999);

        return source.hasMonthlyReadingInPeriod(table, from.getTimeInMillis(), to.getTimeInMillis());
    }


    private static void scheduleAlarm(Context context, SubscriptionPointModel subscriptionPoint, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, subscriptionPoint.getTableO(), subscriptionPoint.getId(), triggerAtMillis,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                canExact = alarmManager.canScheduleExactAlarms();
            } catch (Exception e) {
                Log.w(TAG, "canScheduleExactAlarms() failed", e);
            }
        }

        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } catch (SecurityException e) {
                Log.w(TAG, "setExactAndAllowWhileIdle failed, fallback to inexact", e);
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }


    private static PendingIntent createPendingIntent(Context context, String table, long subscriptionPointId, long scheduledAtMillis, int flags) {
        Intent intent = new Intent(context, MonthlyReadingReminderReceiver.class);
        intent.setAction(ACTION_MONTHLY_READING_REMINDER);
        intent.putExtra(EXTRA_TABLE, table);
        intent.putExtra(EXTRA_SUBSCRIPTION_POINT_ID, subscriptionPointId);
        intent.putExtra(EXTRA_SCHEDULED_TIME, scheduledAtMillis);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }


    private static void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }


    static Calendar createMonthlyCandidate(Calendar monthStart, int requestedDayOfMonth, int hourOfDay, int minute) {
        Calendar trigger = (Calendar) monthStart.clone();
        trigger.set(Calendar.DAY_OF_MONTH, resolveTargetDayOfMonth(requestedDayOfMonth, trigger.getActualMaximum(Calendar.DAY_OF_MONTH)));
        trigger.set(Calendar.HOUR_OF_DAY, hourOfDay);
        trigger.set(Calendar.MINUTE, minute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        return trigger;
    }


    static int resolveTargetDayOfMonth(int requestedDayOfMonth, int maxDayOfMonth) {
        return Math.min(Math.max(requestedDayOfMonth, 1), maxDayOfMonth);
    }


    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }


    private static int[] parseTime(String value) {
        int hour = 8;
        int minute = 0;
        if (value != null) {
            String[] parts = value.trim().split(":");
            if (parts.length == 2) {
                try {
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    hour = 8;
                }
            }
        }
        return new int[]{hour, minute};
    }


    private static int mapDayOfWeek(int index) {
        return switch (index) {
            case 0 -> Calendar.MONDAY;
            case 1 -> Calendar.TUESDAY;
            case 2 -> Calendar.WEDNESDAY;
            case 3 -> Calendar.THURSDAY;
            case 4 -> Calendar.FRIDAY;
            case 5 -> Calendar.SATURDAY;
            default -> Calendar.SUNDAY;
        };
    }

}


