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
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby včetně obchodních a regulovaných cen.
 */
public class InnogyPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "innogy Energie, s.r.o.";
    private static final String DISTRIBUCE = "EG.D, s.r.o.";


    /**
     * Zjistí, zda předaný text z PDF dokumentu pochází od dodavatele innogy Energie, s.r.o.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text obsahuje identifikátor dodavatele innogy
     */
    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("INNOGY");
    }


    /**
     * Rozparsuje text PDF ceníku innogy a vrátí seznam ceníkových modelů pro jednotlivé distribuční sazby.
     *
     * @param context Kontext aplikace
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Seznam načtených objektů {@link PriceListModel}
     */
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
                {"D 01d", "STANDARD", "3540.00", "0.0", "135.00"},
                {"D 02d", "STANDARD", "3540.00", "0.0", "135.00"},
                {"D 25d", "AKU 8", "3540.00", "3540.00", "135.00"},
                {"D 26d", "AKU 8", "3540.00", "3540.00", "135.00"},
                {"D 27d", "E MOBILITA", "3440.00", "3440.00", "135.00"},
                {"D 35d", "AKU 16", "3540.00", "3540.00", "135.00"},
                {"D 45d", "PŘÍMOTOP", "3540.00", "3540.00", "135.00"},
                {"D 56d", "TEPELNÉ ČERPADLO", "3440.00", "3440.00", "135.00"},
                {"D 57d", "EL. TOPENÍ", "3440.00", "3440.00", "135.00"},
                {"D 61d", "VÍKEND", "3540.00", "3540.00", "135.00"}
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

        // Regulované ceny z PDF v distribuční oblasti EG.D
        double[] innogyDistVT = {2711.14, 2295.34, 2243.88, 1237.94, 2243.88, 749.87, 749.87, 749.87, 749.87, 3367.23};
        double[] innogyDistNT = {0.0, 0.0, 224.30, 224.30, 224.30, 224.30, 224.30, 224.30, 224.30, 224.30};
        double[] innogyJ0 = {48.0, 100.0, 98.0, 140.0, 93.0, 190.0, 190.0, 190.0, 189.0, 90.0};
        double[] innogyJ1 = {77.0, 160.0, 157.0, 223.0, 149.0, 304.0, 304.0, 304.0, 302.0, 144.0};
        double[] innogyJ2 = {96.0, 200.0, 196.0, 279.0, 187.0, 380.0, 380.0, 380.0, 378.0, 179.0};
        double[] innogyJ3 = {120.0, 250.0, 245.0, 349.0, 233.0, 475.0, 475.0, 475.0, 473.0, 224.0};
        double[] innogyJ4 = {154.0, 320.0, 314.0, 446.0, 299.0, 608.0, 608.0, 608.0, 605.0, 287.0};
        double[] innogyJ5 = {192.0, 400.0, 392.0, 558.0, 373.0, 760.0, 760.0, 760.0, 764.0, 359.0};
        double[] innogyJ6 = {240.0, 500.0, 491.0, 698.0, 467.0, 950.0, 950.0, 950.0, 1098.0, 449.0};
        double[] innogyJ7 = {302.0, 629.0, 618.0, 879.0, 588.0, 1196.0, 1196.0, 1196.0, 1619.0, 565.0};

        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = (i < parsedVtList.size() && parsedVtList.get(i) > 0) ? parsedVtList.get(i) : Double.parseDouble(rateDefs[i][2]);

            double nt = 0.0;
            if (!sazba.contains("01d") && !sazba.contains("02d")) {
                int ntIndex = i - 2;
                if (ntIndex >= 0 && ntIndex < parsedNtList.size() && parsedNtList.get(ntIndex) > 0) {
                    nt = parsedNtList.get(ntIndex);
                } else {
                    nt = Double.parseDouble(rateDefs[i][3]);
                }
            }

            double plat = (i < parsedPlatList.size() && parsedPlatList.get(i) > 0) ? parsedPlatList.get(i) : Double.parseDouble(rateDefs[i][4]);

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
                    innogyDistVT[i], innogyDistNT[i], innogyJ0[i], innogyJ1[i], innogyJ2[i], innogyJ3[i], innogyJ4[i], innogyJ5[i], innogyJ6[i], innogyJ7[i], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
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


    /**
     * Pomocná metoda pro extrakci číselných cenových hodnot z řádku textu.
     *
     * @param line Řádek textu s cenovými tokeny
     * @return Seznam přečtených cenových hodnot
     */
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
