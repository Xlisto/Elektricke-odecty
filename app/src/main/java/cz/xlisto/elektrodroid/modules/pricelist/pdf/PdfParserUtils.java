package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;
import android.net.Uri;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Pomocné utility pro čtení a parsování PDF ceníků.
 */
public class PdfParserUtils {

    /**
     * Načte a extrahuje textový obsah z PDF souboru pomocí PDFBox.
     *
     * @param context Kontext aplikace
     * @param uri     Uri souboru
     * @return Extrahovaný text z PDF
     * @throws Exception V případě chyby čtení
     */
    public static String extractTextFromPdf(Context context, Uri uri) throws Exception {
        PDFBoxResourceLoader.init(context.getApplicationContext());
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }


    /**
     * Vypočítá milisekundy pro konec roku (31.12. YYYY 23:59:59.999) na základě počátečního data.
     *
     * @param startMillis Počáteční datum v milisekundách
     * @return Datum konce roku v milisekundách
     */
    public static long calculateEndOfYear(long startMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startMillis);
        int year = cal.get(Calendar.YEAR);

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }


    /**
     * Převede řetězec s datem ve formátu D.M.YYYY na milisekundy.
     *
     * @param dateStr Textový zápis data (např. "1.1.2026")
     * @return Datum v milisekundách nebo 0 při chybě
     */
    public static long parseDateToMillis(String dateStr) {
        if (dateStr == null) return 0;
        Pattern pattern = Pattern.compile("(\\d{1,2})\\.\\s*(\\d{1,2})\\.\\s*(\\d{4})");
        Matcher matcher = pattern.matcher(dateStr);
        if (matcher.find()) {
            try {
                String g1 = matcher.group(1);
                String g2 = matcher.group(2);
                String g3 = matcher.group(3);
                if (g1 != null && g2 != null && g3 != null) {
                    int day = Integer.parseInt(g1);
                    int month = Integer.parseInt(g2) - 1; // Calendar měsíce 0-11
                    int year = Integer.parseInt(g3);

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal.getTimeInMillis();
                }
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }


    /**
     * Převede textovou hodinovou cenu na double (odstraní mezery, nahradí čárku tečkou).
     *
     * @param priceStr Textová hodnota ceny
     * @return Číselná hodnota ceny
     */
    public static double parsePrice(String priceStr) {
        if (priceStr == null) return 0.0;
        String cleanStr = priceStr.replace("\u00a0", "")
                .replace(" ", "")
                .replace(",", ".")
                .trim();
        try {
            return Double.parseDouble(cleanStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }


    /**
     * Formátuje sazbu do standardního tvaru s mezerou (např. "D01d" -> "D 01d", "D26d" -> "D 26d").
     *
     * @param rawSazba Název sazby k naformátování
     * @return Naformátovaný název sazby s mezerou (např. "D 26d")
     */
    public static String formatSazba(String rawSazba) {
        if (rawSazba == null) return "";
        String s = rawSazba.trim();
        Pattern pattern = Pattern.compile("^([A-Za-z])\\s*(\\d{1,2})\\s*([A-Za-z]?)$");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            try {
                String group1 = matcher.group(1);
                String group2 = matcher.group(2);
                String group3 = matcher.group(3);
                if (group2 != null) {
                    String prefix = group1 != null ? group1.toUpperCase() : "";
                    int num = Integer.parseInt(group2);
                    String suffix = group3 != null ? group3.toLowerCase() : "";
                    return String.format(java.util.Locale.US, "%s %02d%s", prefix, num, suffix);
                }
            } catch (Exception e) {
                return s;
            }
        }
        return s;
    }


    /**
     * Vrátí zkrácený název distribučního území pro databázi ("ČEZ", "EG.D", "PRE").
     *
     * @param rawDist Původní název distribučního území
     * @return Zkrácený název ("ČEZ", "EG.D" nebo "PRE")
     */
    public static String getShortDistribuce(String rawDist) {
        if (rawDist == null) return "ČEZ";
        String upper = rawDist.toUpperCase();
        if (upper.contains("EG.D")) return "EG.D";
        if (upper.contains("PRE")) return "PRE";
        if (upper.contains("ČEZ") || upper.contains("CEZ")) return "ČEZ";
        if (upper.contains("E.ON") || upper.contains("EON")) return "E.ON";
        return rawDist;
    }


    /**
     * Vrátí plný název distribučního území ze zkráceného kódu pro zobrazení v UI.
     *
     * @param shortDist Zkrácený kód distribučního území ("ČEZ", "EG.D", "PRE", "E.ON")
     * @return Plný název distribučního území (např. "PREdistribuce, a. s.")
     */
    public static String getFullDistribuceName(String shortDist) {
        if (shortDist == null) return "";
        String upper = shortDist.toUpperCase();
        if (upper.contains("EG.D")) return "EG.D, s.r.o.";
        if (upper.contains("PRE")) return "PREdistribuce, a. s.";
        if (upper.contains("ČEZ") || upper.contains("CEZ")) return "ČEZ Distribuce, a.s.";
        if (upper.contains("E.ON") || upper.contains("EON")) return "E.ON Energie, a.s.";
        return shortDist;
    }

}
