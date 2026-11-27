package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele ČEZ Prodej, a.s.
 * <p>
 * Detekuje PDF dokumenty ČEZ Prodej, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby. Obchodní ceny i regulované ceny se parsují dynamicky.
 */
public class CezPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "ČEZ Prodej, a.s.";
    private static final String DISTRIBUCE = "ČEZ";


    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("ČEZ PRODEJ") || text.contains("CEZ PRODEJ") || text.contains("WWW.CEZ.CZ");
    }


    @Override
    public List<PriceListModel> parse(String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');

        // Název produktu
        String produkt = "Elektřina na dobu neurčitou";
        Pattern produktPattern = Pattern.compile("Ceník elektřiny pro domácnosti\\s*\r?\n\\s*([^\r\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(cleanText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null && !pGroup.trim().isEmpty()) {
                produkt = pGroup.trim();
            }
        }
        if ("Elektřina na dobu neurčitou".equals(produkt)) {
            Pattern altProduktPattern = Pattern.compile("(Elektřina\\s+[^\r\n]+)", Pattern.CASE_INSENSITIVE);
            Matcher altProduktMatcher = altProduktPattern.matcher(cleanText);
            if (altProduktMatcher.find()) {
                String pGroup = altProduktMatcher.group(1);
                if (pGroup != null && !pGroup.trim().isEmpty()) {
                    produkt = pGroup.trim();
                }
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("Účinnost obchodních cen:\\s*od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(cleanText);
        if (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
            }
        }
        if (platnostOD == 0) {
            Pattern altOdPattern = Pattern.compile("od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
            Matcher altOdMatcher = altOdPattern.matcher(cleanText);
            if (altOdMatcher.find()) {
                String dateGroup = altOdMatcher.group(1);
                if (dateGroup != null) {
                    platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
                }
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
        Pattern distPattern = Pattern.compile("Distribuční území:\\s*([^\r\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(cleanText);
        if (distMatcher.find()) {
            String group1 = distMatcher.group(1);
            if (group1 != null) {
                distribuce = PdfParserUtils.getShortDistribuce(group1);
            }
        }

        // Seznam definovaných sazeb a řad
        String[][] rateDefs = {
                {"D 01d", "Standard"},
                {"D 02d", "Standard"},
                {"D 25d", "Akumulace 8"},
                {"D 26d", "Akumulace 8"},
                {"D 27d", "Elektromobilita"},
                {"D 35d", "Akumulace 16"},
                {"D 45d", "Přímotop"},
                {"D 56d", "Tepelné čerpadlo"},
                {"D 57d", "Elektrické topení"},
                {"D 61d", "Víkend"}
        };

        // Zkusíme najít dynamicky bloky cen z PDF podle číslovaných řádků ČEZ ceníku:
        // Řádek 1 = Obchodní cena: Vysoký tarif (VT)
        // Řádek 2 = Obchodní cena: Nízký tarif (NT)
        // Řádek 3 = Obchodní cena: Stálá platba Kč/měsíc
        // Řádek 4 = Distribuční cena: Vysoký tarif (VT)
        Pattern vtPattern = Pattern.compile("1[\\t\\s.]*Vysoký tarif[\\s\\S]*?(?=\r?\n\\s*2[\\t\\s.]|2[\\t\\s.]*Nízký tarif)", Pattern.CASE_INSENSITIVE);
        Pattern ntPattern = Pattern.compile("2[\\t\\s.]*Nízký tarif[\\s\\S]*?(?=\r?\n\\s*3[\\t\\s.]|3[\\t\\s.]*Stálá platba)", Pattern.CASE_INSENSITIVE);
        Pattern platPattern = Pattern.compile("3[\\t\\s.]*Stálá platba[\\s\\S]*?(?=\r?\n\\s*4[\\t\\s.]|Distribuční část|Cena za distribuci|$)", Pattern.CASE_INSENSITIVE);

        Matcher vtMatcher = vtPattern.matcher(cleanText);
        Matcher ntMatcher = ntPattern.matcher(cleanText);
        Matcher platMatcher = platPattern.matcher(cleanText);

        String platBlock = platMatcher.find() ? platMatcher.group(0) : "";
        List<Double> parsedVtList = extractPricesFromBlock(vtMatcher.find() ? vtMatcher.group(0) : "");
        List<Double> parsedNtList = extractPricesFromBlock(ntMatcher.find() ? ntMatcher.group(0) : "");
        List<Double> parsedPlatList = extractPricesFromBlock(platBlock);

        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = (i < parsedVtList.size() && parsedVtList.get(i) > 0) ? parsedVtList.get(i) : 0.0;

            // D 01d a D 02d nemají nízký tarif
            double nt = 0.0;
            if (!sazba.contains("01d") && !sazba.contains("02d")) {
                int ntIndex = (parsedNtList.size() == 10) ? i : i - 2;
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
                    0.0, // systémové služby
                    is2025Split ? 10.84 : 0.0, // činnost operátora pro 1. období 2025
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
     * Extrahuje ceny bez DPH (v závorce) z vybraného textového bloku.
     * Pokud ceny v závorce nenajde, zkusí načíst číselné hodnoty.
     *
     * @param block Textový blok příslušné řádky/sekce
     * @return Seznam vyextrahovaných cen bez DPH
     */
    private List<Double> extractPricesFromBlock(String block) {
        List<Double> prices = new ArrayList<>();
        if (block == null || block.isEmpty()) return prices;

        // V ČEZ cenících jsou ceny bez DPH v závorce: (2 950,41)
        Pattern bracketPattern = Pattern.compile("\\(([^)]+)\\)");
        Matcher matcher = bracketPattern.matcher(block);
        while (matcher.find()) {
            String val = matcher.group(1);
            if (val != null) {
                double price = PdfParserUtils.parsePrice(val);
                if (price > 0) {
                    prices.add(price);
                }
            }
        }

        // Záložní řešení pokud v závorkách žádná cena nebyla
        if (prices.isEmpty()) {
            Pattern numberPattern = Pattern.compile("\\b(\\d{1,3}(?:[\\s\\u00a0]\\d{3})*(?:[,.]\\d{1,2})?)\\b");
            Matcher numMatcher = numberPattern.matcher(block);
            while (numMatcher.find()) {
                String val = numMatcher.group(1);
                double price = PdfParserUtils.parsePrice(val);
                if (price > 10) {
                    prices.add(price);
                }
            }
        }
        return prices;
    }


    /**
     * Křížová kontrola správnosti načtených cen podle číslovaných řádků ceníku.
     * Ověřuje, zda obchodní cena VT (řádek 1) je menší než celková jednotková cena VT (řádek 26).
     *
     * @param pdfText      Text ceníku z PDF
     * @param parsedModels Seznam načtených ceníkových modelů
     * @return true pokud celková cena (řádek 26) odpovídá očekávání nebo řádek 26 v PDF chybí
     */
    public boolean validateRowCalculations(String pdfText, List<PriceListModel> parsedModels) {
        if (pdfText == null || parsedModels == null || parsedModels.isEmpty()) return true;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');
        Pattern totalVtPattern = Pattern.compile("26[\\t\\s.]*Vysoký tarif[\\s\\S]*?(?=\r?\n\\s*27[\\t\\s.]|27[\\t\\s.]*Nízký tarif|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = totalVtPattern.matcher(cleanText);
        if (matcher.find()) {
            List<Double> totalVtList = extractPricesFromBlock(matcher.group(0));
            if (!totalVtList.isEmpty()) {
                for (int i = 0; i < Math.min(parsedModels.size(), totalVtList.size()); i++) {
                    PriceListModel model = parsedModels.get(i);
                    double totalVtInPdf = totalVtList.get(i);
                    // Obchodní cena VT (řádek 1) musí být menší než celková cena (řádek 26)
                    if (model.getPriceVT() > 0 && totalVtInPdf > 0 && model.getPriceVT() >= totalVtInPdf) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
