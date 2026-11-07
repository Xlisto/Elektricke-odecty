package cz.xlisto.elektrodroid.modules.aboutme;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.VersionModel;
import cz.xlisto.elektrodroid.utils.AppReviewHelper;


/**
 * Fragment zobrazující informace o aplikaci, historii verzí a seznam provedených změn.
 *
 * <p>Obsahuje také odkaz na zásady ochrany osobních údajů, vyvolání hodnocení aplikace v Google Play
 * a ruční kontrolu dostupnosti aktualizací.</p>
 */
public class AboutMeFragment extends Fragment {

    /**
     * Vytvoří novou instanci {@link AboutMeFragment}.
     * Vyžadováno architekturou {@link Fragment} (např. při změně orientace obrazovky).
     */
    public AboutMeFragment() {
    }


    /**
     * Tovární metoda pro vytvoření nové instance fragmentu O aplikaci.
     *
     * @return nová instance {@link AboutMeFragment}
     */
    public static AboutMeFragment newInstance() {
        AboutMeFragment fragment = new AboutMeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    /**
     * Inicializuje uživatelské rozhraní fragmentu, načte historii verzí ze zdrojů
     * a nastavení obsluhy akcí (zásady ochrany soukromí, hodnocení aplikace, kontrola aktualizací).
     *
     * @param inflater           objekt pro nafouknutí rozvržení
     * @param container          rodičovský kontejner (může být {@code null})
     * @param savedInstanceState uložený stav instance (může být {@code null})
     * @return zobrazení rozvržení fragmentu {@link View}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_me, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.list);
        TextView tvLinkPrivatePolicy = view.findViewById(R.id.tvLinkPrivatePolicy);

        List<VersionModel> versions = readRawFile(requireContext(), R.raw.version);

        Context context = view.getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        recyclerView.setAdapter(new MyItemAboutMeRecyclerViewAdapter(versions));

        for(VersionModel version : versions) {
            System.out.println(version.version());
            System.out.println(version.date());
            for (String change : version.changes()) {
                System.out.println(change);
            }
        }

        tvLinkPrivatePolicy.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://elektrodroid.xlisto.com/privacy-policy/"));
            startActivity(browserIntent);
        });

        TextView tvRateApp = view.findViewById(R.id.tvRateApp);
        if (tvRateApp != null) {
            tvRateApp.setOnClickListener(v -> AppReviewHelper.requestReview(requireActivity(), true));
        }

        TextView tvCheckUpdate = view.findViewById(R.id.tvCheckUpdate);
        if (tvCheckUpdate != null) {
            tvCheckUpdate.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getAppUpdateHelper().checkForUpdate(true);
                }
            });
        }

        return view;
    }


    /**
     * Načte a parsuje JSON soubor historie verzí ze surových zdrojů (raw resources).
     *
     * @param context    kontext aplikace ({@link Context}) pro přístup ke zdrojům
     * @param resourceId identifikátor zdroje (např. {@code R.raw.version})
     * @return seznam objektů {@link VersionModel} představující jednotlivé verze aplikace
     */
    public static List<VersionModel> readRawFile(Context context, int resourceId) {

        List<VersionModel> updateInfoList = new ArrayList<>();
        try (InputStream inputStream = context.getResources().openRawResource(resourceId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            Gson gson = new Gson();
            Type listType = new TypeToken<List<VersionModel>>() {
            }.getType();
            updateInfoList = gson.fromJson(reader, listType);
        } catch (Exception e) {
            Logger.getGlobal().severe("Error while reading raw file: " + e.getMessage());
        }

        return updateInfoList;
    }

}