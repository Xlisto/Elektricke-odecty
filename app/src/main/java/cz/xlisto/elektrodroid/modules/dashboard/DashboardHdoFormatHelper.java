package cz.xlisto.elektrodroid.modules.dashboard;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Pomocná třída pro jednoznačné formátování času dalšího přepnutí HDO
 * a zbývající doby do přepnutí v českém prostředí.
 * <p>
 * Odděluje absolutní čas přepnutí (např. {@code 21:41} nebo {@code Čt 01:30})
 * od relativního odpočtu (např. {@code 2 h 15 min}) a bezpečně ošetřuje neplatné
 * nebo chybějící časy.
 */
public final class DashboardHdoFormatHelper {

    private static final Locale CZECH_LOCALE = Locale.forLanguageTag("cs-CZ");
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR;


    private DashboardHdoFormatHelper() {
    }


    /**
     * Výsledek formátování následujícího přepnutí HDO.
     *
     * @param valid         {@code true}, pokud je k dispozici platný budoucí čas přepnutí
     * @param isToday       {@code true}, pokud přepnutí nastane ve stejný kalendářní den jako referenční čas
     * @param timeText      naformátovaný absolutní čas přepnutí ({@code HH:mm} pro dnešek, jinak {@code Den HH:mm})
     * @param remainingText naformátovaná zbývající doba do přepnutí (např. {@code 2 h 15 min}), nebo {@code null}
     */
    public record FormattedHdoSwitch(boolean valid,
                                     boolean isToday,
                                     @NonNull String timeText,
                                     @Nullable String remainingText) {

        /**
         * Vrátí neplatný výsledek pro stav bez dostupného budoucího přepnutí HDO.
         *
         * @return neplatná instance {@link FormattedHdoSwitch}
         */
        @NonNull
        public static FormattedHdoSwitch invalid() {
            return new FormattedHdoSwitch(false, false, "", null);
        }

    }


    /**
     * Naformátuje čas dalšího přepnutí HDO a relativní odpočet vůči zadanému referenčnímu času
     * v výchozím časovém pásmu zařízení.
     *
     * @param targetDisplayMillis čas dalšího přepnutí v ms (včetně případného posunu času elektroměru)
     * @param referenceNowMillis  aktuální referenční čas v ms (se stejným posunem času elektroměru)
     * @return naformátovaný výsledek {@link FormattedHdoSwitch}
     */
    @NonNull
    public static FormattedHdoSwitch formatNextSwitch(long targetDisplayMillis, long referenceNowMillis) {
        return formatNextSwitch(targetDisplayMillis, referenceNowMillis, TimeZone.getDefault());
    }


    /**
     * Naformátuje čas dalšího přepnutí HDO a relativní odpočet vůči zadanému referenčnímu času
     * v určeném časovém pásmu.
     *
     * @param targetDisplayMillis čas dalšího přepnutí v ms (včetně případného posunu času elektroměru)
     * @param referenceNowMillis  aktuální referenční čas v ms (se stejným posunem času elektroměru)
     * @param timeZone            časové pásmo pro vyhodnocení dne a formátování hodin
     * @return naformátovaný výsledek {@link FormattedHdoSwitch}
     */
    @NonNull
    public static FormattedHdoSwitch formatNextSwitch(long targetDisplayMillis,
                                                      long referenceNowMillis,
                                                      @NonNull TimeZone timeZone) {
        if (referenceNowMillis <= 0L || targetDisplayMillis <= referenceNowMillis) {
            return FormattedHdoSwitch.invalid();
        }

        Calendar refCal = Calendar.getInstance(timeZone, CZECH_LOCALE);
        refCal.setTimeInMillis(referenceNowMillis);

        Calendar targetCal = Calendar.getInstance(timeZone, CZECH_LOCALE);
        targetCal.setTimeInMillis(targetDisplayMillis);

        SimpleDateFormat hhmmFormat = new SimpleDateFormat("HH:mm", CZECH_LOCALE);
        hhmmFormat.setTimeZone(timeZone);
        String hhmm = hhmmFormat.format(targetCal.getTime());

        boolean isToday = refCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
                && refCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR);

        String timeText;
        if (isToday) {
            timeText = hhmm;
        } else {
            String dayAbbr = formatCzechDayOfWeekShort(targetCal.get(Calendar.DAY_OF_WEEK));
            timeText = dayAbbr + " " + hhmm;
        }

        String remainingText = formatRemainingDuration(targetDisplayMillis - referenceNowMillis);
        return new FormattedHdoSwitch(true, isToday, timeText, remainingText);
    }


    /**
     * Převede konstantu dne v týdnu z {@link Calendar} na krátkou českou zkratku
     * s velkým počátečním písmenem ({@code Po}–{@code Ne}).
     *
     * @param dayOfWeek hodnota {@link Calendar#DAY_OF_WEEK}
     * @return krátký český název dne v týdnu
     */
    @NonNull
    public static String formatCzechDayOfWeekShort(int dayOfWeek) {
        return switch (dayOfWeek) {
            case Calendar.MONDAY -> "Po";
            case Calendar.TUESDAY -> "Út";
            case Calendar.WEDNESDAY -> "St";
            case Calendar.THURSDAY -> "Čt";
            case Calendar.FRIDAY -> "Pá";
            case Calendar.SATURDAY -> "So";
            case Calendar.SUNDAY -> "Ne";
            default -> "";
        };
    }


    /**
     * Naformátuje zbývající dobu do přepnutí do srozumitelného českého tvaru
     * (např. {@code 2 h 15 min}, {@code 45 min}, {@code 1 d 3 h 10 min} nebo {@code < 1 min}).
     *
     * @param remainingMillis zbývající doba v milisekundách
     * @return naformátovaný text zbývající doby, nebo {@code null} pro nekladnou hodnotu
     */
    @Nullable
    public static String formatRemainingDuration(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return null;
        }
        if (remainingMillis < MILLIS_PER_MINUTE) {
            return "< 1 min";
        }

        long totalMinutes = remainingMillis / MILLIS_PER_MINUTE;
        long days = totalMinutes / MINUTES_PER_DAY;
        long hours = (totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR;
        StringBuilder sb = getSb(totalMinutes, days, hours);
        return sb.toString();
    }


    @NonNull
    private static StringBuilder getSb(long totalMinutes, long days, long hours) {
        long minutes = totalMinutes % MINUTES_PER_HOUR;

        StringBuilder sb = new StringBuilder();
        boolean hasPart = false;
        if (days > 0L) {
            sb.append(days).append(" d");
            hasPart = true;
        }
        if (hours > 0L) {
            if (hasPart) sb.append(' ');
            sb.append(hours).append(" h");
            hasPart = true;
        }
        if (minutes > 0L || !hasPart) {
            if (hasPart) sb.append(' ');
            sb.append(minutes).append(" min");
        }
        return sb;
    }

}
