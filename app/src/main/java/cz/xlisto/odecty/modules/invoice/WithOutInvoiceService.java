package cz.xlisto.odecty.modules.invoice;

import android.content.Context;

import java.util.ArrayList;
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
        if (monthlyReading == null) {
            monthlyReading = new MonthlyReadingModel(0, 0, 0, 0, 0, "", 0, 0, false);
        }
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
     * Vloží všechny záznamy v období bezfaktury
     *
     * @param context  kontext aplikace
     * @param tableTED název tabulky se záznamy pro období bez faktury
     * @param tableFAK název tabulky se záznamy pro faktury
     * @param tableO   název tabulky s měsíčními odečty
     */
    public static void updateAllItemsInvoice(Context context, String tableTED, String tableFAK, String tableO) {
        //poslední záznam vydané faktury
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel lastInvoice = dataInvoiceSource.loadLastInvoiceByDateFromAll(tableFAK);
        dataInvoiceSource.close();
        //seznam měsíčních odečtů zadaných od posledního data vydané faktury
        ArrayList<MonthlyReadingModel> monthlyReadingModels;
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(context);
        dataMonthlyReadingSource.open();
        long lastInvoiceDateTo = 0L;
        double lastInvoiceVtEnd = 0;
        double lastInvoiceNtEnd = 0;
        if (lastInvoice != null) {
            lastInvoiceDateTo = lastInvoice.getDateTo();
            lastInvoiceVtEnd = lastInvoice.getVtEnd();
            lastInvoiceNtEnd = lastInvoice.getNtEnd();
        }

        monthlyReadingModels = dataMonthlyReadingSource.loadMonthlyReading(tableO, lastInvoiceDateTo);
        dataMonthlyReadingSource.close();

        InvoiceModel newInvoice = null;
        long prevPriceListId = -1L;
        ArrayList<InvoiceModel> invoicesModels = new ArrayList<>();

        if (monthlyReadingModels.isEmpty()) {//zobrazení chyby, pokud není žádný měsíční záznam - nelze generovat záznam
            String title = context.getResources().getString(R.string.error);
            String message = context.getResources().getString(R.string.no_monthly_records);
            OwnAlertDialog.show(context, title, message);
        }
        //procházím seznam měsíčních odečtů a vytvářím záznamy faktur. Pokud je výměna elektroměru nebo jiný ceník, vytvoří se nový záznam
        for (int i = 0; i < monthlyReadingModels.size(); i++) {
            MonthlyReadingModel monthlyReadingModel = monthlyReadingModels.get(i);
            if (monthlyReadingModel.isFirst()) {//nový záznam pro období bez faktury, pokud je vyměněn elektroměr
                newInvoice = new InvoiceModel(monthlyReadingModel.getDate(), monthlyReadingModel.getDate(),
                        monthlyReadingModel.getVt(), monthlyReadingModel.getVt(), monthlyReadingModel.getNt(), monthlyReadingModel.getNt(),
                        -1L, -1L, 0, "0", monthlyReadingModel.isFirst());
                invoicesModels.add(newInvoice);
            } else if (i == 0) {//nový záznam pro období bez faktury, údaje jsou s nejstarším měsíčním odečtem
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(lastInvoiceDateTo);
                if (lastInvoiceDateTo > 0)//pokud je datum na 1.1.1970, tak se nic nepřičítá
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                newInvoice = new InvoiceModel(calendar.getTimeInMillis(), monthlyReadingModel.getDate(),
                        lastInvoiceVtEnd, monthlyReadingModel.getVt(), lastInvoiceNtEnd, monthlyReadingModel.getNt(),
                        -1L, monthlyReadingModel.getPriceListId(), 0, "0", monthlyReadingModel.isFirst());
                invoicesModels.add(newInvoice);
            } else if (prevPriceListId != monthlyReadingModel.getPriceListId()) {//další záznam pro období bez faktury, pokud je jiný ceník
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(newInvoice.getDateTo());
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                newInvoice.setDateTo(calendar.getTimeInMillis());
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                long date = calendar.getTimeInMillis();
                double vt = newInvoice.getVtEnd();
                double nt = newInvoice.getNtEnd();
                newInvoice = new InvoiceModel(date, monthlyReadingModel.getDate(),
                        vt, monthlyReadingModel.getVt(), nt, monthlyReadingModel.getNt(),
                        -1L, monthlyReadingModel.getPriceListId(), 0, "0", monthlyReadingModel.isFirst());
                invoicesModels.add(newInvoice);
            } else {//úprava stávajícího záznamu faktury
                newInvoice.setDateTo(monthlyReadingModel.getDate());
                newInvoice.setVtEnd(monthlyReadingModel.getVt());
                newInvoice.setNtEnd(monthlyReadingModel.getNt());
                newInvoice.setIdPriceList(monthlyReadingModel.getPriceListId());

            }
            prevPriceListId = monthlyReadingModel.getPriceListId();
        }

        //smazání původních záznamů, vložení nových záznamů, reset autoincrement
        dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        dataInvoiceSource.deleteAllInvoices(tableTED);
        dataInvoiceSource.insertAllInvoices(tableTED, invoicesModels);
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
        if (monthlyReading == null)
            OwnAlertDialog.show(context, context.getResources().getString(R.string.error), context.getResources().getString(R.string.no_monthly_records));

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


    private static void showAlertDialog(Context context) {
        OwnAlertDialog.show(context, context.getResources().getString(R.string.error),
                context.getResources().getString(R.string.dates_is_not_correct));
    }


}
