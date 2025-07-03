package cz.xlisto.elektrodroid.modules.hdo;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.models.HdoModel;


/**
 * Třída HdoSiteViewModel slouží jako ViewModel pro správu dat a logiky související s HDO (Hromadné dálkové ovládání).
 * <p>
 * Tato třída poskytuje metody pro asynchronní získávání dat z různých distribučních oblastí (CEZ, EGD, PRE) a jejich zpracování.
 * <p>
 * Klíčové komponenty:
 * - Handler handler: Zpracovává výsledky asynchronních operací.
 * - MutableLiveData<Boolean> showDialog: Indikuje, zda by mělo být zobrazené dialogové okno.
 * - MutableLiveData<Boolean> showAlert: Indikuje, zda by mělo být zobrazené upozornění.
 * - MutableLiveData<Boolean> showProgress: Indikuje, zda by měl být zobrazený progress načítání dat.
 * - MutableLiveData<CodesContainer> containerMutableLiveData: Obsahuje kódy HDO pro EGD.
 * - MutableLiveData<ArrayList<HdoModel>> hdoModels: Obsahuje modely HDO.
 * - MutableLiveData<ArrayList<HdoSiteFragment.HdoListContainer>> hdoListModels: Obsahuje seznamy HDO.
 * - MutableLiveData<String[]> validityDates: Obsahuje data platnosti HDO.
 * <p>
 * Metody:
 * - shouldShowDialog(): Vrací LiveData indikující, zda by mělo být zobrazené dialogové okno.
 * - shouldShowAlert(): Vrací LiveData indikující, zda by mělo být zobrazené upozornění.
 * - shouldShowProgress(): Vrací LiveData indikující, zda by měl být zobrazený progress načítání dat.
 * - getHdoModel(): Vrací MutableLiveData obsahující modely HDO.
 * - getHdoListModels(): Vrací MutableLiveData obsahující seznamy HDO.
 * - getCodesContainer(): Vrací MutableLiveData obsahující kódy HDO pro EGD.
 * - getValidityDates(): Vrací MutableLiveData obsahující data platnosti HDO.
 * - setExceptionBrno(String exceptionBrno): Nastaví text výjimky pro Brno - venkov.
 * - setExceptionJihlava(String exceptionJihlava): Nastaví text výjimky pro Jihlavu.
 * - setExceptionTrebic(String exceptionTrebic): Nastaví text výjimky pro Třebíč.
 * - setExceptionJindrichuvHradec(String exceptionJindrichuvHradec): Nastaví text výjimky pro Jindřichův Hradec.
 * - setExceptionHodonin(String exceptionHodonin): Nastaví text výjimky pro Hodonín.
 * - setExceptionBreclav(String exceptionBreclav): Nastaví text výjimky pro Břeclav.
 * - hideDialog(): Skryje dialog se seznamem kódů HDO pro EGD.
 * - setShowAlert(boolean value): Zobrazí/skryje upozornění.
 * - clearData(): Smaže data v kontejneru s kódy HDO u EGD.
 * - runAsyncOperation(String url, int distributionAreaIndex, String hdoCode, FragmentActivity fragmentActivity, String districtName, int districtIndex, LinearLayout lnHdoButtons): Spustí asynchronní operaci pro získání kódů HDO.
 * - runSecondAsyncOperation(String group, String category, String urlHdo, String code, Context context, String districtName, int districtIndex): Spustí druhou asynchronní operaci pro získání kódů HDO u EGD.
 * - buildRecyclerView(String jsonData): Sestaví z JSON dat ArrayList<HdoModel> a aktualizuje hodnotu hdoModels.
 * <p>
 * Vnitřní třídy:
 * - CodesContainer: Kontejner pro kódy HDO u EGD.
 * <p>
 * Xlisto 11.01.2024 12:14
 */
public class HdoSiteViewModel extends ViewModel {

    private static final String TAG = "MyViewModel";
    private String exceptionBrno;
    private String exceptionJihlava;
    private String exceptionTrebic;
    private String exceptionJindrichuvHradec;
    private String exceptionHodonin;
    private String exceptionBreclav;
    private int distributionAreaIndex;

    private static final Connections connections = new Connections();
    private final MutableLiveData<Boolean> showDialog = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showAlertDialog = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showAlert = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showProgress = new MutableLiveData<>();
    private final MutableLiveData<CodesContainer> containerMutableLiveData = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<HdoModel>> hdoModels = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<HdoSiteFragment.HdoListContainer>> hdoListModels = new MutableLiveData<>();
    private final MutableLiveData<String[]> validityDates = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            showProgress.postValue(false);
            if (msg.what == 101) {
                showAlert.postValue(true);
                showAlertDialog.postValue(true);
                return;//kod 101 znamená, že se nepodařilo získat data; skryje progress a ukončí se
            }
            Connections.ResultData resultData = (Connections.ResultData) msg.obj;
            if (resultData.result.equals("[]")) {
                showAlert.postValue(true);
                showAlertDialog.postValue(true);
                return;
            }
            if (resultData.resultType.equals(Connections.ResultType.CODES)) {
                final ArrayList<String> codes = new ArrayList<>();
                final ArrayList<String[]> groupsList = new ArrayList<>();
                final ArrayList<String> codesException = new ArrayList<>();
                final ArrayList<String[]> groupsExceptionList = new ArrayList<>();
                //zobrazené kodu z EGD
                try {
                    JSONArray jsonArray = new JSONArray(resultData.result);
                    if (jsonArray.length() == 0) {
                        showAlert.postValue(true);
                        return;
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonGroupHdoCodes = jsonArray.getJSONObject(i);
                        JSONArray jsonCategoryGroups = jsonGroupHdoCodes.getJSONArray("kategorieSkupiny");
                        for (int j = 0; j < jsonCategoryGroups.length(); j++) {
                            JSONObject jsonCategoryGroup = jsonCategoryGroups.getJSONObject(j);
                            JSONArray jsonCodes = jsonCategoryGroup.getJSONArray("kody");
                            String category = jsonCategoryGroup.getString("kategorie");//ZAPAD;VYCHOD;VERSACOM
                            String group = jsonCategoryGroup.getString("skupina");

                            if (category.equals(resultData.areaEgd)) {
                                codes.add(jsonCodes.toString());
                                groupsList.add(new String[]{group, category});
                            }

                            if (resultData.area.equals("Brno - venkov") && category.equals("VERSACOM")) {
                                codesException.add(jsonCodes.toString());
                                groupsExceptionList.add(new String[]{group, category});
                            } else if (resultData.area.equals("Jihlava") && category.equals("VYCHOD")) {
                                codesException.add(jsonCodes.toString());
                                groupsExceptionList.add(new String[]{group, category});
                            } else if (resultData.area.equals("Třebíč") && category.equals("VYCHOD")) {
                                codesException.add(jsonCodes.toString());
                                groupsExceptionList.add(new String[]{group, category});
                            } else if (resultData.area.equals("Jindřichův Hradec") && category.equals("VYCHOD")) {
                                codesException.add(jsonCodes.toString());
                                groupsExceptionList.add(new String[]{group, category});
                            } else if (resultData.area.equals("Hodonín") && category.equals("VERSACOM")) {
                                codesException.add(jsonCodes.toString());
                                groupsExceptionList.add(new String[]{group, category});
                            } else if (resultData.area.equals("Břeclav") && category.equals("VERSACOM")) {
                                codesException.add(jsonCodes.toString());
                                groupsExceptionList.add(new String[]{group, category});
                            }
                        }
                        if (!codes.isEmpty()) {
                            String exceptionAreaList = "";
                            switch (resultData.area) {
                                case "Brno - venkov":
                                    exceptionAreaList = exceptionBrno;
                                    break;
                                case "Jihlava":
                                    exceptionAreaList = exceptionJihlava;
                                    break;
                                case "Třebíč":
                                    exceptionAreaList = exceptionTrebic;
                                    break;
                                case "Jindřichův Hradec":
                                    exceptionAreaList = exceptionJindrichuvHradec;
                                    break;
                                case "Hodonín":
                                    exceptionAreaList = exceptionHodonin;
                                    break;
                                case "Břeclav":
                                    exceptionAreaList = exceptionBreclav;
                                    break;
                            }
                            CodesContainer codesContainer = new CodesContainer(codes, groupsList, codesException, groupsExceptionList, resultData.area, exceptionAreaList, resultData.urlString);
                            containerMutableLiveData.postValue(codesContainer);
                        }
                        showDialog.postValue(true);
                        showAlert.postValue(false);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "handleMessage: ", e);
                }

            } else {
                buildRecyclerView(resultData.result);
            }
        }
    };


    /**
     * Zjistí zda by mělo být zobrazené dialogové okno
     *
     * @return true pokud by mělo být zobrazené dialogové okno
     */
    public LiveData<Boolean> shouldShowDialog() {
        return showDialog;
    }


    /**
     * Zjistí zda by mělo být zobrazené upozornění
     *
     * @return true pokud by mělo být zobrazené upozornění
     */
    public LiveData<Boolean> shouldShowAlertDialog() {
        return showAlertDialog;
    }


    /**
     * Zjistí zda by mělo být zobrazený upozornění
     *
     * @return true pokud by mělo být zobrazené upozornění
     */
    public LiveData<Boolean> shouldShowAlert() {
        return showAlert;
    }


    /**
     * Zjistí zda by měl být zobrazený progress načítání dat
     *
     * @return true pokud by měl být zobrazený progress
     */
    public LiveData<Boolean> shouldShowProgress() {
        return showProgress;
    }


    public MutableLiveData<ArrayList<HdoModel>> getHdoModel() {
        return hdoModels;
    }


    /**
     * Vrátí kontejner s časy HDO
     *
     * @return kontejner s časy HDO
     */
    public MutableLiveData<ArrayList<HdoSiteFragment.HdoListContainer>> getHdoListModels() {
        return hdoListModels;
    }


    /**
     * Vrátí kontejner s kódy HDO u EGD
     *
     * @return kontejner s kódy HDO u EGD
     */
    public MutableLiveData<CodesContainer> getCodesContainer() {
        return containerMutableLiveData;
    }


    /**
     * Vrátí pole s platností HDO
     *
     * @return pole s platností HDO
     */
    public MutableLiveData<String[]> getValidityDates() {
        return validityDates;
    }


    /**
     * Nastaví text výjimky pro Brno - venkov
     *
     * @param exceptionBrno text výjimky pro Brno - venkov
     */
    public void setExceptionBrno(String exceptionBrno) {
        this.exceptionBrno = exceptionBrno;
    }


    /**
     * Nastaví text výjimky pro Jihlavu
     *
     * @param exceptionJihlava text výjimky pro Jihlavu
     */
    public void setExceptionJihlava(String exceptionJihlava) {
        this.exceptionJihlava = exceptionJihlava;
    }


    /**
     * Nastaví text výjimky pro Třebíč
     *
     * @param exceptionTrebic text výjimky pro Třebíč
     */
    public void setExceptionTrebic(String exceptionTrebic) {
        this.exceptionTrebic = exceptionTrebic;
    }


    /**
     * Nastaví text výjimky pro Jindřichův Hradec
     *
     * @param exceptionJindrichuvHradec text výjimky pro Jindřichův Hradec
     */
    public void setExceptionJindrichuvHradec(String exceptionJindrichuvHradec) {
        this.exceptionJindrichuvHradec = exceptionJindrichuvHradec;
    }


    /**
     * Nastaví text výjimky pro Hodonín
     *
     * @param exceptionHodonin text výjimky pro Hodonín
     */
    public void setExceptionHodonin(String exceptionHodonin) {
        this.exceptionHodonin = exceptionHodonin;
    }


    /**
     * Nastaví text výjimky pro Břeclav
     *
     * @param exceptionBreclav text výjimky pro Břeclav
     */
    public void setExceptionBreclav(String exceptionBreclav) {
        this.exceptionBreclav = exceptionBreclav;
    }


    /**
     * Skryje dialog se seznamem kódů HDO pro EGD
     */
    public void hideDialog() {
        showDialog.postValue(false);
    }


    /**
     * Zobrazí/skryje upozornění
     *
     * @param value true pokud se má upozornění zobrazit
     */
    public void setShowAlert(boolean value) {
        showAlert.postValue(value);
    }


    /**
     * Zobrazí/skryje dialogové okno s výstrahou
     *
     * @param value true pokud se má dialogové okno zobrazit
     */
    public void setShouldShowAlertDialog(boolean value) {
        showAlertDialog.postValue(value);
    }


    /**
     * Smaže data v kontejneru s kódy HDO u EGD
     */
    public void clearData() {
        hdoListModels.postValue(new ArrayList<>());
        validityDates.postValue(new String[]{});
    }


    /**
     * Spustí asynchronní operaci pro získání kódů HDO, u EGD se získá seznam kodů
     *
     * @param url                   url pro api získání kódů HDO
     * @param distributionAreaIndex index distribuční oblasti
     * @param hdoCode               kód HDO
     * @param fragmentActivity      fragmentActivity
     * @param districtName          název okresu
     * @param districtIndex         index okresu
     * @param lnHdoButtons          layout pro tlačítka HDO
     */
    public void runAsyncOperation(String url, int distributionAreaIndex, String hdoCode, FragmentActivity fragmentActivity, String districtName, int districtIndex, LinearLayout lnHdoButtons) {
        this.distributionAreaIndex = distributionAreaIndex;
        showAlert.postValue(false);
        showProgress.postValue(true);
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            //Background work here
            connections.sendPost(url, distributionAreaIndex, hdoCode, fragmentActivity, districtName, districtIndex, lnHdoButtons, this.handler);
        });
    }


    /**
     * Spustí druhou asynchronní operaci pro získání kódů HDO u EGD
     *
     * @param group         kategorie
     * @param category      kategorie
     * @param urlHdo        url pro api získání kódů HDO
     * @param code          kód HDO
     * @param context       context
     * @param districtName  název okresu
     * @param districtIndex index okresu
     */
    public void runSecondAsyncOperation(String group, String category, String urlHdo, String code, Context context, String districtName, int districtIndex) {
        showProgress.postValue(true);
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            //Background work here
            connections.searchHdo(group, category, urlHdo, code, context, districtName, districtIndex, this.handler);
        });
    }


    /**
     * Sestaví z JSON dat ArrayList<HdoModel> a aktualizuje hodnotu hdoModels
     *
     * @param jsonData JSON data
     */
    private void buildRecyclerView(String jsonData) {
        if (distributionAreaIndex == 0) {
            //CEZ
            ArrayList<HdoSiteFragment.HdoListContainer> hdoList = BuildCEZ.build(jsonData);
            hdoListModels.postValue(hdoList);
            validityDates.postValue(new String[]{hdoList.get(0).validityDate});
        } else if (distributionAreaIndex == 1) {
            //EGD
            ArrayList<HdoSiteFragment.HdoListContainer> hdoListContainer = BuildEGD.build(jsonData);
            //při první průchodu vrátí null, ukončuji
            if (hdoListContainer == null)
                return;
            if (!hdoListContainer.isEmpty()) {
                String[] validityDate = new String[hdoListContainer.size()];
                for (int i = 0; i < hdoListContainer.size(); i++) {
                    validityDate[i] = hdoListContainer.get(i).validityDate;
                }
                validityDates.postValue(validityDate);
                hdoListModels.postValue(hdoListContainer);
                hdoModels.postValue(hdoListContainer.get(0).hdoList);
            }
        } else if (distributionAreaIndex == 2) {
            //PRE
            ArrayList<HdoSiteFragment.HdoListContainer> hdoList = BuildPRE.build(jsonData);
            hdoListModels.postValue(hdoList);
            hdoModels.postValue(hdoList.get(0).hdoList);
            validityDates.postValue(new String[]{hdoList.get(0).validityDate});
        }
    }


    /**
     * Kontejner pro kódy HDO u EGD
     */
    static class CodesContainer {

        ArrayList<String> codes;
        ArrayList<String[]> groupsList;
        ArrayList<String> codesException;
        ArrayList<String[]> groupsExceptionList;
        String area;
        String exceptionAreaList;
        String urlHdo;


        public CodesContainer(ArrayList<String> codes, ArrayList<String[]> groupsList, ArrayList<String> codesException, ArrayList<String[]> groupsExceptionList, String area, String exceptionAreaList, String urlHdo) {
            this.codes = codes;
            this.groupsList = groupsList;
            this.codesException = codesException;
            this.groupsExceptionList = groupsExceptionList;
            this.area = area;
            this.exceptionAreaList = exceptionAreaList;
            this.urlHdo = urlHdo;
        }

    }

}
