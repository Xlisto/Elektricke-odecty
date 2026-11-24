package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Vláknově bezpečná pomocná třída pro formátování cen, spotřeby, finančních částek
 * a časových rozsahů v českém formátu pro domovské widgety.
 */
public final class WidgetFormatHelper {

    private static final Locale CZECH_LOCALE = Locale.forLanguageTag("cs-CZ");


    private WidgetFormatHelper() {
    }


    /**
     * Naformátuje jednotkovou cenu za kWh na 2 desetinná místa (např. {@code 5,42}).
     *
     * @param pricePerKwh cena v Kč/kWh
     * @return formátovaná číselná hodnota bez jednotky
     */
    @NonNull
    public static String formatPriceKwh(double pricePerKwh) {
        double safe = Math.max(0.0, pricePerKwh);
        DecimalFormat df = new DecimalFormat("0.00", createCzechSymbols());
        return df.format(safe);
    }


    /**
     * Naformátuje celočíselnou hodnotu s českým oddělovačem tisíců (např. {@code 12 450}).
     *
     * @param value číselná hodnota
     * @return formátované celé číslo
     */
    @NonNull
    public static String formatInteger(double value) {
        DecimalFormat df = new DecimalFormat("#,##0", createCzechSymbols());
        return df.format(Math.round(value));
    }


    /**
     * Naformátuje spotřebu v kWh včetně jednotky (např. {@code 1 245 kWh}).
     *
     * @param kwh spotřeba v kWh
     * @return formátovaná spotřeba s jednotkou
     */
    @NonNull
    public static String formatKwh(double kwh) {
        return formatInteger(Math.max(0.0, kwh)) + " kWh";
    }


    /**
     * Naformátuje finanční částku v Kč včetně měny (např. {@code 14 500 Kč}).
     *
     * @param czk částka v Kč
     * @return formátovaná částka s měnou
     */
    @NonNull
    public static String formatCzk(double czk) {
        return formatInteger(Math.max(0.0, czk)) + " Kč";
    }


    /**
     * Naformátuje datum do českého formátu {@code dd.MM.yyyy}.
     *
     * @param dateMillis datum v milisekundách
     * @return naformátované datum, nebo prázdný řetězec při neplatném datu
     */
    @NonNull
    public static String formatDate(long dateMillis) {
        return formatDate(dateMillis, TimeZone.getDefault());
    }


    /**
     * Naformátuje datum do českého formátu {@code dd.MM.yyyy} v zadaném časovém pásmu.
     *
     * @param dateMillis datum v milisekundách
     * @param timeZone   časové pásmo
     * @return naformátované datum, nebo prázdný řetězec při neplatném datu
     */
    @NonNull
    public static String formatDate(long dateMillis, @NonNull TimeZone timeZone) {
        if (dateMillis <= 0L) {
            return "";
        }
        SimpleDateFormat fullFormat = new SimpleDateFormat("dd.MM.yyyy", CZECH_LOCALE);
        fullFormat.setTimeZone(timeZone);
        return fullFormat.format(new Date(dateMillis));
    }


    /**
     * Naformátuje zkrácený rozsah sledovaného období. Pokud obě data leží ve stejném kalendářním roce,
     * vynechá rok u počátečního data (např. {@code 01.01. – 24.11.2026}), jinak zobrazí oba roky.
     *
     * @param minDateMillis počáteční datum v milisekundách
     * @param maxDateMillis koncové datum v milisekundách
     * @return naformátovaný textový rozsah, nebo prázdný řetězec při neplatných datech
     */
    @NonNull
    public static String formatShortPeriodRange(long minDateMillis, long maxDateMillis) {
        return formatShortPeriodRange(minDateMillis, maxDateMillis, TimeZone.getDefault());
    }


    /**
     * Naformátuje zkrácený rozsah sledovaného období v zadaném časovém pásmu.
     *
     * @param minDateMillis počáteční datum v milisekundách
     * @param maxDateMillis koncové datum v milisekundách
     * @param timeZone      časové pásmo
     * @return naformátovaný textový rozsah, nebo prázdný řetězec při neplatných datech
     */
    @NonNull
    public static String formatShortPeriodRange(long minDateMillis, long maxDateMillis, @NonNull TimeZone timeZone) {
        if (minDateMillis <= 0L || maxDateMillis <= 0L) {
            return "";
        }
        Calendar cFrom = Calendar.getInstance(timeZone, CZECH_LOCALE);
        cFrom.setTimeInMillis(minDateMillis);
        Calendar cTo = Calendar.getInstance(timeZone, CZECH_LOCALE);
        cTo.setTimeInMillis(maxDateMillis);

        SimpleDateFormat fullFormat = new SimpleDateFormat("dd.MM.yyyy", CZECH_LOCALE);
        fullFormat.setTimeZone(timeZone);
        if (cFrom.get(Calendar.YEAR) == cTo.get(Calendar.YEAR)) {
            SimpleDateFormat dayMonthFormat = new SimpleDateFormat("dd.MM.", CZECH_LOCALE);
            dayMonthFormat.setTimeZone(timeZone);
            return dayMonthFormat.format(new Date(minDateMillis)) + " – " + fullFormat.format(new Date(maxDateMillis));
        }
        return fullFormat.format(new Date(minDateMillis)) + " – " + fullFormat.format(new Date(maxDateMillis));
    }


    /**
     * Vypočítá celočíselné procentní podíly VT a NT tak, aby jejich součet při kladné spotřebě
     * vždy činil přesně {@code 100 %}.
     *
     * @param vtKwh spotřeba VT v kWh
     * @param ntKwh spotřeba NT v kWh
     * @return dvouprvkové pole {@code [vtPercent, ntPercent]}
     */
    @NonNull
    public static int[] computeTariffPercentages(double vtKwh, double ntKwh) {
        double cleanVt = Math.max(0.0, vtKwh);
        double cleanNt = Math.max(0.0, ntKwh);
        double total = cleanVt + cleanNt;
        if (total <= 0.0001) {
            return new int[]{0, 0};
        }
        int vtPct = (int) Math.round((cleanVt / total) * 100.0);
        vtPct = Math.max(0, Math.min(100, vtPct));
        return new int[]{vtPct, 100 - vtPct};
    }


    @NonNull
    private static DecimalFormatSymbols createCzechSymbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(CZECH_LOCALE);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('\u00A0');
        return symbols;
    }

}
