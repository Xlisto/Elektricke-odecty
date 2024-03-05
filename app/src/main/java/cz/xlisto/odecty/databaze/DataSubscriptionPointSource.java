package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;

import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.models.MonthlyReadingModel;
import cz.xlisto.odecty.models.PaymentModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;

import static cz.xlisto.odecty.databaze.DbHelper.CASTKA;
import static cz.xlisto.odecty.databaze.DbHelper.CENIK_ID;
import static cz.xlisto.odecty.databaze.DbHelper.CISLO_ELE;
import static cz.xlisto.odecty.databaze.DbHelper.CISLO_MISTA;
import static cz.xlisto.odecty.databaze.DbHelper.COLUMN_ID;
import static cz.xlisto.odecty.databaze.DbHelper.DATUM;
import static cz.xlisto.odecty.databaze.DbHelper.FAZE;
import static cz.xlisto.odecty.databaze.DbHelper.GARANCE;
import static cz.xlisto.odecty.databaze.DbHelper.ID_FAK;
import static cz.xlisto.odecty.databaze.DbHelper.MIMORADNA;
import static cz.xlisto.odecty.databaze.DbHelper.NT;
import static cz.xlisto.odecty.databaze.DbHelper.ODBERENE_MISTO;
import static cz.xlisto.odecty.databaze.DbHelper.ODBER_ID;
import static cz.xlisto.odecty.databaze.DbHelper.POPIS;
import static cz.xlisto.odecty.databaze.DbHelper.POZNAMKA;
import static cz.xlisto.odecty.databaze.DbHelper.PRIKON;
import static cz.xlisto.odecty.databaze.DbHelper.PRVNI_ODECET;
import static cz.xlisto.odecty.databaze.DbHelper.TABLE_NAME_SUBSCRIPTION_POINT;
import static cz.xlisto.odecty.databaze.DbHelper.VT;
import static cz.xlisto.odecty.databaze.DbHelper.ZAPLACENO;


/**
 * Přístup k databázi odběrných míst, měsíčních odečtů atd.
 */
public class DataSubscriptionPointSource extends DataSource {
    private static final String TAG = "DataSubscriptionPointSource";


    public DataSubscriptionPointSource(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }


    public long insertSubscriptionPoint(SubscriptionPointModel subScriptionPointModel) {
        dbHelper.createSubscriptionPoint(database, subScriptionPointModel.getMilins());
        return database.insert(TABLE_NAME_SUBSCRIPTION_POINT, null, createContentValue(subScriptionPointModel));
    }


    public void insertMonthlyReading(String tableName, MonthlyReadingModel monthlyReading) {
        database.insert(tableName, null, createContentValue(monthlyReading));
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


    public void updatePayment(long id, String table, PaymentModel payment) {
        database.update(table, createContentValue(payment),
                "_id=?", new String[]{String.valueOf(id)});
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
        database.delete(TABLE_NAME_SUBSCRIPTION_POINT, COLUMN_ID+"=?",
                new String[]{String.valueOf(itemId)});
    }


    /**
     * Smaže jeden záznam platby
     *
     * @param paymentId id platby
     * @param table     název tabulky
     */
    public void deletePayment(long paymentId, String table) {
        database.delete(table, COLUMN_ID+"=?",
                new String[]{String.valueOf(paymentId)});
    }


    /**
     * Smaže měsíční zálohy u konkrétní faktury
     * @param idFak id faktury
     * @param table název tabulky
     */
    public void deletePayments(long idFak, String table) {
        database.delete(table, ID_FAK+"=?",
                new String[]{String.valueOf(idFak)});
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
     * Sečte platby jedné faktury
     *
     * @param idFak id faktury
     * @param table jméno tabulky
     * @return součet plateb
     */
    public double loadSumPayment(long idFak, String table) {
        String selection = "id_fak=?";
        String[] args = new String[]{String.valueOf(idFak)};
        Cursor cursor = database.query(table, new String[]{"SUM(castka)"}, selection, args, null, null, null);
        cursor.moveToFirst();
        double sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }


    /**
     * Přeřadí platby z období bez faktury ke konkrétní faktuře
     *
     * @param newIdFak id ke které se záznamy přidají
     * @param table   jméno tabulky s faktury přísluší konkrétnímu odběrnému místu
     * @param invoice záznam faktury
     */
    public void changeInvoicePayment(long newIdFak, String table, InvoiceModel invoice) {
        long timeFrom = invoice.getDateFrom();
        long timeTo = invoice.getDateTo();
        String sql = "UPDATE " + table + " SET id_fak = ? WHERE id_fak = '-1' AND datum BETWEEN ? AND ?";
        database.execSQL(sql, new Object[]{newIdFak, timeFrom, timeTo});
    }


    /**
     * Přeřadí platby k jiné faktuře
     * @param newIdFak id ke které se záznamy přidají
     * @param table jméno tabulky s faktury přísluší konkrétnímu odběrnému místu
     * @param idPayment id původní faktury
     */
    public void changeInvoicePayment(long newIdFak, String table, long idPayment){
        String sql = "UPDATE " + table + " SET id_fak = ? WHERE _id = ?";
        database.execSQL(sql, new Object[]{newIdFak, idPayment});
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
        if (!subscriptionPoints.isEmpty())
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
        if (!monthlyReadings.isEmpty()) {
            return monthlyReadings.get(0);
        }
        return null;
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
     * Načte id prvního nalezeného odběrného místa
     * Vhodné pro nastavení aktuálního odběrného místa po aktualizaci
     *
     * @return id odběrného místa
     */
    public long loadFirstIdSubscriptionPoint() {
        Cursor cursor = database.query(TABLE_NAME_SUBSCRIPTION_POINT,
                new String[]{"_id"},
                null,
                null,
                null,
                null,
                null);
        if (cursor.getCount() == 0)
            return -1;
        cursor.moveToFirst();
        long id = cursor.getLong(0);
        cursor.close();
        return id;
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
     * Sečte počet odběrných míst
     *
     * @return počet odběrných míst
     */
    public int countSubscriptionPoints() {
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME_SUBSCRIPTION_POINT, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
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
