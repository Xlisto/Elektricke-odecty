package cz.xlisto.odecty.models;

import androidx.annotation.NonNull;


/**
 * Model pro časy HDO
 * Xlisto 26.05.2023 18:44
 */

public class HdoModel {
    private static final String TAG = "HdoModel";
    private long id;
    private final String rele;
    private final String timeFrom;
    private final String timeUntil;
    private final String dateFrom;
    private final String dateUntil;
    private final String distributionArea;


    private final int mon, tue, wed, thu, fri, sat, sun;


    public HdoModel(long id, String rele, String dateFrom, String dateUntil, String timeFrom, String timeUntil, int mon, int tue, int wed, int thu, int fri, int sat, int sun, String distributionArea) {
        this.id = id;
        this.rele = rele;
        this.dateFrom = dateFrom;
        this.dateUntil = dateUntil;
        this.timeFrom = timeFrom;
        this.timeUntil = timeUntil;
        this.mon = mon;
        this.tue = tue;
        this.wed = wed;
        this.thu = thu;
        this.fri = fri;
        this.sat = sat;
        this.sun = sun;
        this.distributionArea = distributionArea;
    }


    public HdoModel(String rele, String dateFrom, String dateUntil, String timeFrom, String timeUntil, int mon, int tue, int wed, int thu, int fri, int sat, int sun, String distributionArea) {
        this(-1, rele, dateFrom, dateUntil, timeFrom, timeUntil, mon, tue, wed, thu, fri, sat, sun, distributionArea);
    }


    public void setId(long id) {
        this.id = id;
    }


    public long getId() {
        return id;
    }


    public String getRele() {
        return rele;
    }


    public String getDateFrom() {
        return dateFrom;
    }


    public String getDateUntil() {
        return dateUntil;
    }


    public String getTimeFrom() {
        return timeFrom;
    }


    public String getTimeUntil() {
        if(timeUntil.equals("23:59"))
            return "0:00";
        return timeUntil;
    }


    public int getMon() {
        return mon;
    }


    public int getTue() {
        return tue;
    }


    public int getWed() {
        return wed;
    }


    public int getThu() {
        return thu;
    }


    public int getFri() {
        return fri;
    }


    public int getSat() {
        return sat;
    }


    public int getSun() {
        return sun;
    }


    public String getDistributionArea() {
        if (distributionArea == null)
            return "";
        return distributionArea;
    }


    @NonNull
    @Override
    public String toString() {
        return "HdoModel{" +
                "id=" + id +
                ", rele='" + rele + '\'' +
                ", date='" + dateFrom + '\'' +
                ", date='" + dateUntil + '\'' +
                ", time='" + timeFrom + '\'' +
                ", time='" + timeUntil + '\'' +
                ", mon=" + mon +
                ", tue=" + tue +
                ", wed=" + wed +
                ", thu=" + thu +
                ", fri=" + fri +
                ", sat=" + sat +
                ", sun=" + sun +
                ", distributionArea='" + distributionArea + '\'' +
                '}';
    }


}
