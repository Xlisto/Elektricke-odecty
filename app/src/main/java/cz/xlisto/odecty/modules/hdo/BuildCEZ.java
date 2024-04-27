package cz.xlisto.odecty.modules.hdo;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.ownview.ViewHelper;


/**
 * Sestaví z JSON dat ArrayList<HdoModel>
 * Xlisto 11.01.2024 14:52
 */
public class BuildCEZ {
    private static final String TAG = "BuildCEZ";
    private static final String VALID_FROM = "VALID_FROM";
    private static final String VALID_TO = "VALID_TO";
    private static final String PLATNOST = "PLATNOST";
    private static final String CAS_ZAP = "CAS_ZAP_";
    private static final String CAS_VYP = "CAS_VYP_";
    private static final String NULL = "null";
    private static final String SAZBA = "SAZBA";
    private static final String INFO = "INFO";
    private static final String PO_PA = "Po - Pá";
    private static final String SO_NE = "So - Ne";


    public static ArrayList<HdoSiteFragment.HdoListContainer> build(String jsonData) {
        ArrayList<HdoSiteFragment.HdoListContainer> hdoListContainers = new ArrayList<>();
        ArrayList<HdoModel> hdoList = new ArrayList<>();
        try {
            JSONArray jsonRoot = new JSONArray(jsonData);
            for (int i = 0; i < jsonRoot.length(); i++) {
                JSONObject jsonHdo = jsonRoot.getJSONObject(i);

                String validFrom = ViewHelper.convertFormat(jsonHdo.getString(VALID_FROM));
                String validTo = ViewHelper.convertFormat(jsonHdo.getString(VALID_TO));

                String day = jsonHdo.getString(PLATNOST);
                String sazba = jsonHdo.getString(SAZBA);
                String info = jsonHdo.getString(INFO)
                        .replace("sazba", "")
                        .replace("+", "");
                String rele = sazba;
                if (info.length() > 0) {
                    rele += " + " + info;
                }
                for (int j = 1; j < 11; j++) {
                    String timeOn = jsonHdo.getString(CAS_ZAP + j);
                    String timeOff = jsonHdo.getString(CAS_VYP + j);
                    if (timeOn.equals(NULL) || timeOff.equals(NULL)) {
                        break;
                    }
                    HdoModel hdo = null;
                    if (day.equals(PO_PA)) {
                        hdo = new HdoModel(rele, validFrom, validTo, timeOn, timeOff, 1, 1, 1, 1, 1, 0, 0, DistributionArea.CEZ.toString());

                    }
                    if (day.equals(SO_NE)) {
                        hdo = new HdoModel(rele, validFrom, validTo, timeOn, timeOff, 0, 0, 0, 0, 0, 1, 1, DistributionArea.CEZ.toString());
                    }
                    if (hdo != null) {
                        hdoList.add(hdo);
                    }
                }
                HdoSiteFragment.HdoListContainer hdoListContainer = new HdoSiteFragment.HdoListContainer(hdoList, validFrom + " - " + validTo, HdoSiteFragment.HdoListContainer.Distribution.CEZ);
                hdoListContainers.add(hdoListContainer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hdoListContainers;
    }
}
