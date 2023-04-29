package cz.xlisto.cenik.modules.invoice;

import android.content.Context;
import android.util.Log;

import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.models.InvoiceModel;
import cz.xlisto.cenik.models.MonthlyReadingModel;
import cz.xlisto.cenik.ownview.ViewHelper;
import cz.xlisto.cenik.utils.SubscriptionPoint;

/**
 * Xlisto 18.04.2023 21:06
 */
public class WithOutInvoiceService {
    private static final String TAG = "WithOutInvoiceService";

    /**
     * Rozdělí záznam ve faktuře (jen období bez faktury)
     *
     * @param context
     * @param invoiceOriginal
     * @param invoiceNew
     */
    public static void cutInvoice(Context context, InvoiceModel invoiceOriginal, InvoiceModel invoiceNew) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        String tableTed = SubscriptionPoint.load(context).getTableTED();
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.insertInvoice(tableTed, invoiceNew);
        dataSubscriptionPointSource.updateInvoice(invoiceOriginal.getId(), tableTed, invoiceOriginal);
        dataSubscriptionPointSource.close();

    }

    /**
     * Detekce prvního (nejstaršího) záznamu v období bezfaktury a následná kontrola se zvoleným id faktury
     *
     * @param context
     * @param idFak
     * @param id
     */
    public static boolean firstRecordInvoice(Context context, long idFak, long id) {
        String table = getTable(context, idFak);
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();

        InvoiceModel firstInvoice = dataSubscriptionPointSource.firstInvoiceByDate(idFak, table);
        dataSubscriptionPointSource.close();

        if (firstInvoice.getId() == id) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Detekce posledního (nejnovějšího) záznamu v období bezfaktury a následná kontrola se zvoleným id faktury
     * @param context
     * @param idFak
     * @param id
     * @return
     */
    public static boolean lastRecordInvoice(Context context,long idFak, long id){
        String table = getTable(context, idFak);
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();

        InvoiceModel lastInvoice = dataSubscriptionPointSource.lastInvoiceByDate(idFak, table);
        dataSubscriptionPointSource.close();

        if (lastInvoice.getId() == id) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Podle idFak detekuje a načte tabulku databáze
     *
     * @param context
     * @param idFak
     * @return
     */
    private static String getTable(Context context, long idFak) {
        if (idFak == -1L) {
            return SubscriptionPoint.load(context).getTableTED();
        } else {
            return SubscriptionPoint.load(context).getTableFAK();
        }
    }

    /**
     * Upraví poslední záznam (koncová data) v období bezfaktury podle měsíčního odečtu
     */
    public static void editLastItemInInvoice(Context context, String table, MonthlyReadingModel monthlyReading) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        InvoiceModel invoice = dataSubscriptionPointSource.lastInvoiceByDate(-1L, table);
        invoice.setDateTo(monthlyReading.getDate());
        invoice.setVtEnd(monthlyReading.getVt());
        invoice.setNtEnd(monthlyReading.getNt());
        dataSubscriptionPointSource.updateInvoice(invoice.getId(), table, invoice);
        dataSubscriptionPointSource.close();
    }

    /**
     * Upraví první záznam (počáteční data) v období bezfaktury podle posledního záznamu zapsané faktury
     */
    public static void editFirstItemInInvoice(Context context) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        InvoiceModel invoiceLast = dataSubscriptionPointSource.lastInvoiceByDateFromAll(SubscriptionPoint.load(context).getTableFAK());
        Log.w(TAG, "poslední faktura: " + invoiceLast.getId() + " " + invoiceLast.getDateFrom() + " " + ViewHelper.convertLongToTime(invoiceLast.getDateTo()) + " " + invoiceLast.getVtStart() + " " + invoiceLast.getVtEnd() + " " + invoiceLast.getNtStart() + " " + invoiceLast.getNtEnd());
        InvoiceModel invoice = dataSubscriptionPointSource.firstInvoiceByDate(-1L, SubscriptionPoint.load(context).getTableTED());
        Log.w(TAG, "TED faktura: " + invoice.getId() + " " + ViewHelper.convertLongToTime(invoice.getDateFrom()) + " " + ViewHelper.convertLongToTime(invoice.getDateTo()) + " " + invoice.getVtStart() + " " + invoice.getVtEnd() + " " + invoice.getNtStart() + " " + invoice.getNtEnd());
        invoice.setDateFrom(invoiceLast.getDateTo() + (25 * 60 * 60 * 1000));
        invoice.setVtStart(invoiceLast.getVtEnd());
        invoice.setNtStart(invoiceLast.getNtEnd());
        dataSubscriptionPointSource.updateInvoice(invoice.getId(), SubscriptionPoint.load(context).getTableTED(), invoice);
        dataSubscriptionPointSource.close();
    }
}
