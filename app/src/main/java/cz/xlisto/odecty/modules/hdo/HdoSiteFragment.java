package cz.xlisto.odecty.modules.hdo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.dialogs.SpinnerDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.Keyboard;
import cz.xlisto.odecty.utils.SubscriptionPoint;


/**
 * Xlisto 15.06.2023 21:28
 */
public class HdoSiteFragment extends Fragment {
    private static final String TAG = "HdoSite";
    private static final String FLAG_RESULT_SPINNER_DIALOG_FRAGMENT = "flagResultSpinnerDialogFragment";
    private static final String FLAG_RESULT_YES_NO_DIALOG_FRAGMENT = "flagResultYesNoDialogFragment";
    private static final String ARG_RESULT_JSON = "resultJson";
    private static final String ARG_DISTRICT_SELECTED = "areaSelected";
    private LinearLayout lnCode, lnCodesEdg, lnHdoButtons, lnProgessBarHdoSite;
    private Spinner spDistributionArea, spDistrict, spDateEgd, spA, spB, spPB;
    private EditText etHdoCode;
    private TextView tvAlert, tvValidityDate;
    private RecyclerView rvHdoSite;
    private static final Connections connections = new Connections();
    private final ArrayList<String> codes = new ArrayList<>();
    private final ArrayList<String> codesException = new ArrayList<>();
    private final ArrayList<String[]> groupsList = new ArrayList<>();
    private final ArrayList<String[]> groupsExceptionList = new ArrayList<>();
    private ArrayList<HdoModel> hdoList = new ArrayList<>();
    private String urlHdo, resultJson;
    private ExecutorService executor;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            setVisibilityProgressBar(false);
            Connections.ResultData resultData = (Connections.ResultData) msg.obj;
            if (resultData.resultType.equals(Connections.ResultType.CODES)) {
                codes.clear();
                codesException.clear();
                groupsList.clear();
                groupsExceptionList.clear();
                HdoSiteFragment.this.urlHdo = resultData.urlString;
                //zobrazené kodu z EGD
                try {
                    JSONArray jsonArray = new JSONArray(resultData.result);
                    if (jsonArray.length() == 0) {
                        setVisibilityAlert(true);
                        Toast.makeText(requireActivity(), "Žádná data, zkontrolujte správnost HDO povelu", Toast.LENGTH_SHORT).show();
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
                        if (codes.size() > 0) {
                            //TODO: tady potřebuji okres
                            String exceptionAreaList = "";
                            switch (resultData.area) {
                                case "Brno - venkov":
                                    exceptionAreaList = getResources().getString(R.string.exception_brno);
                                    break;
                                case "Jihlava":
                                    exceptionAreaList = getResources().getString(R.string.exception_jihlava);
                                    break;
                                case "Třebíč":
                                    exceptionAreaList = getResources().getString(R.string.exception_trebic);
                                    break;
                                case "Jindřichův Hradec":
                                    exceptionAreaList = getResources().getString(R.string.exception_jindrichuv_hradec);
                                    break;
                                case "Hodonín":
                                    exceptionAreaList = getResources().getString(R.string.exception_hodonin);
                                    break;
                                case "Břeclav":
                                    exceptionAreaList = getResources().getString(R.string.exception_breclav);
                                    break;
                            }
                            SpinnerDialogFragment.newInstance("Vyber", codes, codesException, groupsList, groupsExceptionList, resultData.area, exceptionAreaList, FLAG_RESULT_SPINNER_DIALOG_FRAGMENT)
                                    .show(getChildFragmentManager(), "spinnerDialog");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                buildRecyclerView(resultData.result);
            }
        }
    };


    public static HdoSiteFragment newInstance() {
        return new HdoSiteFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fagment_hdo_site, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spDistributionArea = view.findViewById(R.id.spDistributionArea);
        spDistrict = view.findViewById(R.id.spDistrict);
        Button btnHdoSite = view.findViewById(R.id.btnHdoSite);
        Button btnHdoLoadData = view.findViewById(R.id.btnHdoLoadData);
        Button btnSaveHdo = view.findViewById(R.id.btnSaveHdo);
        etHdoCode = view.findViewById(R.id.etHdoCode);
        lnCode = view.findViewById(R.id.lnCode);
        lnCodesEdg = view.findViewById(R.id.lnCodeEgd);
        rvHdoSite = view.findViewById(R.id.rvHdoSite);
        tvAlert = view.findViewById(R.id.tvAlertHdoSite);
        tvValidityDate = view.findViewById(R.id.tvValidityDate);
        lnHdoButtons = view.findViewById(R.id.lnHdoButtons);
        spDateEgd = view.findViewById(R.id.spDateEgd);
        lnProgessBarHdoSite = view.findViewById(R.id.lnProgressBarHdoSite);
        spA = view.findViewById(R.id.spA);
        spB = view.findViewById(R.id.spB);
        spPB = view.findViewById(R.id.spPB);


        spA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setHdoCodeFromSpinners();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setHdoCodeFromSpinners();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spPB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setHdoCodeFromSpinners();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //posluchač pro zavření dialogového okna s dotazem na výběr skupiny a kategorie
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_SPINNER_DIALOG_FRAGMENT, this, (requestKey, result) -> {
            String group = result.getString(SpinnerDialogFragment.ARG_GROUP);
            String category = result.getString(SpinnerDialogFragment.ARG_CATEGORY);
            boolean isYes = result.getBoolean(SpinnerDialogFragment.RESULT);

            if (isYes) {
                String code = etHdoCode.getText().toString();
                connections.searchHdo(group, category, urlHdo, code, requireActivity(), spDistrict);
            } else {
                setVisibilityProgressBar(false);
            }
            executor = null;
        });

        //posluchač pro zavření dialogového okna s dotazem na uložení do databáze
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_YES_NO_DIALOG_FRAGMENT, this, (requestKey, result) -> {
            boolean isYes = result.getBoolean(YesNoDialogFragment.RESULT);
            if (isYes) {
                saveHdo();
            }
        });

        spDistributionArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hideWidgets(position);

                switch (position) {
                    case 0:
                        spDistrict.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.area_cez)));
                        break;
                    case 1:
                        spDistrict.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.area_egd)));
                        break;
                    case 2:
                        spDistrict.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.area_pre)));
                        break;

                }

                //setAdapter(new ArrayList<HdoModel>());
                setTextAlert();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                //setAdapter(new ArrayList<HdoModel>());
                setTextAlert();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnHdoSite.setOnClickListener(v -> openHdoSite());

        btnHdoLoadData.setOnClickListener(v -> {
            setAdapter(new ArrayList<>());
            Keyboard.hide(requireActivity());
            urlBuilder();
        });

        btnSaveHdo.setOnClickListener(v -> YesNoDialogFragment.newInstance(getString(R.string.save_hdo), FLAG_RESULT_YES_NO_DIALOG_FRAGMENT, getString(R.string.save_hdo_message))
                .show(requireActivity().getSupportFragmentManager(), "saveHdo"));


        hideWidgets(0);

        spA.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.egd_code1)));
        spB.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.egd_code1)));
        spPB.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.egd_code2)));

        if (savedInstanceState != null) {
            Log.w(TAG, "onViewCreated: " + savedInstanceState.getInt(ARG_DISTRICT_SELECTED));
            resultJson = savedInstanceState.getString(ARG_RESULT_JSON);
            setAdapter(hdoList);

        }
        spDistrict.setSelection(4);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_RESULT_JSON, resultJson);
        outState.putInt(ARG_DISTRICT_SELECTED, spDistrict.getSelectedItemPosition());
    }


    /**
     * Skryje nepotřebná formulářová pole podle výběru distribuční sítě
     *
     * @param item Index vybrané distribuční sítě (0-CEZ, 1-EON, 2- PRE)
     */
    private void hideWidgets(int item) {
        //0-CEZ, 1-EON, 2- PRE
        switch (item) {
            case 0:
                lnCode.setVisibility(View.VISIBLE);
                lnCodesEdg.setVisibility(View.GONE);
                spDateEgd.setVisibility(View.GONE);
                tvValidityDate.setVisibility(View.VISIBLE);
                break;
            case 1:
                lnCode.setVisibility(View.VISIBLE);
                lnCodesEdg.setVisibility(View.VISIBLE);
                spDateEgd.setVisibility(View.VISIBLE);
                tvValidityDate.setVisibility(View.GONE);
                break;
            case 2:
                lnCode.setVisibility(View.GONE);
                lnCodesEdg.setVisibility(View.GONE);
                spDateEgd.setVisibility(View.GONE);
                tvValidityDate.setVisibility(View.VISIBLE);
                break;
        }
    }


    /**
     * Otevře stránku na vyhledání HDO ve webovém prohlížeči
     */
    private void openHdoSite() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(getResources().getStringArray(R.array.url_hdo)[spDistributionArea.getSelectedItemPosition()]));
        startActivity(intent);
    }

    /**
     * Sestaví url podle nastavení formuláře a zavolá načtení dat
     */
    private void urlBuilder() {
        UrlBuilder urlBuilder = new UrlBuilder(spDistributionArea, spDistrict, etHdoCode);
        loadData(urlBuilder.buildUrl(), spDistributionArea.getSelectedItemPosition());
    }


    /**
     * Spustí vlákno na pozadí s dotazem na načtení dat
     *
     * @param url                   url pro dotaz
     * @param distributionAreaIndex index distribuční sítě (0-CEZ, 1-EON, 2- PRE)
     */
    private void loadData(String url, int distributionAreaIndex) {
        setVisibilityProgressBar(true);
        setVisibilityAlert(false);

        executor = Executors.newSingleThreadExecutor();
        //0-CEZ,1-EGD,2-PRE

        executor.execute(() -> {

            //Background work here
            connections.sendPost(url, distributionAreaIndex, etHdoCode.getText().toString(), requireActivity(), spDistrict, lnHdoButtons, handler);
        });
    }

    private void buildRecyclerView(String jsonData) {
        hdoList.clear();
        if (spDistributionArea.getSelectedItemPosition() == 0) {
            hdoList = buildCEZ(jsonData);
        } else if (spDistributionArea.getSelectedItemPosition() == 1) {

            ArrayList<HdoListContainer> hdoListContainer = buildEGD(jsonData);
            //při první průchodu vrátí null, ukončuji
            if (hdoListContainer == null)
                return;
            if (hdoListContainer.size() != 0) {
                hdoList = hdoListContainer.get(0).getHdoList();
                String[] validityDate = new String[hdoListContainer.size()];
                for (int i = 0; i < hdoListContainer.size(); i++) {
                    validityDate[i] = hdoListContainer.get(i).validityDate;
                }
                spDateEgd.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, validityDate));
                spDateEgd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        setAdapter(hdoListContainer.get(position).getHdoList());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

            }
        } else if (spDistributionArea.getSelectedItemPosition() == 2) {
            hdoList = buildPRE(jsonData);
        }

        setAdapter(hdoList);
        rvHdoSite.getRecycledViewPool().clear();
        Objects.requireNonNull(rvHdoSite.getAdapter()).notifyDataSetChanged();
        if (hdoList.size() == 0) {
            tvAlert.setText(getResources().getText(R.string.no_hdo));
            Toast.makeText(requireActivity(), "Žádná data, zkontrolujte správnost HDO povelu", Toast.LENGTH_SHORT).show();
        }
        //requireActivity().runOnUiThread(() -> setVisibilityProgressBar(false));
        setVisibilityProgressBar(false);
    }

    private ArrayList<HdoModel> buildCEZ(String jsonData) {
        ArrayList<HdoModel> hdoList = new ArrayList<>();
        try {
            JSONArray jsonRoot = new JSONArray(jsonData);
            for (int i = 0; i < jsonRoot.length(); i++) {
                JSONObject jsonHdo = jsonRoot.getJSONObject(i);
                String validFrom = ViewHelper.convertLongToDate(jsonHdo.getLong("VALID_FROM"));
                String validTo = ViewHelper.convertLongToDate(jsonHdo.getLong("VALID_TO"));
                setTvValidityDate(validFrom + " - " + validTo);
                String day = jsonHdo.getString("PLATNOST");
                String sazba = jsonHdo.getString("SAZBA");
                String info = jsonHdo.getString("INFO")
                        .replace("sazba", "")
                        .replace("+", "");
                String rele = sazba;
                if (info.length() > 0) {
                    rele += " + " + info;
                }
                for (int j = 1; j < 11; j++) {
                    String timeOn = jsonHdo.getString("CAS_ZAP_" + j);
                    String timeOff = jsonHdo.getString("CAS_VYP_" + j);
                    if (timeOn.equals("null") || timeOff.equals("null")) {
                        break;
                    }
                    HdoModel hdo = null;
                    if (day.equals("Po - Pá")) {
                        hdo = new HdoModel(rele, validFrom, validTo, timeOn, timeOff, 1, 1, 1, 1, 1, 0, 0, DistributionArea.CEZ.toString());

                    }
                    if (day.equals("So - Ne")) {
                        hdo = new HdoModel(rele, validFrom, validTo, timeOn, timeOff, 0, 0, 0, 0, 0, 1, 1, DistributionArea.CEZ.toString());
                    }
                    if (hdo != null) {
                        hdoList.add(hdo);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hdoList;
    }

    private ArrayList<HdoListContainer> buildEGD(String jsonData) {
        ArrayList<HdoListContainer> hdoListContainers = new ArrayList<>();
        Log.w(TAG, "buildEGD: " + jsonData);
        //první průchod vrací objekt s kody - přeskakuji

        try {

            JSONArray jsonRoot = new JSONArray(jsonData);
            for (int i = 0; i < jsonRoot.length(); i++) {
                ArrayList<HdoModel> hdoList = new ArrayList<>();
                JSONObject jsonHdo = jsonRoot.getJSONObject(i);
                JSONObject jsonDateFrom = jsonHdo.getJSONObject("od");
                JSONObject jsonDateTo = jsonHdo.getJSONObject("do");
                String validDate = parseEgdDate(jsonDateFrom) + " - " + parseEgdDate(jsonDateTo);
                JSONArray jsonRates = jsonHdo.getJSONArray("sazby");
                for (int j = 0; j < jsonRates.length(); j++) {
                    JSONObject jsonRate = jsonRates.getJSONObject(j);
                    String rate = jsonRate.getString("sazba");
                    JSONArray jsonDays = jsonRate.getJSONArray("dny");
                    JSONObject[] jsonDaysObj = new JSONObject[jsonDays.length()];
                    for (int k = 0; k < jsonDays.length(); k++) {
                        jsonDaysObj[k] = jsonDays.getJSONObject(k);
                        //Log.w(TAG, "buildEGD: " + jsonDaysObj[k]);
                    }
                    ArrayList<JSONObject> jsonObjectArrayList = JsonHdoEgdMerge.init(jsonDaysObj);
                    for (int l = 0; l < jsonObjectArrayList.size(); l++) {
                        JSONObject jsonObject = jsonObjectArrayList.get(l);
                        JSONArray day = jsonObject.getJSONArray("denVTydnu");
                        JSONArray times = jsonObject.getJSONArray("casy");
                        for (int m = 0; m < times.length(); m++) {
                            JSONObject time = times.getJSONObject(m);
                            String timeOn = time.getString("od");
                            String timeOff = time.getString("do");
                            HdoModel hdo = new HdoModel(rate, parseEgdDate(jsonDateFrom), parseEgdDate(jsonDateTo), timeOn, timeOff,
                                    containsDay(day, "1"),
                                    containsDay(day, "2"),
                                    containsDay(day, "3"),
                                    containsDay(day, "4"),
                                    containsDay(day, "5"),
                                    containsDay(day, "6"),
                                    containsDay(day, "7"),
                                    DistributionArea.EGD.toString());
                            hdoList.add(hdo);
                        }
                    }
                }
                HdoListContainer hdoListContainer = new HdoListContainer(hdoList, validDate);
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
    private String parseEgdDate(JSONObject jsonObject) {
        String date = "";
        try {
            date = "" + jsonObject.get("den") + "." + jsonObject.get("mesic") + "." + jsonObject.get("rok");
            date = date.replace(".9999", "").replace("1900", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return date;
    }


    private int containsDay(JSONArray jsonArray, String day) {
        if (jsonArray.toString().contains(day))
            return 1;
        else
            return 0;
    }


    /**
     * Sestaví seznam HDO pro PRE
     *
     * @param jsonData objekt json s daty HDO
     * @return seznam HDO
     */
    private ArrayList<HdoModel> buildPRE(String jsonData) {
        ArrayList<HdoModel> hdoList = new ArrayList<>();
        try {
            JSONArray jsonRoot = new JSONArray(jsonData);
            String validityDate = null;
            //PRE
            for (int i = 0; i < jsonRoot.length(); i++) {
                JSONObject jsonObject = jsonRoot.getJSONObject(i);
                String rele = jsonObject.getString("kodPovelu");
                String date = jsonObject.getString("platnost");

                if (i == 0)
                    validityDate = date;
                if (i == jsonRoot.length() - 1)
                    validityDate += " - " + date;

                for (int j = 0; j < 10; j++) {
                    String timeOn = jsonObject.getString("casZap" + j);
                    String timeOff = jsonObject.getString("casVyp" + j);
                    if (timeOn.equals("") || timeOff.equals("")) {
                        break;
                    }
                    HdoModel hdo = new HdoModel(rele, date, "", timeOn, timeOff, 0, 0, 0, 0, 0, 0, 0, DistributionArea.PRE.toString());
                    Log.w(TAG, "buildPRE: " + hdo);
                    hdoList.add(hdo);
                }
            }
            setTvValidityDate(validityDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hdoList;
    }

    /**
     * Nastaví adapter pro recycler view
     */
    private void setAdapter(ArrayList<HdoModel> hdoModels) {
        setVisibilityAlert(hdoModels.size() == 0);

        HdoAdapter hdoAdapter = new HdoAdapter(hdoModels, rvHdoSite, false);
        rvHdoSite.setAdapter(hdoAdapter);
        hdoAdapter.notifyDataSetChanged();
        rvHdoSite.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvHdoSite.scheduleLayoutAnimation();
    }


    /**
     * Nastaví text platnosti
     *
     * @param date text platnosti
     */
    private void setTvValidityDate(String date) {
        tvValidityDate.setText("Platnost: " + date);
    }


    /**
     * Nastaví viditelnost upozornění a skryje text platnosti
     *
     * @param b true = zobrazit, false = skrýt
     */
    private void setVisibilityAlert(boolean b) {
        if (b) {
            tvAlert.setVisibility(View.VISIBLE);
            tvValidityDate.setText("");
        } else {
            tvAlert.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * Nastaví text upozornění podle vybraného distributora
     */
    private void setTextAlert() {
        Log.w(TAG, "setTextAlert: " + spDistributionArea.getSelectedItemPosition());
        switch (spDistributionArea.getSelectedItemPosition()) {
            case 0:
            case 1:
                tvAlert.setText(getResources().getString(R.string.write_hdo));
                break;
            case 2:
                tvAlert.setText(getResources().getString(R.string.select_hdo));
                break;
        }
    }


    /**
     * Nastaví viditelnost progressbaru při načítání dat
     *
     * @param b true = zobrazit, false = skrýt
     */
    private void setVisibilityProgressBar(boolean b) {
        if (b) {
            lnProgessBarHdoSite.setVisibility(View.VISIBLE);
        } else {
            lnProgessBarHdoSite.setVisibility(View.GONE);
        }
    }


    /**
     * Nastaví a sestaví kód HDO z vybraných spinnerů
     */
    private void setHdoCodeFromSpinners() {
        String code = "A" + spA.getSelectedItem()
                + "B" + spB.getSelectedItem()
                + "DP" + spPB.getSelectedItem();
        etHdoCode.setText(code);
    }


    /**
     * Uloží HDO do databáze
     */
    private void saveHdo() {
        if (hdoList != null) {
            if (hdoList.size() > 0) {
                SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireActivity());
                assert subscriptionPoint != null;
                String tableHdo = subscriptionPoint.getTableHDO();
                if (tableHdo == null || tableHdo.equals("")) {
                    Toast.makeText(requireActivity(), "Nepodařilo se načíst údaje o odběrném místě", Toast.LENGTH_SHORT).show();
                    return;
                }
                DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
                dataHdoSource.saveHdo(hdoList, tableHdo);
            } else {
                Toast.makeText(requireActivity(), "Nejprve načtěte data", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireActivity(), "Nejprve načtěte data", Toast.LENGTH_SHORT).show();
        }
    }

    static class HdoListContainer {
        ArrayList<HdoModel> hdoList;
        String validityDate;

        public HdoListContainer(ArrayList<HdoModel> hdoList, String validityDate) {
            this.hdoList = hdoList;
            this.validityDate = validityDate;
        }

        public ArrayList<HdoModel> getHdoList() {
            return hdoList;
        }
    }
}
