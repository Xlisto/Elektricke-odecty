package cz.xlisto.elektrodroid.modules.hdo;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

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

import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Třída Connections slouží k navázání komunikace se servery EGD, CEZ a PRE.
 * <p>
 * Tato třída poskytuje metody pro odesílání HTTP POST dotazů na servery a zpracování odpovědí.
 * <p>
 * Klíčové komponenty:
 * - Handler handler: Zpracovává výsledky dotazů.
 * - Handler handlerOnGroupAndCategory: Zpracovává výsledky dotazů na skupiny a kategorie.
 * <p>
 * Metody:
 * - sendPostParameters(String urlString, String urlParameters, String code, Context context, String districtName, int districtIndex, Handler handler): Odesílá POST dotaz na server.
 * - sendPost(String urlString, int distributionAreaIndex, String code, Context context, String districtName, int districtIndex, LinearLayout root, Handler handler): Odesílá POST dotaz na server a zpracovává odpověď.
 * - builderURL(String urlString): Sestaví URL z řetězce.
 * - builderConnection(URL url): Naváže HTTPS spojení.
 * - readInputStream(HttpsURLConnection connection, Context context): Načte InputStream z připojení.
 * - parseJSONCEZ(InputStream in): Načte a zpracuje JSON data z CEZ.
 * - parseJSONPRE(InputStream in): Načte a zpracuje JSON data z PRE.
 * - parseEGD(InputStream in, String code, Context context, String districtName, int districtIndex, LinearLayout root): Načte a zpracuje data z EGD.
 * - onLoadToken(String apiToken, String code, Context context, String districtName, int districtIndex, LinearLayout root): Zpracuje token pro přístup k datům.
 * - readerStream(InputStream in): Načte stream a uloží jej do řetězce.
 * - searchKod(String urlHdo, String code, Context context, String districtName, int districtIndex): Sestaví dotaz pro získání kategorie a skupiny.
 * - searchHdoParseInputDates(String jsonDataString, String districtName, int districtIndex, String code, String urlHdo, Context context, Handler handler): Zpracuje data pro získání časů HDO.
 * - searchHdo(String group, String category, String urlHdo, String code, Context context, String districtName, int districtIndex, Handler handler): Sestaví dotaz pro získání HDO dat o tarifu.
 * - getCategoryEgd(int districtIndex): Vrátí kategorii EGD podle vybraného okresu.
 * <p>
 * Vnitřní třídy:
 * - ResultData: Kontejner pro návratová data.
 * - GroupAndCategoryContainer: Kontejner pro data skupin a kategorií.
 * - ResultType: Enum pro typ výsledku získání dat.
 * <p>
 * Xlisto 19.06.2023 12:27
 */
public class Connections {

    private static final String TAG = "Connections";
    private Handler handler;
    //handler obdrží  výsledek s Category a Group a spustí další dotaz na získání časů HDO
    private final Handler handlerOnGroupAndCategory = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            GroupAndCategoryContainer groupAndCategoryContainer = (GroupAndCategoryContainer) msg.obj;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> sendPostParameters(groupAndCategoryContainer.urlHdo, groupAndCategoryContainer.urlParameters,
                    groupAndCategoryContainer.code, groupAndCategoryContainer.context, groupAndCategoryContainer.districtName, groupAndCategoryContainer.districtIndex, handler)
            );
        }
    };


    /**
     * Odešle dotaz na api.egd pro získání skupiny kodu nebo časů HDO
     *
     * @param urlString     url api adresy
     * @param urlParameters parametry dotazu
     * @param context       kontext aplikace pro zobrazení Toastu při chybě
     */
    public void sendPostParameters(String urlString, String urlParameters, String code, Context context, String districtName, int districtIndex, Handler handler) {
        URL url = builderURL(urlString);
        HttpsURLConnection connection = builderConnection(url);
        //JSONObject jsonParam = null;
        try {
            //hlavička
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Host", "api.egd.cz");
            connection.setRequestProperty("Content-Type", "application/json");

            //parametry v hlavičce dotaz
            //jsonParam = new JSONObject(urlParameters);
            // odeslání parametrů do hlavičky
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            Log.e(TAG, "sendPostParameters: ", e);
        }

        InputStream in = readInputStream(connection);
        if (in == null) {
            handler.sendEmptyMessage(101);
            return;
        }

        String result = readerStream(in);
        Message message = new Message();

        try {
            JSONObject resultJson = new JSONObject(result);
            JSONObject resultData = resultJson.getJSONObject("data");
            JSONObject resultHdo = resultData.getJSONObject("hdo");
            if (resultHdo.has("searchKod")) {
                JSONArray resultSearch = resultHdo.getJSONArray("searchKod");

                JSONObject jsonData = new JSONObject(result);
                JSONArray jsonSearchCode = jsonData.getJSONObject("data")
                        .getJSONObject("hdo")
                        .getJSONArray("searchKod");
                if (jsonSearchCode.length() > 0) {
                    JSONArray jsonCategoryGroups = jsonSearchCode.getJSONObject(0).getJSONArray("kategorieSkupiny");
                    if (jsonCategoryGroups.length() == 1) {
                        searchHdoParseInputDates(result, districtName, districtIndex, code, urlString, context, handler);
                    } else {
                        message.obj = new ResultData(resultSearch.toString(), ResultType.CODES, districtName, getCategoryEgd(districtIndex), urlString);
                        handler.sendMessage(message);
                    }
                } else {
                    message.obj = new ResultData("[]", ResultType.EGD, "", getCategoryEgd(districtIndex), urlString);
                    message.what = 100;
                    handler.sendMessage(message);
                }
            }
            if (resultHdo.has("search")) {
                JSONArray resultSearch = resultHdo.getJSONArray("search");
                message.obj = new ResultData(resultSearch.toString(), ResultType.EGD, "");
                message.what = 100;
                handler.sendMessage(message);
            }

        } catch (JSONException e) {
            Log.e(TAG, "sendPostParameters: ", e);
        }
    }


    /**
     * Odešle dotaz za zobrazení HDO časů. Výsledek je html sránka
     *
     * @param urlString             url api adresy
     * @param distributionAreaIndex index distribuční oblasti (0-CEZ, 1-EON, 2- PRE)
     * @param context               kontext aplikace pro zobrazení Toastu při chybě
     */
    public void sendPost(String urlString, int distributionAreaIndex, String code, Context context, String districtName, int districtIndex, LinearLayout root, Handler handler) {
        this.handler = handler;
        //0-CEZ, 1-EON, 2- PRE
        //načte komplet www stránku ze které potom parsuji potřebná data
        URL url = builderURL(urlString);
        HttpsURLConnection connection = builderConnection(url);
        InputStream in = readInputStream(connection);

        if (in == null) {
            handler.sendEmptyMessage(101);
            return;
        }

        try {
            if (distributionAreaIndex == 0) parseJSONCEZ(in);
            if (distributionAreaIndex == 1)
                parseEGD(in, code, context, districtName, districtIndex, root);
            if (distributionAreaIndex == 2) parseJSONPRE(in);

        } catch (JSONException e) {
            Log.e(TAG, "sendPost: ", e);
        }

    }


    /**
     * Sestaví URL
     *
     * @param urlString url adresa ve Stringu
     * @return URL adresy
     */
    private URL builderURL(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(TAG, "builderURL: ", e);
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
        if (url == null) return null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
        } catch (IOException e) {
            Log.e(TAG, "builderConnection: ", e);
        }
        return connection;
    }


    /**
     * Načte stream
     *
     * @return InputStream načítané stránky
     */
    private InputStream readInputStream(HttpsURLConnection connection) {
        InputStream in;
        try {
            in = new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "readInputStream: ", e);
            return null;
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
        JSONObject jsonRoot = new JSONObject(readerStream);
        JSONArray jsonArray = jsonRoot.getJSONArray("data");
        ResultData resultData = new ResultData(jsonArray.toString(), ResultType.CEZ, "");
        Message message = new Message();
        message.obj = resultData;
        message.what = 100;
        handler.sendMessage(message);
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
        ResultData resultData = new ResultData(jsonArray.toString(), ResultType.PRE, "");
        Message message = new Message();
        message.obj = resultData;
        message.what = 100;
        handler.sendMessage(message);
    }


    /**
     * Načte stream z EGD a nalezne token
     *
     * @param in InputStream
     */
    private void parseEGD(InputStream in, String code, Context context, String districtName, int districtIndex, LinearLayout root) {
        String readerStream = readerStream(in);

        String apiToken;

        org.jsoup.nodes.Document doc = Jsoup.parse(readerStream);//tagy www
        Elements scripts = doc.getElementsByTag("script");//vyhledá všechny tagy scripty

        for (int i = 0; i < scripts.size(); i++) {
            if (scripts.get(i).data().contains("\"api_token\"")) {
                try {
                    JSONObject jsonObject = new JSONObject(scripts.get(i).data());
                    JSONObject jsonEon = jsonObject.getJSONObject("eon");
                    JSONObject jsonHDO = jsonEon.getJSONObject("HDO");
                    //apiUrl = jsonHDO.get("api_url").toString();
                    apiToken = jsonHDO.get("api_token").toString();

                    onLoadToken(apiToken, code, context, districtName, districtIndex, root);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    /**
     * Zobrazení html stránky s javascripty pro načtení tokenu
     *
     * @param apiToken      token pro přístup k datům
     * @param code          kód povelu HDO
     * @param context       kontext aplikace
     * @param districtName  název okresu
     * @param districtIndex index okresu
     * @param root          kořenový layout pro zobrazení webview
     */
    private void onLoadToken(String apiToken, String code, Context context, String districtName, int districtIndex, LinearLayout root) {

        String page = CodeWeb.htmlPage.replace("***12345***", apiToken);

        ((FragmentActivity) context).runOnUiThread(() -> {
            final String[] urlHdo = {""};
            WebView webView = new WebView(context);
            //LinearLayoutCompat.LayoutParams params = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            //webView.setLayoutParams(params);
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
            webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null);

            webViewClient.setOnPageFinishedListener(() ->
                    searchKod(urlHdo[0], code, context, districtName, districtIndex)
            );
            root.addView(webView);
            webView.setVisibility(View.GONE);
        });
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
            Log.e(TAG, "readerStream: ", e);
        }
        return result.toString();
    }


    /**
     * Sestaví dotaz na EGD pro získání kategorie a skupiny
     *
     * @param urlHdo        url HDO
     * @param code          kód
     * @param context       kontext aplikace
     * @param districtName  název okresu
     * @param districtIndex index okresu
     */
    private void searchKod(String urlHdo, String code, Context context, String districtName, int districtIndex) {
        if (urlHdo.endsWith("/")) return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String urlParameters = "{\"operationName\":\"searchKod\",\"variables\":{\"input\":{\"kod\":\"" + code + "\",\"limit\":10}}," +
                    "\"query\":\"query searchKod($input: HdoSearchKodInput!) {hdo { searchKod(input: $input) {kod varianta kategorieSkupiny {kategorie skupina kody __typename}__typename}__typename}}\"}";
            GroupAndCategoryContainer groupAndCategoryContainer = new GroupAndCategoryContainer(urlHdo, urlParameters, code, context, districtName, districtIndex);
            Message message = new Message();
            message.obj = groupAndCategoryContainer;
            message.what = 200;
            handlerOnGroupAndCategory.sendMessage(message);
            //sendPostParameters(urlHdo, urlParameters, code, context, spDistrict);
        });
    }


    /**
     * Z dotazu na EGD získá kategorie a skupiny, potřebný na druhý dotaz pro získání časů HDO
     *
     * @param jsonDataString json data;
     * @param districtName   název okresu
     * @param districtIndex  index okresu
     * @param code           kód HDO
     * @param urlHdo         url HDO
     * @param context        kontext aplikace
     */
    private void searchHdoParseInputDates(String jsonDataString, String districtName, int districtIndex, String code, String urlHdo, Context context, Handler handler) {
        String category = getCategoryEgd(districtIndex);
        String group = "";
        ResultData resultDataObj = new ResultData(jsonDataString, ResultType.CODES, "", getCategoryEgd(districtIndex), urlHdo);
        Message message = new Message();
        message.obj = resultDataObj;
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
                    if (jsonKategorieSkupina.getString("kategorie").equals("PH"))
                        category = "PH";
                    if (jsonKategorieSkupina.getString("kategorie").equals("SM"))
                        category = "SM";
                    if (jsonKategorieSkupina.getString("kategorie").equals("TOU"))
                        category = "TOU";
                    if (jsonKategorieSkupina.getString("kategorie").equals(category)) {
                        group = jsonKategorieSkupina.getString("skupina");
                        if (jsonKategorieSkupina.getString("kategorie").equals("SM"))
                            group = code.toUpperCase();
                    }
                }
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        searchHdo(group, category, urlHdo, code, context, districtName, districtIndex, handler);

    }


    /**
     * Sestaví dotaz pro server pro získání HDO dat o tarifu
     *
     * @param group         skupina tarifu
     * @param category      kategorie tarifu
     * @param urlHdo        url serveru
     * @param code          kód tarifu
     * @param context       kontext aplikace
     * @param districtName  název okresu
     * @param districtIndex index okresu
     */
    public void searchHdo(String group, String category, String urlHdo, String code, Context context, String districtName, int districtIndex, Handler handler) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String urlParameters = "{\"operationName\": \"search\",\"variables\": {\"input\": {\"kategorie\": \"" + category + "\",\"skupina\": \"" + group + "\"}}," +
                    "\"query\": \"query search($input: HdoSearchInput!) { hdo { search(input: $input) { od { den mesic rok __typename}do{den mesic rok __typename}sazby{sazba dny{ denVTydnu casy { od do __typename}__typename} __typename}__typename} __typename}}\"}";

            sendPostParameters(urlHdo, urlParameters, code, context, districtName, districtIndex, handler);
        });
    }


    /**
     * Vrátí kategorii EGD podle vybraného okresu
     *
     * @return kategorie EGD
     */
    private String getCategoryEgd(int districtIndex) {
        //int itemSelectedPosition = spDistrict.getSelectedItemPosition();
        switch (districtIndex) {
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


    /**
     * Kontejner pro návratová data
     */
    static class ResultData {

        String result, areaEgd, area, urlString;
        ResultType resultType;


        public ResultData(String result, ResultType resultType, String area) {
            this(result, resultType, area, "", "");
        }


        public ResultData(String result, ResultType resultType, String area, String areaEgd, String urlString) {
            this.result = result;
            this.resultType = resultType;
            this.areaEgd = areaEgd;
            this.urlString = urlString;
            this.area = area;
        }

    }


    /**
     * Typ výsledku získání dat
     */
    enum ResultType {
        CEZ,
        EGD,
        PRE,
        CODES
    }


    static class GroupAndCategoryContainer {

        String urlHdo, urlParameters, code, districtName;
        Context context;
        int districtIndex;


        public GroupAndCategoryContainer(String urlHdo, String urlParameters, String code, Context context, String districtName, int districtIndex) {
            this.urlHdo = urlHdo;
            this.urlParameters = urlParameters;
            this.code = code;
            this.context = context;
            this.districtName = districtName;
            this.districtIndex = districtIndex;
        }

    }

}
