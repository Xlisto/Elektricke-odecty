package cz.xlisto.elektrodroid.models;


import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Objects;


/**
 * Třída HdoModel představuje model pro časy HDO (Hromadné dálkové ovládání).
 * <p>
 * Tato třída implementuje rozhraní Cloneable, což umožňuje vytváření kopií objektů této třídy.
 * <p>
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
    private Calendar calendarStart;
    private Calendar calendarEnd;

    private int mon, tue, wed, thu, fri, sat, sun, sv;
    private int notifyStart;
    private int notifyEnd;


    /**
     * Konstruktor třídy HdoModel.
     *
     * @param id               Identifikátor HDO modelu.
     * @param rele             Rele HDO.
     * @param dateFrom         Datum začátku platnosti HDO.
     * @param dateUntil        Datum konce platnosti HDO.
     * @param timeFrom         Čas začátku platnosti HDO.
     * @param timeUntil        Čas konce platnosti HDO.
     * @param mon              Indikátor platnosti HDO v pondělí (1 = platí, 0 = neplatí).
     * @param tue              Indikátor platnosti HDO v úterý (1 = platí, 0 = neplatí).
     * @param wed              Indikátor platnosti HDO ve středu (1 = platí, 0 = neplatí).
     * @param thu              Indikátor platnosti HDO ve čtvrtek (1 = platí, 0 = neplatí).
     * @param fri              Indikátor platnosti HDO v pátek (1 = platí, 0 = neplatí).
     * @param sat              Indikátor platnosti HDO v sobotu (1 = platí, 0 = neplatí).
     * @param sun              Indikátor platnosti HDO v neděli (1 = platí, 0 = neplatí).
     * @param distributionArea Distribuční oblast HDO.
     */
    public HdoModel(long id, String rele, String dateFrom, String dateUntil, String timeFrom, String timeUntil, int mon, int tue, int wed, int thu, int fri, int sat, int sun, String distributionArea) {
        this(id, rele, dateFrom, dateUntil, timeFrom, timeUntil, mon, tue, wed, thu, fri, sat, sun, distributionArea, 0, 0);
    }


    /**
     * Konstruktor třídy HdoModel.
     */
    public HdoModel(long id, String rele, String dateFrom, String dateUntil, String timeFrom, String timeUntil,
                    int mon, int tue, int wed, int thu, int fri, int sat, int sun,
                    String distributionArea, int notifyStart, int notifyEnd) {
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
        this.notifyStart = notifyStart;
        this.notifyEnd = notifyEnd;
        if (distributionArea != null)
            setDayOfWeekPRE();
    }


    /**
     * Konstruktor třídy HdoModel.
     *
     * @param rele             Rele HDO.
     * @param dateFrom         Datum začátku platnosti HDO.
     * @param dateUntil        Datum konce platnosti HDO.
     * @param timeFrom         Čas začátku platnosti HDO.
     * @param timeUntil        Čas konce platnosti HDO.
     * @param mon              Indikátor platnosti HDO v pondělí (1 = platí, 0 = neplatí).
     * @param tue              Indikátor platnosti HDO v úterý (1 = platí, 0 = neplatí).
     * @param wed              Indikátor platnosti HDO ve středu (1 = platí, 0 = neplatí).
     * @param thu              Indikátor platnosti HDO ve čtvrtek (1 = platí, 0 = neplatí).
     * @param fri              Indikátor platnosti HDO v pátek (1 = platí, 0 = neplatí).
     * @param sat              Indikátor platnosti HDO v sobotu (1 = platí, 0 = neplatí).
     * @param sun              Indikátor platnosti HDO v neděli (1 = platí, 0 = neplatí).
     * @param distributionArea Distribuční oblast HDO.
     */
    public HdoModel(String rele, String dateFrom, String dateUntil, String timeFrom, String timeUntil, int mon, int tue, int wed, int thu, int fri, int sat, int sun, String distributionArea) {
        this(-1, rele, dateFrom, dateUntil, timeFrom, timeUntil, mon, tue, wed, thu, fri, sat, sun, distributionArea, 0, 0);
    }


    /**
     * Nastaví identifikátor HDO modelu.
     *
     * @param id Identifikátor HDO modelu.
     */
    public void setId(long id) {
        this.id = id;
    }


    /**
     * Vrátí identifikátor HDO modelu.
     *
     * @return Identifikátor HDO modelu.
     */
    public long getId() {
        return id;
    }


    /**
     * Vrátí rele HDO.
     *
     * @return Rele HDO.
     */
    public String getRele() {
        return rele;
    }


    /**
     * Vrátí datum začátku platnosti HDO.
     *
     * @return Datum začátku platnosti HDO.
     */
    public String getDateFrom() {
        return dateFrom;
    }


    /**
     * Vrátí datum konce platnosti HDO.
     *
     * @return Datum konce platnosti HDO.
     */
    public String getDateUntil() {
        return dateUntil;
    }


    /**
     * Vrátí čas začátku platnosti HDO.
     *
     * @return Čas začátku platnosti HDO.
     */
    public String getTimeFrom() {
        return timeFrom;
    }


    /**
     * Vrátí čas konce platnosti HDO.
     *
     * @return Čas konce platnosti HDO.
     */
    public String getTimeUntil() {
        if (timeUntil.equals("23:59") || timeUntil.equals("24:00"))
            return "0:00";
        return timeUntil;
    }


    /**
     * Vrátí indikátor platnosti HDO v pondělí.
     *
     * @return Indikátor platnosti HDO v pondělí.
     */
    public int getMon() {
        return mon;
    }


    /**
     * Vrátí indikátor platnosti HDO v úterý.
     *
     * @return Indikátor platnosti HDO v úterý.
     */
    public int getTue() {
        return tue;
    }


    /**
     * Vrátí indikátor platnosti HDO ve středu.
     *
     * @return Indikátor platnosti HDO ve středu.
     */
    public int getWed() {
        return wed;
    }


    /**
     * Vrátí indikátor platnosti HDO ve čtvrtek.
     *
     * @return Indikátor platnosti HDO ve čtvrtek.
     */
    public int getThu() {
        return thu;
    }


    /**
     * Vrátí indikátor platnosti HDO v pátek.
     *
     * @return Indikátor platnosti HDO v pátek.
     */
    public int getFri() {
        return fri;
    }


    /**
     * Vrátí indikátor platnosti HDO v sobotu.
     *
     * @return Indikátor platnosti HDO v sobotu.
     */
    public int getSat() {
        return sat;
    }


    /**
     * Vrátí indikátor platnosti HDO v neděli.
     *
     * @return Indikátor platnosti HDO v neděli.
     */
    public int getSun() {
        return sun;
    }


    /**
     * Vrátí distribuční oblast HDO.
     *
     * @return Distribuční oblast HDO.
     */
    public String getDistributionArea() {
        if (distributionArea == null)
            return "";
        return distributionArea;
    }


    public int getNotifyStart() {
        return notifyStart;
    }


    public void setNotifyStart(int notifyStart) {
        this.notifyStart = notifyStart;
    }


    public int getNotifyEnd() {
        return notifyEnd;
    }


    public void setNotifyEnd(int notifyEnd) {
        this.notifyEnd = notifyEnd;
    }


    public int getSv() {
        return sv;
    }


    public void setSv(int sv) {
        this.sv = sv;
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


    /**
     * Vytvoří a vrátí kopii objektu HdoModel.
     *
     * @return Kopie objektu HdoModel.
     */
    @NonNull
    @Override
    public HdoModel clone() {
        try {
            return (HdoModel) super.clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "clone: ", e);
            throw new AssertionError();
        }
    }


    /**
     * Vrátí textovou reprezentaci objektu HdoModel.
     *
     * @return Textová reprezentace objektu HdoModel.
     */
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
                ", notifyStart=" + notifyStart +
                ", notifyEnd=" + notifyEnd +
                ", distributionArea='" + distributionArea + '\'' +
                '}';
    }


    /**
     * Porovná aktuální objekt HdoModel s jiným objektem.
     *
     * @param o Objekt, se kterým se má porovnat.
     * @return true, pokud jsou objekty stejné, jinak false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HdoModel hdoModel = (HdoModel) o;

        if (id > 0 && hdoModel.id > 0 && id == hdoModel.id) {
            return true;
        }

        long start1 = calendarStart != null ? calendarStart.getTimeInMillis() : 0L;
        long start2 = hdoModel.calendarStart != null ? hdoModel.calendarStart.getTimeInMillis() : 0L;
        long end1 = calendarEnd != null ? calendarEnd.getTimeInMillis() : 0L;
        long end2 = hdoModel.calendarEnd != null ? hdoModel.calendarEnd.getTimeInMillis() : 0L;

        return id == hdoModel.id &&
                mon == hdoModel.mon && tue == hdoModel.tue && wed == hdoModel.wed &&
                thu == hdoModel.thu && fri == hdoModel.fri && sat == hdoModel.sat &&
                sun == hdoModel.sun && sv == hdoModel.sv &&
                start1 == start2 && end1 == end2 &&
                Objects.equals(rele, hdoModel.rele) &&
                Objects.equals(timeFrom, hdoModel.timeFrom) &&
                Objects.equals(timeUntil, hdoModel.timeUntil) &&
                Objects.equals(dateFrom, hdoModel.dateFrom) &&
                Objects.equals(dateUntil, hdoModel.dateUntil) &&
                Objects.equals(distributionArea, hdoModel.distributionArea);
    }


    /**
     * Vrátí hash kód objektu HdoModel.
     *
     * @return Hash kód objektu HdoModel.
     */
    @Override
    public int hashCode() {
        long start = calendarStart != null ? calendarStart.getTimeInMillis() : 0L;
        long end = calendarEnd != null ? calendarEnd.getTimeInMillis() : 0L;
        return Objects.hash(id, rele, dateFrom, dateUntil, timeFrom, timeUntil, mon, tue, wed, thu, fri, sat, sun, sv, distributionArea, start, end);
    }


    /**
     * Nastaví dny v týdnu pro distribuční oblast PRE.
     */
    private void setDayOfWeekPRE() {
        if (distributionArea != null && distributionArea.equals("PRE") && mon == 0 && tue == 0 && wed == 0 && thu == 0 && fri == 0 && sat == 0 && sun == 0) {
            if (dateFrom == null || dateFrom.isEmpty() || !dateFrom.contains(".")) return;
            try {
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
            } catch (Exception ignored) {
            }
        }
    }

}
