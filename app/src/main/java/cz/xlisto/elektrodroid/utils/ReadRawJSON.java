package cz.xlisto.elektrodroid.utils;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Objects;
import java.util.stream.IntStream;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Čtení JSON souborů (regulované ceny) z res/raw
 */
public class ReadRawJSON {

    private final static String TAG = "ReadRawJSON";
    private final Context context;
    private int year;
    private Calendar startCalBtn, endCalBtn;
    private String distUzemi, sazba;
    private final PriceListModel priceListModel;
    private static final String ROK = "rok";
    private final static String DIST_UZEMI = "dist_uzemi";
    private static final String DISTRIBUCE = "distribuce";
    private static final String JISTICE = "jistice";
    private static final String SAZBA = "sazba";
    private static final String OSTATNI = "ostatni";
    private static final String VT = "vt";
    private final static String NT = "nt";
    private final static String J0 = "J0";
    private static final String J1 = "J1";
    private final static String J2 = "J2";
    private final static String J3 = "J3";
    private final static String J4 = "J4";
    private final static String J5 = "J5";
    private final static String J6 = "J6";
    private final static String J7 = "J7";
    private final static String J8 = "J8";
    private final static String J9 = "J9";
    private final static String J10 = "J10";
    private final static String J11 = "J11";
    private final static String J12 = "J12";
    private final static String J13 = "J13";
    private final static String J14 = "J14";
    private final static String DPH = "dph";
    private final static String DAN = "dan";
    private final static String CINNOST = "cinnost";
    private final static String SYSTEM_SLUZBY = "system_sluzby";
    private final static String POZE1 = "POZE1";
    private final static String POZE2 = "POZE2";


    /**
     * Konstruktor
     *
     * @param context kontext aplikace
     */
    public ReadRawJSON(Context context) {
        this.context = context;
        priceListModel = new PriceListModel();
    }


    /**
     * Načte regulované ceny z raw JSON zdrojů a naplní interní model `priceListModel`.
     * <p>
     * Postup:
     * - nastaví rok podle `startCal` (pomocí `DateUtil`),
     * - uloží parametrické filtry `distUzemi` a `sazba` do polí instance,
     * - načte obsahy souborů `res/raw/distribuce`, `res/raw/ostatni`, `res/raw/jistice`,
     * - zavolá `parse` a `parseOther` pro naplnění `priceListModel`.
     * <p>
     * Vedlejší efekty:
     * - mění stav instance (`this.year`, `this.distUzemi`, `this.sazba`),
     * - aktualizuje a vrací interní `priceListModel`.
     * <p>
     * Poznámka:
     * - parametr `endCal` není v současné implementaci použitý;
     * pokud se má provádět filtrování podle období, je potřeba ho zpracovat.
     *
     * @param startCal  počáteční datum platnosti ceníku (použito pro určení roku)
     * @param endCal    koncové datum platnosti (aktuálně nevyužito)
     * @param distUzemi distribuční oblast pro filtrování
     * @param sazba     distribuční sazba pro filtrování
     * @return naplněný objekt `PriceListModel`
     */
    public PriceListModel read(Calendar startCal, Calendar endCal, String distUzemi, String sazba) {
        this.year = new DateUtil(startCal).getYear();
        this.startCalBtn = startCal;
        this.endCalBtn = endCal;
        this.distUzemi = distUzemi;
        this.sazba = sazba;
        String distribuce = readRaw(R.raw.distribuce);
        String ostatni = readRaw(R.raw.ostatni);
        String jistice = readRaw(R.raw.jistice);
        parse(distribuce, Typ.DISTRIBUCE);
        parseOther(ostatni);
        parse(jistice, Typ.JISTIC);
        return priceListModel;
    }


    /**
     * Načte zdrojový soubor podle resource
     *
     * @param resource int
     * @return String načteného ceníku
     */
    private String readRaw(int resource) {
        String json = "";
        InputStream is = context.getResources().openRawResource(resource);
        int size;
        try {
            size = is.available();
            byte[] buffer = new byte[size];
            int i = is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
            if (i == -1) {
                throw new IOException("EOF reached while trying to read the whole file");
            }
            return json;
        } catch (IOException e) {
            Log.e(TAG, "readRaw: " + e.getMessage());
        }
        return json;
    }


    /**
     * Prohledá objekt JSON a podle typu (distribuční oblast, vt, nt nebo ceny jističe) zavolá
     * parseDistribuce nebo parseJistic
     *
     * @param s   String obsahující JSON s regulovanými cenami
     * @param typ Typ (distribuce, jistice)
     */
    private void parse(String s, Typ typ) {
        try {
            JSONArray jsonArray = new JSONArray(s);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject distribuce = jsonArray.getJSONObject(i);

                if (distribuce.getInt(ROK) == year) {
                    JSONArray distUzemiArray = distribuce.getJSONArray(DIST_UZEMI);

                    for (int j = 0; j < distUzemiArray.length(); j++) {
                        JSONObject distUzemiObject = distUzemiArray.getJSONObject(j);
                        if (typ.equals(Typ.DISTRIBUCE))
                            parseDistribuce(distUzemiObject);
                        if (typ.equals(Typ.JISTIC))
                            parseJistice(distUzemiObject);
                    }
                    break;
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "parse: " + e.getMessage());
        }
    }


    /**
     * Přečte JSON a přiřadí distribuční cenu vt a nt do objektu ceníku
     *
     * @param jsonObject JSONObject
     * @throws JSONException vyjímka JSON
     */
    private void parseDistribuce(JSONObject jsonObject) throws JSONException {

        if (jsonObject.getString(DIST_UZEMI).equals(distUzemi)) {
            JSONArray sazbyArray = jsonObject.getJSONArray(DISTRIBUCE);

            for (int k = 0; k < sazbyArray.length(); k++) {
                JSONObject sazbaObject = sazbyArray.getJSONObject(k);
                if (sazbaObject.getString(SAZBA).equals(sazba)) {
                    priceListModel.setDistVT(sazbaObject.getDouble(VT));
                    priceListModel.setDistNT(sazbaObject.getDouble(NT));
                    break;
                }
            }
        }
    }


    /**
     * Přečte JSON a přiřadí distribuční ceny jističů do objektu ceníku
     *
     * @param jsonObject JSONObject
     * @throws JSONException vyjímka JSON
     */
    private void parseJistice(JSONObject jsonObject) throws JSONException {
        if (jsonObject.getString(DIST_UZEMI).equals(distUzemi)) {
            JSONArray sazbyArray = jsonObject.getJSONArray(JISTICE);

            for (int k = 0; k < sazbyArray.length(); k++) {
                JSONObject sazbaObject = sazbyArray.getJSONObject(k);
                if (sazbaObject.getString(SAZBA).equals(sazba)) {
                    priceListModel.setJ0(sazbaObject.getDouble(J0));
                    priceListModel.setJ1(sazbaObject.getDouble(J1));
                    priceListModel.setJ2(sazbaObject.getDouble(J2));
                    priceListModel.setJ3(sazbaObject.getDouble(J3));
                    priceListModel.setJ4(sazbaObject.getDouble(J4));
                    priceListModel.setJ5(sazbaObject.getDouble(J5));
                    priceListModel.setJ6(sazbaObject.getDouble(J6));
                    priceListModel.setJ7(sazbaObject.getDouble(J7));
                    priceListModel.setJ8(sazbaObject.getDouble(J8));
                    priceListModel.setJ9(sazbaObject.getDouble(J9));
                    priceListModel.setJ10(sazbaObject.getDouble(J10));
                    priceListModel.setJ11(sazbaObject.getDouble(J11));
                    priceListModel.setJ12(sazbaObject.getDouble(J12));
                    priceListModel.setJ13(sazbaObject.getDouble(J13));
                    priceListModel.setJ14(sazbaObject.getDouble(J14));
                    break;
                }
            }
        }
    }


    /**
     * Zpracuje obsah pole `OSTATNI` z JSONu a přiřadí hodnoty do `priceListModel`.
     * <p>
     * Chování:
     * - Najde objekt pro aktuální `year`.
     * - Pokud je `OSTATNI` `JSONObject`, přiřadí jeho hodnoty přímo (SYSTEM_SLUZBY, CINNOST, POZE1, POZE2, DPH, DAN).
     * - Pokud je `OSTATNI` `JSONArray`, očekává položky s poli `od` a `do` (formáty jako `1.1.`, `1.1`, `01.01`).
     * Datumy se parsují pomocí `parseDate` a převedou na `Calendar` (rok = `this.year`).
     * - Pro každou periodu se porovná inkluzivně: `startCalBtn >= startCalendarJson` a `endCalBtn <= endCalendarJson`.
     * Pokud podmínka platí, použijí se hodnoty z té periody (s fallbackem pomocí `optDouble`) a zpracování končí.
     * - Pokud žádná položka nevyhovuje nebo JSON neobsahuje `OSTATNI`, `priceListModel` zůstane beze změny.
     * <p>
     * Vedlejší efekty: aktualizuje interní `priceListModel`.
     *
     * @param s JSON řetězec obsahující pole `OSTATNI`
     */
    private void parseOther(String s) {
        try {
            JSONArray jsonArray = new JSONArray(s);
            // Vyfiltruje podle roku platnosti
            JSONObject otherPriceForSelectYearObj = IntStream.range(0, jsonArray.length())
                    .mapToObj(jsonArray::optJSONObject)
                    .filter(Objects::nonNull)
                    .filter(o -> o.optInt(ROK, -1) == year)
                    .findFirst()
                    .orElse(null);
            if (otherPriceForSelectYearObj == null) return;

            Object otherPrice = otherPriceForSelectYearObj.opt(OSTATNI);
            if (otherPrice == null) return;

            if (otherPrice instanceof JSONObject) {
                priceListModel.setSystemSluzby(((JSONObject) otherPrice).getDouble(SYSTEM_SLUZBY));
                priceListModel.setCinnost(((JSONObject) otherPrice).getDouble(CINNOST));
                priceListModel.setPoze1(((JSONObject) otherPrice).getDouble(POZE1));
                priceListModel.setPoze2(((JSONObject) otherPrice).getDouble(POZE2));
                priceListModel.setDph(((JSONObject) otherPrice).getDouble(DPH));
                priceListModel.setDan(((JSONObject) otherPrice).getDouble(DAN));
            } else if (otherPrice instanceof JSONArray) {
                JSONArray otherPriceArr = (JSONArray) otherPrice;

                for (int i = 0; i < otherPriceArr.length(); i++) {
                    JSONObject otherPriceObj = otherPriceArr.optJSONObject(i);
                    String startDate = otherPriceObj.optString("od");
                    String endDate = otherPriceObj.optString("do");

                    Calendar startCalJson = Calendar.getInstance();
                    Calendar endCalJson = Calendar.getInstance();
                    startCalJson.clear();
                    endCalJson.clear();
                    startCalJson.set(Calendar.DAY_OF_MONTH, parseDate(startDate)[0]);
                    endCalJson.set(Calendar.DAY_OF_MONTH, parseDate(endDate)[0]);
                    startCalJson.set(Calendar.MONTH, parseDate(startDate)[1]);
                    endCalJson.set(Calendar.MONTH, parseDate(endDate)[1]);
                    startCalJson.set(Calendar.YEAR, year);
                    endCalJson.set(Calendar.YEAR, year);
                    if (!startCalBtn.before(startCalJson) && !endCalBtn.after(endCalJson)) {
                        // přiřazení hodnot z periody (bezpečně pomocí opt*)
                        priceListModel.setSystemSluzby(otherPriceObj.optDouble(SYSTEM_SLUZBY, priceListModel.getSystemSluzby()));
                        priceListModel.setCinnost(otherPriceObj.optDouble(CINNOST, priceListModel.getCinnost()));
                        priceListModel.setPoze1(otherPriceObj.optDouble(POZE1, priceListModel.getPoze1()));
                        priceListModel.setPoze2(otherPriceObj.optDouble(POZE2, priceListModel.getPoze2()));
                        priceListModel.setDph(otherPriceObj.optDouble(DPH, priceListModel.getDph()));
                        priceListModel.setDan(otherPriceObj.optDouble(DAN, priceListModel.getDan()));
                        // nalezeno, lze ukončit smyčku
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseOther: " + e.getMessage());
        }
    }


    private int[] parseDate(String date) {
        int day = 1;
        int month = 0;
        String cleanedString = date.trim();
        if (cleanedString.endsWith(".")) {
            cleanedString = cleanedString.substring(0, cleanedString.length() - 1).trim();
        }
        String[] parts = cleanedString.split("\\.");
        try {
            if (parts.length >= 1 && !parts[0].isEmpty()) {
                day = Integer.parseInt(parts[0]);
            }
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                month = Integer.parseInt(parts[1]) - 1; // přizpůsobit 0-based
            }
        } catch (NumberFormatException ignored) {
            // fallback na výchozí day=1, month=0
        }
        return new int[]{day, month};
    }


    enum Typ {
        JISTIC,
        DISTRIBUCE
    }

}
