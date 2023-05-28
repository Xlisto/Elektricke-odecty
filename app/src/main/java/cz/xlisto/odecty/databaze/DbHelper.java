package cz.xlisto.odecty.databaze;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "odecty_a_mista";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME_SUBSCRIPTION_POINT = "odbernaMista";
    public static final String TABLE_NAME_SETTINGS = "nastaveni";
    public static final String TABLE_NAME_INVOICES = "faktury";
    public static final String TABLE_NAME_O = "O";
    public static final String TABLE_NAME_HDO = "HDO";
    public static final String TABLE_NAME_FAK = "FAK";
    public static final String TABLE_NAME_TED = "TED";
    public static final String TABLE_NAME_PLATBY = "PLATBY";
    //odběrné místo
    public static final String ODBERENE_MISTO = "odberne_misto";
    public static final String COLUMN_ID = "_id";
    public static final String POPIS = "popis";
    public static final String ODBER_ID = "odber_id";
    public static final String FAZE = "faze";
    public static final String PRIKON = "prikon";
    public static final String CISLO_ELE = "cislo_ele";
    public static final String CISLO_MISTA = "cislo_mista";
    //měsíční odečty
    public static final String VT = "vt";
    public static final String NT = "nt";
    public static final String CENIK_ID = "cenik_id";
    public static final String DATUM = "datum";
    public static final String PRVNI_ODECET = "prvni_odecet";
    public static final String ZAPLACENO = "zaplaceno";
    public static final String POZNAMKA = "poznamka";
    public static final String GARANCE = "garance";
    //nastavení
    public static final String JMENO = "jmeno";
    public static final String HODNOTA = "hodnota";
    //HDO
    public static final String DATUM_OD = "datumOd";
    public static final String DATUM_DO = "datumDo";
    public static final String COLUMN_MON = "po";
    public static final String COLUMN_TUE = "ut";
    public static final String COLUMN_WED = "st";
    public static final String COLUMN_THU = "ct";
    public static final String COLUMN_FRI = "pa";
    public static final String COLUMN_SAT = "so";
    public static final String COLUMN_SUN = "ne";
    public static final String SV = "sv";
    public static final String COLUMN_TIME_FROM = "cas_od";
    public static final String COLUMN_TIME_UNTIL = "cas_do";
    public static final String COLUMN_RELE = "rele";
    public static final String DISTRIBUCE = "distribuce";
    //faktury
    public static final String CISLO_FAK = "cislo_fak";
    //faktura - detail
    public static final String VT_KON = "vt_kon";
    public static final String NT_KON = "nt_kon";
    public static final String ID_FAK = "id_fak";
    public static final String DATUM_PLATBY = "datum_platby";
    public static final String STALA_PLATBA = "stala_platba";
    //platby
    public static final String CASTKA = "castka";
    public static final String MIMORADNA = "mimoradna";
    public static final String REZERVA1 = "rezerva1";
    public static final String REZERVA2 = "rezerva2";


    public DbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    /**
     * Zavolá metody na vytvoření tabulek odběrných míst, seznamu faktur a nastavení
     *
     * @param db SQLiteDatabase databáze
     */
    private void createTables(SQLiteDatabase db) {
        createSubscriptionPointTable(db);
        createInvoicesListTable(db);
        createSettingsTable(db);

    }


    /**
     * Vytvoří všechny tabulky k odběrnému místu a vrátí základní název
     *
     * @param db SQLiteDatabase databáze
     */
    public void createSubscriptionPoint(SQLiteDatabase db, long milins) {
        createTablesMilins(db, milins);
    }


    /**
     * zavolá metody na vytvoření tabulek s generovaným názvem podle času.
     * Vytvoří tabulky měsíčních odečtů, času HDO, faktur, bezfaktury, zálohových plateb
     *
     * @param db SQLiteDatabase databáze
     */
    private void createTablesMilins(SQLiteDatabase db, long milins) {
        createReadingsTable(db, milins);
        createHDOTable(db, milins);
        createInvoicesTable(db, milins);
        createNoInvoiceTable(db, milins);
        createPayments(db, milins);
    }


    /**
     * Vytvoří tabulku odběrných míst
     */
    private void createSubscriptionPointTable(SQLiteDatabase db) {
        //tabulka odběrných míst
        final String TABLE_SUBSCRIPTION_POINT_CREATE = "CREATE TABLE " + TABLE_NAME_SUBSCRIPTION_POINT +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ODBERENE_MISTO + " TEXT, " + POPIS + " TEXT, " +
                ODBER_ID + " TEXT, " + FAZE + " TEXT, " + PRIKON + " TEXT, " + CISLO_ELE + " TEXT, " + CISLO_MISTA + " TEXT );";
        sqlExec(db, TABLE_SUBSCRIPTION_POINT_CREATE);

    }


    /**
     * Vytvoří tabulku pro seznam faktur
     *
     * @param db SQLiteDatabase databáze
     */
    private void createInvoicesListTable(SQLiteDatabase db) {
        //seznam faktur
        final String TABLE_INVOICES_CREATE = "CREATE TABLE " + TABLE_NAME_INVOICES +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + CISLO_FAK + " TEXT, " + ODBER_ID + " TEXT);";
        sqlExec(db, TABLE_INVOICES_CREATE);
    }


    /**
     * Vytvoří tabulku na seznam nastavení
     *
     * @param db SQLiteDatabase databáze
     */
    private void createSettingsTable(SQLiteDatabase db) {
        //tabulka s nastavením
        final String TABLE_SETTINGS_CREATE = "CREATE TABLE " + TABLE_NAME_SETTINGS +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + JMENO + " TEXT, " + HODNOTA + " TEXT );";
        sqlExec(db, TABLE_SETTINGS_CREATE);
    }


    /**
     * Vytvoří tabulku pro měsíční odečty
     *
     * @param db    SQLiteDatabase databáze
     * @param milins long čas v milisekundách pro rozlišení tabulek
     */
    private void createReadingsTable(SQLiteDatabase db, long milins) {
        //tabulka měsíčních odečtů
        final String TABLE_READING_CREATE = "CREATE TABLE " + TABLE_NAME_O + milins +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + VT + " TEXT, " + NT + " TEXT, " + CENIK_ID + " TEXT, " +
                DATUM + " TEXT, " + PRVNI_ODECET + " TEXT, " + ZAPLACENO + " TEXT, " + POZNAMKA + " TEXT, " + GARANCE + " TEXT );";
        sqlExec(db, TABLE_READING_CREATE);
    }


    /**
     * Vytvoří tabulku pro časy HDO
     *
     * @param db   SQLiteDatabase databáze
     * @param milins long čas v milisekundách pro rozlišení tabulek
     */
    private void createHDOTable(SQLiteDatabase db, long milins) {
        //tabulka HDO
        final String TABLE_HDO_CREATE = "CREATE TABLE " + TABLE_NAME_HDO + milins +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATUM_OD + " TEXT, " + DATUM_DO + " TEXT, " +
                COLUMN_MON + " TEXT, " + COLUMN_TUE + " TEXT, " + COLUMN_WED + " TEXT, " + COLUMN_THU + " TEXT, " + COLUMN_FRI + " TEXT, " + COLUMN_SAT + " TEXT, " + COLUMN_SUN + " TEXT, " +
                SV + " TEXT, " + COLUMN_TIME_FROM + " TEXT, " + COLUMN_TIME_UNTIL + " TEXT, " + COLUMN_RELE + " TEXT, " + DISTRIBUCE + " TEXT );";
        sqlExec(db, TABLE_HDO_CREATE);
    }


    /**
     * Vytvoří tabulku pro jednotlivé záznamy faktury
     *
     * @param db   SQLiteDatabase databáze
     * @param milins long čas v milisekundách pro rozlišení tabulek
     */
    private void createInvoicesTable(SQLiteDatabase db, long milins) {
        //faktura s odečty
        final String TABLE_FAK_CREATE = "CREATE TABLE " + TABLE_NAME_FAK + milins +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATUM_OD + " TEXT, " + DATUM_DO + " TEXT, " + VT + " TEXT, " +
                NT + " TEXT, " + VT_KON + " TEXT, " + NT_KON + " TEXT, " + ID_FAK + " TEXT, " + CENIK_ID + " TEXT, " + GARANCE + " TEXT, " + DATUM_PLATBY + " TEXT, " +
                STALA_PLATBA + " TEXT );";
        sqlExec(db, TABLE_FAK_CREATE);
    }


    /**
     * Vytvoří pro aktuální období bez faktury
     *
     * @param db  SQLiteDatabase databáze
     * @param milins long čas v milisekundách pro rozlišení tabulek
     */
    private void createNoInvoiceTable(SQLiteDatabase db, long milins) {
        //aktuální období bez vystavené faktury
        final String TABLE_TED_CREATE = "CREATE TABLE " + TABLE_NAME_TED + milins +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATUM_OD + " TEXT, " + DATUM_DO + " TEXT, " + VT + " TEXT, " +
                NT + " TEXT, " + VT_KON + " TEXT, " + NT_KON + " TEXT, " + ID_FAK + " TEXT, " + CENIK_ID + " TEXT, " + GARANCE + " TEXT, " + DATUM_PLATBY + " TEXT, " +
                STALA_PLATBA + " TEXT );";
        sqlExec(db, TABLE_TED_CREATE);
    }


    /**
     * Vytvoří tabulku pro zaplacené zálohy
     *
     * @param db   SQLiteDatabase databáze
     * @param milins long čas v milisekundách pro rozlišení tabulek
     */
    private void createPayments(SQLiteDatabase db, long milins) {
        //zaplacené zálohy
        final String TABLE_PAYMENT_CREATE = "CREATE TABLE " + TABLE_NAME_PLATBY + milins +
                " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ID_FAK + " TEXT, " + DATUM + " TEXT, " + CASTKA + " TEXT, " +
                MIMORADNA + " TEXT, " + REZERVA1 + " TEXT, " + REZERVA2 + " TEXT );";
        sqlExec(db, TABLE_PAYMENT_CREATE);
    }


    private void sqlExec(SQLiteDatabase db, String sql) {
        db.execSQL(sql);
    }
}
