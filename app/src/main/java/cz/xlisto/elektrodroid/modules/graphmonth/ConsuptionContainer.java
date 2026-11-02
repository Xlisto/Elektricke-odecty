package cz.xlisto.elektrodroid.modules.graphmonth;


import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.ConsuptionModel;


/**
 * Kontejner pro seznam mesíční, roční spotřeby a seznam měsíčních spotřeb v listu podle měsíců
 * <p>
 * Xlisto 28.08.2023 12:53
 */
public record ConsuptionContainer(ArrayList<ConsuptionModel> monthlyConsuption,
                                  ArrayList<ConsuptionModel> yearConsuption,
                                  ArrayList<ArrayList<ConsuptionModel>> monthsConsuptionsArray) {

    /**
     * Vrátí měsíční spotřebu
     *
     * @return ArrayList s měsíční spotřebou
     */
    @Override
    public ArrayList<ConsuptionModel> monthlyConsuption() {
        return monthlyConsuption;
    }


    /**
     * Vrátí roční spotřebu
     *
     * @return ArrayList s roční spotřebou
     */
    @Override
    public ArrayList<ConsuptionModel> yearConsuption() {
        return yearConsuption;
    }


    /**
     * Vrátí měsíční spotřebu v listu podle měsíců
     *
     * @return ArrayList s měsíční spotřebou v listu podle měsíců
     */
    @Override
    public ArrayList<ArrayList<ConsuptionModel>> monthsConsuptionsArray() {
        return monthsConsuptionsArray;
    }

}
