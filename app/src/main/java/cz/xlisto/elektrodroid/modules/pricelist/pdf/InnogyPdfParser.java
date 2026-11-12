package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
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
    private static final String DISTRIBUCE = "EG.D, s.r.o.";


    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("INNOGY");
    }


    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null) return result;

        // Název produktu
        String produkt = "elektřina Optimal 36";
        Pattern produktPattern = Pattern.compile("(elektřina\\s+[^\n]+?)\\s*CENÍK", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(pdfText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("Ceník je platný od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(pdfText);
        if (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
            }
        }
        if (platnostOD == 0) {
            Pattern altOdPattern = Pattern.compile("od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})");
            Matcher altOdMatcher = altOdPattern.matcher(pdfText);
            if (altOdMatcher.find()) {
                String dateGroup = altOdMatcher.group(1);
                if (dateGroup != null) {
                    platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
                }
            }
        }

        // Maximální platnost do konce aktuálního roku
        long platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("distribuční soustavě\\s+([^\n,]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(pdfText);
        if (distMatcher.find()) {
            String group1 = distMatcher.group(1);
            if (group1 != null) {
                String rawDist = group1.toUpperCase();
                if (rawDist.contains("EG.D") || rawDist.contains("E.ON")) {
                    distribuce = "EG.D, s.r.o.";
                } else if (rawDist.contains("PRE")) {
                    distribuce = "PREdistribuce, a. s.";
                } else if (rawDist.contains("ČEZ") || rawDist.contains("CEZ")) {
                    distribuce = "ČEZ Distribuce, a.s.";
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

        // Zkusíme najít dynamicky ceny silové elektřiny z PDF
        Pattern vtPattern = Pattern.compile("cena silové elektřiny ve vysokém tarifu[^\n]*?([\\d\\s,]+)");
        Pattern ntPattern = Pattern.compile("cena silové elektřiny v nízkém tarifu[^\n]*?([\\d\\s,]+)");
        Pattern platPattern = Pattern.compile("stálý měsíční plat[^\n]*?([\\d\\s,]+)");

        Matcher vtMatcher = vtPattern.matcher(pdfText);
        Matcher ntMatcher = ntPattern.matcher(pdfText);
        Matcher platMatcher = platPattern.matcher(pdfText);

        List<Double> parsedVtList = extractPricesFromLine(vtMatcher.find() ? vtMatcher.group(1) : "");
        List<Double> parsedNtList = extractPricesFromLine(ntMatcher.find() ? ntMatcher.group(1) : "");
        List<Double> parsedPlatList = extractPricesFromLine(platMatcher.find() ? platMatcher.group(1) : "");

        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = (i < parsedVtList.size() && parsedVtList.get(i) > 0) ? parsedVtList.get(i) : 0.0;

            double nt = 0.0;
            if (!sazba.contains("01d") && !sazba.contains("02d")) {
                int ntIndex = i - 2;
                if (ntIndex >= 0 && ntIndex < parsedNtList.size() && parsedNtList.get(ntIndex) > 0) {
                    nt = parsedNtList.get(ntIndex);
                }
            }

            double plat = (i < parsedPlatList.size() && parsedPlatList.get(i) > 0) ? parsedPlatList.get(i) : 0.0;

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
                    0.0, // činnost operátora bez DPH
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
        }

        return result;
    }


    private List<Double> extractPricesFromLine(String line) {
        List<Double> prices = new ArrayList<>();
        if (line == null || line.isEmpty()) return prices;

        String[] tokens = line.split("\\s+");
        for (String tok : tokens) {
            double price = PdfParserUtils.parsePrice(tok);
            if (price > 10.0) {
                prices.add(price);
            }
        }
        return prices;
    }

}
