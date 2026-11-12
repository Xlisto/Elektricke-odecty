package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele Pražská energetika, a. s. (PRE).
 */
public class PrePdfParser implements PdfPriceListParser {

    private static final String FIRMA = "Pražská energetika, a. s.";
    private static final String DISTRIBUCE = "PREdistribuce, a. s.";


    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return (text.contains("PRAŽSKÁ ENERGETIKA") || text.contains("PRAZSKA ENERGETIKA") || text.contains("PRE PROUD") || text.contains("PRE.CZ"));
    }


    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null) return result;

        // Název produktu
        String produkt = "PRE PROUD EKO";
        Pattern produktPattern = Pattern.compile("(PRE PROUD [^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(pdfText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("platný\\s*od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(pdfText);
        if (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
            }
        }

        // Maximální platnost do konce aktuálního roku
        long platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("Distribuční území:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
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
                {"D 01d", "KLASIK 24"},
                {"D 02d", "KLASIK 24"},
                {"D 25d", "AKU 8"},
                {"D 26d", "AKU 8"},
                {"D 27d", "E MOBILITA"},
                {"D 35d", "KOMBI 20"},
                {"D 45d", "PŘÍMOTOP 20"},
                {"D 56d", "TČ 20"},
                {"D 57d", "TOPENÍ 20"},
                {"D 61d", "Víkend 24"}
        };

        for (String[] def : rateDefs) {
            String sazba = PdfParserUtils.formatSazba(def[0]);
            String rada = def[1];

            PriceListModel model = new PriceListModel(
                    0L,
                    rada,
                    produkt,
                    FIRMA,
                    0.0,
                    0.0,
                    0.0,
                    0.0, // daň bez DPH
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // regulované ceny se načítají z raw JSON
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

}
