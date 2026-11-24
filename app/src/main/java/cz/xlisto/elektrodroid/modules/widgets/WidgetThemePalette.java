package cz.xlisto.elektrodroid.modules.widgets;


import android.content.Context;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.DynamicColors;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * Vyhodnocuje aktivní barevnou paletu domovských widgetů podle systémové dostupnosti Material You
 * a uživatelského přepínače {@link ShPSettings#USE_DYNAMIC_COLORS}.
 * <p>
 * Na Androidu 12+ při zapnutých dynamických barvách používá systémové tokeny {@code widget_dyn_*}
 * (odvozené z tapety uživatele a přizpůsobující se i živému přepnutí světlého/tmavého režimu),
 * při vypnutém přepínači nebo na Androidu &lt; 12 vrací výchozí statickou paletu motivu aplikace.
 * Uživatelské barvy tarifů VT/NT a sémantické barvy (přeplatek/nedoplatek, HDO pásma, varování)
 * zůstávají zachovány beze změny.
 */
public final class WidgetThemePalette {

    private final boolean dynamicColorsActive;

    @ColorRes
    private final int primaryRes;
    @ColorRes
    private final int onPrimaryContainerRes;
    @ColorRes
    private final int onSurfaceRes;
    @ColorRes
    private final int onSurfaceVariantRes;

    @ColorInt
    private final int primary;
    @ColorInt
    private final int onPrimaryContainer;
    @ColorInt
    private final int surfaceContainerHigh;
    @ColorInt
    private final int onSurface;
    @ColorInt
    private final int onSurfaceVariant;
    @ColorInt
    private final int outlineVariant;
    @ColorInt
    private final int priceBoxInactiveBg;
    @ColorInt
    private final int priceBoxInactiveStroke;


    private WidgetThemePalette(boolean dynamicColorsActive,
                               @ColorRes int primaryRes,
                               @ColorRes int onPrimaryContainerRes,
                               @ColorRes int onSurfaceRes,
                               @ColorRes int onSurfaceVariantRes,
                               @ColorInt int primary,
                               @ColorInt int onPrimaryContainer,
                               @ColorInt int surfaceContainerHigh,
                               @ColorInt int onSurface,
                               @ColorInt int onSurfaceVariant,
                               @ColorInt int outlineVariant,
                               @ColorInt int priceBoxInactiveBg,
                               @ColorInt int priceBoxInactiveStroke) {
        this.dynamicColorsActive = dynamicColorsActive;
        this.primaryRes = primaryRes;
        this.onPrimaryContainerRes = onPrimaryContainerRes;
        this.onSurfaceRes = onSurfaceRes;
        this.onSurfaceVariantRes = onSurfaceVariantRes;
        this.primary = primary;
        this.onPrimaryContainer = onPrimaryContainer;
        this.surfaceContainerHigh = surfaceContainerHigh;
        this.onSurface = onSurface;
        this.onSurfaceVariant = onSurfaceVariant;
        this.outlineVariant = outlineVariant;
        this.priceBoxInactiveBg = priceBoxInactiveBg;
        this.priceBoxInactiveStroke = priceBoxInactiveStroke;
    }


    /**
     * Ověří, zda mají widgety aktuálně používat dynamické barvy Material You.
     *
     * @param context kontext aplikace
     * @return {@code true}, pokud je zařízení na Androidu 12+ s podporou {@link DynamicColors}
     * a uživatel má zapnutý přepínač dynamických barev v nastavení
     */
    public static boolean isDynamicColorsEnabled(@NonNull Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && DynamicColors.isDynamicColorAvailable()
                && new ShPSettings(context).isUseDynamicColors();
    }


    /**
     * Sestaví paletu pro aktuální konfiguraci a nastavení dynamických barev.
     *
     * @param context kontext aplikace
     * @return vyhodnocená instance {@link WidgetThemePalette}
     */
    @NonNull
    public static WidgetThemePalette resolve(@NonNull Context context) {
        if (isDynamicColorsEnabled(context)) {
            int primary = ContextCompat.getColor(context, R.color.widget_dyn_primary);
            int onPrimaryContainer = ContextCompat.getColor(context, R.color.widget_dyn_on_primary_container);
            int surfaceLow = ContextCompat.getColor(context, R.color.widget_dyn_surface_container_low);
            int surfaceHigh = ContextCompat.getColor(context, R.color.widget_dyn_surface_container_high);
            int onSurface = ContextCompat.getColor(context, R.color.widget_dyn_on_surface);
            int onSurfaceVariant = ContextCompat.getColor(context, R.color.widget_dyn_on_surface_variant);
            int outlineVariant = ContextCompat.getColor(context, R.color.widget_dyn_outline_variant);
            int inactiveBg = ColorUtils.setAlphaComponent(surfaceLow, 217);
            return new WidgetThemePalette(
                    true,
                    R.color.widget_dyn_primary,
                    R.color.widget_dyn_on_primary_container,
                    R.color.widget_dyn_on_surface,
                    R.color.widget_dyn_on_surface_variant,
                    primary,
                    onPrimaryContainer,
                    surfaceHigh,
                    onSurface,
                    onSurfaceVariant,
                    outlineVariant,
                    inactiveBg,
                    outlineVariant
            );
        }

        return new WidgetThemePalette(
                false,
                R.color.md_theme_primary,
                R.color.md_theme_onPrimaryContainer,
                R.color.md_theme_onSurface,
                R.color.md_theme_onSurfaceVariant,
                ContextCompat.getColor(context, R.color.md_theme_primary),
                ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer),
                ContextCompat.getColor(context, R.color.md_theme_surfaceContainerHigh),
                ContextCompat.getColor(context, R.color.md_theme_onSurface),
                ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant),
                ContextCompat.getColor(context, R.color.md_theme_outlineVariant),
                ContextCompat.getColor(context, R.color.dashboard_price_box_inactive_bg),
                ContextCompat.getColor(context, R.color.dashboard_price_box_inactive_stroke)
        );
    }


    public boolean isDynamicColorsActive() {
        return dynamicColorsActive;
    }


    @ColorRes
    public int primaryRes() {
        return primaryRes;
    }


    @ColorRes
    public int onPrimaryContainerRes() {
        return onPrimaryContainerRes;
    }


    @ColorRes
    public int onSurfaceRes() {
        return onSurfaceRes;
    }


    @ColorRes
    public int onSurfaceVariantRes() {
        return onSurfaceVariantRes;
    }


    @ColorInt
    public int primary() {
        return primary;
    }


    @ColorInt
    public int onPrimaryContainer() {
        return onPrimaryContainer;
    }


    @ColorInt
    public int surfaceContainerHigh() {
        return surfaceContainerHigh;
    }


    @ColorInt
    public int onSurface() {
        return onSurface;
    }


    @ColorInt
    public int onSurfaceVariant() {
        return onSurfaceVariant;
    }


    @ColorInt
    public int outlineVariant() {
        return outlineVariant;
    }


    @ColorInt
    public int priceBoxInactiveBg() {
        return priceBoxInactiveBg;
    }


    @ColorInt
    public int priceBoxInactiveStroke() {
        return priceBoxInactiveStroke;
    }


    @DrawableRes
    public int cardBackgroundRes() {
        return dynamicColorsActive ? R.drawable.bg_widget_card_dynamic : R.drawable.bg_widget_card;
    }


    @DrawableRes
    public int priceCardBackgroundRes() {
        return dynamicColorsActive ? R.drawable.bg_widget_price_card_dynamic : R.drawable.bg_widget_price_card;
    }


    @DrawableRes
    public int actionCardBackgroundRes() {
        return dynamicColorsActive ? R.drawable.bg_widget_action_card_dynamic : R.drawable.bg_widget_action_card;
    }


    @DrawableRes
    public int neutralPillBackgroundRes() {
        return dynamicColorsActive ? R.drawable.bg_widget_pill_neutral_dynamic : R.drawable.bg_widget_pill_neutral;
    }


    @DrawableRes
    public int settingsIconRes() {
        return dynamicColorsActive ? R.drawable.ic_widget_settings_24_dynamic : R.drawable.ic_widget_settings_24;
    }


    @DrawableRes
    public int addReadingIconRes() {
        return dynamicColorsActive ? R.drawable.ic_widget_add_reading_24_dynamic : R.drawable.ic_widget_add_reading_24;
    }


    /**
     * Aplikuje barvu textu na prvek v {@link RemoteViews} tak, aby na Androidu 12+ zůstala
     * živě navázaná na systémový resource (automatická reakce na změnu tapety i tmavého režimu).
     *
     * @param views       cílové {@link RemoteViews}
     * @param viewId      ID prvku {@code TextView}
     * @param colorRes    ID barevného zdroje
     * @param fallbackInt vyhodnocená ARGB barva pro starší verze Androidu
     */
    public void applyTextColor(@NonNull RemoteViews views,
                               @IdRes int viewId,
                               @ColorRes int colorRes,
                               @ColorInt int fallbackInt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColor(viewId, "setTextColor", colorRes);
        } else {
            views.setTextColor(viewId, fallbackInt);
        }
    }


    /**
     * Aplikuje tónování (tint) na {@code ImageView} v {@link RemoteViews} s podporou
     * živých barevných zdrojů na Androidu 12+.
     *
     * @param views       cílové {@link RemoteViews}
     * @param viewId      ID prvku {@code ImageView}
     * @param colorRes    ID barevného zdroje
     * @param fallbackInt vyhodnocená ARGB barva pro starší verze Androidu
     */
    public void applyImageTint(@NonNull RemoteViews views,
                               @IdRes int viewId,
                               @ColorRes int colorRes,
                               @ColorInt int fallbackInt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setImageTintList", colorRes);
        } else {
            views.setInt(viewId, "setColorFilter", fallbackInt);
        }
    }

}
