package cz.xlisto.elektrodroid.modules.pricelist.pdf;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;

/**
 * Parser pro ceníky dodavatele CENTROPOL ENERGY, a. s.
 * <p>
 * Detekuje PDF dokumenty Centropol, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby. Obchodní i regulované ceny se parsují dynamicky.
 */
public class CentropolPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "CENTROPOL ENERGY, a. s.";
    private static final String DISTRIBUCE = "ČEZ";

    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("CENTROPOL");
    }

    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');

        // Název produktu
        String produkt = "FIXNĚ ONLINE PRO ZAČÁTEK";
        Pattern produktPattern = Pattern.compile("Produkt(?:/Ceník)?:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(cleanText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("(?:účinný|účinné|platný|platné)\\s+od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(cleanText);
        while (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                long m = PdfParserUtils.parseDateToMillis(dateGroup);
                if (m > platnostOD) {
                    platnostOD = m;
                }
            }
        }
        if (platnostOD == 0) {
            Pattern altOdPattern = Pattern.compile("od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})");
            Matcher altOdMatcher = altOdPattern.matcher(cleanText);
            if (altOdMatcher.find()) {
                String dateGroup = altOdMatcher.group(1);
                if (dateGroup != null) {
                    platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
                }
            }
        }

        boolean isMonthlyProduct = produkt.toLowerCase().contains("měsíčně") || produkt.toLowerCase().contains("mesicne");

        boolean is2025Split = PdfParserUtils.is2025BeforeSeptember(platnostOD);
        long platnostDO = 0;
        if (isMonthlyProduct && platnostOD > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(platnostOD);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            platnostDO = cal.getTimeInMillis();
        } else if (is2025Split) {
            platnostDO = PdfParserUtils.calculateEndOfAugust2025();
        } else {
            platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
        }

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("Distribuční\\s+území:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(cleanText);
        if (distMatcher.find()) {
            String group1 = distMatcher.group(1);
            if (group1 != null) {
                String rawDist = group1.toUpperCase();
                if (rawDist.contains("EG.D") || rawDist.contains("E.ON")) {
                    distribuce = "EG.D";
                } else if (rawDist.contains("PRE")) {
                    distribuce = "PRE";
                } else if (rawDist.contains("ČEZ") || rawDist.contains("CEZ")) {
                    distribuce = "ČEZ";
                }
            }
        }

        // Extrakce obchodních cen (v závorce bez DPH)
        List<Double> parsedVtList = new ArrayList<>();
        List<Double> parsedNtList = new ArrayList<>();
        List<Double> parsedPlatList = new ArrayList<>();

        String[] lines = cleanText.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String lower = line.toLowerCase();

            if (lower.contains("vysoký tarif") && !lower.contains("cena za odběr") && parsedVtList.isEmpty()) {
                String target = line;
                if (!line.contains("(") && i + 1 < lines.length && lines[i + 1].contains("(")) {
                    target = lines[i + 1];
                }
                parsedVtList = extractPricesFromLine(target);
            } else if (lower.contains("nízký tarif") && !lower.contains("cena za odběr") && parsedNtList.isEmpty()) {
                String target = line;
                if (!line.contains("(") && i + 1 < lines.length && lines[i + 1].contains("(")) {
                    target = lines[i + 1];
                }
                parsedNtList = extractPricesFromLine(target);
            } else if ((lower.contains("fixní cena") || lower.contains("stálá platba")) && parsedPlatList.isEmpty()) {
                String target = line;
                if (!line.contains("(") && i + 1 < lines.length && lines[i + 1].contains("(")) {
                    target = lines[i + 1];
                }
                parsedPlatList = extractPricesFromLine(target);
            }
        }

        // Fallback pro ceníky bez číslovaných řádků (např. d-mini-tarif)
        if (parsedVtList.isEmpty()) {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.contains("D01d") && line.contains("D02d") && line.contains("D25d")) {
                    for (int k = i + 1; k < Math.min(i + 8, lines.length); k++) {
                        String l = lines[k].trim();
                        if (l.contains("(")) {
                            List<Double> rowNums = extractPricesFromLine(l);
                            if (rowNums.size() == 10 && parsedVtList.isEmpty()) {
                                parsedVtList = rowNums;
                            } else if (rowNums.size() == 8 && parsedNtList.isEmpty()) {
                                parsedNtList = rowNums;
                            } else if (rowNums.size() == 10 && parsedPlatList.isEmpty()) {
                                parsedPlatList = rowNums;
                            }
                        }
                    }
                    break;
                }
            }
        }

        String[][] rateDefs = {
                {"D 01d", "Základní ceník"},
                {"D 02d", "Základní ceník"},
                {"D 25d", "AKU 8"},
                {"D 26d", "AKU 8"},
                {"D 27d", "AKU 8"},
                {"D 35d", "AKU 16"},
                {"D 45d", "Přímotop"},
                {"D 56d", "Tepelné čerpadlo"},
                {"D 57d", "Elektrické topení"},
                {"D 61d", "Víkend"}
        };

        long regDateMillis = 0;
        Pattern regOdPattern = Pattern.compile("Ceny\\s+distribuce[^\n]*?účinné\\s+od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher regOdMatcher = regOdPattern.matcher(cleanText);
        if (regOdMatcher.find()) {
            String dateGroup = regOdMatcher.group(1);
            if (dateGroup != null) {
                regDateMillis = PdfParserUtils.parseDateToMillis(dateGroup);
            }
        }
        if (regDateMillis == 0) {
            regDateMillis = platnostOD;
        }

        Calendar regCal = Calendar.getInstance();
        regCal.setTimeInMillis(regDateMillis > 0 ? regDateMillis : System.currentTimeMillis());
        int regYear = regCal.get(Calendar.YEAR);

        double poze2 = (regYear < 2026) ? 495.00 : 0.0;

        System.out.println("DEBUG Centropol cleanText contains 495? " + cleanText.contains("495") + " poze2=" + poze2);
        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = (i < parsedVtList.size() && parsedVtList.get(i) > 0) ? parsedVtList.get(i) : 0.0;

            double nt = 0.0;
            if (!sazba.contains("01d") && !sazba.contains("02d")) {
                int ntIndex = i - 2;
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
                    0.0, // daň bez DPH - načítá se z raw JSON
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // regulované ceny se načítají z raw JSON
                    0.0, // systémové služby bez DPH
                    0.0, // činnost operátora bez DPH
                    0.0, // POZE1
                    poze2, // POZE2
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
