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
 * Utility třída pro výpočty související s energetickými ceníky a fakturací.
 *
 * <p>Hlavní odpovědnosti:
 * - Výpočet jednotkových cen (VT, NT) a měsíční stálé platby (s/bez DPH).
 * - Výpočet POZE podle spotřeby nebo podle jističe a agregace POZE z faktur.
 * - Výpočet ceny hlavního jističe podle parametrů ceníku.
 * - Pomocné výpočty (rozdíl dat v měsících, načtení ceníku z databáze, součet faktur).</p>
 * <p>
 * Poznámky:
 * - Třída obsahuje pouze statické metody a není určena k instanciaci.
 * - Některé metody mění interní statické proměnné `countPhaze` a `power` voláním
 * `checkSubscriptionPint`, což znamená, že třída není bezpečná pro paralelní použití bez synchronizace.
 * - Metody pracují s externími modely (`PriceListModel`, `SubscriptionPointModel`,
 * `PozeModel`, `InvoiceModel`) a s databázovým zdrojem `DataPriceListSource`.</p>
 */
public class Calculation {

    public static final String TAG = "Calculation";
    private static int countPhaze = 3;
    private static int power = 25;


    /**
     * Vypočítá ceny s DPH pro VT, NT a měsíční stálou platbu na základě zadaného ceníku a odběrného místa.
     * <p>
     * Postup:
     * - Zavolá {@link #calculatePriceForPriceListKwh(PriceListModel, SubscriptionPointModel)} pro získání základních cen bez DPH.
     * - Ke každé hodnotě přičte DPH podle sazby v `priceList`.
     *
     * @param priceList         Objekt ceníku \`PriceListModel\`
     * @param subscriptionPoint Objekt odběrného místa \`SubscriptionPointModel\` (může být \`null\`)
     * @return double[] pole o délce 3 obsahující ceny s DPH ve formátu {vtDPH, ntDPH, stPlatDPH}
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
     * Vypočítá jednotkovou cenu za kWh pro VT (vysoký tarif) a NT (nízký tarif) a měsíční stálou platbu podle zadaného ceníku a odběrného místa.
     * <p>
     * Postup:
     * - Pokud je `subscriptionPoint` nenulové, aktualizuje interní hodnoty fází a příkonu voláním `checkSubscriptionPint`.
     * - Na základě data platnosti ceníku určí, zda použít státní slevu POZE pro rok 2026 (`poze1`) nebo běžnou hodnotu (`poze2`).
     * - VT a NT se počítají jako součet příslušných složek (cena, distribuce, daň, systémové služby, POZE) děleno 1000 pro převod na Kč/kWh.
     * - `stPlat` (stálá měsíční platba) je součet měsíční platby, OTE, činnosti a ceny za jistič.
     *
     * @param priceList         Objekt ceníku (`PriceListModel`)
     * @param subscriptionPoint Objekt odběrného místa (`SubscriptionPointModel`), může být `null`
     * @return double[] obsahující {vt, nt, stPlat} — vt a nt v Kč/kWh, stPlat v Kč/měsíc
     */
    public static double[] calculatePriceForPriceListKwh(PriceListModel priceList, SubscriptionPointModel subscriptionPoint) {
        checkSubscriptionPint(subscriptionPoint);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(priceList.getPlatnostOD());
        int year = date.get(Calendar.YEAR);

        // zavedení státní slevy za POZE pro rok 2026 (POZE = 0, dle ceníku)
        double poze;
        if (year == 2026)
            poze = priceList.getPoze1();
        else poze = priceList.getPoze2();

        double vt = (priceList.getCenaVT() + priceList.getDistVT() + priceList.getDan() + priceList.getSystemSluzby() + poze) / 1000;
        double nt = (priceList.getCenaNT() + priceList.getDistNT() + priceList.getDan() + priceList.getSystemSluzby() + poze) / 1000;
        double stPlat = priceList.getMesicniPlat() + priceList.getOte() + priceList.getCinnost() + calculatePriceBreaker(priceList, countPhaze, power);
        return new double[]{vt, nt, stPlat};
    }


    /**
     * Výpočítá složky ceny bez POZE v jednotkách MWh (pokud ceník počítá POZE samostatně).
     * <p>
     * Postup:
     * - Zavolá {@link #checkSubscriptionPint} pro případnou aktualizaci interních hodnot fází a jističe.
     * - Pro ceníky se starou platností (rok < NEW_POZE_YEAR) zahrnuje do VT/NT i položku OTE a
     * POZE se bere z {@link PriceListModel#getOze()}.
     * - Pro novější ceníky se OTE do VT/NT nezahrnuje (POZE se vypočítá odděleně pomocí {@link PriceListModel#getPoze2()}).
     * - Stálá platba (stPlat) obsahuje měsíční platbu a cenu za jistič; u novějších ceníků je navíc
     * započítána položka `cinnost`.
     * <p>
     * Poznámky:
     * - Metoda vrací pole double[] ve formátu {vt, nt, stPlat, poze}.
     * - vt a nt jsou v Kč/MWh,
     * - stPlat je v Kč/měsíc,
     * - poze je v Kč (odpovídající hodnotě POZE pro daný ceník a jednotky).
     * - Parametr {@code subscriptionPoint} může být {@code null}; v tom případě se nepřepíší
     * interní statické hodnoty fází a jističe.
     *
     * @param priceList         Objekt ceníku {@code PriceListModel}
     * @param subscriptionPoint Objekt odběrného místa {@code SubscriptionPointModel}, může být {@code null}
     * @return double[] pole o délce 4 ve formátu {vt, nt, stPlat, poze}
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
     * Vrátí ceny bez POZE převedené na jednotky kWh (pokud se původně počítaly pro MWh).
     * <p>
     * Postup:
     * - Deleguje výpočet na {@link #calculatePriceWithoutPozeMwH(PriceListModel, SubscriptionPointModel)}.
     * - Všechny položky kromě měsíční stálé platby (index 2) jsou vyděleny 1000 pro převod z MWh na kWh.
     * <p>
     * Poznámky:
     * - Parametr {@code subscriptionPoint} je předán dál a může ovlivnit výpočet v delegované metodě.
     * - Metoda nemění stav třídy.
     *
     * @param priceList         Objekt ceníku {@code PriceListModel}
     * @param subscriptionPoint Objekt odběrného místa {@code SubscriptionPointModel}, může být {@code null}
     * @return double[] pole o délce 4 ve formátu {vt, nt, stPlat, poze} — vt, nt a poze v Kč/kWh; stPlat v Kč/měsíc
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
     * Vypočítá měsíční cenu za hlavní jistič podle parametrů ceníku, počtu fází a hodnoty jističe.
     * <p>
     * Postup:
     * - Pro 1 fázi: použije se sazba `j0`; pokud je `power` > 25, připočte se zbytek ampér * `j9`.
     * - Pro 3 fáze: zvolí se odpovídající pásmo (`j0`..`j7`) podle hodnoty `power`.
     * - Pokud je v ceníku rozšířený blok jističů (`j10` > 0), použijí se sazby `j10`..`j13`
     * a při překročení posledního pásma se připočte (zbytek) * `j14`.
     * - Pokud rozšířený blok není a `power` > 63, použije se příplatek `j8` nad `j7`.
     * <p>
     * Poznámky:
     * - `countPhaze` očekává hodnoty 1 nebo 3.
     * - `power` je v ampérech (A).
     *
     * @param priceList  Objekt ceníku \`PriceListModel\`
     * @param countPhaze Počet fází (1 nebo 3)
     * @param power      Příkon hlavního jističe v ampérech (A)
     * @return Měsíční cena za jistič v Kč
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
     * Vypočítá rozdíl mezi dvěma daty zadanými jako časy v milisekundách (epoch ms).
     * <p>
     * Postup:
     * - Inicializuje dva objekty {@link Calendar} z předaných časů.
     * - Vytvoří instanci {@link DifferenceDate} s těmito kalendáři a zadaným typem výpočtu.
     * - Vrátí počet měsíců z {@link DifferenceDate#getMonth()} zaokrouhlený na 3 desetinná místa.
     * <p>
     * Poznámky:
     * - Parametry jsou v milisekundách od 1.1.1970 (standardní epoch ms).
     * - Výsledek může být záporný, pokud je druhé datum před prvním.
     *
     * @param date1    První datum jako long (epoch ms)
     * @param date2    Druhé datum jako long (epoch ms)
     * @param typeDate Typ výpočtu ({@link DifferenceDate.TypeDate}) určující drobnosti počítání (např. pro fakturaci)
     * @return double  Rozdíl mezi daty v měsících (zaokrouhleno na 3 desetinná místa)
     * @see DifferenceDate
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
     * Vypočítá rozdíl mezi dvěma daty zadanými jako řetězce ve formátu `dd.MM.yyyy`.
     * <p>
     * Postup:
     * - Parsuje vstupní řetězce na `Calendar` pomocí {@link ViewHelper#parseCalendarFromString}.
     * - Deleguje výpočet na {@link #differentMonth(long, long, DifferenceDate.TypeDate)}.
     * <p>
     * Poznámky:
     * - Očekávaný formát dat je `dd.MM.yyyy`. Pokud parsing selže, může dojít k NPE nebo jiné chybě při parsování.
     * - Vrácená hodnota je rozdíl v měsících zaokrouhlený na tři desetinná místa.
     *
     * @param date1    První datum ve formátu `dd.MM.yyyy`
     * @param date2    Druhé datum ve formátu `dd.MM.yyyy`
     * @param typeDate Typ výpočtu (`DifferenceDate.TypeDate`) určující způsob počítání rozdílu
     * @return double  Rozdíl mezi daty v měsících (zaokrouhleno na 3 desetinná místa)
     */
    public static double differentMonth(String date1, String date2, DifferenceDate.TypeDate typeDate) {
        Calendar cal1 = ViewHelper.parseCalendarFromString(date1);
        Calendar cal2 = ViewHelper.parseCalendarFromString(date2);
        return differentMonth(cal1.getTimeInMillis(), cal2.getTimeInMillis(), typeDate);
    }


    /**
     * Ověří objekt odběrného místa a při nenulovém objektu aktualizuje interní hodnoty.
     * <p>
     * Postup:
     * - Pokud je parametr nenulový, přečte z něj počet fází a hodnotu jističe a uloží je do
     * statických proměnných třídy (`countPhaze`, `power`).
     * <p>
     * Poznámky:
     * - Metoda mění stav třídy (statické proměnné), není bezstavová.
     *
     * @param subscriptionPoint Objekt odběrného místa; pokud je {@code null}, hodnoty se nemění
     */
    private static void checkSubscriptionPint(SubscriptionPointModel subscriptionPoint) {
        if (subscriptionPoint != null) {
            countPhaze = subscriptionPoint.getCountPhaze();
            power = subscriptionPoint.getPhaze();
        }
    }


    /**
     * Vrátí cenu POZE odpovídající zvolenému typu pro zadaný ceník a období.
     * <p>
     * Postup:
     * - Zavolá interně {@link #getPoze(PriceListModel, double, double, double, double)} pro výpočet obou složek POZE.
     * - Podle hodnoty {@code typePoze} vrátí buď složku {@code poze1} (dle jističe) nebo {@code poze2} (dle spotřeby).
     * <p>
     * Poznámky:
     * - {@code consumption} je očekáváno v MWh.
     * - {@code month} je délka období v měsících.
     * - Metoda sama o sobě nemění stav, ale deleguje výpočet na {@link #getPoze(PriceListModel, double, double, double, double)}.
     *
     * @param priceList   Objekt ceníku (\`PriceListModel\`)
     * @param countPhaze  Počet fází (např. 1 nebo 3)
     * @param power       Příkon hlavního jističe (A)
     * @param consumption Spotřeba v MWh
     * @param month       Délka období v měsících
     * @param typePoze    Typ POZE (\`PozeModel.TypePoze\`) - určuje, kterou složku vrátit
     * @return Cena POZE v Kč bez DPH pro zvolený typ
     */
    public static double getPozeByType(PriceListModel priceList, double countPhaze, double power, double consumption, double month, PozeModel.TypePoze typePoze) {
        PozeModel poze = getPoze(priceList, countPhaze, power, consumption, month);
        if (typePoze.equals(POZE1))
            return poze.getPoze1();
        else
            return poze.getPoze2();
    }


    /**
     * Vypočítá obě složky POZE: `poze1` (dle jističe) a `poze2` (dle spotřeby) pro zadaný ceník.
     * <p>
     * Postup:
     * - `poze2` se standardně počítá jako `priceList.getPoze2() * consumption` (consumption v MWh).
     * - `poze1` se počítá jako `countPhaze * power * priceList.getPoze1() * month`.
     * - Pokud je rok platnosti ceníku před konstantou `NEW_POZE_YEAR`, použije se místo `poze2`
     * hodnota `priceList.getOze()` a `poze1` se nastaví shodně s `poze2`.
     *
     * @param priceList   Objekt ceníku \`PriceListModel\`
     * @param countPhaze  Počet fází (např. 1 nebo 3)
     * @param power       Příkon hlavního jističe (A)
     * @param consumption Spotřeba v MWh
     * @param month       Délka období v měsících
     * @return \`PozeModel\` obsahující \`poze1\` a \`poze2\` v Kč (bez DPH)
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
     * Agreguje POZE (obě varianty: dle jističe i dle spotřeby) ze seznamu faktur.
     * <p>
     * Postup:
     * - Pro každou fakturu načte příslušný ceník (voláním {@link #getPriceList(InvoiceModel, Context)}).
     * - Převede hodnoty VT a NT z faktury dělením 1000 (faktury mají hodnoty v Wh, zde se převádí na kWh).
     * - Spočítá délku období v měsících mezi daty faktury a zavolá {@link #getPoze(PriceListModel, double, double, double, double)}
     * pro výpočet poze pro danou položku.
     * - Sečte poze1 a poze2 z jednotlivých faktur do kumulovaného objektu {@link PozeModel}.
     * <p>
     * Poznámky:
     * - Metoda při zpracování každé faktury otevírá zdroj ceníků z databáze (přes {@code getPriceList}).
     * - Očekává, že hodnoty spotřeby v {@code InvoiceModel} jsou v Wh.
     *
     * @param invoices   Seznam faktur (\`ArrayList<InvoiceModel>\`), může být prázdný
     * @param countPhaze Počet fází odběrného místa (např. 1 nebo 3)
     * @param power      Příkon hlavního jističe (A)
     * @param context    Android kontext pro načítání ceníků z databáze; nesmí být \`null\`
     * @return PozeModel obsahující součty poze1 a poze2 pro všechna zpracovaná období (bez DPH)
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
     * Načte ceník podle id uloženého ve faktuře z databáze.
     * <p>
     * Postup:
     * - Otevře DataPriceListSource pomocí zadaného kontextu.
     * - Načte ceník podle hodnoty {@code invoice.getIdPriceList()}.
     * - Zavře zdroj.
     * - Pokud ceník není v databázi nalezen, vrátí novou instanci {@code PriceListModel}.
     *
     * @param invoice Objekt faktury (\`InvoiceModel\`) obsahující id ceníku; nesmí být \`null\`
     * @param context Kontext pro přístup k databázi; nesmí být \`null\`
     * @return Načtený objekt \`PriceListModel\` (pokud není nalezen, vrací novou, prázdnou instanci)
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
     * Vypočítá celkovou částku pro zadaný seznam faktur včetně POZE a DPH.
     * <p>
     * Postup:
     * - Pro každou fakturu načte příslušný ceník z databáze a aplikuje regulaci přes {@link PriceListRegulBuilder}.
     * - Spočítá položky: cena VT, cena NT, měsíční stálá platba a POZE (dle typu POZE nebo dle jističe).
     * - Ke každé faktuře přičte DPH podle sazby v příslušném ceníku a přidá do výsledné sumy.
     * <p>
     * Poznámky:
     * - {@code subscriptionPoint} musí být nenulové (metoda volá jeho get metody).
     * - Metoda při zpracování každé faktury otevírá zdroj ceníků z databáze (DataPriceListSource).
     * - Výpočet očekává, že hodnoty spotřeby ve faktuře jsou v Wh a výsledná suma je v Kč.
     *
     * @param invoices          Seznam faktur (\`ArrayList<InvoiceModel>\`)
     * @param subscriptionPoint Objekt odběrného místa (\`SubscriptionPointModel\`), nesmí být \`null\`
     * @param context           Kontext pro přístup k databázi
     * @return Celková suma všech faktur v Kč (včetně POZE a DPH)
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
