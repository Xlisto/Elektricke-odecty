package cz.xlisto.elektrodroid.modules.hdo;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.dialogs.SelectHdoCategoryDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.Keyboard;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Fragment pro práci s HDO (Hromadné Dálkové Ovládání).
 *
 * <p>
 * Tento fragment umožňuje uživateli sestavit HDO kód, načíst odpovídající data z webu,
 * zobrazit je v seznamu a případně uložit do lokální databáze. Fragment komunikuje s
 * {@code HdoSiteViewModel} pro asynchronní operace a uchovává dočasné výsledky v
 * interních polích.
 */
public class HdoSiteFragment extends Fragment {

    private static final String TAG = "HdoSiteFragment";
    private static final String FLAG_RESULT_SPINNER_DIALOG_FRAGMENT = "flagResultSpinnerDialogFragment";
    private static final String FLAG_RESULT_YES_NO_DIALOG_FRAGMENT = "flagResultYesNoDialogFragment";
    private static final String ARG_RESULT_JSON = "resultJson";
    // JSON keys used to save/load HDO widgets settings
    private static final String KEY_DISTRIBUTION_AREA_INDEX = "distributionAreaIndex";
    private static final String KEY_DISTRIBUTION_AREA = "distributionArea";
    private static final String KEY_DISTRICT_INDEX = "districtIndex";
    private static final String KEY_DISTRICT = "district";
    private static final String KEY_A_INDEX = "aIndex";
    private static final String KEY_A = "a";
    private static final String KEY_B_INDEX = "bIndex";
    private static final String KEY_B = "b";
    private static final String KEY_PB_INDEX = "pbIndex";
    private static final String KEY_PB = "pb";
    private static final String KEY_HDO_CODE = "hdoCode";
    private static final int DIST_CEZ = 0;
    private static final int DIST_EGD = 1; // EON/EGD
    private static final int DIST_PRE = 2;
    private LinearLayout lnCode, lnCodesEdg, lnHdoButtons, lnProgessBarHdoSite;
    private Spinner spDistributionArea, spDistrict, spDateEgd, spA, spB, spPB;
    private EditText etHdoCode;
    private TextView tvAlert, tvValidityDate;
    private RecyclerView rvHdoSite;
    private final ArrayList<String> codes = new ArrayList<>();
    private final ArrayList<String> codesException = new ArrayList<>();
    private final ArrayList<String[]> groupsList = new ArrayList<>();
    private final ArrayList<String[]> groupsExceptionList = new ArrayList<>();
    private final ArrayList<HdoModel> hdoList = new ArrayList<>();
    private final ArrayList<HdoListContainer> hdoListContainer = new ArrayList<>();
    private String urlHdo, resultJson;
    private boolean isClearArrayLists = true;
    private HdoSiteViewModel viewModel;
    private String resultArea, exceptionArea;
    private final AdapterView.OnItemSelectedListener distributionAreaListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            hideWidgets(position);
            setSpDistrict(position);
            clear();
            setTextAlert();
        }


        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private final AdapterView.OnItemSelectedListener hdoEGDListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            setHdoCodeFromSpinners();
        }


        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private final TextWatcher etHdoCodeTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            spA.setSelection(0);
            spB.setSelection(0);
            spPB.setSelection(0);
        }


        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }


        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            if (msg.what == 300) {
                isClearArrayLists = true;
            }
        }
    };


    /**
     * Vytvoří novou instanci fragmentu HdoSiteFragment.
     *
     * @return Nová instance HdoSiteFragment.
     */
    public static HdoSiteFragment newInstance() {
        return new HdoSiteFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HdoSiteViewModel.class);

        viewModel.setExceptionBrno(getResources().getString(R.string.exception_brno));
        viewModel.setExceptionJihlava(getResources().getString(R.string.exception_jihlava));
        viewModel.setExceptionTrebic(getResources().getString(R.string.exception_trebic));
        viewModel.setExceptionJindrichuvHradec(getResources().getString(R.string.exception_jindrichuv_hradec));
        viewModel.setExceptionHodonin(getResources().getString(R.string.exception_hodonin));
        viewModel.setExceptionBreclav(getResources().getString(R.string.exception_breclav));
        viewModel.setShowAlert(true);

        viewModel.getCodesContainer().observe(this, codesContainer -> {
            this.codes.clear();
            this.codes.addAll(codesContainer.codes);
            this.codesException.clear();
            this.codesException.addAll(codesContainer.codesException);
            this.groupsList.clear();
            this.groupsList.addAll(codesContainer.groupsList);
            this.groupsExceptionList.clear();
            this.groupsExceptionList.addAll(codesContainer.groupsExceptionList);
            this.resultArea = codesContainer.area;
            this.exceptionArea = codesContainer.exceptionAreaList;
            this.urlHdo = codesContainer.urlHdo;

        });

        // Sleduje změny v LiveData objektu shouldShowDialog a zobrazuje dialogové okno s výběrem kategorie, pokud je hodnota true.
        viewModel.shouldShowDialog().observe(this, shouldShowDialog -> {
            if (shouldShowDialog) {
                SelectHdoCategoryDialogFragment.newInstance(getResources().getString(R.string.select), codes, codesException, groupsList, groupsExceptionList, resultArea, exceptionArea, FLAG_RESULT_SPINNER_DIALOG_FRAGMENT)
                        .show(getChildFragmentManager(), "spinnerDialog");
            }
            viewModel.hideDialog();
        });

        // Sleduje změny v LiveData objektu shouldShowAlertDialog a zobrazuje dialogové okno s upozorněním, pokud je hodnota true.
        viewModel.shouldShowAlertDialog().observe(this, shouldShowAlertDialog -> {
            if (shouldShowAlertDialog) {
                OwnAlertDialog.showDialog(requireActivity(),
                        requireContext().getResources().getString(R.string.error),
                        requireContext().getResources().getString(R.string.no_data_alert),
                        () -> {
                            viewModel.setShouldShowAlertDialog(false);
                            HdoSiteFragment.this.setVisibilityProgressBar(false);
                        });
            }
        });

        // Sleduje změny v LiveData objektu shouldShowAlert a zobrazuje upozornění, pokud je hodnota true.
        viewModel.shouldShowAlert().observe(this, this::setVisibilityAlert);

        // Sleduje změny v LiveData objektu shouldShowProgress a zobrazuje progress, pokud je hodnota true.
        viewModel.shouldShowProgress().observe(this, this::setVisibilityProgressBar);

        if (savedInstanceState != null) {
            isClearArrayLists = false;
        }
    }

    /**
     * Voláno při vytvoření instance view fragmentu.
     * Inicializuje a nastavuje view komponenty včetně listenerů a načítání uložených nastavení.
     *
     * @param inflater           inflater pro vytvoření view
     * @param container          rodičovský kontejner
     * @param savedInstanceState uložený stav (pokud existuje)
     */


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hdo_site, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spDistributionArea = view.findViewById(R.id.spDistributionArea);
        spDistrict = view.findViewById(R.id.spDistrict);
        Button btnHdoSite = view.findViewById(R.id.btnHdoSite);
        Button btnHdoLoadData = view.findViewById(R.id.btnHdoLoadData);
        Button btnSaveHdo = view.findViewById(R.id.btnSaveHdo);
        Button btnClipData = view.findViewById(R.id.btnClipData);
        etHdoCode = view.findViewById(R.id.etHdoCode);
        lnCode = view.findViewById(R.id.lnCode);
        lnCodesEdg = view.findViewById(R.id.lnCodeEgd);
        rvHdoSite = view.findViewById(R.id.rvHdoSite);
        tvAlert = view.findViewById(R.id.tvAlertHdoSite);
        tvValidityDate = view.findViewById(R.id.tvValidityDate);
        lnHdoButtons = view.findViewById(R.id.lnHdoButtons1);
        spDateEgd = view.findViewById(R.id.spDateEgd);
        lnProgessBarHdoSite = view.findViewById(R.id.lnProgressBar);
        spA = view.findViewById(R.id.spA);
        spB = view.findViewById(R.id.spB);
        spPB = view.findViewById(R.id.spPB);

        spA.setOnItemSelectedListener(hdoEGDListener);
        spB.setOnItemSelectedListener(hdoEGDListener);
        spPB.setOnItemSelectedListener(hdoEGDListener);

        //posluchač pro zavření dialogového okna s dotazem na výběr skupiny a kategorie
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_SPINNER_DIALOG_FRAGMENT, this, (requestKey, result) -> {
            String group = result.getString(SelectHdoCategoryDialogFragment.ARG_GROUP);
            String category = result.getString(SelectHdoCategoryDialogFragment.ARG_CATEGORY);
            boolean isYes = result.getBoolean(SelectHdoCategoryDialogFragment.RESULT);

            if (isYes) {
                String code = etHdoCode.getText().toString();

                viewModel.runSecondAsyncOperation(group, category, urlHdo, code, requireActivity(), spDistrict.getSelectedItem().toString(), spDistrict.getSelectedItemPosition());
            }
            viewModel.hideDialog();
        });

        //posluchač pro zavření dialogového okna s dotazem na uložení do databáze
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_RESULT_YES_NO_DIALOG_FRAGMENT, this, (requestKey, result) -> {
            boolean isYes = result.getBoolean(YesNoDialogFragment.RESULT);
            if (isYes) {
                saveHdo();
            }
        });

        spDistributionArea.setOnItemSelectedListener(distributionAreaListener);

        spDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clear();
                setTextAlert();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etHdoCode.setOnClickListener(v -> etHdoCode.addTextChangedListener(etHdoCodeTextWatcher));

        btnHdoSite.setOnClickListener(v -> openHdoSite());

        btnHdoLoadData.setOnClickListener(v -> {
            SubscriptionPointModel subscriptionPoint = getSubscriptionPoint();
            if (subscriptionPoint != null) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put(KEY_DISTRIBUTION_AREA_INDEX, spDistributionArea.getSelectedItemPosition());
                    obj.put(KEY_DISTRIBUTION_AREA, spDistributionArea.getSelectedItem().toString());
                    obj.put(KEY_DISTRICT_INDEX, spDistrict.getSelectedItemPosition());
                    obj.put(KEY_DISTRICT, spDistrict.getSelectedItem().toString());
                    obj.put(KEY_A_INDEX, spA.getSelectedItemPosition());
                    obj.put(KEY_A, spA.getSelectedItem().toString());
                    obj.put(KEY_B_INDEX, spB.getSelectedItemPosition());
                    obj.put(KEY_B, spB.getSelectedItem().toString());
                    obj.put(KEY_PB_INDEX, spPB.getSelectedItemPosition());
                    obj.put(KEY_PB, spPB.getSelectedItem().toString());
                    obj.put(KEY_HDO_CODE, etHdoCode.getText().toString());

                    String json = obj.toString();

                    DataSettingsSource dataSettingsSource = new DataSettingsSource(requireActivity());
                    dataSettingsSource.open(); // pokud DataSource má open/close
                    dataSettingsSource.setHdoWidgets(subscriptionPoint.getId(), json);
                    dataSettingsSource.close();

                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON for HDO settings", e);
                }
            }
            clearArrayLists();
            Keyboard.hide(requireActivity());
            urlBuilder();
        });

        btnSaveHdo.setOnClickListener(v -> YesNoDialogFragment.newInstance(getString(R.string.save_hdo), FLAG_RESULT_YES_NO_DIALOG_FRAGMENT, getString(R.string.save_hdo_message))
                .show(requireActivity().getSupportFragmentManager(), "saveHdo"));

        spA.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.egd_code1)));
        spB.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.egd_code1)));
        spPB.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.egd_code2)));

        if (savedInstanceState != null) {
            resultJson = savedInstanceState.getString(ARG_RESULT_JSON);
            isClearArrayLists = false;
        }
        spDistrict.setSelection(4);

        btnClipData.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            String textClip = HdoClipData.create(hdoList, spDistributionArea.getSelectedItem().toString());
            ClipData clip = ClipData.newPlainText("HDO", textClip);
            clipboard.setPrimaryClip(clip);
        });

        viewModel.getValidityDates().observe(getViewLifecycleOwner(), validityDates -> {
            if (validityDates.length == 0) return;
            setTvValidityDate(validityDates[0]);
            spDateEgd.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, validityDates));
        });

        viewModel.getHdoListModels().observe(getViewLifecycleOwner(), hdoListModels -> {
            hdoListContainer.clear();
            hdoListContainer.addAll(hdoListModels);
            if (!hdoListModels.isEmpty()) {
                setAdapter(hdoListModels.get(0).getHdoList());
                hdoList.clear();

                if (hdoListModels.get(0).getDistribution() == HdoListContainer.Distribution.EGD)
                    hdoList.addAll(hdoListModels.get(spDateEgd.getSelectedItemPosition()).getHdoList());
                else
                    hdoList.addAll(hdoListModels.get(0).getHdoList());

                if (hdoListModels.get(0).getDistribution() == HdoListContainer.Distribution.EGD) {
                    spDateEgd.setVisibility(View.VISIBLE);
                } else {
                    tvValidityDate.setVisibility(View.VISIBLE);
                }
            }
            setAdapter(hdoList);
        });

        spDateEgd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (hdoListContainer.isEmpty()) return;
                setAdapter(hdoListContainer.get(position).getHdoList());
                hdoList.clear();
                hdoList.addAll(hdoListContainer.get(position).getHdoList());
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (getSubscriptionPoint() == null) {
            btnSaveHdo.setEnabled(false);
        }

        SubscriptionPointModel subscriptionPoint = getSubscriptionPoint();
        if (subscriptionPoint != null) {
            DataSettingsSource dataSettingsSource = new DataSettingsSource(requireActivity());
            dataSettingsSource.open();
            String json = dataSettingsSource.loadHdoWidgets(subscriptionPoint.getId());
            dataSettingsSource.close();

            if (json != null && !json.isEmpty()) {
                try {
                    JSONObject obj = new JSONObject(json);
                    int distIndex = obj.optInt(KEY_DISTRIBUTION_AREA_INDEX, -1);
                    if (distIndex >= 0 && distIndex < spDistributionArea.getCount()) {
                        spDistributionArea.setOnItemSelectedListener(null);
                        spDistributionArea.setSelection(distIndex);
                        setSpDistrict(distIndex);
                        hideWidgets(distIndex);

                        final int districtIndex = obj.optInt(KEY_DISTRICT_INDEX, -1);
                        spDistrict.post(() -> {
                            if (districtIndex >= 0 && spDistrict.getAdapter() != null && districtIndex < spDistrict.getAdapter().getCount()) {
                                spDistrict.setSelection(districtIndex);
                            }
                            spDistributionArea.setOnItemSelectedListener(distributionAreaListener);
                        });

                        int aIndex = obj.optInt(KEY_A_INDEX, -1);
                        if (aIndex >= 0 && aIndex < spA.getCount()) spA.setSelection(aIndex);
                        int bIndex = obj.optInt(KEY_B_INDEX, -1);
                        if (bIndex >= 0 && bIndex < spB.getCount()) spB.setSelection(bIndex);
                        int pbIndex = obj.optInt(KEY_PB_INDEX, -1);
                        if (pbIndex >= 0 && pbIndex < spPB.getCount()) spPB.setSelection(pbIndex);
                        String hdoCode = obj.optString(KEY_HDO_CODE, "");
                        if (!hdoCode.isEmpty()) etHdoCode.setText(hdoCode);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON for HDO settings", e);
                }
            }
        }

    }


    @Override
    public void onResume() {
        super.onResume();

        //handler pro povolení vymazání obsahu arraylistu
        Message message = new Message();
        message.what = 300;
        handler.sendMessageDelayed(message, 1000);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_RESULT_JSON, resultJson);
    }


    /**
     * Skryje nepotřebná formulářová pole podle výběru distribuční sítě
     *
     * @param item Index vybrané distribuční sítě (0-CEZ, 1-EON, 2- PRE)
     */
    private void hideWidgets(int item) {
        // DIST_CEZ, DIST_EGD, DIST_PRE
        switch (item) {
            case DIST_CEZ:
                lnCode.setVisibility(View.VISIBLE);
                lnCodesEdg.setVisibility(View.GONE);
                spDateEgd.setVisibility(View.GONE);
                tvValidityDate.setVisibility(View.VISIBLE);
                break;
            case DIST_EGD:
                lnCode.setVisibility(View.VISIBLE);
                lnCodesEdg.setVisibility(View.VISIBLE);
                spDateEgd.setVisibility(View.VISIBLE);
                tvValidityDate.setVisibility(View.GONE);
                break;
            case DIST_PRE:
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
        // DIST_CEZ, DIST_EGD, DIST_PRE
        viewModel.runAsyncOperation(url, distributionAreaIndex, etHdoCode.getText().toString(), requireActivity(), spDistrict.getSelectedItem().toString(), spDistrict.getSelectedItemPosition(), lnHdoButtons);
    }


    /**
     * Nastaví adapter pro RecyclerView a spustí layout animaci.
     *
     * @param hdoModels seznam {@link HdoModel} který se má zobrazit
     */
    private void setAdapter(ArrayList<HdoModel> hdoModels) {
        HdoAdapter hdoAdapter = new HdoAdapter(hdoModels, rvHdoSite, false);
        rvHdoSite.setAdapter(hdoAdapter);
        rvHdoSite.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvHdoSite.scheduleLayoutAnimation();
    }


    /**
     * Nastaví text platnosti
     *
     * @param date text platnosti
     */
    private void setTvValidityDate(String date) {
        tvValidityDate.setText(getResources().getString(R.string.validity, date));
    }


    /**
     * Nastaví viditelnost textového upozornění (tvAlert).
     *
     * @param b true = zobrazit upozornění, false = skrýt
     */
    private void setVisibilityAlert(boolean b) {
        if (b)
            tvAlert.setVisibility(View.VISIBLE);
        else
            tvAlert.setVisibility(View.INVISIBLE);

    }


    /**
     * Aktualizuje text upozornění podle aktuálně vybrané distribuční sítě.
     * <p>
     * Používá lokalizované řetězce z resources: pro CEZ/EON zobrazí instrukci pro psaní HDO,
     * pro PRE zobrazí instrukci pro výběr HDO ze seznamu.
     */
    private void setTextAlert() {
        switch (spDistributionArea.getSelectedItemPosition()) {
            case DIST_CEZ:
            case DIST_EGD:
                tvAlert.setText(getResources().getString(R.string.write_hdo));
                break;
            case DIST_PRE:
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
     * Sestaví text HDO kódu z hodnot vybraných ve spinnerech A, B a PB
     * a vloží jej do editTextu {@code etHdoCode}.
     * <p>
     * Pokud jsou všechny tři položky prázdné, metoda nic neprovádí.
     */
    private void setHdoCodeFromSpinners() {
        if (spA.getSelectedItem().equals("")
                && spB.getSelectedItem().equals("")
                && spPB.getSelectedItem().equals("")) return;

        etHdoCode.removeTextChangedListener(etHdoCodeTextWatcher);

        String code = "A" + spA.getSelectedItem()
                + "B" + spB.getSelectedItem()
                + "DP" + spPB.getSelectedItem();
        etHdoCode.setText(code);
    }


    /**
     * Uloží aktuálně načtený seznam HDO do lokální databáze.
     * <p>
     * Metoda ověří, že existují načtená data a že je k dispozici odběrné místo
     * s definovanou tabulkou pro HDO. V případě chyb vypíše krátké Toast hlášení.
     */
    private void saveHdo() {
        if (!hdoList.isEmpty()) {
            SubscriptionPointModel subscriptionPoint = getSubscriptionPoint();
            if (subscriptionPoint == null) {
                return;
            }

            String tableHdo = subscriptionPoint.getTableHDO();
            if (tableHdo == null || tableHdo.isEmpty()) {
                Toast.makeText(requireActivity(), "Nepodařilo se načíst údaje o odběrném místě", Toast.LENGTH_SHORT).show();
                return;
            }
            DataHdoSource dataHdoSource = new DataHdoSource(requireActivity());
            dataHdoSource.saveHdo(hdoList, tableHdo);
        } else {
            Toast.makeText(requireActivity(), "Nejprve načtěte data", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Smaže obsah arraylistů
     */
    private void clearArrayLists() {
        hdoList.clear();
        hdoListContainer.clear();
        viewModel.clearData();
    }


    /**
     * Smaže obsah arraylistů, recyclerview, spinnerů
     */
    private void clear() {
        if (isClearArrayLists) {
            clearArrayLists();
            setAdapter(new ArrayList<>());
            spDateEgd.setVisibility(View.GONE);
            tvValidityDate.setVisibility(View.GONE);
        }
    }


    /**
     * Vrátí odběrné místo
     *
     * @return odběrné místo
     */
    private SubscriptionPointModel getSubscriptionPoint() {
        return SubscriptionPoint.load(requireActivity());
    }


    /**
     * Naplní spinner seznamem okresů podle vybrané distribuční sítě.
     *
     * @param position index distribuční sítě (DIST_CEZ, DIST_EGD, DIST_PRE)
     */
    private void setSpDistrict(int position) {
        switch (position) {
            case DIST_CEZ:
                spDistrict.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.area_cez)));
                break;
            case DIST_EGD:
                spDistrict.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.area_egd)));
                break;
            case DIST_PRE:
                spDistrict.setAdapter(new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.area_pre)));
                break;
        }
    }


    /**
     * Třída HdoListContainer slouží k ukládání seznamu HDO, data platnosti a distribuční oblasti.
     * <p>
     * Tato třída poskytuje metody pro získání seznamu HDO a distribuční oblasti.
     * <p>
     * Klíčové komponenty:
     * - ArrayList\<HdoModel\> hdoList: Seznam HDO modelů.
     * - String validityDate: Datum platnosti HDO.
     * - Distribution distribution: Distribuční oblast (CEZ, EGD, PRE).
     * <p>
     * Metody:
     * - HdoListContainer(ArrayList\<HdoModel\> hdoList, String validityDate, Distribution distributionArea): Konstruktor pro inicializaci seznamu HDO, data platnosti a distribuční oblasti.
     * - ArrayList\<HdoModel\> getHdoList(): Vrací seznam HDO modelů.
     * - Distribution getDistribution(): Vrací distribuční oblast.
     * <p>
     * Enum Distribution:
     * - Představuje různé distribuční oblasti (CEZ, EGD, PRE).
     */
    static class HdoListContainer {

        ArrayList<HdoModel> hdoList;
        String validityDate;
        Distribution distribution;


        /**
         * Konstruktor pro inicializaci seznamu HDO, data platnosti a distribuční oblasti.
         *
         * @param hdoList          Seznam HDO modelů.
         * @param validityDate     Datum platnosti HDO.
         * @param distributionArea Distribuční oblast (CEZ, EGD, PRE).
         */
        public HdoListContainer(ArrayList<HdoModel> hdoList, String validityDate, Distribution distributionArea) {
            this.hdoList = hdoList;
            this.validityDate = validityDate;
            this.distribution = distributionArea;
        }


        /**
         * Vrací seznam HDO modelů.
         *
         * @return Seznam HDO modelů.
         */
        public ArrayList<HdoModel> getHdoList() {
            return hdoList;
        }


        /**
         * Vrací distribuční oblast.
         *
         * @return Distribuční oblast.
         */
        public Distribution getDistribution() {
            return distribution;
        }


        /**
         * Enum představující různé distribuční oblasti.
         */
        enum Distribution {
            CEZ, EGD, PRE
        }

    }

}
