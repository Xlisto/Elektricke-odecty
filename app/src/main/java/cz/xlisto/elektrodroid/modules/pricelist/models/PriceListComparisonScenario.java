package cz.xlisto.elektrodroid.modules.pricelist.models;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Konfigurace porovnávacího scénáře uchovávající zdroj spotřeby, vybrané ceníky, parametry připojení a simulace sazeb.
 */
public class PriceListComparisonScenario implements Serializable {

    public enum ConsumptionSource {
        MANUAL,
        HISTORICAL_YEAR,
        HISTORICAL_AVERAGE
    }


    public enum SimulationMode {
        PRESERVE_TOTAL_CONSUMPTION,
        SIMULATE_NT_SHARE
    }


    private long subscriptionPointId = -1L;
    private ConsumptionSource consumptionSource = ConsumptionSource.MANUAL;
    private int selectedYear = -1;
    private double vtKwh = 1000.0;
    private double ntKwh = 2000.0;
    private double months = 12.0;
    private double countPhaze = 3.0;
    private double powerBreaker = 25.0;
    private double servicesLeft = 0.0;
    private double servicesRight = 0.0;
    private double simulatedNtPercent = 60.0;
    private SimulationMode simulationMode = SimulationMode.PRESERVE_TOTAL_CONSUMPTION;

    private boolean isManuallyModified = false;
    private String dataQualityNote = "";

    private List<Long> selectedPriceListIds = new ArrayList<>();


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
        if (vtKwh + ntKwh > 0) {
            this.simulatedNtPercent = (ntKwh / (vtKwh + ntKwh)) * 100.0;
        }
    }


    public long getSubscriptionPointId() {
        return subscriptionPointId;
    }


    public void setSubscriptionPointId(long subscriptionPointId) {
        this.subscriptionPointId = subscriptionPointId;
    }


    public ConsumptionSource getConsumptionSource() {
        return consumptionSource;
    }


    public void setConsumptionSource(ConsumptionSource consumptionSource) {
        this.consumptionSource = consumptionSource;
    }


    public int getSelectedYear() {
        return selectedYear;
    }


    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
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


    public double getSimulatedNtPercent() {
        return simulatedNtPercent;
    }


    public void setSimulatedNtPercent(double simulatedNtPercent) {
        this.simulatedNtPercent = simulatedNtPercent;
    }


    public SimulationMode getSimulationMode() {
        return simulationMode;
    }


    public void setSimulationMode(SimulationMode simulationMode) {
        this.simulationMode = simulationMode;
    }


    public boolean isManuallyModified() {
        return isManuallyModified;
    }


    public void setManuallyModified(boolean manuallyModified) {
        isManuallyModified = manuallyModified;
    }


    public String getDataQualityNote() {
        return dataQualityNote;
    }


    public void setDataQualityNote(String dataQualityNote) {
        this.dataQualityNote = dataQualityNote;
    }


    public List<Long> getSelectedPriceListIds() {
        return selectedPriceListIds;
    }


    public void setSelectedPriceListIds(List<Long> selectedPriceListIds) {
        this.selectedPriceListIds = selectedPriceListIds != null ? selectedPriceListIds : new ArrayList<>();
    }

}
