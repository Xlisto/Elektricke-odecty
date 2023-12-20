package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;

import static cz.xlisto.odecty.databaze.DbHelper.CASTKA;
import static cz.xlisto.odecty.databaze.DbHelper.CENIK_ID;
import static cz.xlisto.odecty.databaze.DbHelper.CISLO_ELE;
import static cz.xlisto.odecty.databaze.DbHelper.CISLO_FAK;
import static cz.xlisto.odecty.databaze.DbHelper.CISLO_MISTA;
import static cz.xlisto.odecty.databaze.DbHelper.DATUM;
import static cz.xlisto.odecty.databaze.DbHelper.COLUMN_DATE_UNTIL;
import static cz.xlisto.odecty.databaze.DbHelper.COLUMN_DATE_FROM;
import static cz.xlisto.odecty.databaze.DbHelper.DATUM_PLATBY;
import static cz.xlisto.odecty.databaze.DbHelper.FAZE;
import static cz.xlisto.odecty.databaze.DbHelper.GARANCE;
import static cz.xlisto.odecty.databaze.DbHelper.ID_FAK;
import static cz.xlisto.odecty.databaze.DbHelper.MIMORADNA;
import static cz.xlisto.odecty.databaze.DbHelper.NT;
import static cz.xlisto.odecty.databaze.DbHelper.NT_KON;
import static cz.xlisto.odecty.databaze.DbHelper.ODBERENE_MISTO;
import static cz.xlisto.odecty.databaze.DbHelper.ODBER_ID;
import static cz.xlisto.odecty.databaze.DbHelper.POPIS;
import static cz.xlisto.odecty.databaze.DbHelper.POZNAMKA;
import static cz.xlisto.odecty.databaze.DbHelper.PRIKON;
import static cz.xlisto.odecty.databaze.DbHelper.PRVNI_ODECET;
import static cz.xlisto.odecty.databaze.DbHelper.TABLE_NAME_INVOICES;
import static cz.xlisto.odecty.databaze.DbHelper.TABLE_NAME_SUBSCRIPTION_POINT;
import static cz.xlisto.odecty.databaze.DbHelper.VT;
import static cz.xlisto.odecty.databaze.DbHelper.VT_KON;
import static cz.xlisto.odecty.databaze.DbHelper.ZAPLACENO;

/**
 * Přístup k databázi odběrných míst, faktur, měsíčních odečtů atd.
 */
public class DataSubscriptionPointSource extends DataSource{
    private static final String TAG = "DataSubscriptionPointSource";


    public DataSubscriptionPointSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    public long insertSubscriptionPoint(SubscriptionPointModel subScriptionPointModel) {
        dbHelper.createSubscriptionPoint(database, subScriptionPointModel.getMilins());
        return database.insert(TABLE_NAME_SUBSCRIPTION_POINT, null, createContentValue(subScriptionPointModel));
    }


    public void insertMonthlyReading(String tableName,MonthlyReadingModel monthlyReading) {
        database.insert(tableName, null, createContentValue(monthlyReading));
    }


    /**
     * Vloží novou fakturu - číslo faktury, id odběného místa
     *
     * @param numberInvoiceList   číslo faktury
     * @param idSubscriptionPoint id odběrného místa
     */
    public void insertInvoiceList(String numberInvoiceList, long idSubscriptionPoint) {
        database.insert(TABLE_NAME_INVOICES, null, createContentValue(numberInvoiceList, idSubscriptionPoint));
    }


    /**
     * Vloží jeden záznam do faktury
     *
     * @param table   název tabulky
     * @param invoice záznam faktury
     */
    public void insertInvoice(String table, InvoiceModel invoice) {
        database.insert(table, null, createContentValue(invoice));
    }


    public void insertPayment(String table, PaymentModel payment) {
        database.insert(table, null, createContentValue(payment));
    }


    public void updateSubscriptionPoint(SubscriptionPointModel subscriptionPointModel, long itemId) {
        database.update(TABLE_NAME_SUBSCRIPTION_POINT, createContentValue(subscriptionPointModel),
                "_id=?", new String[]{String.valueOf(itemId)});
    }


    public void updateMonthlyReading(MonthlyReadingModel monthlyReading, long itemId, String tableName) {
        database.update(tableName, createContentValue(monthlyReading),
                "_id=?", new String[]{String.valueOf(itemId)});

    }


    public void updateInvoiceList(String number, long id) {
        database.update(TABLE_NAME_INVOICES, createContentValue(number),
                "_id=?", new String[]{String.valueOf(id)});

    }


    public void updateInvoice(long id, String table, InvoiceModel invoice) {
        database.update(table, createContentValue(invoice),
                "_id=?", new String[]{String.valueOf(id)});
    }


    public void updatePayment(long id, String table, PaymentModel payment) {
        database.update(table, createContentValue(payment),
                "_id=?", new String[]{String.valueOf(id)});
    }


    /**
     * Provede součet zálohových plateb ve faktuře
     *
     * @param idFak id faktury
     * @param table název tabulky
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
     * @param idFak id faktury
     * @param table název tabulky
     * @return součet slevy DPH
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
     * @param itemId id odběrného místa
     */
    public void deleteSubscriptionPoint(long itemId, long milins) {
        database.execSQL("DROP TABLE IF EXISTS " + "O" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "HDO" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "FAK" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "TED" + milins);
        database.execSQL("DROP TABLE IF EXISTS " + "PLATBY" + milins);
        database.delete(TABLE_NAME_SUBSCRIPTION_POINT, "_id=?",
                new String[]{String.valueOf(itemId)});
    }


    /**
     * Smaže jeden záznam ve faktuře
     *
     * @param itemId id záznamu
     * @param table  název tabulky
     */
    public void deleteInvoice(long itemId, String table) {
        database.delete(table, "_id=?",
                new String[]{String.valueOf(itemId)});
    }


    /**
     * Smaže jeden záznam platby
     *
     * @param paymentId id platby
     * @param table     název tabulky
     */
    public void deletePayment(long paymentId, String table) {
        database.delete(table, "_id=?",
                new String[]{String.valueOf(paymentId)});
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

            stringArrayList.add(name);
        }
        cursor.close();
        return stringArrayList;
    }


    /**
     * Načte arraylist odběrných míst
     *
     * @return arraylist odběrných míst
     */
    public ArrayList<SubscriptionPointModel> loadSubscriptionPoints() {
        return readSubscriptionPoints(null, null);
    }


    /**
     * Načte arraylist měsíčních odečtů
     *
     * @param table Jméno tabulky s odečty. Začíná na O.
     * @return arraylist měsíčních odečtů
     */
    public ArrayList<MonthlyReadingModel> loadMonthlyReadings(String table, long from, long to) {
        return loadMonthlyReadings(table, null, from, to, DATUM + " DESC, " + PRVNI_ODECET + " DESC, " + VT + " DESC, " + NT + " DESC");
    }


    /**
     * Načte seznam faktur, jako první záznam bude období bez faktury
     *
     * @param subscriptionPoint - odběrné místo
     * @return seznam faktur
     */
    public ArrayList<InvoiceListModel> loadInvoiceLists(SubscriptionPointModel subscriptionPoint) {
        ArrayList<InvoiceListModel> invoices = new ArrayList<>();

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

        String sql = "select *,(SELECT min(datumOd) from " + subscriptionPoint.getTableFAK() + " WHERE id_fak=faktury._id) as minDate from faktury " +
                " WHERE odber_id=?" +
                " ORDER BY minDate DESC";
        String[] args = new String[]{String.valueOf(subscriptionPoint.getId())};

        Cursor cursor = database.rawQuery(sql, args);

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
     * @param idInvoice id faktury
     * @return seznam záznamů faktury
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
     * @param id    id záznamu
     * @param table jméno tabulky
     * @return záznam faktury
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
     * @param cursor kurzor
     * @return záznam faktury
     */
    private InvoiceModel createInvoice(Cursor cursor) {
        return new InvoiceModel(cursor.getLong(0),
                cursor.getLong(1), cursor.getLong(2),
                cursor.getDouble(3), cursor.getDouble(5),
                cursor.getDouble(4), cursor.getDouble(6),
                cursor.getLong(7), cursor.getLong(8),
                cursor.getDouble(9), "", cursor.getInt(10) == 1);
    }


    /**
     * Načte seznam zálohových plateb
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return seznam plateb
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
     * Kontrola alespoň existence jednoho záznamu faktury (pro období bez faktury)
     *
     * @param table název tabulky
     * @return true - existuje, false - neexistuje
     */
    public boolean checkInvoiceExists(String table) {
        String sql = "SELECT count(*) FROM " + table;
        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();
        boolean exists = cursor.getInt(0) > 0;
        cursor.close();
        return exists;
    }


    /**
     * Vytvoří nulový záznam a vloží jej do faktury (pro období bez faktury)
     *
     * @param table název tabulky
     */
    public void insertFirstRecordWithoutInvoice(String table) {
        InvoiceModel invoice = new InvoiceModel(0, 0, 0, 0, 0, 0, -1L, 0, 0, "", false);
        insertInvoice(table, invoice);
    }


    /**
     * Načte jeden záznam zálohové platby podle id
     *
     * @param id    id záznamu
     * @param table jméno tabulky
     * @return záznam zálohové platby
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
     * @param cursor kurzor
     * @return zálohová platba
     */
    private PaymentModel createPayment(Cursor cursor) {
        return new PaymentModel(cursor.getLong(0), cursor.getLong(1),
                cursor.getLong(2), cursor.getDouble(3), cursor.getInt(4));
    }


    /**
     * Načte jedno odběrné místo podle id
     *
     * @param id id odběrného místa
     * @return odběrné místo
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
     * @param table  jméno tabulky
     * @param itemId id odběrného místa
     * @return měsíční odečet
     */
    public MonthlyReadingModel loadMonthlyReading(String table, long itemId, long from, long to) {
        ArrayList<MonthlyReadingModel> monthlyReadings = loadMonthlyReadings(table, itemId, from, to, null);
        if (monthlyReadings.size() > 0) {
            return monthlyReadings.get(0);
        }
        return null;
    }


    /**
     * Najde nejvzdálenější datum záznamu faktury
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return long datum
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
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return long datum
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
        String sql = "SELECT datumOd,vt " +
                "FROM " + table +
                " WHERE id_fak=? " +
                "ORDER BY datumOd ASC";
        return readersStatsInvoice(sql, args);
    }


    public double maxVTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
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
     * @param sql  sql příkaz
     * @param args argumenty
     * @return long datum
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
     * @return faktura
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
     * @return faktura
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
     * Načte poslední fakturu z tabulky, bez ohledu na id faktury
     *
     * @param table jméno tabulky
     * @return faktura
     */
    public InvoiceModel lastInvoiceByDateFromAll(String table) {
        String sql = "SELECT * " +
                "FROM " + table +
                " ORDER BY datumDo DESC" +
                " LIMIT 1";
        return oneInvoice(null, sql);
    }


    /**
     * Vytvoří objekt měsíčního odečtu z kurzoru
     *
     * @param cursor kurzor
     * @return měsíční odečet
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
        return new MonthlyReadingModel(
                id,
                date,
                vt, nt,
                payment,
                description,
                otherServices,
                priceListId,
                firstReading == 1);
    }


    /**
     * Načte první fakturu podle argumentu
     *
     * @param args argument id faktury
     * @param sql  sql dotaz
     * @return faktura
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
     * @param table  jméno tabulky
     * @param itemId id položky
     * @return list měsíčních odečtů
     */
    private ArrayList<MonthlyReadingModel> loadMonthlyReadings(String table, Long itemId, long from, long to, String orderBy) {
        String selection;
        String[] args;
        if (itemId == null) {
            args = new String[]{String.valueOf(from), String.valueOf(to)};
            selection = "datum>=? AND datum<=?";
        } else {
            args = new String[]{String.valueOf(itemId), String.valueOf(from), String.valueOf(to)};
            selection = "_id=? AND datum>=? AND datum<=?";
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
     * @param selection     podmínka
     * @param argsSelection argumenty
     * @return arraylist odběrných míst
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
            String numberElectrometer = cursor.getString(6);
            String numberSubscriptionPoint = cursor.getString(7);
            SubscriptionPointModel subscriptionPoints = new SubscriptionPointModel(id, name, description, milins, countPhaze, phaze, numberElectrometer, numberSubscriptionPoint);
            stringArrayList.add(subscriptionPoints);
        }
        cursor.close();
        return stringArrayList;
    }


    /**
     * Celkový počet ceníků podle id ceníku
     *
     * @param priceId id ceníku
     * @return počet ceníků
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
     * @param subScriptionPoint odběrné místo
     * @return data odběrného místa
     */
    private ContentValues createContentValue(SubscriptionPointModel subScriptionPoint) {
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
     * @param monthlyReading měsíční odečet
     * @return data měsíčního odečtu
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
     * @param numberInvoice       číslo faktury
     * @param idSubscriptionPoint id odběrného místa
     * @return data čísla faktury
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
     * @param numberInvoice číslo faktury
     * @return data čísla faktury
     */
    private ContentValues createContentValue(String numberInvoice) {
        ContentValues values = new ContentValues();
        values.put(CISLO_FAK, numberInvoice);
        return values;
    }


    /**
     * Sestaví jeden zápis faktury
     *
     * @param invoice faktura
     * @return data faktury
     */
    private ContentValues createContentValue(InvoiceModel invoice) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE_FROM, invoice.getDateFrom());
        values.put(COLUMN_DATE_UNTIL, invoice.getDateTo());
        values.put(VT, invoice.getVtStart());
        values.put(VT_KON, invoice.getVtEnd());
        values.put(NT, invoice.getNtStart());
        values.put(NT_KON, invoice.getNtEnd());
        values.put(ID_FAK, invoice.getIdInvoice());
        values.put(CENIK_ID, invoice.getIdPriceList());
        values.put(GARANCE, invoice.getOtherServices());
        //TODO: přejmenovat název sloupce v databázi/ teď se zde nachází údaj o výměně elektroměru
        values.put(DATUM_PLATBY, invoice.isChangedElectricMeter());
        return values;
    }


    /**
     * Sestaví jeden zápis platby
     *
     * @param payment platba
     * @return data platby
     */
    private ContentValues createContentValue(PaymentModel payment) {
        ContentValues values = new ContentValues();
        values.put(CASTKA, payment.getPayment());
        values.put(ID_FAK, payment.getIdFak());
        values.put(MIMORADNA, payment.getTypePayment());
        values.put(DATUM, payment.getDate());
        return values;
    }

}
