package cz.xlisto.odecty.modules.hdo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.xlisto.odecty.models.HdoModel;


/**
 * Sestaví z JSON dat ArrayList<HdoModel>
 * Xlisto 11.01.2024 14:35
 */
public class BuildPRE {
    private static final String TAG = "BuildPRE";
    private static final String KOD_POVELU = "kodPovelu";
    private static final String PLATNOST = "platnost";
    private static final String CAS_ZAP = "casZap";
    private static final String CAS_VYP = "casVyp";
    private static final String EMPTY = "";


    /**
     * Sestaví seznam HDO pro PRE
     *
     * @param jsonData objekt json s daty HDO
     * @return seznam HDO
     */
    public static ArrayList<HdoSiteFragment.HdoListContainer> build(String jsonData) {
        ArrayList<HdoSiteFragment.HdoListContainer> hdoListContainers = new ArrayList<>();
        ArrayList<HdoModel> hdoList = new ArrayList<>();
        try {
            JSONArray jsonRoot = new JSONArray(jsonData);
            String validityDate = null;
            //PRE
            for (int i = 0; i < jsonRoot.length(); i++) {
                JSONObject jsonObject = jsonRoot.getJSONObject(i);
                String rele = jsonObject.getString(KOD_POVELU);
                String date = jsonObject.getString(PLATNOST);

                if (i == 0)
                    validityDate = date;
                if (i == jsonRoot.length() - 1)
                    validityDate += " - " + date;

                for (int j = 0; j < 10; j++) {
                    String timeOn = jsonObject.getString(CAS_ZAP + j);
                    String timeOff = jsonObject.getString(CAS_VYP + j);
                    if (timeOn.equals(EMPTY) || timeOff.equals(EMPTY)) {
                        break;
                    }
                    HdoModel hdo = new HdoModel(rele, date, "", timeOn, timeOff, 0, 0, 0, 0, 0, 0, 0, DistributionArea.PRE.toString());
                    hdoList.add(hdo);
                }
            }
            HdoSiteFragment.HdoListContainer hdoListContainer = new HdoSiteFragment.HdoListContainer(hdoList, validityDate, HdoSiteFragment.HdoListContainer.Distribution.PRE);
            hdoListContainers.add(hdoListContainer);
            //setTvValidityDate(validityDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hdoListContainers;
    }
}
