package cz.xlisto.elektrodroid;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cz.xlisto.elektrodroid.modules.hdo.CodeWeb;
import cz.xlisto.elektrodroid.modules.hdo.WebViewClientImpl;

/**
 * Testovací {@link Fragment} pro zobrazování HTML obsahu v prvku {@link WebView}.
 * <p>
 * Slouží k vývojovému testování HTML/JavaScript funkcionalit (např. stránky HDO).
 * Konfiguruje prvek {@link WebView} s vlastními instancemi {@link WebViewClientImpl}
 * a {@link WebChromeClient}.
 */
public class TestFragment extends Fragment {

    /**
     * Výchozí prázdný konstruktor požadovaný systémem pro instanciování fragmentu.
     */
    public TestFragment() {
        // Required empty public constructor
    }

    /**
     * Tovární metoda pro vytvoření nové instance {@link TestFragment}.
     *
     * @return nová instance {@link TestFragment}
     */
    public static TestFragment newInstance() {
        return new TestFragment();
    }


    /**
     * Inflátuje vizuální rozložení (layout) fragmentu z XML předlohy {@code R.layout.test}.
     *
     * @param inflater           objekt {@link LayoutInflater} pro nafouknutí rozložení
     * @param container          rodičovský kontejner pro vložení rozložení
     * @param savedInstanceState uložený stav fragmentu při jeho obnovení
     * @return zobrazení rozložení fragmentu ({@link View})
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.test, container, false);
    }


    /**
     * Inicializuje {@link WebView} po vytvoření rozložení fragmentu.
     * <p>
     * Nastavuje klienty {@link WebViewClientImpl} a {@link WebChromeClient}, povoluje spouštění JavaScriptu
     * a načítá HTML kód z {@link CodeWeb#htmlPage}.
     *
     * @param view               vytvořené kořenové zobrazení fragmentu
     * @param savedInstanceState uložený stav fragmentu při obnovení
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WebView webView = view.findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClientImpl());
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        //webView.loadUrl("https://elektrodroid.xlisto.com/test.html");
        webView.loadData(CodeWeb.htmlPage, "text/html; charset=utf-8", "UTF-8");
        String s = webView.toString();
        Log.w("TestFragment", "onViewCreated: " + s);
    }
}
