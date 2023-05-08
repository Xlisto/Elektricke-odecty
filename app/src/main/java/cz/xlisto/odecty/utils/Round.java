package cz.xlisto.odecty.utils;

/**
 * Zaokrouhlení double čísel
 */
public class Round {


    /**
     * Zaokrouhlí na dvě desetinná místa
     *
     * @param number double zaokrouhlované číslo
     * @return double zaokrouhlené číslo
     */
    public static double round(double number) {//zaokrouhlování na 2 desetinná místa
        return (Math.round(number * 100.0)) / 100.0;
    }


    /**
     * Zaokrouhlí na zadaný počet míst
     *
     * @param number double  zaokrouhlované číslo
     * @param round  int počet desetinných míst
     * @return double zaokrouhlené číslo
     */
    public static double round(double number, int round) {

        StringBuilder s = new StringBuilder("1");
        for (int i = 0; i < round; i++) {
            s.append("0");
        }
        return Math.round(number * Double.parseDouble(s.toString())) / Double.parseDouble(s.toString());
    }
}
