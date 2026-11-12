package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Generický záložní parser pro pokus o extrakci distribučních sazeb z neznámého nebo neznámě strukturovaného PDF ceníku.
 * <p>
 * Slouží jako fallback v případě, že vyhledaný text z PDF dokumentu neodpovídá žádné z registrovaných šablon dodavatelů.
 */
public class GenericPdfParser implements PdfPriceListParser {

    private static final String[] SAZBY = {"D 01d", "D 02d", "D 25d", "D 26d", "D 27d", "D 35d", "D 45d", "D 56d", "D 57d", "D 61d"};


    /**
     * Zjistí, zda je předaný text vhodný ke zpracování. Jako záložní parser přijímá jakýkoliv neprázdný text.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text není null ani prázdný
     */
    @Override
    public boolean canParse(String pdfText) {
        return pdfText != null && !pdfText.isEmpty();
    }


    /**
     * Sestaví ceníkové položky pro standardní distribuční sazby z obecně rozpoznaných údajů v textu PDF.
     *
     * @param context Kontext aplikace
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Seznam generovaných objektů {@link PriceListModel}
     */
    @Override
    public List<PriceListModel> parse(Context context, String pdfText) {
        List<PriceListModel> result = new ArrayList<>();
        if (pdfText == null || pdfText.isEmpty()) return result;

        // Pokus o nalezení platnosti od
        long platnostOD = 0;
        Pattern odPattern = Pattern.compile("od\\s*(\\d{1,2}\\.\\s*\\d{1,2}\\.\\s*\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher odMatcher = odPattern.matcher(pdfText);
        if (odMatcher.find()) {
            String g1 = odMatcher.group(1);
            if (g1 != null) {
                platnostOD = PdfParserUtils.parseDateToMillis(g1);
            }
        }

        long platnostDO = PdfParserUtils.calculateEndOfYear(platnostOD > 0 ? platnostOD : System.currentTimeMillis());

        // Pokus o nalezení dodavatele z prvních řádků
        String firma = "Dodavatel";
        if (pdfText.contains("ČEZ") || pdfText.contains("CEZ")) firma = "ČEZ Prodej, a.s.";
        else if (pdfText.contains("PRE") || pdfText.contains("Pražská energetika"))
            firma = "Pražská energetika, a. s.";
        else if (pdfText.contains("E.ON") || pdfText.contains("EG.D")) firma = "E.ON Energie, a.s.";

        String distribuce = "ČEZ Distribuce, a.s.";
        if (pdfText.contains("PREdistribuce")) distribuce = "PREdistribuce, a. s.";
        else if (pdfText.contains("EG.D")) distribuce = "EG.D, s.r.o.";

        String produkt = "PDF Ceník";

        for (String sazba : SAZBY) {
            PriceListModel model = new PriceListModel(
                    0L,
                    "Standard",
                    produkt,
                    firma,
                    0.0,
                    0.0,
                    0.0,
                    28.30,
                    sazba,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
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
