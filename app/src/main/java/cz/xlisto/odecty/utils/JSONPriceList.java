package cz.xlisto.odecty.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import androidx.documentfile.provider.DocumentFile;
import cz.xlisto.odecty.databaze.DataPriceListSource;
import cz.xlisto.odecty.models.PriceListModel;
import cz.xlisto.odecty.ownview.ViewHelper;

/**
 * Xlisto 08.12.2023 21:20
 */
public class JSONPriceList {
    private static final String TAG = "JSONPriceList";


    /**
     * Načte obsah JSON souboru s ceníkem
     *
     * @param context
     * @param f
     */
    public String loadJSONPriceListFile(Context context, DocumentFile f) {
        InputStreamReader reader;
        try {
            reader = new InputStreamReader(context.getContentResolver().openInputStream(f.getUri()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line;
            try {
                if ((line = br.readLine()) == null) break;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            sb.append(line);
        }
        String result = sb.toString();
        importPriceList(context, result);
        return result;
    }


    /**
     * Načte obsah JSON souboru s ceníkem a sestaví ArrayList s PriceListModel
     * @param context kontext aplikace
     * @param f soubor s ceníkem
     * @return ArrayList s PriceListModel
     */
    public ArrayList<PriceListModel> getPriceList(Context context, DocumentFile f) {
        ArrayList<PriceListModel> priceLists = new ArrayList<>();
        JSONObject jsonPriceListFile;
        try {
            jsonPriceListFile = new JSONObject(loadJSONPriceListFile(context, f)); //celý soubor
            JSONObject jsonPriceList = jsonPriceListFile.getJSONObject("Cenik"); //Hlavička ceníku
            int countPriceList = jsonPriceList.getInt("Pocet");
            String email = jsonPriceList.getString("Email");
            String autor = jsonPriceList.getString("Autor");
            String createDate = jsonPriceList.getString("Datum vytvoreni");
            for (int i = 0; i < countPriceList; i++) {
                JSONArray jsArrayPriceList = new JSONArray(jsonPriceListFile.get("produkt" + i).toString());
                PriceListModel priceList = createPriceListObject(jsArrayPriceList, autor, email, createDate);
                priceLists.add(priceList);
            }
            return priceLists;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Vytvoří objekt ceníku
     */
    private PriceListModel createPriceListObject(JSONArray pricelist, String autor, String email, String createDate) throws JSONException {
        String rada = pricelist.getString(0);
        String produkt = pricelist.getString(1);
        String distribucniFirma = pricelist.getString(2);
        double cenaVT = pricelist.getDouble(3);
        double cenaNT = pricelist.getDouble(4);
        double mesicPlat = pricelist.getDouble(5);
        double dan = pricelist.getDouble(6);
        String sazba = pricelist.getString(7);
        double distVT = pricelist.getDouble(8);
        double distNT = pricelist.getDouble(9);
        double j0 = pricelist.getDouble(10);
        double j1 = pricelist.getDouble(11);
        double j2 = pricelist.getDouble(12);
        double j3 = pricelist.getDouble(13);
        double j4 = pricelist.getDouble(14);
        double j5 = pricelist.getDouble(15);
        double j6 = pricelist.getDouble(16);
        double j7 = pricelist.getDouble(17);
        double j8 = pricelist.getDouble(18);
        double j9 = pricelist.getDouble(19);
        double j10 = pricelist.getDouble(20);
        double j11 = pricelist.getDouble(21);
        double j12 = pricelist.getDouble(22);
        double j13 = pricelist.getDouble(23);
        double j14 = pricelist.getDouble(24);
        double systemSluzby = pricelist.getDouble(25);
        double cinnost = pricelist.getDouble(26);
        double poze1 = pricelist.getDouble(27);
        double poze2 = pricelist.getDouble(28);
        double oze = pricelist.getDouble(29);
        double ote = pricelist.getDouble(30);
        long platnostOD = pricelist.getLong(31);
        long platnostDO = pricelist.getLong(32);
        double dph = pricelist.getDouble(33);
        String distribuce = pricelist.getString(34);

        return new PriceListModel(0L, rada, produkt, distribucniFirma, cenaVT,
                cenaNT, mesicPlat, dan, sazba, distVT,
                distNT, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10,
                j11, j12, j13, j14, systemSluzby,
                cinnost, poze1, poze2, oze, ote,
                platnostOD, platnostDO, dph, distribuce,
                autor, ViewHelper.parseCalendarFromString(createDate).getTimeInMillis(), email);
    }


    private void importPriceList(Context context, String json) {
        JSONObject jsonObject;
        String produktovaRada, email, autor, distribucniFirma, datumVytvoreniStr, distribucniUzemi;
        int pocetCenikuVSouboru;
        String platnostOd;
        try {
            jsonObject = new JSONObject(json);//celý řetězec, který se použije na JSON data
            JSONObject cenikJS = jsonObject.getJSONObject("Cenik");
            //naplnění proměnných z hlavičky json
            produktovaRada = cenikJS.getString("Produktova rada");
            email = cenikJS.getString("Email");
            autor = cenikJS.getString("Autor");
            distribucniFirma = cenikJS.getString("Distribucni firma");
            pocetCenikuVSouboru = cenikJS.getInt("Pocet");
            platnostOd = cenikJS.getString("Platnost");
            datumVytvoreniStr = cenikJS.getString("Datum vytvoreni");
            JSONArray produkt0 = new JSONArray(jsonObject.get("produkt0").toString());
            distribucniUzemi = produkt0.get(34).toString();

            DataPriceListSource dataPriceListSource = new DataPriceListSource(context);
            dataPriceListSource.open();
            //int pocetCenikuVDatabazi = dataPriceListSource.countPriceListItems(produktovaRada, distribucniFirma, platnostOd, distribucniUzemi);
            dataPriceListSource.close();

            addPriceListsToDatabase(context, jsonObject, pocetCenikuVSouboru, autor, datumVytvoreniStr, email);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * Přidá ceníky do databáze
     *
     * @param context
     * @param priceLists
     * @param pocetCenikuVSouboru
     */
    private void addPriceListsToDatabase(Context context, JSONObject priceLists, int pocetCenikuVSouboru, String autor, String datumVytvoreniStr, String email) {
        for (int i = 0; i < pocetCenikuVSouboru; i++) {//cyklus pro všech 9-10 ceníků
            String rada, distribucniFirma, produkt, sazba, distribuce;
            double cenaVT, cenaNT, mesicPlat, dan, distVT, distNT, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12, j13, j14, systemSluzby, cinnost, poze1, poze2, oze, ote, dph;
            long platnostDO, platnostOD;
            try {
                JSONArray cenikJSA = new JSONArray(priceLists.get("produkt" + i).toString());
                rada = cenikJSA.getString(0);
                produkt = cenikJSA.getString(1);
                distribucniFirma = cenikJSA.getString(2);
                cenaVT = cenikJSA.getDouble(3);
                cenaNT = cenikJSA.getDouble(4);
                mesicPlat = cenikJSA.getDouble(5);
                dan = cenikJSA.getDouble(6);
                sazba = cenikJSA.getString(7);
                distVT = cenikJSA.getDouble(8);
                distNT = cenikJSA.getDouble(9);
                j0 = cenikJSA.getDouble(10);
                j1 = cenikJSA.getDouble(11);
                j2 = cenikJSA.getDouble(12);
                j3 = cenikJSA.getDouble(13);
                j4 = cenikJSA.getDouble(14);
                j5 = cenikJSA.getDouble(15);
                j6 = cenikJSA.getDouble(16);
                j7 = cenikJSA.getDouble(17);
                j8 = cenikJSA.getDouble(18);
                j9 = cenikJSA.getDouble(19);
                j10 = cenikJSA.getDouble(20);
                j11 = cenikJSA.getDouble(21);
                j12 = cenikJSA.getDouble(22);
                j13 = cenikJSA.getDouble(23);
                j14 = cenikJSA.getDouble(24);
                systemSluzby = cenikJSA.getDouble(25);
                cinnost = cenikJSA.getDouble(26);
                poze1 = cenikJSA.getDouble(27);
                poze2 = cenikJSA.getDouble(28);
                oze = cenikJSA.getDouble(29);
                ote = cenikJSA.getDouble(30);
                platnostOD = cenikJSA.getLong(31);
                platnostDO = cenikJSA.getLong(32);
                dph = cenikJSA.getDouble(33);
                distribuce = cenikJSA.getString(34);

                PriceListModel priceList = new PriceListModel(0L, rada, produkt, distribucniFirma, cenaVT,
                        cenaNT, mesicPlat, dan, sazba, distVT,
                        distNT, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10,
                        j11, j12, j13, j14, systemSluzby,
                        cinnost, poze1, poze2, oze, ote,
                        platnostOD, platnostDO, dph, distribuce,
                        autor, ViewHelper.parseCalendarFromString(datumVytvoreniStr).getTimeInMillis(), email);


                //new Databaze(getActivity()).ulozCenik(cenik, datumVytvoreniStr);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
