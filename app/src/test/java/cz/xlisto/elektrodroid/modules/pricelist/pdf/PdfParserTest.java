package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Unit testy pro PDF parsery ceníků (ČEZ, PRE, E.ON).
 */
public class PdfParserTest {

    private static final String CEZ_SAMPLE_TEXT =
            """
                    Ceník elektřiny pro domácnosti
                    Elektřina na dobu neurčitou
                    www.cez.cz
                    Účinnost obchodních cen: od 1. 1. 2026
                    Distribuční území: ČEZ Distribuce, a.s.
                    1 Vysoký tarif Kč/MWh (3 190,08) (3 190,08) (3 272,73) (3 272,73) (3 272,73) (3 421,49) (3 421,49) (3 421,49) (3 421,49) (3 190,08)
                    2 Nízký tarif Kč/MWh – – (3 057,85) (3 057,85) (3 016,53) (3 322,31) (3 322,31) (3 322,31) (3 322,31) (3 190,08)
                    3 Stálá platba Kč/měsíc (135,00) (135,00) (121,00) (121,00) (121,00) (121,00) (121,00) (121,00) (121,00) (121,00)
                    """;

    private static final String PRE_SAMPLE_TEXT = "PREdistribuce, a. s.\nPRE PROUD EKO 10/2026\nCeník elektřiny pro domácnosti platný od 1. 10. 2026\n";

    private static final String EON_SAMPLE_TEXT = "E.ON Energie, a.s.\nEG.D, s.r.o.\nCeník Komplet elektřina MAX na 3 roky 11/26\nObchodní cena za elektřinu platná od 4. 11. 2026\n";

    private static final String CENTROPOL_SAMPLE_TEXT = "CENTROPOL ENERGY, a. s.\nCENÍK ELEKTŘINY SPOLEČNOSTI CENTROPOL ENERGY, a.s.\nProdukt/Ceník: FIXNĚ ONLINE PRO ZAČÁTEK na 3 roky\nDistribuční území: ČEZ Distribuce, a.s.\nCeník je účinný od 4.11.2026\n";

    private static final String MND_SAMPLE_TEXT = "MND Energie a.s.\nProdukt Proud - Domácnosti\nCeny obchodní za elektřinu pro domácnosti s DPH [bez DPH] platné od 1. 1. 2026 v distribuční oblasti EG.D, s.r.o.\n";

    private static final String INNOGY_SAMPLE_TEXT = "innogy Energie, s.r.o.\nelektřina Optimal 36 CENÍK ELEKTŘINY V KATEGORII DOMÁCNOST\nCeník je platný od 1. 11. 2026.\nč připojeno k distribuční soustavě EG.D, s.r.o.\n";

    private static final String CRE_SAMPLE_TEXT = "ČESKÁ REGIONÁLNÍ ENERGETIKA A.S.\nPlatný od 1. 1. 2026\nPRODUKTOVÁ ŘADA: FUTURE 2022 DISTRIBUČNÍ ÚZEMÍ: PREDISTRIBUCE, a.s.\n";


    @Test
    public void testCezPdfParser() {
        CezPdfParser parser = new CezPdfParser();
        assertTrue("ČEZ parser by měl detekovat text", parser.canParse(CEZ_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, CEZ_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("ČEZ ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("ČEZ Prodej, a.s.", d01.getFirma());
        assertEquals("ČEZ Distribuce, a.s.", d01.getDistribuce());
        assertEquals(3190.08, d01.getCenaVT(), 0.01);
        assertEquals(0.0, d01.getCenaNT(), 0.01);
        assertEquals(135.00, d01.getMesicniPlat(), 0.01);

        // Kontrola zastropování platnosti do konce roku (31.12.2026)
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testPrePdfParser() {
        PrePdfParser parser = new PrePdfParser();
        assertTrue("PRE parser by měl detekovat text", parser.canParse(PRE_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, PRE_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("PRE ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("Pražská energetika, a. s.", d01.getFirma());
        assertEquals("PREdistribuce, a. s.", d01.getDistribuce());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testEonPdfParser() {
        EonPdfParser parser = new EonPdfParser();
        assertTrue("E.ON parser by měl detekovat text", parser.canParse(EON_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, EON_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("E.ON ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("E.ON Energie, a.s.", d01.getFirma());
        assertEquals("EG.D, s.r.o.", d01.getDistribuce());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testCentropolPdfParser() {
        CentropolPdfParser parser = new CentropolPdfParser();
        assertTrue("Centropol parser by měl detekovat text", parser.canParse(CENTROPOL_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, CENTROPOL_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("Centropol ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("CENTROPOL ENERGY, a. s.", d01.getFirma());
        assertEquals("ČEZ Distribuce, a.s.", d01.getDistribuce());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testMndPdfParser() {
        MndPdfParser parser = new MndPdfParser();
        assertTrue("MND parser by měl detekovat text", parser.canParse(MND_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, MND_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("MND ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("MND Energie a.s.", d01.getFirma());
        assertEquals("EG.D, s.r.o.", d01.getDistribuce());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testInnogyPdfParser() {
        InnogyPdfParser parser = new InnogyPdfParser();
        assertTrue("innogy parser by měl detekovat text", parser.canParse(INNOGY_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, INNOGY_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("innogy ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("innogy Energie, s.r.o.", d01.getFirma());
        assertEquals("EG.D, s.r.o.", d01.getDistribuce());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testCrePdfParser() {
        CrePdfParser parser = new CrePdfParser();
        assertTrue("ČRE parser by měl detekovat text", parser.canParse(CRE_SAMPLE_TEXT));

        List<PriceListModel> items = parser.parse(null, CRE_SAMPLE_TEXT);
        assertNotNull(items);
        assertEquals("ČRE ceník by měl obsahovat 10 sazeb", 10, items.size());

        PriceListModel d01 = items.get(0);
        assertEquals("D 01d", d01.getSazba());
        assertEquals("Česká Regionální Energetika a.s.", d01.getFirma());
        assertEquals("PREdistribuce, a. s.", d01.getDistribuce());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d01.getPlatnostDO());
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }


    @Test
    public void testParserFactory() {
        List<PriceListModel> cezItems = PdfParserFactory.parsePdf(null, CEZ_SAMPLE_TEXT);
        assertEquals(10, cezItems.size());
        assertEquals("ČEZ Prodej, a.s.", cezItems.get(0).getFirma());

        List<PriceListModel> preItems = PdfParserFactory.parsePdf(null, PRE_SAMPLE_TEXT);
        assertEquals(10, preItems.size());
        assertEquals("Pražská energetika, a. s.", preItems.get(0).getFirma());

        List<PriceListModel> eonItems = PdfParserFactory.parsePdf(null, EON_SAMPLE_TEXT);
        assertEquals(10, eonItems.size());
        assertEquals("E.ON Energie, a.s.", eonItems.get(0).getFirma());
    }


    @Test
    public void testFormatSazba() {
        assertEquals("D 01d", PdfParserUtils.formatSazba("D01d"));
        assertEquals("D 01d", PdfParserUtils.formatSazba("D 01d"));
        assertEquals("D 26d", PdfParserUtils.formatSazba("D26d"));
        assertEquals("D 26d", PdfParserUtils.formatSazba("D 26d"));
        assertEquals("D 61d", PdfParserUtils.formatSazba("D61d"));
        assertEquals("C 01d", PdfParserUtils.formatSazba("C01d"));
    }


    @Test
    public void testCalculateEndOfYear() {
        long startMillis = PdfParserUtils.parseDateToMillis("1.10.2026");
        long endOfYearMillis = PdfParserUtils.calculateEndOfYear(startMillis);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(endOfYearMillis);
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

}
