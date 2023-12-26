package cz.xlisto.odecty.modules.invoice;

import android.content.Context;

import java.util.Calendar;
import java.util.Objects;

import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.MonthlyReadingModel;
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
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        String tableTed = Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED();
        dataInvoiceSource.open();
        dataInvoiceSource.insertInvoice(tableTed, invoiceNew);
        dataInvoiceSource.updateInvoice(invoiceOriginal.getId(), tableTed, invoiceOriginal);
        dataInvoiceSource.close();

    }


    /**
     * Detekce prvního (nejstaršího) záznamu v období bezfaktury a následná kontrola se zvoleným id faktury
     *
     * @param context kontext aplikace
     * @param idFak   long id faktury
     * @param id      long id záznamu
     */
    public static boolean firstRecordInvoice(Context context, long idFak, long id) {
        String table = getTable(context, idFak);
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();

        InvoiceModel firstInvoice = dataInvoiceSource.firstInvoiceByDate(idFak, table);
        dataInvoiceSource.close();

        return firstInvoice.getId() == id;
    }


    /**
     * Detekce posledního (nejnovějšího) záznamu v období bezfaktury a následná kontrola se zvoleným id faktury
     *
     * @param context kontext aplikace
     * @param idFak   long id faktury
     * @param id      long id záznamu
     * @return boolean true - poslední záznam, false - není poslední záznam
     */
    public static boolean lastRecordInvoice(Context context, long idFak, long id) {
        String table = getTable(context, idFak);
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel lastInvoice = dataInvoiceSource.lastInvoiceByDate(idFak, table);
        dataInvoiceSource.close();

        return lastInvoice.getId() == id;
    }


    /**
     * Podle idFak detekuje a načte tabulku databáze
     *
     * @param context kontext aplikace
     * @param idFak   long id faktury
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
     *
     * @param context kontext aplikace
     */
    public static void editLastItemInInvoice(Context context, String table, MonthlyReadingModel monthlyReading) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel invoice = dataInvoiceSource.lastInvoiceByDate(-1L, table);
        invoice.setDateTo(monthlyReading.getDate());
        invoice.setVtEnd(monthlyReading.getVt());
        invoice.setNtEnd(monthlyReading.getNt());
        dataInvoiceSource.updateInvoice(invoice.getId(), table, invoice);
        dataInvoiceSource.close();
    }


    /**
     * Upraví první záznam (počáteční data) v období bezfaktury podle posledního záznamu zapsané faktury
     *
     * @param context kontext aplikace
     */
    public static void editFirstItemInInvoice(Context context) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel itemFirstWithoutInvoice = dataInvoiceSource.firstInvoiceByDate(-1L, Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
        InvoiceModel itemLastInvoice = dataInvoiceSource.lastInvoiceByDateFromAll(Objects.requireNonNull(SubscriptionPoint.load(context)).getTableFAK());
        if (itemLastInvoice != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(itemLastInvoice.getDateTo());
            calendar.add(Calendar.DATE,1);
            itemFirstWithoutInvoice.setDateFrom(calendar.getTimeInMillis());
            itemFirstWithoutInvoice.setVtStart(itemLastInvoice.getVtEnd());
            itemFirstWithoutInvoice.setNtStart(itemLastInvoice.getNtEnd());
        }
        dataInvoiceSource.updateInvoice(itemFirstWithoutInvoice.getId(), Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED(), itemFirstWithoutInvoice);
        dataInvoiceSource.close();
    }


    /**
     * Zkontroluje datum prvního záznamu (koncová data) v období bezfaktury podle, aby nepřesáhly koncový datum prvního záznamu
     *
     * @param context           kontext aplikace
     * @param itemEditedInvoice upravovaný záznam
     * @return boolean true - datum je v pořádku (menší), false - datum je chybné (větší)
     */
    public static boolean checkDateFirstItemInvoice(Context context, InvoiceModel itemEditedInvoice) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel itemFirstWithoutInvoice = dataInvoiceSource.firstInvoiceByDate(-1L, Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
        dataInvoiceSource.close();
        return itemEditedInvoice.getDateTo() < itemFirstWithoutInvoice.getDateTo();
    }
}
