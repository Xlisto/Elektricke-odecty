package cz.xlisto.elektrodroid.modules.hdo;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Třída pro slučování a porovnávání HDO časů z distribuční sítě EG.D.
 * <p>
 * Xlisto 08.07.2023 21:31
 */
public class JsonHdoEgdMerge {

    private static final String TAG = "JsonHdoEgdMerge";


    /**
     * Porovná pole JSON objektů a vrátí seznam sloučených JSON objektů, které obsahují indexy stejných časů HDO.
     *
     * @param jsonObjects pole JSON objektů s časy HDO pro jednotlivé dny
     * @return seznam sloučených JSON objektů
     */
    public static ArrayList<JSONObject> init(JSONObject[] jsonObjects) {
        ArrayList<JSONObject> jsonMergeObjects = new ArrayList<>();
        ArrayList<ArrayList<Integer>> sameIndexesList = new ArrayList<>(); // seznam seznamů indexů stejných časů HDO
        ArrayList<Integer> excludedIndexesList = new ArrayList<>(); // vyloučené indexy, které se již nebudou prohledávat

        // první iterace pole JSON objektů
        for (JSONObject jsonObjectI : jsonObjects) {
            // druhá iterace pole JSON objektů pro porovnání každého prvku s každým
            ArrayList<Integer> sameIndexesList2 = new ArrayList<>();
            for (int j = 0; j < jsonObjects.length; j++) {
                // pokud se nalezne shoda, přidá se index do seznamu, který se bude přeskakovat
                if (compareJsonHdoTime(jsonObjectI, jsonObjects[j])) {
                    if (excludedIndexesList.contains(j)) {
                        break;
                    }
                    sameIndexesList2.add(j);
                    excludedIndexesList.add(j);
                }
            }
            sameIndexesList.add(sameIndexesList2);
        }

        // výpis seznamu seznamů indexů stejných časů HDO a sestavení JSON objektu HDO s upravenými dny platnosti
        for (ArrayList<Integer> sameIndexesList2 : sameIndexesList) {
            if (!sameIndexesList2.isEmpty()) {
                JSONObject jsonObject = new JSONObject();
                try {
                    ArrayList<Integer> denVTydnu = new ArrayList<>();
                    for (int index : sameIndexesList2) {
                        int x = jsonObjects[index].getInt("denVTydnu");
                        denVTydnu.add(x);
                    }
                    jsonObject.put("denVTydnu", new JSONArray(denVTydnu));
                    jsonObject.put("casy", jsonObjects[sameIndexesList2.get(0)].getJSONArray("casy"));
                    jsonMergeObjects.add(jsonObject);
                } catch (JSONException e) {
                    Log.e(TAG, "Chyba při sestavování sloučeného JSON objektu HDO", e);
                }
            }
        }
        return jsonMergeObjects;
    }


    /**
     * Porovná dva JSON objekty časů HDO a vrátí true, pokud jsou stejné.
     *
     * @param json1 HDO časy z EGD
     * @param json2 HDO časy z EGD
     * @return true, pokud jsou časy stejné
     */
    public static boolean compareJsonHdoTime(JSONObject json1, JSONObject json2) {
        int result = 0;
        int totalLength = 0;
        try {
            JSONArray time1 = json1.getJSONArray("casy");
            JSONArray time2 = json2.getJSONArray("casy");

            if (time1.length() != time2.length()) {
                return false;
            }

            totalLength = time1.length();

            for (int i = 0; i < time1.length(); i++) {
                JSONObject cas1 = time1.getJSONObject(i);
                for (int j = 0; j < time2.length(); j++) {
                    JSONObject cas2 = time2.getJSONObject(j);
                    if (cas1.getString("od").equals(cas2.getString("od")) && cas1.getString("do").equals(cas2.getString("do"))) {
                        // přičíst 1, pokud souhlasí čas. Když je celkový součet roven délce pole, pak jsou časy stejné
                        result++;
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Chyba při porovnávání HDO časů", e);
        }

        return result == totalLength;
    }

}
