package cz.xlisto.elektrodroid.modules.graphmonth;


import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.ConsuptionModel;

/**
 * Kontejner pro seznam mesíční, roční spotřeby a seznam měsíčních spotřeb v listu podle měsíců
 * <p>
 * Xlisto 28.08.2023 12:53
 */
public class ConsuptionContainer {
    private static final String TAG = "ConsuptionContainer";
    private final ArrayList<ConsuptionModel> monthlyConsuption;
    private final ArrayList<ConsuptionModel> yearConsuption;
    private final ArrayList<ArrayList<ConsuptionModel>> monthsConsuptionsArray;


    public ConsuptionContainer(ArrayList<ConsuptionModel> monthlyConsuption, ArrayList<ConsuptionModel> yearConsuption, ArrayList<ArrayList<ConsuptionModel>> monthsConsuptionsArray) {
        this.monthlyConsuption = monthlyConsuption;
        this.yearConsuption = yearConsuption;
        this.monthsConsuptionsArray = monthsConsuptionsArray;

    }


    /**
     * Vrátí měsíční spotřebu
     *
     * @return ArrayList s měsíční spotřebou
     */
    public ArrayList<ConsuptionModel> getMonthlyConsuption() {
        return monthlyConsuption;
    }


    /**
     * Vrátí roční spotřebu
     *
     * @return ArrayList s roční spotřebou
     */
    public ArrayList<ConsuptionModel> getYearConsuption() {
        return yearConsuption;
    }


    /**
     * Vrátí měsíční spotřebu v listu podle měsíců
     *
     * @return ArrayList s měsíční spotřebou v listu podle měsíců
     */
    public ArrayList<ArrayList<ConsuptionModel>> getMonthsConsuptionsArray() {
        return monthsConsuptionsArray;
    }
}
