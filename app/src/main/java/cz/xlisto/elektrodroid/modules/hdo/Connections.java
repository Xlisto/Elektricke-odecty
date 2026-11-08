package cz.xlisto.elektrodroid.modules.hdo;


import android.annotation.SuppressLint;
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
import org.jsoup.nodes.Document;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Třída {@code Connections} slouží k navázání síťové komunikace se servery distribučních společností (ČEZ, EG.D, PRE)
 * a zpracování dat HDO (hromadné dálkové ovládání).
 * <p>
 * Zajišťuje odesílání HTTP/HTTPS POST požadavků, parsování JSON a HTML odpovědí,
 * extrakci tokenů přes skrytý {@link WebView} a sestavování GraphQL dotazů pro získání platných časů spínání tarifů.
 */
public class Connections {

    /**
     * Logovací značka pro třídu Connections
     */
    private static final String TAG = "Connections";

    /** Handler pro doručování finálních výsledků volajícímu rozhraní */
    private Handler handler;

    /** Handler obdrží výsledek s kategorií a skupinou a spustí následný dotaz na získání časů HDO na novém vlákně */
    private final Handler handlerOnGroupAndCategory = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            GroupAndCategoryContainer groupAndCategoryContainer = (GroupAndCategoryContainer) msg.obj;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> sendPostParameters(groupAndCategoryContainer.urlHdo, groupAndCategoryContainer.urlParameters,
                    groupAndCategoryContainer.code, groupAndCategoryContainer.context, groupAndCategoryContainer.districtName, groupAndCategoryContainer.districtIndex, handler)
            );
        }
    };


    /**
     * Odešle HTTP POST požadavek na GraphQL API EG.D pro získání skupiny kódu nebo časů spínání HDO.
     *
     * @param urlString     URL adresa API rozhraní
     * @param urlParameters parametry POST požadavku ve formátu JSON
     * @param code          kód povelu HDO
     * @param context       kontext aplikace
     * @param districtName  název vybraného okresu
     * @param districtIndex index vybraného okresu
     * @param handler       {@link Handler} pro zaslání výsledné zprávy (kód 100 pro úspěch, 101 pro chybu)
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
            if (resultJson.has("errors")) {
                handler.sendEmptyMessage(101);
                return;
            }
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
     * Odešle HTTP požadavek na zadanou URL adresu distribuční společnosti a zahájí parsování odpovědi podle distribuční oblasti.
     *
     * @param urlString             URL adresa serveru distribuční společnosti
     * @param distributionAreaIndex index distribuční oblasti (0 = ČEZ, 1 = EG.D, 2 = PRE)
     * @param code                  kód povelu HDO
     * @param context               kontext aplikace
     * @param districtName          název vybraného okresu
     * @param districtIndex         index vybraného okresu
     * @param root                  kořenový {@link LinearLayout} pro skryté vložení {@link WebView} (vyžadováno u EG.D)
     * @param handler               {@link Handler} pro vrácení výsledku volajícímu
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
     * Sestaví objekt {@link URL} z textového řetězce.
     *
     * @param urlString URL adresa ve tvaru řetězce
     * @return vytvořený objekt {@link URL} nebo {@code null} při chybném formátu URL
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
     * Otevře HTTPS připojení k zadané URL adrese.
     *
     * @param url cílová {@link URL} adresa
     * @return vytvořený objekt {@link HttpsURLConnection} nebo {@code null} při selhání
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
     * Otevře vstupní stream pro načtení dat z HTTPS připojení.
     *
     * @param connection otevřené připojení {@link HttpsURLConnection}
     * @return instanci {@link InputStream} s odpovědí serveru nebo {@code null} při I/O chybě
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
     * Načte stream s JSON odpovědí od ČEZ a odešle sestavený {@link ResultData} přes {@link Handler}.
     *
     * @param in {@link InputStream} obsahující JSON zprávu
     * @throws JSONException při chybě parsování JSON odpovědi
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
     * Načte a vyhodnotí HTML stránku s tabulkou spínání HDO od PRE a odešle vytvořený {@link ResultData} přes {@link Handler}.
     *
     * @param in {@link InputStream} s HTML kódováním stránky
     */
    private void parseJSONPRE(InputStream in) {
        String readerStream = readerStream(in);

        Document doc = Jsoup.parse(readerStream);//tagy www
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
            String dayText = !td.isEmpty() ? td.get(0).text() : "";
            String[] times = td.get(1).text().split(", ");
            if (i > 0)
                calendar.add(Calendar.DATE, 1);
            try {
                jsonObject.put("platnost", ViewHelper.convertLongToDate(calendar.getTimeInMillis()));
                jsonObject.put("kodPovelu", selectedOption.text());
                jsonObject.put("denText", dayText);
                jsonObject.put("isHoliday", isCzechHoliday(calendar) || dayText.toLowerCase(Locale.ROOT).contains("svátek"));
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
     * Vypočítá datum Velikonoční neděle pro daný rok podle Meeusova/Jonesova/Butcherova algoritmu.
     *
     * @param year rok pro výpočet
     * @return {@link Calendar} nastavený na půlnoc (00:00:00.000) Velikonoční neděle
     */
    public static Calendar getEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int p = h + l - 7 * m + 114;
        int month = p / 31; // 3 = Březen, 4 = Duben
        int day = (p % 31) + 1;

        Calendar easter = Calendar.getInstance();
        easter.set(year, month - 1, day, 0, 0, 0);
        easter.set(Calendar.MILLISECOND, 0);
        return easter;
    }


    /**
     * Zjistí, zda zadané datum odpovídá státnímu svátku v ČR (včetně pohyblivých svátků jako Velký pátek a Velikonoční pondělí).
     *
     * @param c {@link Calendar} obsahující datum k prověření
     * @return {@code true}, pokud je zadané datum státním svátkem, jinak {@code false}
     */
    public static boolean isCzechHoliday(Calendar c) {
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        int year = c.get(Calendar.YEAR);

        // Pevné svátky
        if (day == 1 && month == 1) return true;  // Nový rok
        if (day == 1 && month == 5) return true;  // Svátek práce
        if (day == 8 && month == 5) return true;  // Den vítězství
        if (day == 5 && month == 7) return true;  // Cyril a Metoděj
        if (day == 6 && month == 7) return true;  // Jan Hus
        if (day == 28 && month == 9) return true; // Den české státnosti
        if (day == 28 && month == 10) return true; // Den vzniku ČSR
        if (day == 17 && month == 11) return true; // Den boje za svobodu a demokracii
        if (day == 24 && month == 12) return true; // Štědrý den
        if (day == 25 && month == 12) return true; // 1. svátek vánoční
        if (day == 26 && month == 12) return true; // 2. svátek vánoční

        // Pohyblivé velikonoční svátky (Velký pátek a Velikonoční pondělí)
        Calendar easterSunday = getEasterSunday(year);

        // Velikonoční pondělí (+1 den od Velikonoční neděle)
        Calendar easterMonday = (Calendar) easterSunday.clone();
        easterMonday.add(Calendar.DAY_OF_MONTH, 1);
        if (c.get(Calendar.DAY_OF_MONTH) == easterMonday.get(Calendar.DAY_OF_MONTH) &&
                c.get(Calendar.MONTH) == easterMonday.get(Calendar.MONTH)) {
            return true;
        }

        // Velký pátek (-2 dny od Velikonoční neděle) - svátek v ČR od roku 2016
        if (year >= 2016) {
            Calendar goodFriday = (Calendar) easterSunday.clone();
            goodFriday.add(Calendar.DAY_OF_MONTH, -2);
            return c.get(Calendar.DAY_OF_MONTH) == goodFriday.get(Calendar.DAY_OF_MONTH) &&
                    c.get(Calendar.MONTH) == goodFriday.get(Calendar.MONTH);
        }

        return false;
    }


    /**
     * Načte a zpracuje HTML kód stránky EG.D a vyhledá v něm vložený API token pro další dotazy.
     *
     * @param in            {@link InputStream} s HTML kódováním stránky
     * @param code          kód povelu HDO
     * @param context       kontext aplikace
     * @param districtName  název vybraného okresu
     * @param districtIndex index vybraného okresu
     * @param root          kořenový {@link LinearLayout} pro vložení skrytého {@link WebView}
     */
    private void parseEGD(InputStream in, String code, Context context, String districtName, int districtIndex, LinearLayout root) {
        String readerStream = readerStream(in);

        String apiToken;

        Document doc = Jsoup.parse(readerStream);//tagy www
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
     * Inicializuje skrytý {@link WebView} na UI vlákně s předaným API tokenem a vyčká na načtení dynamických skriptů.
     *
     * @param apiToken      získaný token pro přístup k API EG.D
     * @param code          kód povelu HDO
     * @param context       kontext aplikace
     * @param districtName  název vybraného okresu
     * @param districtIndex index vybraného okresu
     * @param root          kořenový {@link LinearLayout} pro přidání prvku {@link WebView}
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void onLoadToken(String apiToken, String code, Context context, String districtName, int districtIndex, LinearLayout root) {

        String page = CodeWeb.htmlPage.replace("***12345***", apiToken);

        ((FragmentActivity) context).runOnUiThread(() -> {
            try {
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
                webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null);

                webViewClient.setOnPageFinishedListener(() ->
                        searchKod(urlHdo[0], code, context, districtName, districtIndex)
                );

                root.addView(webView);
                webView.setVisibility(View.GONE);
            } catch (Throwable e) {
                Log.e(TAG, "onLoadToken: Inicializace WebView selhala (chybí balíček WebView v systému)", e);
                if (handler != null) {
                    handler.sendEmptyMessage(101);
                }
            }
        });
    }


    /**
     * Přečte kompletní obsah ze vstupního streamu a vrátí jej jako textový řetězec.
     *
     * @param in {@link InputStream} pro přečtení
     * @return načtený text jako {@link String} (pokud je {@code in} rovno {@code null}, vrátí prázdný řetězec)
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
     * Asynchronně sestaví a odešle dotaz na GraphQL API EG.D pro zjištění kategorie a skupiny povelu HDO.
     *
     * @param urlHdo        URL adresa API rozhraní
     * @param code          kód povelu HDO
     * @param context       kontext aplikace
     * @param districtName  název vybraného okresu
     * @param districtIndex index vybraného okresu
     */
    private void searchKod(String urlHdo, String code, Context context, String districtName, int districtIndex) {
        if (urlHdo.endsWith("/")) return;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String jsonData = "{\"query\":{\"kind\":\"Document\",\"definitions\":[{\"kind\":\"OperationDefinition\",\"operation\":\"query\",\"name\":{\"kind\":\"Name\",\"value\":\"searchKod\"},\"variableDefinitions\":[{\"kind\":\"VariableDefinition\",\"variable\":{\"kind\":\"Variable\",\"name\":{\"kind\":\"Name\",\"value\":\"input\"}},\"type\":{\"kind\":\"NonNullType\",\"type\":{\"kind\":\"NamedType\",\"name\":{\"kind\":\"Name\",\"value\":\"HdoSearchKodInput\"}}},\"directives\":[]}],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"hdo\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"searchKod\"},\"arguments\":[{\"kind\":\"Argument\",\"name\":{\"kind\":\"Name\",\"value\":\"input\"},\"value\":{\"kind\":\"Variable\",\"name\":{\"kind\":\"Name\",\"value\":\"input\"}}}],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"kod\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"varianta\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"kategorieSkupiny\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"kategorie\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"skupina\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"kody\"},\"arguments\":[],\"directives\":[]}]}}]}}]}}]}}],\"loc\":{\"start\":0,\"end\":315}}," +
                    "\"variables\":{\"input\":{\"kod\":\"" + code + "\",\"limit\":10}}}";

            String urlParametersEncoded = "{\n" +
                    "    \"operationName\": \"searchKod\",\n" +
                    "    \"variables\": {\n" +
                    "        \"encodedQuery\": \"" + HeaderDataEncoder.getEncodedQueryVariables(jsonData) + "\"\n" +
                    "    },\n" +
                    "    \"query\": \"query searchKod($encodedQuery: String!) {\\n  processEncodedQuery(encodedQuery: $encodedQuery)\\n}\\n\"\n" +
                    "}";

            GroupAndCategoryContainer groupAndCategoryContainer = new GroupAndCategoryContainer(urlHdo, urlParametersEncoded, code, context, districtName, districtIndex);
            Message message = new Message();
            message.obj = groupAndCategoryContainer;
            message.what = 200;
            handlerOnGroupAndCategory.sendMessage(message);
            //sendPostParameters(urlHdo, urlParameters, code, context, spDistrict);
        });
    }


    /**
     * Zpracuje odpověď dotazu {@code searchKod} z EG.D, extrahuje kategorii a skupinu a vyvolá navazující dotaz pro získání časů HDO.
     *
     * @param jsonDataString JSON data odpovědi z předchozího dotazu
     * @param districtName   název vybraného okresu
     * @param districtIndex  index vybraného okresu
     * @param code           kód povelu HDO
     * @param urlHdo         URL adresa API rozhraní
     * @param context        kontext aplikace
     * @param handler        {@link Handler} pro vrácení výstupních dat
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
     * Asynchronně sestaví a odešle GraphQL dotaz na server EG.D pro získání konkrétních časů spínání tarifu HDO.
     *
     * @param group         skupina tarifu HDO
     * @param category      kategorie tarifu HDO
     * @param urlHdo        URL adresa serveru
     * @param code          kód povelu HDO
     * @param context       kontext aplikace
     * @param districtName  název vybraného okresu
     * @param districtIndex index vybraného okresu
     * @param handler       {@link Handler} pro zaslání výsledku
     */
    public void searchHdo(String group, String category, String urlHdo, String code, Context context, String districtName, int districtIndex, Handler handler) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String jsonData = "{\"query\":{\"kind\":\"Document\",\"definitions\":[{\"kind\":\"OperationDefinition\",\"operation\":\"query\",\"name\":{\"kind\":\"Name\",\"value\":\"search\"},\"variableDefinitions\":[{\"kind\":\"VariableDefinition\",\"variable\":{\"kind\":\"Variable\",\"name\":{\"kind\":\"Name\",\"value\":\"input\"}},\"type\":{\"kind\":\"NonNullType\",\"type\":{\"kind\":\"NamedType\",\"name\":{\"kind\":\"Name\",\"value\":\"HdoSearchInput\"}}},\"directives\":[]}],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"hdo\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"search\"},\"arguments\":[{\"kind\":\"Argument\",\"name\":{\"kind\":\"Name\",\"value\":\"input\"},\"value\":{\"kind\":\"Variable\",\"name\":{\"kind\":\"Name\",\"value\":\"input\"}}}],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"od\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"den\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"mesic\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"rok\"},\"arguments\":[],\"directives\":[]}]}},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"do\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"den\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"mesic\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"rok\"},\"arguments\":[],\"directives\":[]}]}},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"sazby\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"sazba\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"dny\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"denVTydnu\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"casy\"},\"arguments\":[],\"directives\":[],\"selectionSet\":{\"kind\":\"SelectionSet\",\"selections\":[{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"od\"},\"arguments\":[],\"directives\":[]},{\"kind\":\"Field\",\"name\":{\"kind\":\"Name\",\"value\":\"do\"},\"arguments\":[],\"directives\":[]}]}}]}}]}}]}}]}}]}}],\"loc\":{\"start\":0,\"end\":620}}," +
                    "\"variables\":{\"input\":{\"kategorie\":\"" + category + "\",\"skupina\":\"" + group + "\"}}}";

            String urlParametersEncoded = "{\n" +
                    "    \"operationName\": \"search\",\n" +
                    "    \"variables\": {\n" +
                    "        \"encodedQuery\": \"" + HeaderDataEncoder.getEncodedQueryVariables(jsonData) + "\"\n" +
                    "    },\n" +
                    "    \"query\": \"query search($encodedQuery: String!) {\\n  processEncodedQuery(encodedQuery: $encodedQuery)\\n}\\n\"\n" +
                    "}";

            sendPostParameters(urlHdo, urlParametersEncoded, code, context, districtName, districtIndex, handler);
        });
    }


    /**
     * Určí kód kategorie EG.D (oblast VÝCHOD / ZÁPAD) na základě vybraného indexu okresu.
     *
     * @param districtIndex index vybraného okresu
     * @return řetězec s kódovým označením kategorie ({@code "VYCHOD"}, {@code "ZAPAD"} nebo prázdný řetězec)
     */
    private String getCategoryEgd(int districtIndex) {
        //int itemSelectedPosition = spDistrict.getSelectedItemPosition();
        return switch (districtIndex) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 22, 23 -> "VYCHOD";
            case 14, 15, 16, 17, 18, 19, 20, 21 -> "ZAPAD";
            default -> "";
        };
    }


    /**
     * Datový kontejner (record) uchovávající výsledky načítání HDO dat.
     *
     * @param result     JSON řetězec s naparsovanými výslednými daty
     * @param areaEgd    kód oblasti EG.D (např. VÝCHOD/ZÁPAD)
     * @param area       identifikátor oblasti nebo název okresu
     * @param urlString  URL adresa, ze které byla data stažena
     * @param resultType typ výsledku podle distribuční společnosti ({@link ResultType})
     */
    record ResultData(String result, String areaEgd, String area, String urlString,
                      ResultType resultType) {

        /**
         * Vytvoří kontejner s výchozí prázdnou oblastí EG.D a URL adresou.
         *
         * @param result     JSON řetězec s výslednými daty
         * @param resultType typ výsledku ({@link ResultType})
         * @param area       identifikátor oblasti nebo okresu
         */
        public ResultData(String result, ResultType resultType, String area) {
            this(result, resultType, area, "", "");
        }


        /**
         * Vytvoří plně specifikovaný kontejner dat.
         *
         * @param result     JSON řetězec s výslednými daty
         * @param resultType typ výsledku ({@link ResultType})
         * @param area       identifikátor oblasti nebo okresu
         * @param areaEgd    kód oblasti EG.D
         * @param urlString  URL adresa serveru
         */
        public ResultData(String result, ResultType resultType, String area, String areaEgd, String urlString) {
            this(result, areaEgd, area, urlString, resultType);
        }

    }


    /**
     * Určuje typ a původ získaných dat HDO podle distribuční společnosti.
     */
    enum ResultType {
        /** Datový výsledek od ČEZ */
        CEZ,
        /** Datový výsledek od EG.D */
        EGD,
        /** Datový výsledek od PRE */
        PRE,
        /** Seznam kódů a kategorií z mezikroku parsování EG.D */
        CODES
    }


    /**
     * Kontejner (record) předávající parametry dotazu na skupinu a kategorii pro EG.D.
     *
     * @param urlHdo        URL adresa GraphQL API pro HDO
     * @param urlParameters kódované parametry požadavku ve JSON
     * @param code          kód povelu HDO
     * @param districtName  název vybraného okresu
     * @param context       kontext aplikace
     * @param districtIndex index vybraného okresu
     */
    record GroupAndCategoryContainer(String urlHdo, String urlParameters, String code,
                                     String districtName, Context context, int districtIndex) {

        /**
         * Konstruktor kontejneru pro uchování parametrů dotazu EG.D.
         *
         * @param urlHdo        URL adresa API
         * @param urlParameters kódované parametry
         * @param code          kód povelu HDO
         * @param context       kontext aplikace
         * @param districtName  název vybraného okresu
         * @param districtIndex index vybraného okresu
         */
        public GroupAndCategoryContainer(String urlHdo, String urlParameters, String code, Context context, String districtName, int districtIndex) {
            this(urlHdo, urlParameters, code, districtName, context, districtIndex);
        }


        @NonNull
        @Override
        public String toString() {
            return "GroupAndCategoryContainer{" +
                    "urlHdo='" + urlHdo + '\'' +
                    ", urlParameters='" + urlParameters + '\'' +
                    ", code='" + code + '\'' +
                    ", districtName='" + districtName + '\'' +
                    ", context=" + context +
                    ", districtIndex=" + districtIndex +
                    '}';
        }

    }

}
