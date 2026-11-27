package cz.xlisto.elektrodroid.modules.pricelist.models;


import java.io.Serializable;


/**
 * Konfigurace porovnávacího scénáře uchovávající spotřebu, délku období, parametry připojení a ceny služeb.
 */
public class PriceListComparisonScenario implements Serializable {

    private double vtKwh = 1000.0;
    private double ntKwh = 2000.0;
    private double months = 12.0;
    private double countPhaze = 3.0;
    private double powerBreaker = 25.0;
    private double servicesLeft = 0.0;
    private double servicesRight = 0.0;


    public PriceListComparisonScenario() {
    }


    public PriceListComparisonScenario(double vtKwh, double ntKwh, double months, double countPhaze, double powerBreaker, double servicesLeft, double servicesRight) {
        this.vtKwh = vtKwh;
        this.ntKwh = ntKwh;
        this.months = months;
        this.countPhaze = countPhaze;
        this.powerBreaker = powerBreaker;
        this.servicesLeft = servicesLeft;
        this.servicesRight = servicesRight;
    }


    public double getVtKwh() {
        return vtKwh;
    }


    public void setVtKwh(double vtKwh) {
        this.vtKwh = vtKwh;
    }


    public double getNtKwh() {
        return ntKwh;
    }


    public void setNtKwh(double ntKwh) {
        this.ntKwh = ntKwh;
    }


    public double getTotalKwh() {
        return vtKwh + ntKwh;
    }


    public double getNtRatio() {
        double total = getTotalKwh();
        return total > 0 ? (ntKwh / total) : 0.0;
    }


    public double getMonths() {
        return months;
    }


    public void setMonths(double months) {
        this.months = months;
    }


    public double getCountPhaze() {
        return countPhaze;
    }


    public void setCountPhaze(double countPhaze) {
        this.countPhaze = countPhaze;
    }


    public double getPowerBreaker() {
        return powerBreaker;
    }


    public void setPowerBreaker(double powerBreaker) {
        this.powerBreaker = powerBreaker;
    }


    public double getServicesLeft() {
        return servicesLeft;
    }


    public void setServicesLeft(double servicesLeft) {
        this.servicesLeft = servicesLeft;
    }


    public double getServicesRight() {
        return servicesRight;
    }


    public void setServicesRight(double servicesRight) {
        this.servicesRight = servicesRight;
    }

}
