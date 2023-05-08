package cz.xlisto.odecty.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.PriceListModel;

/**
 * Čtení JSON souborů (regulované ceny) z res/raw
 */
public class ReadRawJSON {
    private final static String TAG = "ReadRawJSON";
    private final Context context;
    private int year;
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
     * Prohledá objekt JSON s regulovanými cenami a podle typu (distribuční sazba, hodnota jistice) naplní model ceníku
     *
     * @param year      rok ceníku pro který jsou platný regulované ceny
     * @param distUzemi oblast distribučního území
     * @param sazba     distribuční sazba
     */
    public PriceListModel read(int year, String distUzemi, String sazba) {
        this.year = year;
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
            e.printStackTrace();
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
            e.printStackTrace();
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
     * Sestaví JSON ze stringu. Vyfiltruje podle roku platnosti a přiřadí hodnoty systémové služby, činnost operátora,
     * poze podle jističe a poze podle spotřeby, dph a dan
     *
     * @param s String obsahující JSON s ostatními cenami
     */
    private void parseOther(String s) {
        try {
            JSONArray jsonArray = new JSONArray(s);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject other = jsonArray.getJSONObject(i);
                if (other.getInt(ROK) == year) {
                    JSONObject otherPrice = other.getJSONObject(OSTATNI);
                    priceListModel.setSystemSluzby(otherPrice.getDouble(SYSTEM_SLUZBY));
                    priceListModel.setCinnost(otherPrice.getDouble(CINNOST));
                    priceListModel.setPoze1(otherPrice.getDouble(POZE1));
                    priceListModel.setPoze2(otherPrice.getDouble(POZE2));
                    priceListModel.setDph(otherPrice.getDouble(DPH));
                    priceListModel.setDan(otherPrice.getDouble(DAN));
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    enum Typ {
        JISTIC,
        DISTRIBUCE
    }
}
