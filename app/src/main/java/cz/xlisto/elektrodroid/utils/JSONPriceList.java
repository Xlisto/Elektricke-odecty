package cz.xlisto.elektrodroid.utils;


import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListSumModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

/**
 * Xlisto 08.12.2023 21:20
 */
public class JSONPriceList {
    private static final String TAG = "JSONPriceList";


    /**
     * Načte obsah JSON souboru s ceníkem
     *
     * @param context kontext aplikace
     * @param f       soubor s ceníkem
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
        return sb.toString();
    }


    /**
     * Načte obsah JSON souboru s ceníkem a sestaví ArrayList s PriceListModel
     *
     * @param context kontext aplikace
     * @param f       soubor s ceníkem
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
            String author = jsonPriceList.getString("Autor");
            String createDate = jsonPriceList.getString("Datum vytvoreni");
            for (int i = 0; i < countPriceList; i++) {
                JSONArray jsArrayPriceList = new JSONArray(jsonPriceListFile.get("produkt" + i).toString());
                PriceListModel priceList = createPriceListObject(jsArrayPriceList, author, email, createDate);
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
    private PriceListModel createPriceListObject(JSONArray priceList, String author, String email, String createDate) throws JSONException {
        String rada = priceList.getString(0);
        String produkt = priceList.getString(1);
        String distribucniFirma = priceList.getString(2);
        double cenaVT = priceList.getDouble(3);
        double cenaNT = priceList.getDouble(4);
        double mesicPlat = priceList.getDouble(5);
        double dan = priceList.getDouble(6);
        String sazba = priceList.getString(7);
        double distVT = priceList.getDouble(8);
        double distNT = priceList.getDouble(9);
        double j0 = priceList.getDouble(10);
        double j1 = priceList.getDouble(11);
        double j2 = priceList.getDouble(12);
        double j3 = priceList.getDouble(13);
        double j4 = priceList.getDouble(14);
        double j5 = priceList.getDouble(15);
        double j6 = priceList.getDouble(16);
        double j7 = priceList.getDouble(17);
        double j8 = priceList.getDouble(18);
        double j9 = priceList.getDouble(19);
        double j10 = priceList.getDouble(20);
        double j11 = priceList.getDouble(21);
        double j12 = priceList.getDouble(22);
        double j13 = priceList.getDouble(23);
        double j14 = priceList.getDouble(24);
        double systemSluzby = priceList.getDouble(25);
        double cinnost = priceList.getDouble(26);
        double poze1 = priceList.getDouble(27);
        double poze2 = priceList.getDouble(28);
        double oze = priceList.getDouble(29);
        double ote = priceList.getDouble(30);
        long platnostOD = priceList.getLong(31);
        long platnostDO = priceList.getLong(32);
        double dph = priceList.getDouble(33);
        String distribuce = priceList.getString(34);

        return new PriceListModel(0L, rada, produkt, distribucniFirma, cenaVT,
                cenaNT, mesicPlat, dan, sazba, distVT,
                distNT, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10,
                j11, j12, j13, j14, systemSluzby,
                cinnost, poze1, poze2, oze, ote,
                platnostOD, platnostDO, dph, distribuce,
                author, ViewHelper.parseCalendarFromString(createDate).getTimeInMillis(), email);
    }


    /**
     * Sestaví JSON pro export do souboru
     *
     * @param priceListSumModel hlavička ceníku
     * @param priceListModels   ceníky
     * @return ceník JSON
     */
    public String buildJSON(PriceListSumModel priceListSumModel, ArrayList<PriceListModel> priceListModels) {
        Calendar cal = Calendar.getInstance();
        String createDateTime = ViewHelper.getSimpleDateTimeFormat().format(cal.getTimeInMillis());
        String email = "";
        String author = "";
        StringBuilder json = new StringBuilder("{\"Cenik\":{\"Produktova rada\":\"" + priceListSumModel.getRada()
                + "\",\"Distribucni firma\":\"" + priceListSumModel.getFirma()
                + "\",\"Platnost\":\"" + priceListSumModel.getDatum()
                + "\",\"Datum\":\"" + ViewHelper.getSimpleDateFormat().format(priceListSumModel.getDatum())
                + "\",\"Pocet\":\"" + priceListModels.size() + "\",\"Autor\":\""
                + author + "\",\"Datum vytvoreni\":\""
                + createDateTime + "\",\"Email\":\"" + email + "\"}");
        for (int i = 0; i < priceListModels.size(); i++) {
            String jedenCenikJSON = ",\"produkt" + i + "\":[\"" + priceListModels.get(i).getRada() + "\",\"" + priceListModels.get(i).getProdukt() + "\",\"" + priceListModels.get(i).getFirma() + "\",\"" + priceListModels.get(i).getCenaVT() + "\",\"" + priceListModels.get(i).getCenaNT() + "\",\"" + priceListModels.get(i).getMesicniPlat() + "\",\"" + priceListModels.get(i).getDan() + "\",\"" + priceListModels.get(i).getSazba() + "\",\""
                    + priceListModels.get(i).getDistVT() + "\",\"" + priceListModels.get(i).getDistNT() + "\",\"" + priceListModels.get(i).getJ0() + "\",\"" + priceListModels.get(i).getJ1() + "\",\"" + priceListModels.get(i).getJ2() + "\",\"" + priceListModels.get(i).getJ3() + "\",\"" + priceListModels.get(i).getJ4() + "\",\"" + priceListModels.get(i).getJ5() + "\",\"" + priceListModels.get(i).getJ6() + "\",\"" + priceListModels.get(i).getJ7() + "\",\"" + priceListModels.get(i).getJ8() + "\",\""
                    + priceListModels.get(i).getJ9() + "\",\"" + priceListModels.get(i).getJ10() + "\",\"" + priceListModels.get(i).getJ11() + "\",\"" + priceListModels.get(i).getJ12() + "\",\"" + priceListModels.get(i).getJ13() + "\",\"" + priceListModels.get(i).getJ14() + "\",\"" + priceListModels.get(i).getSystemSluzby() + "\",\"" + priceListModels.get(i).getCinnost() + "\",\"" + priceListModels.get(i).getPoze1() + "\",\"" + priceListModels.get(i).getPoze2() + "\",\"" + priceListModels.get(i).getOze() + "\",\"" + priceListModels.get(i).getOte() + "\",\"" + priceListModels.get(i).getPlatnostOD() + "\",\"" + priceListModels.get(i).getPlatnostDO() + "\",\""
                    + priceListModels.get(i).getDph() + "\",\"" + priceListModels.get(i).getDistribuce() + "\"]";
            json.append(jedenCenikJSON);
        }
        json.append("}");
        return json.toString();
    }
}
