package cz.xlisto.odecty.models;

import java.util.Calendar;

/**
 * Model měsíčního odečtu
 */
public class MonthlyReadingModel {
    private long id;
    private long date;
    private double vt;
    private double nt;
    private double payment;
    private String description;
    private double otherServices;
    private boolean first;
    private long priceListId;

    public MonthlyReadingModel(long id, long date, double vt, double nt, double payment, String description, double otherServices, long priceListId, boolean first) {
        this(date, vt, nt, payment, description, otherServices, priceListId, first);
        this.id = id;
    }

    public MonthlyReadingModel(long date, double vt, double nt, double payment, String description, double otherServices, long priceListId, boolean first) {
        this.date = date;
        this.vt = vt;
        this.nt = nt;
        this.payment = payment;
        this.description = description;
        this.otherServices = otherServices;
        this.priceListId = priceListId;
        this.first = first;
    }

    public double getPayment() {
        return payment;
    }

    public void setPayment(double payment) {
        this.payment = payment;
    }

    public long getId() {
        return id;
    }

    public long getPriceListId() {
        return priceListId;
    }

    public long getDate() {
        return date;
    }

    public double getVt() {
        return vt;
    }

    public void setVt(double vt) {
        this.vt = vt;
    }

    public double getNt() {
        return nt;
    }

    public boolean isFirst() {
        return first;
    }


    public void setNt(double nt) {
        this.nt = nt;
    }

    public String getDescription() {
        return description;
    }

    public void set_id(long _id) {
        this.id = _id;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getOtherServices() {
        return otherServices;
    }

    public void setOtherServices(double otherServices) {
        this.otherServices = otherServices;
    }

    public void setPriceListId(long priceListId) {
        this.priceListId = priceListId;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    /**
     * Výpočet slevy na DPH za měsíce listopad a prosinec v roce 2021
     */
    //TODO: sleva na DPH za měsíce listopad a prosinec v roce 2021 v měsíčních odečtech
    public double getDifferenceDPH() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        if(year == 2021 && month >=10) {
            //return (sum / 121 * 21);
            return (payment * 0.21);
        }
        return 0.0;
    }
}
