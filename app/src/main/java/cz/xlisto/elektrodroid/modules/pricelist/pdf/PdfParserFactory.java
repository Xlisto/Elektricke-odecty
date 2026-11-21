package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.utils.ReadRawJSON;


/**
 * Továrna pro výběr a spuštění odpovídajícího PDF parseru ceníků elektřiny.
 * <p>
 * Registruje dostupné parsery pro jednotlivé dodavatele (ČEZ, PRE, E.ON, Centropol, MND, innogy, TEDOM)
 * a zajišťuje záložní zpracování obecných ceníků včetně doplňování regulovaných cen z databáze aplikace.
 */
public class PdfParserFactory {

    private static final List<PdfPriceListParser> parsers = new ArrayList<>();

    static {
        parsers.add(new CezPdfParser());
        parsers.add(new PrePdfParser());
        parsers.add(new EonPdfParser());
        parsers.add(new CentropolPdfParser());
        parsers.add(new MndPdfParser());
        parsers.add(new InnogyPdfParser());
        parsers.add(new TedomPdfParser());
    }

    /**
     * Zjistí, zda předaný text z PDF dokumentu patří některému z otestovaných a podporovaných dodavatelů
     * (ČEZ, PRE, E.ON, Centropol, MND, innogy, TEDOM).
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud text odpovídá některému z otestovaných dodavatelů, jinak false
     */
    public static boolean isKnownCompany(String pdfText) {
        if (pdfText == null || pdfText.isEmpty()) {
            return false;
        }
        for (PdfPriceListParser parser : parsers) {
            if (parser.canParse(pdfText)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vybere vhodný parser podle obsahu textu z PDF, rozparsuje ceníkové položky
     * a pokusí se získat a aktualizovat regulované ceny z interních JSON zdrojů aplikace.
     *
     * @param context Kontext aplikace pro načtení regulovaných cen z JSON zdrojů
     * @param pdfText Extrahovaný text z PDF dokumentu
     * @return Seznam získaných objektů {@link PriceListModel} pro jednotlivé distribuční sazby, nebo prázdný seznam
     */
    public static List<PriceListModel> parsePdf(Context context, String pdfText) {
        if (pdfText == null || pdfText.isEmpty()) {
            return new ArrayList<>();
        }

        List<PriceListModel> result = null;
        for (PdfPriceListParser parser : parsers) {
            if (parser.canParse(pdfText)) {
                result = parser.parse(context, pdfText);
                break;
            }
        }

        if (result == null) {
            // Pokud se nejedná o známou otestovanou společnost, ceník nelze přečíst
            return new ArrayList<>();
        }

        for (PriceListModel model : result) {
            applyRegulatedPricesFromRaw(context, model);
        }

        return result;
    }


    /**
     * Pokusí se načíst oficiální regulované ceny z interního JSON zdroje {@link ReadRawJSON}
     * pro daný rok, zkrácené distribuční území a sazbu, a přepíše jimi regulované položky v modelu.
     * <p>
     * Pokud v aplikaci podklady pro daný rok neexistují nebo selžou, ponechá v modelu regulované ceny načtené přímo z PDF.
     *
     * @param context Kontext aplikace
     * @param model   Model ceníku k aktualizaci
     */
    private static void applyRegulatedPricesFromRaw(Context context, PriceListModel model) {
        try {
            ReadRawJSON reader = new ReadRawJSON(context);
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTimeInMillis(model.getValidFrom() > 0 ? model.getValidFrom() : System.currentTimeMillis());
            endCal.setTimeInMillis(model.getValidTo() > 0 ? model.getValidTo() : System.currentTimeMillis());

            String shortDist = PdfParserUtils.getShortDistribuce(model.getDistribution());
            PriceListModel regModel = reader.read(startCal, endCal, shortDist, model.getRate());

            if (regModel != null && (regModel.getDistVT() > 0 || regModel.getJ0() > 0)) {
                if (model.getDistVT() == 0.0) model.setDistVT(regModel.getDistVT());
                if (model.getDistNT() == 0.0) model.setDistNT(regModel.getDistNT());
                if (model.getJ0() == 0.0) model.setJ0(regModel.getJ0());
                if (model.getJ1() == 0.0) model.setJ1(regModel.getJ1());
                if (model.getJ2() == 0.0) model.setJ2(regModel.getJ2());
                if (model.getJ3() == 0.0) model.setJ3(regModel.getJ3());
                if (model.getJ4() == 0.0) model.setJ4(regModel.getJ4());
                if (model.getJ5() == 0.0) model.setJ5(regModel.getJ5());
                if (model.getJ6() == 0.0) model.setJ6(regModel.getJ6());
                if (model.getJ7() == 0.0) model.setJ7(regModel.getJ7());
                if (model.getJ8() == 0.0) model.setJ8(regModel.getJ8());
                if (model.getJ9() == 0.0) model.setJ9(regModel.getJ9());
                if (model.getJ10() == 0.0) model.setJ10(regModel.getJ10());
                if (model.getJ11() == 0.0) model.setJ11(regModel.getJ11());
                if (model.getJ12() == 0.0) model.setJ12(regModel.getJ12());
                if (model.getJ13() == 0.0) model.setJ13(regModel.getJ13());
                if (model.getJ14() == 0.0) model.setJ14(regModel.getJ14());
                if (model.getSystemServices() == 0.0)
                    model.setSystemServices(regModel.getSystemServices());
                if (model.getActivity() == 0.0) model.setActivity(regModel.getActivity());
                if (model.getPoze1() == 0.0) model.setPoze1(regModel.getPoze1());
                if (!model.getCompany().contains("CENTROPOL") && !model.getCompany().contains("TEDOM") && !model.getCompany().contains("MND") && model.getPoze2() == 0.0) {
                    model.setPoze2(regModel.getPoze2());
                }
                if (model.getTax() == 0.0) model.setTax(regModel.getTax());
                if (regModel.getVat() > 0 && model.getVat() == 0.0) {
                    model.setVat(regModel.getVat());
                }
            }
        } catch (Exception e) {
            // Ponechá ceny načtené přímo z PDF
        }
    }

}
