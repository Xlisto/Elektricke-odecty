package cz.xlisto.odecty.databaze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.models.PriceListSumModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;

import static cz.xlisto.odecty.databaze.DBHelperPriceList.AUTOR;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.CENA_NT;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.CENA_VT;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.CINNOST;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.DAN;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.DATUM_VYTVORENI;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.DISTRIBUCE;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.DIST_NT;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.DIST_VT;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.DPH;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.EMAIL;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.FIRMA;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J0;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J1;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J10;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J11;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J12;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J13;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J14;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J2;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J3;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J4;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J5;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J6;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J7;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J8;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.J9;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.MESIC_PLAT;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.OTE;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.OZE;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.PLATNOST_DO;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.PLATNOST_OD;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.POZE1;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.POZE2;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.PRODUKT;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.RADA;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.SAZBA;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.SYSTEM_SLUZBY;
import static cz.xlisto.odecty.databaze.DBHelperPriceList.TABLE_NAME_PRICE;

public class DataPriceListSource {
    private final String TAG = getClass().getName() + " ";
    public static final String VSE = "[VŠE]";
    private final Context context;
    private DBHelperPriceList dbHelperPriceList;
    private SQLiteDatabase database;


    public DataPriceListSource(Context context) {
        this.context = context;
        dbHelperPriceList = new DBHelperPriceList(context);
    }


    /**
     * Otevře spojení s databází ceníku
     *
     * @throws SQLException pokud se nepodaří otevřít databázi
     */
    public void open() throws SQLException {
        if (database != null) {
            return;
        }

        try {
            database = dbHelperPriceList.getWritableDatabase();
        } catch (NullPointerException e) {
            e.printStackTrace();
            dbHelperPriceList = new DBHelperPriceList(context);
            database = dbHelperPriceList.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Zavře spojení s databází ceníku
     */
    public void close() {
        if (dbHelperPriceList != null) {
            database.close();
            database = null;
        }
    }


    /**
     * Přidá jednu sazbu ceníku
     *
     * @param priceListModel objekt ceník
     * @return long insertID id vloženého ceníku
     */
    public long insertPriceList(PriceListModel priceListModel) {
        return database.insert(TABLE_NAME_PRICE, null, createContentValue(priceListModel));
    }


    /**
     * Upraví jednu sazbu ceníku
     *
     * @param priceListModel objekt ceník
     * @param itemId         id ceníku
     * @return long updateID id upraveného ceníku
     */
    public long updatePriceList(PriceListModel priceListModel, long itemId) {
        return database.update(TABLE_NAME_PRICE, createContentValue(priceListModel),
                "_id=?", new String[]{String.valueOf(itemId)});
    }


    /**
     * Smaže ceník podle ID
     *
     * @param itemId id ceníku
     */
    public void deletePriceList(long itemId) {
        database.delete(TABLE_NAME_PRICE, "_id=?",
                new String[]{String.valueOf(itemId)});
    }


    /**
     * Vyhledá id ceníků ve všech tabulkách, které nejsou využitý a následně  je smaže
     */
    public void deleteUnusedPriceList() {
        //dotaz na všechny použité id ceníků
        StringBuilder select = new StringBuilder("SELECT cenik_id \nFROM (");
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPointModels = dataSubscriptionPointSource.loadSubscriptionPoints();
        //sestavení dotazu na všechny použité id ceníků
        for (int i = 0; i < subscriptionPointModels.size(); i++) {
            if (i > 0)
                select.append("\nUNION\n");
            select.append("SELECT DISTINCT cenik_id FROM ").append(subscriptionPointModels.get(i).getTableO()).append("\nUNION\n");
            select.append("SELECT DISTINCT cenik_id FROM ").append(subscriptionPointModels.get(i).getTableFAK());
        }

        select.append(");");

        Cursor cursor = dataSubscriptionPointSource.database.rawQuery(select.toString(), null);
        //seznam použitých id ceníků
        String[] ids = new String[cursor.getCount()];
        //podle počtu použitých id ceníků se vytvoří počet zástupných znaků (?)
        StringBuilder questionMarks = new StringBuilder();
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            ids[i] = cursor.getString(0);
            if (i > 0)
                questionMarks.append(",");
            questionMarks.append("?");
        }
        database.delete(TABLE_NAME_PRICE, "_id NOT IN (" + questionMarks + ")", ids);
        cursor.close();
        dataSubscriptionPointSource.close();
    }


    /**
     * Vytvoří ContentValues pro vložení do databáze
     *
     * @param priceListModel objekt ceník
     * @return ContentValues hodnoty ceníku
     */
    private ContentValues createContentValue(PriceListModel priceListModel) {
        ContentValues values = new ContentValues();
        values.put(RADA, priceListModel.getRada());
        values.put(PRODUKT, priceListModel.getProdukt());
        values.put(FIRMA, priceListModel.getFirma());
        values.put(CENA_VT, priceListModel.getCenaVT());
        values.put(CENA_NT, priceListModel.getCenaNT());
        values.put(MESIC_PLAT, priceListModel.getMesicniPlat());
        values.put(DAN, priceListModel.getDan());
        values.put(SAZBA, priceListModel.getSazba());
        values.put(DIST_VT, priceListModel.getDistVT());
        values.put(DIST_NT, priceListModel.getDistNT());
        values.put(J0, priceListModel.getJ0());
        values.put(J1, priceListModel.getJ1());
        values.put(J2, priceListModel.getJ2());
        values.put(J3, priceListModel.getJ3());
        values.put(J4, priceListModel.getJ4());
        values.put(J5, priceListModel.getJ5());
        values.put(J6, priceListModel.getJ6());
        values.put(J7, priceListModel.getJ7());
        values.put(J8, priceListModel.getJ8());
        values.put(J9, priceListModel.getJ9());
        values.put(J10, priceListModel.getJ10());
        values.put(J11, priceListModel.getJ11());
        values.put(J12, priceListModel.getJ12());
        values.put(J13, priceListModel.getJ13());
        values.put(J14, priceListModel.getJ14());
        values.put(SYSTEM_SLUZBY, priceListModel.getSystemSluzby());
        values.put(CINNOST, priceListModel.getCinnost());
        values.put(POZE1, priceListModel.getPoze1());
        values.put(POZE2, priceListModel.getPoze2());
        values.put(OZE, priceListModel.getOze());
        values.put(OTE, priceListModel.getOte());
        values.put(PLATNOST_OD, priceListModel.getPlatnostOD());
        values.put(PLATNOST_DO, priceListModel.getPlatnostDO());
        values.put(DPH, priceListModel.getDph());
        values.put(DISTRIBUCE, priceListModel.getDistribuce());
        values.put(AUTOR, priceListModel.getAutor());
        values.put(DATUM_VYTVORENI, priceListModel.getDatumVytvoreni());
        values.put(EMAIL, priceListModel.getEmail());
        return values;
    }


    /**
     * Načte všechny ceníky, filtr je nastaven na výběr všeho
     *
     * @return ArrayList<PriceListModel> seznam ceníků
     */
    public ArrayList<PriceListModel> readPriceList() {
        return readPriceList("%", "%", "%", "%", "%", "%");
    }


    /**
     * Spočítá ceníky podle filtru
     *
     * @param rada       filtr pro název řady
     * @param firma      filtr pro název distribuční firmy
     * @param platnostOd filtr pro datum platnosti
     * @param distribuce filtr pro název distribučního území
     * @return int počet ceníků
     */
    public int countPriceListItems(String rada, String produkt, String sazba, String firma, String platnostOd, String distribuce) {
        String sql = "SELECT count(*) FROM ceniky WHERE rada = ? AND produkt = ? AND sazba = ? AND firma = ? AND platnost_od = ? AND distribuce = ? ";
        String[] args = new String[]{rada, produkt, sazba, firma, platnostOd, distribuce};
        Cursor cursor = database.rawQuery(sql, args);
        cursor.moveToFirst();
        int itemsCount = cursor.getInt(0);
        cursor.close();
        return itemsCount;
    }


    /**
     * Vrátí id ceníku podle parametrů
     *
     * @param rada       filtr pro název řady
     * @param produkt    filtr pro název produktu
     * @param sazba      filtr pro sazbu
     * @param firma      filtr pro název dodavatele
     * @param platnostOd filtr pro datum platnosti
     * @param distribuce filtr pro název distribučního území
     * @return long id ceníku
     */
    public long idPriceListItem(String rada, String produkt, String sazba, String firma, String platnostOd, String distribuce) {
        String sql = "SELECT _id FROM ceniky WHERE rada = ? AND produkt = ? AND sazba = ? AND firma = ? AND platnost_od = ? AND distribuce = ? ";
        String[] args = new String[]{rada, produkt, sazba, firma, platnostOd, distribuce};
        Cursor cursor = database.rawQuery(sql, args);
        cursor.moveToFirst();
        long id = cursor.getLong(0);
        cursor.close();
        return id;
    }


    /**
     * Načte všechny ceníky vyhovující filtru
     *
     * @param rada      filtr pro název řady
     * @param produkt   filtr pro název produktu
     * @param sazba     filtr pro sazbu
     * @param dodavatel filtr pro název dodavatele
     * @param uzemi     filtr pro název území
     * @param datum     filtr pro datum platnosti
     * @return ArrayList<PriceListModel> seznam ceníků
     */
    public ArrayList<PriceListModel> readPriceList(String rada, String produkt, String sazba, String dodavatel, String uzemi, String datum) {
        String selection = "rada LIKE ? AND produkt LIKE ? AND sazba LIKE ? AND firma LIKE ? AND distribuce LIKE ? AND platnost_od LIKE ?";
        String[] args = new String[]{rada, produkt, sazba, dodavatel, uzemi, datum};
        return readPriceList(selection, args);
    }


    /**
     * Načte jeden ceník podle id v databázi
     *
     * @param id id ceníku
     * @return PriceListModel objekt ceníku
     */
    public PriceListModel readPrice(long id) {
        String selection = "_id=?";
        String[] argsSelection = new String[]{String.valueOf(id)};
        ArrayList<PriceListModel> priceListModels = readPriceList(selection, argsSelection);
        if (priceListModels.size() > 0)
            return priceListModels.get(0);
        else
            return new PriceListModel();
    }


    /**
     * Privátní metoda pro načítání ceníků podle parametrů
     *
     * @param selection     podmínka pro výběr ceníku
     * @param argsSelection argumenty pro výběr ceníku
     * @return ArrayList<PriceListModel> seznam ceníků
     */
    private ArrayList<PriceListModel> readPriceList(String selection, String[] argsSelection) {
        Cursor cursor = database.query(TABLE_NAME_PRICE, null,
                selection,
                argsSelection,
                null, null, "rada, produkt, firma");
        ArrayList<PriceListModel> priceListModels = new ArrayList<>();

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            PriceListModel priceListModel = new PriceListModel(cursor.getLong(0), cursor.getString(1), //id,rada
                    cursor.getString(2), cursor.getString(3), //produkt, firma
                    cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getDouble(7),//cena vt cena nt,měs. plat,dan
                    cursor.getString(8),//sazba
                    cursor.getDouble(9), cursor.getDouble(10), cursor.getDouble(11),//dist vt, dist nt, j0
                    cursor.getDouble(12), cursor.getDouble(13), cursor.getDouble(14),//j1,j2,j3
                    cursor.getDouble(15), cursor.getDouble(16), cursor.getDouble(17),//j4,j5,j6
                    cursor.getDouble(18), cursor.getDouble(19), cursor.getDouble(20),//j7,j8,j9
                    cursor.getDouble(31), cursor.getDouble(32), cursor.getDouble(33),//j10,j11,j12
                    cursor.getDouble(34), cursor.getDouble(35),//j13,j14
                    cursor.getDouble(21), cursor.getDouble(22), cursor.getDouble(23),//system_sluzby,cinnost,poze1
                    cursor.getDouble(24), cursor.getDouble(25), cursor.getDouble(26),//poze2,oze,ote
                    cursor.getLong(27), cursor.getLong(28), cursor.getDouble(29),//platnost_od,platnost_do,dph
                    cursor.getString(30),//distribuce,
                    cursor.getString(36), cursor.getLong(37), cursor.getString(38)//autor,datum vytvoření,email

            );
            priceListModels.add(priceListModel);
        }
        cursor.close();
        return priceListModels;
    }


    /**
     * Načte souhrný seznam ceníků podle podle rada a platnost_od
     *
     * @return ArrayList<PriceListSumModel> seznam souhrnu ceníků
     */
    public ArrayList<PriceListSumModel> readSumPriceList() {
        Cursor cursor = database.query(TABLE_NAME_PRICE, new String[]{"rada", "distribuce", "platnost_od", "firma","count(*)"}, null, null,
                "rada, platnost_od", null, null);
        ArrayList<PriceListSumModel> priceListSumModels = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            PriceListSumModel priceListSumModel = new PriceListSumModel(cursor.getString(0),
                    cursor.getString(1), cursor.getLong(2), cursor.getString(3),cursor.getInt(4));
            priceListSumModels.add(priceListSumModel);
        }
        cursor.close();
        return priceListSumModels;
    }


    /**
     * Načte seznam ceníků podle podle názvů
     *
     * @param column konstanty DBHelperPriceList.RADA, DBHelperPriceList.PRODUKT, DBHelperPriceList.SAZBA,
     *               DBHelperPriceList.FIRMA,DBHelperPriceList.DISTRIBUCE a DBHelperPriceList.DATUM_PLATNOSTI
     *               PLATNOST_OD převede z Long na datum String
     * @return ArrayList<String> seznam názvů
     */
    public ArrayList<String> readPriceListCount(String column) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(VSE);
        String[] columns = new String[]{column, "COUNT(" + column + ") AS count"};
        Cursor cursor = database.query(TABLE_NAME_PRICE, columns,
                null, null,
                column,
                "count>0",
                null);
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            if (column.equals(PLATNOST_OD)) {
                arrayList.add(ViewHelper.convertLongToDate(cursor.getLong(0)));
            } else {
                arrayList.add(cursor.getString(0));
            }

        }
        cursor.close();
        return arrayList;
    }


    /**
     * Celkový počet ceníků
     *
     * @return int počet ceníků
     */
    public int countPriceItems() {
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME_PRICE, null);
        cursor.moveToFirst();
        int itemsCount = cursor.getInt(0);
        cursor.close();
        return itemsCount;
    }


}
