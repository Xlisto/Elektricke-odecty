package cz.xlisto.elektrodroid.format;

import java.text.DecimalFormat;

/**
 * Pomocná třída poskytující předpřipravené instance DecimalFormat pro formátování
 * číslic s pevným počtem desetinných míst.
 * <p>
 * Jedná se o statické konstanty pro formátování:
 * - df0: bez desetinných míst (0)
 * - df1: jedno desetinné místo (0.0)
 * - df2: dvě desetinná místa (0.00)
 * - df3: tři desetinná místa (0.000)
 */
public class DecimalFormatHelper {
    public static final DecimalFormat df0 = new DecimalFormat("0");
    public static final DecimalFormat df1 = new DecimalFormat("0.0");
    public static final DecimalFormat df2 = new DecimalFormat("0.00");
    public static final DecimalFormat df3 = new DecimalFormat("0.000");
}
