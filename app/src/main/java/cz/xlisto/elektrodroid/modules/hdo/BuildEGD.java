package cz.xlisto.elektrodroid.modules.hdo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Sestaví z JSON dat ArrayList<HdoModel>
 * Xlisto 11.01.2024 17:27
 */
public class BuildEGD {
    private static final String TAG = "BuildEGD";
    private static final String OD = "od";
    private static final String DO = "do";
    private static final String SAZBY = "sazby";
    private static final String SAZBA = "sazba";
    private static final String DNY = "dny";
    private static final String DEN_V_TYDNU = "denVTydnu";
    private static final String CASY = "casy";
    private static final String DEN_1 = "1";
    private static final String DEN_2 = "2";
    private static final String DEN_3 = "3";
    private static final String DEN_4 = "4";
    private static final String DEN_5 = "5";
    private static final String DEN_6 = "6";
    private static final String DEN_7 = "7";


    public static ArrayList<HdoSiteFragment.HdoListContainer> build(String jsonData) {
        ArrayList<HdoSiteFragment.HdoListContainer> hdoListContainers = new ArrayList<>();
        //první průchod vrací objekt s kody - přeskakuji

        try {

            JSONArray jsonRoot = new JSONArray(jsonData);
            for (int i = 0; i < jsonRoot.length(); i++) {
                ArrayList<HdoModel> hdoList = new ArrayList<>();
                JSONObject jsonHdo = jsonRoot.getJSONObject(i);
                JSONObject jsonDateFrom = jsonHdo.getJSONObject(OD);
                JSONObject jsonDateTo = jsonHdo.getJSONObject(DO);
                String validDate = parseEgdDate(jsonDateFrom) + " - " + parseEgdDate(jsonDateTo);
                JSONArray jsonRates = jsonHdo.getJSONArray(SAZBY);
                for (int j = 0; j < jsonRates.length(); j++) {
                    JSONObject jsonRate = jsonRates.getJSONObject(j);
                    String rate = jsonRate.getString(SAZBA);
                    JSONArray jsonDays = jsonRate.getJSONArray(DNY);
                    JSONObject[] jsonDaysObj = new JSONObject[jsonDays.length()];
                    for (int k = 0; k < jsonDays.length(); k++) {
                        jsonDaysObj[k] = jsonDays.getJSONObject(k);
                    }
                    ArrayList<JSONObject> jsonObjectArrayList = JsonHdoEgdMerge.init(jsonDaysObj);
                    for (int l = 0; l < jsonObjectArrayList.size(); l++) {
                        JSONObject jsonObject = jsonObjectArrayList.get(l);
                        JSONArray day = jsonObject.getJSONArray(DEN_V_TYDNU);
                        JSONArray times = jsonObject.getJSONArray(CASY);
                        for (int m = 0; m < times.length(); m++) {
                            JSONObject time = times.getJSONObject(m);
                            String timeOn = time.getString(OD);
                            String timeOff = time.getString(DO);
                            HdoModel hdo = new HdoModel(rate, parseEgdDate(jsonDateFrom), parseEgdDate(jsonDateTo), timeOn, timeOff,
                                    containsDay(day, DEN_1),
                                    containsDay(day, DEN_2),
                                    containsDay(day, DEN_3),
                                    containsDay(day, DEN_4),
                                    containsDay(day, DEN_5),
                                    containsDay(day, DEN_6),
                                    containsDay(day, DEN_7),
                                    DistributionArea.EGD.toString());
                            hdoList.add(hdo);
                        }
                    }
                }
                HdoSiteFragment.HdoListContainer hdoListContainer = new HdoSiteFragment.HdoListContainer(hdoList, validDate, HdoSiteFragment.HdoListContainer.Distribution.EGD);
                hdoListContainers.add(hdoListContainer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return hdoListContainers;
    }


    /**
     * Sestaví text data z objektu json
     *
     * @param jsonObject objekt json s datem platnosti HDO
     * @return textový řetězec s datem platnosti HDO, např. 1.1.2020 - 31.12.2020, Datumy platnosti jako 9999 odstraní
     */
    private static String parseEgdDate(JSONObject jsonObject) {
        String date = "";
        try {
            date = jsonObject.get("den") + "." + jsonObject.get("mesic") + "." + jsonObject.get("rok");
            date = date.replace(".9999", "").replace("1900", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return date;
    }


    private static int containsDay(JSONArray jsonArray, String day) {
        if (jsonArray.toString().contains(day))
            return 1;
        else
            return 0;
    }
}
