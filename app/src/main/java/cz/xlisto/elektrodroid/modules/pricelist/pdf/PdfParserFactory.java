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
 * Registruje dostupné parsery pro jednotlivé dodavatele (ČEZ, PRE, E.ON, Centropol, MND, innogy, ČRE)
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
        parsers.add(new CrePdfParser());
    }

    /**
     * Vybere vhodný parser podle obsahu textu z PDF, rozparsuje ceníkové položky
     * a pokusí se získat a aktualizovat regulované ceny z interních JSON zdrojů aplikace.
     *
     * @param context Kontext aplikace pro načtení regulovaných cen z JSON zdrojů
     * @param pdfText Extrahovaný text z PDF dokumentu
     * @return Seznam získaných objektů {@link PriceListModel} pro jednotlivé distribuční sazby
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
            result = new GenericPdfParser().parse(context, pdfText);
        }

        if (context != null) {
            for (PriceListModel model : result) {
                applyRegulatedPricesFromRaw(context, model);
            }
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
            startCal.setTimeInMillis(model.getPlatnostOD() > 0 ? model.getPlatnostOD() : System.currentTimeMillis());
            endCal.setTimeInMillis(model.getPlatnostDO() > 0 ? model.getPlatnostDO() : System.currentTimeMillis());

            String shortDist = PdfParserUtils.getShortDistribuce(model.getDistribuce());
            PriceListModel regModel = reader.read(startCal, endCal, shortDist, model.getSazba());

            if (regModel != null && (regModel.getDistVT() > 0 || regModel.getJ0() > 0)) {
                model.setDistVT(regModel.getDistVT());
                model.setDistNT(regModel.getDistNT());
                model.setJ0(regModel.getJ0());
                model.setJ1(regModel.getJ1());
                model.setJ2(regModel.getJ2());
                model.setJ3(regModel.getJ3());
                model.setJ4(regModel.getJ4());
                model.setJ5(regModel.getJ5());
                model.setJ6(regModel.getJ6());
                model.setJ7(regModel.getJ7());
                model.setJ8(regModel.getJ8());
                model.setJ9(regModel.getJ9());
                model.setJ10(regModel.getJ10());
                model.setJ11(regModel.getJ11());
                model.setJ12(regModel.getJ12());
                model.setJ13(regModel.getJ13());
                model.setJ14(regModel.getJ14());
                model.setSystemSluzby(regModel.getSystemSluzby());
                model.setCinnost(regModel.getCinnost());
                model.setPoze1(regModel.getPoze1());
                model.setPoze2(regModel.getPoze2());
                model.setDan(regModel.getDan());
                if (regModel.getDph() > 0) {
                    model.setDph(regModel.getDph());
                }
            }
        } catch (Exception e) {
            // Ponechá ceny načtené přímo z PDF
        }
    }

}
