package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele E.ON Energie, a.s. (distribuční území EG.D, ČEZ, PRE).
 * <p>
 * Detekuje PDF dokumenty E.ON / EG.D, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby s načtenými obchodními cenami.
 */
public class EonPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "E.ON Energie, a.s.";
    private static final String DEFAULT_DISTRIBUCE = "EG.D";


    /**
     * Zjistí, zda předaný text z PDF dokumentu pochází od dodavatele E.ON Energie, a.s. nebo EG.D.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return {@code true}, pokud text obsahuje identifikátory E.ON nebo EG.D
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
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Seznam načtených objektů {@link PriceListModel} s obchodními cenami
     */
    @Override
    public List<PriceListModel> parse(String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');

        // Název produktu
        String produkt = "Komplet elektřina MAX";
        Pattern produktPattern = Pattern.compile("Produktová řada\\s+([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(cleanText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        } else {
            Pattern altProduktPattern = Pattern.compile("Ceník\\s+([^\n]+)", Pattern.CASE_INSENSITIVE);
            Matcher altProduktMatcher = altProduktPattern.matcher(cleanText);
            if (altProduktMatcher.find()) {
                String pGroup = altProduktMatcher.group(1);
                if (pGroup != null) {
                    produkt = pGroup.trim();
                }
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("platná\\s+od\\s+(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(cleanText);
        if (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
            }
        }
        if (platnostOD == 0) {
            Pattern altOdPattern = Pattern.compile("od\\s+(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})");
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

        // Detekce distribučního území
        String distribuce = DEFAULT_DISTRIBUCE;
        String upperText = cleanText.toUpperCase();
        if (upperText.contains("PREDISTRIBUCE") || upperText.contains("PRE")) {
            distribuce = "PRE";
        } else if (upperText.contains("ČEZ DISTRIBUCE") || upperText.contains("CEZ DISTRIBUCE")) {
            distribuce = "ČEZ";
        } else if (upperText.contains("EG.D") || upperText.contains("E.ON")) {
            distribuce = "EG.D";
        }

        // Extrakce obchodních cen bez DPH z číslovaných řádků
        // V E.ON ceníku: ř. 01 (VT), ř. 02 (NT), ř. 03 (Stálý plat)
        // V E.ON ceníku je 5 řad: Klasik (D01d, D02d), Aku (D25d, D26d, D27d), Kombi (D35d), Přímotop (D45d, D56d, D57d), Víkend (D61d)
        List<Double> vtPrices = extractRowPricesByNumber(cleanText, "01", "Cena ve vysokém tarifu \\(VT\\)");
        if (vtPrices.isEmpty()) {
            vtPrices = extractRowPrices(cleanText, "Cena ve vysokém tarifu \\(VT\\)");
        }
        List<Double> ntPrices = extractRowPricesByNumber(cleanText, "02", "Cena v nízkém tarifu \\(NT\\)");
        if (ntPrices.isEmpty()) {
            ntPrices = extractRowPrices(cleanText, "Cena v nízkém tarifu \\(NT\\)");
        }
        List<Double> platPrices = extractRowPricesByNumber(cleanText, "03", "Stálý měsíční plat");
        if (platPrices.isEmpty()) {
            platPrices = extractRowPrices(cleanText, "Stálý měsíční plat");
        }

        // Fallback na 1. stranu ceníku pokud se z tabulky na 2. straně nezískalo 5 cen
        if (vtPrices.size() < 5) {
            vtPrices = extractPage1VtPrices(cleanText);
        }
        if (ntPrices.size() < 4) {
            ntPrices = extractPage1NtPrices(cleanText);
        }
        if (platPrices.isEmpty()) {
            platPrices = extractPage1PlatPrices(cleanText);
        }

        double defaultPlat = !platPrices.isEmpty() ? platPrices.get(0) : 0.0;

        String[][] rateDefs = {
                {"D 01d", "Klasik"},
                {"D 02d", "Klasik"},
                {"D 25d", "Aku"},
                {"D 26d", "Aku"},
                {"D 27d", "Aku"},
                {"D 35d", "Kombi"},
                {"D 45d", "Přímotop"},
                {"D 56d", "Přímotop"},
                {"D 57d", "Přímotop"},
                {"D 61d", "Víkend"}
        };

        for (String[] def : rateDefs) {
            String sazba = PdfParserUtils.formatSazba(def[0]);
            String rada = def[1];

            double vt = getVtForRate(sazba, vtPrices);
            double nt = getNtForRate(sazba, ntPrices);

            PriceListModel model = new PriceListModel(
                    0L,
                    rada,
                    produkt,
                    FIRMA,
                    vt,
                    nt,
                    defaultPlat,
                    0.0,
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0,
                    is2025Split ? 10.84 : 0.0,
                    0.0,
                    0.0,
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
     * Získává část textu PDF odpovídající výhradně sekci obchodních cen
     * (od "Obchodní cena za dodávku elektřiny" po "Regulovaná cena").
     *
     * @param text Celkový vyčištěný text z PDF
     * @return Textový blok obsahující pouze obchodní ceny
     */
    private String extractObchodniSection(String text) {
        Pattern pattern = Pattern.compile("Obchodní cena za dodávku elektřiny[\\s\\S]*?(?=Regulovaná cena|Celková jednotková|Ostatní regulované|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return text;
    }


    /**
     * Extrahuje číselné hodnoty obchodních cen bez DPH z konkrétního číslovaného řádku (např. 01, 02, 03) v E.ON ceníku.
     *
     * @param text        Extrahovaný a vyčištěný text z PDF
     * @param rowNumLong  Číslo řádku s úvodní nulou (např. "01", "02", "03")
     * @param headerRegex Regulární výraz pro vyhledání záhlaví řádku cen
     * @return Seznam přečtených cenových hodnot bez DPH
     */
    private List<Double> extractRowPricesByNumber(String text, String rowNumLong, String headerRegex) {
        List<Double> prices = new ArrayList<>();
        if (text == null || text.isEmpty()) return prices;

        String section = extractObchodniSection(text);

        // 1. Zkusíme řádek s číslem 01/02/03 obsahující ceny bez DPH
        Pattern pattern = Pattern.compile(headerRegex + "[^\n]*?\n\\s*([^\n]*?\\b" + rowNumLong + "\\b[^\n]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(section);
        if (matcher.find()) {
            prices.addAll(extractPricesFromLine(matcher.group(1)));
        }

        // 2. Pokud záhlaví obsahuje číslo řádku na stejném řádku jako název, ceny bez DPH jsou na následujícím řádku
        if (prices.isEmpty()) {
            Pattern altPattern = Pattern.compile(headerRegex + "[^\n]*?\\b" + rowNumLong + "\\b[^\n]*?\n\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
            Matcher altMatcher = altPattern.matcher(section);
            if (altMatcher.find()) {
                prices.addAll(extractPricesFromLine(altMatcher.group(1)));
            }
        }

        return prices;
    }


    /**
     * Extrahuje seznam číselných cen ve formátu X XXX,XX z daného řádku.
     *
     * @param line Řádek textu
     * @return Seznam cen v Kč
     */
    private List<Double> extractPricesFromLine(String line) {
        List<Double> prices = new ArrayList<>();
        if (line == null || line.isEmpty()) return prices;
        Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s\\u00a0]\\d{3})*,\\d{2})");
        Matcher pm = pricePattern.matcher(line);
        while (pm.find()) {
            double p = PdfParserUtils.parsePrice(pm.group(1));
            if (p > 0) {
                prices.add(p);
            }
        }
        return prices;
    }


    /**
     * Křížová kontrola správnosti načtených cen podle číslovaných řádků ceníku E.ON.
     * Ověřuje, zda obchodní cena VT (řádek 01) je menší než celková jednotková cena VT (řádek 25).
     *
     * @param pdfText      Text ceníku z PDF
     * @param parsedModels Seznam načtených ceníkových modelů
     * @return true pokud celková cena (řádek 25) odpovídá očekávání nebo řádek 25 v PDF chybí
     */
    public boolean validateRowCalculations(String pdfText, List<PriceListModel> parsedModels) {
        if (pdfText == null || parsedModels == null || parsedModels.isEmpty()) return true;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');

        // Vyhledá blok řádku 25 (Celková cena VT) - např. "Cena ve vysokém tarifu (VT)... 25" nebo "25... Vysoký tarif"
        Pattern totalVtPattern = Pattern.compile("25[\\t\\s.]*Cena ve vysokém tarifu|Celková jednotková cena[\\s\\S]*?Vysoký tarif[\\s\\S]*?25\\b|Vysoký tarif[^\n]*?\n[^\n]*?25\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = totalVtPattern.matcher(cleanText);
        if (matcher.find()) {
            List<Double> totalVtList = extractPricesFromBlock(matcher.group(0));
            if (!totalVtList.isEmpty()) {
                for (int i = 0; i < Math.min(parsedModels.size(), totalVtList.size()); i++) {
                    PriceListModel model = parsedModels.get(i);
                    double totalVtInPdf = totalVtList.get(i);
                    // Obchodní cena VT (řádek 01) musí být menší než celková jednotková cena VT (řádek 25)
                    if (model.getPriceVT() > 0 && totalVtInPdf > 0 && model.getPriceVT() >= totalVtInPdf) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    /**
     * Extrahuje číselné hodnoty cen bez DPH z vybraného textového bloku pro účely křížové kontroly.
     *
     * @param block Textový blok příslušné řádky/sekce
     * @return Seznam vyextrahovaných cen bez DPH
     */
    private List<Double> extractPricesFromBlock(String block) {
        List<Double> prices = new ArrayList<>();
        if (block == null || block.isEmpty()) return prices;

        String[] lines = block.split("\r?\n");
        String targetLine = lines.length > 1 ? lines[lines.length - 1] : block;

        Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s\\u00a0]\\d{3})*,\\d{2})");
        Matcher pm = pricePattern.matcher(targetLine);
        while (pm.find()) {
            double p = PdfParserUtils.parsePrice(pm.group(1));
            if (p > 100) {
                prices.add(p);
            }
        }

        if (prices.isEmpty()) {
            Matcher pmBlock = pricePattern.matcher(block);
            while (pmBlock.find()) {
                double p = PdfParserUtils.parsePrice(pmBlock.group(1));
                if (p > 100) {
                    prices.add(p);
                }
            }
        }
        return prices;
    }


    /**
     * Extrahuje číselné hodnoty obchodních cen bez DPH z řádku tabulky E.ON podle záhlaví.
     *
     * @param text        Extrahovaný a vyčištěný text z PDF
     * @param headerRegex Regulární výraz pro vyhledání záhlaví řádku cen
     * @return Seznam přečtených cenových hodnot v Kč/MWh nebo Kč/měsíc bez DPH
     */
    private List<Double> extractRowPrices(String text, String headerRegex) {
        List<Double> prices = new ArrayList<>();
        Pattern pattern = Pattern.compile(headerRegex + "[^\n]*?\n\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String line = matcher.group(1);
            if (line != null) {
                // Vyhledá všechny číselné ceny bez DPH (ve formátu 3 299,00)
                Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s\\u00a0]\\d{3})*,\\d{2})");
                Matcher pm = pricePattern.matcher(line);
                while (pm.find()) {
                    String raw = pm.group(1);
                    double p = PdfParserUtils.parsePrice(raw);
                    if (p > 0) {
                        prices.add(p);
                    }
                }
            }
        }
        return prices;
    }


    /**
     * Extrahuje ceny vysokého tarifu (VT) bez DPH z úvodní přehledové tabulky na 1. straně ceníku.
     *
     * @param text Vyčištěný text PDF dokumentu
     * @return Seznam načtených cen VT pro jednotlivé produktové řady
     */
    private List<Double> extractPage1VtPrices(String text) {
        List<Double> prices = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d[\\s\\d]{2,5})\\s+-\\s+(\\d{2,4})");
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            double p = PdfParserUtils.parsePrice(m.group(2));
            if (p > 1000) prices.add(p);
        }
        Pattern pattern2 = Pattern.compile("(\\d[\\s\\d]{2,5})\\s+(\\d[\\s\\d]{2,5})\\s+(\\d{2,4})");
        Matcher m2 = pattern2.matcher(text);
        while (m2.find()) {
            double p = PdfParserUtils.parsePrice(m2.group(3));
            if (p > 1000) prices.add(p);
        }
        return prices;
    }


    /**
     * Extrahuje ceny nízkého tarifu (NT) bez DPH z úvodní přehledové tabulky na 1. straně ceníku.
     *
     * @param text Vyčištěný text PDF dokumentu
     * @return Seznam načtených cen NT pro dvoutarifní produktové řady
     */
    private List<Double> extractPage1NtPrices(String text) {
        List<Double> prices = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d[\\s\\d]{2,5})\\s+(\\d[\\s\\d]{2,5})\\s+(\\d{2,4})");
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            double p = PdfParserUtils.parsePrice(m.group(3));
            if (p > 1000) prices.add(p);
        }
        return prices;
    }


    /**
     * Extrahuje stálý měsíční plat bez DPH z úvodní přehledové tabulky na 1. straně ceníku.
     *
     * @param text Vyčištěný text PDF dokumentu
     * @return Seznam načtených cen stálého měsíčního platu
     */
    private List<Double> extractPage1PlatPrices(String text) {
        List<Double> prices = new ArrayList<>();
        Pattern pattern = Pattern.compile("Kč/měsíc[^\n]*?\n[^\n]*?(\\d{2,4})");
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            double p = PdfParserUtils.parsePrice(m.group(1));
            if (p > 0) prices.add(p);
        }
        return prices;
    }


    /**
     * Přiřadí příslušnou cenu vysokého tarifu (VT) konkrétní distribuční sazbě na základě produktové řady.
     *
     * @param sazba    Název distribuční sazby (např. "D 01d", "D 25d")
     * @param vtPrices Seznam načtených cen VT bez DPH pro produktové řady
     * @return Cena VT bez DPH v Kč/MWh pro zadanou sazbu
     */
    private double getVtForRate(String sazba, List<Double> vtPrices) {
        if (vtPrices.isEmpty()) return 0.0;
        if (sazba.contains("01d") || sazba.contains("02d")) {
            return vtPrices.get(0);
        }
        if (sazba.contains("25d") || sazba.contains("26d") || sazba.contains("27d")) {
            return vtPrices.size() > 1 ? vtPrices.get(1) : vtPrices.get(0);
        }
        if (sazba.contains("35d")) {
            return vtPrices.size() > 2 ? vtPrices.get(2) : vtPrices.get(0);
        }
        if (sazba.contains("45d") || sazba.contains("56d") || sazba.contains("57d")) {
            return vtPrices.size() > 3 ? vtPrices.get(3) : vtPrices.get(0);
        }
        if (sazba.contains("61d")) {
            return vtPrices.size() > 4 ? vtPrices.get(4) : vtPrices.get(0);
        }
        return vtPrices.get(0);
    }


    /**
     * Přiřadí příslušnou cenu nízkého tarifu (NT) konkrétní distribuční sazbě na základě produktové řady.
     * Pro jednotarifní sazby (D 01d, D 02d) vrací 0.0.
     *
     * @param sazba    Název distribuční sazby (např. "D 25d", "D 57d")
     * @param ntPrices Seznam načtených cen NT bez DPH pro dvoutarifní produktové řady
     * @return Cena NT bez DPH v Kč/MWh pro zadanou sazbu, nebo 0.0 pro jednotarifní sazby
     */
    private double getNtForRate(String sazba, List<Double> ntPrices) {
        if (ntPrices.isEmpty() || sazba.contains("01d") || sazba.contains("02d")) {
            return 0.0;
        }
        if (sazba.contains("25d") || sazba.contains("26d") || sazba.contains("27d")) {
            return ntPrices.get(0);
        }
        if (sazba.contains("35d")) {
            return ntPrices.size() > 1 ? ntPrices.get(1) : ntPrices.get(0);
        }
        if (sazba.contains("45d") || sazba.contains("56d") || sazba.contains("57d")) {
            return ntPrices.size() > 2 ? ntPrices.get(2) : ntPrices.get(0);
        }
        if (sazba.contains("61d")) {
            return ntPrices.size() > 3 ? ntPrices.get(3) : ntPrices.get(0);
        }
        return 0.0;
    }

}
