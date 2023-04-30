package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;

import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.CASTKA;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.CENIK_ID;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.CISLO_ELE;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.CISLO_FAK;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.CISLO_MISTA;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.DATUM;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.DATUM_DO;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.DATUM_OD;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.FAZE;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.GARANCE;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.ID_FAK;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.MIMORADNA;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.NT;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.NT_KON;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.ODBERENE_MISTO;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.ODBER_ID;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.POPIS;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.POZNAMKA;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.PRIKON;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.PRVNI_ODECET;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.TABLE_NAME_INVOICES;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.TABLE_NAME_SUBSCRIPTION_POINT;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.VT;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.VT_KON;
import static cz.xlisto.odecty.databaze.DbHelperSubscriptionPoint.ZAPLACENO;

/**
 * Přístuo k databázi odběrných míst, faktur, měsíčních odečtů atd.
 * Created by xlisto on 11.11.16.
 */
public class DataSubscriptionPointSource {
    private final String TAG = getClass().getName() + " ";
    private Context context;
    private DbHelperSubscriptionPoint dbHelperSubscriptionPoint;
    private SQLiteDatabase database;

    public DataSubscriptionPointSource(Context context) {
        this.context = context;
        dbHelperSubscriptionPoint = new DbHelperSubscriptionPoint(context);
    }

    /**
     * Otevře spojení s databází odečtů, odběrných míst atd.
     *
     * @return
     * @throws SQLException
     */
    public void open() throws SQLException {
        if (database != null) {
            return;
        }

        try {
            database = dbHelperSubscriptionPoint.getWritableDatabase();
        } catch (NullPointerException e) {
            e.printStackTrace();
            dbHelperSubscriptionPoint = new DbHelperSubscriptionPoint(context);
            database = dbHelperSubscriptionPoint.getWritableDatabase();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Zavře spojení s databází ceníku
     */
    public void close() {
        if (dbHelperSubscriptionPoint != null) {
            database.close();
        }
    }

    public long insertSubscriptionPoint(SubscriptionPointModel subScriptionPointModel) {
        dbHelperSubscriptionPoint.createSubscriptionPoint(database, subScriptionPointModel.getMilins());
        return database.insert(TABLE_NAME_SUBSCRIPTION_POINT, null, createContenValue(subScriptionPointModel));
    }

    public long insertMonthlyReading(MonthlyReadingModel monthlyReading, String tableName) {
        return database.insert(tableName, null, createContentValue(monthlyReading));
    }

    /**
     * Vloží novou fakturu - číslo faktury, id odběného místa
     *
     * @param numberInvoiceList
     * @param idSubscriptionPoint
     * @return
     */
    public long insertInvoiceList(String numberInvoiceList, long idSubscriptionPoint) {
        return database.insert(TABLE_NAME_INVOICES, null, createContentValue(numberInvoiceList, idSubscriptionPoint));
    }

    /**
     * Vloží jeden záznam do faktury
     *
     * @param table
     * @param invoice
     * @return
     */
    public long insertInvoice(String table, InvoiceModel invoice) {
        return database.insert(table, null, createContentValue(invoice));
    }


    public long insertPayment(String table, PaymentModel payment) {
        return database.insert(table, null, createContentValue(payment));
    }

    public long updateSubscriptionPoint(SubscriptionPointModel subscriptionPointModel, long itemId) {
        return database.update(TABLE_NAME_SUBSCRIPTION_POINT, createContenValue(subscriptionPointModel),
                "_id=?", new String[]{String.valueOf(itemId)});
    }

    public long updateMonthlyReading(MonthlyReadingModel monthlyReading, long itemId, String tableName) {
        return database.update(tableName, createContentValue(monthlyReading),
                "_id=?", new String[]{String.valueOf(itemId)});

    }

    public long updateInvoiceList(String number, long id) {
        return database.update(TABLE_NAME_INVOICES, createContentValue(number),
                "_id=?", new String[]{String.valueOf(id)});

    }

    public long updateInvoice(long id, String table, InvoiceModel invoice) {
        //Log.w(TAG, "invoice " + id + " " + table);
        //Log.w(TAG, "invoice id " + invoice.getIdInvoice());
        //Log.w(TAG, "invoice fa " + invoice.getIdPriceList());
        return database.update(table, createContentValue(invoice),
                "_id=?", new String[]{String.valueOf(id)});
        //return -1l;
    }

    public long updatePayment(long id, String table, PaymentModel payment) {
        return database.update(table, createContentValue(payment),
                "_id=?", new String[]{String.valueOf(id)});
    }

    /**
     * Provede součet zálohových plateb ve faktuře
     *
     * @param idFak
     * @param table
     */
    public double sumPayment(long idFak, String table) {
        Cursor cursor = database.rawQuery("SELECT (" +
                "IFNULL((SELECT sum(castka) FROM " + table + " WHERE id_fak=? AND mimoradna!=3),0)  - " +
                "IFNULL((SELECT sum(castka) FROM " + table + " WHERE id_fak=? AND mimoradna=3),0)) as total " +
                "GROUP BY total", new String[]{String.valueOf(idFak), String.valueOf(idFak)});
        cursor.moveToFirst();
        double result = cursor.getDouble(0);
        cursor.close();
        return result;
    }

    /**
     * Provede součet slevy DPH zálohových plateb v listopadu a prosinci 2021
     *
     * @param idFak
     * @param table
     * @return
     */
    public double sumDiscount(long idFak, String table) {
        Cursor cursor = database.rawQuery("SELECT (" +
                        "IFNULL((SELECT sum(castka*0.21) FROM " + table + " WHERE id_fak=? AND mimoradna!=3 AND datum >= 1635721200000 AND datum <= 1640908800000),0))",
                new String[]{String.valueOf(idFak)});
        cursor.moveToFirst();
        double result = cursor.getDouble(0);
        cursor.close();
        return result;
    }

    /**
     * Smaže odběrné místo podle ID
     *
     * @param itemId
     * @return
     */
    public long deleteSubscriptionPoint(long itemId, long milins) {
        database.execSQL("DROP TABLE IF EXISTS " + "O" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "HDO" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "FAK" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "TED" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "PLATBY" + milins);
        long deleteId = database.delete(TABLE_NAME_SUBSCRIPTION_POINT, "_id=?",
                new String[]{String.valueOf(itemId)});
        return deleteId;
    }

    /**
     * Smaže měsíční odečet podle ID
     *
     * @param itemId
     * @param table
     * @return
     */
    public long deleteMonthlyReading(long itemId, String table) {
        long deleteId = database.delete(table, "_id=?",
                new String[]{String.valueOf(itemId)});
        return deleteId;
    }

    /**
     * Smaže jeden záznam ve faktuře
     *
     * @param itemId
     * @param table
     * @return
     */
    public long deleteInvoice(long itemId, String table) {
        long deleteId = database.delete(table, "_id=?",
                new String[]{String.valueOf(itemId)});
        return deleteId;
    }

    /**
     * Smaže jeden záznam platby
     *
     * @param paymentId
     * @param table
     * @return
     */
    public long deletePayment(long paymentId, String table) {
        long deleteId = database.delete(table, "_id=?",
                new String[]{String.valueOf(paymentId)});
        return deleteId;
    }

    public ArrayList<String> loadSubscriptionPointName() {
        ArrayList<String> stringArrayList = new ArrayList<>();
        Cursor cursor = database.query(TABLE_NAME_SUBSCRIPTION_POINT,
                new String[]{ODBERENE_MISTO},
                null,
                null,
                null,
                null,
                null);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            String name = cursor.getString(0);
            //String description = cursor.getString(2);
            //int countPhaze = cursor.getInt(4);
            //int phaze = cursor.getInt(5);
            //String numerElectrometer = cursor.getString(6);
            //String numberSubscriptionPoint = cursor.getString(7);
            //SubscriptionPointModel subscriptionPointModel = new SubscriptionPointModel(name,description,,countPhaze,phaze,numerElectrometer,numberSubscriptionPoint);
            stringArrayList.add(name);
        }
        cursor.close();
        return stringArrayList;
    }

    /**
     * Načte arraylist odběrných míst
     *
     * @return
     */
    public ArrayList<SubscriptionPointModel> loadSubscriptionPoints() {
        return readSubscriptionPoints(null, null);
    }

    /**
     * Načte arraylist měsíčních odečtů
     *
     * @param table Jméno tabulky s odečty. Začíná na O.
     * @return
     */
    public ArrayList<MonthlyReadingModel> loadMonthlyReadings(String table) {
        return loadMonthlyReadings(table, null, DATUM + " DESC, " + PRVNI_ODECET + " ASC, " + VT + " DESC, " + NT + " DESC");
    }

    /**
     * Načte seznam faktur, jako první záznam bude období bez faktury
     *
     * @param subscriptionPoint - odběrné místo
     * @return
     */
    public ArrayList<InvoiceListModel> loadInvoiceLists(SubscriptionPointModel subscriptionPoint) {
        ArrayList<InvoiceListModel> invoices = new ArrayList<>();

        String sql0 = "select * WHERE " + subscriptionPoint.getTableTED() +
                " WHERE ";

        //načtení údajů o období bez faktury
        long minDateNow = minDateInvoice(-1L, subscriptionPoint.getTableTED());
        long maxDateNow = maxDateInvoice(-1L, subscriptionPoint.getTableTED());
        double minVtNow = minVTInvoice(-1L, subscriptionPoint.getTableTED());
        double maxVtNow = maxVTInvoice(-1L, subscriptionPoint.getTableTED());
        double minNtNow = minNTInvoice(-1L, subscriptionPoint.getTableTED());
        double maxNtNow = maxNTInvoice(-1L, subscriptionPoint.getTableTED());
        long countPaymentsNow = countItems(-1L, subscriptionPoint.getTablePLATBY());
        long countReadsNow = countItems(-1L, subscriptionPoint.getTableTED());

        invoices.add(new InvoiceListModel(-1L, "Období bez faktury", minDateNow, maxDateNow, countPaymentsNow, countReadsNow, minVtNow, maxVtNow, minNtNow, maxNtNow));


        String selection = "odber_id=?";
        String sql = "select *,(SELECT min(datumOd) from " + subscriptionPoint.getTableFAK() + " WHERE id_fak=faktury._id) as minDate from faktury " +
                " WHERE odber_id=?" +
                " ORDER BY minDate DESC";
        String[] args = new String[]{String.valueOf(subscriptionPoint.get_id())};


        Cursor cursor = database.rawQuery(sql, args);
        /*Cursor cursor = database.query(TABLE_NAME_INVOICES,
                null,
                selection,
                args,
                null,
                null,
                null);*/

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            long idFak = cursor.getLong(0);
            long minDate = minDateInvoice(idFak, subscriptionPoint.getTableFAK());
            long maxDate = maxDateInvoice(idFak, subscriptionPoint.getTableFAK());
            double minVt = minVTInvoice(idFak, subscriptionPoint.getTableFAK());
            double maxVt = maxVTInvoice(idFak, subscriptionPoint.getTableFAK());
            double minNt = minNTInvoice(idFak, subscriptionPoint.getTableFAK());
            double maxNt = maxNTInvoice(idFak, subscriptionPoint.getTableFAK());
            long countPayments = countItems(idFak, subscriptionPoint.getTablePLATBY());
            long countReads = countItems(idFak, subscriptionPoint.getTableFAK());
            invoices.add(new InvoiceListModel(cursor.getLong(0), cursor.getString(1), minDate, maxDate, countPayments, countReads, minVt, maxVt, minNt, maxNt));
        }
        cursor.close();


        return invoices;
    }


    /**
     * Načte seznam záznamů faktury podle id faktury
     *
     * @param idInvoice
     * @return
     */
    public ArrayList<InvoiceModel> loadInvoices(long idInvoice, String table) {
        String selection = "id_fak=?";
        String[] args = new String[]{String.valueOf(idInvoice)};

        ArrayList<InvoiceModel> invoices = new ArrayList<>();
        Cursor cursor = database.query(table, null, selection, args, null, null, "datumOd DESC");
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            invoices.add(createInvoice(cursor));
        }
        cursor.close();
        return invoices;
    }

    /**
     * Načte jeden záznam faktury podle id z databáze
     *
     * @param id
     * @param table
     * @return
     */
    public InvoiceModel loadInvoice(long id, String table) {
        InvoiceModel invoice = null;
        String selection = "_id=?";
        String[] args = new String[]{String.valueOf(id)};

        Cursor cursor = database.query(table, null, selection, args, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            invoice = createInvoice(cursor);
        }
        cursor.close();
        return invoice;

    }

    /**
     * Sestaví objekt jednoho záznamu faktury z cursor
     *
     * @param cursor
     * @return
     */
    private InvoiceModel createInvoice(Cursor cursor) {
        return new InvoiceModel(cursor.getLong(0),
                cursor.getLong(1), cursor.getLong(2),
                cursor.getDouble(3), cursor.getDouble(5),
                cursor.getDouble(4), cursor.getDouble(6),
                cursor.getLong(7), cursor.getLong(8),
                cursor.getDouble(9), "");
    }

    /**
     * Načte seznam zálohových plateb
     *
     * @param idFak
     * @param table
     * @return
     */
    public ArrayList<PaymentModel> loadPayments(long idFak, String table) {
        String selection = "id_fak=?";
        String[] args = new String[]{String.valueOf(idFak)};

        ArrayList<PaymentModel> payments = new ArrayList<>();
        Cursor cursor = database.query(table, null, selection, args, null, null, "datum DESC");
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            payments.add(createPayment(cursor));
        }
        cursor.close();
        return payments;
    }

    /**
     * Načte jeden záznam zálohové platby podle id
     *
     * @param id
     * @param table
     * @return
     */
    public PaymentModel loadPayment(long id, String table) {
        PaymentModel payment = null;
        String selection = "_id=?";
        String[] args = new String[]{String.valueOf(id)};
        Cursor cursor = database.query(table, null, selection, args, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            payment = createPayment(cursor);
        }
        cursor.close();
        return payment;
    }

    /**
     * Sestaví objekt zálohové platby
     *
     * @param cursor
     * @return
     */
    private PaymentModel createPayment(Cursor cursor) {
        return new PaymentModel(cursor.getLong(0), cursor.getLong(1),
                cursor.getLong(2), cursor.getDouble(3), cursor.getInt(4));
    }

    /**
     * Načte jedno odběrné místo podle id
     *
     * @param id
     * @return
     */
    public SubscriptionPointModel loadSubscriptionPoint(long id) {
        String selection = "_id=?";
        String[] args = new String[]{String.valueOf(id)};
        ArrayList<SubscriptionPointModel> subscriptionPoints = readSubscriptionPoints(selection, args);
        if (subscriptionPoints.size() > 0)
            return subscriptionPoints.get(0);
        else
            return null;
    }

    /**
     * Načte jeden měsíční odečet
     *
     * @param table
     * @param itemId
     * @return
     */
    public MonthlyReadingModel loadMonthlyReading(String table, long itemId) {
        ArrayList<MonthlyReadingModel> monthlyReadings = loadMonthlyReadings(table, itemId, null);
        if (monthlyReadings.size() > 0) {
            return monthlyReadings.get(0);
        }
        return null;
    }

    /**
     * Najde nejvzdálenější datum záznamu faktury
     *
     * @param idFak
     * @param table
     * @return long
     */
    public long minDateInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT MIN(datumOd)" +
                " FROM " + table +
                " WHERE id_fak=?";
        return dateInvoice(sql, args);
    }

    /**
     * Najde nejbližší datum záznamu faktury
     *
     * @param idFak
     * @param table
     * @return long
     */
    public long maxDateInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT MAX(datumDo)" +
                " FROM " + table +
                " WHERE id_fak=?";
        return dateInvoice(sql, args);
    }

    public long countItems(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT COUNT(*)" +
                " FROM " + table +
                " WHERE id_fak=?";
        return dateInvoice(sql, args);
    }

    public double minVTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        /*String sql = "SELECT MIN(vt)" +
                " FROM " + table +
                " WHERE id_fak=?";*/
        String sql = "SELECT datumOd,vt " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumOd ASC";
        return readersStatsInvoice(sql, args);
    }

    public double maxVTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        /*String sql = "SELECT MAX(vt_kon)" +
                " FROM " + table +
                " WHERE id_fak=?";*/
        String sql = "SELECT datumDo,vt_kon " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumDo DESC";
        return readersStatsInvoice(sql, args);
    }

    public double minNTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT datumOd,nt " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumOd ASC";
        return readersStatsInvoice(sql, args);
    }

    public double maxNTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT datumDo,nt_kon " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumDo DESC";
        return readersStatsInvoice(sql, args);
    }

    /**
     * Provede sql příkaz s argumentem a  vrátí první výsledek
     *
     * @param sql
     * @param args
     * @return long
     */
    private long dateInvoice(String sql, String[] args) {
        Cursor cursor = database.rawQuery(sql, args);
        cursor.moveToFirst();
        long date = cursor.getLong(0);
        cursor.close();
        return date;
    }


    private double readersStatsInvoice(String sql, String[] args) {
        Cursor cursor = database.rawQuery(sql, args);
        if (cursor.getCount() == 0)
            return 0;
        cursor.moveToFirst();
        double date = cursor.getDouble(1);
        cursor.close();
        return date;
    }

    /**
     * Načte první fakturu podle id
     *
     * @param idFak long id faktury
     * @return
     */
    public InvoiceModel firstInvoiceByDate(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT * " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumOd ASC";
        return oneInvoice(args, sql);
    }

    /**
     * Načte poslední fakturu podle id
     *
     * @param idFak long id faktury
     * @return
     */
    public InvoiceModel lastInvoiceByDate(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT * " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumOd DESC";
        return oneInvoice(args, sql);
    }

    /**
     * Nasčte poslední fakturu z tabulky, bez ohledu na id faktury
     * @param table
     * @return
     */
    public InvoiceModel lastInvoiceByDateFromAll(String table) {
        String sql = "SELECT * " +
                "FROM " + table +
                " ORDER BY datumDo DESC" +
                " LIMIT 1";
        return oneInvoice(null, sql);
    }

    public MonthlyReadingModel lastMonthlyReadingByDate(String table) {
        //String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT * " +
                "FROM " + table +
                " ORDER BY datum DESC" +
                " LIMIT 1";

        Cursor cursor = database.rawQuery(sql, null);
        MonthlyReadingModel monthlyReading = null;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            monthlyReading = createMonthlyReading(cursor);
        }
        cursor.close();
        return monthlyReading;
    }

    /**
     * Vytvoří objekt měsíčního odečtu z kurzoru
     * @param cursor
     * @return
     */
    private MonthlyReadingModel createMonthlyReading(Cursor cursor) {
        long id = cursor.getLong(0);
        double vt = cursor.getDouble(1);
        double nt = cursor.getDouble(2);
        long priceListId = cursor.getLong(3);
        long date = cursor.getLong(4);
        int firstReading = cursor.getInt(5);
        double payment = cursor.getDouble(6);
        String description = cursor.getString(7);
        double otherServices = cursor.getDouble(8);
        MonthlyReadingModel monthlyReading = new MonthlyReadingModel(
                id,
                date,
                vt, nt,
                payment,
                description,
                otherServices,
                priceListId,
                (firstReading == 1) ? true : false);
        return monthlyReading;
    }

    /**
     * Načte první fakturu podel argumentu
     *
     * @param args argument id faktury
     * @param sql  sql dotaz
     * @return
     */
    private InvoiceModel oneInvoice(String[] args, String sql) {
        InvoiceModel invoice = null;
        Cursor cursor = database.rawQuery(sql, args);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            invoice = createInvoice(cursor);
        }

        cursor.close();
        return invoice;
    }

    /**
     * Privátní metoda pro načtení měsíčních odečtů podle parametrů
     *
     * @param table
     * @param itemId
     * @return
     */
    private ArrayList<MonthlyReadingModel> loadMonthlyReadings(String table, Long itemId, String orderBy) {
        String selection = "_id=?";
        String[] args = new String[]{String.valueOf(itemId)};
        if (itemId == null) {
            args = null;
            selection = null;
        }

        ArrayList<MonthlyReadingModel> monthlyReadings = new ArrayList<>();
        Cursor cursor = database.query(table,
                null,
                selection,
                args,
                null,
                null,
                orderBy);

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            monthlyReadings.add(createMonthlyReading(cursor));
        }
        cursor.close();
        return monthlyReadings;
    }

    /**
     * Načte arraylist odběrných míst podle argumentů a setřídí podle názvu odběrného místa
     *
     * @param selection
     * @param argsSelection
     * @return
     */
    private ArrayList<SubscriptionPointModel> readSubscriptionPoints(String selection, String[] argsSelection) {
        ArrayList<SubscriptionPointModel> stringArrayList = new ArrayList<>();
        Cursor cursor = database.query(TABLE_NAME_SUBSCRIPTION_POINT,
                null,
                selection,
                argsSelection,
                null,
                null,
                ODBERENE_MISTO);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            String description = cursor.getString(2);
            long milins = Long.parseLong(cursor.getString(3).replace("O", ""));
            int countPhaze = cursor.getInt(4);
            int phaze = cursor.getInt(5);
            String numerElectrometer = cursor.getString(6);
            String numberSubscriptionPoint = cursor.getString(7);
            SubscriptionPointModel subscriptionPoints = new SubscriptionPointModel(id, name, description, milins, countPhaze, phaze, numerElectrometer, numberSubscriptionPoint);
            stringArrayList.add(subscriptionPoints);
        }
        cursor.close();
        return stringArrayList;
    }

    /** Celkový počet ceníků podle id ceníku
     * @param priceId
     * @return
     */

    public int countPriceItems(String table, long priceId) {
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + table + " WHERE cenik_id=" + priceId, null);
        cursor.moveToFirst();
        int itemsCount = cursor.getInt(0);
        cursor.close();
        return itemsCount;
    }


    /**
     * Sestaví data odběrného místa pro zápis do databáze
     *
     * @param subScriptionPoint
     * @return
     */
    private ContentValues createContenValue(SubscriptionPointModel subScriptionPoint) {
        ContentValues values = new ContentValues();
        values.put(ODBERENE_MISTO, subScriptionPoint.getName());
        values.put(POPIS, subScriptionPoint.getDescription());
        values.put(ODBER_ID, subScriptionPoint.getTableO());
        values.put(FAZE, subScriptionPoint.getCountPhaze());
        values.put(PRIKON, subScriptionPoint.getPhaze());
        values.put(CISLO_ELE, subScriptionPoint.getNumberElectricMeter());
        values.put(CISLO_MISTA, subScriptionPoint.getNumberSubscriptionPoint());
        return values;
    }

    /**
     * Sestaví data měsíčního odečtu pro zápis do databáze
     *
     * @param monthlyReading
     * @return
     */
    private ContentValues createContentValue(MonthlyReadingModel monthlyReading) {
        ContentValues values = new ContentValues();
        values.put(VT, monthlyReading.getVt());
        values.put(NT, monthlyReading.getNt());
        values.put(ZAPLACENO, monthlyReading.getPayment());
        values.put(CENIK_ID, monthlyReading.getPriceListId());
        values.put(DATUM, monthlyReading.getDate());
        values.put(PRVNI_ODECET, monthlyReading.isFirst());
        values.put(GARANCE, monthlyReading.getOtherServices());
        values.put(POZNAMKA, monthlyReading.getDescription());
        return values;
    }

    /**
     * Sestaví data čísla faktury pro seznam faktur pro zápis do databáze
     *
     * @param numberInvoice
     * @param idSubscriptionPoint
     * @return
     */
    private ContentValues createContentValue(String numberInvoice, long idSubscriptionPoint) {
        ContentValues values = new ContentValues();
        values.put(CISLO_FAK, numberInvoice);
        values.put(ODBER_ID, idSubscriptionPoint);
        return values;
    }

    /**
     * Sestaví číslo faktury pro úpravu dat do databáze
     *
     * @param numberInvoice
     * @return
     */
    private ContentValues createContentValue(String numberInvoice) {
        ContentValues values = new ContentValues();
        values.put(CISLO_FAK, numberInvoice);
        return values;
    }

    /**
     * Sestaví jeden zápis faktury
     *
     * @param invoice
     * @return
     */
    private ContentValues createContentValue(InvoiceModel invoice) {
        ContentValues values = new ContentValues();
        values.put(DATUM_OD, invoice.getDateFrom());
        values.put(DATUM_DO, invoice.getDateTo());
        values.put(VT, invoice.getVtStart());
        values.put(VT_KON, invoice.getVtEnd());
        values.put(NT, invoice.getNtStart());
        values.put(NT_KON, invoice.getNtEnd());
        values.put(ID_FAK, invoice.getIdInvoice());
        values.put(CENIK_ID, invoice.getIdPriceList());
        values.put(GARANCE, invoice.getOtherServices());
        return values;
    }

    private ContentValues createContentValue(PaymentModel payment) {
        ContentValues values = new ContentValues();
        values.put(CASTKA, payment.getPayment());
        values.put(ID_FAK, payment.getIdFak());
        values.put(MIMORADNA, payment.getTypePayment());
        values.put(DATUM, payment.getDate());
        return values;
    }

}
