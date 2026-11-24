package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele TEDOM energie s.r.o.
 * <p>
 * Detekuje PDF dokumenty TEDOM, extrahuje název produktu, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby včetně obchodních a regulovaných cen.
 * Podporuje standardní i měsíční (spotové) ceníky s křížovou kontrolou číslovaných řádků L (A+D+G+K) a M (B+E+G+K).
 */
public class TedomPdfParser implements PdfPriceListParser {

    private static final String FIRMA = "TEDOM energie s.r.o.";
    private static final String DISTRIBUCE = "ČEZ";


    /**
     * Zjistí, zda předaný text z PDF dokumentu pochází od dodavatele TEDOM energie s.r.o.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text obsahuje identifikátor dodavatele TEDOM
     */
    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("TEDOM");
    }


    /**
     * Rozparsuje text PDF ceníku TEDOM a vrátí seznam ceníkových modelů pro jednotlivé distribuční sazby.
     *
     * @param context Kontext aplikace
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Seznam načtených objektů {@link PriceListModel}
     */
    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');

        // Název produktu
        String produkt = "Jistota na měsíc";
        Pattern prodPattern1 = Pattern.compile("(Jistota\\s+na\\s+[^\n]+|T-Fix\\s+[^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher prodMatcher1 = prodPattern1.matcher(cleanText);
        if (prodMatcher1.find()) {
            String g1 = prodMatcher1.group(1);
            if (g1 != null) {
                produkt = g1.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("platný\\s+od\\s+(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
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

        // Detekce zda produkt obsahuje zimní garanci do 31. 3. YYYY
        long platnostDO = 0;
        Pattern winterEndPattern = Pattern.compile("fixní\\s+cenu[^\n]*?do\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher winterEndMatcher = winterEndPattern.matcher(cleanText);
        if (winterEndMatcher.find()) {
            String dateStr = winterEndMatcher.group(1);
            if (dateStr != null) {
                long seasonEnd = PdfParserUtils.parseDateToMillis(dateStr);
                if (seasonEnd > platnostOD) {
                    Calendar calW = Calendar.getInstance();
                    calW.setTimeInMillis(seasonEnd);
                    calW.set(Calendar.HOUR_OF_DAY, 23);
                    calW.set(Calendar.MINUTE, 59);
                    calW.set(Calendar.SECOND, 59);
                    calW.set(Calendar.MILLISECOND, 999);
                    long yearEnd = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
                    platnostDO = Math.min(yearEnd, calW.getTimeInMillis());
                }
            }
        }

        boolean is2025Split = PdfParserUtils.is2025BeforeSeptember(platnostOD);
        if (is2025Split) {
            platnostDO = PdfParserUtils.calculateEndOfAugust2025();
        } else if (platnostDO == 0) {
            platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
        }

        // Detekce distribučního území z PDF
        String distribuce = DISTRIBUCE;
        Pattern distPattern = Pattern.compile("Distribuční\\s+území\\s+([^\n,]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(cleanText);
        if (distMatcher.find()) {
            String group1 = distMatcher.group(1);
            if (group1 != null) {
                String rawDist = group1.toUpperCase();
                if (rawDist.contains("EG.D") || rawDist.contains("E.ON") || rawDist.contains("EGE")) {
                    distribuce = "EG.D";
                } else if (rawDist.contains("PRE")) {
                    distribuce = "PRE";
                } else if (rawDist.contains("ČEZ") || rawDist.contains("CEZ")) {
                    distribuce = "ČEZ";
                }
            }
        }

        boolean isMonthlyProduct = produkt.toLowerCase().contains("jistota na měsíc") || produkt.toLowerCase().contains("jistota na mesic");

        // Extrakce obchodních cen z řádků PDF
        List<Double> vtList = new ArrayList<>();
        List<Double> ntList = new ArrayList<>();
        List<Double> platList = new ArrayList<>();

        String[] lines = cleanText.split("\n");
        for (String l : lines) {
            String rowStr = l.trim();
            if (rowStr.startsWith("(")) continue; // Přeskočit ceny s DPH v závorce

            List<Double> rowNums = parseNumbersFromLine(rowStr);
            if (rowNums.size() == 10 && allInRange(rowNums, 10.0, 500.0) && platList.isEmpty()) {
                platList = rowNums;
            } else if (rowNums.size() == 10 && allInRange(rowNums, 1000.0, 10000.0) && vtList.isEmpty()) {
                vtList = rowNums;
            } else if (rowNums.size() == 8 && allInRange(rowNums, 1000.0, 10000.0) && ntList.isEmpty()) {
                ntList = rowNums;
            }
        }

        // Extrakce regulovaných cen z 2. sekce tabulky na straně 2 (poslední záhlaví sazeb D01d...)
        List<List<Double>> regRows = new ArrayList<>();
        int lastHeaderIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("D01d") && line.contains("D02d") && line.contains("D25d")) {
                lastHeaderIndex = i;
            }
        }

        if (lastHeaderIndex >= 0) {
            for (int j = lastHeaderIndex + 1; j < lines.length; j++) {
                String rowStr = lines[j].trim();
                if (rowStr.startsWith("(")) continue;
                List<Double> rowNums = parseNumbersFromLine(rowStr);
                if (!rowNums.isEmpty()) {
                    regRows.add(rowNums);
                }
            }
        }

        List<Double> regDistVT = (!regRows.isEmpty() && regRows.get(0).size() == 10) ? regRows.get(0) : new ArrayList<>();
        List<Double> regDistNT = (regRows.size() > 1 && regRows.get(1).size() == 8) ? regRows.get(1) : new ArrayList<>();
        List<Double> regJ0 = (regRows.size() > 2 && regRows.get(2).size() == 10) ? regRows.get(2) : new ArrayList<>();
        List<Double> regJ1 = (regRows.size() > 3 && regRows.get(3).size() == 10) ? regRows.get(3) : new ArrayList<>();
        List<Double> regJ2 = (regRows.size() > 4 && regRows.get(4).size() == 10) ? regRows.get(4) : new ArrayList<>();
        List<Double> regJ3 = (regRows.size() > 5 && regRows.get(5).size() == 10) ? regRows.get(5) : new ArrayList<>();
        List<Double> regJ4 = (regRows.size() > 6 && regRows.get(6).size() == 10) ? regRows.get(6) : new ArrayList<>();
        List<Double> regJ5 = (regRows.size() > 7 && regRows.get(7).size() == 10) ? regRows.get(7) : new ArrayList<>();
        List<Double> regJ6 = (regRows.size() > 8 && regRows.get(8).size() == 10) ? regRows.get(8) : new ArrayList<>();
        List<Double> regJ7 = (regRows.size() > 9 && regRows.get(9).size() == 10) ? regRows.get(9) : new ArrayList<>();
        List<Double> regJ10 = (regRows.size() > 10 && !regRows.get(10).isEmpty()) ? regRows.get(10) : new ArrayList<>();
        List<Double> regJ11 = (regRows.size() > 11 && !regRows.get(11).isEmpty()) ? regRows.get(11) : new ArrayList<>();
        List<Double> regJ12 = (regRows.size() > 12 && !regRows.get(12).isEmpty()) ? regRows.get(12) : new ArrayList<>();
        List<Double> regJ13 = (regRows.size() > 13 && !regRows.get(13).isEmpty()) ? regRows.get(13) : new ArrayList<>();
        List<Double> regJ14 = (regRows.size() > 14 && !regRows.get(14).isEmpty()) ? regRows.get(14) : new ArrayList<>();
        List<Double> regJ8 = (regRows.size() > 15 && regRows.get(15).size() >= 9) ? regRows.get(15) : new ArrayList<>();
        List<Double> regJ9 = (regRows.size() > 16 && regRows.get(16).size() == 10) ? regRows.get(16) : new ArrayList<>();

        String[][] rateDefs = {
                {"D 01d", "Klasik"},
                {"D 02d", "Klasik"},
                {"D 25d", "AKU"},
                {"D 26d", "AKU"},
                {"D 27d", "Elektromobil"},
                {"D 35d", "Kombi"},
                {"D 45d", "Přímotop"},
                {"D 56d", "Přímotop"},
                {"D 57d", "Přímotop"},
                {"D 61d", "Víkend"}
        };

        List<Double> monthlyVtPrices = isMonthlyProduct ? extractMonthlyPricesFromPage3(cleanText) : new ArrayList<>();

        if (isMonthlyProduct && monthlyVtPrices.size() == 12) {
            // Sestavení 12 měsíčních ceníků pro produkt Jistota na měsíc
            Calendar baseCal = Calendar.getInstance();
            baseCal.setTimeInMillis(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
            int year = baseCal.get(Calendar.YEAR);

            for (int m = 1; m <= 12; m++) {
                Calendar calStart = Calendar.getInstance();
                calStart.set(Calendar.YEAR, year);
                calStart.set(Calendar.MONTH, m - 1);
                calStart.set(Calendar.DAY_OF_MONTH, 1);
                calStart.set(Calendar.HOUR_OF_DAY, 0);
                calStart.set(Calendar.MINUTE, 0);
                calStart.set(Calendar.SECOND, 0);
                calStart.set(Calendar.MILLISECOND, 0);

                Calendar calEnd = Calendar.getInstance();
                calEnd.set(Calendar.YEAR, year);
                calEnd.set(Calendar.MONTH, m - 1);
                calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
                calEnd.set(Calendar.HOUR_OF_DAY, 23);
                calEnd.set(Calendar.MINUTE, 59);
                calEnd.set(Calendar.SECOND, 59);
                calEnd.set(Calendar.MILLISECOND, 999);

                long startM = calStart.getTimeInMillis();
                long endM = calEnd.getTimeInMillis();
                double monthlyVt = monthlyVtPrices.get(m - 1);

                boolean is2025M = calStart.get(Calendar.YEAR) == 2025;
                double mSysServ = is2025M ? 170.92 : 164.24;
                double mAct = is2025M ? (calStart.get(Calendar.MONTH) < Calendar.SEPTEMBER ? 10.84 : 12.45) : 12.87;
                double mPoze1 = is2025M ? 84.70 : 0.0;
                double mPoze2 = is2025M ? 495.00 : 0.0;

                for (int i = 0; i < rateDefs.length; i++) {
                    String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
                    String rada = rateDefs[i][1];
                    double plat = (i < platList.size() && platList.get(i) > 0) ? platList.get(i) : 109.0;
                    double nt = (sazba.contains("01d") || sazba.contains("02d")) ? 0.0 : monthlyVt;

                    double dVT = i < regDistVT.size() ? regDistVT.get(i) : 0.0;
                    double dNT = (!sazba.contains("01d") && !sazba.contains("02d") && (i - 2) < regDistNT.size()) ? regDistNT.get(i - 2) : 0.0;
                    double j0 = i < regJ0.size() ? regJ0.get(i) : 0.0;
                    double j1 = i < regJ1.size() ? regJ1.get(i) : 0.0;
                    double j2 = i < regJ2.size() ? regJ2.get(i) : 0.0;
                    double j3 = i < regJ3.size() ? regJ3.get(i) : 0.0;
                    double j4 = i < regJ4.size() ? regJ4.get(i) : 0.0;
                    double j5 = i < regJ5.size() ? regJ5.get(i) : 0.0;
                    double j6 = i < regJ6.size() ? regJ6.get(i) : 0.0;
                    double j7 = i < regJ7.size() ? regJ7.get(i) : 0.0;
                    double j10 = (i == 8 && !regJ10.isEmpty()) ? regJ10.get(0) : 0.0;
                    double j11 = (i == 8 && !regJ11.isEmpty()) ? regJ11.get(0) : 0.0;
                    double j12 = (i == 8 && !regJ12.isEmpty()) ? regJ12.get(0) : 0.0;
                    double j13 = (i == 8 && !regJ13.isEmpty()) ? regJ13.get(0) : 0.0;
                    double j14 = (i == 8 && !regJ14.isEmpty()) ? regJ14.get(0) : 0.0;
                    double j8 = (i != 8 && !regJ8.isEmpty()) ? regJ8.get(i < 8 ? i : i - 1) : 0.0;
                    double j9 = i < regJ9.size() ? regJ9.get(i) : 0.0;

                    PriceListModel model = new PriceListModel(
                            0L,
                            rada,
                            produkt,
                            FIRMA,
                            monthlyVt,
                            nt,
                            plat,
                            28.30,
                            sazba,
                            dVT, dNT, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12, j13, j14,
                            mSysServ, // systémové služby bez DPH
                            mAct,     // činnost operátora bez DPH
                            mPoze1,   // POZE1
                            mPoze2,   // POZE2
                            0.0, 0.0,
                            startM,
                            endM,
                            21.0,
                            distribuce,
                            "PDF Import",
                            System.currentTimeMillis(),
                            ""
                    );
                    result.add(model);
                }
            }
        } else {
            // Standardní roční / sezónní ceník
            for (int i = 0; i < rateDefs.length; i++) {
                String sazba = PdfParserUtils.formatSazba(rateDefs[i][0]);
                String rada = rateDefs[i][1];

                double vt = vtList.size() == 10 ? vtList.get(i) : 0.0;
                double nt = 0.0;
                if (!sazba.contains("01d") && !sazba.contains("02d")) {
                    int ntIndex = i - 2;
                    if (ntIndex >= 0 && ntIndex < ntList.size()) {
                        nt = ntList.get(ntIndex);
                    }
                }
                double plat = platList.size() == 10 ? platList.get(i) : 0.0;

                double dVT = i < regDistVT.size() ? regDistVT.get(i) : 0.0;
                double dNT = (!sazba.contains("01d") && !sazba.contains("02d") && (i - 2) < regDistNT.size()) ? regDistNT.get(i - 2) : 0.0;
                double j0 = i < regJ0.size() ? regJ0.get(i) : 0.0;
                double j1 = i < regJ1.size() ? regJ1.get(i) : 0.0;
                double j2 = i < regJ2.size() ? regJ2.get(i) : 0.0;
                double j3 = i < regJ3.size() ? regJ3.get(i) : 0.0;
                double j4 = i < regJ4.size() ? regJ4.get(i) : 0.0;
                double j5 = i < regJ5.size() ? regJ5.get(i) : 0.0;
                double j6 = i < regJ6.size() ? regJ6.get(i) : 0.0;
                double j7 = i < regJ7.size() ? regJ7.get(i) : 0.0;
                double j10 = (i == 8 && !regJ10.isEmpty()) ? regJ10.get(0) : 0.0;
                double j11 = (i == 8 && !regJ11.isEmpty()) ? regJ11.get(0) : 0.0;
                double j12 = (i == 8 && !regJ12.isEmpty()) ? regJ12.get(0) : 0.0;
                double j13 = (i == 8 && !regJ13.isEmpty()) ? regJ13.get(0) : 0.0;
                double j14 = (i == 8 && !regJ14.isEmpty()) ? regJ14.get(0) : 0.0;
                double j8 = (i != 8 && !regJ8.isEmpty()) ? regJ8.get(i < 8 ? i : i - 1) : 0.0;
                double j9 = i < regJ9.size() ? regJ9.get(i) : 0.0;

                Calendar calOD = Calendar.getInstance();
                calOD.setTimeInMillis(platnostOD > 0 ? platnostOD : System.currentTimeMillis());
                int yearOD = calOD.get(Calendar.YEAR);
                boolean isYear2025 = yearOD == 2025;

                double defaultSysServ = isYear2025 ? 170.92 : 164.24;
                double defaultAct = isYear2025 ? (is2025Split ? 10.84 : 12.45) : 12.87;
                double defaultPoze1 = isYear2025 ? 84.70 : 0.0;
                double defaultPoze2 = isYear2025 ? 495.00 : 0.0;

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
                        dVT, dNT, j0, j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, j11, j12, j13, j14,
                        defaultSysServ,
                        defaultAct,
                        defaultPoze1,
                        defaultPoze2,
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
        }

        return result;
    }


    /**
     * Křížová kontrola součtových řádků L (A+D+G+K) a M (B+E+G+K) z PDF oproti rozparsovaným ceníkovým modelům.
     *
     * @param pdfText    Text z PDF
     * @param parsedList Rozparsované modely
     * @return true pokud výpočty souhlasí s tolerancí 1.0 Kč nebo pokud jde o měsíční produkt
     */
    public boolean validateRowCalculations(String pdfText, List<PriceListModel> parsedList) {
        if (pdfText == null || parsedList == null || parsedList.isEmpty()) return true;

        String cleanText = pdfText.replace('\u00a0', ' ').replace('–', '-');
        if (cleanText.toLowerCase().contains("jistota na měsíc") || cleanText.toLowerCase().contains("jistota na mesic")) {
            return true; // Pro měsíc se celkové ceny počítají dynamicky
        }

        // Hledáme řádek L (Cena ve VT) a řádek M (Cena v NT)
        List<Double> lPrices = new ArrayList<>();
        List<Double> mPrices = new ArrayList<>();

        String[] lines = cleanText.split("\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.contains("Cena ve VT (Kč/MWh) L") || (l.contains("Cena ve VT") && l.contains("L"))) {
                List<Double> rowNums = parseNumbersFromLine(l);
                if (rowNums.size() == 10) lPrices = rowNums;
            }
            if (l.contains("Cena v NT (Kč/MWh) M") || (l.contains("Cena v NT") && l.contains("M"))) {
                List<Double> rowNums = parseNumbersFromLine(l);
                if (rowNums.size() == 8 || rowNums.size() == 10) mPrices = rowNums;
            }
        }

        if (lPrices.isEmpty()) return true;

        for (int i = 0; i < Math.min(10, parsedList.size()); i++) {
            PriceListModel m = parsedList.get(i);
            double calcL = m.getPriceVT() + m.getDistVT() + m.getSystemServices() + m.getTax();
            double pdfL = lPrices.get(i);
            if (Math.abs(calcL - pdfL) > 1.0) {
                return false;
            }
            if (!mPrices.isEmpty() && i >= 2) {
                int mIndex = mPrices.size() == 8 ? i - 2 : i;
                if (mIndex < mPrices.size()) {
                    double calcM = m.getPriceNT() + m.getDistNT() + m.getSystemServices() + m.getTax();
                    double pdfM = mPrices.get(mIndex);
                    if (Math.abs(calcM - pdfM) > 1.0) {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /**
     * Extrahuje 12 měsíčních cen bez DPH z tabulky na straně 3 pro produkt Jistota na měsíc.
     *
     * @param pdfText Extrahovaný text z PDF
     * @return Seznam 12 měsíčních cen bez DPH pro Leden až Prosinec
     */
    private List<Double> extractMonthlyPricesFromPage3(String pdfText) {
        List<Double> list = new ArrayList<>();
        if (pdfText == null) return list;

        int idx = pdfText.indexOf("Měsíc roku");
        if (idx < 0) {
            idx = pdfText.indexOf("Mesic roku");
        }
        if (idx < 0) {
            idx = pdfText.indexOf("Vypočtená obchodní cena");
        }
        if (idx < 0) {
            idx = pdfText.indexOf("Vypoctena obchodni cena");
        }
        int page3Index = idx >= 0 ? Math.max(0, idx - 1000) : 0;
        String p3Text = pdfText.substring(page3Index);

        String[] lines = p3Text.split("\n");
        List<Double> page3Floats = new ArrayList<>();
        for (String l : lines) {
            String clean = l.trim();
            if (!clean.startsWith("(") && !clean.endsWith(")")) {
                Pattern p = Pattern.compile("^\\d+(?:\\s\\d+)*,\\d{2}$");
                Matcher m = p.matcher(clean);
                if (m.find()) {
                    double price = PdfParserUtils.parsePrice(clean);
                    if (price > 100.0) {
                        page3Floats.add(price);
                    }
                }
            }
        }

        if (page3Floats.size() >= 12) {
            for (int i = 0; i < 12; i++) {
                list.add(page3Floats.get(i));
            }
        }
        return list;
    }


    private boolean allInRange(List<Double> numbers, double min, double max) {
        if (numbers == null || numbers.isEmpty()) return false;
        for (double d : numbers) {
            if (d < min || d > max) return false;
        }
        return true;
    }


    /**
     * Pomocná metoda pro extrakci číselných hodnot z řádku textu.
     *
     * @param line Řádek s textem
     * @return Seznam přečtených desetiných čísel bez DPH
     */
    private List<Double> parseNumbersFromLine(String line) {
        List<Double> list = new ArrayList<>();
        if (line == null || line.trim().startsWith("(")) return list;

        Pattern p = Pattern.compile("\\b(\\d+,\\d{1,2})\\b");
        Matcher m = p.matcher(line);
        while (m.find()) {
            String val = m.group(1);
            if (val != null) {
                double num = PdfParserUtils.parsePrice(val);
                list.add(num);
            }
        }
        return list;
    }

}
