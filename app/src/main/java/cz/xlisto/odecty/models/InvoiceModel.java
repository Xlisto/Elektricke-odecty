package cz.xlisto.odecty.models;

import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Calculation;
import cz.xlisto.odecty.utils.DifferenceDate;

/**
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
    private double otherServices;
    private String numberInvoice;
    private TypePoze typePoze;

    public InvoiceModel(long id, long dateOf, long dateTo, double vtStart, double vtEnd, double ntStart, double ntEnd, long idInvoice, long idPriceList, double otherServices, String numberInvoice) {
        this(dateOf, dateTo, vtStart, vtEnd, ntStart, ntEnd, idInvoice, idPriceList, otherServices, numberInvoice);
        this.id = id;
    }

    public InvoiceModel(long dateOf, long dateTo, double vtStart, double vtEnd, double ntStart, double ntEnd, long idInvoice, long idPriceList, double otherServices, String numberInvoice) {
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

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Spotřeba vysokého tarifu
     * @return
     */
    public double getVt(){
        return vtEnd-vtStart;
    }

    /**
     * Spotřeba nízského tarifu
     * @return
     */
    public double getNt(){
        return ntEnd-ntStart;
    }

    /**
     * Spotřeba vyokého a nízského tarifu
     * @return
     */
    public double getVtNt(){
        return getVt()+getNt();
    }

    /**
     * Rozdíl mezi datumy v měsících
     * @return
     */
    public double getDifferentDate(DifferenceDate.TypeDate type){
        String dateOf = ViewHelper.convertLongToTime(getDateFrom());
        String dateTo = ViewHelper.convertLongToTime(getDateTo());
        return Calculation.differentMonth(dateOf, dateTo, type);
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

    enum TypePoze {
        POZE1,
        POZE2
    }
}
