package cz.xlisto.elektrodroid.modules.widgets;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.dashboard.DashboardHdoFormatHelper;
import cz.xlisto.elektrodroid.utils.InvoiceBalanceHelper;


/**
 * Sestavuje {@link RemoteViews} pro všech pět typů domovských widgetů aplikace ElektroDroid.
 * <p>
 * Každá metoda pracuje výhradně s předaným {@code appWidgetId}, jeho {@link WidgetConfig}
 * a ověřeným odběrným místem z {@link WidgetPointResolver.ResolutionResult}, bez jakékoliv
 * vazby na globálně zvolené odběrné místo hlavní aplikace.
 */
public final class WidgetRemoteViewsBuilder {

    private static final int HDO_CLOCK_MINI_MAX_WIDTH_DP = 105;
    private static final int HDO_CLOCK_LARGE_MIN_HEIGHT_DP = 125;
    private static final int HDO_CLOCK_WIDE_THRESHOLD_DP = 200;
    private static final int HDO_CLOCK_STRIP_WIDE_THRESHOLD_DP = 190;
    private static final int NEW_READING_WIDE_THRESHOLD_DP = 105;
    private static final int PERIOD_WIDGET_TALL_THRESHOLD_DP = 112;
    private static final int CONSUMPTION_COMPACT_PERCENT_MIN_WIDTH_DP = 250;
    private static final int CURRENT_PRICE_TALL_THRESHOLD_DP = 96;
    private static final int CURRENT_PRICE_TALL_EXPIRED_THRESHOLD_DP = 116;
    private static final int CURRENT_PRICE_COMPACT_FULL_UNIT_WIDTH_DP = 270;
    private static final int CURRENT_PRICE_COMPACT_HEADER_WIDTH_DP = 260;


    private WidgetRemoteViewsBuilder() {
    }


    /**
     * Sestaví {@link RemoteViews} pro zadanou instanci widgetu.
     *
     * @param context       kontext aplikace
     * @param widgetType    typ widgetu
     * @param appWidgetId   systémové ID instance widgetu
     * @param config        uložená konfigurace instance (může být {@code null})
     * @param resolution    výsledek ověření odběrného místa
     * @param widgetOptions aktuální rozměrové parametry widgetu z {@link AppWidgetManager#getAppWidgetOptions(int)}
     * @return připravené {@link RemoteViews}
     */
    @NonNull
    public static RemoteViews build(@NonNull Context context,
                                    @NonNull WidgetType widgetType,
                                    int appWidgetId,
                                    @Nullable WidgetConfig config,
                                    @NonNull WidgetPointResolver.ResolutionResult resolution,
                                    @Nullable Bundle widgetOptions) {
        WidgetThemePalette palette = WidgetThemePalette.resolve(context);
        return switch (widgetType) {
            case CURRENT_PRICE ->
                    buildCurrentPriceWidget(context, palette, appWidgetId, config, resolution, widgetOptions);
            case HDO_CLOCK ->
                    buildHdoClockWidget(context, palette, appWidgetId, config, resolution, widgetOptions);
            case CONSUMPTION ->
                    buildConsumptionWidget(context, palette, appWidgetId, config, resolution, widgetOptions);
            case FINANCIAL_BALANCE ->
                    buildFinancialBalanceWidget(context, palette, appWidgetId, config, resolution, widgetOptions);
            case NEW_READING ->
                    buildNewReadingWidget(context, palette, appWidgetId, config, resolution, widgetOptions);
        };
    }


    @NonNull
    private static RemoteViews buildCurrentPriceWidget(@NonNull Context context,
                                                       @NonNull WidgetThemePalette palette,
                                                       int appWidgetId,
                                                       @Nullable WidgetConfig config,
                                                       @NonNull WidgetPointResolver.ResolutionResult resolution,
                                                       @Nullable Bundle widgetOptions) {
        RemoteViews views = createBaseRemoteViews(context, R.layout.widget_current_price, appWidgetId, true);
        applyCurrentPriceTheme(views, palette);

        if (resolution.status() != WidgetPointResolver.ResolutionStatus.RESOLVED
                || resolution.point() == null
                || config == null) {
            views.setTextViewText(R.id.tv_widget_price_mode, "");
            bindMissingOrUnconfiguredState(context, views, appWidgetId, WidgetType.CURRENT_PRICE, config, resolution, true);
            return views;
        }

        int widthDp = resolveWidgetWidthDp(widgetOptions, 250);
        int heightDp = resolveWidgetHeightDp(widgetOptions, 72);
        float fontScale = Math.max(1.0f, context.getResources().getConfiguration().fontScale);
        float effectiveHeightDp = heightDp / fontScale;

        SubscriptionPointModel point = resolution.point();
        views.setTextViewText(R.id.tv_widget_point_name, point.getName());

        PendingIntent navIntent = WidgetNavigationHelper.createNavigationPendingIntent(
                context, appWidgetId, WidgetNavigationHelper.DEST_DASHBOARD, point.getId(), point.getMilins());
        views.setOnClickPendingIntent(R.id.widget_root, navIntent);

        WidgetDataService.CurrentPriceSnapshot snapshot = WidgetDataService.loadCurrentPriceSnapshot(context, point, config);
        boolean isExpired = snapshot.isPriceListExpired();
        int tallThresholdDp = isExpired ? CURRENT_PRICE_TALL_EXPIRED_THRESHOLD_DP : CURRENT_PRICE_TALL_THRESHOLD_DP;
        boolean compactMode = effectiveHeightDp < tallThresholdDp;
        views.setTextViewText(
                R.id.tv_widget_price_mode,
                resolvePriceModeLabel(context, config, compactMode || widthDp < CURRENT_PRICE_COMPACT_HEADER_WIDTH_DP)
        );

        if (!snapshot.hasDisplayablePrices()) {
            String errorTitle;
            if (snapshot.priceListStatus() == WidgetDataService.PriceListValidityStatus.NOT_YET_VALID) {
                String validFromText = WidgetFormatHelper.formatDate(snapshot.priceListValidFromMillis());
                errorTitle = validFromText.isEmpty()
                        ? context.getString(R.string.widget_price_not_yet_valid)
                        : context.getString(R.string.widget_price_not_yet_valid_from_format, validFromText);
            } else {
                errorTitle = context.getString(R.string.widget_price_missing);
            }
            views.setViewVisibility(R.id.widget_content_container, View.GONE);
            views.setViewVisibility(R.id.widget_error_container, View.VISIBLE);
            views.setTextViewText(R.id.tv_widget_error_title, errorTitle);
            views.setTextViewText(R.id.tv_widget_error_subtitle, context.getString(R.string.widget_price_invalid_hint));
            return views;
        }

        views.setViewVisibility(R.id.widget_content_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_error_container, View.GONE);

        WidgetDataService.HdoSnapshot hdo = snapshot.hdoSnapshot();
        boolean isSingleTariff = snapshot.isSingleTariff();
        boolean vtActive = isSingleTariff || (hdo.hasHdo() && !hdo.isNtActive());
        boolean ntActive = !isSingleTariff && hdo.hasHdo() && hdo.isNtActive();
        String validToText = WidgetFormatHelper.formatDate(snapshot.priceListValidToMillis());

        if (compactMode) {
            views.setViewVisibility(R.id.layout_price_compact, View.VISIBLE);
            views.setViewVisibility(R.id.layout_price_tall, View.GONE);
            views.setViewVisibility(R.id.box_widget_nt_compact, isSingleTariff ? View.GONE : View.VISIBLE);

            String unitText = context.getString(
                    (isSingleTariff || widthDp >= CURRENT_PRICE_COMPACT_FULL_UNIT_WIDTH_DP)
                            ? R.string.dashboard_price_kwh_unit
                            : R.string.kc
            );

            bindTariffPriceBox(
                    palette,
                    views,
                    R.id.iv_widget_vt_bg_compact,
                    R.id.iv_widget_vt_dot_compact,
                    R.id.tv_widget_vt_label_compact,
                    R.id.iv_widget_vt_active_compact,
                    R.id.tv_widget_vt_price_compact,
                    R.id.tv_widget_vt_unit_compact,
                    WidgetFormatHelper.formatPriceKwh(snapshot.vtPriceKwh()),
                    unitText,
                    snapshot.colorVt(),
                    vtActive,
                    true
            );

            if (!isSingleTariff) {
                bindTariffPriceBox(
                        palette,
                        views,
                        R.id.iv_widget_nt_bg_compact,
                        R.id.iv_widget_nt_dot_compact,
                        R.id.tv_widget_nt_label_compact,
                        R.id.iv_widget_nt_active_compact,
                        R.id.tv_widget_nt_price_compact,
                        R.id.tv_widget_nt_unit_compact,
                        WidgetFormatHelper.formatPriceKwh(snapshot.ntPriceKwh()),
                        unitText,
                        snapshot.colorNt(),
                        ntActive,
                        true
                );
            }

            if (isExpired) {
                views.setViewVisibility(R.id.ll_widget_footer_compact, View.VISIBLE);
                views.setViewVisibility(R.id.iv_widget_price_warning_compact, View.VISIBLE);
                views.setTextColor(
                        R.id.tv_widget_footer_compact,
                        ContextCompat.getColor(context, R.color.pricelist_badge_custom_regul_text)
                );
                views.setTextViewText(
                        R.id.tv_widget_footer_compact,
                        formatExpiredPriceCompactFooter(context, validToText, hdo, isSingleTariff)
                );
            } else if (isSingleTariff && !hdo.hasHdo()) {
                views.setViewVisibility(R.id.ll_widget_footer_compact, View.GONE);
            } else {
                views.setViewVisibility(R.id.ll_widget_footer_compact, View.VISIBLE);
                views.setViewVisibility(R.id.iv_widget_price_warning_compact, View.GONE);
                palette.applyTextColor(
                        views,
                        R.id.tv_widget_footer_compact,
                        palette.onSurfaceVariantRes(),
                        palette.onSurfaceVariant()
                );
                views.setTextViewText(R.id.tv_widget_footer_compact, formatPriceWidgetHdoFooter(context, hdo));
            }
        } else {
            views.setViewVisibility(R.id.layout_price_compact, View.GONE);
            views.setViewVisibility(R.id.layout_price_tall, View.VISIBLE);
            views.setViewVisibility(R.id.box_widget_nt, isSingleTariff ? View.GONE : View.VISIBLE);

            String unitText = context.getString(R.string.dashboard_price_kwh_unit);
            bindTariffPriceBox(
                    palette,
                    views,
                    R.id.iv_widget_vt_bg,
                    R.id.iv_widget_vt_dot,
                    R.id.tv_widget_vt_label,
                    R.id.iv_widget_vt_active,
                    R.id.tv_widget_vt_price,
                    R.id.tv_widget_vt_unit,
                    WidgetFormatHelper.formatPriceKwh(snapshot.vtPriceKwh()),
                    unitText,
                    snapshot.colorVt(),
                    vtActive,
                    false
            );

            if (!isSingleTariff) {
                bindTariffPriceBox(
                        palette,
                        views,
                        R.id.iv_widget_nt_bg,
                        R.id.iv_widget_nt_dot,
                        R.id.tv_widget_nt_label,
                        R.id.iv_widget_nt_active,
                        R.id.tv_widget_nt_price,
                        R.id.tv_widget_nt_unit,
                        WidgetFormatHelper.formatPriceKwh(snapshot.ntPriceKwh()),
                        unitText,
                        snapshot.colorNt(),
                        ntActive,
                        false
                );
            }

            if (isExpired) {
                views.setViewVisibility(R.id.ll_widget_price_warning_tall, View.VISIBLE);
                views.setTextViewText(
                        R.id.tv_widget_price_warning_tall,
                        formatExpiredPriceWarning(context, validToText)
                );
            } else {
                views.setViewVisibility(R.id.ll_widget_price_warning_tall, View.GONE);
            }

            if (isSingleTariff && !hdo.hasHdo()) {
                views.setViewVisibility(R.id.tv_widget_hdo_status, View.GONE);
            } else {
                views.setViewVisibility(R.id.tv_widget_hdo_status, View.VISIBLE);
                views.setTextViewText(R.id.tv_widget_hdo_status, formatPriceWidgetHdoFooter(context, hdo));
            }
        }

        return views;
    }


    private static void bindTariffPriceBox(@NonNull WidgetThemePalette palette,
                                           @NonNull RemoteViews views,
                                           int bgViewId,
                                           int dotViewId,
                                           int labelViewId,
                                           int activeIconViewId,
                                           int priceViewId,
                                           int unitViewId,
                                           @NonNull String formattedPrice,
                                           @NonNull String unitText,
                                           int tariffColor,
                                           boolean isActive,
                                           boolean compactMode) {
        views.setTextViewText(priceViewId, formattedPrice);
        views.setTextViewText(unitViewId, unitText);

        int bgHeight = compactMode ? 72 : 128;
        float cornerRadius = compactMode ? 16f : 22f;

        if (isActive) {
            Bitmap activeBg = WidgetBitmapRenderer.renderRoundedBoxBitmap(tariffColor, tariffColor, 260, bgHeight, cornerRadius);
            views.setImageViewBitmap(bgViewId, activeBg);

            int textColor = WidgetBitmapRenderer.resolveContrastingTextColor(tariffColor);
            views.setTextColor(labelViewId, textColor);
            views.setTextColor(priceViewId, textColor);
            views.setTextColor(unitViewId, textColor);

            views.setViewVisibility(dotViewId, View.GONE);
            views.setViewVisibility(activeIconViewId, View.VISIBLE);
            views.setInt(activeIconViewId, "setColorFilter", textColor);
        } else {
            Bitmap inactiveBg = WidgetBitmapRenderer.renderRoundedBoxBitmap(
                    palette.priceBoxInactiveBg(),
                    palette.priceBoxInactiveStroke(),
                    260,
                    bgHeight,
                    cornerRadius
            );
            views.setImageViewBitmap(bgViewId, inactiveBg);

            palette.applyTextColor(views, labelViewId, palette.onSurfaceRes(), palette.onSurface());
            palette.applyTextColor(views, priceViewId, palette.onSurfaceRes(), palette.onSurface());
            palette.applyTextColor(views, unitViewId, palette.onSurfaceVariantRes(), palette.onSurfaceVariant());

            views.setViewVisibility(dotViewId, View.VISIBLE);
            views.setImageViewBitmap(dotViewId, WidgetBitmapRenderer.renderColorDotBitmap(tariffColor, 18));
            views.setViewVisibility(activeIconViewId, View.GONE);
        }
    }


    @NonNull
    private static RemoteViews buildHdoClockWidget(@NonNull Context context,
                                                   @NonNull WidgetThemePalette palette,
                                                   int appWidgetId,
                                                   @Nullable WidgetConfig config,
                                                   @NonNull WidgetPointResolver.ResolutionResult resolution,
                                                   @Nullable Bundle widgetOptions) {
        RemoteViews views = createBaseRemoteViews(context, R.layout.widget_hdo_clock, appWidgetId, true);
        applyHdoClockTheme(views, palette);
        views.setOnClickPendingIntent(
                R.id.btn_widget_config_strip,
                WidgetNavigationHelper.createConfigurePendingIntent(context, appWidgetId)
        );

        int widthDp = resolveWidgetWidthDp(widgetOptions, 140);
        int heightDp = resolveWidgetHeightDp(widgetOptions, 72);
        boolean miniMode = widthDp < HDO_CLOCK_MINI_MAX_WIDTH_DP;

        if (resolution.status() != WidgetPointResolver.ResolutionStatus.RESOLVED
                || resolution.point() == null
                || config == null) {
            bindMissingOrUnconfiguredState(context, views, appWidgetId, WidgetType.HDO_CLOCK, config, resolution, !miniMode);
            if (miniMode) {
                views.setViewVisibility(R.id.tv_widget_error_title, View.GONE);
                views.setViewVisibility(R.id.tv_widget_error_subtitle, View.GONE);
            } else {
                views.setViewVisibility(R.id.tv_widget_error_title, View.VISIBLE);
                views.setViewVisibility(R.id.tv_widget_error_subtitle, View.VISIBLE);
            }
            return views;
        }

        SubscriptionPointModel point = resolution.point();
        views.setViewVisibility(R.id.widget_content_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_error_container, View.GONE);

        PendingIntent navIntent = WidgetNavigationHelper.createNavigationPendingIntent(
                context, appWidgetId, WidgetNavigationHelper.DEST_HDO, point.getId(), point.getMilins());
        views.setOnClickPendingIntent(R.id.widget_root, navIntent);

        long nowMillis = System.currentTimeMillis();
        WidgetDataService.HdoSnapshot hdo = WidgetDataService.loadHdoSnapshot(context, point, nowMillis);
        float density = context.getResources().getDisplayMetrics().density;

        boolean stripMode = !miniMode && heightDp < HDO_CLOCK_LARGE_MIN_HEIGHT_DP;
        boolean wideLargeMode = !miniMode && !stripMode && widthDp >= HDO_CLOCK_WIDE_THRESHOLD_DP;

        int clockTargetDp;
        if (miniMode) {
            clockTargetDp = Math.max(44, Math.min(widthDp - 8, heightDp - 8));
        } else if (stripMode) {
            clockTargetDp = Math.max(48, Math.min(Math.round(widthDp * 0.42f), heightDp - 12));
        } else if (wideLargeMode) {
            clockTargetDp = Math.min(widthDp / 2, Math.max(96, heightDp - 36));
        } else {
            clockTargetDp = Math.min(Math.max(88, widthDp - 24), Math.max(88, heightDp - 52));
        }
        int clockSizePx = Math.round(clockTargetDp * density);

        Bitmap clockBitmap = WidgetBitmapRenderer.renderHdoClockBitmap(
                context,
                palette,
                hdo.allHdoModels(),
                hdo.timeShiftMillis(),
                nowMillis,
                clockSizePx
        );

        if (miniMode) {
            // Režim 1×1: čistý ciferník bez textových popisků
            views.setViewVisibility(R.id.layout_clock_mini, View.VISIBLE);
            views.setViewVisibility(R.id.layout_clock_strip, View.GONE);
            views.setViewVisibility(R.id.layout_clock_large, View.GONE);
            views.setImageViewBitmap(R.id.iv_widget_clock_mini, clockBitmap);
            return views;
        }

        String nextSwitchCompact = formatHdoCompactNextText(context, hdo);
        DashboardHdoFormatHelper.FormattedHdoSwitch formatted = hdo.formattedSwitch();

        if (stripMode) {
            // Výchozí pruhový režim 2×1 (a 3×1)
            views.setViewVisibility(R.id.layout_clock_mini, View.GONE);
            views.setViewVisibility(R.id.layout_clock_strip, View.VISIBLE);
            views.setViewVisibility(R.id.layout_clock_large, View.GONE);

            views.setImageViewBitmap(R.id.iv_widget_clock_strip, clockBitmap);
            views.setTextViewText(R.id.tv_widget_point_name_strip, point.getName());

            boolean wideStrip = widthDp >= HDO_CLOCK_STRIP_WIDE_THRESHOLD_DP;
            if (!hdo.hasHdo()) {
                views.setTextViewText(R.id.tv_widget_hdo_status_strip, context.getString(R.string.dashboard_hdo_not_set));
                views.setViewVisibility(R.id.tv_widget_hdo_detail_strip, View.GONE);
            } else if (wideStrip) {
                views.setTextViewText(
                        R.id.tv_widget_hdo_status_strip,
                        context.getString(hdo.isNtActive()
                                ? R.string.widget_hdo_clock_active_nt
                                : R.string.widget_hdo_clock_active_vt)
                );
                String detail = formatPriceWidgetHdoFooter(context, hdo);
                views.setViewVisibility(R.id.tv_widget_hdo_detail_strip, View.VISIBLE);
                views.setTextViewText(R.id.tv_widget_hdo_detail_strip, detail);
            } else {
                String statusText;
                if (formatted.valid()) {
                    statusText = context.getString(
                            hdo.isNtActive()
                                    ? R.string.widget_hdo_clock_strip_nt_next
                                    : R.string.widget_hdo_clock_strip_vt_next,
                            formatted.timeText()
                    );
                } else {
                    statusText = context.getString(
                            hdo.isNtActive()
                                    ? R.string.widget_hdo_clock_strip_nt
                                    : R.string.widget_hdo_clock_strip_vt
                    );
                }
                views.setTextViewText(R.id.tv_widget_hdo_status_strip, statusText);

                if (heightDp >= 64 && formatted.valid()
                        && formatted.remainingText() != null
                        && !formatted.remainingText().isEmpty()) {
                    views.setViewVisibility(R.id.tv_widget_hdo_detail_strip, View.VISIBLE);
                    views.setTextViewText(
                            R.id.tv_widget_hdo_detail_strip,
                            context.getString(R.string.widget_hdo_clock_remaining_compact, formatted.remainingText())
                    );
                } else {
                    views.setViewVisibility(R.id.tv_widget_hdo_detail_strip, View.GONE);
                }
            }
            return views;
        }

        // Větší zobrazení 2×2 a 3×2+
        views.setViewVisibility(R.id.layout_clock_mini, View.GONE);
        views.setViewVisibility(R.id.layout_clock_strip, View.GONE);
        views.setViewVisibility(R.id.layout_clock_large, View.VISIBLE);
        views.setTextViewText(R.id.tv_widget_point_name, point.getName());

        if (wideLargeMode) {
            views.setViewVisibility(R.id.layout_clock_compact, View.GONE);
            views.setViewVisibility(R.id.layout_clock_wide, View.VISIBLE);
            views.setImageViewBitmap(R.id.iv_widget_clock_wide, clockBitmap);

            if (!hdo.hasHdo()) {
                views.setTextViewText(R.id.tv_widget_hdo_tariff_wide, context.getString(R.string.dashboard_hdo_not_set));
                views.setTextViewText(R.id.tv_widget_hdo_next_wide, "");
                views.setTextViewText(R.id.tv_widget_hdo_remaining_wide, "");
            } else {
                views.setTextViewText(
                        R.id.tv_widget_hdo_tariff_wide,
                        context.getString(hdo.isNtActive()
                                ? R.string.widget_hdo_clock_active_nt
                                : R.string.widget_hdo_clock_active_vt)
                );
                views.setTextViewText(R.id.tv_widget_hdo_next_wide, nextSwitchCompact);
                if (formatted.valid() && formatted.remainingText() != null && !formatted.remainingText().isEmpty()) {
                    views.setTextViewText(
                            R.id.tv_widget_hdo_remaining_wide,
                            context.getString(R.string.widget_hdo_clock_remaining_compact, formatted.remainingText())
                    );
                } else {
                    views.setTextViewText(R.id.tv_widget_hdo_remaining_wide, "");
                }
            }
        } else {
            views.setViewVisibility(R.id.layout_clock_compact, View.VISIBLE);
            views.setViewVisibility(R.id.layout_clock_wide, View.GONE);
            views.setImageViewBitmap(R.id.iv_widget_clock_compact, clockBitmap);
            views.setTextViewText(R.id.tv_widget_hdo_next_compact, nextSwitchCompact);
        }

        return views;
    }


    @NonNull
    private static RemoteViews buildConsumptionWidget(@NonNull Context context,
                                                      @NonNull WidgetThemePalette palette,
                                                      int appWidgetId,
                                                      @Nullable WidgetConfig config,
                                                      @NonNull WidgetPointResolver.ResolutionResult resolution,
                                                      @Nullable Bundle widgetOptions) {
        RemoteViews views = createBaseRemoteViews(context, R.layout.widget_consumption, appWidgetId, true);
        applyConsumptionTheme(views, palette);

        if (resolution.status() != WidgetPointResolver.ResolutionStatus.RESOLVED
                || resolution.point() == null
                || config == null) {
            views.setTextViewText(R.id.tv_widget_period, "");
            bindMissingOrUnconfiguredState(context, views, appWidgetId, WidgetType.CONSUMPTION, config, resolution, true);
            return views;
        }

        SubscriptionPointModel point = resolution.point();
        views.setTextViewText(R.id.tv_widget_point_name, point.getName());

        PendingIntent navIntent = WidgetNavigationHelper.createNavigationPendingIntent(
                context, appWidgetId, WidgetNavigationHelper.DEST_MONTHLY_READINGS, point.getId(), point.getMilins());
        views.setOnClickPendingIntent(R.id.widget_root, navIntent);

        WidgetDataService.PeriodSnapshot snapshot = WidgetDataService.loadPeriodSnapshot(context, point);
        if (!snapshot.hasPeriod()) {
            views.setTextViewText(R.id.tv_widget_period, "");
            views.setViewVisibility(R.id.widget_content_container, View.GONE);
            views.setViewVisibility(R.id.widget_error_container, View.VISIBLE);
            views.setTextViewText(R.id.tv_widget_error_title, context.getString(R.string.widget_consumption_no_data));
            views.setTextViewText(R.id.tv_widget_error_subtitle, "");
            return views;
        }

        views.setViewVisibility(R.id.widget_content_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_error_container, View.GONE);

        String periodText = WidgetFormatHelper.formatShortPeriodRange(snapshot.minDateMillis(), snapshot.maxDateMillis());
        views.setTextViewText(R.id.tv_widget_period, periodText);

        float density = context.getResources().getDisplayMetrics().density;
        float fontScale = Math.max(1.0f, context.getResources().getConfiguration().fontScale);
        int widthDp = resolveWidgetWidthDp(widgetOptions, 250);
        int heightDp = resolveWidgetHeightDp(widgetOptions, 72);
        boolean compactMode = (heightDp / fontScale) < PERIOD_WIDGET_TALL_THRESHOLD_DP;

        String totalFormatted = WidgetFormatHelper.formatKwh(snapshot.totalKwh());
        String vtFormatted = WidgetFormatHelper.formatKwh(snapshot.vtKwh());
        String ntFormatted = WidgetFormatHelper.formatKwh(snapshot.ntKwh());

        if (compactMode) {
            views.setViewVisibility(R.id.layout_consumption_compact, View.VISIBLE);
            views.setViewVisibility(R.id.layout_consumption_tall, View.GONE);
            views.setTextViewText(R.id.tv_widget_total_kwh_compact, totalFormatted);

            int barWidthPx = Math.round(Math.max(80, widthDp - 120) * density);
            int barHeightPx = Math.round(9 * density);
            Bitmap splitBar = WidgetBitmapRenderer.renderConsumptionSplitBarBitmap(
                    palette,
                    snapshot.vtKwh(),
                    snapshot.ntKwh(),
                    snapshot.isSingleTariff(),
                    snapshot.colorVt(),
                    snapshot.colorNt(),
                    barWidthPx,
                    barHeightPx
            );
            views.setImageViewBitmap(R.id.iv_widget_consumption_bar_compact, splitBar);
            views.setImageViewBitmap(R.id.iv_widget_vt_dot_compact, WidgetBitmapRenderer.renderColorDotBitmap(snapshot.colorVt(), 18));

            if (snapshot.isSingleTariff()) {
                views.setViewVisibility(R.id.layout_widget_nt_legend_compact, View.GONE);
                views.setTextViewText(
                        R.id.tv_widget_vt_value_compact,
                        context.getString(R.string.widget_consumption_vt_short_format, vtFormatted)
                );
            } else {
                views.setViewVisibility(R.id.layout_widget_nt_legend_compact, View.VISIBLE);
                views.setImageViewBitmap(R.id.iv_widget_nt_dot_compact, WidgetBitmapRenderer.renderColorDotBitmap(snapshot.colorNt(), 18));
                boolean includePercent = widthDp >= CONSUMPTION_COMPACT_PERCENT_MIN_WIDTH_DP;
                if (includePercent) {
                    int[] pct = WidgetFormatHelper.computeTariffPercentages(snapshot.vtKwh(), snapshot.ntKwh());
                    views.setTextViewText(
                            R.id.tv_widget_vt_value_compact,
                            context.getString(R.string.widget_consumption_vt_compact_format, vtFormatted, pct[0])
                    );
                    views.setTextViewText(
                            R.id.tv_widget_nt_value_compact,
                            context.getString(R.string.widget_consumption_nt_compact_format, ntFormatted, pct[1])
                    );
                } else {
                    views.setTextViewText(
                            R.id.tv_widget_vt_value_compact,
                            context.getString(R.string.widget_consumption_vt_short_format, vtFormatted)
                    );
                    views.setTextViewText(
                            R.id.tv_widget_nt_value_compact,
                            context.getString(R.string.widget_consumption_nt_short_format, ntFormatted)
                    );
                }
            }
        } else {
            views.setViewVisibility(R.id.layout_consumption_compact, View.GONE);
            views.setViewVisibility(R.id.layout_consumption_tall, View.VISIBLE);
            views.setTextViewText(R.id.tv_widget_total_kwh, totalFormatted);

            int barWidthPx = Math.round(Math.max(120, widthDp - 24) * density);
            int barHeightPx = Math.round(12 * density);
            Bitmap splitBar = WidgetBitmapRenderer.renderConsumptionSplitBarBitmap(
                    palette,
                    snapshot.vtKwh(),
                    snapshot.ntKwh(),
                    snapshot.isSingleTariff(),
                    snapshot.colorVt(),
                    snapshot.colorNt(),
                    barWidthPx,
                    barHeightPx
            );
            views.setImageViewBitmap(R.id.iv_widget_consumption_bar, splitBar);
            views.setImageViewBitmap(R.id.iv_widget_vt_dot, WidgetBitmapRenderer.renderColorDotBitmap(snapshot.colorVt(), 20));

            if (snapshot.isSingleTariff()) {
                views.setViewVisibility(R.id.layout_widget_nt_legend, View.GONE);
                views.setTextViewText(R.id.tv_widget_vt_value, vtFormatted);
            } else {
                views.setViewVisibility(R.id.layout_widget_nt_legend, View.VISIBLE);
                views.setImageViewBitmap(R.id.iv_widget_nt_dot, WidgetBitmapRenderer.renderColorDotBitmap(snapshot.colorNt(), 20));
                int[] pct = WidgetFormatHelper.computeTariffPercentages(snapshot.vtKwh(), snapshot.ntKwh());
                views.setTextViewText(
                        R.id.tv_widget_vt_value,
                        context.getString(R.string.widget_consumption_tariff_value_format, vtFormatted, pct[0])
                );
                views.setTextViewText(
                        R.id.tv_widget_nt_value,
                        context.getString(R.string.widget_consumption_tariff_value_format, ntFormatted, pct[1])
                );
            }
        }

        return views;
    }


    @NonNull
    private static RemoteViews buildFinancialBalanceWidget(@NonNull Context context,
                                                           @NonNull WidgetThemePalette palette,
                                                           int appWidgetId,
                                                           @Nullable WidgetConfig config,
                                                           @NonNull WidgetPointResolver.ResolutionResult resolution,
                                                           @Nullable Bundle widgetOptions) {
        RemoteViews views = createBaseRemoteViews(context, R.layout.widget_financial_balance, appWidgetId, true);
        applyFinancialBalanceTheme(views, palette);

        if (resolution.status() != WidgetPointResolver.ResolutionStatus.RESOLVED
                || resolution.point() == null
                || config == null) {
            views.setTextViewText(R.id.tv_widget_period, "");
            bindMissingOrUnconfiguredState(context, views, appWidgetId, WidgetType.FINANCIAL_BALANCE, config, resolution, true);
            return views;
        }

        SubscriptionPointModel point = resolution.point();
        views.setTextViewText(R.id.tv_widget_point_name, point.getName());

        PendingIntent navIntent = WidgetNavigationHelper.createNavigationPendingIntent(
                context, appWidgetId, WidgetNavigationHelper.DEST_BILLING, point.getId(), point.getMilins());
        views.setOnClickPendingIntent(R.id.widget_root, navIntent);

        WidgetDataService.PeriodSnapshot snapshot = WidgetDataService.loadPeriodSnapshot(context, point);
        if (!snapshot.hasPeriod()) {
            views.setTextViewText(R.id.tv_widget_period, "");
            views.setViewVisibility(R.id.widget_content_container, View.GONE);
            views.setViewVisibility(R.id.widget_error_container, View.VISIBLE);
            views.setTextViewText(R.id.tv_widget_error_title, context.getString(R.string.widget_balance_no_data));
            views.setTextViewText(R.id.tv_widget_error_subtitle, "");
            return views;
        }

        views.setViewVisibility(R.id.widget_content_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_error_container, View.GONE);

        String periodText = WidgetFormatHelper.formatShortPeriodRange(snapshot.minDateMillis(), snapshot.maxDateMillis());
        views.setTextViewText(R.id.tv_widget_period, periodText);

        float density = context.getResources().getDisplayMetrics().density;
        float fontScale = Math.max(1.0f, context.getResources().getConfiguration().fontScale);
        int widthDp = resolveWidgetWidthDp(widgetOptions, 250);
        int heightDp = resolveWidgetHeightDp(widgetOptions, 72);
        boolean compactMode = (heightDp / fontScale) < PERIOD_WIDGET_TALL_THRESHOLD_DP;

        String advancesFormatted = WidgetFormatHelper.formatCzk(snapshot.advancesKc());
        String costsFormatted = WidgetFormatHelper.formatCzk(snapshot.costsWithTaxKc());
        String absBalanceText = WidgetFormatHelper.formatCzk(Math.abs(snapshot.balanceKc()));
        InvoiceBalanceHelper.BalanceState balanceState = snapshot.balanceState();

        String badgeText;
        int badgeBgRes;
        int badgeTextColorRes;
        int badgeTextColorInt;
        if (balanceState == InvoiceBalanceHelper.BalanceState.OVERPAYMENT) {
            badgeText = context.getString(R.string.widget_balance_overpayment_format, absBalanceText);
            badgeBgRes = R.drawable.bg_widget_badge_overpayment;
            badgeTextColorRes = R.color.dashboard_overpayment_text;
            badgeTextColorInt = ContextCompat.getColor(context, R.color.dashboard_overpayment_text);
        } else if (balanceState == InvoiceBalanceHelper.BalanceState.UNDERPAYMENT) {
            badgeText = context.getString(R.string.widget_balance_underpayment_format, absBalanceText);
            badgeBgRes = R.drawable.bg_widget_badge_underpayment;
            badgeTextColorRes = R.color.dashboard_underpayment_text;
            badgeTextColorInt = ContextCompat.getColor(context, R.color.dashboard_underpayment_text);
        } else {
            badgeText = context.getString(R.string.widget_balance_balanced_format, WidgetFormatHelper.formatCzk(0.0));
            badgeBgRes = palette.neutralPillBackgroundRes();
            badgeTextColorRes = palette.onSurfaceVariantRes();
            badgeTextColorInt = palette.onSurfaceVariant();
        }

        if (compactMode) {
            views.setViewVisibility(R.id.layout_balance_compact, View.VISIBLE);
            views.setViewVisibility(R.id.layout_balance_tall, View.GONE);

            views.setTextViewText(R.id.tv_widget_balance_badge_compact, badgeText);
            views.setInt(R.id.tv_widget_balance_badge_compact, "setBackgroundResource", badgeBgRes);
            palette.applyTextColor(views, R.id.tv_widget_balance_badge_compact, badgeTextColorRes, badgeTextColorInt);

            views.setTextViewText(
                    R.id.tv_widget_advances_compact,
                    context.getString(R.string.widget_balance_advances_compact_format, advancesFormatted)
            );
            views.setTextViewText(
                    R.id.tv_widget_costs_compact,
                    context.getString(R.string.widget_balance_costs_compact_format, costsFormatted)
            );

            int barWidthPx = Math.round(Math.max(80, widthDp - 150) * density);
            int barHeightPx = Math.round(10 * density);
            Bitmap balanceBar = WidgetBitmapRenderer.renderBalanceBarBitmap(
                    context,
                    palette,
                    snapshot.advancesKc(),
                    snapshot.costsWithTaxKc(),
                    barWidthPx,
                    barHeightPx
            );
            views.setImageViewBitmap(R.id.iv_widget_balance_bar_compact, balanceBar);
        } else {
            views.setViewVisibility(R.id.layout_balance_compact, View.GONE);
            views.setViewVisibility(R.id.layout_balance_tall, View.VISIBLE);

            views.setTextViewText(R.id.tv_widget_balance_badge, badgeText);
            views.setInt(R.id.tv_widget_balance_badge, "setBackgroundResource", badgeBgRes);
            palette.applyTextColor(views, R.id.tv_widget_balance_badge, badgeTextColorRes, badgeTextColorInt);

            views.setTextViewText(R.id.tv_widget_advances_value, advancesFormatted);
            views.setTextViewText(R.id.tv_widget_costs_value, costsFormatted);

            int barWidthPx = Math.round(Math.max(120, widthDp - 24) * density);
            int barHeightPx = Math.round(16 * density);
            Bitmap balanceBar = WidgetBitmapRenderer.renderBalanceBarBitmap(
                    context,
                    palette,
                    snapshot.advancesKc(),
                    snapshot.costsWithTaxKc(),
                    barWidthPx,
                    barHeightPx
            );
            views.setImageViewBitmap(R.id.iv_widget_balance_bar, balanceBar);
        }

        return views;
    }


    @NonNull
    private static RemoteViews buildNewReadingWidget(@NonNull Context context,
                                                     @NonNull WidgetThemePalette palette,
                                                     int appWidgetId,
                                                     @Nullable WidgetConfig config,
                                                     @NonNull WidgetPointResolver.ResolutionResult resolution,
                                                     @Nullable Bundle widgetOptions) {
        RemoteViews views = createBaseRemoteViews(context, R.layout.widget_new_reading, appWidgetId, false);
        applyNewReadingTheme(views, palette);
        int widthDp = resolveWidgetWidthDp(widgetOptions, 64);
        boolean compactMode = widthDp < NEW_READING_WIDE_THRESHOLD_DP;

        if (resolution.status() != WidgetPointResolver.ResolutionStatus.RESOLVED
                || resolution.point() == null
                || config == null) {
            bindMissingOrUnconfiguredState(context, views, appWidgetId, WidgetType.NEW_READING, config, resolution, false);
            views.setViewVisibility(R.id.tv_widget_error_title, compactMode ? View.GONE : View.VISIBLE);
            return views;
        }

        SubscriptionPointModel point = resolution.point();
        views.setViewVisibility(R.id.widget_content_container, View.VISIBLE);
        views.setViewVisibility(R.id.widget_error_container, View.GONE);

        if (compactMode) {
            views.setViewVisibility(R.id.layout_new_reading_compact, View.VISIBLE);
            views.setViewVisibility(R.id.layout_new_reading_wide, View.GONE);
            views.setTextViewText(R.id.tv_widget_point_name_compact, point.getName());
        } else {
            views.setViewVisibility(R.id.layout_new_reading_compact, View.GONE);
            views.setViewVisibility(R.id.layout_new_reading_wide, View.VISIBLE);
            views.setTextViewText(R.id.tv_widget_point_name, point.getName());
        }

        PendingIntent navIntent = WidgetNavigationHelper.createNavigationPendingIntent(
                context, appWidgetId, WidgetNavigationHelper.DEST_NEW_READING, point.getId(), point.getMilins());
        views.setOnClickPendingIntent(R.id.widget_root, navIntent);

        return views;
    }


    @NonNull
    private static RemoteViews createBaseRemoteViews(@NonNull Context context,
                                                     @LayoutRes int layoutResId,
                                                     int appWidgetId,
                                                     boolean hasConfigButton) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutResId);
        if (hasConfigButton) {
            views.setOnClickPendingIntent(
                    R.id.btn_widget_config,
                    WidgetNavigationHelper.createConfigurePendingIntent(context, appWidgetId)
            );
        }
        return views;
    }


    private static void applyCurrentPriceTheme(@NonNull RemoteViews views,
                                               @NonNull WidgetThemePalette palette) {
        if (!palette.isDynamicColorsActive()) {
            return;
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.priceCardBackgroundRes());
        palette.applyImageTint(views, R.id.iv_widget_pylon, palette.primaryRes(), palette.primary());
        views.setImageViewResource(R.id.btn_widget_config, palette.settingsIconRes());
        applyTextColorToViews(views, palette, palette.onSurfaceRes(), palette.onSurface(),
                R.id.tv_widget_point_name,
                R.id.tv_widget_error_title);
        applyTextColorToViews(views, palette, palette.onSurfaceVariantRes(), palette.onSurfaceVariant(),
                R.id.tv_widget_price_mode,
                R.id.tv_widget_hdo_status,
                R.id.tv_widget_error_subtitle);
    }


    private static void applyHdoClockTheme(@NonNull RemoteViews views,
                                           @NonNull WidgetThemePalette palette) {
        if (!palette.isDynamicColorsActive()) {
            return;
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.cardBackgroundRes());
        views.setImageViewResource(R.id.btn_widget_config, palette.settingsIconRes());
        views.setImageViewResource(R.id.btn_widget_config_strip, palette.settingsIconRes());
        applyTextColorToViews(views, palette, palette.onSurfaceRes(), palette.onSurface(),
                R.id.tv_widget_point_name_strip,
                R.id.tv_widget_point_name,
                R.id.tv_widget_hdo_next_wide,
                R.id.tv_widget_error_title);
        applyTextColorToViews(views, palette, palette.onSurfaceVariantRes(), palette.onSurfaceVariant(),
                R.id.tv_widget_hdo_detail_strip,
                R.id.tv_widget_hdo_next_compact,
                R.id.tv_widget_hdo_remaining_wide,
                R.id.tv_widget_error_subtitle);
        applyTextColorToViews(views, palette, palette.primaryRes(), palette.primary(),
                R.id.tv_widget_hdo_status_strip,
                R.id.tv_widget_hdo_tariff_wide);
    }


    private static void applyConsumptionTheme(@NonNull RemoteViews views,
                                              @NonNull WidgetThemePalette palette) {
        if (!palette.isDynamicColorsActive()) {
            return;
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.cardBackgroundRes());
        palette.applyImageTint(views, R.id.iv_widget_bg_art, palette.primaryRes(), palette.primary());
        views.setImageViewResource(R.id.btn_widget_config, palette.settingsIconRes());
        applyTextColorToViews(views, palette, palette.onSurfaceRes(), palette.onSurface(),
                R.id.tv_widget_point_name,
                R.id.tv_widget_total_kwh_compact,
                R.id.tv_widget_vt_value_compact,
                R.id.tv_widget_nt_value_compact,
                R.id.tv_widget_total_kwh,
                R.id.tv_widget_vt_value,
                R.id.tv_widget_nt_value,
                R.id.tv_widget_error_title);
        applyTextColorToViews(views, palette, palette.onSurfaceVariantRes(), palette.onSurfaceVariant(),
                R.id.tv_widget_period,
                R.id.tv_widget_total_label,
                R.id.tv_widget_vt_label_tall,
                R.id.tv_widget_nt_label_tall,
                R.id.tv_widget_error_subtitle);
    }


    private static void applyFinancialBalanceTheme(@NonNull RemoteViews views,
                                                   @NonNull WidgetThemePalette palette) {
        if (!palette.isDynamicColorsActive()) {
            return;
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.cardBackgroundRes());
        palette.applyImageTint(views, R.id.iv_widget_bg_art, palette.primaryRes(), palette.primary());
        views.setImageViewResource(R.id.btn_widget_config, palette.settingsIconRes());
        applyTextColorToViews(views, palette, palette.onSurfaceRes(), palette.onSurface(),
                R.id.tv_widget_point_name,
                R.id.tv_widget_advances_compact,
                R.id.tv_widget_costs_compact,
                R.id.tv_widget_advances_value,
                R.id.tv_widget_costs_value,
                R.id.tv_widget_error_title);
        applyTextColorToViews(views, palette, palette.onSurfaceVariantRes(), palette.onSurfaceVariant(),
                R.id.tv_widget_period,
                R.id.tv_widget_advances_label,
                R.id.tv_widget_costs_label,
                R.id.tv_widget_error_subtitle);
    }


    private static void applyNewReadingTheme(@NonNull RemoteViews views,
                                             @NonNull WidgetThemePalette palette) {
        if (!palette.isDynamicColorsActive()) {
            return;
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.actionCardBackgroundRes());
        views.setImageViewResource(R.id.iv_widget_action_icon_compact, palette.addReadingIconRes());
        views.setImageViewResource(R.id.iv_widget_action_icon, palette.addReadingIconRes());
        applyTextColorToViews(views, palette, palette.onPrimaryContainerRes(), palette.onPrimaryContainer(),
                R.id.tv_widget_point_name_compact,
                R.id.tv_widget_action_title,
                R.id.tv_widget_point_name);
        palette.applyTextColor(views, R.id.tv_widget_error_title, palette.onSurfaceRes(), palette.onSurface());
    }


    private static void applyTextColorToViews(@NonNull RemoteViews views,
                                              @NonNull WidgetThemePalette palette,
                                              int colorRes,
                                              int fallbackColorInt,
                                              int... viewIds) {
        for (int viewId : viewIds) {
            palette.applyTextColor(views, viewId, colorRes, fallbackColorInt);
        }
    }


    private static void bindMissingOrUnconfiguredState(@NonNull Context context,
                                                       @NonNull RemoteViews views,
                                                       int appWidgetId,
                                                       @NonNull WidgetType widgetType,
                                                       @Nullable WidgetConfig config,
                                                       @NonNull WidgetPointResolver.ResolutionResult resolution,
                                                       boolean hasSubtitleAndHeader) {
        views.setViewVisibility(R.id.widget_content_container, View.GONE);
        views.setViewVisibility(R.id.widget_error_container, View.VISIBLE);

        boolean isMissing = resolution.status() == WidgetPointResolver.ResolutionStatus.MISSING;
        String errorTitle = context.getString(isMissing ? R.string.widget_point_missing : R.string.widget_point_unconfigured);
        views.setTextViewText(R.id.tv_widget_error_title, errorTitle);

        if (hasSubtitleAndHeader) {
            String headerName = (config != null && !config.getSubscriptionPointNameSnapshot().isEmpty())
                    ? config.getSubscriptionPointNameSnapshot()
                    : context.getString(widgetType.getLabelResId());
            views.setTextViewText(R.id.tv_widget_point_name, headerName);
            views.setTextViewText(R.id.tv_widget_error_subtitle, context.getString(R.string.widget_point_missing_hint));
        }

        views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetNavigationHelper.createConfigurePendingIntent(context, appWidgetId)
        );
    }


    @NonNull
    private static String resolvePriceModeLabel(@NonNull Context context,
                                                @NonNull WidgetConfig config,
                                                boolean compactMode) {
        if (compactMode) {
            if (config.isShowTotalPrice()) {
                return context.getString(config.isIncludeVat()
                        ? R.string.widget_price_mode_compact_total_with_vat
                        : R.string.widget_price_mode_compact_total_without_vat);
            }
            return context.getString(config.isIncludeVat()
                    ? R.string.widget_price_mode_compact_commercial_with_vat
                    : R.string.widget_price_mode_compact_commercial_without_vat);
        }
        if (config.isShowTotalPrice()) {
            return context.getString(config.isIncludeVat()
                    ? R.string.widget_price_mode_total_with_vat
                    : R.string.widget_price_mode_total_without_vat);
        }
        return context.getString(config.isIncludeVat()
                ? R.string.widget_price_mode_commercial_with_vat
                : R.string.widget_price_mode_commercial_without_vat);
    }


    @NonNull
    private static String formatExpiredPriceWarning(@NonNull Context context, @NonNull String validToText) {
        if (validToText.isEmpty()) {
            return context.getString(R.string.widget_price_expired_indicative_no_date);
        }
        return context.getString(R.string.widget_price_expired_indicative_format, validToText);
    }


    @NonNull
    private static String formatExpiredPriceCompactFooter(@NonNull Context context,
                                                          @NonNull String validToText,
                                                          @NonNull WidgetDataService.HdoSnapshot hdo,
                                                          boolean isSingleTariff) {
        DashboardHdoFormatHelper.FormattedHdoSwitch formatted = hdo.formattedSwitch();
        if (!isSingleTariff && hdo.hasHdo() && formatted.valid()) {
            if (validToText.isEmpty()) {
                return context.getString(R.string.widget_price_expired_compact_no_date_with_hdo_format, formatted.timeText());
            }
            return context.getString(R.string.widget_price_expired_compact_with_hdo_format, validToText, formatted.timeText());
        }
        return formatExpiredPriceWarning(context, validToText);
    }


    @NonNull
    private static String formatPriceWidgetHdoFooter(@NonNull Context context,
                                                     @NonNull WidgetDataService.HdoSnapshot hdo) {
        if (!hdo.hasHdo()) {
            return context.getString(R.string.dashboard_hdo_not_set);
        }
        DashboardHdoFormatHelper.FormattedHdoSwitch formatted = hdo.formattedSwitch();
        if (!formatted.valid()) {
            return context.getString(R.string.dashboard_hdo_invalid_data);
        }
        if (formatted.remainingText() != null && !formatted.remainingText().isEmpty()) {
            return context.getString(
                    R.string.widget_hdo_next_with_remaining,
                    formatted.timeText(),
                    formatted.remainingText()
            );
        }
        return formatted.isToday()
                ? context.getString(R.string.widget_hdo_next_today, formatted.timeText())
                : context.getString(R.string.widget_hdo_next_day, formatted.timeText());
    }


    @NonNull
    private static String formatHdoCompactNextText(@NonNull Context context,
                                                   @NonNull WidgetDataService.HdoSnapshot hdo) {
        if (!hdo.hasHdo()) {
            return context.getString(R.string.dashboard_hdo_not_set);
        }
        DashboardHdoFormatHelper.FormattedHdoSwitch formatted = hdo.formattedSwitch();
        if (!formatted.valid()) {
            return context.getString(R.string.dashboard_hdo_invalid_data);
        }
        return context.getString(R.string.widget_hdo_clock_next_compact, formatted.timeText());
    }


    private static int resolveWidgetWidthDp(@Nullable Bundle options, int fallbackDp) {
        if (options == null) {
            return fallbackDp;
        }
        int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        if (minW > 0) {
            return minW;
        }
        return maxW > 0 ? maxW : fallbackDp;
    }


    private static int resolveWidgetHeightDp(@Nullable Bundle options, int fallbackDp) {
        if (options == null) {
            return fallbackDp;
        }
        int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        int maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (minH > 0) {
            return minH;
        }
        return maxH > 0 ? maxH : fallbackDp;
    }

}
