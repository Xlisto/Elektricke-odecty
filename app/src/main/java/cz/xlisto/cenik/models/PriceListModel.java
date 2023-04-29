package cz.xlisto.cenik.models;

import java.util.Calendar;

import static cz.xlisto.cenik.ownview.ViewHelper.convertLongToTime;

/**
 * Model ceníku
 */
public class PriceListModel {
    public final static int NEW_POZE_YEAR = 2016;
    private long id;
    private String rada;
    private String produkt;
    private String firma;
    private double cenaVT;
    private double cenaNT;
    private double mesicniPlat;
    private double dan;
    private String sazba;
    private double distVT;
    private double distNT;
    private double j0;
    private double j1;
    private double j2;
    private double j3;
    private double j4;
    private double j5;
    private double j6;
    private double j7;
    private double j8;
    private double j9;
    private double j10;
    private double j11;
    private double j12;
    private double j13;
    private double j14;
    private double systemSluzby;
    private double cinnost;
    private double poze1;
    private double poze2;
    private double oze;
    private double ote;
    private long platnostOD;
    private long platnostDO;
    private double dph;
    private String distribuce;
    private String autor;
    private long datumVytvoreni;
    private String email;
    private String mwh = " Kč/MWh";
    private String mes = " Kč/měsíc";
    private String jis = " Kč za 1A/měsíc";

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

    //konstruktor

    public PriceListModel() {
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
     * @return
     */
    public String getName(){
        return getProdukt() + ", " +getSazba() + ",\nPlatný od: " + convertLongToTime(getPlatnostOD());
    }

    public String toString() {
        String s = "Ceník:\n" +
                "id: " + getId() + " Řada: " + getRada() +
                "\nProdukt: " + getProdukt() + " Distribuční firma: " + getFirma() +
                "\nCenaVT:" + getCenaVT() + " CenaNT:" + getCenaNT() + " Měsíční plat: " + getMesicniPlat() + " Daň: " + getDan() +
                "\nSazba: " + getSazba() +
                "\nDist. VT: " + getDistVT() + " NT: " + getDistNT() + " J0:" + getJ0() +
                "\nJ1:" + getJ1() + " J2:" + getJ2() + " J3:" + getJ3() +
                "\nJ4:" + getJ4() + " J5:" + getJ5() + " J6:" + getJ6() +
                "\nJ7:" + getJ7() + " J8:" + getJ8() + " J9:" + getJ9() +
                "\nJ10:" + getJ10() + " J11:" + getJ11() +" J12:" + getJ12() +
                "\nJ13:" + getJ13() + " J14:" + getJ14() +
                "\nSystem.služby: " + getSystemSluzby() + " Činnost OTE:" + getCinnost() +" POZE1: " + getPoze1() +
                "\nPOZE2:" + getPoze2() + " OZE:" + getOze() + " OTE:" + getOte() +
                "\nPlatnost OD:" + getPlatnostOD() + " DO:" + getPlatnostDO() +
                "\nDistribuce: " + getDistribuce() +
                "\nDph:" + getDph() +  " Datum Vytvoření: " + getDatumVytvoreni() + " Autor:" + getAutor()+" email :"+getEmail();
        return s;
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
        return cenaVT;
    }

    public Double getCenaNT() {
        return cenaNT;
    }

    public Double getMesicniPlat() {
        return mesicniPlat;
    }

    public Double getDan() {
        return dan;
    }

    public String getSazba() {
        return sazba;
    }

    public Double getDistVT() {
        return distVT;
    }

    public Double getDistNT() {
        return distNT;
    }

    public Double getJ0() {
        return j0;
    }

    public Double getJ1() {
        return j1;
    }

    public Double getJ2() {
        return j2;
    }

    public Double getJ3() {
        return j3;
    }

    public Double getJ4() {
        return j4;
    }

    public Double getJ5() {
        return j5;
    }

    public Double getJ6() {
        return j6;
    }

    public Double getJ7() {
        return j7;
    }

    public Double getJ8() {
        return j8;
    }

    public Double getJ9() {
        return j9;
    }

    public Double getJ10() {
        return j10;
    }

    public Double getJ11() {
        return j11;
    }

    public Double getJ12() {
        return j12;
    }

    public Double getJ13() {
        return j13;
    }

    public Double getJ14() {
        return j14;
    }

    public Double getSystemSluzby() {
        return systemSluzby;
    }

    public Double getCinnost() {
        return cinnost;
    }

    public Double getPoze1() {
        return poze1;
    }

    public Double getPoze2() {
        return poze2;
    }

    public Double getOze() {
        return oze;
    }

    public Double getOte() {
        return ote;
    }

    public Long getPlatnostOD() {
        return platnostOD;
    }

    /*public String getPlatnostODAsString() {
        return HlavniActivity.formatDatumu.format(getPlatnostOD());
    }*/

    public Long getPlatnostDO() {
        return platnostDO;
    }

    /*public String getPlatnostDOAsString() {
        return HlavniActivity.formatDatumu.format(getPlatnostDO());
    }*/

    public Double getDph() {
        return dph;
    }

    public String getDistribuce() {
        return distribuce;
    }

    public String getAutor() {
        return autor;
    }

    public Long getDatumVytvoreni() {
        return datumVytvoreni;
    }

    public int getRokPlatnost(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getPlatnostOD());
        return calendar.get(Calendar.YEAR);
    }

    public String getEmail() {
        return email;
    }

    public String getRadaName() {
        return "Produktová řada";
    }

    public String getProduktName() {
        return "Produkt";
    }

    public String getFirmaName() {
        return "Distribuční firma";
    }

    public String getCenaVTName() {
        return "Cena ve VT";
    }

    public String getCenaNTName() {
        return "Cena ve NT";
    }

    public String getMesicniPlatName() {
        return "Stálý měsíční plat";
    }

    public String getDanName() {
        return "Daň z elektřiny";
    }

    public String getSazbaName() {
        return "Sazba distribuce";
    }

    public String getDistVTName() {
        return "Distribuce ve VT";
    }

    public String getDistNTName() {
        return "Distribuce ve NT";
    }

    public String getJ0Name() {
        return "Jistič do 3x10A a do 1x25A vč.";
    }

    public String getJ1Name() {
        return "Jistič nad 3x10A a do 3x16A vč.";
    }

    public String getJ2Name() {
        return "Jistič nad 3x16A a do 3x20A vč.";
    }

    public String getJ3Name() {
        return "Jistič nad 3x20A a do 3x25A vč.";
    }

    public String getJ4Name() {
        return "Jistič nad 3x25A a do 3x32A vč.";
    }

    public String getJ5Name() {
        return "Jistič nad 3x32A a do 3x40A vč.";
    }

    public String getJ6Name() {
        return "Jistič nad 3x40A a do 3x50A vč.";
    }

    public String getJ7Name() {
        return "Jistič nad 3x50A a do 3x63A vč.";
    }

    public String getJ8Name() {
        return "Nad 3x63A a za A";
    }

    public String getJ9Name() {
        return "Nad 1x25A a za A";
    }

    public String getJ10Name() {
        return "Jistič nad 3x63A a do 3x80A vč.";
    }

    public String getJ11Name() {
        return "Jistič nad 3x80A a do 3x100A vč.";
    }

    public String getJ12Name() {
        return "Jistič nad 3x100A a do 3x125A vč.";
    }

    public String getJ13Name() {
        return "Jistič nad 3x125A a do 3x160A vč.";
    }

    public String getJ14Name() {
        return "Nad 3x160A a za A";
    }

    public String getSystemSluzbyName() {
        return "Systémové služby";
    }

    public String getCinnostName() {
        return "Činnost operátora trhu";
    }

    public String getPoze1Name() {
        return "POZE dle jističe";
    }

    public String getPoze2Name() {
        return "POZE dle spotřeby";
    }

    public String getOzeName() {
        return "Podpora výkupu el. z OZE, KVET a DZ";
    }

    public String getOteName() {
        return "Činnost OTE";
    }

    public String getPlatnostODName() {
        return "Platnost OD";
    }

    public String getPlatnost() {
        return "Platnost";
    }

    public String getPlatnostDOName() {
        return "Platnost DO";
    }

    public String getDphName() {
        return "DPH";
    }

    public String getDistribuceName() {
        return "Distribuční území";
    }


    public void setId(Long id) {
        this.id = id;
    }

    public void setRada(String rada) {
        this.rada = rada;
    }

    public void setProdukt(String produkt) {
        this.produkt = produkt;
    }

    public void setFirma(String firma) {
        this.firma = firma;
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

    public void setOze(Double oze) {
        this.oze = oze;
    }

    public void setOte(Double ote) {
        this.ote = ote;
    }

    public void setPlatnostOD(Long platnostOD) {
        this.platnostOD = platnostOD;
    }

    public void setPlatnostDO(Long platnostDO) {
        this.platnostDO = platnostDO;
    }

    public void setDph(Double dph) {
        this.dph = dph;
    }

    public void setDistribuce(String distribuce) {
        this.distribuce = distribuce;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public void setDatumVytvoreni(Long datumVytvoreni) {
        this.datumVytvoreni = datumVytvoreni;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
