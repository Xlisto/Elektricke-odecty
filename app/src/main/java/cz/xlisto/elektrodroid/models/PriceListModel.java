package cz.xlisto.elektrodroid.models;


import static cz.xlisto.elektrodroid.ownview.ViewHelper.convertLongToDate;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Model ceníku
 */
public class PriceListModel implements Serializable {

    public final static int NEW_POZE_YEAR = 2016;
    public final static String EON = "E.ON";
    public final static String EGD = "EG.D";
    private final static String NO_PRICE_LIST = "Ceník nenalezen!!!";
    private long id;
    private String series;
    private String product;
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


    //nulový konstruktor s názvem Ceník nenalezen
    public PriceListModel() {
        this(NO_PRICE_LIST);
    }


    //nulový konstruktor
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
     * Zobrazí název, sazbu a platnost ceníku
     *
     * @return String název, sazba a platnost ceníku
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
     * @return boolean true - prázdný, false - není prázdný
     */
    public boolean isEmpty() {
        return this.product.equals(NO_PRICE_LIST);
    }


    @NonNull
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


    public Long getId() {
        return id;
    }


    public String getSeries() {
        return series;
    }


    public String getProduct() {
        return product;
    }


    public String getCompany() {
        return company;
    }


    public Double getPriceVT() {
        return priceVT != null ? priceVT : 0.0;
    }


    public Double getPriceNT() {
        return priceNT != null ? priceNT : 0.0;
    }


    public Double getMonthlyPayment() {
        return monthlyPayment != null ? monthlyPayment : 0.0;
    }


    public Double getTax() {
        return tax != null ? tax : 0.0;
    }


    public String getRate() {
        return rate;
    }


    public Double getDistVT() {
        return distVT != null ? distVT : 0.0;
    }


    public Double getDistNT() {
        return distNT != null ? distNT : 0.0;
    }


    public Double getJ0() {
        return j0 != null ? j0 : 0.0;
    }


    public Double getJ1() {
        return j1 != null ? j1 : 0.0;
    }


    public Double getJ2() {
        return j2 != null ? j2 : 0.0;
    }


    public Double getJ3() {
        return j3 != null ? j3 : 0.0;
    }


    public Double getJ4() {
        return j4 != null ? j4 : 0.0;
    }


    public Double getJ5() {
        return j5 != null ? j5 : 0.0;
    }


    public Double getJ6() {
        return j6 != null ? j6 : 0.0;
    }


    public Double getJ7() {
        return j7 != null ? j7 : 0.0;
    }


    public Double getJ8() {
        return j8 != null ? j8 : 0.0;
    }


    public Double getJ9() {
        return j9 != null ? j9 : 0.0;
    }


    public Double getJ10() {
        return j10 != null ? j10 : 0.0;
    }


    public Double getJ11() {
        return j11 != null ? j11 : 0.0;
    }


    public Double getJ12() {
        return j12 != null ? j12 : 0.0;
    }


    public Double getJ13() {
        return j13 != null ? j13 : 0.0;
    }


    public Double getJ14() {
        return j14 != null ? j14 : 0.0;
    }


    public Double getSystemServices() {
        return systemServices != null ? systemServices : 0.0;
    }


    public Double getActivity() {
        return activity != null ? activity : 0.0;
    }


    public Double getPoze1() {
        return poze1 != null ? poze1 : 0.0;
    }


    public Double getPoze2() {
        return poze2 != null ? poze2 : 0.0;
    }


    public Double getOze() {
        return oze != null ? oze : 0.0;
    }


    public Double getOte() {
        return ote != null ? ote : 0.0;
    }


    public Long getValidFrom() {
        return validFrom;
    }


    public Long getValidTo() {
        return validTo;
    }


    public Double getVat() {
        return vat;
    }


    /**
     * Vrací oblast distribuce. (ČEZ, E.ON, EG.D, PRE)
     * DO ROKU 2020: ČEZ, E.ON, PRE
     * OD ROKU 2021: ČEZ, EG.D, PRE
     *
     * @return string oblasti distribuce
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


    public String getAuthor() {
        return author;
    }


    public Long getCreationDate() {
        return creationDate;
    }


    public int getValidityYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getValidFrom());
        return calendar.get(Calendar.YEAR);
    }


    public String getEmail() {
        return email;
    }


    public boolean isChecked() {
        return isChecked;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public void setProduct(String product) {
        this.product = product;
    }


    public void setPriceVT(Double priceVT) {
        this.priceVT = priceVT;
    }


    public void setPriceNT(Double priceNT) {
        this.priceNT = priceNT;
    }


    public void setMonthlyPayment(Double monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }


    public void setTax(Double tax) {
        this.tax = tax;
    }


    public void setRate(String rate) {
        this.rate = rate;
    }


    public void setDistVT(Double distVT) {
        this.distVT = distVT;
    }


    public void setDistNT(Double distNT) {
        this.distNT = distNT;
    }


    public void setJ0(Double j0) {
        this.j0 = j0;
    }


    public void setJ1(Double j1) {
        this.j1 = j1;
    }


    public void setJ2(Double j2) {
        this.j2 = j2;
    }


    public void setJ3(Double j3) {
        this.j3 = j3;
    }


    public void setJ4(Double j4) {
        this.j4 = j4;
    }


    public void setJ5(Double j5) {
        this.j5 = j5;
    }


    public void setJ6(Double j6) {
        this.j6 = j6;
    }


    public void setJ7(Double j7) {
        this.j7 = j7;
    }


    public void setJ8(Double j8) {
        this.j8 = j8;
    }


    public void setJ9(Double j9) {
        this.j9 = j9;
    }


    public void setJ10(Double j10) {
        this.j10 = j10;
    }


    public void setJ11(Double j11) {
        this.j11 = j11;
    }


    public void setJ12(Double j12) {
        this.j12 = j12;
    }


    public void setJ13(Double j13) {
        this.j13 = j13;
    }


    public void setJ14(Double j14) {
        this.j14 = j14;
    }


    public void setSystemServices(Double systemServices) {
        this.systemServices = systemServices;
    }


    public void setActivity(Double activity) {
        this.activity = activity;
    }


    public void setPoze1(Double poze1) {
        this.poze1 = poze1;
    }


    public void setPoze2(Double poze2) {
        this.poze2 = poze2;
    }


    public void setVat(Double vat) {
        this.vat = vat;
    }


    public void setChecked(boolean checked) {
        isChecked = checked;
    }


    public void setValidTo(Long validTo) {
        this.validTo = validTo;
    }


    public void setValidFrom(Long validFrom) {
        this.validFrom = validFrom;
    }

}
