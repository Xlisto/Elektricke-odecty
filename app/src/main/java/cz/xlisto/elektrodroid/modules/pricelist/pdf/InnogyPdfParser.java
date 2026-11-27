package cz.xlisto.elektrodroid.modules.pricelist.pdf;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;

/**
 * Parser pro ceníky dodavatele innogy Energie, s.r.o.
 * <p>
 * Detekuje PDF dokumenty innogy, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby. Obchodní i regulované ceny se parsují dynamicky.
 */
public class InnogyPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "innogy Energie, s.r.o.";
    private static final String DISTRIBUCE = "EG.D";

    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("INNOGY");
    }

    @Override
    public List<PriceListModel> parse(String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');

        // Název produktu
        String produkt = "elektřina Optimal 36";
        Pattern produktPattern = Pattern.compile("(elektřina\\s+[^\n]+?)\\s*CENÍK", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(cleanText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("(?:platnost|účinný|účinné|platný|platné|je)\\s+od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(cleanText);
        while (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                long m = PdfParserUtils.parseDateToMillis(dateGroup);
                if (m > platnostOD) {
                    platnostOD = m;
                }
            }
        }

        Calendar calOD = Calendar.getInstance();
        calOD.setTimeInMillis(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
        boolean is2025Split = calOD.get(Calendar.YEAR) == 2025 && PdfParserUtils.is2025BeforeSeptember(platnostOD);

        long platnostDO = is2025Split
                ? PdfParserUtils.calculateEndOfAugust2025()
                : PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("distribuční soustavě\\s+([^\n,]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(cleanText);
        if (distMatcher.find()) {
            String group1 = distMatcher.group(1);
            if (group1 != null) {
                String rawDist = group1.toUpperCase();
                if (rawDist.contains("EG.D") || rawDist.contains("E.ON")) {
                    distribuce = "EG.D";
                } else if (rawDist.contains("PRE")) {
                    distribuce = "PRE";
                } else if (rawDist.contains("ČEZ") || rawDist.contains("CEZ")) {
                    distribuce = "ČEZ";
                }
            }
        }

        String[][] rateDefs = {
                {"D 01d", "STANDARD"},
                {"D 02d", "STANDARD"},
                {"D 25d", "AKU 8"},
                {"D 26d", "AKU 8"},
                {"D 27d", "E MOBILITA"},
                {"D 35d", "AKU 16"},
                {"D 45d", "PŘÍMOTOP"},
                {"D 56d", "TEPELNÉ ČERPADLO"},
                {"D 57d", "EL. TOPENÍ"},
                {"D 61d", "VÍKEND"}
        };

        int[] rateSeries8 = {0, 0, 1, 1, 2, 3, 4, 5, 6, 7};
        int[] ntSeries7 = {0, 0, 1, 2, 3, 4, 5, 6};

        // Zkusíme najít dynamicky ceny silové elektřiny z PDF
        List<Double> parsedVtList = new ArrayList<>();
        List<Double> parsedNtList = new ArrayList<>();
        List<Double> parsedPlatList = new ArrayList<>();

        String[] lines = cleanText.split("\n");
        for (String l : lines) {
            String lower = l.toLowerCase();
            if (lower.contains("vysokém tarifu") && !lower.contains("cena za odběr") && parsedVtList.isEmpty()) {
                parsedVtList = extractPricesFromLine(l);
            } else if (lower.contains("nízkém tarifu") && !lower.contains("cena za odběr") && parsedNtList.isEmpty()) {
                parsedNtList = extractPricesFromLine(l);
            } else if ((lower.contains("stálý") || lower.contains("stálá")) && !lower.contains("jistič") && parsedPlatList.isEmpty()) {
                parsedPlatList = extractPricesFromLine(l);
            }
        }

        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = 0.0;
            if (parsedVtList.size() == 1) {
                vt = parsedVtList.get(0);
            } else if (parsedVtList.size() == 8) {
                vt = parsedVtList.get(rateSeries8[i]);
            } else if (i < parsedVtList.size()) {
                vt = parsedVtList.get(i);
            }

            double nt = 0.0;
            if (!sazba.contains("01d") && !sazba.contains("02d")) {
                if (parsedNtList.size() == 1) {
                    nt = parsedNtList.get(0);
                } else if (parsedNtList.size() == 7) {
                    nt = parsedNtList.get(ntSeries7[i - 2]);
                } else if (parsedNtList.size() == 8) {
                    nt = parsedNtList.get(i - 2);
                } else if (i < parsedNtList.size()) {
                    nt = parsedNtList.get(i);
                }
            }

            double plat = 0.0;
            if (parsedPlatList.size() == 1) {
                plat = parsedPlatList.get(0);
            } else if (parsedPlatList.size() == 8) {
                plat = parsedPlatList.get(rateSeries8[i]);
            } else if (i < parsedPlatList.size()) {
                plat = parsedPlatList.get(i);
            }

            PriceListModel model = new PriceListModel(
                    0L,
                    rada,
                    produkt,
                    FIRMA,
                    vt,
                    nt,
                    plat,
                    0.0, // daň bez DPH
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // regulované ceny načítány dynamicky z raw JSON
                    0.0, // systémové služby bez DPH
                    is2025Split ? 10.84 : 0.0, // činnost operátora bez DPH pro 1. období 2025
                    0.0, // POZE1
                    0.0, // POZE2
                    0.0, 0.0,
                    platnostOD,
                    platnostDO,
                    21.0,
                    distribuce,
                    "PDF Import",
                    System.currentTimeMillis(),
                    ""
            );

            result.add(model);

            if (is2025Split) {
                PriceListModel model2 = PdfParserUtils.create2025SecondPeriodModel(model);
                result.add(model2);
            }
        }

        return result;
    }

    private List<Double> extractPricesFromLine(String line) {
        List<Double> prices = new ArrayList<>();
        if (line == null || line.isEmpty()) return prices;

        Pattern p = Pattern.compile("\\b(\\d+(?:[\\s\\u00a0]\\d+)*,\\d{2})\\b");
        Matcher m = p.matcher(line);
        while (m.find()) {
            String val = m.group(1);
            if (val != null) {
                double price = PdfParserUtils.parsePrice(val);
                if (price > 10.0) {
                    prices.add(price);
                }
            }
        }
        return prices;
    }
}
