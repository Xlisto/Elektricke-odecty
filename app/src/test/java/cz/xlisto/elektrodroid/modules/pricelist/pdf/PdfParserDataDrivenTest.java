package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Datově řízený unit test pro ověření PDF parserů vůči uloženým JSON specifikacím.
 * Podporuje jak jednotliné JSON specifikace, tak i více JSON specifikací pro jeden PDF ceník
 * (např. při rozdělení období v průběhu roku, např. od 1.1.2025 do 31.8.2025 a od 1.9.2025 do 31.12.2025).
 */
public class PdfParserDataDrivenTest {

    private static final String RESOURCE_BASE_PATH = "pdf_pricelists";


    @Test
    public void testAllPdfPriceListsInResources() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            System.out.println("ClassLoader nebyl nalezen.");
            return;
        }

        URL rootUrl = classLoader.getResource(RESOURCE_BASE_PATH);
        if (rootUrl == null) {
            System.out.println("Složka " + RESOURCE_BASE_PATH + " nebyla v resources nalezena. Vytvořte ji v app/src/test/resources/" + RESOURCE_BASE_PATH);
            return;
        }

        File rootDir = new File(rootUrl.toURI());
        List<File> pdfFiles = findPdfFiles(rootDir);

        if (pdfFiles.isEmpty()) {
            System.out.println("V " + rootDir.getAbsolutePath() + " zatím nejsou žádné PDF soubory pro automatický test.");
            return;
        }

        for (File pdfFile : pdfFiles) {
            List<File> matchingJsonFiles = findMatchingJsonFiles(pdfFile);

            if (matchingJsonFiles.isEmpty()) {
                System.out.println("Přeskakuji PDF (chybí JSON specifikace): " + pdfFile.getName());
                continue;
            }

            testPdfWithJsonFiles(pdfFile, matchingJsonFiles);
        }
    }


    private void testPdfWithJsonFiles(File pdfFile, List<File> jsonFiles) throws Exception {
        System.out.println("Ověřuji PDF: " + pdfFile.getName() + " (počet JSON specifikací: " + jsonFiles.size() + ")");

        String pdfText;
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            pdfText = stripper.getText(doc);
        }

        List<PriceListModel> parsedList = PdfParserFactory.parsePdf(null, pdfText);
        assertNotNull("Výsledek parsování PDF " + pdfFile.getName() + " nesmí být null", parsedList);

        if (pdfText.contains("ČEZ PRODEJ") || pdfText.contains("CEZ PRODEJ")) {
            assertTrue("Křížová kontrola číslovaných řádků ČEZ pro " + pdfFile.getName() + " selhala",
                    new CezPdfParser().validateRowCalculations(pdfText, parsedList));
        }

        for (File jsonFile : jsonFiles) {
            testSingleJsonSpecification(jsonFile, parsedList);
        }
    }


    private void testSingleJsonSpecification(File jsonFile, List<PriceListModel> fullParsedList) throws Exception {
        System.out.println("  -> Kontrola vůči JSON: " + jsonFile.getName());

        String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
        JSONObject expectedJson = new JSONObject(jsonContent);

        List<PriceListModel> filteredModels = filterModelsForJson(fullParsedList, expectedJson);

        if (expectedJson.has("expectedRatesCount")) {
            assertEquals("Počet sazeb pro " + jsonFile.getName(),
                    expectedJson.getInt("expectedRatesCount"), filteredModels.size());
        }

        if (!filteredModels.isEmpty()) {
            PriceListModel firstModel = filteredModels.get(0);

            if (expectedJson.has("company")) {
                assertEquals("Firma v " + jsonFile.getName(),
                        expectedJson.getString("company"), firstModel.getCompany());
            }

            if (expectedJson.has("distribution")) {
                String expectedDist = PdfParserUtils.getShortDistribuce(expectedJson.getString("distribution"));
                String actualDist = PdfParserUtils.getShortDistribuce(firstModel.getDistribution());
                assertEquals("Distribuce v " + jsonFile.getName(), expectedDist, actualDist);
            }

            // Kontrola data platnosti
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
            if (expectedJson.has("validFrom")) {
                String parsedValidFrom = dateFormat.format(firstModel.getValidFrom());
                assertEquals("Platnost OD v " + jsonFile.getName(),
                        expectedJson.getString("validFrom"), parsedValidFrom);
            }
            if (expectedJson.has("validTo")) {
                String parsedValidTo = dateFormat.format(firstModel.getValidTo());
                assertEquals("Platnost DO v " + jsonFile.getName(),
                        expectedJson.getString("validTo"), parsedValidTo);
            }
            if (expectedJson.has("validFromMillis")) {
                assertEquals("Platnost OD (millis) v " + jsonFile.getName(),
                        expectedJson.getLong("validFromMillis"), (long) firstModel.getValidFrom());
            }
            if (expectedJson.has("validToMillis")) {
                assertEquals("Platnost DO (millis) v " + jsonFile.getName(),
                        expectedJson.getLong("validToMillis"), (long) firstModel.getValidTo());
            }
        }

        if (expectedJson.has("rates")) {
            Map<String, PriceListModel> parsedMap = filteredModels.stream()
                    .collect(Collectors.toMap(PriceListModel::getRate, item -> item, (r1, r2) -> r1));

            JSONArray expectedRates = expectedJson.getJSONArray("rates");
            int ratesCount = expectedRates.length();
            for (int index = 0; index < ratesCount; index++) {
                JSONObject rateJson = expectedRates.getJSONObject(index);
                String rateName = rateJson.getString("rate");

                assertTrue("Sazba " + rateName + " by měla být v rozparsovaném období pro " + jsonFile.getName(),
                        parsedMap.containsKey(rateName));

                PriceListModel parsedModel = parsedMap.get(rateName);
                assertNotNull("Model pro sazbu " + rateName + " nesmí být null v " + jsonFile.getName(), parsedModel);

                // Kontrola obchodních cen
                if (rateJson.has("priceVT")) {
                    assertEquals("VT u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("priceVT"), parsedModel.getPriceVT(), 0.01);
                }
                if (rateJson.has("priceNT")) {
                    assertEquals("NT u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("priceNT"), parsedModel.getPriceNT(), 0.01);
                }
                if (rateJson.has("monthlyPayment")) {
                    assertEquals("Stálý plat u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("monthlyPayment"), parsedModel.getMonthlyPayment(), 0.01);
                }

                // Kontrola regulovaných cen a poplatků
                if (rateJson.has("distVT")) {
                    assertEquals("distVT u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("distVT"), parsedModel.getDistVT(), 0.01);
                }
                if (rateJson.has("distNT")) {
                    assertEquals("distNT u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("distNT"), parsedModel.getDistNT(), 0.01);
                }
                if (rateJson.has("tax")) {
                    assertEquals("tax u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("tax"), parsedModel.getTax(), 0.01);
                }
                if (rateJson.has("vat")) {
                    assertEquals("vat u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("vat"), parsedModel.getVat(), 0.01);
                }
                if (rateJson.has("systemServices")) {
                    assertEquals("systemServices u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("systemServices"), parsedModel.getSystemServices(), 0.01);
                }
                if (rateJson.has("activity")) {
                    assertEquals("activity u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("activity"), parsedModel.getActivity(), 0.01);
                }
                if (rateJson.has("poze1")) {
                    assertEquals("poze1 u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("poze1"), parsedModel.getPoze1(), 0.01);
                }
                if (rateJson.has("poze2")) {
                    assertEquals("poze2 u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("poze2"), parsedModel.getPoze2(), 0.01);
                }
                if (rateJson.has("oze")) {
                    assertEquals("oze u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("oze"), parsedModel.getOze(), 0.01);
                }
                if (rateJson.has("ote")) {
                    assertEquals("ote u " + rateName + " v " + jsonFile.getName(),
                            rateJson.getDouble("ote"), parsedModel.getOte(), 0.01);
                }

                // Kontrola jističů
                if (rateJson.has("jistice")) {
                    JSONObject j = rateJson.getJSONObject("jistice");
                    if (j.has("j0"))
                        assertEquals("j0 u " + rateName, j.getDouble("j0"), parsedModel.getJ0(), 0.01);
                    if (j.has("j1"))
                        assertEquals("j1 u " + rateName, j.getDouble("j1"), parsedModel.getJ1(), 0.01);
                    if (j.has("j2"))
                        assertEquals("j2 u " + rateName, j.getDouble("j2"), parsedModel.getJ2(), 0.01);
                    if (j.has("j3"))
                        assertEquals("j3 u " + rateName, j.getDouble("j3"), parsedModel.getJ3(), 0.01);
                    if (j.has("j4"))
                        assertEquals("j4 u " + rateName, j.getDouble("j4"), parsedModel.getJ4(), 0.01);
                    if (j.has("j5"))
                        assertEquals("j5 u " + rateName, j.getDouble("j5"), parsedModel.getJ5(), 0.01);
                    if (j.has("j6"))
                        assertEquals("j6 u " + rateName, j.getDouble("j6"), parsedModel.getJ6(), 0.01);
                    if (j.has("j7"))
                        assertEquals("j7 u " + rateName, j.getDouble("j7"), parsedModel.getJ7(), 0.01);
                    if (j.has("j8"))
                        assertEquals("j8 u " + rateName, j.getDouble("j8"), parsedModel.getJ8(), 0.01);
                    if (j.has("j9"))
                        assertEquals("j9 u " + rateName, j.getDouble("j9"), parsedModel.getJ9(), 0.01);
                    if (j.has("j10"))
                        assertEquals("j10 u " + rateName, j.getDouble("j10"), parsedModel.getJ10(), 0.01);
                    if (j.has("j11"))
                        assertEquals("j11 u " + rateName, j.getDouble("j11"), parsedModel.getJ11(), 0.01);
                    if (j.has("j12"))
                        assertEquals("j12 u " + rateName, j.getDouble("j12"), parsedModel.getJ12(), 0.01);
                    if (j.has("j13"))
                        assertEquals("j13 u " + rateName, j.getDouble("j13"), parsedModel.getJ13(), 0.01);
                    if (j.has("j14"))
                        assertEquals("j14 u " + rateName, j.getDouble("j14"), parsedModel.getJ14(), 0.01);
                }
            }
        }
    }


    private List<PriceListModel> filterModelsForJson(List<PriceListModel> fullList, JSONObject expectedJson) {
        if (!expectedJson.has("validFromMillis") && !expectedJson.has("validFrom")) {
            return fullList;
        }

        List<PriceListModel> result = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

        long targetValidFromMillis = expectedJson.optLong("validFromMillis", -1);
        String targetValidFromStr = expectedJson.optString("validFrom", "");

        for (PriceListModel model : fullList) {
            boolean matches = false;
            if (targetValidFromMillis > 0) {
                // Porovnání podle milisekund (s tolerancí 1 dne / 86400000ms)
                if (Math.abs(model.getValidFrom() - targetValidFromMillis) < 86400000L) {
                    matches = true;
                }
            } else if (!targetValidFromStr.isEmpty()) {
                String modelValidFromStr = dateFormat.format(model.getValidFrom());
                if (Objects.equals(targetValidFromStr, modelValidFromStr)) {
                    matches = true;
                }
            }

            if (matches) {
                result.add(model);
            }
        }

        return result.isEmpty() ? fullList : result;
    }


    private List<File> findMatchingJsonFiles(File pdfFile) {
        List<File> jsonFiles = new ArrayList<>();
        File parentDir = pdfFile.getParentFile();
        if (parentDir == null || !parentDir.isDirectory()) {
            return jsonFiles;
        }

        String pdfName = pdfFile.getName();
        String baseName = pdfName.replaceAll("(?i)\\.pdf$", "");

        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if (name.toLowerCase().endsWith(".json")) {
                    String jsonBase = name.replaceAll("(?i)\\.json$", "");
                    if (jsonBase.equals(baseName) || jsonBase.startsWith(baseName + "__")) {
                        jsonFiles.add(f);
                    }
                }
            }
        }
        jsonFiles.sort(java.util.Comparator.comparing(File::getName));
        return jsonFiles;
    }


    private List<File> findPdfFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    result.addAll(findPdfFiles(f));
                } else if (f.getName().toLowerCase().endsWith(".pdf")) {
                    result.add(f);
                }
            }
        }
        return result;
    }

}
