package cz.xlisto.elektrodroid.modules.pricelist;


import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfParserUtils;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPFilter;
import cz.xlisto.elektrodroid.utils.ReadRawJSON;


/**
 * Pomocná třída pro určení původu ceníku a stavu jeho regulovaných cen,
 * grafické vykreslení odznaku (badge) v seznamu i detailu ceníku a filtrování podle typu ceníku.
 */
public final class PriceListOriginHelper {

    /**
     * Hodnota pole {@code autor} zapisovaná při přímém importu ceníku z PDF dodavatele.
     */
    public static final String AUTHOR_PDF_IMPORT = "PDF Import";

    private static final double EPSILON = 0.005;
    private static final Map<String, PriceListModel> REFERENCE_CACHE = new ConcurrentHashMap<>();


    private PriceListOriginHelper() {
        // Statická pomocná třída
    }


    /**
     * Kategorie původu ceníku a stavu jeho regulovaných cen.
     */
    public enum OriginType {
        /**
         * Kompletně načtený ceník z PDF dodavatele bez jakéhokoliv zásahu uživatele.
         */
        PDF_ORIGINAL(
                R.string.pricelist_origin_pdf_short,
                R.string.pricelist_origin_pdf_detail,
                R.drawable.ic_add_pdf_24,
                R.drawable.bg_pricelist_badge_pdf,
                R.color.pricelist_badge_pdf_text
        ),

        /**
         * Ručně vytvořený nebo upravený ceník, jehož regulované ceny se shodují s uloženými regulovanými cenami v aplikaci.
         */
        APP_REGULATED(
                R.string.pricelist_origin_app_regul_short,
                R.string.pricelist_origin_app_regul_detail,
                R.drawable.ic_check_circle_24,
                R.drawable.bg_pricelist_badge_app_regul,
                R.color.pricelist_badge_app_regul_text
        ),

        /**
         * Ceník v období podporovaném aplikací, u kterého se některé regulované ceny liší od uložených cen aplikace.
         */
        CUSTOM_REGULATED(
                R.string.pricelist_origin_custom_regul_short,
                R.string.pricelist_origin_custom_regul_detail,
                R.drawable.ic_warning_24,
                R.drawable.bg_pricelist_badge_custom_regul,
                R.color.pricelist_badge_custom_regul_text
        ),

        /**
         * Kompletně ručně vytvořený ceník (pro období/rok, kde aplikace nemá uložené automatické regulované ceny).
         */
        MANUAL_COMPLETE(
                R.string.pricelist_origin_manual_short,
                R.string.pricelist_origin_manual_detail,
                R.drawable.ic_edit_24,
                R.drawable.bg_pricelist_badge_manual,
                R.color.pricelist_badge_manual_text
        );

        @StringRes
        private final int shortLabelRes;
        @StringRes
        private final int detailLabelRes;
        @DrawableRes
        private final int iconRes;
        @DrawableRes
        private final int backgroundRes;
        @ColorRes
        private final int textColorRes;


        OriginType(@StringRes int shortLabelRes,
                   @StringRes int detailLabelRes,
                   @DrawableRes int iconRes,
                   @DrawableRes int backgroundRes,
                   @ColorRes int textColorRes) {
            this.shortLabelRes = shortLabelRes;
            this.detailLabelRes = detailLabelRes;
            this.iconRes = iconRes;
            this.backgroundRes = backgroundRes;
            this.textColorRes = textColorRes;
        }


        @StringRes
        public int getShortLabelRes() {
            return shortLabelRes;
        }


        @StringRes
        public int getDetailLabelRes() {
            return detailLabelRes;
        }


        @DrawableRes
        public int getIconRes() {
            return iconRes;
        }


        @DrawableRes
        public int getBackgroundRes() {
            return backgroundRes;
        }


        @ColorRes
        public int getTextColorRes() {
            return textColorRes;
        }
    }


    /**
     * Klasifikuje ceník do jedné ze čtyř kategorií podle jeho původu a shody regulovaných cen s aplikací.
     *
     * @param context   kontext aplikace
     * @param priceList model ceníku k vyhodnocení
     * @return zjištěný typ {@link OriginType}
     */
    @NonNull
    public static OriginType classify(@Nullable Context context, @Nullable PriceListModel priceList) {
        if (priceList == null || priceList.isEmpty()) {
            return OriginType.MANUAL_COMPLETE;
        }

        if (isPdfAuthor(priceList.getAuthor())) {
            return OriginType.PDF_ORIGINAL;
        }

        PriceListModel ref = getReferenceRegulatedPriceList(context, priceList);
        if (ref == null || (ref.getDistVT() == 0.0 && ref.getJ0() == 0.0)) {
            return OriginType.MANUAL_COMPLETE;
        }

        if (ref.getVat() > 0.0 && areRegulatedPricesMatching(priceList, ref)) {
            return OriginType.APP_REGULATED;
        }

        return OriginType.CUSTOM_REGULATED;
    }


    /**
     * Ověří, zda text pole {@code autor} označuje ceník přímo importovaný z PDF bez ručních úprav.
     *
     * @param author hodnota pole {@code autor} z modelu ceníku
     * @return {@code true} pokud ceník pochází přímo z PDF importu
     */
    public static boolean isPdfAuthor(@Nullable String author) {
        if (author == null) return false;
        String trimmed = author.trim();
        return AUTHOR_PDF_IMPORT.equalsIgnoreCase(trimmed) || "PDF".equalsIgnoreCase(trimmed);
    }


    /**
     * Načte (s využitím in-memory cache) referenční model s oficiálními regulovanými cenami
     * pro období, distribuční území a sazbu daného ceníku.
     *
     * @param context   kontext aplikace
     * @param priceList ceník, pro který se hledají referenční regulované ceny
     * @return referenční {@link PriceListModel} z {@link ReadRawJSON}, nebo {@code null} u víceletého rozsahu
     */
    @Nullable
    public static PriceListModel getReferenceRegulatedPriceList(@Nullable Context context, @Nullable PriceListModel priceList) {
        if (priceList == null || priceList.getValidFrom() <= 0 || priceList.getValidTo() <= 0) {
            return null;
        }

        String startDay = ViewHelper.convertLongToDate(priceList.getValidFrom());
        String endDay = ViewHelper.convertLongToDate(priceList.getValidTo());
        Calendar startCal = ViewHelper.parseCalendarFromString(startDay);
        Calendar endCal = ViewHelper.parseCalendarFromString(endDay);

        if (startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR)) {
            return null;
        }

        String shortDist = PdfParserUtils.getShortDistribuce(priceList.getDistribution());
        String formattedRate = PdfParserUtils.formatSazba(priceList.getRate());
        String cacheKey = startDay + "|" + endDay + "|" + shortDist + "|" + formattedRate;

        PriceListModel cached = REFERENCE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Context appContext = context != null ? context.getApplicationContext() : null;
        PriceListModel loaded = new ReadRawJSON(appContext).read(startCal, endCal, shortDist, formattedRate);
        if (loaded != null) {
            REFERENCE_CACHE.put(cacheKey, loaded);
        }
        return loaded;
    }


    /**
     * Porovná všechny regulované položky ceníku s referenčním ceníkem z interních zdrojů aplikace.
     *
     * @param priceList kontrolovaný ceník
     * @param ref       referenční regulovaný ceník z {@link ReadRawJSON}
     * @return {@code true}, pokud se všechny regulované ceny shodují v toleranci {@link #EPSILON}
     */
    public static boolean areRegulatedPricesMatching(@NonNull PriceListModel priceList, @NonNull PriceListModel ref) {
        return nearlyEqual(priceList.getDistVT(), ref.getDistVT())
                && nearlyEqual(priceList.getDistNT(), ref.getDistNT())
                && nearlyEqual(priceList.getJ0(), ref.getJ0())
                && nearlyEqual(priceList.getJ1(), ref.getJ1())
                && nearlyEqual(priceList.getJ2(), ref.getJ2())
                && nearlyEqual(priceList.getJ3(), ref.getJ3())
                && nearlyEqual(priceList.getJ4(), ref.getJ4())
                && nearlyEqual(priceList.getJ5(), ref.getJ5())
                && nearlyEqual(priceList.getJ6(), ref.getJ6())
                && nearlyEqual(priceList.getJ7(), ref.getJ7())
                && nearlyEqual(priceList.getJ8(), ref.getJ8())
                && nearlyEqual(priceList.getJ9(), ref.getJ9())
                && nearlyEqual(priceList.getJ10(), ref.getJ10())
                && nearlyEqual(priceList.getJ11(), ref.getJ11())
                && nearlyEqual(priceList.getJ12(), ref.getJ12())
                && nearlyEqual(priceList.getJ13(), ref.getJ13())
                && nearlyEqual(priceList.getJ14(), ref.getJ14())
                && nearlyEqual(priceList.getSystemServices(), ref.getSystemServices())
                && nearlyEqual(priceList.getActivity(), ref.getActivity())
                && nearlyEqual(priceList.getPoze1(), ref.getPoze1())
                && nearlyEqual(priceList.getPoze2(), ref.getPoze2())
                && nearlyEqual(priceList.getTax(), ref.getTax())
                && nearlyEqual(priceList.getVat(), ref.getVat());
    }


    /**
     * Vrátí seznam názvů regulovaných položek, ve kterých se ceník liší od uložených regulovaných cen v aplikaci.
     *
     * @param context   kontext aplikace
     * @param priceList kontrolovaný ceník
     * @return seznam názvů odlišných regulovaných položek (prázdný seznam, pokud se neliší)
     */
    @NonNull
    public static List<String> getDifferingRegulatedFields(@NonNull Context context, @Nullable PriceListModel priceList) {
        List<String> diffs = new ArrayList<>();
        if (priceList == null || priceList.isEmpty()) {
            return diffs;
        }

        PriceListModel ref = getReferenceRegulatedPriceList(context, priceList);
        if (ref == null || (ref.getDistVT() == 0.0 && ref.getJ0() == 0.0)) {
            return diffs;
        }

        if (ref.getVat() == 0.0) {
            diffs.add(context.getString(R.string.pricelist_origin_custom_regul_period_mismatch));
            return diffs;
        }

        if (!nearlyEqual(priceList.getDistVT(), ref.getDistVT())) {
            diffs.add(context.getString(R.string.vt) + " (" + context.getString(R.string.cena_za_distribuci) + ")");
        }
        if (!nearlyEqual(priceList.getDistNT(), ref.getDistNT())) {
            diffs.add(context.getString(R.string.nt) + " (" + context.getString(R.string.cena_za_distribuci) + ")");
        }

        int[] breakerLabels = new int[]{
                R.string.jistic_do_10, R.string.jistic_nad_10, R.string.jistic_nad_16,
                R.string.jistic_nad_20, R.string.jistic_nad_25, R.string.jistic_nad_32,
                R.string.jistic_nad_40, R.string.jistic_nad_50, R.string.jistic_nad_63_za_A,
                R.string.jistic_nad_25_za_A, R.string.jistic_nad_63, R.string.jistic_nad_80,
                R.string.jistic_nad_100, R.string.jistic_nad_125, R.string.jistic_nad_160
        };
        double[] actualBreakers = new double[]{
                priceList.getJ0(), priceList.getJ1(), priceList.getJ2(), priceList.getJ3(), priceList.getJ4(),
                priceList.getJ5(), priceList.getJ6(), priceList.getJ7(), priceList.getJ8(), priceList.getJ9(),
                priceList.getJ10(), priceList.getJ11(), priceList.getJ12(), priceList.getJ13(), priceList.getJ14()
        };
        double[] refBreakers = new double[]{
                ref.getJ0(), ref.getJ1(), ref.getJ2(), ref.getJ3(), ref.getJ4(),
                ref.getJ5(), ref.getJ6(), ref.getJ7(), ref.getJ8(), ref.getJ9(),
                ref.getJ10(), ref.getJ11(), ref.getJ12(), ref.getJ13(), ref.getJ14()
        };

        for (int i = 0; i < breakerLabels.length; i++) {
            if (!nearlyEqual(actualBreakers[i], refBreakers[i])) {
                diffs.add(context.getString(breakerLabels[i]));
            }
        }

        if (!nearlyEqual(priceList.getSystemServices(), ref.getSystemServices())) {
            diffs.add(context.getString(R.string.systemove_sluzby));
        }
        if (!nearlyEqual(priceList.getActivity(), ref.getActivity())) {
            boolean isNewLabel = priceList.getValidFrom() >= 1719784800000L;
            diffs.add(context.getString(isNewLabel ? R.string.provoz_nesitove_infrastruktury : R.string.cinnost_operatora_trhu));
        }
        if (!nearlyEqual(priceList.getPoze1(), ref.getPoze1())) {
            diffs.add(context.getString(R.string.poze_dle_jistice));
        }
        if (!nearlyEqual(priceList.getPoze2(), ref.getPoze2())) {
            diffs.add(context.getString(R.string.poze_dle_spotreby));
        }
        if (!nearlyEqual(priceList.getTax(), ref.getTax())) {
            diffs.add(context.getString(R.string.dan_elektriny));
        }
        if (!nearlyEqual(priceList.getVat(), ref.getVat())) {
            diffs.add(context.getString(R.string.dph));
        }

        return diffs;
    }


    /**
     * Nastaví vzhled (pozadí, ikonu, barvu a text) odznaku původu ceníku.
     *
     * @param context       kontext pro načtení barev a textů
     * @param originType    zjištěný typ ceníku
     * @param container     kontejner odznaku, kterému se nastaví pozadí
     * @param iconView      {@link ImageView} pro ikonu stavu
     * @param textView      {@link TextView} pro textový popis stavu
     * @param useDetailText {@code true} pro delší popis v detailu ceníku, {@code false} pro kompaktní štítek v seznamu
     */
    public static void bindBadge(@NonNull Context context,
                                 @NonNull OriginType originType,
                                 @NonNull View container,
                                 @NonNull ImageView iconView,
                                 @NonNull TextView textView,
                                 boolean useDetailText) {
        container.setBackgroundResource(originType.getBackgroundRes());
        int textColor = ContextCompat.getColor(context, originType.getTextColorRes());

        iconView.setImageResource(originType.getIconRes());
        ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(textColor));

        textView.setText(useDetailText ? originType.getDetailLabelRes() : originType.getShortLabelRes());
        textView.setTextColor(textColor);
    }


    /**
     * Vrátí seznam možností pro filtr původu/typu ceníku v {@code PriceListFilterDialogFragment}.
     *
     * @param context kontext pro načtení lokalizovaných názvů
     * @return seznam možností začínající položkou {@link DataPriceListSource#VSE}
     */
    @NonNull
    public static ArrayList<String> getFilterOptions(@NonNull Context context) {
        ArrayList<String> options = new ArrayList<>();
        options.add(DataPriceListSource.VSE);
        for (OriginType type : OriginType.values()) {
            options.add(context.getString(type.getShortLabelRes()));
        }
        return options;
    }


    /**
     * Zjistí, zda je filtr původu ceníku aktivní.
     *
     * @param filterPref uložená hodnota filtru z {@link ShPFilter}
     * @return {@code true}, pokud je vybrán alespoň jeden konkrétní typ ceníku
     */
    public static boolean isOriginFilterActive(@Nullable String filterPref) {
        return filterPref != null
                && !filterPref.trim().isEmpty()
                && !filterPref.equals(ShPFilter.DEFAULT)
                && !filterPref.equalsIgnoreCase(DataPriceListSource.VSE);
    }


    /**
     * Ověří, zda daný ceník vyhovuje nastavenému filtru původu/typu ceníku.
     *
     * @param context    kontext aplikace
     * @param priceList  model ceníku k ověření
     * @param filterPref uložená hodnota filtru (jedna hodnota nebo více hodnot oddělených středníkem)
     * @return {@code true}, pokud ceník filtru vyhovuje
     */
    public static boolean matchesOriginFilter(@NonNull Context context,
                                              @Nullable PriceListModel priceList,
                                              @Nullable String filterPref) {
        if (!isOriginFilterActive(filterPref)) {
            return true;
        }
        if (priceList == null) {
            return false;
        }

        OriginType actualType = classify(context, priceList);
        String actualLabel = context.getString(actualType.getShortLabelRes());

        String[] parts = filterPref.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.equalsIgnoreCase(actualLabel) || trimmed.equalsIgnoreCase(actualType.name())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Porovná dvě desetinná čísla v toleranci {@link #EPSILON} (0,005).
     */
    public static boolean nearlyEqual(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

}
