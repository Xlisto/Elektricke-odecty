package cz.xlisto.odecty.models;

import android.content.Context;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.ownview.ViewHelper;

import static cz.xlisto.odecty.format.DecimalFormatHelper.df2;

/**
 * Nastaví regulovaní ceny do objektu PriceList
 * Zobrazí informační texty o regulovaných cenách
 */
public class PriceListRegulBuilder extends PriceListModel {
    private PriceListModel priceList;
    private boolean isRegulPrice = false;
    private int year = -1;
    private int month = -1;
    private int day = -1;
    private String STROP = "Cenový strop: ";
    private String POZE_2022 = "Zrušení poplatku POZE od října 2022: ";
    private String POZE_2023 = "Zrušení poplatku POZE: ";
    private double MAX_VT_2023 = 5000d;
    private double MAX_NT_2023 = 5000d;
    private double MAX_PLAT_2023 = 130d;

    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Rok platnosti použuje z vlastního data platnosti
     * @param priceList
     */
    public PriceListRegulBuilder(PriceListModel priceList) {
        this(priceList, priceList.getRokPlatnost());
    }

    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od prvního ledna až do dokonce roku
     * @param priceList
     * @param year
     */
    public PriceListRegulBuilder(PriceListModel priceList, int year) {
        this(priceList, year, -1, 1);
    }

    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od zadaného data až do konce roku
     * @param priceList
     * @param year
     * @param month
     * @param day
     */
    public PriceListRegulBuilder(PriceListModel priceList, int year, int month, int day) {
        this.month = month;
        this.day = day;
        this.year = year;
        isRegulPrice = false;
        this.priceList = new PriceListModel(priceList.getId(), priceList.getRada(), priceList.getProdukt(), priceList.getFirma(), priceList.getCenaVT(),
                priceList.getCenaNT(), priceList.getMesicniPlat(), priceList.getDan(), priceList.getSazba(), priceList.getDistVT(),
                priceList.getDistNT(), priceList.getJ0(), priceList.getJ1(), priceList.getJ2(), priceList.getJ3(), priceList.getJ4(),
                priceList.getJ5(), priceList.getJ6(), priceList.getJ7(), priceList.getJ8(), priceList.getJ9(), priceList.getJ10(),
                priceList.getJ11(), priceList.getJ12(), priceList.getJ13(), priceList.getJ14(), priceList.getSystemSluzby(),
                priceList.getCinnost(), priceList.getPoze1(), priceList.getPoze2(), priceList.getOze(), priceList.getOte(),
                priceList.getPlatnostOD(), priceList.getPlatnostDO(), priceList.getDph(), priceList.getDistribuce(),
                priceList.getAutor(), priceList.getDatumVytvoreni(), priceList.getEmail());
        //nastavení poze0, nt, vt
        if (year == 2023) {
            setVtMax5000();
            setNtMax5000();
            setPoze0();
            setPlatMax130();
            isRegulPrice = true;
        }
        //nastavení poze0
        // pro celý rok - využití v zobrazení ceníku
        // pro říjen až konec roku - pro měsíční odečty
        //0=leden 9=říjen
        if ((year == 2022 && month == -1) || (year == 2022 && month >= 9)) {
            setPoze0();
            isRegulPrice = true;
        }
    }

    /**
     * Nastaví v ceníku zrušení poplatku POZE
     */
    private void setPoze0() {
        priceList.setPoze1(0d);
        priceList.setPoze2(0d);
    }

    /**
     * Nastaví v ceníku zastropovanou cenu VT
     */
    private void setVtMax5000() {
        if (priceList.getCenaVT() > MAX_VT_2023)
            priceList.setCenaVT(MAX_VT_2023);
    }

    /**
     * Nastaví v ceníku zastropovanou cenu NT
     */
    private void setNtMax5000() {
        if (priceList.getCenaNT() > MAX_NT_2023)
            priceList.setCenaNT(MAX_NT_2023);
    }

    /**
     * Nastaví v ceníku zastropovanou cenu měsíčního platu
     */
    private void setPlatMax130() {
        if (priceList.getMesicniPlat() > MAX_PLAT_2023)
            priceList.setMesicniPlat(MAX_PLAT_2023);
    }

    /**
     * Vrátí zastropovanou cenu VT
     *
     * @return
     */
    public String getMaxVT() {
        if (year == 2023)
            return STROP + df2.format(MAX_VT_2023);
        return null;
    }

    /**
     * Vrátí zastropovanou cenu NT
     *
     * @return
     */
    public String getMaxNT() {
        if (year == 2023)
            return STROP + df2.format(MAX_NT_2023);
        return null;
    }

    /**
     * Vrátí max možný měsíční plat s popisem
     *
     * @return
     */
    public String getMaxPlat() {
        if (year == 2023)
            return STROP + df2.format(MAX_PLAT_2023);
        return null;
    }

    /**
     * Vrátí max cenu POZE pro daný rok s popisem
     *
     * @return
     */
    public String getMaxPOZE() {
        if (year == 2022)
            return POZE_2022 + df2.format(0.0);
        if (year == 2023)
            return POZE_2023 + df2.format(0.0);
        return null;
    }

    public PriceListModel getRegulPriceList() {
        return priceList;
    }

    /**
     * Zobrazí text upozornující na poznámky k ceníku
     *
     * @param context
     * @return
     */
    public String getNotes(Context context) {
        isRegulPrice = false;
        if (year == 2022) {
            isRegulPrice = true;
            return context.getResources().getString(R.string.poznamka_2022);
        }
        if (year == 2023) {
            isRegulPrice = true;
            return context.getResources().getString(R.string.poznamka_2023);
        }
        return "";
    }

    /**
     * Vrátí, zdali se je dostupná regulovaná cena
     *
     * @return
     */
    public boolean isRegulPrice() {
        return isRegulPrice;
    }

    /**
     * Vrátí aktuální datum začátku platnosti regulace pro daný rok
     *
     * @return
     */
    public long getDateStart() {
        if (year == 2022)
            return ViewHelper.parseCalendarFromString("1.10.2022").getTimeInMillis();
        if (year == 2023)
            return ViewHelper.parseCalendarFromString("1.1.2023").getTimeInMillis();
        return ViewHelper.parseCalendarFromString("1.1."+year).getTimeInMillis();
    }

    /**
     * Vrátí aktuální datum konce platnosti regulace pro daný rok
     *
     * @return
     */
    public long getDateEnd() {
        if (year == 2022)
            return ViewHelper.parseCalendarFromString("31.12.2022").getTimeInMillis();
        if (year == 2023)
            return ViewHelper.parseCalendarFromString("31.12.2023").getTimeInMillis();
        return ViewHelper.parseCalendarFromString("31.12."+year).getTimeInMillis();
    }
}
