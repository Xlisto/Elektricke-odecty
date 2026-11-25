package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Unit testy pro registr podporovaných PDF ceníků v {@link PdfParserFactory}
 * a výpočet podvzorkování náhledů v {@link AssetPreviewImageLoader}.
 */
public class SupportedPriceListCatalogTest {

    @Test
    public void testAllSevenSuppliersAreRegistered() {
        List<SupportedPriceList> items = PdfParserFactory.getSupportedPriceLists();
        assertEquals("Registr musí obsahovat 7 podporovaných dodavatelů", 7, items.size());

        Set<String> ids = new HashSet<>();
        Set<String> supplierIds = new HashSet<>();
        for (SupportedPriceList item : items) {
            assertNotNull(item.getId());
            assertFalse(item.getId().isEmpty());
            assertTrue("Duplicitní ID položky: " + item.getId(), ids.add(item.getId()));

            assertNotNull(item.getSupplierId());
            assertFalse(item.getSupplierId().isEmpty());
            supplierIds.add(item.getSupplierId());

            assertNotEquals("Chybí string resource názvu dodavatele", 0, item.getSupplierNameResId());
            assertNotEquals("Chybí string resource popisu", 0, item.getDescriptionResId());
            assertNotNull(item.getParserId());
            assertFalse(item.getParserId().isEmpty());
            assertTrue("Cesta k náhledu musí končit .webp", item.getPreviewAssetPath().endsWith(".webp"));
        }

        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_CENTROPOL));
        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_CEZ));
        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_EON));
        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_INNOGY));
        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_MND));
        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_PRE));
        assertTrue(supplierIds.contains(PdfParserFactory.SUPPLIER_ID_TEDOM));
    }


    @Test
    public void testSamplePageCountsAndWarnings() {
        SupportedPriceList centropol = PdfParserFactory.findSupportedPriceListBySupplierId(
                PdfParserFactory.SUPPLIER_ID_CENTROPOL
        );
        assertNotNull(centropol);
        assertEquals(1, centropol.getSamplePageCount());
        assertFalse(centropol.hasWarning());
        assertEquals(0, centropol.getWarningResId());

        String[] twoPageSuppliers = {
                PdfParserFactory.SUPPLIER_ID_CEZ,
                PdfParserFactory.SUPPLIER_ID_EON,
                PdfParserFactory.SUPPLIER_ID_INNOGY,
                PdfParserFactory.SUPPLIER_ID_MND,
                PdfParserFactory.SUPPLIER_ID_PRE,
                PdfParserFactory.SUPPLIER_ID_TEDOM
        };

        for (String supplierId : twoPageSuppliers) {
            SupportedPriceList item = PdfParserFactory.findSupportedPriceListBySupplierId(supplierId);
            assertNotNull("Dodavatel nebyl nalezen: " + supplierId, item);
            assertEquals("Očekáván dvoustránkový ukázkový ceník pro " + supplierId, 2, item.getSamplePageCount());

            if (PdfParserFactory.SUPPLIER_ID_EON.equals(supplierId)) {
                assertTrue("E.ON musí obsahovat upozornění na zkrácený ceník", item.hasWarning());
                assertNotEquals(0, item.getWarningResId());
            } else {
                assertFalse("Dodavatel " + supplierId + " nemá mít specifické upozornění", item.hasWarning());
                assertEquals(0, item.getWarningResId());
            }
        }
    }


    @Test
    public void testAllPreviewAssetFilesExistOnDisk() {
        File assetsDir = new File("src/main/assets");
        if (!assetsDir.exists()) {
            assetsDir = new File("app/src/main/assets");
        }
        assertTrue("Adresář assets nebyl nalezen: " + assetsDir.getAbsolutePath(), assetsDir.isDirectory());

        for (SupportedPriceList item : PdfParserFactory.getSupportedPriceLists()) {
            File previewFile = new File(assetsDir, item.getPreviewAssetPath());
            assertTrue("Chybí náhledový obrázek: " + previewFile.getPath(), previewFile.isFile());
            assertTrue("Náhledový obrázek nesmí být prázdný: " + previewFile.getPath(), previewFile.length() > 0);
        }
    }


    @Test
    public void testDetectSupplierIdForAllSupportedSuppliers() {
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_CENTROPOL,
                PdfParserFactory.detectSupplierId("CENTROPOL ENERGY, a.s. Ceník elektřiny")
        );
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_CEZ,
                PdfParserFactory.detectSupplierId("ČEZ Prodej, a.s. www.cez.cz Ceník elektřiny")
        );
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_EON,
                PdfParserFactory.detectSupplierId("E.ON Energie, a.s. Ceník elektřiny")
        );
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_INNOGY,
                PdfParserFactory.detectSupplierId("innogy Energie, s.r.o. Ceník elektřiny")
        );
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_MND,
                PdfParserFactory.detectSupplierId("MND Energie a.s. Ceník elektřiny")
        );
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_PRE,
                PdfParserFactory.detectSupplierId("Pražská energetika, a.s. PRE PROUD")
        );
        assertEquals(
                PdfParserFactory.SUPPLIER_ID_TEDOM,
                PdfParserFactory.detectSupplierId("TEDOM energie s.r.o. Jistota na měsíc")
        );

        assertNull(PdfParserFactory.detectSupplierId("Neznámý dodavatel s.r.o."));
        assertNull(PdfParserFactory.detectSupplierId(""));
        assertNull(PdfParserFactory.detectSupplierId(null));

        assertTrue(PdfParserFactory.isKnownCompany("CENTROPOL ENERGY, a.s."));
        assertFalse(PdfParserFactory.isKnownCompany("Neznámý dodavatel s.r.o."));
    }


    @Test
    public void testFindSupportedPriceListBySupplierIdEdgeCases() {
        assertNull(PdfParserFactory.findSupportedPriceListBySupplierId(null));
        assertNull(PdfParserFactory.findSupportedPriceListBySupplierId(""));
        assertNull(PdfParserFactory.findSupportedPriceListBySupplierId("unknown_supplier"));
    }


    @Test
    public void testCalculateInSampleSize() {
        assertEquals(1, AssetPreviewImageLoader.calculateInSampleSize(400, 300, 600, 400));
        assertEquals(1, AssetPreviewImageLoader.calculateInSampleSize(800, 600, 600, 400));
        assertEquals(2, AssetPreviewImageLoader.calculateInSampleSize(1600, 1200, 600, 400));
        assertEquals(4, AssetPreviewImageLoader.calculateInSampleSize(2400, 1700, 600, 400));
        assertEquals(8, AssetPreviewImageLoader.calculateInSampleSize(4800, 3200, 600, 400));
    }

}
