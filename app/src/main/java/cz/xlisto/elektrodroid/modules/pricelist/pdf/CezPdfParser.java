package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele ČEZ Prodej, a.s.
 * <p>
 * Detekuje PDF dokumenty ČEZ Prodej, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby včetně obchodních a regulovaných cen.
 */
public class CezPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "ČEZ Prodej, a.s.";
    private static final String DISTRIBUCE = "ČEZ Distribuce, a.s.";


    /**
     * Zjistí, zda předaný text z PDF dokumentu pochází od dodavatele ČEZ Prodej, a.s.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text obsahuje identifikátory dodavatele ČEZ Prodej a ČEZ Distribuce
     */
    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return (text.contains("ČEZ PRODEJ") || text.contains("CEZ PRODEJ") || text.contains("WWW.CEZ.CZ"))
                && (text.contains("ČEZ DISTRIBUCE") || text.contains("CEZ DISTRIBUCE"));
    }


    /**
     * Rozparsuje text PDF ceníku ČEZ a vrátí seznam ceníkových modelů pro jednotlivé distribuční sazby.
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
        String produkt = "Elektřina na dobu neurčitou";
        Pattern produktPattern = Pattern.compile("Ceník elektřiny pro domácnosti\\s*\n\\s*([^\n]+)");
        Matcher produktMatcher = produktPattern.matcher(pdfText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("Účinnost obchodních cen:\\s*od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
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

        // Seznam definovaných sazeb a jejich parametrů pro ČEZ (hodnoty bez DPH)
        // [sazba, rada, cenaVT_bezDPH, cenaNT_bezDPH, stalyPlat_bezDPH]
        String[][] rateDefs = {
                {"D 01d", "Standard", "3190.08", "0.0", "135.00"},
                {"D 02d", "Standard", "3190.08", "0.0", "135.00"},
                {"D 25d", "Akumulace 8", "3272.73", "3057.85", "121.00"},
                {"D 26d", "Akumulace 8", "3272.73", "3057.85", "121.00"},
                {"D 27d", "Elektromobilita", "3272.73", "3016.53", "121.00"},
                {"D 35d", "Akumulace 16", "3421.49", "3322.31", "121.00"},
                {"D 45d", "Přímotop", "3421.49", "3322.31", "121.00"},
                {"D 56d", "Tepelné čerpadlo", "3421.49", "3322.31", "121.00"},
                {"D 57d", "Elektrické topení", "3421.49", "3322.31", "121.00"},
                {"D 61d", "Víkend", "3190.08", "3190.08", "121.00"}
        };

        // Zkusíme najít dynamicky ceny z PDF pokud tam jsou vzory (v závorce bez DPH)
        Pattern vtPattern = Pattern.compile("1\\s*Vysoký tarif Kč/MWh([^\n]+)");
        Pattern ntPattern = Pattern.compile("2\\s*Nízký tarif Kč/MWh([^\n]+)");
        Pattern platPattern = Pattern.compile("3\\s*Stálá platba Kč/měsíc([^\n]+)");

        Matcher vtMatcher = vtPattern.matcher(pdfText);
        Matcher ntMatcher = ntPattern.matcher(pdfText);
        Matcher platMatcher = platPattern.matcher(pdfText);

        List<Double> parsedVtList = extractPricesFromLine(vtMatcher.find() ? vtMatcher.group(1) : "");
        List<Double> parsedNtList = extractPricesFromLine(ntMatcher.find() ? ntMatcher.group(1) : "");
        List<Double> parsedPlatList = extractPricesFromLine(platMatcher.find() ? platMatcher.group(1) : "");

        double[] cezDistVT = {6192.48, 5488.93, 5739.06, 4517.95, 5739.06, 4342.16, 4342.16, 4342.16, 4342.16, 6671.90};
        double[] cezDistNT = {0.0, 0.0, 3463.07, 3463.07, 3421.75, 3727.53, 3727.53, 3727.53, 3727.53, 3595.30};
        double[] cezJ0 = {44.0, 98.0, 95.0, 140.0, 90.0, 171.0, 188.0, 188.0, 188.0, 79.0};
        double[] cezJ1 = {70.0, 157.0, 152.0, 223.0, 144.0, 273.0, 300.0, 300.0, 300.0, 126.0};
        double[] cezJ2 = {87.0, 197.0, 190.0, 279.0, 180.0, 341.0, 375.0, 375.0, 375.0, 158.0};
        double[] cezJ3 = {109.0, 246.0, 237.0, 349.0, 225.0, 427.0, 469.0, 469.0, 469.0, 197.0};
        double[] cezJ4 = {140.0, 315.0, 303.0, 447.0, 288.0, 546.0, 600.0, 600.0, 600.0, 252.0};
        double[] cezJ5 = {175.0, 394.0, 379.0, 558.0, 360.0, 683.0, 750.0, 750.0, 750.0, 315.0};
        double[] cezJ6 = {219.0, 492.0, 474.0, 698.0, 450.0, 853.0, 938.0, 938.0, 938.0, 394.0};
        double[] cezJ7 = {276.0, 620.0, 597.0, 879.0, 567.0, 1075.0, 1181.0, 1181.0, 1181.0, 496.0};

        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = (i < parsedVtList.size() && parsedVtList.get(i) > 0) ? parsedVtList.get(i) : Double.parseDouble(rateDefs[i][2]);

            // D 01d a D 02d nemají nízký tarif
            double nt = 0.0;
            if (!sazba.contains("01d") && !sazba.contains("02d")) {
                int ntIndex = i - 2; // Nízký tarif má 8 hodnot pro D25d až D61d
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
                    28.30, // daň bez DPH
                    sazba,
                    cezDistVT[i], cezDistNT[i], cezJ0[i], cezJ1[i], cezJ2[i], cezJ3[i], cezJ4[i], cezJ5[i], cezJ6[i], cezJ7[i], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    170.92, // systémové služby bez DPH
                    12.45,  // činnost operátora bez DPH
                    84.70,  // POZE1 bez DPH
                    495.00, // POZE2 bez DPH
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
     * @param line Řádek textu s cenovými hodnotami
     * @return Seznam přečtených cenových hodnot
     */
    private List<Double> extractPricesFromLine(String line) {
        List<Double> prices = new ArrayList<>();
        if (line == null || line.isEmpty()) return prices;

        // Hledáme závorky s cenami bez DPH např. (3 190,08) nebo (135,00)
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
