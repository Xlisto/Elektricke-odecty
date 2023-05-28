package cz.xlisto.odecty.format;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Xlisto 26.05.2023 11:15
 */
public class SimpleDateFormatHelper {
    private static final String TAG = "SimpleDateFormatHelper";

    public static SimpleDateFormat timeFormatOnlyTime = new SimpleDateFormat("HH:mm:ss EE", Locale.getDefault());
    public static SimpleDateFormat timeFormatDateAndTime = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    public static SimpleDateFormat timeFormatDateAndTimeWithSeconds = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
}
