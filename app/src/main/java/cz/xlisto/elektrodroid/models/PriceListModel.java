package cz.xlisto.elektrodroid.models;


import static cz.xlisto.elektrodroid.ownview.ViewHelper.convertLongToDate;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Datový model ceníku elektřiny zahrnující neregulované (obchodní) i regulované složky ceny,
 * poplatky za jističe (J0–J14), systémové služby, podporované zdroje energie (POZE/OZE/OTE)
 * a časový interval platnosti ceníku.
 */
public class PriceListModel implements Serializable {

    /**
     * Rok přechodu na nový výpočet poplatku POZE (2016).
     */
    public final static int NEW_POZE_YEAR = 2016;

    /**
     * Kód distribuční oblasti E.ON (používaný do roku 2020).
     */
    public final static String EON = "E.ON";

    /**
     * Kód distribuční oblasti EG.D (používaný od roku 2021).
     */
    public final static String EGD = "EG.D";

    private final static String NO_PRICE_LIST = "Ceník nenalezen!!!";
    private long id;
    private String series;
    private final String product;
    private final String company;
    private Double priceVT;
    private Double priceNT;
    private Double monthlyPayment;
    private Double tax;
    private String rate;
    private Double distVT;
    private Double distNT;
    private Double j0;
    private Double j1;
    private Double j2;
    private Double j3;
    private Double j4;
    private Double j5;
    private Double j6;
    private Double j7;
    private Double j8;
    private Double j9;
    private Double j10;
    private Double j11;
    private Double j12;
    private Double j13;
    private Double j14;
    private Double systemServices;
    private Double activity;
    private Double poze1;
    private Double poze2;
    private final Double oze;
    private final Double ote;
    private long validFrom;
    private long validTo;
    private double vat;
    private final String distribution;
    private final String author;
    private final long creationDate;
    private final String email;
    private boolean isChecked = false;


    /**
     * Vytvoří prázdný ceník reprezentující nenalezený záznam s výchozími nulovými hodnotami.
     */
    public PriceListModel() {
        this(NO_PRICE_LIST);
    }


    /**
     * Vytvoří nový ceník se zadaným názvem produktu a výchozími nulovými hodnotami ostatních položek.
     *
     * @param product název produktu či ceníku
     */
    public PriceListModel(String product) {
        this.id = 0L;
        this.product = product;
        this.company = "";
        this.priceVT = 0.0;
        this.priceNT = 0.0;
        this.monthlyPayment = 0.0;
        this.tax = 0.0;
        this.rate = "";
        this.distVT = 0.0;
        this.distNT = 0.0;
        this.j0 = 0.0;
        this.j1 = 0.0;
        this.j2 = 0.0;
        this.j3 = 0.0;
        this.j4 = 0.0;
        this.j5 = 0.0;
        this.j6 = 0.0;
        this.j7 = 0.0;
        this.j8 = 0.0;
        this.j9 = 0.0;
        this.j10 = 0.0;
        this.j11 = 0.0;
        this.j12 = 0.0;
        this.j13 = 0.0;
        this.j14 = 0.0;
        this.systemServices = 0.0;
        this.activity = 0.0;
        this.poze1 = 0.0;
        this.poze2 = 0.0;
        this.oze = 0.0;
        this.ote = 0.0;
        this.validFrom = 0L;
        this.validTo = 0L;
        this.vat = 0.0;
        this.distribution = "";
        this.author = "";
        this.creationDate = 0L;
        this.email = "";
    }


    /**
     * Vytvoří kompletní instanci ceníku se všemi položkami cen a parametrů distribuce.
     *
     * @param id             identifikátor ceníku v databázi
     * @param series         produktová řada ceníku
     * @param product        název konkrétního produktu
     * @param company        obchodní společnost (dodavatel)
     * @param priceVT        obchodní cena vysokého tarifu (VT) v Kč/MWh bez DPH
     * @param priceNT        obchodní cena nízkého tarifu (NT) v Kč/MWh bez DPH
     * @param monthlyPayment stálý měsíční plat za odběrné místo v Kč bez DPH
     * @param tax            daň z elektřiny v Kč/MWh
     * @param rate           distribuční sazba (např. D01d, D02d, D25d)
     * @param distVT         distribuční cena vysokého tarifu v Kč/MWh bez DPH
     * @param distNT         distribuční cena nízkého tarifu v Kč/MWh bez DPH
     * @param j0             měsíční plat za jistič kategorie J0 v Kč bez DPH
     * @param j1             měsíční plat za jistič kategorie J1 v Kč bez DPH
     * @param j2             měsíční plat za jistič kategorie J2 v Kč bez DPH
     * @param j3             měsíční plat za jistič kategorie J3 v Kč bez DPH
     * @param j4             měsíční plat za jistič kategorie J4 v Kč bez DPH
     * @param j5             měsíční plat za jistič kategorie J5 v Kč bez DPH
     * @param j6             měsíční plat za jistič kategorie J6 v Kč bez DPH
     * @param j7             měsíční plat za jistič kategorie J7 v Kč bez DPH
     * @param j8             měsíční plat za jistič kategorie J8 v Kč bez DPH
     * @param j9             měsíční plat za jistič kategorie J9 v Kč bez DPH
     * @param j10            měsíční plat za jistič kategorie J10 v Kč bez DPH
     * @param j11            měsíční plat za jistič kategorie J11 v Kč bez DPH
     * @param j12            měsíční plat za jistič kategorie J12 v Kč bez DPH
     * @param j13            měsíční plat za jistič kategorie J13 v Kč bez DPH
     * @param j14            měsíční plat za jistič kategorie J14 v Kč bez DPH
     * @param systemServices cena za systémové služby v Kč/MWh bez DPH
     * @param activity       cena za činnost zúčtování OTE v Kč/měsíc bez DPH
     * @param poze1          poplatek POZE složka 1 (podle jističe) v Kč bez DPH
     * @param poze2          poplatek POZE složka 2 (podle spotřeby) v Kč/MWh bez DPH
     * @param oze            poplatek OZE v Kč bez DPH
     * @param ote            cena za operátora trhu OTE v Kč bez DPH
     * @param validFrom      počátek platnosti ceníku v milisekundách
     * @param validTo        konec platnosti ceníku v milisekundách
     * @param vat            sazba DPH v procentech
     * @param distribution   kód distribučního území (ČEZ, E.ON, EG.D, PRE)
     * @param author         autor záznamu ceníku
     * @param creationDate   datum vytvoření záznamu v milisekundách
     * @param email          kontaktní e-mail autora
     */
    public PriceListModel(long id, String series, String product, String company, double priceVT,
                          double priceNT, double monthlyPayment, double tax, String rate, double distVT,
                          double distNT, double j0, double j1, double j2, double j3, double j4,
                          double j5, double j6, double j7, double j8, double j9, double j10,
                          double j11, double j12, double j13, double j14, double systemServices,
                          double activity, double poze1, double poze2, double oze, double ote,
                          long validFrom, long validTo, double vat, String distribution,
                          String author, long creationDate, String email) {
        this.id = id;
        this.series = series;
        this.product = product;
        this.company = company;
        this.priceVT = priceVT;
        this.priceNT = priceNT;
        this.monthlyPayment = monthlyPayment;
        this.tax = tax;
        this.rate = rate;
        this.distVT = distVT;
        this.distNT = distNT;
        this.j0 = j0;
        this.j1 = j1;
        this.j2 = j2;
        this.j3 = j3;
        this.j4 = j4;
        this.j5 = j5;
        this.j6 = j6;
        this.j7 = j7;
        this.j8 = j8;
        this.j9 = j9;
        this.j10 = j10;
        this.j11 = j11;
        this.j12 = j12;
        this.j13 = j13;
        this.j14 = j14;
        this.systemServices = systemServices;
        this.activity = activity;
        this.poze1 = poze1;
        this.poze2 = poze2;
        this.oze = oze;
        this.ote = ote;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.vat = vat;
        this.distribution = distribution;
        this.author = author;
        this.creationDate = creationDate;
        this.email = email;
    }


    /**
     * Zobrazí název, sazbu a platnost ceníku.
     *
     * @return název, sazba a platnost ceníku
     */
    public String getName() {
        if (this.product.equals(NO_PRICE_LIST))
            return NO_PRICE_LIST;

        return getProduct() + ", " + getRate()
                + ",\nPlatný: \n" + convertLongToDate(getValidFrom() + 1)
                + " - " + convertLongToDate(getValidTo());
    }


    /**
     * Vrátí formátovaný textový interval platnosti ceníku (např. "01.01.2025 – 31.08.2025").
     *
     * @return textový interval platnosti
     */
    public String getValidityText() {
        if (validFrom > 0 && validTo > 0) {
            return convertLongToDate(validFrom) + " – " + convertLongToDate(validTo);
        } else if (validFrom > 0) {
            return "od " + convertLongToDate(validFrom);
        } else if (validTo > 0) {
            return "do " + convertLongToDate(validTo);
        }
        return "";
    }


    /**
     * Vrátí název ceníku se sazbou a obdobím platnosti na jednom řádku.
     *
     * @return název ceníku s obdobím platnosti
     */
    public String getNameWithPeriod() {
        if (this.product.equals(NO_PRICE_LIST))
            return NO_PRICE_LIST;
        String dates = getValidityText();
        String name = (this.series != null && !this.series.trim().isEmpty() && !this.series.trim().equalsIgnoreCase(this.product.trim()))
                ? this.series.trim() + " - " + getProduct()
                : getProduct();
        if (!dates.isEmpty()) {
            return name + " (" + getRate() + " • " + dates + ")";
        }
        return name + " (" + getRate() + ")";
    }


    /**
     * Zjistí, zdali je ceník prázdný. Kontroluje se podle názvu ceníku.
     *
     * @return {@code true} pokud je prázdný (neexistující), jinak {@code false}
     */
    public boolean isEmpty() {
        return this.product.equals(NO_PRICE_LIST);
    }


    @NonNull
    @Override
    public String toString() {
        return "Ceník:\n" +
                "id: " + getId() + " Řada: " + getSeries() +
                "\nProdukt: " + getProduct() + " Distribuční firma: " + getCompany() +
                "\nCenaVT:" + getPriceVT() + " CenaNT:" + getPriceNT() + " Měsíční plat: " + getMonthlyPayment() + " Daň: " + getTax() +
                "\nSazba: " + getRate() +
                "\nDist. VT: " + getDistVT() + " NT: " + getDistNT() + " J0:" + getJ0() +
                "\nJ1:" + getJ1() + " J2:" + getJ2() + " J3:" + getJ3() +
                "\nJ4:" + getJ4() + " J5:" + getJ5() + " J6:" + getJ6() +
                "\nJ7:" + getJ7() + " J8:" + getJ8() + " J9:" + getJ9() +
                "\nJ10:" + getJ10() + " J11:" + getJ11() + " J12:" + getJ12() +
                "\nJ13:" + getJ13() + " J14:" + getJ14() +
                "\nSystem.služby: " + getSystemServices() + " Činnost OTE:" + getActivity() + " POZE1: " + getPoze1() +
                "\nPOZE2:" + getPoze2() + " OZE:" + getOze() + " OTE:" + getOte() +
                "\nPlatnost OD:" + getValidFrom() + "(" + ViewHelper.convertLongToDate(getValidFrom()) + ") DO:" + getValidTo() + " (" + ViewHelper.convertLongToDate(getValidTo()) + ")" +
                "\nDistribuce: " + getDistribution() +
                "\nDph:" + getVat() + " Datum Vytvoření: " + getCreationDate() + " Autor:" + getAuthor() + " email :" + getEmail();
    }


    /**
     * Vrátí identifikátor ceníku v databázi.
     *
     * @return ID ceníku
     */
    public Long getId() {
        return id;
    }


    /**
     * Vrátí produktovou řadu ceníku.
     *
     * @return řada ceníku
     */
    public String getSeries() {
        return series;
    }


    /**
     * Vrátí název produktu.
     *
     * @return název produktu
     */
    public String getProduct() {
        return product;
    }


    /**
     * Vrátí obchodní společnost (dodavatele).
     *
     * @return název společnosti
     */
    public String getCompany() {
        return company;
    }


    /**
     * Vrátí obchodní cenu vysokého tarifu (VT) v Kč/MWh bez DPH.
     *
     * @return cena VT bez DPH, nebo 0.0 pokud není nastavena
     */
    public Double getPriceVT() {
        return priceVT != null ? priceVT : 0.0;
    }


    /**
     * Vrátí obchodní cenu nízkého tarifu (NT) v Kč/MWh bez DPH.
     *
     * @return cena NT bez DPH, nebo 0.0 pokud není nastavena
     */
    public Double getPriceNT() {
        return priceNT != null ? priceNT : 0.0;
    }


    /**
     * Vrátí stálý měsíční plat za odběrné místo v Kč bez DPH.
     *
     * @return stálý plat bez DPH, nebo 0.0 pokud není nastaven
     */
    public Double getMonthlyPayment() {
        return monthlyPayment != null ? monthlyPayment : 0.0;
    }


    /**
     * Vrátí daň z elektřiny v Kč/MWh.
     *
     * @return daň z elektřiny, nebo 0.0 pokud není nastavena
     */
    public Double getTax() {
        return tax != null ? tax : 0.0;
    }


    /**
     * Vrátí distribuční sazbu (např. D01d, D02d, D25d).
     *
     * @return kód sazby
     */
    public String getRate() {
        return rate;
    }


    /**
     * Vrátí distribuční cenu vysokého tarifu (VT) v Kč/MWh bez DPH.
     *
     * @return distribuční cena VT bez DPH, nebo 0.0 pokud není nastavena
     */
    public Double getDistVT() {
        return distVT != null ? distVT : 0.0;
    }


    /**
     * Vrátí distribuční cenu nízkého tarifu (NT) v Kč/MWh bez DPH.
     *
     * @return distribuční cena NT bez DPH, nebo 0.0 pokud není nastavena
     */
    public Double getDistNT() {
        return distNT != null ? distNT : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J0 v Kč bez DPH.
     *
     * @return plat za jistič J0
     */
    public Double getJ0() {
        return j0 != null ? j0 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J1 v Kč bez DPH.
     *
     * @return plat za jistič J1
     */
    public Double getJ1() {
        return j1 != null ? j1 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J2 v Kč bez DPH.
     *
     * @return plat za jistič J2
     */
    public Double getJ2() {
        return j2 != null ? j2 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J3 v Kč bez DPH.
     *
     * @return plat za jistič J3
     */
    public Double getJ3() {
        return j3 != null ? j3 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J4 v Kč bez DPH.
     *
     * @return plat za jistič J4
     */
    public Double getJ4() {
        return j4 != null ? j4 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J5 v Kč bez DPH.
     *
     * @return plat za jistič J5
     */
    public Double getJ5() {
        return j5 != null ? j5 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J6 v Kč bez DPH.
     *
     * @return plat za jistič J6
     */
    public Double getJ6() {
        return j6 != null ? j6 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J7 v Kč bez DPH.
     *
     * @return plat za jistič J7
     */
    public Double getJ7() {
        return j7 != null ? j7 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J8 v Kč bez DPH.
     *
     * @return plat za jistič J8
     */
    public Double getJ8() {
        return j8 != null ? j8 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J9 v Kč bez DPH.
     *
     * @return plat za jistič J9
     */
    public Double getJ9() {
        return j9 != null ? j9 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J10 v Kč bez DPH.
     *
     * @return plat za jistič J10
     */
    public Double getJ10() {
        return j10 != null ? j10 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J11 v Kč bez DPH.
     *
     * @return plat za jistič J11
     */
    public Double getJ11() {
        return j11 != null ? j11 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J12 v Kč bez DPH.
     *
     * @return plat za jistič J12
     */
    public Double getJ12() {
        return j12 != null ? j12 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J13 v Kč bez DPH.
     *
     * @return plat za jistič J13
     */
    public Double getJ13() {
        return j13 != null ? j13 : 0.0;
    }


    /**
     * Vrátí měsíční plat za jistič kategorie J14 v Kč bez DPH.
     *
     * @return plat za jistič J14
     */
    public Double getJ14() {
        return j14 != null ? j14 : 0.0;
    }


    /**
     * Vrátí cenu za systémové služby v Kč/MWh bez DPH.
     *
     * @return cena za systémové služby, nebo 0.0 pokud není nastavena
     */
    public Double getSystemServices() {
        return systemServices != null ? systemServices : 0.0;
    }


    /**
     * Vrátí poplatek za činnost zúčtování OTE v Kč/měsíc bez DPH.
     *
     * @return poplatek OTE za měsíc, nebo 0.0 pokud není nastaven
     */
    public Double getActivity() {
        return activity != null ? activity : 0.0;
    }


    /**
     * Vrátí složku POZE 1 (podle jističe) v Kč bez DPH.
     *
     * @return složka POZE 1, nebo 0.0 pokud není nastavena
     */
    public Double getPoze1() {
        return poze1 != null ? poze1 : 0.0;
    }


    /**
     * Vrátí složku POZE 2 (podle spotřeby) v Kč/MWh bez DPH.
     *
     * @return složka POZE 2, nebo 0.0 pokud není nastavena
     */
    public Double getPoze2() {
        return poze2 != null ? poze2 : 0.0;
    }


    /**
     * Vrátí poplatek OZE v Kč bez DPH.
     *
     * @return poplatek OZE, nebo 0.0 pokud není nastaven
     */
    public Double getOze() {
        return oze != null ? oze : 0.0;
    }


    /**
     * Vrátí poplatek OTE v Kč bez DPH.
     *
     * @return poplatek OTE, nebo 0.0 pokud není nastaven
     */
    public Double getOte() {
        return ote != null ? ote : 0.0;
    }


    /**
     * Vrátí počátek platnosti ceníku v milisekundách.
     *
     * @return počátek platnosti
     */
    public Long getValidFrom() {
        return validFrom;
    }


    /**
     * Vrátí konec platnosti ceníku v milisekundách.
     *
     * @return konec platnosti
     */
    public Long getValidTo() {
        return validTo;
    }


    /**
     * Vrátí sazbu DPH v procentech (např. 21.0).
     *
     * @return sazba DPH
     */
    public Double getVat() {
        return vat;
    }


    /**
     * Vrací oblast distribuce (ČEZ, E.ON, EG.D, PRE) s automatickou konverzí podle roku platnosti:
     * do roku 2020 E.ON, od roku 2021 EG.D.
     *
     * @return název distribuční oblasti
     */
    public String getDistribution() {
        if (getValidityYear() >= 2021) {
            if (distribution.equals(EON))
                return EGD;
        } else {
            if (distribution.equals(EGD))
                return EON;
        }
        return distribution;
    }


    /**
     * Vrátí autora záznamu ceníku.
     *
     * @return autor ceníku
     */
    public String getAuthor() {
        return author;
    }


    /**
     * Vrátí datum a čas vytvoření záznamu v milisekundách.
     *
     * @return datum vytvoření
     */
    public Long getCreationDate() {
        return creationDate;
    }


    /**
     * Vrátí kalendářní rok počátku platnosti ceníku.
     *
     * @return rok počátku platnosti
     */
    public int getValidityYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getValidFrom());
        return calendar.get(Calendar.YEAR);
    }


    /**
     * Vrátí kontaktní e-mail autora záznamu.
     *
     * @return e-mail autora
     */
    public String getEmail() {
        return email;
    }


    /**
     * Vrátí příznak, zda je položka ceníku označena (vybrána) v uživatelském rozhraní.
     *
     * @return {@code true}, pokud je ceník označen
     */
    public boolean isChecked() {
        return isChecked;
    }


    /**
     * Nastaví identifikátor ceníku v databázi.
     *
     * @param id ID ceníku
     */
    public void setId(Long id) {
        this.id = id;
    }


    /**
     * Nastaví obchodní cenu vysokého tarifu (VT) v Kč/MWh bez DPH.
     *
     * @param priceVT cena VT
     */
    public void setPriceVT(Double priceVT) {
        this.priceVT = priceVT;
    }


    /**
     * Nastaví obchodní cenu nízkého tarifu (NT) v Kč/MWh bez DPH.
     *
     * @param priceNT cena NT
     */
    public void setPriceNT(Double priceNT) {
        this.priceNT = priceNT;
    }


    /**
     * Nastaví stálý měsíční plat za odběrné místo v Kč bez DPH.
     *
     * @param monthlyPayment měsíční plat
     */
    public void setMonthlyPayment(Double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }


    /**
     * Nastaví daň z elektřiny v Kč/MWh.
     *
     * @param tax daň z elektřiny
     */
    public void setTax(Double tax) {
        this.tax = tax;
    }


    /**
     * Nastaví distribuční sazbu.
     *
     * @param rate kód sazby
     */
    public void setRate(String rate) {
        this.rate = rate;
    }


    /**
     * Nastaví distribuční cenu vysokého tarifu (VT) v Kč/MWh bez DPH.
     *
     * @param distVT distribuční cena VT
     */
    public void setDistVT(Double distVT) {
        this.distVT = distVT;
    }


    /**
     * Nastaví distribuční cenu nízkého tarifu (NT) v Kč/MWh bez DPH.
     *
     * @param distNT distribuční cena NT
     */
    public void setDistNT(Double distNT) {
        this.distNT = distNT;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J0 v Kč bez DPH.
     *
     * @param j0 plat za jistič J0
     */
    public void setJ0(Double j0) {
        this.j0 = j0;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J1 v Kč bez DPH.
     *
     * @param j1 plat za jistič J1
     */
    public void setJ1(Double j1) {
        this.j1 = j1;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J2 v Kč bez DPH.
     *
     * @param j2 plat za jistič J2
     */
    public void setJ2(Double j2) {
        this.j2 = j2;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J3 v Kč bez DPH.
     *
     * @param j3 plat za jistič J3
     */
    public void setJ3(Double j3) {
        this.j3 = j3;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J4 v Kč bez DPH.
     *
     * @param j4 plat za jistič J4
     */
    public void setJ4(Double j4) {
        this.j4 = j4;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J5 v Kč bez DPH.
     *
     * @param j5 plat za jistič J5
     */
    public void setJ5(Double j5) {
        this.j5 = j5;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J6 v Kč bez DPH.
     *
     * @param j6 plat za jistič J6
     */
    public void setJ6(Double j6) {
        this.j6 = j6;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J7 v Kč bez DPH.
     *
     * @param j7 plat za jistič J7
     */
    public void setJ7(Double j7) {
        this.j7 = j7;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J8 v Kč bez DPH.
     *
     * @param j8 plat za jistič J8
     */
    public void setJ8(Double j8) {
        this.j8 = j8;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J9 v Kč bez DPH.
     *
     * @param j9 plat za jistič J9
     */
    public void setJ9(Double j9) {
        this.j9 = j9;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J10 v Kč bez DPH.
     *
     * @param j10 plat za jistič J10
     */
    public void setJ10(Double j10) {
        this.j10 = j10;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J11 v Kč bez DPH.
     *
     * @param j11 plat za jistič J11
     */
    public void setJ11(Double j11) {
        this.j11 = j11;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J12 v Kč bez DPH.
     *
     * @param j12 plat za jistič J12
     */
    public void setJ12(Double j12) {
        this.j12 = j12;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J13 v Kč bez DPH.
     *
     * @param j13 plat za jistič J13
     */
    public void setJ13(Double j13) {
        this.j13 = j13;
    }


    /**
     * Nastaví měsíční plat za jistič kategorie J14 v Kč bez DPH.
     *
     * @param j14 plat za jistič J14
     */
    public void setJ14(Double j14) {
        this.j14 = j14;
    }


    /**
     * Nastaví cenu za systémové služby v Kč/MWh bez DPH.
     *
     * @param systemServices cena systémových služeb
     */
    public void setSystemServices(Double systemServices) {
        this.systemServices = systemServices;
    }


    /**
     * Nastaví poplatek za činnost zúčtování OTE v Kč/měsíc bez DPH.
     *
     * @param activity poplatek OTE
     */
    public void setActivity(Double activity) {
        this.activity = activity;
    }


    /**
     * Nastaví složku POZE 1 (podle jističe) v Kč bez DPH.
     *
     * @param poze1 složka POZE 1
     */
    public void setPoze1(Double poze1) {
        this.poze1 = poze1;
    }


    /**
     * Nastaví složku POZE 2 (podle spotřeby) v Kč/MWh bez DPH.
     *
     * @param poze2 složka POZE 2
     */
    public void setPoze2(Double poze2) {
        this.poze2 = poze2;
    }


    /**
     * Nastaví sazbu DPH v procentech.
     *
     * @param vat sazba DPH
     */
    public void setVat(Double vat) {
        this.vat = vat;
    }


    /**
     * Nastaví příznak označení ceníku.
     *
     * @param checked {@code true} pokud je ceník označen
     */
    public void setChecked(boolean checked) {
        isChecked = checked;
    }


    /**
     * Nastaví koncové datum platnosti ceníku v milisekundách.
     *
     * @param validTo konec platnosti
     */
    public void setValidTo(Long validTo) {
        this.validTo = validTo;
    }


    /**
     * Nastaví počáteční datum platnosti ceníku v milisekundách.
     *
     * @param validFrom počátek platnosti
     */
    public void setValidFrom(Long validFrom) {
        this.validFrom = validFrom;
    }

}
