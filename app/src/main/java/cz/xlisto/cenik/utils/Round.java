package cz.xlisto.cenik.utils;

/**
 * Zaokrouhlování double čísel
 */
public class Round {
    /**
     * Zaokrouhlí na dvě desetinná místa
     *
     * @param d
     * @return
     */
    public static double round(double d) {//zaokrouhlování na 2 desetinná místa
        return Math.round(d * 100.0) / 100.0;
    }

    /**
     * Zaokrouhlí na zadadný počet míst
     *
     * @param d
     * @param x
     * @return
     */
    public static double round(double d, int x) {//zaokrouhlování na 2 nebo 3 desetinných míst
        String s = "1";
        for (int i = 0; i < x; i++) {
            s = s + "0";
        }
        return Math.round(d * Double.parseDouble(s)) / Double.parseDouble(s);
    }
}
