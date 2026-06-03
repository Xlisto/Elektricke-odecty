package cz.xlisto.elektrodroid.format;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Pomocná třída poskytující předpřipravené instance SimpleDateFormat
 * pro formátování datumů a časů v češtině/německy.
 * <p>
 * Obsahuje statické konstanty pro formátování:
 * - onlyTime: jen čas s dnem v týdnu (HH:mm:ss EE)
 * - dateAndTime: datum a čas bez sekund (dd.MM.yyyy HH:mm)
 * - dateAndTimeWithSeconds: datum a čas se sekundami (dd.MM.yyyy HH:mm:ss)
 * - month: pouze měsíc (MM)
 * - year: pouze rok (yyyy)
 * - dateFormat: pouze datum (dd.MM.yyyy)
 * <p>
 * Xlisto 26.05.2023 11:15
 */
public class SimpleDateFormatHelper {

    public static SimpleDateFormat onlyTime = new SimpleDateFormat("HH:mm:ss EE", Locale.getDefault());
    public static SimpleDateFormat dateAndTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    public static SimpleDateFormat dateAndTimeWithSeconds = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
    public static SimpleDateFormat month = new SimpleDateFormat("MM",Locale.getDefault());
    public static SimpleDateFormat year = new SimpleDateFormat("yyyy",Locale.getDefault());
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
}
