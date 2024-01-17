package cz.xlisto.odecty.modules.hdo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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


import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.dialogs.SpinnerDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.utils.Keyboard;
import cz.xlisto.odecty.utils.SubscriptionPoint;


/**
 * Xlisto 15.06.2023 21:28
 */
public class HdoSiteFragment extends Fragment {
    private static final String TAG = "HdoSiteFragment";
    private static final String FLAG_RESULT_SPINNER_DIALOG_FRAGMENT = "flagResultSpinnerDialogFragment";
    private static final String FLAG_RESULT_YES_NO_DIALOG_FRAGMENT = "flagResultYesNoDialogFragment";
    private static final String ARG_RESULT_JSON = "resultJson";
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
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            if (msg.what == 300) {
                isClearArrayLists = true;
            }
        }
    };


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
        viewModel.setShowAlert();


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

        viewModel.shouldShowDialog().observe(this, shouldShowDialog -> {
            if (shouldShowDialog) {
                SpinnerDialogFragment.newInstance(getResources().getString(R.string.select), codes, codesException, groupsList, groupsExceptionList, resultArea, exceptionArea, FLAG_RESULT_SPINNER_DIALOG_FRAGMENT)
                        .show(getChildFragmentManager(), "spinnerDialog");
            }
            viewModel.hideDialog();
        });

        viewModel.shouldShowAlert().observe(this, shouldShowAlert -> {
            setVisibilityAlert(shouldShowAlert);
            if (shouldShowAlert) {
                setVisibilityProgressBar(false);
                //Toast.makeText(requireActivity(), getResources().getString(R.string.no_hdo_codes), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.shouldShowProgress().observe(this, this::setVisibilityProgressBar);

        if (savedInstanceState != null) {
            isClearArrayLists = false;
        }
    }


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

                viewModel.runSecondAsyncOperation(group, category, urlHdo, code, requireActivity(), spDistrict.getSelectedItem().toString(), spDistrict.getSelectedItemPosition());
            } //else {
            //setVisibilityProgressBar(false);
            //}
            viewModel.hideDialog();
            //executor = null;
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
                clear();
                setTextAlert();
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

        btnHdoSite.setOnClickListener(v -> openHdoSite());

        btnHdoLoadData.setOnClickListener(v -> {
            clearArrayLists();
            Keyboard.hide(requireActivity());
            urlBuilder();
        });

        btnSaveHdo.setOnClickListener(v -> YesNoDialogFragment.newInstance(getString(R.string.save_hdo), FLAG_RESULT_YES_NO_DIALOG_FRAGMENT, getString(R.string.save_hdo_message))
                .show(requireActivity().getSupportFragmentManager(), "saveHdo"));


        //hideWidgets(0);

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
            if (hdoListModels.size() > 0) {
                setAdapter(hdoListModels.get(0).getHdoList());
                hdoList.clear();
                //hdoList.addAll(hdoListModels.get(spDateEgd.getSelectedItemPosition()).getHdoList());
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
                if (hdoListContainer.size() == 0) return;
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

        //executor = Executors.newSingleThreadExecutor();
        //0-CEZ,1-EGD,2-PRE
        viewModel.runAsyncOperation(url, distributionAreaIndex, etHdoCode.getText().toString(), requireActivity(), spDistrict.getSelectedItem().toString(), spDistrict.getSelectedItemPosition(), lnHdoButtons);
    }


    /**
     * Nastaví adapter pro recycler view
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
     * Nastaví viditelnost upozornění a skryje text platnosti
     */
    private void setVisibilityAlert(boolean b) {
        if (b)
            tvAlert.setVisibility(View.VISIBLE);
        else
            tvAlert.setVisibility(View.INVISIBLE);


    }


    /**
     * Nastaví text upozornění podle vybraného distributora
     */
    private void setTextAlert() {
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
        if (spA.getSelectedItem().equals("")
                && spB.getSelectedItem().equals("")
                && spPB.getSelectedItem().equals("")) return;

        String code = "A" + spA.getSelectedItem()
                + "B" + spB.getSelectedItem()
                + "DP" + spPB.getSelectedItem();
        etHdoCode.setText(code);
    }


    /**
     * Uloží HDO do databáze
     */
    private void saveHdo() {
        if (hdoList.size() > 0) {
            SubscriptionPointModel subscriptionPoint = getSubscriptionPoint();
            if (subscriptionPoint == null) {
                return;
            }

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


    static class HdoListContainer {
        ArrayList<HdoModel> hdoList;
        String validityDate;
        Distribution distribution;


        public HdoListContainer(ArrayList<HdoModel> hdoList, String validityDate, Distribution distributionArea) {
            this.hdoList = hdoList;
            this.validityDate = validityDate;
            this.distribution = distributionArea;
        }


        public ArrayList<HdoModel> getHdoList() {
            return hdoList;
        }


        public Distribution getDistribution() {
            return distribution;
        }


        enum Distribution {
            CEZ, EGD, PRE
        }
    }
}
