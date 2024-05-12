package cz.xlisto.elektrodroid.modules.hdo;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Sestaví textovou podobu týdenního HDO rozvrhu pro vložení do schránky
 * Xlisto 10.08.2023 13:53
 */
public class HdoClipData {
    private static final String TAG = "HdoClipData";


    /**
     * Sestaví textovou podobu týdenního HDO rozvrhu pro vložení do schránky
     *
     * @param hdoList seznam HDO časů
     * @return String textová podoba HDO rozvrhu
     */
    public static String create(ArrayList<HdoModel> hdoList, String distributionArea) {
        String textClip = "";

        if (hdoList.size() == 0) {
            return textClip;
        }

        if (distributionArea.equals(DistributionArea.PRE.toString()))
            textClip = createPRE(hdoList, distributionArea);
        else
            textClip = createOther(hdoList, distributionArea);
        return textClip + "\n\n";
    }


    /**
     * Sestaví textovou podobu PRE HDO rozvrhu pro vložení do schránky
     *
     * @param hdoList          seznam HDO časů
     * @param distributionArea distribuční oblast
     * @return String textová podoba HDO rozvrhu
     */
    private static String createPRE(ArrayList<HdoModel> hdoList, String distributionArea) {
        StringBuilder textClip = new StringBuilder();
        for (int i = 0; i < hdoList.size(); i++) {
            HdoModel hdoModel = hdoList.get(i);
            if (i == 0)
                textClip.append(getRele(hdoModel));
            textClip.append(getTime(hdoModel, distributionArea));
        }
        return textClip.toString();
    }


    /**
     * Sestaví textovou podobu HDO EGD a CEZ rozvrhu pro vložení do schránky
     *
     * @param hdoList          seznam HDO časů
     * @param distributionArea distribuční oblast
     * @return String textová podoba HDO rozvrhu
     */
    private static String createOther(ArrayList<HdoModel> hdoList, String distributionArea) {
        StringBuilder textClip;
        //první řádek
        HdoModel hdo = hdoList.get(0);
        textClip = new StringBuilder(getDate(hdo));
        if (!hdo.getRele().isEmpty())
            textClip.append(getRele(hdo));
        textClip.append(getDays(hdo));
        textClip.append(getTime(hdo, distributionArea));

        for (int i = 1; i < hdoList.size(); i++) {
            HdoModel hdoModel = hdoList.get(i);
            HdoModel hdoPrevius = hdoList.get(i - 1);

            if (!getRele(hdoModel).equals(getRele(hdoPrevius)))
                textClip.append("\n").append(getRele(hdoModel));

            if (!getDays(hdoModel).equals(getDays(hdoPrevius)))
                textClip.append(getDays(hdoModel));

            textClip.append(getTime(hdoModel, distributionArea));
        }
        return textClip.toString();
    }


    /**
     * Sestaví textovou podobu platnosti HDO
     *
     * @param hdo HDO
     * @return String textová podoba platnosti HDO
     */
    private static String getDate(HdoModel hdo) {
        return "Platnost " + hdo.getDateFrom() + " až " + hdo.getDateUntil();
    }


    /**
     * Sestaví textovou podobu rele HDO
     *
     * @param hdo HDO
     * @return String textová podoba rele HDO
     */
    private static String getRele(HdoModel hdo) {
        return "\nRelé: " + hdo.getRele();
    }


    /**
     * Sestaví textovou podobu dnů HDO
     *
     * @param hdo HDO
     * @return String textová podoba dnů HDO
     */
    private static String getDays(HdoModel hdo) {
        if (hdo.getMon() == 1 && hdo.getTue() == 1 && hdo.getWed() == 1 && hdo.getThu() == 1 && hdo.getFri() == 1) {
            return "\n\nPracovní dny:";
        }
        if (hdo.getSat() == 1 && hdo.getSun() == 1) {
            return "\n\nVíkendy a svátky:";
        }
        if (hdo.getMon() == 1)
            return "\n\nPondělí:";
        if (hdo.getTue() == 1)
            return "\n\nÚterý:";
        if (hdo.getWed() == 1)
            return "\n\nStředa:";
        if (hdo.getThu() == 1)
            return "\n\nČtvrtek:";
        if (hdo.getFri() == 1)
            return "\n\nPátek:";
        if (hdo.getSat() == 1)
            return "\n\nSobota:";
        if (hdo.getSun() == 1)
            return "\n\nNeděle:";
        return "";
    }


    /**
     * Sestaví textovou podobu časů HDO
     *
     * @param hdo              HDO
     * @param distributionArea oblast distribuce
     * @return String textová podoba časů HDO
     */
    private static String getTime(HdoModel hdo, String distributionArea) {
        if (distributionArea.equals(DistributionArea.PRE.toString()))
            return "\n" + hdo.getDateFrom() + " " + hdo.getTimeFrom() + "-" + hdo.getTimeUntil();
        else
            return "\n" + hdo.getTimeFrom() + "-" + hdo.getTimeUntil();
    }
}
