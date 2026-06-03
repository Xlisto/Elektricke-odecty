package cz.xlisto.elektrodroid;

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
 * Testovací fragment pro zobrazení WebView s HTML obsahem.
 * <p>
 * Využívá se pro testování a vývoj HTML/JavaScript funkcionalit.
 * Obsahuje WebView, který je konfigurován s vlastním WebViewClient
 * a WebChromeClient pro lepší kontrolu chování.
 */
public class TestFragment extends Fragment {

    /**
     * Prázdný konstruktor - je vyžadován pro vytvoření instance fragmentu.
     */
    public TestFragment() {
        // Required empty public constructor
    }

    /**
     * Tovární metoda pro vytvoření nové instance TestFragmentu.
     *
     * @return nová instance TestFragmentu
     */
    public static TestFragment newInstance() {
        return new TestFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.test, container, false);
    }

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
