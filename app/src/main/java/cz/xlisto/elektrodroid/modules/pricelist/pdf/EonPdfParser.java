package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

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
     * @param context Kontext aplikace
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Seznam načtených objektů {@link PriceListModel} s obchodními cenami
     */
    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
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

        long platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

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

        // Extrakce obchodních cen bez DPH
        // V E.ON ceníku jsou 5 řad: Klasik (D01d, D02d), Aku (D25d, D26d, D27d), Kombi (D35d), Přímotop (D45d, D56d, D57d), Víkend (D61d)
        List<Double> vtPrices = extractRowPrices(cleanText, "Cena ve vysokém tarifu \\(VT\\)");
        List<Double> ntPrices = extractRowPrices(cleanText, "Cena v nízkém tarifu \\(NT\\)");
        List<Double> platPrices = extractRowPrices(cleanText, "Stálý měsíční plat");

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
                    0.0,
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
        }

        return result;
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
