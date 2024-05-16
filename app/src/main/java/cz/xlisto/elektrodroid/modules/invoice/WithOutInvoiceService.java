package cz.xlisto.elektrodroid.modules.invoice;


import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Třída obsahuje metody pro úpravu záznamů v období bez faktury
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
    public static void editLastItemInInvoice(Context context, String tableTED, String tableFAK, MonthlyReadingModel monthlyReading) {
        if (monthlyReading == null) {
            monthlyReading = new MonthlyReadingModel(0, 0, 0, 0, 0, "", 0, 0, false);
        }

        //od měsíčního odečtu odečítám jeden den, protože se jedná o koncová data
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(monthlyReading.getDate());
        if (monthlyReading.getDate() > 0)
            calendar.add(Calendar.DAY_OF_MONTH, -1);

        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel invoice = dataInvoiceSource.lastInvoiceByDate(-1L, tableTED);
        InvoiceModel lastInvoice = dataInvoiceSource.loadLastInvoiceByDateFromAll(tableFAK);
        if (lastInvoice == null) {//zobrazení chyby, pokud není žádná faktura - nelze generovat záznam
            dataInvoiceSource.deleteAllInvoices(tableTED);
            dataInvoiceSource.close();
            String title = context.getResources().getString(R.string.error);
            String message = context.getResources().getString(R.string.no_invoice_records);
            OwnAlertDialog.show(context, title, message);
            return;
        }
        invoice.setDateTo(calendar.getTimeInMillis());
        invoice.setVtEnd(monthlyReading.getVt());
        invoice.setNtEnd(monthlyReading.getNt());
        dataInvoiceSource.updateInvoice(invoice.getId(), tableTED, invoice);
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
        Log.w(TAG, "updateAllItemsInvoice: " + tableTED + " " + tableFAK + " " + tableO);
        //poslední záznam vydané faktury
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(context);
        dataInvoiceSource.open();
        InvoiceModel lastInvoice = dataInvoiceSource.loadLastInvoiceByDateFromAll(tableFAK);
        if (lastInvoice == null)
            dataInvoiceSource.deleteAllInvoices(tableTED);
        dataInvoiceSource.close();
        Log.w(TAG, "updateAllItemsInvoice: lastInvoice:" + lastInvoice);
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

        Calendar dateLastInvoice = Calendar.getInstance();
        dateLastInvoice.setTimeInMillis(lastInvoiceDateTo);
        dateLastInvoice.add(Calendar.DAY_OF_MONTH, 1);

        //načte měsíční odečty od poslední faktury
        monthlyReadingModels = dataMonthlyReadingSource.loadMonthlyReading(tableO, dateLastInvoice.getTimeInMillis());
        dataMonthlyReadingSource.close();

        InvoiceModel newInvoice = null;
        long prevPriceListId = -1L;
        ArrayList<InvoiceModel> invoicesModels = new ArrayList<>();

        Log.w(TAG, "updateAllItemsInvoice: size:" + invoicesModels.size());
        if (lastInvoice == null) {//zobrazení chyby, pokud není žádná faktura - nelze generovat záznam
            String title = context.getResources().getString(R.string.error);
            String message = context.getResources().getString(R.string.no_invoice_records);
            OwnAlertDialog.show(context, title, message);
            return;
        }

        if (monthlyReadingModels.isEmpty()) {//zobrazení chyby, pokud není žádný měsíční záznam od poslední faktury - nelze generovat záznam
            String title = context.getResources().getString(R.string.error);
            String message = context.getResources().getString(R.string.no_monthly_records);
            OwnAlertDialog.show(context, title, message);
            return;
        }

        for (MonthlyReadingModel mr : monthlyReadingModels) {
            Log.w(TAG, "updateAllItemsInvoice: měsíční odečet:" + ViewHelper.convertLongToDate(mr.getDate()) + " " + mr.isFirst());
        }
        //procházím seznam měsíčních odečtů a vytvářím záznamy faktur. Pokud je výměna elektroměru nebo jiný ceník, vytvoří se nový záznam
        for (int i = 0; i < monthlyReadingModels.size(); i++) {
            MonthlyReadingModel monthlyReadingModel = monthlyReadingModels.get(i);
            Log.w(TAG, "updateAllItemsInvoice: " + monthlyReadingModel.getDate() + " " + monthlyReadingModel.isFirst());
            if (monthlyReadingModel.isFirst()) {//nový záznam pro období bez faktury, pokud je vyměněn elektroměr
                Log.w(TAG, "updateAllItemsInvoice: isFirst " + monthlyReadingModel.getDate() + " " + monthlyReadingModel.isFirst());
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(monthlyReadingModel.getDate());
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                newInvoice = new InvoiceModel(calendar.getTimeInMillis(), calendar.getTimeInMillis(),
                        monthlyReadingModel.getVt(), monthlyReadingModel.getVt(), monthlyReadingModel.getNt(), monthlyReadingModel.getNt(),
                        -1L, -1L, 0, "0", monthlyReadingModel.isFirst());
                invoicesModels.add(newInvoice);
            } else if (i == 0) {//nový záznam pro období bez faktury, údaje jsou s nejstarším měsíčním odečtem
                Log.w(TAG, "updateAllItemsInvoice: i==0 " + monthlyReadingModel.getDate() + " " + monthlyReadingModel.isFirst());
                //Calendar dateLastInvoice = Calendar.getInstance();
                Calendar dateLastMonthlyReading = Calendar.getInstance();
                dateLastInvoice.setTimeInMillis(lastInvoiceDateTo);
                dateLastMonthlyReading.setTimeInMillis(monthlyReadingModel.getDate());
                //dateLastMonthlyReading.add(Calendar.DAY_OF_MONTH, -1);
                if (lastInvoiceDateTo > 0)//pokud je datum na 1.1.1970, tak se nic nepřičítá
                    dateLastInvoice.add(Calendar.DAY_OF_MONTH, 1);
                newInvoice = new InvoiceModel(dateLastInvoice.getTimeInMillis(), dateLastMonthlyReading.getTimeInMillis(),
                        lastInvoiceVtEnd, monthlyReadingModel.getVt(), lastInvoiceNtEnd, monthlyReadingModel.getNt(),
                        -1L, monthlyReadingModel.getPriceListId(), 0, "0", monthlyReadingModel.isFirst());
                invoicesModels.add(newInvoice);
            } else if (prevPriceListId != monthlyReadingModel.getPriceListId()) {//další záznam pro období bez faktury, pokud je jiný ceník
                Log.w(TAG, "updateAllItemsInvoice: prevPriceListId != " + monthlyReadingModel.getDate() + " " + monthlyReadingModel.isFirst());
                Calendar dateFrom = Calendar.getInstance();
                Calendar dateTo = Calendar.getInstance();
                dateTo.setTimeInMillis(monthlyReadingModel.getDate());
                dateTo.add(Calendar.DAY_OF_MONTH, -1);
                dateFrom.setTimeInMillis(newInvoice.getDateTo());
                dateFrom.add(Calendar.DAY_OF_MONTH, -1);
                newInvoice.setDateTo(dateFrom.getTimeInMillis());
                dateFrom.add(Calendar.DAY_OF_MONTH, 1);

                double vt = newInvoice.getVtEnd();
                double nt = newInvoice.getNtEnd();
                newInvoice = new InvoiceModel(dateFrom.getTimeInMillis(), dateTo.getTimeInMillis(),
                        vt, monthlyReadingModel.getVt(), nt, monthlyReadingModel.getNt(),
                        -1L, monthlyReadingModel.getPriceListId(), 0, "0", monthlyReadingModel.isFirst());
                invoicesModels.add(newInvoice);
            } else {//úprava stávajícího záznamu faktury
                Log.w(TAG, "updateAllItemsInvoice: else " + monthlyReadingModel.getDate() + " " + monthlyReadingModel.isFirst());
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(monthlyReadingModel.getDate());
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                newInvoice.setDateTo(calendar.getTimeInMillis());
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
        if (monthlyReading == null) {//pokud nejsou měsíční odečty, zobrazí se chyba a smaže se stávající období bez faktury
            OwnAlertDialog.show(context, context.getResources().getString(R.string.error), context.getResources().getString(R.string.no_monthly_records));
            dataInvoiceSource.deleteAllInvoices(Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
            dataMonthlyReadingSource.close();
            dataInvoiceSource.close();
            return;
        }

        if (itemLastInvoice == null) {//pokud nejsou žádné faktury, zobrazí se chyba a smaže se stávající období bez faktury
            dataInvoiceSource.deleteAllInvoices(Objects.requireNonNull(SubscriptionPoint.load(context)).getTableTED());
            dataMonthlyReadingSource.close();
            dataInvoiceSource.close();
            OwnAlertDialog.show(context, context.getResources().getString(R.string.error), context.getResources().getString(R.string.no_invoice_records));
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(itemLastInvoice.getDateTo());
        calendar.add(Calendar.DATE, 1);
        itemFirstWithoutInvoice.setDateFrom(calendar.getTimeInMillis());
        itemFirstWithoutInvoice.setVtStart(itemLastInvoice.getVtEnd());
        itemFirstWithoutInvoice.setNtStart(itemLastInvoice.getNtEnd());
        if (calendar.getTimeInMillis() > monthlyReading.getDate()) {
            showAlertDialog(context);
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
