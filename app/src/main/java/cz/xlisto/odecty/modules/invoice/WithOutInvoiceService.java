package cz.xlisto.odecty.modules.invoice;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.Objects;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataInvoiceSource;
import cz.xlisto.odecty.databaze.DataMonthlyReadingSource;
import cz.xlisto.odecty.dialogs.OwnAlertDialog;
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
     * Upraví poslední záznam (koncová data) v období bezfaktury podle posledního měsíčního odečtu. Pokud se jedná o první záznam, vloží nový záznam.
     *
     * @param context kontext aplikace
     */
    public static void editLastItemInInvoice(Context context, String table, MonthlyReadingModel monthlyReading) {
        Log.w(TAG, "editLastItemInInvoice: " + monthlyReading.getId());

        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel invoice = dataInvoiceSource.lastInvoiceByDate(-1L, table);
        invoice.setDateTo(monthlyReading.getDate());
        invoice.setVtEnd(monthlyReading.getVt());
        invoice.setNtEnd(monthlyReading.getNt());
        dataInvoiceSource.updateInvoice(invoice.getId(), table, invoice);
        dataInvoiceSource.close();

        if (invoice.getDateFrom() > monthlyReading.getDate())
            showAlertDialog(context);
    }


    /**
     * Vloží nový záznam v období bezfaktury podle měsíčního odečtu s nastaveným parametrem: Výměna elektroměru
     *
     * @param context        kontext aplikace
     * @param monthlyReading měsíční odečet
     */
    public static void insertNewItemInInvoice(Context context, String table, MonthlyReadingModel monthlyReading) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel invoice = new InvoiceModel(monthlyReading.getDate(), monthlyReading.getDate(),
                monthlyReading.getVt(), monthlyReading.getVt(), monthlyReading.getNt(), monthlyReading.getNt(),
                -1L, -1L, 0, "0", true);
        invoice.setIdMonthlyReading(monthlyReading.getId());

        dataInvoiceSource.insertInvoice(table, invoice);
        dataInvoiceSource.close();
    }


    /**
     * Upraví záznam v období bezfaktury podle měsíčního odečtu
     *
     * @param context kontext aplikace
     * @param table   název tabulky
     * @param monthlyReading faktura
     */
    public static void updateItemInvoice(Context context, String table, MonthlyReadingModel monthlyReading) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel invoice = dataInvoiceSource.loadInvoiceByMonthlyReading(monthlyReading.getId(), table);
        invoice.setDateFrom(monthlyReading.getDate());
        invoice.setVtStart(monthlyReading.getVt());
        invoice.setNtStart(monthlyReading.getNt());
        dataInvoiceSource.updateInvoice(invoice.getId(), table, invoice);
        dataInvoiceSource.close();
    }


    /**
     * Zkontroluje, zda-li existuje záznam v období bezfaktury podle id měsíčního odečtu (záznam o výměně elektroměru)
     *
     * @param context          kontext aplikace
     * @param table            název tabulky
     * @param idMonthlyReading id měsíčního odečtu
     * @return boolean true - záznam existuje, false - záznam neexistuje
     */
    public static boolean isExistItemInInvoice(Context context, String table, long idMonthlyReading) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        boolean exist = dataInvoiceSource.isInvoiceItemExistsByMonthlyReading(table, idMonthlyReading);
        dataInvoiceSource.close();
        return exist;
    }


    /**
     * Smaže záznam v období bezfaktury podle měsíčního odečtu
     *
     * @param context        kontext aplikace
     * @param table          název tabulky
     * @param monthlyReading měsíční odečet
     */
    public static void deleteItemInInvoiceByIdMonthlyReading(Context context, String table, MonthlyReadingModel monthlyReading) {
        deleteItemInInvoiceByIdMonthlyReading(context, table, monthlyReading.getId());
    }


    /**
     * Smaže záznam v období bezfaktury podle id měsíčního odečtu
     *
     * @param context kontext aplikace
     * @param table   název tabulky
     * @param id      id měsíčního odečeto
     */
    public static void deleteItemInInvoiceByIdMonthlyReading(Context context, String table, long id) {
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        dataInvoiceSource.deleteInvoiceByIdMonthlyReading(table, id);
        dataInvoiceSource.close();
    }


    /**
     * Upraví první záznam (počáteční data) v období bezfaktury podle posledního záznamu zapsané faktury
     *
     * @param context kontext aplikace
     */
    public static void editFirstItemInInvoice(Context context) {
        String tableO = Objects.requireNonNull(SubscriptionPoint.load(context)).getTableO();
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(context);
        dataInvoiceSource.open();
        dataMonthlyReadingSource.open();
        InvoiceModel itemFirstWithoutInvoice = dataInvoiceSource.firstInvoiceByDate(-1L, Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
        InvoiceModel itemLastInvoice = dataInvoiceSource.loadLastInvoiceByDateFromAll(Objects.requireNonNull(SubscriptionPoint.load(context)).getTableFAK());
        MonthlyReadingModel monthlyReading = dataMonthlyReadingSource.loadLastMonthlyReadingByDate(tableO);

        if (itemLastInvoice != null && monthlyReading != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(itemLastInvoice.getDateTo());
            calendar.add(Calendar.DATE, 1);
            itemFirstWithoutInvoice.setDateFrom(calendar.getTimeInMillis());
            itemFirstWithoutInvoice.setVtStart(itemLastInvoice.getVtEnd());
            itemFirstWithoutInvoice.setNtStart(itemLastInvoice.getNtEnd());
            if (calendar.getTimeInMillis() > monthlyReading.getDate()) {
                showAlertDialog(context);
            }
        }
        dataInvoiceSource.updateInvoice(itemFirstWithoutInvoice.getId(), Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED(), itemFirstWithoutInvoice);
        dataMonthlyReadingSource.close();
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
        //když upravuji záznam bezfaktury - tuto podmínku vyřazuji
        if (itemEditedInvoice.getIdInvoice() == -1L)
            return true;
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel itemFirstWithoutInvoice = dataInvoiceSource.firstInvoiceByDate(-1L, Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
        dataInvoiceSource.close();
        return itemEditedInvoice.getDateTo() < itemFirstWithoutInvoice.getDateTo();
    }


    private static void showAlertDialog(Context context) {
        OwnAlertDialog.show(context, context.getResources().getString(R.string.error),
                context.getResources().getString(R.string.dates_is_not_correct));
    }


}
