package cz.xlisto.cenik.databaze;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Calendar;

import androidx.annotation.Nullable;

public class DbHelperSubscriptionPoint extends SQLiteOpenHelper {
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
    public static final String _ID = "_id";
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
    public static final String PO = "po";
    public static final String UT = "ut";
    public static final String ST = "st";
    public static final String CT = "ct";
    public static final String PA = "pa";
    public static final String SO = "so";
    public static final String NE = "ne";
    public static final String SV = "sv";
    public static final String CAS_OD = "cas_od";
    public static final String CAS_DO = "cas_do";
    public static final String RELE = "rele";
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


    private Context context;


    public DbHelperSubscriptionPoint(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
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
     * @param db
     */
    private void createTables(SQLiteDatabase db) {
        createSubscriptionPointTable(db);
        createInvoicesListTable(db);
        createSettingsTable(db);

    }

    /**
     * Vytvoří všechny tabulky k odběrnému místu a vrátí základní název
     * @param db
     * @return
     */
    public void createSubscriptionPoint(SQLiteDatabase db, long milins){
        createTablesMilins(db, milins);
    }

    /**
     * zavolá metody na vytvoření tabulek s generovaným názvem podle času.
     * Vytvoří tabulky měsíčních odečtů, času HDO, faktur, bezfaktury, zálohových plateb
     * @param db
     */
    private String createTablesMilins(SQLiteDatabase db, long milins){
        createReadingsTable(db,milins);
        createHDOTable(db,milins);
        createInvoicesTable(db,milins);
        createNoInvoiceTable(db,milins);
        createPayments(db,milins);
        return "O"+milins;
    }

    /**
     * Vytvoří tabulku odběrných míst
     */
    private void createSubscriptionPointTable(SQLiteDatabase db) {
        //tabulka odběrný míst
        final String TABLE_SUBSCRIPTION_POINT_CREATE = "CREATE TABLE " + TABLE_NAME_SUBSCRIPTION_POINT +
                "(" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ODBERENE_MISTO + " TEXT, " + POPIS + " TEXT, " +
                ODBER_ID + " TEXT, " + FAZE + " TEXT, " + PRIKON + " TEXT, " + CISLO_ELE + " TEXT, " + CISLO_MISTA + " TEXT );";
        sqlExec(db, TABLE_SUBSCRIPTION_POINT_CREATE);

    }

    /**
     * Vytvoří tabulku pro seznam faktur
     * @param db
     */
    private void createInvoicesListTable(SQLiteDatabase db){
        //seznam faktur
        final String TABLE_INVOICES_CREATE = "CREATE TABLE " + TABLE_NAME_INVOICES +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + CISLO_FAK + " TEXT, " + ODBER_ID + " TEXT);";
        sqlExec(db,TABLE_INVOICES_CREATE);
    }

    /**
     * Vytvoří tabulku na seznamm nastavení
     * @param db
     */
    private void createSettingsTable(SQLiteDatabase db){
        //tabulka s nastavením
        final String TABLE_SETTINGS_CREATE = "CREATE TABLE " + TABLE_NAME_SETTINGS +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + JMENO + " TEXT, " + HODNOTA + " TEXT );";
        sqlExec(db,TABLE_SETTINGS_CREATE);
    }

    /**
     * Vytvoří tabulku pro měsíční odečty
     * @param db
     * @param milins
     */
    private void createReadingsTable(SQLiteDatabase db, long milins){
        //tabulka měsíčních odečtů
        final String TABLE_READING_CREATE = "CREATE TABLE " + TABLE_NAME_O + milins +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + VT + " TEXT, " + NT + " TEXT, " + CENIK_ID + " TEXT, " +
                DATUM + " TEXT, " + PRVNI_ODECET + " TEXT, " + ZAPLACENO + " TEXT, " + POZNAMKA + " TEXT, " + GARANCE + " TEXT );";
        sqlExec(db,TABLE_READING_CREATE);
    }

    /**
     * Vytvoří tabulku pro časy HDO
     * @param db
     * @param milins
     */
    private void createHDOTable(SQLiteDatabase db, long milins){
        //tabulka HDO
        final String TABLE_HDO_CREATE = "CREATE TABLE " + TABLE_NAME_HDO + milins +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATUM_OD + " TEXT, " + DATUM_DO + " TEXT, " +
                PO + " TEXT, " + UT + " TEXT, " + ST + " TEXT, " + CT + " TEXT, " + PA + " TEXT, " + SO + " TEXT, " + NE + " TEXT, " +
                SV + " TEXT, " + CAS_OD + " TEXT, " + CAS_DO + " TEXT, " + RELE + " TEXT, " + DISTRIBUCE + " TEXT );";
        sqlExec(db,TABLE_HDO_CREATE);
    }

    /**
     * Vytvoří tabulku pro jednotlivé záznamy faktury
     * @param db
     * @param milins
     */
    private void createInvoicesTable(SQLiteDatabase db, long milins) {
        //faktura s odečty
        final String TABLE_FAK_CREATE = "CREATE TABLE " + TABLE_NAME_FAK + milins +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATUM_OD + " TEXT, " + DATUM_DO + " TEXT, " + VT + " TEXT, " +
                NT + " TEXT, " + VT_KON + " TEXT, " + NT_KON + " TEXT, " + ID_FAK + " TEXT, " + CENIK_ID + " TEXT, " + GARANCE + " TEXT, " + DATUM_PLATBY + " TEXT, " +
                STALA_PLATBA + " TEXT );";
        sqlExec(db,TABLE_FAK_CREATE);
    }

    /**
     * Vytvoří pro aktuální období bez faktury
     * @param db
     * @param milins
     */
    private void createNoInvoiceTable(SQLiteDatabase db, long milins) {
        //aktuální obdobý bez vystavené faktury
        final String TABLE_TED_CREATE = "CREATE TABLE " + TABLE_NAME_TED + milins +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DATUM_OD + " TEXT, " + DATUM_DO + " TEXT, " + VT + " TEXT, " +
                NT + " TEXT, " + VT_KON + " TEXT, " + NT_KON + " TEXT, " + ID_FAK + " TEXT, " + CENIK_ID + " TEXT, " + GARANCE + " TEXT, " + DATUM_PLATBY + " TEXT, " +
                STALA_PLATBA + " TEXT );";
        sqlExec(db,TABLE_TED_CREATE);
    }

    /**
     * Vytvoří tabulku pro zaplacené zálohy
     * @param db
     * @param milins
     */
    private void createPayments(SQLiteDatabase db, long milins) {
        //zaplacené zálohy
        final String TABLE_PAYMENT_CREATE = "CREATE TABLE " + TABLE_NAME_PLATBY + milins +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ID_FAK + " TEXT, " + DATUM + " TEXT, " + CASTKA + " TEXT, " +
                MIMORADNA + " TEXT, " + REZERVA1 + " TEXT, " + REZERVA2 + " TEXT );";
        sqlExec(db,TABLE_PAYMENT_CREATE);
    }

    private void sqlExec(SQLiteDatabase db, String sql) {
        db.execSQL(sql);
    }

}
