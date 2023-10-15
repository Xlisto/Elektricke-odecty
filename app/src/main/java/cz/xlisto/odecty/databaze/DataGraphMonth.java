package cz.xlisto.odecty.databaze;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import cz.xlisto.odecty.format.SimpleDateFormatHelper;
import cz.xlisto.odecty.models.ConsuptionModel;
import cz.xlisto.odecty.models.MonthlyConsuptionModel;
import cz.xlisto.odecty.models.YearConsuptionModel;
import cz.xlisto.odecty.modules.graphmonth.ConsuptionContainer;
import cz.xlisto.odecty.utils.SubscriptionPoint;

/**
 * Xlisto 21.08.2023 3:35
 */
public class DataGraphMonth extends DataSource {
    private static final String TAG = "DataGraphMonth";
    ArrayList<ConsuptionModel> monthlyConsuptions = new ArrayList<>();
    private final ArrayList<ConsuptionModel> yearConsuptions = new ArrayList<>();
    private final ArrayList<ArrayList<ConsuptionModel>> monthsConsuptionsArray = new ArrayList<>();


    public DataGraphMonth(Context context) {
        super.context = context;
        dbHelper = new DbHelper(context);
    }

    /**
     * Z databáze s měsíčními odečty sestaví ArrayList s celkovou měsíční spotřebou
     *
     * @return ArrayList sestavený z objektů SpotrebaFaktura, který reprezentuje celkovou měsíční spotřebu
     */
    public ConsuptionContainer loadConsuptions() {
        String table = Objects.requireNonNull(SubscriptionPoint.load(context)).getTableO();
        if (table == null) return null;
        String orderBy = DbHelper.DATUM + " ASC, " + DbHelper.PRVNI_ODECET + " ASC";
        boolean add = false;
        int firstReader;
        open();
        //Cursor cursor = database.rawQuery("SELECT _ID, vt, nt, datum, prvni_odecet " + "FROM '" + table + "' ORDER BY datum", null);
        Cursor cursor = database.query(table,
                new String[]{DbHelper.COLUMN_ID, DbHelper.VT, DbHelper.NT, DbHelper.DATUM, DbHelper.PRVNI_ODECET},
                null,
                null,
                null,
                null,
                orderBy);
        //Log.w(TAG, "loadConsuption: " + cursor.getCount());

        monthlyConsuptions.clear();

        for (int i = 0; i < cursor.getCount(); i++) {
            if (i == 0) {
                cursor.moveToPosition(i);
            } else {
                cursor.moveToPosition(i - 1);
                double previousVT = cursor.getDouble(1);
                double previousNT = cursor.getDouble(2);
                long previousDate = removeMonth(cursor.getLong(3));
                cursor.moveToPosition(i);
                double actualVT = cursor.getDouble(1);
                double actualNT = cursor.getDouble(2);
                long actualDate = removeMonth(cursor.getLong(3));
                firstReader = cursor.getInt(4);
                if (firstReader == 1) continue;


                //přidat nový záznam do měsíční spotřeby
                if (add) {
                    monthlyConsuptions.get(monthlyConsuptions.size() - 1).addConsuptionVT(actualVT - previousVT);
                    monthlyConsuptions.get(monthlyConsuptions.size() - 1).addConsuptionNT(actualNT - previousNT);
                    monthlyConsuptions.get(monthlyConsuptions.size() - 1).setDates(actualDate);
                    monthlyConsuptions.get(monthlyConsuptions.size() - 1).setFirstReader(firstReader);
                } else {
                    //proč potřebuji předchozí datum v modelu spotřeby?
                    monthlyConsuptions.add(new MonthlyConsuptionModel(actualVT, actualNT, previousVT, previousNT, actualDate, firstReader));
                }

                //sčítat záznam odečtu je potřeba o jeden pozadu, proto je kontrola po načtení a až při dalším cyklu se uplatňuje
                add = SimpleDateFormatHelper.month.format(actualDate).equals(SimpleDateFormatHelper.month.format(previousDate));
            }
        }
        cursor.close();
        close();
        buildConsuptions();
        return new ConsuptionContainer(monthlyConsuptions, yearConsuptions, monthsConsuptionsArray);
    }


    /**
     * Sečte měsíční spotřebu do spotřeby roční a měsíční porovnání
     */
    public void buildConsuptions() {
        yearConsuptions.clear();

        //seznam měsíčních spotřeb
        //inicializace
        for (int i = 0; i < 12; i++) {
            monthsConsuptionsArray.add(new ArrayList<>());
        }

        for (int i = 0; i < monthlyConsuptions.size(); i++) {
            //sestavení měsíční spotřeby
            int month = Integer.parseInt(String.valueOf(monthlyConsuptions.get(i).getDateMonth()));
            monthsConsuptionsArray.get(month - 1).add(monthlyConsuptions.get(i));

            //sestavení roční spotřeby
            int year = monthlyConsuptions.get(i).getYearAsInt();
            if (i == 0) {
                addNewItemYearConsuption(monthlyConsuptions.get(i), yearConsuptions);
            } else {
                int lastYear = monthlyConsuptions.get(i - 1).getYearAsInt();
                if (year == lastYear) {
                    updateItemYearConsuption(yearConsuptions.get(yearConsuptions.size() - 1), monthlyConsuptions.get(i));
                } else {
                    addNewItemYearConsuption(monthlyConsuptions.get(i), yearConsuptions);
                }
            }
        }
    }


    /**
     * Přidá nový záznam do roční spotřeby
     *
     * @param monthlyConsuption měsíční spotřeba
     * @param yearConsuptions   roční spotřeba
     */
    private void addNewItemYearConsuption(ConsuptionModel monthlyConsuption, ArrayList<ConsuptionModel> yearConsuptions) {
        yearConsuptions.add(new YearConsuptionModel(monthlyConsuption.getConsuptionVT(),
                monthlyConsuption.getConsuptionNT(),
                monthlyConsuption.getYearAsInt()));
    }


    /**
     * Aktualizuje záznam v roční spotřebě
     *
     * @param yearConsuption    roční spotřeba
     * @param monthlyConsuption měsíční spotřeba
     */
    private void updateItemYearConsuption(ConsuptionModel yearConsuption, ConsuptionModel monthlyConsuption) {
        yearConsuption.addConsuptionVT(monthlyConsuption.getConsuptionVT());
        yearConsuption.addConsuptionNT(monthlyConsuption.getConsuptionNT());
    }


    /**
     * Odečte jeden měsíc od data
     *
     * @param date datum
     */
    private long removeMonth(long date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        cal.add(Calendar.MONTH, -1);
        date = cal.getTimeInMillis();
        return date;
    }
}
