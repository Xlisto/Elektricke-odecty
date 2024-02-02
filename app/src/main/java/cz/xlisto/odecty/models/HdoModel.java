package cz.xlisto.odecty.models;

import java.util.Calendar;

import androidx.annotation.NonNull;


/**
 * Model pro časy HDO
 * Xlisto 26.05.2023 18:44
 */

public class HdoModel implements Cloneable {
    private static final String TAG = "HdoModel";
    private long id;
    private final String rele;
    private final String timeFrom;
    private final String timeUntil;
    private final String dateFrom;
    private final String dateUntil;
    private final String distributionArea;
    private Calendar calendarStart, calendarEnd;


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
        if (timeUntil.equals("23:59"))
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


    /**
     * Nastaví aktuální datum a čas začátku a konce HDO
     *
     * @param calendar aktuální datum a čas
     */
    public void setCalendar(Calendar calendar) {
        calendarStart = Calendar.getInstance();
        calendarEnd = Calendar.getInstance();
        calendarStart.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        calendarEnd.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        calendarStart.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeFrom.split(":")[0]));
        calendarStart.set(Calendar.MINUTE, Integer.parseInt(timeFrom.split(":")[1]));
        calendarEnd.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeUntil.split(":")[0]));
        calendarEnd.set(Calendar.MINUTE, Integer.parseInt(timeUntil.split(":")[1]));
        if (timeUntil.equals("00:00"))
            calendarEnd.add(Calendar.DAY_OF_MONTH, 1);

        calendarStart.set(Calendar.SECOND, 0);
        calendarStart.set(Calendar.MILLISECOND, 0);
        calendarEnd.set(Calendar.SECOND, 0);
        calendarEnd.set(Calendar.MILLISECOND, 0);

    }


    public Calendar getCalendarStart() {
        return calendarStart;
    }


    public Calendar getCalendarEnd() {
        return calendarEnd;
    }


    @NonNull
    @Override
    public HdoModel clone() {
        try {
            return (HdoModel) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw new AssertionError();
        }
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
