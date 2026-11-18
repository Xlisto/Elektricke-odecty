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
    private static final String DISTRIBUCE = "PRE";


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

        // Rozpoznání zda jde o 2025 s počátkem před zářím
        boolean is2025Split = PdfParserUtils.is2025BeforeSeptember(platnostOD);
        if (is2025Split) {
            platnostOD = PdfParserUtils.normalize2025StartMillis(platnostOD);
        }
        long platnostDO = is2025Split
                ? PdfParserUtils.calculateEndOfAugust2025()
                : PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("distribuční[m\\s]+území:?\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(pdfText);
        if (distMatcher.find()) {
            String group1 = distMatcher.group(1);
            if (group1 != null) {
                distribuce = PdfParserUtils.getShortDistribuce(group1);
            }
        }
        if (DISTRIBUCE.equals(distribuce)) {
            String upperText = pdfText.toUpperCase();
            if (upperText.contains("EG.D") || upperText.contains("E.ON")) {
                distribuce = "EG.D";
            } else if (upperText.contains("ČEZ DISTRIBUCE") || upperText.contains("CEZ DISTRIBUCE") || upperText.contains("ÚZEMÍ ČEZ")) {
                distribuce = "ČEZ";
            } else if (upperText.contains("PREDISTRIBUCE") || upperText.contains("PRE DISTRIBUCE")) {
                distribuce = "PRE";
            }
        }

        // Extrakce obchodních cen bez DPH
        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');
        boolean bracketIsWithoutVat = cleanText.toLowerCase().contains("v závorkách jsou bez dph")
                || cleanText.toLowerCase().contains("v závorkách jsou bez dph.");

        List<Double> vtPrices = extractRowPrices(cleanText, "ve vysokém tarifu", bracketIsWithoutVat);
        List<Double> ntPrices = extractRowPrices(cleanText, "v nízkém tarifu", bracketIsWithoutVat);
        List<Double> platPrices = extractRowPrices(cleanText, "Měsíční plat za odběrné místo", bracketIsWithoutVat);

        double defaultPlat = !platPrices.isEmpty() ? platPrices.get(0) : 0.0;

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

        for (int i = 0; i < rateDefs.length; i++) {
            String[] def = rateDefs[i];
            String sazba = PdfParserUtils.formatSazba(def[0]);
            String rada = def[1];

            double vt = getVtForRate(sazba, i, vtPrices);
            double nt = getNtForRate(sazba, i, ntPrices);

            PriceListModel model = new PriceListModel(
                    0L,
                    rada,
                    produkt,
                    FIRMA,
                    vt,
                    nt,
                    defaultPlat,
                    0.0, // daň bez DPH
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // regulované ceny se načítají z raw JSON
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


    /**
     * Získává část textu PDF odpovídající výhradně sekci obchodních cen PRE.
     *
     * @param text Celkový vyčištěný text z PDF
     * @return Textový blok obsahující obchodní ceny
     */
    private String extractObchodniSection(String text) {
        Pattern pattern = Pattern.compile("Cena za spotřebovanou elektřinu[\\s\\S]*?(?=CENA ZA DISTRIBUOVANÉ|Regulovaná cena|Podmínky|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        String lastSection = null;
        while (matcher.find()) {
            lastSection = matcher.group(0);
        }
        if (lastSection != null) {
            return lastSection;
        }
        return text;
    }


    /**
     * Extrahuje číselné hodnoty obchodních cen bez DPH z konkrétního řádku PRE ceníku.
     *
     * @param text                Vyčištěný text z PDF
     * @param headerRegex         Regulární výraz pro záhlaví řádku
     * @param bracketIsWithoutVat true pokud jsou ceny bez DPH v tomto PDF v závorkách (formát PRE 2026)
     * @return Seznam cen bez DPH v Kč
     */
    private List<Double> extractRowPrices(String text, String headerRegex, boolean bracketIsWithoutVat) {
        String section = extractObchodniSection(text);
        String[] lines = section.split("\r?\n");

        boolean isPlat = headerRegex.toLowerCase().contains("měsíční plat");
        int[] offsets = isPlat ? new int[]{0, 1, 2} : new int[]{0, -1, 1, -2, 2};

        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].toLowerCase().contains(headerRegex.toLowerCase())) {
                for (int offset : offsets) {
                    int targetIndex = i + offset;
                    if (targetIndex >= 0 && targetIndex < lines.length) {
                        List<Double> extracted = extractPricesFromLine(lines[targetIndex], bracketIsWithoutVat);
                        if (!extracted.isEmpty()) {
                            return extracted;
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }


    private List<Double> extractPricesFromLine(String line, boolean bracketIsWithoutVat) {
        List<Double> prices = new ArrayList<>();
        if (line == null || line.isEmpty()) return prices;

        String targetText;
        if (bracketIsWithoutVat) {
            // V PRE 2026 ceníkách jsou ceny bez DPH uvnitř závorek: (3 570,00)
            StringBuilder sb = new StringBuilder();
            Matcher bm = Pattern.compile("\\(([^)]+)\\)").matcher(line);
            while (bm.find()) {
                String g = bm.group(1);
                if (g != null) {
                    sb.append(g).append(" ");
                }
            }
            targetText = sb.toString();
        } else {
            // V PRE 2025 ceníkách jsou ceny bez DPH vně závorek: 3 200,00 (3 872,00)
            targetText = line.replaceAll("\\([^)]*\\)", " ");
        }

        if (targetText.isEmpty()) {
            return prices;
        }

        Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s\\u00a0]\\d{3})*,\\d{2})");
        Matcher pm = pricePattern.matcher(targetText);
        while (pm.find()) {
            double p = PdfParserUtils.parsePrice(pm.group(1));
            if (p > 0) {
                prices.add(p);
            }
        }
        return prices;
    }


    private double getVtForRate(String sazba, int index, List<Double> vtPrices) {
        if (vtPrices.isEmpty()) return 0.0;
        if (vtPrices.size() == 10) return vtPrices.get(index);
        if (sazba.contains("01d") || sazba.contains("02d")) return vtPrices.get(0);
        if (sazba.contains("25d") || sazba.contains("26d"))
            return vtPrices.size() > 1 ? vtPrices.get(1) : vtPrices.get(0);
        if (sazba.contains("27d")) return vtPrices.size() > 2 ? vtPrices.get(2) : vtPrices.get(0);
        if (sazba.contains("35d")) return vtPrices.size() > 3 ? vtPrices.get(3) : vtPrices.get(0);
        if (sazba.contains("45d")) return vtPrices.size() > 4 ? vtPrices.get(4) : vtPrices.get(0);
        if (sazba.contains("56d")) return vtPrices.size() > 5 ? vtPrices.get(5) : vtPrices.get(0);
        if (sazba.contains("57d")) return vtPrices.size() > 6 ? vtPrices.get(6) : vtPrices.get(0);
        if (sazba.contains("61d")) return vtPrices.size() > 7 ? vtPrices.get(7) : vtPrices.get(0);
        return vtPrices.get(0);
    }


    private double getNtForRate(String sazba, int index, List<Double> ntPrices) {
        if (ntPrices.isEmpty() || sazba.contains("01d") || sazba.contains("02d")) return 0.0;
        if (ntPrices.size() == 10) return ntPrices.get(index);
        if (sazba.contains("25d") || sazba.contains("26d")) return ntPrices.get(0);
        if (sazba.contains("27d")) return ntPrices.size() > 1 ? ntPrices.get(1) : ntPrices.get(0);
        if (sazba.contains("35d")) return ntPrices.size() > 2 ? ntPrices.get(2) : ntPrices.get(0);
        if (sazba.contains("45d")) return ntPrices.size() > 3 ? ntPrices.get(3) : ntPrices.get(0);
        if (sazba.contains("56d")) return ntPrices.size() > 4 ? ntPrices.get(4) : ntPrices.get(0);
        if (sazba.contains("57d")) return ntPrices.size() > 5 ? ntPrices.get(5) : ntPrices.get(0);
        if (sazba.contains("61d")) return ntPrices.size() > 6 ? ntPrices.get(6) : ntPrices.get(0);
        return 0.0;
    }

}
