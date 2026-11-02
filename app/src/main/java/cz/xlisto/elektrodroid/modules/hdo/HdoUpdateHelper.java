package cz.xlisto.elektrodroid.modules.hdo;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.HdoOldReminderDialogFragment;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.services.HdoNotice;
import cz.xlisto.elektrodroid.shp.ShPHdo;


/**
 * Třída HdoUpdateHelper zajišťuje kontrolu platnosti HDO kódů,
 * správu budoucích a prošlých kódů v databázi, přenos nastavení notifikací
 * a detekci změn v časech HDO.
 * <p>
 * Xlisto 20.02.2025
 */
public class HdoUpdateHelper {

    private static final String TAG = "HdoUpdateHelper";


    /**
     * Rozhraní pro zpětné volání při detekci rozdílů v časech HDO (např. 30-60 minut)
     * a nutnosti dotázat se uživatele na přenos/úpravu notifikací.
     */
    public interface OnHdoTimeDifferenceListener {

        void onTimeDifferenceDetected(HdoModel oldModel, HdoModel newModel, int diffMinutes);

    }


    /**
     * Zkontroluje platnost HDO kódů pro všechna odběrná místa při startu aplikace
     * a v případě prošlých kódů (a pokud není odloženo nebo zakázáno) zobrazí dialog
     * s možností stažení, odložení nebo trvalého vypnutí.
     * <p>
     * Metoda bere v potaz, že každé odběrné místo může mít vlastní HDO tabulku,
     * případně žádné HDO časy uloženy nemá (prázdná tabulka či neexistující název).
     *
     * @param activity FragmentActivity aktivita
     */
    public static void checkHdoValidityAndShowDialogIfNeeded(FragmentActivity activity) {
        ShPHdo shPHdo = new ShPHdo(activity);
        boolean disabled = shPHdo.get(ShPHdo.HDO_OLD_REMINDER_DISABLED, false);
        Log.d(TAG, "checkHdoValidityAndShowDialogIfNeeded: disabled=" + disabled);
        if (disabled) {
            checkAllHdoValidityAndNotify(activity);
            return;
        }

        long snoozeUntil = shPHdo.get(ShPHdo.HDO_OLD_REMINDER_SNOOZE_UNTIL, 0L);
        Log.d(TAG, "checkHdoValidityAndShowDialogIfNeeded: snoozeUntil=" + snoozeUntil + ", now=" + System.currentTimeMillis());
        if (System.currentTimeMillis() < snoozeUntil) {
            checkAllHdoValidityAndNotify(activity);
            return;
        }

        DataSubscriptionPointSource source = new DataSubscriptionPointSource(activity);
        source.open();
        ArrayList<SubscriptionPointModel> points = source.loadSubscriptionPoints();
        source.close();
        Log.d(TAG, "checkHdoValidityAndShowDialogIfNeeded: loaded subscription points count=" + points.size());

        boolean dialogShown = false;
        for (SubscriptionPointModel point : points) {
            String table = point.getTableHDO();
            Log.d(TAG, "Checking subscription point: " + point.getName() + ", table=" + table);
            if (table != null && !table.isEmpty()) {
                DataHdoSource hdoSource = new DataHdoSource(activity);
                hdoSource.open();
                ArrayList<HdoModel> models = hdoSource.loadHdo(table);
                hdoSource.close();
                Log.d(TAG, "Loaded HDO models count=" + models.size() + " for table " + table);

                // Pokud odběrné místo nemá žádné HDO časy, přeskočíme ho
                if (models.isEmpty()) {
                    continue;
                }

                String dateFromStr = models.get(0).getDateFrom();
                String dateUntilStr = models.get(0).getDateUntil();
                Log.d(TAG, "HDO dateFromStr=" + dateFromStr + ", dateUntilStr=" + dateUntilStr);
                Date[] range = parseDateRange(dateFromStr, dateUntilStr);
                if (range != null && range[1] != null) {
                    Calendar untilCal = Calendar.getInstance();
                    untilCal.setTime(range[1]);
                    setStartOfDay(untilCal);

                    Calendar todayCal = Calendar.getInstance();
                    setStartOfDay(todayCal);

                    Log.d(TAG, "Comparing untilCal=" + untilCal.getTime() + " with todayCal=" + todayCal.getTime());

                    // Pokud je datum konce platnosti před dnešním dnem, kód je starý/prošlý
                    if (untilCal.before(todayCal)) {
                        Log.d(TAG, "HDO code is expired! Showing dialog for place: " + point.getName());
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                HdoOldReminderDialogFragment.newInstance(point.getName())
                                        .show(activity.getSupportFragmentManager(), "hdoOldReminderDialog");
                            }
                        });
                        dialogShown = true;
                        break;
                    }
                } else {
                    Log.d(TAG, "Failed to parse date range: " + dateFromStr + " - " + dateUntilStr);
                }
            }
        }

        if (!dialogShown) {
            checkAllHdoValidityAndNotify(activity);
        }
    }


    /**
     * Zkontroluje platnost HDO kódů pro všechna odběrná místa a v případě
     * prošlé platnosti nebo posledního dne platnosti (mezi 16:00 a 20:00)
     * zobrazí příslušnou notifikaci.
     *
     * @param context Kontext aplikace
     */
    public static void checkAllHdoValidityAndNotify(Context context) {
        DataSubscriptionPointSource source = new DataSubscriptionPointSource(context);
        source.open();
        ArrayList<SubscriptionPointModel> points = source.loadSubscriptionPoints();
        source.close();

        for (SubscriptionPointModel point : points) {
            String table = point.getTableHDO();
            if (table != null && !table.isEmpty()) {
                checkTableValidityAndNotify(context, table, point.getName(), point.getId());
            }
        }
    }


    /**
     * Zkontroluje platnost HDO kódů v dané tabulce a vyvolá notifikaci.
     *
     * @param context             Kontext aplikace
     * @param table               Název HDO tabulky
     * @param placeName           Název odběrného místa
     * @param subscriptionPointId ID odběrného místa
     */
    public static void checkTableValidityAndNotify(Context context, String table, String placeName, long subscriptionPointId) {
        DataHdoSource source = new DataHdoSource(context);
        source.open();
        ArrayList<HdoModel> models = source.loadHdo(table);
        source.close();

        if (models.isEmpty()) {
            return;
        }

        // Získáme datum konce platnosti z prvního záznamu (případně projdeme všechny)
        String dateFromStr = models.get(0).getDateFrom();
        String dateUntilStr = models.get(0).getDateUntil();
        Date[] range = parseDateRange(dateFromStr, dateUntilStr);
        if (range == null || range[1] == null) {
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar untilCal = Calendar.getInstance();
        untilCal.setTime(range[1]);
        setStartOfDay(untilCal);

        Calendar todayCal = Calendar.getInstance();
        setStartOfDay(todayCal);

        // 1. Kontrola, zda je kód již starý (prošlý) -> dateUntil < today
        if (untilCal.before(todayCal)) {
            String title = context.getString(R.string.hdo_notification_expired_title);
            String content = placeName.isEmpty()
                    ? context.getString(R.string.hdo_notification_expired_message)
                    : context.getString(R.string.hdo_notification_expired_message_place, placeName);
            HdoNotice.setNotice(context, title, context.getString(R.string.hdo_service_name), content, subscriptionPointId);
            return;
        }

        // 2. Kontrola, zda je poslední den platnosti (dateUntil == today) a čas je mezi 16:00 a 20:00
        if (untilCal.equals(todayCal)) {
            int hour = now.get(Calendar.HOUR_OF_DAY);
            if (hour >= 16 && hour <= 20) {
                String title = context.getString(R.string.hdo_notification_last_day_title);
                String content = placeName.isEmpty()
                        ? context.getString(R.string.hdo_notification_last_day_message)
                        : context.getString(R.string.hdo_notification_last_day_message_place, placeName);
                HdoNotice.setNotice(context, title, context.getString(R.string.hdo_service_name), content, subscriptionPointId);
            }
        }
    }


    /**
     * Inteligentně uloží nové HDO kódy do databáze.
     * Zohledňuje stávající kódy, budoucí platnosti, nahrazení prošlých kódů
     * a přenos nastavení notifikací.
     *
     * @param context   Kontext aplikace
     * @param newModels Seznam nových HDO modelů
     * @param table     Název HDO tabulky
     * @param listener  Volitelný poslech pro detekci rozdílů v časech
     */
    public static void smartSaveHdo(Context context, ArrayList<HdoModel> newModels, String table, OnHdoTimeDifferenceListener listener) {
        ArrayList<HdoModel> oldModels;
        DataHdoSource loadSource = new DataHdoSource(context);
        loadSource.open();
        oldModels = loadSource.loadHdo(table);
        loadSource.close();

        Calendar today = Calendar.getInstance();
        setStartOfDay(today);

        // Zjistíme, zda nové modely jsou pro budoucí období
        boolean isFuture = false;
        if (!newModels.isEmpty()) {
            Date[] newRange = parseDateRange(newModels.get(0).getDateFrom(), newModels.get(0).getDateUntil());
            if (newRange != null && newRange[0] != null) {
                Calendar fromCal = Calendar.getInstance();
                fromCal.setTime(newRange[0]);
                setStartOfDay(fromCal);
                if (fromCal.after(today)) {
                    isFuture = true;
                }
            }
        }
        final boolean isFutureCode = isFuture;

        // Přenos nebo kontrola nastavení notifikací
        for (HdoModel newModel : newModels) {
            for (HdoModel oldModel : oldModels) {
                // Porovnáme relé a časy pro nalezení odpovídajícího intervalu
                if (oldModel.getRele().equals(newModel.getRele())) {
                    int diffStart = calculateTimeDifferenceMinutes(oldModel.getTimeFrom(), newModel.getTimeFrom());
                    int diffEnd = calculateTimeDifferenceMinutes(oldModel.getTimeUntil(), newModel.getTimeUntil());

                    if (diffStart == 0 && diffEnd == 0) {
                        // Pokud jsou časy totožné a starý model měl zapnutou notifikaci, přeneseme ji
                        if (oldModel.getNotifyStart() == 1 || oldModel.getNotifyEnd() == 1) {
                            newModel.setNotifyStart(oldModel.getNotifyStart());
                            newModel.setNotifyEnd(oldModel.getNotifyEnd());
                            break; // Našli jsme přesnou shodu časů pro tento interval
                        }
                    } else if ((Math.abs(diffStart) <= 60 || Math.abs(diffEnd) <= 60) && (oldModel.getNotifyStart() == 1 || oldModel.getNotifyEnd() == 1)) {
                        // Rozdíl např. 30-60 minut -> upozorníme uživatele
                        if (listener != null) {
                            listener.onTimeDifferenceDetected(oldModel, newModel, Math.max(Math.abs(diffStart), Math.abs(diffEnd)));
                        }
                        break;
                    }
                }
            }
        }

        // Rozhodnutí o uložení:
        boolean hasValidOldCodes = false;
        for (HdoModel old : oldModels) {
            Date[] oldRange = parseDateRange(old.getDateFrom(), old.getDateUntil());
            if (oldRange != null && oldRange[1] != null) {
                Calendar untilCal = Calendar.getInstance();
                untilCal.setTime(oldRange[1]);
                setStartOfDay(untilCal);
                if (!untilCal.before(today)) {
                    hasValidOldCodes = true;
                    break;
                }
            }
        }

        DataHdoSource saveSource = new DataHdoSource(context);
        if (hasValidOldCodes && isFutureCode) {
            // Uložíme nové budoucí kódy do databáze vedle stávajících
            for (HdoModel model : newModels) {
                saveSource.saveHdo(model, table);
            }
        } else {
            // Standardní uložení (nahrazení starých, např. prošlých kódů)
            saveSource.saveHdo(newModels, table);
        }
    }


    /**
     * Vypočítá rozdíl mezi dvěma časy ve formátu "HH:mm" v minutách.
     *
     * @param time1 První čas
     * @param time2 Druhý čas
     * @return Rozdíl v minutách
     */
    private static int calculateTimeDifferenceMinutes(String time1, String time2) {
        try {
            String[] p1 = time1.split(":");
            String[] p2 = time2.split(":");
            int m1 = Integer.parseInt(p1[0]) * 60 + Integer.parseInt(p1[1]);
            int m2 = Integer.parseInt(p2[0]) * 60 + Integer.parseInt(p2[1]);
            return m1 - m2;
        } catch (Exception e) {
            return 999; // Neznámý rozdíl
        }
    }


    /**
     * Převede datum na začátek dne (00:00:00.000).
     *
     * @param calendar Kalendář k úpravě
     */
    private static void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }


    /**
     * Parsuje dvojici dat platnosti HDO (začátek a konec) s použitím aktuálního data jako referenčního.
     *
     * @param dateFromStr  Datum od (např. "01.11.", "01.09.2026", "SVÁTEK")
     * @param dateUntilStr Datum do (např. "31.01.", "31.10.2026")
     * @return Pole Date[] o dvou prvcích [dateFrom, dateUntil], nebo null pokud obě data chybí
     */
    public static Date[] parseDateRange(String dateFromStr, String dateUntilStr) {
        return parseDateRange(dateFromStr, dateUntilStr, null);
    }


    /**
     * Parsuje dvojici dat platnosti HDO (začátek a konec) s ohledem na referenční datum.
     * Vyhodnocuje správně formáty bez roku (např. u EGD "01.11." až "31.01.") a ošetřuje přechod přes konec roku.
     *
     * @param dateFromStr   Datum od (např. "01.11.", "01.09.2026", "SVÁTEK")
     * @param dateUntilStr  Datum do (např. "31.01.", "31.10.2026")
     * @param referenceDate Referenční datum k určení příslušného roku, nebo null pro aktuální datum
     * @return Pole Date[] o dvou prvcích [dateFrom, dateUntil], nebo null pokud obě data chybí
     */
    public static Date[] parseDateRange(String dateFromStr, String dateUntilStr, Date referenceDate) {
        if ((dateFromStr == null || dateFromStr.trim().isEmpty() || "0".equals(dateFromStr) || "SVÁTEK".equalsIgnoreCase(dateFromStr))
                && (dateUntilStr == null || dateUntilStr.trim().isEmpty() || "0".equals(dateUntilStr))) {
            return null;
        }

        Date refDate = referenceDate != null ? referenceDate : new Date();
        Calendar refCal = Calendar.getInstance();
        refCal.setTime(refDate);

        Date fromExact = parseSingleDateWithYear(dateFromStr);
        Date untilExact = parseSingleDateWithYear(dateUntilStr);

        if (fromExact != null && untilExact != null) {
            return new Date[]{fromExact, untilExact};
        }

        int[] fromDM = extractDayAndMonth(dateFromStr);
        int[] untilDM = extractDayAndMonth(dateUntilStr);

        if (fromDM == null && untilDM == null) {
            Date f = fromExact != null ? fromExact : parseDateSingle(dateFromStr, refCal);
            Date u = untilExact != null ? untilExact : parseDateSingle(dateUntilStr, refCal);
            if (f == null && u == null) return null;
            return new Date[]{f, u};
        }

        if (fromExact != null && untilDM != null) {
            Calendar cFrom = Calendar.getInstance();
            cFrom.setTime(fromExact);
            int yearFrom = cFrom.get(Calendar.YEAR);
            int monthFrom = cFrom.get(Calendar.MONTH) + 1;
            int dayFrom = cFrom.get(Calendar.DAY_OF_MONTH);

            boolean crossesNewYear = (monthFrom > untilDM[1]) || (monthFrom == untilDM[1] && dayFrom > untilDM[0]);
            int yearUntil = crossesNewYear ? yearFrom + 1 : yearFrom;

            Calendar cUntil = Calendar.getInstance();
            cUntil.set(yearUntil, untilDM[1] - 1, untilDM[0], 23, 59, 59);
            cUntil.set(Calendar.MILLISECOND, 999);
            return new Date[]{fromExact, cUntil.getTime()};
        }

        if (untilExact != null && fromDM != null) {
            Calendar cUntil = Calendar.getInstance();
            cUntil.setTime(untilExact);
            int yearUntil = cUntil.get(Calendar.YEAR);
            int monthUntil = cUntil.get(Calendar.MONTH) + 1;
            int dayUntil = cUntil.get(Calendar.DAY_OF_MONTH);

            boolean crossesNewYear = (fromDM[1] > monthUntil) || (fromDM[1] == monthUntil && fromDM[0] > dayUntil);
            int yearFrom = crossesNewYear ? yearUntil - 1 : yearUntil;

            Calendar cFrom = Calendar.getInstance();
            cFrom.set(yearFrom, fromDM[1] - 1, fromDM[0], 0, 0, 0);
            cFrom.set(Calendar.MILLISECOND, 0);
            return new Date[]{cFrom.getTime(), untilExact};
        }

        if (fromDM != null && untilDM != null) {
            int monthFrom = fromDM[1];
            int dayFrom = fromDM[0];
            int monthUntil = untilDM[1];
            int dayUntil = untilDM[0];

            boolean crossesNewYear = (monthFrom > monthUntil) || (monthFrom == monthUntil && dayFrom > dayUntil);

            int currentYear = refCal.get(Calendar.YEAR);
            int[] candidateYears = new int[]{currentYear - 1, currentYear, currentYear + 1};

            Calendar bestFrom = null;
            Calendar bestUntil = null;
            long minDistance = Long.MAX_VALUE;

            long refMs = refCal.getTimeInMillis();

            for (int Y : candidateYears) {
                Calendar cFrom = Calendar.getInstance();
                cFrom.set(Y, monthFrom - 1, dayFrom, 0, 0, 0);
                cFrom.set(Calendar.MILLISECOND, 0);

                Calendar cUntil = Calendar.getInstance();
                int Yuntil = crossesNewYear ? Y + 1 : Y;
                cUntil.set(Yuntil, monthUntil - 1, dayUntil, 23, 59, 59);
                cUntil.set(Calendar.MILLISECOND, 999);

                long fromMs = cFrom.getTimeInMillis();
                long untilMs = cUntil.getTimeInMillis();

                if (fromMs <= refMs && refMs <= untilMs) {
                    bestFrom = cFrom;
                    bestUntil = cUntil;
                    break;
                }

                long dist;
                if (refMs < fromMs) {
                    dist = fromMs - refMs;
                } else {
                    dist = refMs - untilMs;
                }

                if (dist < minDistance) {
                    minDistance = dist;
                    bestFrom = cFrom;
                    bestUntil = cUntil;
                }
            }

            return new Date[]{bestFrom != null ? bestFrom.getTime() : null, bestUntil != null ? bestUntil.getTime() : null};
        }

        Date f = fromDM != null ? parseDayMonthWithRefYear(fromDM[0], fromDM[1], refCal, false) : parseDateSingle(dateFromStr, refCal);
        Date u = untilDM != null ? parseDayMonthWithRefYear(untilDM[0], untilDM[1], refCal, true) : parseDateSingle(dateUntilStr, refCal);
        return new Date[]{f, u};
    }


    private static Date parseDayMonthWithRefYear(int day, int month, Calendar refCal, boolean isEnd) {
        int year = refCal.get(Calendar.YEAR);
        Calendar c = Calendar.getInstance();
        if (isEnd) {
            c.set(year, month - 1, day, 23, 59, 59);
            c.set(Calendar.MILLISECOND, 999);
        } else {
            c.set(year, month - 1, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
        }
        return c.getTime();
    }


    private static Date parseSingleDateWithYear(String value) {
        if (value == null || value.trim().isEmpty() || "0".equals(value) || "SVÁTEK".equalsIgnoreCase(value)) {
            return null;
        }

        String cleaned = value.trim()
                .replace(".9999", "")
                .replace(".1900", "")
                .replace("9999", "")
                .replace("1900", "");

        String[] parts = cleaned.split("\\.");
        if (parts.length < 3 || parts[2].trim().isEmpty()) {
            return null;
        }

        String[] formatsWithYear = {"d.M.yyyy", "d. M. yyyy", "yyyy-MM-dd", "d.M.yy", "d. M. yy"};
        for (String fmt : formatsWithYear) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                Date d = sdf.parse(cleaned);
                if (d != null) {
                    return d;
                }
            } catch (ParseException ignored) {
            }
        }
        return null;
    }


    private static int[] extractDayAndMonth(String value) {
        if (value == null || value.trim().isEmpty() || "0".equals(value) || "SVÁTEK".equalsIgnoreCase(value)) {
            return null;
        }

        String cleaned = value.trim()
                .replace(".9999", ".")
                .replace(".1900", ".")
                .replace("9999", "")
                .replace("1900", "");

        String[] formatsNoYear = {"d.M.", "d. M.", "d.M", "d. M", "dd.MM", "dd. MM"};
        for (String fmt : formatsNoYear) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                Date d = sdf.parse(cleaned);
                if (d != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    return new int[]{c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1};
                }
            } catch (ParseException ignored) {
            }
        }

        try {
            String[] parts = cleaned.split("\\.");
            if (parts.length >= 2) {
                int day = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());
                if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
                    return new int[]{day, month};
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }


    private static Date parseDateSingle(String value, Calendar refCal) {
        if (value == null || value.trim().isEmpty() || "0".equals(value)) {
            return null;
        }

        String cleanedValue = value.trim()
                .replace(".9999", ".")
                .replace(".1900", ".")
                .replace("9999", "")
                .replace("1900", "");

        String[] formats = {"d.M.yyyy", "d. M. yyyy", "yyyy-MM-dd", "d.M.", "d. M.", "d.M", "d. M", "dd.MM", "dd. MM", "d.M.yy"};
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                Date date = sdf.parse(cleanedValue);
                if (date != null) {
                    if (!format.contains("yyyy") && !format.contains("yy")) {
                        Calendar cal = Calendar.getInstance();
                        int currentYear = refCal.get(Calendar.YEAR);
                        cal.setTime(date);
                        cal.set(Calendar.YEAR, currentYear);
                        return cal.getTime();
                    }
                    return date;
                }
            } catch (ParseException ignored) {
            }
        }
        Log.e(TAG, "parseDateSingle: Nelze parsovat datum " + value);
        return null;
    }

}
