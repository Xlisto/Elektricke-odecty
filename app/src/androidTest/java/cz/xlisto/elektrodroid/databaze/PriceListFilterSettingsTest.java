package cz.xlisto.elektrodroid.databaze;


import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cz.xlisto.elektrodroid.shp.ShPFilter;


@RunWith(AndroidJUnit4.class)
public class PriceListFilterSettingsTest {

    @Test
    public void databaseCopyRetainsIndependentFiltersAndMigrationDoesNotOverwriteThem() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File original = File.createTempFile("price-list-filters", ".db", context.getCacheDir());
        File restored = File.createTempFile("restored-price-list-filters", ".db", context.getCacheDir());
        String general = ShPFilter.FilterType.GENERAL.name();
        String monthly = ShPFilter.FilterType.MONTHLY_READING.name();
        String invoice = ShPFilter.FilterType.INVOICE.name();

        try {
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(original, null)) {
                new DbHelper(context).onCreate(db);
                addPoint(db, "O100");
                addPoint(db, "O200");

                DataSettingsSource settings = new DataSettingsSource(context);
                settings.database = db;
                Map<String, String> legacyFilters = new HashMap<>();
                legacyFilters.put(general, new JSONObject(Collections.singletonMap(ShPFilter.SAZBA, "D25d;D26d")).toString());
                legacyFilters.put(monthly, new JSONObject(Collections.singletonMap(ShPFilter.COMPANY, "ČEZ")).toString());
                legacyFilters.put(invoice, new JSONObject(Collections.singletonMap(ShPFilter.DATE_START, "1.1.2024")).toString());
                settings.migrateLegacyPriceListFilters(legacyFilters);

                assertEquals("D25d;D26d", settings.loadPriceListFilter("100", general).getString(ShPFilter.SAZBA));
                assertEquals("D25d;D26d", settings.loadPriceListFilter("200", general).getString(ShPFilter.SAZBA));
                assertEquals("ČEZ", settings.loadPriceListFilter("100", monthly).getString(ShPFilter.COMPANY));
                assertEquals("1.1.2024", settings.loadPriceListFilter("100", invoice).getString(ShPFilter.DATE_START));

                settings.savePriceListFilter("100", general, Collections.singletonMap(ShPFilter.SAZBA, ShPFilter.DEFAULT));
                settings.savePriceListFilter("100", monthly, Collections.singletonMap(ShPFilter.AREA, "EG.D"));
                settings.migrateLegacyPriceListFilters(legacyFilters);
                addPoint(db, "O300");
                assertEquals(0, settings.loadPriceListFilter("300", general).length());
            }

            try (FileInputStream input = new FileInputStream(original);
                 FileOutputStream output = new FileOutputStream(restored)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }

            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(restored.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                DataSettingsSource settings = new DataSettingsSource(context);
                settings.database = db;
                assertEquals(ShPFilter.DEFAULT, settings.loadPriceListFilter("100", general).getString(ShPFilter.SAZBA));
                assertEquals("D25d;D26d", settings.loadPriceListFilter("200", general).getString(ShPFilter.SAZBA));
                assertEquals("ČEZ", settings.loadPriceListFilter("100", monthly).getString(ShPFilter.COMPANY));
                assertEquals("EG.D", settings.loadPriceListFilter("100", monthly).getString(ShPFilter.AREA));
                assertEquals("1.1.2024", settings.loadPriceListFilter("100", invoice).getString(ShPFilter.DATE_START));
                assertEquals(0, settings.loadPriceListFilter("300", monthly).length());
            }
        } finally {
            SQLiteDatabase.deleteDatabase(original);
            SQLiteDatabase.deleteDatabase(restored);
        }
    }


    private static void addPoint(SQLiteDatabase db, String table) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.ODBER_ID, table);
        db.insertOrThrow(DbHelper.TABLE_NAME_SUBSCRIPTION_POINT, null, values);
    }

}
