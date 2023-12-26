package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.InvoiceListModel;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;

import static cz.xlisto.odecty.databaze.DbHelper.CENIK_ID;
import static cz.xlisto.odecty.databaze.DbHelper.CISLO_FAK;
import static cz.xlisto.odecty.databaze.DbHelper.COLUMN_DATE_FROM;
import static cz.xlisto.odecty.databaze.DbHelper.COLUMN_DATE_UNTIL;
import static cz.xlisto.odecty.databaze.DbHelper.DATUM_PLATBY;
import static cz.xlisto.odecty.databaze.DbHelper.GARANCE;
import static cz.xlisto.odecty.databaze.DbHelper.ID_FAK;
import static cz.xlisto.odecty.databaze.DbHelper.NT;
import static cz.xlisto.odecty.databaze.DbHelper.NT_KON;
import static cz.xlisto.odecty.databaze.DbHelper.ODBER_ID;
import static cz.xlisto.odecty.databaze.DbHelper.TABLE_NAME_INVOICES;
import static cz.xlisto.odecty.databaze.DbHelper.VT;
import static cz.xlisto.odecty.databaze.DbHelper.VT_KON;

/**
 * Přístup k databázi faktur
 * Xlisto 26.12.2023 18:45
 */
public class DataInvoiceSource extends DataSource {
    private static final String TAG = "DataInvoiceSource";

    public DataInvoiceSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
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


    /**
     * Aktualizuje číslo faktury
     *
     * @param number číslo faktury
     * @param id     id faktury
     */
    public void updateInvoiceList(String number, long id) {
        database.update(TABLE_NAME_INVOICES, createContentValue(number),
                "_id=?", new String[]{String.valueOf(id)});

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
     * Aktualizuje záznam faktury
     *
     * @param id      id záznamu
     * @param table   jméno tabulky
     * @param invoice objekt faktury InvoiceModel
     */
    public void updateInvoice(long id, String table, InvoiceModel invoice) {
        database.update(table, createContentValue(invoice),
                "_id=?", new String[]{String.valueOf(id)});
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
     * Vytvoří nulový záznam a vloží jej do faktury (pro období bez faktury)
     *
     * @param table název tabulky
     */
    public void insertFirstRecordWithoutInvoice(String table) {
        InvoiceModel invoice = new InvoiceModel(0, 0, 0, 0, 0, 0, -1L, 0, 0, "", false);
        insertInvoice(table, invoice);
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
        return getLongCountInvoice(sql, args);
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
        return getLongCountInvoice(sql, args);
    }


    /**
     * Vrátí počet záznamů ve faktuře
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return long počet záznamů
     */
    public long countItems(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT COUNT(*)" +
                " FROM " + table +
                " WHERE id_fak=?";
        return getLongCountInvoice(sql, args);
    }


    /**
     * Najde nejnižší hodnotu VT záznamu faktury
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return double hodnota VT
     */
    public double minVTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT " + COLUMN_DATE_FROM + "," + VT + " " +
                "FROM " + table +
                " WHERE " + ID_FAK + "=? " +
                "ORDER BY " + COLUMN_DATE_FROM + " ASC";
        return getDoubleStatsInvoice(sql, args);
    }


    /**
     * Najde nejvyšší hodnotu VT záznamu faktury
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return double hodnota VT
     */
    public double maxVTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT " + COLUMN_DATE_UNTIL + "," + VT_KON + " " +
                "FROM " + table +
                " WHERE " + ID_FAK + "=? " +
                "ORDER BY " + COLUMN_DATE_UNTIL + " DESC";
        return getDoubleStatsInvoice(sql, args);
    }


    /**
     * Najde nejnižší hodnotu NT záznamu faktury
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return double hodnota NT
     */
    public double minNTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT " + COLUMN_DATE_FROM + "," + NT + " " +
                "FROM " + table +
                " WHERE " + ID_FAK + "=? " +
                "ORDER BY " + COLUMN_DATE_FROM + " ASC";
        return getDoubleStatsInvoice(sql, args);
    }


    /**
     * Najde nejvyšší hodnotu NT záznamu faktury
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return double hodnota NT
     */
    public double maxNTInvoice(long idFak, String table) {
        String[] args = new String[]{String.valueOf(idFak)};
        String sql = "SELECT " + COLUMN_DATE_UNTIL + "," + NT_KON + " " +
                "FROM " + table +
                " WHERE " + ID_FAK + "=? " +
                "ORDER BY " + COLUMN_DATE_UNTIL + " DESC";
        return getDoubleStatsInvoice(sql, args);
    }


    /**
     * Provede sql příkaz s argumentem a vrátí první výsledek jako long
     * Vhodné pro agregační funkce
     *
     * @param sql  sql příkaz
     * @param args argumenty
     * @return long datum
     */
    private long getLongCountInvoice(String sql, String[] args) {
        Cursor cursor = database.rawQuery(sql, args);
        cursor.moveToFirst();
        long date = cursor.getLong(0);
        cursor.close();
        return date;
    }


    /**
     * Provede sql příkaz s argumentem a vrátí první výsledek jako double
     * Vhodné pro agregační funkce
     * @param sql sql příkaz
     * @param args argumenty
     * @return double hodnota
     */
    private double getDoubleStatsInvoice(String sql, String[] args) {
        Cursor cursor = database.rawQuery(sql, args);
        if (cursor.getCount() == 0)
            return 0;
        cursor.moveToFirst();
        double date = cursor.getDouble(1);
        cursor.close();
        return date;
    }
}
