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

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.VersionModel;


/**
 * Fragment představující seznam položek provedených změn v aplikaci.
 */
public class AboutMeFragment extends Fragment {

    /**
     * Povinný prázdný konstruktor pro fragment manager k instanciaci
     * fragmentu (např. při změnách orientace obrazovky).
     */
    public AboutMeFragment() {
    }


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
            System.out.println(version.getVersion());
            System.out.println(version.getDate());
            for(String change : version.getChanges()) {
                System.out.println(change);
            }
        }

        tvLinkPrivatePolicy.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://elektrodroid.xlisto.com/privacy-policy/"));
            startActivity(browserIntent);
        });

        return view;
    }


    /**
     * Čte soubor z raw resources a vrací jeho obsah jako řetězec.
     *
     * @param context    Kontext aplikace.
     * @param resourceId ID resource souboru.
     * @return Obsah souboru jako řetězec.
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