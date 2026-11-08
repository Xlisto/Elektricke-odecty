package cz.xlisto.elektrodroid.format;


import java.text.SimpleDateFormat;
import java.util.Locale;


/**
 * Pomocná třída poskytující předpřipravené instance {@link SimpleDateFormat}
 * pro formátování dat a časů s využitím aktuální lokalizace.
 */
public class SimpleDateFormatHelper {

    /**
     * Vrátí novou instanci {@link SimpleDateFormat} pro formátování času s dnem v týdnu ("HH:mm:ss EE").
     *
     * @return objekt {@link SimpleDateFormat}
     */
    public static SimpleDateFormat getOnlyTime() {
        return new SimpleDateFormat("HH:mm:ss EE", Locale.getDefault());
    }


    /**
     * Vrátí novou instanci {@link SimpleDateFormat} pro formátování měsíce ("MM").
     *
     * @return objekt {@link SimpleDateFormat}
     */
    public static SimpleDateFormat getMonth() {
        return new SimpleDateFormat("MM", Locale.getDefault());
    }


    /**
     * Vrátí novou instanci {@link SimpleDateFormat} pro formátování roku ("yyyy").
     *
     * @return objekt {@link SimpleDateFormat}
     */
    public static SimpleDateFormat getYear() {
        return new SimpleDateFormat("yyyy", Locale.getDefault());
    }


    /**
     * Vrátí novou instanci {@link SimpleDateFormat} pro formátování data ("dd.MM.yyyy").
     *
     * @return objekt {@link SimpleDateFormat}
     */
    public static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

}
