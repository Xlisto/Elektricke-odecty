package cz.xlisto.cenik.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.cenik.databaze.DataPriceListSource;
import cz.xlisto.cenik.models.InvoiceModel;
import cz.xlisto.cenik.models.PozeModel;
import cz.xlisto.cenik.models.PriceListModel;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.ownview.ViewHelper;

import static cz.xlisto.cenik.models.PozeModel.TypePoze.POZE1;
import static cz.xlisto.cenik.models.PriceListModel.NEW_POZE_YEAR;

public class Calculation {
    private static int faze = 3;
    private static int prikon = 25;

    /**
     * Výpočet jednotkové ceny každé kilowaty v ceníku s DPH
     *
     * @param priceList
     * @return
     */
    public static double[] calculatePriceForPriceListDPH(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        double[] prices = calculatePriceForPriceListKwh(priceList, subscriptionPoint);
        double[] pricesDPH = new double[3];
        double dph = priceList.getDph();
        for (int i = 0; i < prices.length; i++) {
            pricesDPH[i] = prices[i] + (prices[i] * dph / 100);
        }
        return pricesDPH;
    }

    /**
     * Výpočet jednotkové ceny každé kilowaty v ceníku
     *
     * @param priceList
     * @return
     */
    public static double[] calculatePriceForPriceListKwh(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        checkSubscriptionPint(subscriptionPoint);
        double vt = (priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby() + priceList.getPoze2()) / 1000;
        double nt = (priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby() + priceList.getPoze2()) / 1000;
        double stPlat = priceList.getMesicniPlat() + priceList.getOte() + priceList.getCinnost() + calculatePriceBreaker(priceList, faze, prikon);
        return new double[]{vt, nt, stPlat};
    }

    /**
     * Výpočet jednotkové ceny každé megawaty s odděleným poze podle spotřeby
     *
     * @param priceList
     * @return vt, nt, stPlat, poze
     */
    public static double[] calculatePriceWithoutPozeMwH(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        checkSubscriptionPint(subscriptionPoint);
        double vt = (priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby());
        double nt = (priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby());
        double poze = priceList.getPoze2();
        double stPlat = priceList.getMesicniPlat() + priceList.getOte() + priceList.getCinnost() + calculatePriceBreaker(priceList, faze, prikon);
        return new double[]{vt, nt, stPlat, poze};
    }

    /**
     * Výpočet jednotkové ceny každé kilowaty bez poze
     *
     * @param priceList
     * @return vt, nt, stPlat, poze
     */
    public static double[] calculatePriceWithoutPozeKwh(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        //checkSubscriptionPint(subscriptionPoint);
        //double vt = (priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby()) / 1000;
        //double nt = (priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby()) / 1000;
        //double poze = priceList.getPoze2() / 1000;
        //double stPlat = priceList.getMesicniPlat() + priceList.getOte() + priceList.getCinnost() + calculatePriceBreaker(priceList, faze, prikon);
        double[] result = calculatePriceWithoutPozeMwH(priceList, subscriptionPoint);
        for (int i = 0; i < result.length; i++) {
            if (i != 2) //vynechávám platbu za měsíc
                result[i] = result[i] / 1000;
        }
        return result;
    }

    /**
     * Výpočet měsíční platby za podle příkonové hodnoty hlavního jističe
     * Doplnit parametr z odběrného místa na nastavení jističů
     *
     * @param priceList
     * @return
     */
    public static double calculatePriceBreaker(PriceListModel priceList, int faze, int prikon) {
        double cenaZaJistic = 0.0;
        double pocetAmperuNavic;
        if (faze == 1.0) {
            if (prikon <= 25) {
                cenaZaJistic = priceList.getJ0();
            } else {
                pocetAmperuNavic = Double.valueOf(prikon % 25);//Zbytek po dělení - příklad Fáze 1x30 tj. 30/25 = 1 a zbytek 5 (výsledek), výpočet ceny sazba za 1x25A+(počet Amperu navíc*cena)
                cenaZaJistic = priceList.getJ0() + (pocetAmperuNavic * priceList.getJ9());
            }
        }
        if (faze == 3.0) {
            if (prikon <= 10) {
                cenaZaJistic = priceList.getJ0();
            }
            if ((prikon > 10) && (prikon <= 16)) {
                cenaZaJistic = priceList.getJ1();
            }
            if ((prikon > 16) && (prikon <= 20)) {
                cenaZaJistic = priceList.getJ2();
            }
            if ((prikon > 20) && (prikon <= 25)) {
                cenaZaJistic = priceList.getJ3();
            }
            if ((prikon > 25) && (prikon <= 32)) {
                cenaZaJistic = priceList.getJ4();
            }
            if ((prikon > 32) && (prikon <= 40)) {
                cenaZaJistic = priceList.getJ5();
            }
            if ((prikon > 40) && (prikon <= 50)) {
                cenaZaJistic = priceList.getJ6();
            }
            if ((prikon > 50) && (prikon <= 63)) {
                cenaZaJistic = priceList.getJ7();
            }
            if (priceList.getJ10() > 0) {
                //obsahuje rozšířený ceník jističů
                if ((prikon > 63) && (prikon <= 80)) {
                    cenaZaJistic = priceList.getJ10();
                }
                if ((prikon > 80) && (prikon <= 100)) {
                    cenaZaJistic = priceList.getJ11();
                }
                if ((prikon > 100) && (prikon <= 125)) {
                    cenaZaJistic = priceList.getJ12();
                }
                if ((prikon > 125) && (prikon <= 160)) {
                    cenaZaJistic = priceList.getJ13();
                }
                if (prikon > 160) {
                    pocetAmperuNavic = Double.valueOf(prikon % 160);
                    cenaZaJistic = priceList.getJ13() + (pocetAmperuNavic * priceList.getJ14());
                }

            } else {
                if (prikon > 63) {
                    pocetAmperuNavic = Double.valueOf(prikon % 63);
                    cenaZaJistic = priceList.getJ7() + (pocetAmperuNavic * priceList.getJ8());
                }
            }

        }
        return cenaZaJistic;
    }

    public static double differentMonth(String date1, String date2, DifferenceDate.TypeDate typeDate) {
        Calendar cal1 = ViewHelper.parseCalendarFromString(date1);
        Calendar cal2 = ViewHelper.parseCalendarFromString(date2);
        DifferenceDate differenceDate = new DifferenceDate(cal1, cal2, typeDate);
        return Round.round(differenceDate.getMonth(), 3);
    }

    /**
     * Kontrola objektu odběrného místa, pokud vyhovuje nastaví se hodnoty jističů
     *
     * @param subscriptionPoint
     */
    private static void checkSubscriptionPint(SubscriptionPointModel subscriptionPoint) {
        if (subscriptionPoint != null) {
            faze = subscriptionPoint.getCountPhaze();
            prikon = subscriptionPoint.getPhaze();
        }
    }

    /**
     * Výpočet POZE dle jeho typu (podle spotřeby nebo podle jističe)
     * @param priceList
     * @param countPhaze
     * @param power
     * @param consuption spotřeba v MWh
     * @param month
     * @param typePoze
     * @return double cenapoze bezDPH
     */
    public static double getPozeByType(PriceListModel priceList, double countPhaze, double power, double consuption, double month, PozeModel.TypePoze typePoze) {
        PozeModel poze = getPoze(priceList, countPhaze, power, consuption, month);
        if (typePoze.equals(POZE1))
            return poze.getPoze1();
        else
            return poze.getPoze2();
    }

    /**
     * Vypočítá obě dvě poze
     * pokud je ceník do roku 2015, vrátí se obě dvě hodnoty stejné
     *
     * @return objekt PozeModel
     */
    public static PozeModel getPoze(PriceListModel priceList, double countPhaze, double power, double consuption, double month) {
        double poze2 = priceList.getPoze2() * consuption;
        double phaze = countPhaze * power;
        double poze1 = phaze * priceList.getPoze1() * month;
        if (priceList.getRokPlatnost() < NEW_POZE_YEAR) {
            poze2 = priceList.getOze() * consuption;
            poze1 = poze2;
        }
        //return new double[]{poze1, poze2};
        return new PozeModel(poze1, poze2);
    }

    /**
     * Vypočítá poze u všech položek seznamu (faktury)
     *
     * @param invoices
     * @param countPhaze
     * @param phaze
     * @param context
     * @return objekt PozeModel
     */
    public static PozeModel getPoze(ArrayList<InvoiceModel> invoices, int countPhaze, int phaze, Context context) {
        //double[] totalPoze = new double[]{0, 0};
        PozeModel poze = new PozeModel(0, 0);
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            String dateOf = ViewHelper.convertLongToTime(invoice.getDateFrom());
            String dateTo = ViewHelper.convertLongToTime(invoice.getDateTo());
            PriceListModel priceList = getPriceList(invoice, context);
            double vt = invoice.getVt() / 1000;
            double nt = invoice.getNt() / 1000;
            double differentDate = Calculation.differentMonth(dateOf, dateTo, DifferenceDate.TypeDate.INVOICE);
            PozeModel tmpPoze = Calculation.getPoze(priceList, countPhaze, phaze, vt + nt, differentDate);
            poze.addPoze1(tmpPoze.getPoze1());
            poze.addPoze2(tmpPoze.getPoze2());
        }


        return poze;
    }

    /**
     * Výpočet POZE dle jeho typu (podle spotřeby nebo podle jističe)
     *
     * @param invoice
     * @param countPhaze
     * @param phaze
     * @param context
     * @param typePoze
     * @return
     */
    /*public static double getPoze(InvoiceModel invoice, int countPhaze, int phaze, Context context, PozeModel.TypePoze typePoze) {
        PriceListModel priceList = getPriceList(invoice, context);
        if (priceList.getRokPlatnost() < NEW_POZE_YEAR)
            return invoice.getVtNt() * priceList.getOze();
        if (typePoze.equals(PozeModel.TypePoze.POZE2)) {
            return invoice.getVtNt() * priceList.getPoze2();
        } else {
            return countPhaze * phaze * priceList.getPoze1() * invoice.getDifferentDate(DifferenceDate.TypeDate.INVOICE);
        }
    }*/

    /**
     * Načte ceník podle id uložený ve faktuře
     *
     * @param invoice
     * @return
     */
    private static PriceListModel getPriceList(InvoiceModel invoice, Context context) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(invoice.getIdPriceList());
        dataPriceListSource.close();
        return priceList;
    }

    /**
     * Typy poze
     * POZE1 - podle hodnoty jističe
     * POZE2 - podle spotřeby - max. 495 kč/MWh
     */
    /*public enum Poze {
        POZE1,
        POZE2
    }*/


    /*public Double vypocetMesicuProMesicniOdecty(Long odL, Long doL, int zaokrouhli) {
        //tady je od a do přehozen
        double pocetMesicu = -1.0;
        int den, mesic, rok, pDen, pRok, pMesic;
        Calendar datumODCalendar = Calendar.getInstance();
        Calendar datumDOCalendar = Calendar.getInstance();
        datumODCalendar.setTimeInMillis(odL);
        datumDOCalendar.setTimeInMillis(doL);
        den = datumODCalendar.get(Calendar.DAY_OF_MONTH);
        mesic = datumODCalendar.get(Calendar.MONTH);
        rok = datumODCalendar.get(Calendar.YEAR);
        pDen = datumDOCalendar.get(Calendar.DAY_OF_MONTH);
        pMesic = datumDOCalendar.get(Calendar.MONTH);
        pRok = datumDOCalendar.get(Calendar.YEAR);
        DifferenceDate differenceDate = new DifferenceDate(datumDOCalendar, datumODCalendar);
        pocetMesicu = differenceDate.getMonth();
        return zaokrouhli(pocetMesicu, zaokrouhli);
    }*/
}
