package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Parser pro ceníky dodavatele Česká Regionální Energetika a.s. (ČRE).
 * <p>
 * Detekuje PDF dokumenty ČRE, extrahuje název produktové řady, platnost OD/DO, distribuční území
 * a sestaví ceníkové položky pro jednotlivé distribuční sazby. Obchodní i regulované ceny se parsují dynamicky.
 */
public class CrePdfParser implements PdfPriceListParser {

    private static final String FIRMA = "Česká Regionální Energetika a.s.";
    private static final String DISTRIBUCE = "PRE";


    @Override
    public boolean canParse(String pdfText) {
        if (pdfText == null) return false;
        String text = pdfText.toUpperCase();
        return text.contains("ČESKÁ REGIONÁLNÍ ENERGETIKA") || text.contains("CESKA REGIONALNI ENERGETIKA") || text.contains("CR-ENERGETIKA");
    }


    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null) return result;

        // Název produktu
        String produkt = "FUTURE 2022";
        Pattern produktPattern = Pattern.compile("PRODUKTOVÁ ŘADA:\\s*([^\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher produktMatcher = produktPattern.matcher(pdfText);
        if (produktMatcher.find()) {
            String pGroup = produktMatcher.group(1);
            if (pGroup != null) {
                produkt = pGroup.trim();
            }
        }

        // Datum platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("Platný od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
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
        Pattern distPattern = Pattern.compile("DISTRIBUČNÍ ÚZEMÍ:\\s*([^\n,]+)", Pattern.CASE_INSENSITIVE);
        Matcher distMatcher = distPattern.matcher(pdfText);
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
                {"D 01d", produkt},
                {"D 02d", produkt},
                {"D 25d", produkt},
                {"D 26d", produkt},
                {"D 27d", produkt},
                {"D 35d", produkt},
                {"D 45d", produkt},
                {"D 56d", produkt},
                {"D 57d", produkt},
                {"D 61d", produkt}
        };

        for (String[] def : rateDefs) {
            String sazba = PdfParserUtils.formatSazba(def[0]);
            String rada = def[1];

            PriceListModel model = new PriceListModel(
                    0L,
                    rada,
                    produkt,
                    FIRMA,
                    0.0,
                    0.0,
                    0.0,
                    0.0, // daň bez DPH
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, // regulované ceny se načítají z raw JSON
                    0.0, // systémové služby bez DPH
                    0.0, // činnost operátora bez DPH
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
        }

        return result;
    }

}
