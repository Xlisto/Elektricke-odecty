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
 * Třída pro čtení a parsování regulovaných cen elektřiny z interních JSON zdrojů aplikace (složka {@code res/raw}).
 * <p>
 * Zabezpečuje načítání cen distribučních sazeb (VT, NT), stálých měsíčních platů za jističe
 * a ostatních regulovaných poplatků (systémové služby, činnost OTE, POZE, daň z elektřiny a DPH)
 * pro zadané distribuční území, sazbu a časové období.
 */
public class ReadRawJSON {

    private final static String TAG = "ReadRawJSON";
    private final Context context;
    private int year;
    private Calendar startCalBtn;
    private Calendar endCalBtn;
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
     * Konstruktor třídy {@code ReadRawJSON}.
     *
     * @param context kontext aplikace, nebo {@code null} při spuštění v testovacím prostředí bez Android kontextu
     */
    public ReadRawJSON(Context context) {
        this.context = context;
        priceListModel = new PriceListModel();
    }


    /**
     * Načte regulované ceny z raw JSON zdrojů a naplní interní objekt {@link PriceListModel}.
     * <p>
     * Postup načtení:
     * <ul>
     *   <li>Určí rok podle počátečního data {@code startCal}.</li>
     *   <li>Uloží parametry pro filtrování (distribuční území a sazba).</li>
     *   <li>Načte obsahy JSON souborů (distribuce, platby za jističe a ostatní poplatky).</li>
     *   <li>Rozparsuje data a nastaví příslušné regulované ceny do {@link PriceListModel}.</li>
     * </ul>
     *
     * @param startCal  počáteční datum platnosti pro určení roku a filtrování periody
     * @param endCal    koncové datum platnosti pro filtrování periody (pokud je {@code null}, použije se {@code startCal})
     * @param distUzemi název distribuční oblasti (např. ČEZ, EG.D, PRE)
     * @param sazba     distribuční sazba (např. D 01d, D 25d)
     * @return naplněný objekt {@link PriceListModel} s regulovanými cenami
     */
    public PriceListModel read(Calendar startCal, Calendar endCal, String distUzemi, String sazba) {
        this.year = new DateUtil(startCal).getYear();
        this.startCalBtn = startCal;
        this.endCalBtn = (endCal != null) ? endCal : startCal;
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
     * Načte textový obsah raw zdroje.
     * <p>
     * Pokud je k dispozici kontext aplikace, načte zdroj pomocí {@link android.content.res.Resources#openRawResource(int)}.
     * V opačném případě (např. při unit testech na JVM) se pokusí načíst odpovídající soubor přímo z disku.
     *
     * @param resource ID zdroje v {@code R.raw}
     * @return načtený JSON řetězec, nebo prázdný řetězec v případě chyby
     */
    private String readRaw(int resource) {
        if (context != null) {
            try (InputStream is = context.getResources().openRawResource(resource)) {
                int size = is.available();
                byte[] buffer = new byte[size];
                int i = is.read(buffer);
                if (i == -1) {
                    throw new IOException("EOF reached while trying to read the whole file");
                }
                return new String(buffer, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "readRaw: " + e.getMessage());
            }
            return "";
        } else {
            return readRawFromFilesystem(resource);
        }
    }


    /**
     * Načte obsah JSON souboru z disku pro testovací prostředí bez Android kontextu.
     *
     * @param resource ID zdroje v {@code R.raw}
     * @return obsah souboru jako řetězec, nebo prázdný řetězec při neexistenci či chybě čtení
     */
    private String readRawFromFilesystem(int resource) {
        String fileName;
        if (resource == R.raw.distribuce) {
            fileName = "distribuce.json";
        } else if (resource == R.raw.ostatni) {
            fileName = "ostatni.json";
        } else if (resource == R.raw.jistice) {
            fileName = "jistice.json";
        } else {
            return "";
        }

        try {
            java.io.File file = new java.io.File("src/main/res/raw/" + fileName);
            java.io.File targetFile = file.exists() ? file : new java.io.File("app/src/main/res/raw/" + fileName);
            if (targetFile.exists()) {
                try (InputStream is = new java.io.FileInputStream(targetFile)) {
                    byte[] buffer = new byte[(int) targetFile.length()];
                    int readBytes = is.read(buffer);
                    if (readBytes > 0) {
                        return new String(buffer, 0, readBytes, StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readRawFromFilesystem: " + e.getMessage());
        }
        return "";
    }


    /**
     * Prohledá JSON pole pro aktuální rok a distribuční území a podle typu předá zpracování metodě
     * {@link #parseDistribuce(JSONObject)} nebo {@link #parseJistice(JSONObject)}.
     *
     * @param s   JSON řetězec s daty regulovaných cen
     * @param typ typ zpracovávaných dat (distribuce nebo jističe)
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
     * Vyhledá v JSON objektu distribuční území a konkrétní distribuční sazbu a přiřadí ceny distribuce VT a NT
     * do objektu {@link #priceListModel}.
     *
     * @param jsonObject JSON objekt obsahující distribuční data pro dané území
     * @throws JSONException při chybě parsování JSON struktury
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
     * Vyhledá v JSON objektu distribuční území a konkrétní distribuční sazbu a přiřadí měsíční platby za jističe
     * (J0 až J14) do objektu {@link #priceListModel}.
     *
     * @param jsonObject JSON objekt obsahující data plateb za jističe pro dané území
     * @throws JSONException při chybě parsování JSON struktury
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
     * Zpracuje sekci {@code OSTATNI} z JSON zdroje a přiřadí hodnoty ostatních regulovaných poplatků
     * (systémové služby, činnost OTE, POZE1, POZE2, DPH a daň) do objektu {@link #priceListModel}.
     * <p>
     * Pokud sekce obsahuje pole period platností, vyhledá periodu odpovídající zadanému časovému rozmezí.
     *
     * @param s JSON řetězec obsahující data ostatních poplatků
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
                priceListModel.setSystemServices(((JSONObject) otherPrice).getDouble(SYSTEM_SLUZBY));
                priceListModel.setActivity(((JSONObject) otherPrice).getDouble(CINNOST));
                priceListModel.setPoze1(((JSONObject) otherPrice).getDouble(POZE1));
                priceListModel.setPoze2(((JSONObject) otherPrice).getDouble(POZE2));
                priceListModel.setVat(((JSONObject) otherPrice).getDouble(DPH));
                priceListModel.setTax(((JSONObject) otherPrice).getDouble(DAN));
            } else if (otherPrice instanceof JSONArray otherPriceArr) {

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
                    endCalJson.set(Calendar.HOUR_OF_DAY, 23);
                    endCalJson.set(Calendar.MINUTE, 59);
                    endCalJson.set(Calendar.SECOND, 59);
                    endCalJson.set(Calendar.MILLISECOND, 999);

                    Calendar endCalToUse = (endCalBtn != null) ? endCalBtn : startCalBtn;
                    if (!startCalBtn.before(startCalJson) && !endCalToUse.after(endCalJson)) {
                        // přiřazení hodnot z periody (bezpečně pomocí opt*)
                        priceListModel.setSystemServices(otherPriceObj.optDouble(SYSTEM_SLUZBY, priceListModel.getSystemServices()));
                        priceListModel.setActivity(otherPriceObj.optDouble(CINNOST, priceListModel.getActivity()));
                        priceListModel.setPoze1(otherPriceObj.optDouble(POZE1, priceListModel.getPoze1()));
                        priceListModel.setPoze2(otherPriceObj.optDouble(POZE2, priceListModel.getPoze2()));
                        priceListModel.setVat(otherPriceObj.optDouble(DPH, priceListModel.getVat()));
                        priceListModel.setTax(otherPriceObj.optDouble(DAN, priceListModel.getTax()));
                        // nalezeno, lze ukončit smyčku
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseOther: " + e.getMessage());
        }
    }


    /**
     * Převede textový řetězec s datem ve formátu {@code D.M.} nebo {@code D.M} na pole s dnem a měsícem.
     *
     * @param date textový zápis data
     * @return pole o 2 prvcích: {@code [den, měsíc (0-based)]}
     */
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


    /**
     * Typ zpracovávaných regulovaných dat z JSON zdrojů.
     */
    enum Typ {
        /**
         * Ceny za měsíční platby jističů.
         */
        JISTIC,
        /** Ceny za distribuci elektřiny (VT, NT). */
        DISTRIBUCE
    }

}
