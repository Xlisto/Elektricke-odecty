package cz.xlisto.elektrodroid.utils;


import static cz.xlisto.elektrodroid.models.PozeModel.TypePoze.POZE1;
import static cz.xlisto.elektrodroid.models.PriceListModel.NEW_POZE_YEAR;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

/**
 * Výpočty
 */
public class Calculation {
    public static final String TAG = "Calculation";
    private static int countPhaze = 3;
    private static int power = 25;


    /**
     * Výpočet jednotkové ceny každé kilowaty v ceníku s DPH
     *
     * @param priceList         Objekt ceníku PriceListModel
     * @param subscriptionPoint Objekt odběrného místa SubscriptionPointModel
     * @return double[] - vt, nt, stPlat
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
     * @param priceList         Objekt ceníku PriceListModel
     * @param subscriptionPoint Objekt odběrného místa SubscriptionPointModel
     * @return double[] - vt, nt, stPlat
     */
    public static double[] calculatePriceForPriceListKwh(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        checkSubscriptionPint(subscriptionPoint);
        double vt = (priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby() + priceList.getPoze2()) / 1000;
        double nt = (priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby() + priceList.getPoze2()) / 1000;
        double stPlat = priceList.getMesicniPlat() + priceList.getOte() + priceList.getCinnost() + calculatePriceBreaker(priceList, countPhaze, power);
        return new double[]{vt, nt, stPlat};
    }


    /**
     * Výpočet jednotkové ceny každé megawaty s odděleným poze podle spotřeby, POZE nastaven na max. hodnotu (495)
     *
     * @param priceList         Objekt ceníku PriceListModel
     * @param subscriptionPoint Objekt odběrného místa SubscriptionPointModel
     * @return vt, nt, stPlat, poze
     */
    public static double[] calculatePriceWithoutPozeMwH(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        checkSubscriptionPint(subscriptionPoint);
        double vt;
        double nt;
        double poze;
        double stPlat;
        //podmínka pro starší ceník před rokem 2016/
        if (priceList.getRokPlatnost() < NEW_POZE_YEAR) {
            //Výpočet cenaVT+distribuceVT+dan+systemSluzby+OTE (OZE,KVET a DZ (zkráceně POZE) výpočet zvlášť)
            //Výpočet OZE,KVET,DZ (zkráceně POZE) podle spotřeby
            //Výpočet stálé měsíční platby: měsíc * (měsíční plat + cena za jistič)
            vt = (priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby() + priceList.getOte());
            nt = (priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby() + priceList.getOte());
            poze = priceList.getOze();
            stPlat = priceList.getMesicniPlat() + calculatePriceBreaker(priceList, countPhaze, power);
        } else {
            //Výpočet cenaVT+distribuceVT+dan+systemSluzby
            //Výpočet POZE podle spotřeby
            //Výpočet stálé měsíční platby: měsíc * (měsíční plat + cena za jistič + OTE)
            vt = priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby();
            nt = priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby();
            poze = priceList.getPoze2();
            stPlat = priceList.getMesicniPlat() + calculatePriceBreaker(priceList, countPhaze, power) + priceList.getCinnost();
        }
        return new double[]{vt, nt, stPlat, poze};
    }


    /**
     * Výpočet jednotkové ceny každé kilowaty bez poze
     *
     * @param priceList         Objekt ceníku PriceListModel
     * @param subscriptionPoint Objekt odběrného místa SubscriptionPointModel
     * @return vt, nt, stPlat, poze
     */
    public static double[] calculatePriceWithoutPozeKwh(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        double[] result = calculatePriceWithoutPozeMwH(priceList, subscriptionPoint);
        for (int i = 0; i < result.length; i++) {
            if (i != 2) //vynechávám platbu za měsíc
                result[i] = result[i] / 1000;
        }
        return result;
    }


    /**
     * Výpočet měsíční platby za podle příkonové hodnoty hlavního jističe
     *
     * @param priceList  Objekt ceníku PriceListModel
     * @param countPhaze int - počet fází
     * @param power      int - příkon hlavního jističe
     * @return double - cena za jistič
     */
    public static double calculatePriceBreaker(PriceListModel priceList, int countPhaze, int power) {
        double priceCircuitBreaker = 0.0;
        double morePower;
        if (countPhaze == 1.0) {
            if (power <= 25) {
                priceCircuitBreaker = priceList.getJ0();
            } else {
                morePower = (power % 25);//Zbytek po dělení - příklad Fáze 1x30 tj. 30/25 = 1 a zbytek 5 (výsledek), výpočet ceny sazba za 1x25A+(počet Amperů navíc*cena)
                priceCircuitBreaker = priceList.getJ0() + (morePower * priceList.getJ9());
            }
        }
        if (countPhaze == 3.0) {
            if (power <= 10) {
                priceCircuitBreaker = priceList.getJ0();
            }
            if ((power > 10) && (power <= 16)) {
                priceCircuitBreaker = priceList.getJ1();
            }
            if ((power > 16) && (power <= 20)) {
                priceCircuitBreaker = priceList.getJ2();
            }
            if ((power > 20) && (power <= 25)) {
                priceCircuitBreaker = priceList.getJ3();
            }
            if ((power > 25) && (power <= 32)) {
                priceCircuitBreaker = priceList.getJ4();
            }
            if ((power > 32) && (power <= 40)) {
                priceCircuitBreaker = priceList.getJ5();
            }
            if ((power > 40) && (power <= 50)) {
                priceCircuitBreaker = priceList.getJ6();
            }
            if ((power > 50) && (power <= 63)) {
                priceCircuitBreaker = priceList.getJ7();
            }
            if (priceList.getJ10() > 0) {
                //obsahuje rozšířený ceník jističů
                if ((power > 63) && (power <= 80)) {
                    priceCircuitBreaker = priceList.getJ10();
                }
                if ((power > 80) && (power <= 100)) {
                    priceCircuitBreaker = priceList.getJ11();
                }
                if ((power > 100) && (power <= 125)) {
                    priceCircuitBreaker = priceList.getJ12();
                }
                if ((power > 125) && (power <= 160)) {
                    priceCircuitBreaker = priceList.getJ13();
                }
                if (power > 160) {
                    morePower = (power % 160);
                    priceCircuitBreaker = priceList.getJ13() + (morePower * priceList.getJ14());
                }

            } else {
                if (power > 63) {
                    morePower = (power % 63);
                    priceCircuitBreaker = priceList.getJ7() + (morePower * priceList.getJ8());
                }
            }
        }
        return priceCircuitBreaker;
    }


    /**
     * Vypočítá rozdíl mezi dvěma daty. Výsledek je v měsících.
     *
     * @param date1    První datum jako číslo long
     * @param date2    Druhé datum jako číslo long
     * @param typeDate Typ výpočtu podle použití měsíční odečty/fakturace (používá se rozdíl jednoho dne    )
     * @return double - rozdíl mezi daty v měsících zaokrouhlený na tři desetinná místa
     */
    public static double differentMonth(long date1, long date2, DifferenceDate.TypeDate typeDate) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(date1);
        cal2.setTimeInMillis(date2);
        DifferenceDate differenceDate = new DifferenceDate(cal1, cal2, typeDate);
        return Round.round(differenceDate.getMonth(), 3);
    }


    /**
     * Vypočítá rozdíl mezi dvěma daty. Výsledek je v měsících.
     *
     * @param date1    První datum ve formátu dd.MM.yyyy
     * @param date2    Druhé datum ve formátu dd.MM.yyyy
     * @param typeDate Typ výpočtu podle použití měsíční odečty/fakturace (používá se rozdíl jednoho dne    )
     * @return double - rozdíl mezi daty v měsících zaokrouhlený na tři desetinná místa
     */
    public static double differentMonth(String date1, String date2, DifferenceDate.TypeDate typeDate) {
        Calendar cal1 = ViewHelper.parseCalendarFromString(date1);
        Calendar cal2 = ViewHelper.parseCalendarFromString(date2);
        return differentMonth(cal1.getTimeInMillis(), cal2.getTimeInMillis(), typeDate);
    }


    /**
     * Kontrola objektu odběrného místa, pokud vyhovuje nastaví se hodnoty jističů do proměnných faze a příkon
     *
     * @param subscriptionPoint Objekt odběrného místa
     */
    private static void checkSubscriptionPint(SubscriptionPointModel subscriptionPoint) {
        if (subscriptionPoint != null) {
            countPhaze = subscriptionPoint.getCountPhaze();
            power = subscriptionPoint.getPhaze();
        }
    }


    /**
     * Výpočet POZE dle jeho typu (podle spotřeby nebo podle jističe)
     *
     * @param priceList   Objekt ceníku PriceListModel
     * @param countPhaze  Počet fází
     * @param power       Příkon hlavního jističe
     * @param consumption spotřeba v MWh
     * @param month       Počet měsíců
     * @param typePoze    Typ poze (podle spotřeby nebo podle jističe)
     * @return double  Cena POZE bezDPH
     */
    public static double getPozeByType(PriceListModel priceList, double countPhaze, double power, double consumption, double month, PozeModel.TypePoze typePoze) {
        PozeModel poze = getPoze(priceList, countPhaze, power, consumption, month);
        if (typePoze.equals(POZE1))
            return poze.getPoze1();
        else
            return poze.getPoze2();
    }


    /**
     * Vypočítá obě dvě poze
     * pokud je ceník do roku 2015, vrátí se obě dvě hodnoty stejné
     *
     * @param priceList   Objekt ceníku PriceListModel
     * @param countPhaze  Počet fází
     * @param power       Příkon hlavního jističe
     * @param consumption Spotřeba v MWh
     * @param month       Počet měsíců
     * @return objekt PozeModel
     */
    public static PozeModel getPoze(PriceListModel priceList, double countPhaze, double power, double consumption, double month) {
        double poze2, poze1;
        double phaze = countPhaze * power;
        poze2 = priceList.getPoze2() * consumption;
        poze1 = phaze * priceList.getPoze1() * month;
        if (priceList.getRokPlatnost() < NEW_POZE_YEAR) {
            poze2 = priceList.getOze() * consumption;
            poze1 = poze2;
        }
        return new PozeModel(poze1, poze2);
    }


    /**
     * Vypočítá celkové poze (podle jističe i podle spotřeby) ze všech položek seznamu (faktury)
     *
     * @param invoices   Seznam faktur
     * @param countPhaze Počet fází
     * @param power      Příkon hlavního jističe
     * @param context    Kontext
     * @return objekt PozeModel
     */
    public static PozeModel getPoze(ArrayList<InvoiceModel> invoices, int countPhaze, int power, Context context) {
        PozeModel poze = new PozeModel(0, 0);
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceModel invoice = invoices.get(i);
            String dateOf = ViewHelper.convertLongToDate(invoice.getDateFrom());
            String dateTo = ViewHelper.convertLongToDate(invoice.getDateTo());
            PriceListModel priceList = getPriceList(invoice, context);
            double vt = invoice.getVt() / 1000;
            double nt = invoice.getNt() / 1000;
            double differentDate = Calculation.differentMonth(dateOf, dateTo, DifferenceDate.TypeDate.INVOICE);
            PozeModel tmpPoze = Calculation.getPoze(priceList, countPhaze, power, vt + nt, differentDate);
            poze.addPoze1(tmpPoze.getPoze1());
            poze.addPoze2(tmpPoze.getPoze2());
        }


        return poze;
    }


    /**
     * Načte ceník podle id uložený ve faktuře
     *
     * @param invoice Objekt faktury
     * @return Objekt ceníku PriceListModel
     */
    private static PriceListModel getPriceList(InvoiceModel invoice, Context context) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
        dataPriceListSource.open();
        PriceListModel priceList = dataPriceListSource.readPrice(invoice.getIdPriceList());
        dataPriceListSource.close();
        if (priceList == null)
            priceList = new PriceListModel();
        return priceList;
    }


    /**
     * Vypočítá celkovou cenu faktur
     *
     * @param invoices Seznam faktur
     * @return double - celková suma faktur
     */
    public static double getTotalSumInvoice(ArrayList<InvoiceModel> invoices, SubscriptionPointModel subscriptionPoint, Context context) {
        double sum = 0;
        for (int i = 0; i < invoices.size(); i++) {
            PriceListRegulBuilder priceListRegulBuilder = new PriceListRegulBuilder(getPriceList(invoices.get(i), context));
            PriceListModel priceList = priceListRegulBuilder.getRegulPriceList();
            PozeModel poze = Calculation.getPoze(invoices, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze(), context);

            double vtConsuption = invoices.get(i).getVt();//nastavení spotřeby VT
            double ntConsuption = invoices.get(i).getNt();//nastavení spotřeby NT
            double differentMonth = differentMonth(invoices.get(i).getDateFrom(), invoices.get(i).getDateTo(), DifferenceDate.TypeDate.INVOICE);//nastavení počtu měsíců

            double[] price = calculatePriceWithoutPozeMwH(priceList, subscriptionPoint);//vt, nt, stPlat, poze

            //poze počítá podle typu, který se vybere podle celkové spotřeby na faktuře
            double pozePrice;
            //TODO předělat objekt POZE, aby obsahoval tuto podmínku, stejná je v InvoiceFragment
            if (poze.getTypePoze() == PozeModel.TypePoze.POZE2) {
                if (priceList.getRokPlatnost() < NEW_POZE_YEAR) {
                    pozePrice = (vtConsuption + ntConsuption) * priceList.getOze() / 1000;//poze dle spotřeby starší ceník
                } else {
                    pozePrice = (vtConsuption + ntConsuption) * priceList.getPoze2() / 1000;//poze dle spotřeby novější ceník
                }
            } else {
                pozePrice = subscriptionPoint.getCountPhaze() * subscriptionPoint.getPhaze() * differentMonth * priceList.getPoze1();//poze dle jističe
            }

            double vtPrice = vtConsuption * price[0] / 1000;
            double ntPrice = ntConsuption * price[1] / 1000;
            double stPlat = differentMonth * price[2];

            sum += (vtPrice + ntPrice + stPlat + pozePrice + ((vtPrice + ntPrice + stPlat + pozePrice) * priceList.getDph() / 100));
        }
        return sum;
    }
}
