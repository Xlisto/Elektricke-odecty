package cz.xlisto.odecty.models;

import android.content.Context;

import java.util.Calendar;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.ownview.ViewHelper;

import static cz.xlisto.odecty.format.DecimalFormatHelper.df2;


/**
 * Nastaví regulovaný ceny do objektu PriceList
 * Zobrazí informační texty o regulovaných cenách
 */
public class PriceListRegulBuilder extends PriceListModel {
    private PriceListModel priceList;
    private boolean isRegulPrice = false;
    private int year = -1;
    private final String STROP = "Cenový strop: ";
    private final double MAX_VT_2023 = 5000d;
    private final double MAX_NT_2023 = 5000d;
    private final double MAX_PLAT_2023 = 130d;


    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Rok platnosti použije z vlastního data platnosti
     *
     * @param priceList ceník
     */
    public PriceListRegulBuilder(PriceListModel priceList) {
        this(priceList, priceList.getRokPlatnost());
    }


    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od prvního ledna až do do konce roku
     *
     * @param priceList ceník
     * @param year      rok
     */
    public PriceListRegulBuilder(PriceListModel priceList, int year) {
        initialize(priceList, year, -1);
    }


    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od zadaného data až do konce roku
     *
     * @param priceList      ceník
     * @param monthlyReading  předchozí!! měsíční odečet
     */
    public PriceListRegulBuilder(PriceListModel priceList, MonthlyReadingModel monthlyReading) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(monthlyReading.getDate());
        int[] date = ViewHelper.parseIntsFromCalendar(calendar);//převedení data na pole

        initialize(priceList, date[2], date[1]);
    }


    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od zadaného data až do konce roku
     *
     * @param priceList ceník
     * @param invoice   faktura
     */
    public PriceListRegulBuilder(PriceListModel priceList, InvoiceModel invoice) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(invoice.getDateFrom());
        int[] date = ViewHelper.parseIntsFromCalendar(calendar);

        initialize(priceList, date[2], date[1]);
    }


    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od zadaného data až do konce roku
     *
     * @param priceList ceník
     * @param year      rok
     * @param month     měsíc
     */
    public PriceListRegulBuilder(PriceListModel priceList, int year, int month) {
        initialize(priceList, year, month);
    }


    /**
     * Nastaví regulované ceny v ceníku dodaný v parametru. Regulované ceny začínají od zadaného data až do konce roku
     *
     * @param priceList ceník
     * @param year      rok
     * @param month     měsíc
     */
    private void initialize(PriceListModel priceList, int year, int month) {
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
     * @return zastropovaná cena VT
     */
    public String getMaxVT() {
        if (year == 2023)
            return STROP + df2.format(MAX_VT_2023);
        return null;
    }


    /**
     * Vrátí zastropovanou cenu NT
     *
     * @return zastropovaná cena NT
     */
    public String getMaxNT() {
        if (year == 2023)
            return STROP + df2.format(MAX_NT_2023);
        return null;
    }


    /**
     * Vrátí max možný měsíční plat s popisem
     *
     * @return max měsíční plat
     */
    public String getMaxPlat() {
        if (year == 2023)
            return STROP + df2.format(MAX_PLAT_2023);
        return null;
    }


    /**
     * Vrátí max cenu POZE pro daný rok s popisem
     *
     * @return max cena POZE
     */
    public String getMaxPOZE() {
        String POZE_2022 = "Zrušení poplatku POZE od října 2022: ";
        if (year == 2022)
            return POZE_2022 + df2.format(0.0);
        String POZE_2023 = "Zrušení poplatku POZE: ";
        if (year == 2023)
            return POZE_2023 + df2.format(0.0);
        return null;
    }


    public PriceListModel getRegulPriceList() {
        return priceList;
    }


    /**
     * Zobrazí text upozorňující na poznámky k ceníku
     *
     * @param context kontext aplikace
     * @return text poznámky
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
     * @return true, pokud je regulovaná cena
     */
    public boolean isRegulPrice() {
        return isRegulPrice;
    }


    /**
     * Vrátí aktuální datum začátku platnosti regulace pro daný rok
     *
     * @return datum začátku platnosti regulace
     */
    public long getDateStart() {
        if (year == 2022)
            return ViewHelper.parseCalendarFromString("1.10.2022").getTimeInMillis();
        if (year == 2023)
            return ViewHelper.parseCalendarFromString("1.1.2023").getTimeInMillis();
        return ViewHelper.parseCalendarFromString("1.1." + year).getTimeInMillis();
    }


    /**
     * Vrátí aktuální datum konce platnosti regulace pro daný rok
     *
     * @return datum konce platnosti regulace
     */
    public long getDateEnd() {
        if (year == 2022)
            return ViewHelper.parseCalendarFromString("31.12.2022").getTimeInMillis();
        if (year == 2023)
            return ViewHelper.parseCalendarFromString("31.12.2023").getTimeInMillis();
        return ViewHelper.parseCalendarFromString("31.12." + year).getTimeInMillis();
    }
}
