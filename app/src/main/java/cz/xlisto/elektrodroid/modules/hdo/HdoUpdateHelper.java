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

                String dateUntilStr = models.get(0).getDateUntil();
                Log.d(TAG, "HDO dateUntilStr=" + dateUntilStr);
                Date dateUntil = parseDate(dateUntilStr);
                if (dateUntil != null) {
                    Calendar untilCal = Calendar.getInstance();
                    untilCal.setTime(dateUntil);
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
                    Log.d(TAG, "Failed to parse dateUntilStr: " + dateUntilStr);
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
        String dateUntilStr = models.get(0).getDateUntil();
        Date dateUntil = parseDate(dateUntilStr);
        if (dateUntil == null) {
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar untilCal = Calendar.getInstance();
        untilCal.setTime(dateUntil);
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
            Date newDateFrom = parseDate(newModels.get(0).getDateFrom());
            if (newDateFrom != null) {
                Calendar fromCal = Calendar.getInstance();
                fromCal.setTime(newDateFrom);
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
            Date until = parseDate(old.getDateUntil());
            if (until != null) {
                Calendar untilCal = Calendar.getInstance();
                untilCal.setTime(until);
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
     * Parsuje textové datum ve formátu dd.MM.yyyy, dd.MM, dd.M. atd.
     * Pokud rok chybí (např. u EGD ve tvaru "31.08", "31.8." nebo "31.8.9999"),
     * odvodí správný rok na základě aktuálního data.
     *
     * @param value Textový řetězec data
     * @return Objekt Date nebo null při chybě
     */
    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty() || "0".equals(value)) {
            return null;
        }

        // Ošetření EGD formátů bez roku nebo s placeholder rokem (9999, 1900)
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
                        int currentYear = cal.get(Calendar.YEAR);
                        cal.setTime(date);
                        cal.set(Calendar.YEAR, currentYear);

                        // Heuristika pro data bez roku: pokud je datum v aktuálním roce
                        // více než 6 měsíců v budoucnosti, pravděpodobně patřilo do předchozího roku.
                        Calendar today = Calendar.getInstance();
                        long diffMillis = cal.getTimeInMillis() - today.getTimeInMillis();
                        long sixMonthsMillis = 180L * 24L * 60L * 60L * 1000L;
                        if (diffMillis > sixMonthsMillis) {
                            cal.set(Calendar.YEAR, currentYear - 1);
                        }
                        return cal.getTime();
                    }
                    return date;
                }
            } catch (ParseException ignored) {
            }
        }
        Log.e(TAG, "parseDate: Nelze parsovat datum " + value);
        return null;
    }

}
