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

        // Seznam definovaných sazeb a jejich parametrů pro PRE (ceny v Kč/MWh bez DPH)
        String[][] rateDefs = {
                {"D 01d", "KLASIK 24", "3570.00", "0.0", "129.00"},
                {"D 02d", "KLASIK 24", "3570.00", "0.0", "129.00"},
                {"D 25d", "AKU 8", "3470.00", "3270.00", "129.00"},
                {"D 26d", "AKU 8", "3470.00", "3270.00", "129.00"},
                {"D 27d", "E MOBILITA", "3470.00", "3270.00", "129.00"},
                {"D 35d", "KOMBI 20", "3570.00", "3370.00", "129.00"},
                {"D 45d", "PŘÍMOTOP 20", "3770.00", "3470.00", "129.00"},
                {"D 56d", "TČ 20", "3770.00", "3470.00", "129.00"},
                {"D 57d", "TOPENÍ 20", "3770.00", "3470.00", "129.00"},
                {"D 61d", "Víkend 24", "3470.00", "3270.00", "129.00"}
        };

        double[] preDistVT = {1854.71, 1516.53, 1656.49, 1009.35, 1656.49, 421.52, 421.52, 421.52, 421.52, 2343.05};
        double[] preDistNT = {0.0, 0.0, 175.20, 175.20, 175.20, 175.20, 175.20, 175.20, 175.20, 175.20};
        double[] preJ0 = {44.0, 87.0, 80.0, 110.0, 76.0, 154.0, 154.0, 154.0, 154.0, 64.0};
        double[] preJ1 = {70.0, 139.0, 128.0, 175.0, 122.0, 246.0, 246.0, 246.0, 246.0, 103.0};
        double[] preJ2 = {88.0, 173.0, 160.0, 219.0, 152.0, 308.0, 308.0, 308.0, 308.0, 128.0};
        double[] preJ3 = {110.0, 217.0, 200.0, 274.0, 191.0, 385.0, 385.0, 385.0, 385.0, 161.0};
        double[] preJ4 = {140.0, 277.0, 256.0, 350.0, 244.0, 492.0, 492.0, 492.0, 492.0, 205.0};
        double[] preJ5 = {175.0, 347.0, 320.0, 438.0, 305.0, 616.0, 616.0, 616.0, 622.0, 257.0};
        double[] preJ6 = {219.0, 434.0, 401.0, 548.0, 381.0, 770.0, 770.0, 770.0, 933.0, 321.0};
        double[] preJ7 = {276.0, 546.0, 505.0, 690.0, 480.0, 970.0, 970.0, 970.0, 1433.0, 404.0};

        for (int i = 0; i < rateDefs.length; i++) {
            String[] def = rateDefs[i];
            String sazba = PdfParserUtils.formatSazba(def[0]);
            String rada = def[1];
            double vt = Double.parseDouble(def[2]);
            double nt = Double.parseDouble(def[3]);
            double plat = Double.parseDouble(def[4]);

            PriceListModel model = new PriceListModel(
                    0L,
                    rada,
                    produkt,
                    FIRMA,
                    vt,
                    nt,
                    plat,
                    28.30,
                    sazba,
                    preDistVT[i], preDistNT[i], preJ0[i], preJ1[i], preJ2[i], preJ3[i], preJ4[i], preJ5[i], preJ6[i], preJ7[i], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    164.24, // systémové služby bez DPH
                    12.87,  // činnost operátora bez DPH
                    0.0,    // POZE1
                    495.00, // POZE2
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
