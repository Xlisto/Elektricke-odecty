package cz.xlisto.odecty.modules.hdo;

import android.content.Context;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import androidx.fragment.app.FragmentActivity;
import cz.xlisto.odecty.dialogs.OwnAlertDialog;
import cz.xlisto.odecty.ownview.ViewHelper;

/**
 * Xlisto 19.06.2023 12:27
 */
public class Connections {
    private static final String TAG = "Connections";

    private OnLoadResultDataListener onLoadResultDataListener;


    /**
     * Načte obsah www stránky EGD
     *
     * @param urlString     url api adresy
     * @param urlParameters parametry dotazu
     * @param context       kontext aplikace pro zobrazení Toastu při chybě
     */
    public void sendPostParameters(String urlString, String urlParameters, Context context, Spinner spDistrict) {
        URL url = builderURL(urlString);
        HttpsURLConnection connection = builderConnection(url);
        JSONObject jsonParam = null;
        try {
            //hlavička
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Host", "api.egd.cz");

            //parametry v hlavičce dotaz
            jsonParam = new JSONObject(urlParameters);
            // odeslání parametrů do hlavičky
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        InputStream in = readInputStream(connection, context);
        String result = readerStream(in);

        onLoadResultDataListener.onLoadResultData(result);
        try {
            if (jsonParam != null) {
                if (jsonParam.get("operationName").toString().equals("searchKod")) {
                    test2(result, spDistrict, urlString, context);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Odešle dotaz za zobrazení HDO časů. Výsledek je html sránka
     *
     * @param urlString             url api adresy
     * @param distributionAreaIndex index distribuční oblasti (0-CEZ, 1-EON, 2- PRE)
     * @param context               kontext aplikace pro zobrazení Toastu při chybě
     */
    public void sendPost(String urlString, int distributionAreaIndex, String code, Context context, Spinner spDistrict, LinearLayout root) {
        //0-CEZ, 1-EON, 2- PRE
        //načte komplet www stránku ze které potom parsuji potřebná data
        URL url = builderURL(urlString);
        HttpsURLConnection connection = builderConnection(url);
        InputStream in = readInputStream(connection, context);

        try {
            if (distributionAreaIndex == 0) parseJSONCEZ(in);
            if (distributionAreaIndex == 1) parseEGD(in, code, context, spDistrict, root);
            if (distributionAreaIndex == 2) parseJSONPRE(in);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Sestaví URL
     *
     * @param urlString url adresa ve Stringu
     * @return URL adresy
     */
    private URL builderURL(String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }


    /**
     * Naváže spojení
     *
     * @param url URL adresa
     * @return HttpsURLConnection připojení
     */
    private HttpsURLConnection builderConnection(URL url) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connection;
    }


    /**
     * Načte stream
     *
     * @return InputStream načítané stránky
     */
    private InputStream readInputStream(HttpsURLConnection connection, Context context) {
        InputStream in = null;
        try {
            in = new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            OwnAlertDialog.show(context, "Varování", "Nejste připojeni k internetu");
        }
        return in;
    }


    /**
     * Načte stream z CEZ a sestaví JSON pole
     *
     * @param in stream
     * @throws JSONException chyba při parsování JSONu
     */
    private void parseJSONCEZ(InputStream in) throws JSONException {
        String readerStream = readerStream(in);
        JSONArray jsonArray = new JSONArray(readerStream);
        onLoadResultDataListener.onLoadResultData(jsonArray.toString());
    }


    /**
     * Načte stream z EGD a nalezne token
     *
     * @param in InputStream
     */
    private void parseEGD(InputStream in, String code, Context context, Spinner spDistrict, LinearLayout root) {
        String readerStream = readerStream(in);

        String apiUrl, apiToken, apiAuthBasic;

        org.jsoup.nodes.Document doc = Jsoup.parse(readerStream);//tagy www
        Elements scripts = doc.getElementsByTag("script");//vyhledá všechny tagy scripty
        for (int i = 0; i < scripts.size(); i++) {
            if (scripts.get(i).data().contains("\"api_token\"")) {
                try {
                    JSONObject jsonObject = new JSONObject(scripts.get(i).data());
                    JSONObject jsonEon = jsonObject.getJSONObject("eon");
                    JSONObject jsonHDO = jsonEon.getJSONObject("HDO");
                    apiUrl = jsonHDO.get("api_url").toString();
                    apiToken = jsonHDO.get("api_token").toString();
                    apiAuthBasic = jsonHDO.get("api_authbasic").toString();
                    onLoadToken(apiUrl, apiToken, apiAuthBasic, code, context, spDistrict, root);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private void onLoadToken(String apiUrl, String apiToken, String apiAuthBasic, String code, Context context, Spinner spDistrict, LinearLayout root) {

        String page = CodeWeb.htmlPage.replace("***12345***", apiToken);

        ((FragmentActivity) context).runOnUiThread(() -> {
            final String[] urlHdo = {""};
            WebView webView = new WebView(context);
            WebViewClientImpl webViewClient = new WebViewClientImpl();
            WebAppInterface webAppInterface = new WebAppInterface();
            webAppInterface.setOnSaveUrlListener(url -> urlHdo[0] = urlHdo[0] + url);

            webView.setWebViewClient(webViewClient);
            webView.setWebChromeClient(new WebChromeClient());
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearMatches();
            webView.clearFormData();
            webView.addJavascriptInterface(webAppInterface, "Android");
            webView.loadData(page, "text/html", "UTF-8");
            webViewClient.setOnPageFinishedListener(() -> searchKod(urlHdo[0], code, context, spDistrict));
            root.addView(webView);
            webView.setVisibility(View.GONE);

        });
    }

    private void searchKod(String urlHdo, String code, Context context, Spinner spDistrict) {
        if (urlHdo.endsWith("/")) return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            //String code = etHdoCode.getText().toString();
            String urlParameters = "{\"operationName\":\"searchKod\",\"variables\":{\"input\":{\"kod\":\"" + code + "\",\"limit\":10}}," +
                    "\"query\":\"query searchKod($input: HdoSearchKodInput!) {hdo { searchKod(input: $input) {kod varianta kategorieSkupiny {kategorie skupina kody __typename}__typename}__typename}}\"}";
            sendPostParameters(urlHdo, urlParameters, context, spDistrict);
        });
    }


    /**
     * Načte stream z PRE a sestaví JSON pole
     *
     * @param in InputStream stránky
     */
    private void parseJSONPRE(InputStream in) {
        String readerStream = readerStream(in);

        org.jsoup.nodes.Document doc = Jsoup.parse(readerStream);//tagy www
        Elements options = doc.getElementsByTag("option");//vyhledá všechny tagy option
        Elements selectedOption = options.select("[selected]");//vyhledá všechny tagy option s atributem selected
        Elements tbody = doc.getElementsByTag("tbody");//vyhledá tělo tabulky - je jen jedna, lze hledat podle tagu
        Elements tr = tbody.select("tr");//vyhledá řádky tabulky
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < tr.size(); i++) {
            JSONObject jsonObject = new JSONObject();

            Elements td = tr.get(i).select("td");//vyhledá buňky v řádku
            String[] times = td.get(1).text().split(", ");
            if (i > 0)
                calendar.add(Calendar.DATE, 1);
            try {
                jsonObject.put("platnost", ViewHelper.convertLongToDate(calendar.getTimeInMillis()));
                jsonObject.put("kodPovelu", selectedOption.text());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            for (int j = 0; j <= 10; j++) {
                try {
                    if (j < times.length) {
                        String[] time = times[j].split("-");
                        jsonObject.put("casZap" + j, time[0]);
                        jsonObject.put("casVyp" + j, time[1]);
                    } else {
                        jsonObject.put("casZap" + j, "");
                        jsonObject.put("casVyp" + j, "");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            jsonArray.put(jsonObject);

        }

        onLoadResultDataListener.onLoadResultData(jsonArray.toString());
    }


    /**
     * Přečte stream a uloží jej do stringu
     *
     * @param in InputStream
     * @return String načtené stránky
     */
    private String readerStream(InputStream in) {
        if (in == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                result.append(line); //zde je uložená komplet www stránka
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }


    /**
     * Nastavení posluchače pro načtení finálních dat HDO
     *
     * @param onLoadResultDataListener posluchač pro načtení finálních dat HDO
     */
    public void setOnLoadResultDataListener(OnLoadResultDataListener onLoadResultDataListener) {
        this.onLoadResultDataListener = onLoadResultDataListener;
    }

    private void test2(String jsonDataString, Spinner spDistrict, String urlHdo, Context context) {

        String category = getCategoryEgd(spDistrict);
        String group = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonDataString);
            JSONObject jsonData = jsonObject.getJSONObject("data");
            JSONObject jsonHdo = jsonData.getJSONObject("hdo");
            JSONArray jsonSearchKod = jsonHdo.getJSONArray("searchKod");
            for (int i = 0; i < jsonSearchKod.length(); i++) {
                JSONObject jsonKod = jsonSearchKod.getJSONObject(i);
                JSONArray jsonKategorieSkupiny = jsonKod.getJSONArray("kategorieSkupiny");
                for (int j = 0; j < jsonKategorieSkupiny.length(); j++) {
                    JSONObject jsonKategorieSkupina = jsonKategorieSkupiny.getJSONObject(j);
                    if (jsonKategorieSkupina.getString("kategorie").equals(category))
                        group = jsonKategorieSkupina.getString("skupina");
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();

        String finalGroup = group;
        executor.execute(() -> {
            String urlParameters = "{\"operationName\": \"search\",\"variables\": {\"input\": {\"kategorie\": \"" + category + "\",\"skupina\": \"" + finalGroup + "\"}}," +
                    "\"query\": \"query search($input: HdoSearchInput!) { hdo { search(input: $input) { od { den mesic rok __typename}do{den mesic rok __typename}sazby{sazba dny{ denVTydnu casy { od do __typename}__typename} __typename}__typename} __typename}}\"}";

            sendPostParameters(urlHdo, urlParameters, context, spDistrict);

        });
    }


    /**
     * Posluchač na finální načtení dat HDO
     */
    interface OnLoadResultDataListener {
        void onLoadResultData(String result);
    }

    /**
     * Vrátí kategorii EGD podle vybraného okresu
     *
     * @return kategorie EGD
     */
    private String getCategoryEgd(Spinner spDistrict) {
        int itemSelectedPosition = spDistrict.getSelectedItemPosition();
        switch (itemSelectedPosition) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 22:
            case 23:
                return "VYCHOD";
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
                return "ZAPAD";
        }
        return "";
    }
}
