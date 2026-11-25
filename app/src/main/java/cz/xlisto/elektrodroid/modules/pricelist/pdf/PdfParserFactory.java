package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.utils.ReadRawJSON;


/**
 * Centrální registr a továrna pro výběr a spuštění odpovídajícího PDF parseru ceníků elektřiny.
 * <p>
 * Registruje dostupné parsery pro jednotlivé dodavatele (ČEZ, PRE, E.ON, Centropol, MND, innogy, TEDOM)
 * společně s metadaty jejich podporovaných formátů ({@link SupportedPriceList}) pro obrazovku nápovědy
 * a zajišťuje doplňování regulovaných cen z databáze aplikace.
 */
public class PdfParserFactory {

    /**
     * Identifikátor dodavatele Centropol.
     */
    public static final String SUPPLIER_ID_CENTROPOL = "centropol";
    /**
     * Identifikátor dodavatele ČEZ.
     */
    public static final String SUPPLIER_ID_CEZ = "cez";
    /**
     * Identifikátor dodavatele E.ON.
     */
    public static final String SUPPLIER_ID_EON = "eon";
    /**
     * Identifikátor dodavatele innogy.
     */
    public static final String SUPPLIER_ID_INNOGY = "innogy";
    /**
     * Identifikátor dodavatele MND.
     */
    public static final String SUPPLIER_ID_MND = "mnd";
    /**
     * Identifikátor dodavatele PRE.
     */
    public static final String SUPPLIER_ID_PRE = "pre";
    /**
     * Identifikátor dodavatele TEDOM.
     */
    public static final String SUPPLIER_ID_TEDOM = "tedom";

    private static final String PREVIEW_ASSETS_DIR = "pricelist_previews/";


    /**
     * Interní záznam registrace propojující parser dodavatele s jeho podporovanými formáty ceníků.
     */
    private record ParserRegistration(
            @NonNull String supplierId,
            @NonNull PdfPriceListParser parser,
            @NonNull List<SupportedPriceList> supportedFormats
    ) {

    }


    private static final List<ParserRegistration> REGISTRATIONS = new ArrayList<>();

    static {
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_CEZ,
                new CezPdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "cez_default",
                        SUPPLIER_ID_CEZ,
                        R.string.supported_supplier_cez_name,
                        PREVIEW_ASSETS_DIR + "cez.webp",
                        2,
                        R.string.supported_supplier_cez_desc,
                        0,
                        CezPdfParser.class.getSimpleName()
                ))
        ));
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_PRE,
                new PrePdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "pre_default",
                        SUPPLIER_ID_PRE,
                        R.string.supported_supplier_pre_name,
                        PREVIEW_ASSETS_DIR + "pre.webp",
                        2,
                        R.string.supported_supplier_pre_desc,
                        0,
                        PrePdfParser.class.getSimpleName()
                ))
        ));
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_EON,
                new EonPdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "eon_default",
                        SUPPLIER_ID_EON,
                        R.string.supported_supplier_eon_name,
                        PREVIEW_ASSETS_DIR + "eon.webp",
                        2,
                        R.string.supported_supplier_eon_desc,
                        R.string.supported_supplier_eon_warning,
                        EonPdfParser.class.getSimpleName()
                ))
        ));
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_CENTROPOL,
                new CentropolPdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "centropol_default",
                        SUPPLIER_ID_CENTROPOL,
                        R.string.supported_supplier_centropol_name,
                        PREVIEW_ASSETS_DIR + "centropol.webp",
                        1,
                        R.string.supported_supplier_centropol_desc,
                        0,
                        CentropolPdfParser.class.getSimpleName()
                ))
        ));
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_MND,
                new MndPdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "mnd_default",
                        SUPPLIER_ID_MND,
                        R.string.supported_supplier_mnd_name,
                        PREVIEW_ASSETS_DIR + "mnd.webp",
                        2,
                        R.string.supported_supplier_mnd_desc,
                        0,
                        MndPdfParser.class.getSimpleName()
                ))
        ));
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_INNOGY,
                new InnogyPdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "innogy_default",
                        SUPPLIER_ID_INNOGY,
                        R.string.supported_supplier_innogy_name,
                        PREVIEW_ASSETS_DIR + "innogy.webp",
                        2,
                        R.string.supported_supplier_innogy_desc,
                        0,
                        InnogyPdfParser.class.getSimpleName()
                ))
        ));
        REGISTRATIONS.add(new ParserRegistration(
                SUPPLIER_ID_TEDOM,
                new TedomPdfParser(),
                Collections.singletonList(new SupportedPriceList(
                        "tedom_default",
                        SUPPLIER_ID_TEDOM,
                        R.string.supported_supplier_tedom_name,
                        PREVIEW_ASSETS_DIR + "tedom.webp",
                        2,
                        R.string.supported_supplier_tedom_desc,
                        0,
                        TedomPdfParser.class.getSimpleName()
                ))
        ));
    }


    /**
     * Zjistí, zda předaný text z PDF dokumentu patří některému z otestovaných a podporovaných dodavatelů
     * (ČEZ, PRE, E.ON, Centropol, MND, innogy, TEDOM).
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return {@code true}, pokud text odpovídá některému z otestovaných dodavatelů, jinak {@code false}
     */
    public static boolean isKnownCompany(@Nullable String pdfText) {
        return detectSupplierId(pdfText) != null;
    }


    /**
     * Pokusí se podle textu extrahovaného z PDF identifikovat podporovaného dodavatele.
     *
     * @param pdfText Extrahovaný text z PDF souboru
     * @return Identifikátor rozpoznaného dodavatele (např. {@link #SUPPLIER_ID_EON}), nebo {@code null}
     */
    @Nullable
    public static String detectSupplierId(@Nullable String pdfText) {
        if (pdfText == null || pdfText.isEmpty()) {
            return null;
        }
        for (ParserRegistration registration : REGISTRATIONS) {
            if (registration.parser().canParse(pdfText)) {
                return registration.supplierId();
            }
        }
        return null;
    }


    /**
     * Vrátí všechny registrované podporované formáty PDF ceníků seřazené abecedně podle českého řazení
     * zobrazovaného názvu dodavatele.
     *
     * @param context Kontext pro načtení lokalizovaných názvů dodavatelů
     * @return Nemodifikovatelný abecedně seřazený seznam objektů {@link SupportedPriceList}
     */
    @NonNull
    public static List<SupportedPriceList> getSupportedPriceLists(@NonNull Context context) {
        List<SupportedPriceList> items = new ArrayList<>(getSupportedPriceLists());
        Collator collator = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));
        collator.setStrength(Collator.SECONDARY);
        items.sort((first, second) -> {
            String firstName = context.getString(first.getSupplierNameResId());
            String secondName = context.getString(second.getSupplierNameResId());
            int nameCompare = collator.compare(firstName, secondName);
            if (nameCompare != 0) {
                return nameCompare;
            }
            return first.getId().compareTo(second.getId());
        });
        return Collections.unmodifiableList(items);
    }


    /**
     * Vrátí všechny registrované podporované formáty PDF ceníků v pořadí registrace.
     *
     * @return Nemodifikovatelný seznam objektů {@link SupportedPriceList}
     */
    @NonNull
    public static List<SupportedPriceList> getSupportedPriceLists() {
        List<SupportedPriceList> items = new ArrayList<>();
        for (ParserRegistration registration : REGISTRATIONS) {
            items.addAll(registration.supportedFormats());
        }
        return Collections.unmodifiableList(items);
    }


    /**
     * Vyhledá první podporovaný formát podle jednoznačného identifikátoru dodavatele.
     *
     * @param supplierId Identifikátor dodavatele
     * @return Nalezený objekt {@link SupportedPriceList} nebo {@code null}
     */
    @Nullable
    public static SupportedPriceList findSupportedPriceListBySupplierId(@Nullable String supplierId) {
        if (supplierId == null || supplierId.isEmpty()) {
            return null;
        }
        for (ParserRegistration registration : REGISTRATIONS) {
            if (registration.supplierId().equals(supplierId) && !registration.supportedFormats().isEmpty()) {
                return registration.supportedFormats().get(0);
            }
        }
        return null;
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
        for (ParserRegistration registration : REGISTRATIONS) {
            if (registration.parser().canParse(pdfText)) {
                result = registration.parser().parse(context, pdfText);
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
