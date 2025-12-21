package cz.xlisto.elektrodroid.format;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Xlisto 26.05.2023 11:15
 */
public class SimpleDateFormatHelper {
    private static final String TAG = "SimpleDateFormatHelper";

    public static SimpleDateFormat onlyTime = new SimpleDateFormat("HH:mm:ss EE", Locale.getDefault());
    public static SimpleDateFormat dateAndTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    public static SimpleDateFormat dateAndTimeWithSeconds = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
    public static SimpleDateFormat month = new SimpleDateFormat("MM",Locale.getDefault());
    public static SimpleDateFormat year = new SimpleDateFormat("yyyy",Locale.getDefault());
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
}
