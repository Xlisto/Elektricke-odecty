package cz.xlisto.elektrodroid.modules.pricelist;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPFilter;
import cz.xlisto.elektrodroid.utils.ReadRawJSON;


/**
 * Unit testy pro {@link PriceListOriginHelper} ověřující klasifikaci původu ceníků
 * a pomocné filtrační funkce.
 */
public class PriceListOriginHelperTest {

    private PriceListModel createReferenceCopy(String author) {
        Calendar startCal = ViewHelper.parseCalendarFromString("01.01.2025");
        Calendar endCal = ViewHelper.parseCalendarFromString("31.08.2025");
        PriceListModel ref = new ReadRawJSON(null).read(startCal, endCal, "ČEZ", "D 02d");
        assertNotNull("Referenční ceník pro rok 2025 musí být dostupný", ref);
        return new PriceListModel(
                1L, "", "Test", "Dodavatel",
                3100.0, 0.0, 120.0,
                ref.getTax(), "D 02d",
                ref.getDistVT(), ref.getDistNT(),
                ref.getJ0(), ref.getJ1(), ref.getJ2(), ref.getJ3(), ref.getJ4(),
                ref.getJ5(), ref.getJ6(), ref.getJ7(), ref.getJ8(), ref.getJ9(),
                ref.getJ10(), ref.getJ11(), ref.getJ12(), ref.getJ13(), ref.getJ14(),
                ref.getSystemServices(), ref.getActivity(), ref.getPoze1(), ref.getPoze2(),
                ref.getOze(), ref.getOte(),
                startCal.getTimeInMillis(), endCal.getTimeInMillis(),
                ref.getVat(), "ČEZ", author, System.currentTimeMillis(), ""
        );
    }


    @Test
    public void classifyPdfOriginalReturnsPdfOriginal() {
        PriceListModel model = createReferenceCopy(PriceListOriginHelper.AUTHOR_PDF_IMPORT);
        assertEquals(PriceListOriginHelper.OriginType.PDF_ORIGINAL, PriceListOriginHelper.classify(null, model));
    }


    @Test
    public void classifyManualWithMatchingRegulatedReturnsAppRegulated() {
        PriceListModel model = createReferenceCopy("");
        assertEquals(PriceListOriginHelper.OriginType.APP_REGULATED, PriceListOriginHelper.classify(null, model));
    }


    @Test
    public void classifyManualWithDifferingRegulatedReturnsCustomRegulated() {
        PriceListModel model = createReferenceCopy("");
        model.setDistVT(model.getDistVT() + 10.0);
        assertEquals(PriceListOriginHelper.OriginType.CUSTOM_REGULATED, PriceListOriginHelper.classify(null, model));
    }


    @Test
    public void classifyUnsupportedYearReturnsManualComplete() {
        PriceListModel model = createReferenceCopy("");
        Calendar startCal = ViewHelper.parseCalendarFromString("01.01.2019");
        Calendar endCal = ViewHelper.parseCalendarFromString("31.12.2019");
        model.setValidFrom(startCal.getTimeInMillis());
        model.setValidTo(endCal.getTimeInMillis());
        assertEquals(PriceListOriginHelper.OriginType.MANUAL_COMPLETE, PriceListOriginHelper.classify(null, model));
    }


    @Test
    public void classifyMultiYearReturnsManualComplete() {
        PriceListModel model = createReferenceCopy("");
        Calendar startCal = ViewHelper.parseCalendarFromString("01.01.2024");
        Calendar endCal = ViewHelper.parseCalendarFromString("31.12.2025");
        model.setValidFrom(startCal.getTimeInMillis());
        model.setValidTo(endCal.getTimeInMillis());
        assertEquals(PriceListOriginHelper.OriginType.MANUAL_COMPLETE, PriceListOriginHelper.classify(null, model));
    }


    @Test
    public void helperChecksForPdfAuthorAndFilterActivity() {
        assertTrue(PriceListOriginHelper.isPdfAuthor("PDF Import"));
        assertTrue(PriceListOriginHelper.isPdfAuthor("pdf"));
        assertFalse(PriceListOriginHelper.isPdfAuthor(""));
        assertFalse(PriceListOriginHelper.isPdfAuthor(null));

        assertFalse(PriceListOriginHelper.isOriginFilterActive(null));
        assertFalse(PriceListOriginHelper.isOriginFilterActive(ShPFilter.DEFAULT));
        assertFalse(PriceListOriginHelper.isOriginFilterActive(DataPriceListSource.VSE));
        assertTrue(PriceListOriginHelper.isOriginFilterActive("Načteno z PDF"));

        assertTrue(PriceListOriginHelper.nearlyEqual(100.001, 100.004));
        assertFalse(PriceListOriginHelper.nearlyEqual(100.0, 100.05));
    }

}
