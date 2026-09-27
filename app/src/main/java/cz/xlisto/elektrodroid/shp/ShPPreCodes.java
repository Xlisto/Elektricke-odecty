package cz.xlisto.elektrodroid.shp;


import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;

import cz.xlisto.elektrodroid.R;


/**
 * Třída pro ukládání a načítání kódů povelů PRE v SharedPreferences.
 */
public class ShPPreCodes extends ShP {

    public static final String KEY_PRE_CODES_JSON = "preCodesJson";
    public static final String KEY_LAST_UPDATE_TIME = "preCodesLastUpdate";


    public ShPPreCodes(Context context) {
        this.context = context;
    }


    /**
     * Uloží seznam kódů PRE jako JSON pole v SharedPreferences
     *
     * @param codes seznam kódů povelů
     */
    public void savePreCodes(ArrayList<String> codes) {
        if (codes == null || codes.isEmpty()) return;
        JSONArray jsonArray = new JSONArray();
        for (String code : codes) {
            jsonArray.put(code);
        }
        set(KEY_PRE_CODES_JSON, jsonArray.toString());
        set(KEY_LAST_UPDATE_TIME, System.currentTimeMillis());
    }


    /**
     * Načte uložený seznam kódů PRE. Pokud v SharedPreferences ještě žádný uložený není,
     * načte výchozí seznam z R.array.area_pre.
     *
     * @return ArrayList kódů povelů PRE
     */
    public ArrayList<String> getPreCodes() {
        String json = get(KEY_PRE_CODES_JSON, "");
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                ArrayList<String> codes = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    codes.add(jsonArray.getString(i));
                }
                if (!codes.isEmpty()) {
                    return codes;
                }
            } catch (JSONException ignored) {
            }
        }
        // Fallback k R.array.area_pre z array.xml
        String[] defaultArray = context.getResources().getStringArray(R.array.area_pre);
        ArrayList<String> defaultList = new ArrayList<>();
        Collections.addAll(defaultList, defaultArray);
        return defaultList;
    }


    /**
     * Vrátí, zda již existuje uložený dynamický seznam kódů PRE
     *
     * @return true pokud existuje uložený seznam, jinak false
     */
    public boolean hasSavedPreCodes() {
        String json = get(KEY_PRE_CODES_JSON, "");
        return json != null && !json.isEmpty();
    }

}
