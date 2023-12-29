package cz.xlisto.odecty.models;

import java.util.ArrayList;

/**
 * Model seznamu součtu faktur
 * Xlisto 28.12.2023 10:52
 */
public class InvoiceListSumModel {
    private static final String TAG = "InvoiceListSumModel";
    private final ArrayList<String> names;
    private final ArrayList<double[]> maxValue;
    private final ArrayList<ArrayList<InvoiceSumModel>> invoiceSumModels;


    public InvoiceListSumModel() {
        this.names = new ArrayList<>();
        this.maxValue = new ArrayList<>();
        this.invoiceSumModels = new ArrayList<>();
    }


    public void addInvoiceSumModel(ArrayList<InvoiceSumModel> invoiceSumModel, String name, double[] maxValue) {
        invoiceSumModels.add(invoiceSumModel);
        names.add(name);
        this.maxValue.add(maxValue);
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
        return Math.max(maxValue.get(index)[0], maxValue.get(index)[1]);
    }


    /**
     * Vrátí maximální hodnotu celkem VT + NT
     *
     * @param index index v ArrayListu součtu maximálních hodnot
     * @return maximální hodnota celkem VT + NT
     */
    public double getMaxValueTotal(int index) {
        return maxValue.get(index)[0] + maxValue.get(index)[1];
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
}
