package cz.xlisto.elektrodroid.modules.pricelist.pdf;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;

/**
 * Parser pro ceníky dodavatele MND Energie a.s. (Moravské naftové doly).
 * <p>
 * Detekuje PDF dokumenty MND, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby. Obchodní ceny se vyhledávají
 * primárně z obchodní tabulky v příloze ceníku se záložním zpracováním vertikální tabulky.
 */
public class MndPdfParser implements PdfPriceListParser {

    /**
     * Název dodavatele zapisovaný do výsledných instancí {@link PriceListModel}.
     */
    private static final String FIRMA = "MND Energie a.s.";
    /** Výchozí distribuční území použité v případě, že oblast není v textu PDF rozpoznána. */
    private static final String DISTRIBUCE = "EG.D";


    /**
     * Ověří, zda předaný text PDF patří ceníku dodavatele MND Energie.
     *
     * @param pdfText extrahovaný text PDF dokumentu
     * @return {@code true}, pokud text obsahuje identifikátor MND ({@code MND ENERGIE}, {@code MND.CZ} nebo {@code MND}); jinak {@code false}
     */
    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("MND ENERGIE") || text.contains("MND.CZ") || text.contains("MND");
    }


    /**
     * Zpracuje text PDF ceníku MND a sestaví seznam ceníků pro jednotlivé distribuční sazby.
     * <p>
     * Extrahuje název produktu, období platnosti (včetně případného stropu „do konce ...“
     * a rozdělení roku 2025 na dvě období před a od září), distribuční území a obchodní ceny
     * VT, NT a stálého měsíčního platu. Regulované složky ponechává nulové pro dynamické načtení
     * z interního JSON souboru.
     *
     * @param context aplikační kontext vyžadovaný rozhraním {@link PdfPriceListParser} (v této implementaci se nepoužívá)
     * @param pdfText extrahovaný text PDF ceníku
     * @return seznam sestavených položek {@link PriceListModel}; při prázdném nebo {@code null} vstupu prázdný seznam
     */
    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-').replace('—', '-');

        // Název produktu
        String produkt = "Proud - Domácnosti";
        Pattern produktPattern = Pattern.compile("Produkt\\s+([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(cleanText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("platné\\s+(?:pro\\s+domácnosti\\s+)?od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(cleanText);
        if (odMatcher.find()) {
            String dateGroup = odMatcher.group(1);
            if (dateGroup != null) {
                platnostOD = PdfParserUtils.parseDateToMillis(dateGroup);
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

        Calendar calOD = Calendar.getInstance();
        calOD.setTimeInMillis(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

        long platnostDO = 0;
        Pattern stropPattern = Pattern.compile("do\\s+konce\\s+(března|června|září|prosince)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher stropMatcher = stropPattern.matcher(cleanText);
        if (stropMatcher.find()) {
            String monthGroup = stropMatcher.group(1);
            String yearGroup = stropMatcher.group(2);
            if (monthGroup != null && yearGroup != null) {
                String monthName = monthGroup.toLowerCase();
                int yearStrop = Integer.parseInt(yearGroup);
                Calendar calStrop = getCalStrop(yearStrop, monthName);
                long endOfYear = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
                platnostDO = Math.min(endOfYear, calStrop.getTimeInMillis());
            }
        }

        boolean is2025Split = calOD.get(Calendar.YEAR) == 2025 && PdfParserUtils.is2025BeforeSeptember(platnostOD) && (platnostDO == 0 || platnostDO > PdfParserUtils.calculateEndOfAugust2025());

        if (is2025Split) {
            platnostDO = PdfParserUtils.calculateEndOfAugust2025();
        } else if (platnostDO == 0) {
            platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
        }

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("distribuční oblasti\\s+([^\n,]+)", Pattern.CASE_INSENSITIVE);
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

        String[][] rateDefs = {
                {"D 01d", "Standard"},
                {"D 02d", "Standard"},
                {"D 25d", "Aku 8H"},
                {"D 26d", "Aku 8H"},
                {"D 27d", "Elektromobil"},
                {"D 35d", "Aku 16H"},
                {"D 45d", "Přímotop"},
                {"D 56d", "Tepelné čerpadlo"},
                {"D 57d", "Elektrické topení"},
                {"D 61d", "Víkend"}
        };

        int[] vtSeries7 = {0, 0, 1, 1, 1, 2, 3, 4, 5, 6};
        int[] rateSeries8 = {0, 0, 1, 1, 2, 3, 4, 5, 6, 7};
        int[] ntSeries7 = {0, 0, 1, 2, 3, 4, 5, 6};

        double[] vtPrices = new double[10];
        double[] ntPrices = new double[10];
        double[] platPrices = new double[10];

        // 1. Vyhledání obchodní tabulky z Přílohy ceníku (zpravidla druhá stránka)
        int prilohaIndex = cleanText.indexOf("Příloha ceníku");
        String textToParse = (prilohaIndex >= 0) ? cleanText.substring(prilohaIndex) : cleanText;
        String[] lines = textToParse.split("\n");

        List<String> commLines = new ArrayList<>();
        boolean inComm = false;
        for (String l : lines) {
            String lower = l.toLowerCase();
            if (lower.contains("ceny obchodní") || lower.contains("obchodní sazby")) {
                inComm = true;
            } else if (lower.contains("distribuční sazba") || lower.contains("ceny a sazby regulované") || lower.contains("cena za distribuované")) {
                if (inComm && !commLines.isEmpty()) {
                    break;
                }
            }

            if (inComm) {
                commLines.add(l);
            }
        }

        List<Double> vts = new ArrayList<>();
        List<Double> nts = new ArrayList<>();
        List<Double> plats = new ArrayList<>();
        String currentMode = null;

        for (String l : commLines) {
            String lower = l.toLowerCase();
            if (lower.contains("ve vysokém tarifu")) {
                currentMode = "vt";
            } else if (lower.contains("v nízkém tarifu")) {
                currentMode = "nt";
            } else if ((lower.contains("měsíční platba") || lower.contains("stálý plat") || lower.contains("stálá platba")) && !lower.contains("celkem")) {
                currentMode = "plat";
            }

            List<Double> b = extractBracketPricesFromText(l);
            if (!b.isEmpty() && currentMode != null) {
                switch (currentMode) {
                    case "plat" -> plats.addAll(b);
                    case "vt" -> vts.addAll(b);
                    case "nt" -> nts.addAll(b);
                }
            }
        }

        if (!vts.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                if (vts.size() == 1) vtPrices[i] = vts.get(0);
                else if (vts.size() == 7) vtPrices[i] = vts.get(vtSeries7[i]);
                else if (vts.size() == 8) vtPrices[i] = vts.get(rateSeries8[i]);
                else if (i < vts.size()) vtPrices[i] = vts.get(i);

                if (plats.size() == 1) platPrices[i] = plats.get(0);
                else if (plats.size() == 7) platPrices[i] = plats.get(vtSeries7[i]);
                else if (plats.size() == 8) platPrices[i] = plats.get(rateSeries8[i]);
                else if (i < plats.size()) platPrices[i] = plats.get(i);

                if (i >= 2) {
                    if (nts.size() == 1) ntPrices[i] = nts.get(0);
                    else if (nts.size() == 7) ntPrices[i] = nts.get(ntSeries7[i - 2]);
                    else if (nts.size() == 8) ntPrices[i] = nts.get(i - 2);
                    else if (i - 2 < nts.size()) ntPrices[i] = nts.get(i - 2);
                }
            }
        } else {
            // 2. Fallback na Layout 2 (vertikální tabulku) pokud obchodní tabulka nebyla nalezena
            boolean inRegulatedSection = false;
            for (int i = 0; i < lines.length; i++) {
                String l = lines[i];
                String lower = l.toLowerCase();
                if (lower.contains("ceny a sazby regulované") || lower.contains("regulované ceny") || lower.contains("distribuční sazba") || lower.contains("cena za distribuované")) {
                    inRegulatedSection = true;
                }
                if (inRegulatedSection) {
                    continue;
                }

                Pattern rPat = Pattern.compile("D\\s*\\d{1,2}\\s*d", Pattern.CASE_INSENSITIVE);
                List<String> mRates = new ArrayList<>();
                for (int k = Math.max(0, i - 5); k <= i; k++) {
                    Matcher rMatK = rPat.matcher(lines[k]);
                    while (rMatK.find()) {
                        String code = rMatK.group().toLowerCase().replace(" ", "");
                        if (!mRates.contains(code)) {
                            mRates.add(code);
                        }
                    }
                }

                if (!mRates.isEmpty()) {
                    List<Double> brackets = extractBracketPricesFromText(l);
                    if (!brackets.isEmpty()) {
                        for (int k = 0; k < mRates.size(); k++) {
                            String rcode = mRates.get(k);
                            double vtVal;
                            double ntVal = 0.0;

                            if (brackets.size() == mRates.size()) {
                                vtVal = brackets.get(k);
                            } else if (brackets.size() == mRates.size() * 2) {
                                vtVal = brackets.get(k);
                                ntVal = brackets.get(mRates.size() + k);
                            } else {
                                vtVal = brackets.get(0);
                                if (brackets.size() >= 2) ntVal = brackets.get(1);
                            }

                            for (int rIdx = 0; rIdx < rateDefs.length; rIdx++) {
                                String sazbaClean = PdfParserUtils.formatSazba(rateDefs[rIdx][0]).replace(" ", "");
                                if (sazbaClean.equalsIgnoreCase(rcode)) {
                                    if (vtPrices[rIdx] == 0.0) vtPrices[rIdx] = vtVal;
                                    if (ntPrices[rIdx] == 0.0) ntPrices[rIdx] = ntVal;
                                }
                            }
                        }
                    }
                }
            }
        }

        int yearOD = calOD.get(Calendar.YEAR);
        double poze2 = (yearOD < 2026) ? 495.00 : 0.0;

        for (int i = 0; i < rateDefs.length; i++) {
            String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
            String rada = rateDefs[i][1];

            double vt = vtPrices[i];
            double nt = sazba.contains("01d") || sazba.contains("02d") ? 0.0 : ntPrices[i];
            double plat = platPrices[i];

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
                    0.0, // systémové služby bez DPH
                    is2025Split ? 10.84 : 0.0, // činnost operátora bez DPH pro 1. období 2025
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


    /**
     * Vytvoří datum konce platnosti (poslední den měsíce v čase {@code 23:59:59.999})
     * podle roku a českého názvu čtvrtletního měsíce z textu cenového stropu.
     *
     * @param yearStrop rok konce platnosti
     * @param monthName název měsíce malými písmeny (očekává kořen slov březen, červen, září nebo prosinec)
     * @return instance {@link Calendar} nastavená na konec odpovídajícího měsíce
     */
    @NonNull
    private static Calendar getCalStrop(int yearStrop, String monthName) {
        Calendar calStrop = Calendar.getInstance();
        calStrop.set(Calendar.YEAR, yearStrop);
        if (monthName.contains("březn")) {
            calStrop.set(Calendar.MONTH, Calendar.MARCH);
            calStrop.set(Calendar.DAY_OF_MONTH, 31);
        } else if (monthName.contains("červn")) {
            calStrop.set(Calendar.MONTH, Calendar.JUNE);
            calStrop.set(Calendar.DAY_OF_MONTH, 30);
        } else if (monthName.contains("září")) {
            calStrop.set(Calendar.MONTH, Calendar.SEPTEMBER);
            calStrop.set(Calendar.DAY_OF_MONTH, 30);
        } else if (monthName.contains("prosinc")) {
            calStrop.set(Calendar.MONTH, Calendar.DECEMBER);
            calStrop.set(Calendar.DAY_OF_MONTH, 31);
        }
        calStrop.set(Calendar.HOUR_OF_DAY, 23);
        calStrop.set(Calendar.MINUTE, 59);
        calStrop.set(Calendar.SECOND, 59);
        calStrop.set(Calendar.MILLISECOND, 999);
        return calStrop;
    }


    /**
     * Extrahuje z řádku textu všechny kladné číselné ceny uzavřené v hranatých závorkách {@code [...]}.
     * Popisky {@code [bez DPH]} jsou ignorovány.
     *
     * @param text jeden řádek obchodní tabulky z PDF
     * @return seznam nalezených kladných cen v pořadí výskytu; při prázdném nebo {@code null} vstupu prázdný seznam
     */
    private List<Double> extractBracketPricesFromText(String text) {
        List<Double> prices = new ArrayList<>();
        if (text == null || text.isEmpty()) return prices;

        Pattern bracketPattern = Pattern.compile("\\[([^]]+)]");
        Matcher matcher = bracketPattern.matcher(text);
        while (matcher.find()) {
            String val = matcher.group(1);
            if (val != null && !val.equalsIgnoreCase("bez DPH")) {
                double price = PdfParserUtils.parsePrice(val);
                if (price > 0) {
                    prices.add(price);
                }
            }
        }
        return prices;
    }
}
