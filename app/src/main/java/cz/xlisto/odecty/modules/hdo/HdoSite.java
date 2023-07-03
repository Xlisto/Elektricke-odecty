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


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cz.xlisto.odecty.R;


/**
 * Xlisto 15.06.2023 21:28
 */
public class HdoSite extends Fragment {
    private static final String TAG = "HdoSite";
    private LinearLayout lnCode, lnCodesEdg;
    private Spinner spDistributionArea, spDistrict;
    private EditText etHdoCode;
    private final Connections connections = new Connections();

    public static HdoSite newInstance() {
        return new HdoSite();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        etHdoCode = view.findViewById(R.id.etHdoCode);
        lnCode = view.findViewById(R.id.lnCode);
        lnCodesEdg = view.findViewById(R.id.lnCodeEgd);
        RecyclerView rvHdoSite = view.findViewById(R.id.rvHdoSite);


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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnHdoSite.setOnClickListener(v -> openHdoSite());

        btnHdoLoadData.setOnClickListener(v -> urlBuilder());

        hideWidgets(0);
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
                break;
            case 1:
                lnCode.setVisibility(View.VISIBLE);
                lnCodesEdg.setVisibility(View.VISIBLE);
                break;
            case 2:
                lnCode.setVisibility(View.GONE);
                lnCodesEdg.setVisibility(View.GONE);
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

    private void urlBuilder() {
        UrlBuilder urlBuilder = new UrlBuilder(spDistributionArea, spDistrict, etHdoCode);
        loadData(urlBuilder.buildUrl(), spDistributionArea.getSelectedItemPosition());
    }

    private void loadData(String url, int distributionAreaIndex) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        //0-CEZ,1-EGD,2-PRE

        connections.setOnLoadResultDataListener(new Connections.OnLoadResultDataListener() {
            @Override
            public void onLoadResultData(String result) {
              Log.w(TAG, "onLoadResultData: " + result);
            }
        });



        executor.execute(() -> {

            //Background work here
            connections.sendPost(url, distributionAreaIndex, etHdoCode.getText().toString(), requireActivity(), spDistrict, lnCode);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    //UI Thread work here

                }
            });
        });
    }




}
