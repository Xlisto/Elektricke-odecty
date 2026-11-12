package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;


/**
 * Rozhraní pro parsery ceníků elektřiny z PDF dokumentů.
 */
public interface PdfPriceListParser {

    /**
     * Zjistí, zda je tento parser vhodný pro zpracování daného textu z PDF.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return true pokud parser dokáže zpracovat dané PDF
     */
    boolean canParse(String pdfText);

    /**
     * Rozparsuje text z PDF a vrátí seznam ceníkových modelů pro jednotlivé distribuční sazby.
     *
     * @param context Kontext aplikace
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Seznam načtených ceníkových položek (PriceListModel)
     */
    List<PriceListModel> parse(Context context, String pdfText);

}
