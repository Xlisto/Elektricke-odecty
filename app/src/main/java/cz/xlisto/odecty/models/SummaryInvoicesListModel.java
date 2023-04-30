package cz.xlisto.odecty.models;

import java.util.ArrayList;

/**
 * Xlisto 21.03.2023 20:57
 */
public class SummaryInvoicesListModel {
    private static final String TAG = "SummaryInvoicesListModel";
    private String title;
    private ArrayList<SummaryInvoiceModel> summaryInvoices = new ArrayList<>();

    public SummaryInvoicesListModel(String title) {
        this.title = title;
    }


}
