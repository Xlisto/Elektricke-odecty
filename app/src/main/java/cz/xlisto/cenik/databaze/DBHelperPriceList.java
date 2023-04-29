package cz.xlisto.cenik.databaze;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelperPriceList extends SQLiteOpenHelper {
    public static final String DATABASE_NAME_PRICE_LIST = "databaze_cenik";
    public static final String TABLE_NAME_PRICE = "ceniky";
    public static final int DATABASE_VERSION = 1;

    public static final String FIRMA = "firma";
    public static final String RADA = "rada";
    public static final String CENA_VT = "cena_vt";
    public static final String PRODUKT = "produkt";
    public static final String CENA_NT = "cena_nt";
    public static final String MESIC_PLAT = "mesic_plat";
    public static final String DAN = "dan";
    public static final String SAZBA = "sazba";
    public static final String DIST_VT = "dist_vt";
    public static final String DIST_NT = "dist_nt";
    public static final String J0 = "j0";
    public static final String J1 = "j1";
    public static final String J2 = "j2";
    public static final String J3 = "j3";
    public static final String J4 = "j4";
    public static final String J5 = "j5";
    public static final String J6 = "j6";
    public static final String J7 = "j7";
    public static final String J8 = "j8";
    public static final String J9 = "j9";
    public static final String J10 = "j10";
    public static final String J11 = "j11";
    public static final String J12 = "j12";
    public static final String J13 = "j13";
    public static final String J14 = "j14";
    public static final String SYSTEM_SLUZBY = "system_sluzby";
    public static final String CINNOST = "cinnost";
    public static final String POZE1 = "poze1";
    public static final String POZE2 = "poze2";
    public static final String OZE = "oze";
    public static final String OTE = "ote";
    public static final String PLATNOST_OD = "platnost_od";
    public static final String PLATNOST_DO = "platnost_do";
    public static final String DPH = "dph";
    public static final String DISTRIBUCE = "distribuce";
    public static final String AUTOR = "autor";
    public static final String DATUM_VYTVORENI = "datum_vytvoreni";
    public static final String EMAIL = "email";
    public static final String REZERVA1 = "rezerva1";
    public static final String REZERVA2 = "rezerva2";

    private static final String TABLE_PRICELIST_CREATE = "CREATE TABLE " + TABLE_NAME_PRICE +
            " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            RADA + " TEXT, " +
            PRODUKT + " TEXT, " +
            FIRMA + " TEXT, " +
            CENA_VT + " TEXT, " +
            CENA_NT + " TEXT, " +
            MESIC_PLAT + " TEXT, " +
            DAN + " TEXT, " +
            SAZBA + " TEXT, " +
            DIST_VT + " TEXT, " +
            DIST_NT + " TEXT, " +
            J0 + " TEXT, " +
            J1 + " TEXT, " +
            J2 + " TEXT, " +
            J3 + " TEXT, " +
            J4 + " TEXT, " +
            J5 + " TEXT, " +
            J6 + " TEXT, " +
            J7 + " TEXT, " +
            J8 + " TEXT, " +
            J9 + " TEXT, " +
            SYSTEM_SLUZBY + " TEXT, " +
            CINNOST + " TEXT, " +
            POZE1 + " TEXT, " +
            POZE2 + " TEXT, " +
            OZE + " TEXT, " +
            OTE + " TEXT, " +
            PLATNOST_OD + " TEXT, " +
            PLATNOST_DO + " TEXT, " +
            DPH + " TEXT, " +
            DISTRIBUCE + " TEXT, " +
            J10 + " TEXT, " +
            J11 + " TEXT, " +
            J12 + " TEXT, " +
            J13 + " TEXT, " +
            J14 + " TEXT, " +
            AUTOR + " TEXT, " +
            DATUM_VYTVORENI + " TEXT, " +
            EMAIL + " TEXT, " +
            REZERVA1 + " TEXT, " +
            REZERVA2 + " TEXT);";


    private Context context;

    public DBHelperPriceList(@Nullable Context context) {
        super(context, DATABASE_NAME_PRICE_LIST, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_PRICELIST_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
