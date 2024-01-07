package cz.xlisto.odecty.models;

import java.util.ArrayList;

/**
 * Model seznamu součtu faktur
 * Xlisto 28.12.2023 10:52
 */
public class InvoiceListSumModel {
    private static final String TAG = "InvoiceListSumModel";
    private final ArrayList<String> names;
    private final ArrayList<double[]> maxValues;
    private final ArrayList<ArrayList<InvoiceSumModel>> invoiceSumModels;
    private final ArrayList<ArrayList<HdoModel>> hdoModels;
    private final ArrayList<Double> invoiceSumPayments, invoiceSumTotalPrices;
    private final ArrayList<Long> hdoTimeShifts;


    public InvoiceListSumModel() {
        this.invoiceSumPayments = new ArrayList<>();
        this.names = new ArrayList<>();
        this.maxValues = new ArrayList<>();
        this.invoiceSumModels = new ArrayList<>();
        this.invoiceSumTotalPrices = new ArrayList<>();
        this.hdoModels = new ArrayList<>();
        this.hdoTimeShifts = new ArrayList<>();
    }


    public void addInvoiceSumModel(ArrayList<InvoiceSumModel> invoiceSumModel, ArrayList<HdoModel> hdoModels, long timeShift, String name, double[] maxValue, double sumPayment, double sumTotalPrice) {
        invoiceSumModels.add(invoiceSumModel);
        this.hdoModels.add(hdoModels);
        names.add(name);
        maxValues.add(maxValue);
        invoiceSumPayments.add(sumPayment);
        invoiceSumTotalPrices.add(sumTotalPrice);
        hdoTimeShifts.add(timeShift);
    }


    /**
     * Vrátí název odběrného místa
     *
     * @param index index v ArrayListu odběrných míst
     * @return název odběrného místa
     */
    public String getName(int index) {
        return names.get(index);
    }


    /**
     * Vrátí maximální hodnotu
     *
     * @param index index v ArrayListu maximálních hodnot
     * @return maximální hodnota
     */
    public double getMaxValue(int index) {
        return Math.max(maxValues.get(index)[0], maxValues.get(index)[1]);
    }


    /**
     * Vrátí maximální hodnotu celkem VT + NT
     *
     * @param index index v ArrayListu součtu maximálních hodnot
     * @return maximální hodnota celkem VT + NT
     */
    public double getMaxValueTotal(int index) {
        return maxValues.get(index)[0] + maxValues.get(index)[1];
    }


    /**
     * Vrátí počet odběrných míst
     *
     * @return počet odběrných míst
     */
    public int size() {
        return invoiceSumModels.size();
    }


    /**
     * Vrátí seznam souhrnů faktur
     *
     * @param index index v ArrayListu souhrnů faktur
     * @return seznam souhrnů faktur
     */
    public ArrayList<InvoiceSumModel> getInvoiceSumModels(int index) {
        return invoiceSumModels.get(index);
    }


    /**
     * Vrátí seznam HdoModelů
     *
     * @param index index v ArrayListu HdoModelů
     * @return seznam HdoModelů
     */
    public ArrayList<HdoModel> getHdoModels(int index) {
        return hdoModels.get(index);
    }


    /**
     * Vrátí součet plateb
     *
     * @param index index v ArrayListu součtů plateb
     * @return součet plateb
     */
    public double getInvoiceSumPayments(int index) {
        return invoiceSumPayments.get(index);
    }


    /**
     * Vrátí časový posun hodin v elektroměru
     *
     * @param index index v ArrayListu časových posunů
     * @return long časový posun hodin v elektroměru
     */
    public long getHdoTimeShift(int index) {
        return hdoTimeShifts.get(index);
    }


    /**
     * Vrátí součet celkové ceny
     *
     * @param index index v ArrayListu součtů celkové ceny
     * @return součet celkové ceny
     */
    public double getInvoiceSumTotalPrices(int index) {
        return invoiceSumTotalPrices.get(index);
    }
}
