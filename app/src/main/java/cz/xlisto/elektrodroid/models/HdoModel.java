package cz.xlisto.elektrodroid.models;


import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;


/**
 * Model pro časy HDO
 * Xlisto 26.05.2023 18:44
 */

public class HdoModel implements Cloneable {
    private static final String TAG = "HdoModel";
    private long id;
    private final String rele;
    private String timeFrom;
    private String timeUntil;
    private final String dateFrom;
    private final String dateUntil;
    private final String distributionArea;
    private Calendar calendarStart;
    private Calendar calendarEnd;


    private int mon, tue, wed, thu, fri, sat, sun;


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
        if (distributionArea != null)
            setDayOfWeekPRE();
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
        if (timeUntil.equals("23:59") || timeUntil.equals("24:00"))
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
    public void setCalendar(Calendar calendar, long timeShift) {
        calendarStart = Calendar.getInstance();
        calendarEnd = Calendar.getInstance();
        Calendar calendarCurrent = Calendar.getInstance();
        calendarCurrent.setTimeInMillis(calendar.getTimeInMillis() + timeShift);
        calendarStart.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        calendarEnd.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        calendarStart.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeFrom.split(":")[0]));
        calendarStart.set(Calendar.MINUTE, Integer.parseInt(timeFrom.split(":")[1]));
        calendarEnd.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeUntil.split(":")[0]));
        calendarEnd.set(Calendar.MINUTE, Integer.parseInt(timeUntil.split(":")[1]));
        //nastavení datumu u hodin 00:00
        if (timeUntil.equals("00:00"))
            calendarEnd.add(Calendar.DAY_OF_MONTH, 1);

        //pokud je HDO přes půlnoc
        if (calendarStart.getTimeInMillis() > calendarEnd.getTimeInMillis())
            calendarEnd.add(Calendar.DAY_OF_MONTH, 1);

        calendarStart.set(Calendar.SECOND, 0);
        calendarStart.set(Calendar.MILLISECOND, 0);
        calendarEnd.set(Calendar.SECOND, 0);
        calendarEnd.set(Calendar.MILLISECOND, 0);
    }


    /**
     * Nastaví datum a čas začátku HDO
     *
     * @param time datum a čas v milisekundách
     */
    public void setCalendarStart(long time) {
        calendarStart.setTimeInMillis(time);
        timeFrom = calendarStart.get(Calendar.HOUR_OF_DAY) + ":" + calendarStart.get(Calendar.MINUTE);
    }


    /**
     * Nastaví datum a čas konce HDO
     *
     * @param time datum a čas v milisekundách
     */
    public void setCalendarEnd(long time) {
        calendarEnd.setTimeInMillis(time);
        timeUntil = calendarEnd.get(Calendar.HOUR_OF_DAY) + ":" + calendarEnd.get(Calendar.MINUTE);
    }


    /**
     * Vrátí datum a čas začátku HDO
     *
     * @return datum a čas začátku HDO
     */
    public Calendar getCalendarStart() {
        return calendarStart;
    }


    /**
     * Vrátí datum a čas konce HDO
     *
     * @return datum a čas konce HDO
     */
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


    /**
     * Vrátí datum a čas začátku a konce HDO
     *
     * @return datum a čas začátku a konce HDO
     */
    public String getDates() {
        if (calendarStart == null || calendarEnd == null)
            return "";
        int dayStart = calendarStart.get(Calendar.DAY_OF_MONTH);
        int monthStart = calendarStart.get(Calendar.MONTH) + 1;
        int yearStart = calendarStart.get(Calendar.YEAR);
        int hourStart = calendarStart.get(Calendar.HOUR_OF_DAY);
        int minuteStart = calendarStart.get(Calendar.MINUTE);
        int dayEnd = calendarEnd.get(Calendar.DAY_OF_MONTH);
        int monthEnd = calendarEnd.get(Calendar.MONTH) + 1;
        int yearEnd = calendarEnd.get(Calendar.YEAR);
        int hourEnd = calendarEnd.get(Calendar.HOUR_OF_DAY);
        int minuteEnd = calendarEnd.get(Calendar.MINUTE);
        String formatString = "%02d.%02d.%d %02d:%02d - %02d.%02d.%d %02d:%02d";
        return String.format(Locale.getDefault(), formatString, dayStart, monthStart, yearStart, hourStart, minuteStart, dayEnd, monthEnd, yearEnd, hourEnd, minuteEnd);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HdoModel hdoModel = (HdoModel) o;
        return calendarStart.getTimeInMillis() == hdoModel.calendarStart.getTimeInMillis() &&
                calendarEnd.getTimeInMillis() == hdoModel.calendarEnd.getTimeInMillis() &&
                rele.equals(hdoModel.rele);
    }


    @Override
    public int hashCode() {
        return Objects.hash(calendarStart.getTimeInMillis(), calendarEnd.getTimeInMillis(), rele);
    }


    private void setDayOfWeekPRE() {
        if (distributionArea.equals("PRE")) {
            int dayOfMonth = Integer.parseInt(dateFrom.split("\\.")[0]);
            int month = Integer.parseInt(dateFrom.split("\\.")[1]);
            int year = Integer.parseInt(dateFrom.split("\\.")[2]);
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month - 1, dayOfMonth);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

            switch (dayOfWeek) {
                case Calendar.MONDAY:
                    mon = 1;
                    break;
                case Calendar.TUESDAY:
                    tue = 1;
                    break;
                case Calendar.WEDNESDAY:
                    wed = 1;
                    break;
                case Calendar.THURSDAY:
                    thu = 1;
                    break;
                case Calendar.FRIDAY:
                    fri = 1;
                    break;
                case Calendar.SATURDAY:
                    sat = 1;
                    break;
                case Calendar.SUNDAY:
                    sun = 1;
                    break;
            }
        }
    }
}
