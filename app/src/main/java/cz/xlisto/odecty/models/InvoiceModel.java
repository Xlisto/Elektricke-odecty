package cz.xlisto.odecty.models;

import androidx.annotation.NonNull;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.utils.DifferenceDate;


/**
 * Model jednoho záznamu pro fakturu
 * Xlisto 02.02.2023 3:40
 */
public class InvoiceModel {
    private static final String TAG = "InvoiceModel";

    private long id;
    private long dateFrom;
    private long dateTo;
    private double vtStart;
    private double vtEnd;
    private double ntStart;
    private double ntEnd;
    private long idPriceList;
    private long idInvoice;
    private final double otherServices;
    private final String numberInvoice;
    private final boolean isChangedElectricMeter;
    private TypePoze typePoze;
    private boolean isSelected;
    private long idMonthlyReading = -1L;


    public InvoiceModel(long id, long dateOf, long dateTo, double vtStart, double vtEnd, double ntStart, double ntEnd, long idInvoice, long idPriceList, double otherServices, String numberInvoice, boolean isChangedElectricMeter) {
        this(dateOf, dateTo, vtStart, vtEnd, ntStart, ntEnd, idInvoice, idPriceList, otherServices, numberInvoice, isChangedElectricMeter);
        this.id = id;
    }


    public InvoiceModel(long dateOf, long dateTo, double vtStart, double vtEnd, double ntStart, double ntEnd, long idInvoice, long idPriceList, double otherServices, String numberInvoice, boolean isChangedElectricMeter) {
        this.dateFrom = dateOf;
        this.dateTo = dateTo;
        this.vtStart = vtStart;
        this.vtEnd = vtEnd;
        this.ntStart = ntStart;
        this.ntEnd = ntEnd;
        this.idPriceList = idPriceList;
        this.idInvoice = idInvoice;
        this.otherServices = otherServices;
        this.numberInvoice = numberInvoice;
        this.isChangedElectricMeter = isChangedElectricMeter;
        isSelected = false;
    }


    public long getId() {
        return id;
    }


    public long getDateFrom() {
        return dateFrom;
    }


    public long getDateTo() {
        return dateTo;
    }


    public double getVtStart() {
        return vtStart;
    }


    public double getVtEnd() {
        return vtEnd;
    }


    public double getNtStart() {
        return ntStart;
    }


    public double getNtEnd() {
        return ntEnd;
    }


    public long getIdPriceList() {
        return idPriceList;
    }


    public long getIdInvoice() {
        return idInvoice;
    }


    public double getOtherServices() {
        return otherServices;
    }


    public String getNumberInvoice() {
        return numberInvoice;
    }


    public TypePoze getTypePoze() {
        return typePoze;
    }


    /**
     * Vrátí id měsíčního odečtu, který je nastaven jako "První odečet"
     *
     * @return
     */
    public long getIdMonthlyReading() {return idMonthlyReading;}


    public void setId(long id) {
        this.id = id;
    }


    public void setIdPriceList(long idPriceList) {
        this.idPriceList = idPriceList;
    }


    public void setIdInvoice(long idInvoice) {
        this.idInvoice = idInvoice;
    }


    /**
     * Spotřeba vysokého tarifu
     *
     * @return double spotřeba vysokého tarifu
     */
    public double getVt() {
        return vtEnd - vtStart;
    }


    /**
     * Spotřeba nízského tarifu
     *
     * @return double spotřeba nízského tarifu
     */
    public double getNt() {
        return ntEnd - ntStart;
    }


    /**
     * Spotřeba vysokého a nízského tarifu
     *
     * @return double spotřeba vysokého a nízského tarifu
     */
    public double getVtNt() {
        return getVt() + getNt();
    }


    /**
     * Rozdíl mezi datumy v měsících
     *
     * @return double rozdíl mezi datumy v měsících
     */
    public double getDifferentDate(DifferenceDate.TypeDate type) {
        String dateOf = ViewHelper.convertLongToDate(getDateFrom());
        String dateTo = ViewHelper.convertLongToDate(getDateTo());
        return Calculation.differentMonth(dateOf, dateTo, type);
    }


    /**
     * Vrátí, zda-li je vybraný záznam
     *
     * @return true pokud je vybraný záznam
     */
    public boolean isSelected() {
        return isSelected;
    }


    /**
     * Zda byl změněn elektroměr
     *
     * @return true pokud byl změněn elektroměr
     */
    public boolean isChangedElectricMeter() {
        return isChangedElectricMeter;
    }


    public void setDateFrom(long dateOf) {
        this.dateFrom = dateOf;
    }


    public void setDateTo(long dateTo) {
        this.dateTo = dateTo;
    }


    public void setVtStart(double vtStart) {
        this.vtStart = vtStart;
    }


    public void setVtEnd(double vtEnd) {
        this.vtEnd = vtEnd;
    }


    public void setNtStart(double ntStart) {
        this.ntStart = ntStart;
    }


    public void setNtEnd(double ntEnd) {
        this.ntEnd = ntEnd;
    }


    /**
     * Nastaví, zda-li je vybraný záznam
     *
     * @param selected true pokud je vybraný záznam
     */
    public void setSelected(boolean selected) {
        isSelected = selected;
    }


    /**
     * Nastaví id na měsíční odečet, který je nastaven jako "První odečet"
     *
     * @param idMonthlyReading id měsíčního odečtu
     */
    public void setIdMonthlyReading(long idMonthlyReading) {
        this.idMonthlyReading = idMonthlyReading;
    }


    @NonNull
    @Override
    public String toString() {
        return "InvoiceModel{" +
                "id=" + id +
                ", dateFrom=" + ViewHelper.convertLongToDate(dateFrom) +
                ", dateTo=" + ViewHelper.convertLongToDate(dateTo) +
                ", \nvtStart=" + vtStart +
                ", vtEnd=" + vtEnd +
                ", \nntStart=" + ntStart +
                ", ntEnd=" + ntEnd +
                ", \nidPriceList=" + idPriceList +
                ", idInvoice=" + idInvoice +
                ", otherServices=" + otherServices +
                ", numberInvoice='" + numberInvoice + '\'' +
                ", isChangedElectricMeter=" + isChangedElectricMeter +
                ", typePoze=" + typePoze +
                ", isSelected=" + isSelected +
                '}';
    }


    enum TypePoze {
        POZE1,
        POZE2
    }
}
