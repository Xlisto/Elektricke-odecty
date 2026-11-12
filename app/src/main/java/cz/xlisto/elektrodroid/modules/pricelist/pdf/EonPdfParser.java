package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele E.ON Energie, a.s. (distribuční území EG.D).
 * <p>
 * Detekuje PDF dokumenty E.ON / EG.D, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby včetně obchodních a regulovaných cen.
 */
public class EonPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "E.ON Energie, a.s.";
    private static final String DISTRIBUCE = "EG.D, s.r.o.";


    /**
     * Zjistí, zda předaný text z PDF dokumentu pochází od dodavatele E.ON Energie, a.s. / EG.D.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text obsahuje identifikátory E.ON nebo EG.D
     */
    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return (text.contains("E.ON") || text.contains("EON.CZ") || text.contains("ENERGIE24"));
    }


    /**
     * Rozparsuje text PDF ceníku E.ON / EG.D a vrátí seznam ceníkových modelů pro jednotlivé distribuční sazby.
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
        String produkt = "Komplet elektřina MAX";
        Pattern produktPattern = Pattern.compile("Ceník\\s+([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(pdfText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("platná\\s+od\\s+(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
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

        // Seznam definovaných sazeb a jejich parametrů pro E.ON (ceny v Kč/MWh a Kč/měsíc bez DPH)
        String[][] rateDefs = {
                {"D 01d", "Klasik", "3299.00", "0.0", "160.00"},
                {"D 02d", "Klasik", "3299.00", "0.0", "160.00"},
                {"D 25d", "Aku", "3447.00", "3102.00", "160.00"},
                {"D 26d", "Aku", "3447.00", "3102.00", "160.00"},
                {"D 27d", "Aku", "3447.00", "3102.00", "160.00"},
                {"D 35d", "Kombi", "3471.00", "3217.00", "160.00"},
                {"D 45d", "Přímotop", "3466.00", "3283.00", "160.00"},
                {"D 56d", "Přímotop", "3466.00", "3283.00", "160.00"},
                {"D 57d", "Přímotop", "3466.00", "3283.00", "160.00"},
                {"D 61d", "Víkend", "3459.00", "3101.00", "160.00"}
        };

        double[] eonDistVT = {2711.14, 2295.34, 2243.88, 1237.94, 2243.88, 749.87, 749.87, 749.87, 749.87, 3367.23};
        double[] eonDistNT = {0.0, 0.0, 224.30, 224.30, 224.30, 224.30, 224.30, 224.30, 224.30, 224.30};
        double[] eonJ0 = {48.0, 100.0, 98.0, 140.0, 93.0, 190.0, 190.0, 190.0, 189.0, 90.0};
        double[] eonJ1 = {77.0, 160.0, 157.0, 223.0, 149.0, 304.0, 304.0, 304.0, 302.0, 144.0};
        double[] eonJ2 = {96.0, 200.0, 196.0, 279.0, 187.0, 380.0, 380.0, 380.0, 378.0, 179.0};
        double[] eonJ3 = {120.0, 250.0, 245.0, 349.0, 233.0, 475.0, 475.0, 475.0, 473.0, 224.0};
        double[] eonJ4 = {154.0, 320.0, 314.0, 446.0, 299.0, 608.0, 608.0, 608.0, 605.0, 287.0};
        double[] eonJ5 = {192.0, 400.0, 392.0, 558.0, 373.0, 760.0, 760.0, 760.0, 764.0, 359.0};
        double[] eonJ6 = {240.0, 500.0, 491.0, 698.0, 467.0, 950.0, 950.0, 950.0, 1098.0, 449.0};
        double[] eonJ7 = {302.0, 629.0, 618.0, 879.0, 588.0, 1196.0, 1196.0, 1196.0, 1619.0, 565.0};

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
                    eonDistVT[i], eonDistNT[i], eonJ0[i], eonJ1[i], eonJ2[i], eonJ3[i], eonJ4[i], eonJ5[i], eonJ6[i], eonJ7[i], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
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
