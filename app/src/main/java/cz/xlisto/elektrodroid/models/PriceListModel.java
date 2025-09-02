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
    public final static String PRE = "PRE";
    public final static String EON = "E.ON";
    public final static String EGD = "EG.D";
    private final static String NO_PRICE_LIST = "Ceník nenalezen!!!";
    private long id;
    private String rada;
    private String produkt;
    private final String firma;
    private Double cenaVT;
    private Double cenaNT;
    private Double mesicniPlat;
    private Double dan;
    private String sazba;
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
    private Double systemSluzby;
    private Double cinnost;
    private Double poze1;
    private Double poze2;
    private final Double oze;
    private final Double ote;
    private long platnostOD;
    private long platnostDO;
    private double dph;
    private final String distribuce;
    private final String autor;
    private final long datumVytvoreni;
    private final String email;
    private boolean isChecked = false;


    //nulový konstruktor s názvem Ceník nenalezen
    public PriceListModel() {
        this(NO_PRICE_LIST);
    }


    //nulový konstruktor
    public PriceListModel(String produkt) {
        this.id = 0L;
        this.produkt = produkt;
        this.firma = "";
        this.cenaVT = 0.0;
        this.cenaNT = 0.0;
        this.mesicniPlat = 0.0;
        this.dan = 0.0;
        this.sazba = "";
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
        this.systemSluzby = 0.0;
        this.cinnost = 0.0;
        this.poze1 = 0.0;
        this.poze2 = 0.0;
        this.oze = 0.0;
        this.ote = 0.0;
        this.platnostOD = 0L;
        this.platnostDO = 0L;
        this.dph = 0.0;
        this.distribuce = "";
        this.autor = "";
        this.datumVytvoreni = 0L;
        this.email = "";
    }


    public PriceListModel(long id, String rada, String produkt, String firma, double cenaVT,
                          double cenaNT, double mesicniPlat, double dan, String sazba, double distVT,
                          double distNT, double j0, double j1, double j2, double j3, double j4,
                          double j5, double j6, double j7, double j8, double j9, double j10,
                          double j11, double j12, double j13, double j14, double systemSluzby,
                          double cinnost, double poze1, double poze2, double oze, double ote,
                          long platnostOD, long platnostDO, double dph, String distribuce,
                          String autor, long datumVytvoreni, String email) {
        this.id = id;
        this.rada = rada;
        this.produkt = produkt;
        this.firma = firma;
        this.cenaVT = cenaVT;
        this.cenaNT = cenaNT;
        this.mesicniPlat = mesicniPlat;
        this.dan = dan;
        this.sazba = sazba;
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
        this.systemSluzby = systemSluzby;
        this.cinnost = cinnost;
        this.poze1 = poze1;
        this.poze2 = poze2;
        this.oze = oze;
        this.ote = ote;
        this.platnostOD = platnostOD;
        this.platnostDO = platnostDO;
        this.dph = dph;
        this.distribuce = distribuce;
        this.autor = autor;
        this.datumVytvoreni = datumVytvoreni;
        this.email = email;
    }


    /**
     * Zobrazí název, sazbu a platnost ceníku
     *
     * @return String název, sazba a platnost ceníku
     */
    public String getName() {
        if (this.produkt.equals(NO_PRICE_LIST))
            return NO_PRICE_LIST;

        return getProdukt() + ", " + getSazba()
                + ",\nPlatný: \n" + convertLongToDate(getPlatnostOD() + 1)
                + " - " + convertLongToDate(getPlatnostDO());
    }


    /**
     * Zjistí, zdali je ceník prázdný. Kontroluje se podle názvu ceníku.
     *
     * @return boolean true - prázdný, false - není prázdný
     */
    public boolean isEmpty() {
        return this.produkt.equals(NO_PRICE_LIST);
    }


    @NonNull
    public String toString() {
        return "Ceník:\n" +
                "id: " + getId() + " Řada: " + getRada() +
                "\nProdukt: " + getProdukt() + " Distribuční firma: " + getFirma() +
                "\nCenaVT:" + getCenaVT() + " CenaNT:" + getCenaNT() + " Měsíční plat: " + getMesicniPlat() + " Daň: " + getDan() +
                "\nSazba: " + getSazba() +
                "\nDist. VT: " + getDistVT() + " NT: " + getDistNT() + " J0:" + getJ0() +
                "\nJ1:" + getJ1() + " J2:" + getJ2() + " J3:" + getJ3() +
                "\nJ4:" + getJ4() + " J5:" + getJ5() + " J6:" + getJ6() +
                "\nJ7:" + getJ7() + " J8:" + getJ8() + " J9:" + getJ9() +
                "\nJ10:" + getJ10() + " J11:" + getJ11() + " J12:" + getJ12() +
                "\nJ13:" + getJ13() + " J14:" + getJ14() +
                "\nSystem.služby: " + getSystemSluzby() + " Činnost OTE:" + getCinnost() + " POZE1: " + getPoze1() +
                "\nPOZE2:" + getPoze2() + " OZE:" + getOze() + " OTE:" + getOte() +
                "\nPlatnost OD:" + getPlatnostOD() + "(" + ViewHelper.convertLongToDate(getPlatnostOD()) + ") DO:" + getPlatnostDO() + " (" + ViewHelper.convertLongToDate(getPlatnostDO()) + ")" +
                "\nDistribuce: " + getDistribuce() +
                "\nDph:" + getDph() + " Datum Vytvoření: " + getDatumVytvoreni() + " Autor:" + getAutor() + " email :" + getEmail();
    }


    public Long getId() {
        return id;
    }


    public String getRada() {
        return rada;
    }


    public String getProdukt() {
        return produkt;
    }


    public String getFirma() {
        return firma;
    }


    public Double getCenaVT() {
        return cenaVT != null ? cenaVT : 0.0;
    }


    public Double getCenaNT() {
        return cenaNT != null ? cenaNT : 0.0;
    }


    public Double getMesicniPlat() {
        return mesicniPlat != null ? mesicniPlat : 0.0;
    }


    public Double getDan() {
        return dan != null ? dan : 0.0;
    }


    public String getSazba() {
        return sazba;
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


    public Double getSystemSluzby() {
        return systemSluzby != null ? systemSluzby : 0.0;
    }


    public Double getCinnost() {
        return cinnost != null ? cinnost : 0.0;
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


    public Long getPlatnostOD() {
        return platnostOD;
    }


    public Long getPlatnostDO() {
        return platnostDO;
    }


    public Double getDph() {
        return dph;
    }


    /**
     * Vrací oblast distribuce. (ČEZ, E.ON, EG.D, PRE)
     * DO ROKU 2020: ČEZ, E.ON, PRE
     * OD ROKU 2021: ČEZ, EG.D, PRE
     *
     * @return string oblasti distribuce
     */
    public String getDistribuce() {
        if (getRokPlatnost() >= 2021) {
            if (distribuce.equals(EON))
                return EGD;
        } else {
            if (distribuce.equals(EGD))
                return EON;
        }
        return distribuce;
    }


    public String getAutor() {
        return autor;
    }


    public Long getDatumVytvoreni() {
        return datumVytvoreni;
    }


    public int getRokPlatnost() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getPlatnostOD());
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


    public void setProdukt(String produkt) {
        this.produkt = produkt;
    }


    public void setCenaVT(Double cenaVT) {
        this.cenaVT = cenaVT;
    }


    public void setCenaNT(Double cenaNT) {
        this.cenaNT = cenaNT;
    }


    public void setMesicniPlat(Double mesicniPlat) {
        this.mesicniPlat = mesicniPlat;
    }


    public void setDan(Double dan) {
        this.dan = dan;
    }


    public void setSazba(String sazba) {
        this.sazba = sazba;
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


    public void setSystemSluzby(Double systemSluzby) {
        this.systemSluzby = systemSluzby;
    }


    public void setCinnost(Double cinnost) {
        this.cinnost = cinnost;
    }


    public void setPoze1(Double poze1) {
        this.poze1 = poze1;
    }


    public void setPoze2(Double poze2) {
        this.poze2 = poze2;
    }


    public void setDph(Double dph) {
        this.dph = dph;
    }


    public void setChecked(boolean checked) {
        isChecked = checked;
    }


    public void setPlatnostDO(Long platnostDO) {
        this.platnostDO = platnostDO;
    }


    public void setPlatnostOD(Long platnostOD) {
        this.platnostOD = platnostOD;
    }

}
