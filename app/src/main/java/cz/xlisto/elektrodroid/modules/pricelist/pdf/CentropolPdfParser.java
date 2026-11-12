package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele CENTROPOL ENERGY, a. s.
 * <p>
 * Detekuje PDF dokumenty Centropol, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby včetně obchodních a regulovaných cen.
 */
public class CentropolPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "CENTROPOL ENERGY, a. s.";
    private static final String DISTRIBUCE = "ČEZ Distribuce, a.s.";


    /**
     * Zjistí, zda předaný text z PDF dokumentu pochází od dodavatele CENTROPOL ENERGY, a. s.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text obsahuje identifikátor dodavatele Centropol
     */
    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("CENTROPOL");
    }


    /**
     * Rozparsuje text PDF ceníku Centropol a vrátí seznam ceníkových modelů pro jednotlivé distribuční sazby.
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
        String produkt = "FIXNĚ ONLINE PRO ZAČÁTEK";
        Pattern produktPattern = Pattern.compile("Produkt/Ceník:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(pdfText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("Ceník je účinný od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
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
                {"D 01d", "Základní ceník", "2817.00", "0.0", "135.00"},
                {"D 02d", "Základní ceník", "2817.00", "0.0", "135.00"},
                {"D 25d", "AKU 8", "2958.00", "2676.00", "135.00"},
                {"D 26d", "AKU 8", "2958.00", "2676.00", "135.00"},
                {"D 27d", "AKU 8", "2958.00", "2676.00", "135.00"},
                {"D 35d", "AKU 16", "2986.00", "2874.00", "135.00"},
                {"D 45d", "Přímotop", "2958.00", "2902.00", "135.00"},
                {"D 56d", "Tepelné čerpadlo", "2958.00", "2902.00", "135.00"},
                {"D 57d", "Elektrické topení", "2958.00", "2902.00", "135.00"},
                {"D 61d", "Víkend", "2958.00", "2705.00", "135.00"}
        };

        // Zkusíme najít dynamicky ceny silové elektřiny z PDF (v závorce bez DPH)
        Pattern vtPattern = Pattern.compile("1\\s*Vysoký tarif Kč/MWh([^\n]+)");
        Pattern ntPattern = Pattern.compile("2\\s*Nízký tarif Kč/MWh([^\n]+)");
        Pattern platPattern = Pattern.compile("3\\s*Fixní cena Kč/měsíc([^\n]+)");

        Matcher vtMatcher = vtPattern.matcher(pdfText);
        Matcher ntMatcher = ntPattern.matcher(pdfText);
        Matcher platMatcher = platPattern.matcher(pdfText);

        List<Double> parsedVtList = extractPricesFromLine(vtMatcher.find() ? vtMatcher.group(1) : "");
        List<Double> parsedNtList = extractPricesFromLine(ntMatcher.find() ? ntMatcher.group(1) : "");
        List<Double> parsedPlatList = extractPricesFromLine(platMatcher.find() ? platMatcher.group(1) : "");

        // Regulované ceny z PDF
        double[] cenDistVT = {2666.66, 2078.58, 2252.45, 1202.06, 2252.45, 754.77, 754.77, 754.77, 754.77, 3306.67};
        double[] cenDistNT = {0.0, 0.0, 116.50, 116.50, 116.50, 116.50, 116.50, 116.50, 116.50, 116.50};
        double[] cenJ0 = {50.0, 102.0, 107.0, 145.0, 102.0, 222.0, 222.0, 222.0, 222.0, 89.0};
        double[] cenJ1 = {79.0, 164.0, 172.0, 231.0, 163.0, 355.0, 355.0, 355.0, 355.0, 143.0};
        double[] cenJ2 = {99.0, 205.0, 215.0, 289.0, 204.0, 444.0, 444.0, 444.0, 444.0, 179.0};
        double[] cenJ3 = {124.0, 256.0, 269.0, 362.0, 255.0, 555.0, 555.0, 555.0, 555.0, 224.0};
        double[] cenJ4 = {158.0, 327.0, 344.0, 463.0, 326.0, 710.0, 710.0, 710.0, 710.0, 286.0};
        double[] cenJ5 = {198.0, 409.0, 430.0, 578.0, 408.0, 888.0, 888.0, 888.0, 896.0, 358.0};
        double[] cenJ6 = {248.0, 512.0, 537.0, 723.0, 510.0, 1110.0, 1110.0, 1110.0, 1289.0, 447.0};
        double[] cenJ7 = {312.0, 644.0, 677.0, 911.0, 643.0, 1399.0, 1399.0, 1399.0, 1949.0, 563.0};

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
                    cenDistVT[i], cenDistNT[i], cenJ0[i], cenJ1[i], cenJ2[i], cenJ3[i], cenJ4[i], cenJ5[i], cenJ6[i], cenJ7[i], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
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
     * Pomocná metoda pro extrakci číselných cen bez DPH (uvedených v závorce) z řádku textu.
     *
     * @param line Řádek textu s cenovými hodnotami v závorkách
     * @return Seznam přečtených cenových hodnot
     */
    private List<Double> extractPricesFromLine(String line) {
        List<Double> prices = new ArrayList<>();
        if (line == null || line.isEmpty()) return prices;

        Pattern bracketPattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = bracketPattern.matcher(line);
        while (matcher.find()) {
            String val = matcher.group(1);
            if (val != null) {
                double price = PdfParserUtils.parsePrice(val);
                if (price > 0) {
                    prices.add(price);
                }
            }
        }
        return prices;
    }

}
