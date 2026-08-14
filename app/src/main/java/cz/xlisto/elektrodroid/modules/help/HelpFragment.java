package cz.xlisto.elektrodroid.modules.help;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

import cz.xlisto.elektrodroid.R;


/**
 * Fragment s nápovědou z README renderovanou do HTML.
 */
public class HelpFragment extends Fragment {

    private static final String HELP_ASSET_PATH = "help/readme.md";
    private static final String HELP_ASSET_FALLBACK_PATH = "help/README.md";
    private static final String HELP_BASE_URL = "file:///android_asset/help/";
    private static final Pattern HEADING_PATTERN = Pattern.compile("^\\s{0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final Pattern HTML_HEADING_PATTERN = Pattern.compile("<h([1-6])>(.*?)</h\\1>", Pattern.DOTALL);


    public HelpFragment() {
    }


    public static HelpFragment newInstance() {
        return new HelpFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WebView webView = view.findViewById(R.id.webViewHelp);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadDataWithBaseURL(HELP_BASE_URL, buildHelpHtml(requireContext()), "text/html", "UTF-8", null);
    }


    private String buildHelpHtml(@NonNull Context context) {
        String markdown = readAsset(context, HELP_ASSET_PATH, HELP_ASSET_FALLBACK_PATH);
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(false).build();
        String body = addHeadingIds(renderer.render(document), extractHeadingSlugs(markdown));
        return "<!doctype html>"
                + "<html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<style>"
                + "body{font-family:sans-serif;margin:0;padding:16px;line-height:1.5;color:#222;}"
                + "img{max-width:100%;height:auto;}"
                + "pre,code{white-space:pre-wrap;}"
                + "table{border-collapse:collapse;max-width:100%;}"
                + "th,td{border:1px solid #ccc;padding:4px 8px;}"
                + "</style></head><body>"
                + body
                + "</body></html>";
    }


    private List<String> extractHeadingSlugs(@NonNull String markdown) {
        List<String> slugs = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        String[] lines = markdown.split("\\R");
        for (String line : lines) {
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String headingText = matcher.group(2);
            if (headingText != null) {
                headingText = headingText.trim();
            } else {
                continue;
            }
            String baseSlug = slugify(headingText);
            if (baseSlug.isEmpty()) {
                continue;
            }
            Integer countObj = occurrences.get(baseSlug);
            int count = (countObj != null ? countObj : 0) + 1;
            occurrences.put(baseSlug, count);
            slugs.add(count == 1 ? baseSlug : baseSlug + "-" + count);
        }
        return slugs;
    }


    @SuppressWarnings("StringBufferReplaceableByStringBuilder")
    // API 23 compat: Matcher.appendReplacement/appendTail need StringBuffer until API 34
    private String addHeadingIds(@NonNull String html, @NonNull List<String> slugs) {
        Matcher matcher = HTML_HEADING_PATTERN.matcher(html);
        //noinspection StringBufferReplaceableByStringBuilder
        @SuppressWarnings("all")
        StringBuffer buffer = new StringBuffer();
        int index = 0;
        while (matcher.find()) {
            String replacement = matcher.group(0);
            if (index < slugs.size()) {
                replacement = "<h" + matcher.group(1) + " id=\"" + slugs.get(index) + "\">" + matcher.group(2) + "</h" + matcher.group(1) + ">";
            }
            if (replacement != null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            index++;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }


    private String slugify(@NonNull String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }


    @SuppressWarnings("SameParameterValue")
    private String readAsset(@NonNull Context context, @NonNull String... assetPaths) {
        for (String assetPath : assetPaths) {
            try (InputStream inputStream = context.getAssets().open(assetPath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            } catch (IOException ignored) {
            }
        }
        return "# Nápověda\n\nREADME se nepodařilo načíst.";
    }

}





