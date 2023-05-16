package cz.xlisto.odecty.modules.invoice;

import android.content.Context;
import android.util.Log;

import java.util.Objects;

import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.SubscriptionPoint;

/**
 * Xlisto 18.04.2023 21:06
 */
public class WithOutInvoiceService {
    private static final String TAG = "WithOutInvoiceService";

    /**
     * Rozdělí záznam ve faktuře (jen období bez faktury)
     *
     * @param context         kontext aplikace
     * @param invoiceOriginal původní záznam
     * @param invoiceNew      nový záznam
     */
    public static void cutInvoice(Context context, InvoiceModel invoiceOriginal, InvoiceModel invoiceNew) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        String tableTed = Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED();
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.insertInvoice(tableTed, invoiceNew);
        dataSubscriptionPointSource.updateInvoice(invoiceOriginal.getId(), tableTed, invoiceOriginal);
        dataSubscriptionPointSource.close();

    }

    /**
     * Detekce prvního (nejstaršího) záznamu v období bezfaktury a následná kontrola se zvoleným id faktury
     *
     * @param context kontext aplikace
     * @param idFak long id faktury
     * @param id long id záznamu
     */
    public static boolean firstRecordInvoice(Context context, long idFak, long id) {
        String table = getTable(context, idFak);
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();

        InvoiceModel firstInvoice = dataSubscriptionPointSource.firstInvoiceByDate(idFak, table);
        dataSubscriptionPointSource.close();

        return firstInvoice.getId() == id;
    }

    /**
     * Detekce posledního (nejnovějšího) záznamu v období bezfaktury a následná kontrola se zvoleným id faktury
     *
     * @param context kontext aplikace
     * @param idFak     long id faktury
     * @param id    long id záznamu
     * @return boolean true - poslední záznam, false - není poslední záznam
     */
    public static boolean lastRecordInvoice(Context context, long idFak, long id) {
        String table = getTable(context, idFak);
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();

        InvoiceModel lastInvoice = dataSubscriptionPointSource.lastInvoiceByDate(idFak, table);
        dataSubscriptionPointSource.close();

        return lastInvoice.getId() == id;
    }

    /**
     * Podle idFak detekuje a načte tabulku databáze
     *
     * @param context   kontext aplikace
     * @param idFak     long id faktury
     * @return String název tabulky
     */
    private static String getTable(Context context, long idFak) {
        if (idFak == -1L) {
            return Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED();
        } else {
            return Objects.requireNonNull(SubscriptionPoint.load(context)).getTableFAK();
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
        InvoiceModel invoice = dataSubscriptionPointSource.firstInvoiceByDate(-1L, Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
        InvoiceModel invoiceLast = dataSubscriptionPointSource.lastInvoiceByDateFromAll(Objects.requireNonNull(SubscriptionPoint.load(context)).getTableFAK());
        if (invoiceLast != null) {
            invoice.setDateFrom(invoiceLast.getDateTo() + (25 * 60 * 60 * 1000));
            invoice.setVtStart(invoiceLast.getVtEnd());
            invoice.setNtStart(invoiceLast.getNtEnd());
        }
        dataSubscriptionPointSource.updateInvoice(invoice.getId(), Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED(), invoice);
        dataSubscriptionPointSource.close();
    }
}
