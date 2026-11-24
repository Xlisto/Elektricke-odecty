package cz.xlisto.elektrodroid.modules.widgets;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.modules.dashboard.DashboardBillingBalanceBarView;
import cz.xlisto.elektrodroid.utils.BuilderHDOStack;


/**
 * Vykresluje bitmapové grafické prvky pro {@link android.widget.RemoteViews} widgetů
 * pomocí {@link Canvas} a {@link Paint} s přísným omezením rozměrů pro šetrné nakládání s pamětí IPC.
 * <p>
 * Zajišťuje vykreslování:
 * <ul>
 *     <li>24hodinových analogových hodin HDO s mezikružími TUV, TAR a PV (shodně s {@code DashboardHdoClockView}),</li>
 *     <li>poměrového pruhu spotřeby VT / NT respektujícího uživatelské barvy,</li>
 *     <li>bilaterálního pruhu finanční bilance v sémantických barvách (zelená / červená),</li>
 *     <li>barevného pozadí tarifního boxu a barevných bodů legendy.</li>
 * </ul>
 */
public final class WidgetBitmapRenderer {

    private static final int[] HOURS_8 = {0, 3, 6, 9, 12, 15, 18, 21};
    private static final int[] HOURS_4 = {0, 6, 12, 18};
    private static final float RING_STEP_RATIO = 0.28f;
    private static final int MAX_CLOCK_SIZE_PX = 420;
    private static final int MIN_CLOCK_SIZE_PX = 120;
    private static final int MAX_BAR_WIDTH_PX = 540;
    private static final int MIN_BAR_WIDTH_PX = 160;
    private static final int MAX_BAR_HEIGHT_PX = 36;


    private WidgetBitmapRenderer() {
    }


    /**
     * Vrátí barvu prstence ciferníku a bodu legendy odpovídající zadanému typu relé HDO.
     *
     * @param context   kontext aplikace
     * @param relayType kategorie relé ({@link WidgetDataService.HdoRelayType})
     * @return ARGB barva relé
     */
    @ColorInt
    public static int resolveRelayColor(@NonNull Context context,
                                        @NonNull WidgetDataService.HdoRelayType relayType) {
        return switch (relayType) {
            case TUV -> ContextCompat.getColor(context, R.color.color_hdo_time_TUV);
            case TAR -> ContextCompat.getColor(context, R.color.color_hdo_time_TAR);
            case PV -> ContextCompat.getColor(context, R.color.color_hdo_time_PV);
        };
    }


    /**
     * Vykreslí 24hodinové analogové hodiny HDO s dnešními intervaly a ručičkou aktuálního času
     * do nové instance {@link Bitmap}.
     *
     * @param context         kontext aplikace (pro rozlišení světlého/tmavého motivu a barev HDO)
     * @param palette         aktivní barevná paleta widgetu (respektující dynamické barvy)
     * @param allHdoModels    seznam HDO modelů odběrného místa
     * @param timeShiftMillis posun času elektroměru v ms
     * @param nowMillis       aktuální systémový čas v ms
     * @param requestedSizePx cílová velikost čtvercové bitmapy v pixelech
     * @return vykreslená {@link Bitmap}
     */
    @NonNull
    public static Bitmap renderHdoClockBitmap(@NonNull Context context,
                                              @NonNull WidgetThemePalette palette,
                                              @Nullable List<HdoModel> allHdoModels,
                                              long timeShiftMillis,
                                              long nowMillis,
                                              int requestedSizePx) {
        int size = Math.max(MIN_CLOCK_SIZE_PX, Math.min(MAX_CLOCK_SIZE_PX, requestedSizePx));
        float density = Math.max(1.0f, context.getResources().getDisplayMetrics().density);
        float clockDp = size / density;
        boolean tinyDial = clockDp < 52f;
        boolean compactDial = clockDp < 95f;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float cx = size / 2f;
        float cy = size / 2f;
        float strokeWidth = Math.max(2f, size * (compactDial ? 0.022f : 0.015f));
        float pad = strokeWidth + 2f;
        float radius = (size / 2f) - pad;

        int colorDialBg = palette.surfaceContainerHigh();
        int colorDialStroke = palette.primary();
        int colorOnSurface = palette.onSurface();
        int colorHdoTuv = ContextCompat.getColor(context, R.color.color_hdo_time_TUV);
        int colorHdoTar = ContextCompat.getColor(context, R.color.color_hdo_time_TAR);
        int colorHdoPv = ContextCompat.getColor(context, R.color.color_hdo_time_PV);
        int colorHand = 0xFFD32F2F;

        // 1. Pozadí ciferníku
        Paint paintDialBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDialBg.setStyle(Paint.Style.FILL);
        paintDialBg.setColor(colorDialBg);
        canvas.drawCircle(cx, cy, radius, paintDialBg);

        // 2. Dnešní intervaly HDO (TUV, TAR, PV)
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTimeInMillis(nowMillis + timeShiftMillis);
        drawTodayHdoSectors(canvas, allHdoModels, timeShiftMillis, todayCal, cx, cy, radius,
                colorHdoTuv, colorHdoTar, colorHdoPv);

        // 3. Hodinové značky (24 dílků, v mini režimu silnější hlavní dílky po 3 hodinách)
        Paint paintTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTick.setStyle(Paint.Style.STROKE);
        paintTick.setStrokeWidth(Math.max(1.2f, size * (compactDial ? 0.012f : 0.008f)));
        paintTick.setColor(ColorUtils.setAlphaComponent(colorOnSurface, 175));

        float majorTickLen = Math.max(4f, radius * (compactDial ? 0.13f : 0.10f));
        float minorTickLen = Math.max(2.5f, radius * 0.055f);
        for (int i = 0; i < 24; i++) {
            boolean isMajor = (i % 3 == 0);
            if (tinyDial && !isMajor) {
                continue;
            }
            double rad = Math.toRadians(270 + i * 15);
            float tickLen = isMajor ? majorTickLen : minorTickLen;
            float sx = (float) (cx + radius * Math.cos(rad));
            float sy = (float) (cy + radius * Math.sin(rad));
            float ex = (float) (cx + (radius - tickLen) * Math.cos(rad));
            float ey = (float) (cy + (radius - tickLen) * Math.sin(rad));
            canvas.drawLine(sx, sy, ex, ey, paintTick);
        }

        // 4. Popisky hodin (u velmi malého ciferníku 1×1 vynechány pro čitelnost)
        if (!tinyDial) {
            Paint paintNumbers = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintNumbers.setStyle(Paint.Style.FILL);
            paintNumbers.setColor(colorOnSurface);
            paintNumbers.setTextAlign(Paint.Align.CENTER);
            paintNumbers.setFakeBoldText(true);
            paintNumbers.setTextSize(Math.max(11f, radius * (compactDial ? 0.21f : 0.17f)));

            int[] hoursToShow = compactDial ? HOURS_4 : HOURS_8;
            float textRadius = radius - majorTickLen - (radius * (compactDial ? 0.15f : 0.13f));
            Paint.FontMetrics fm = paintNumbers.getFontMetrics();
            float textVerticalOffset = -(fm.ascent + fm.descent) / 2f;

            for (int hour : hoursToShow) {
                double rad = Math.toRadians(270 + hour * 15);
                float tx = (float) (cx + textRadius * Math.cos(rad));
                float ty = (float) (cy + textRadius * Math.sin(rad)) + textVerticalOffset;
                canvas.drawText(String.valueOf(hour), tx, ty, paintNumbers);
            }
        }

        // 5. Vnější obrys ciferníku
        Paint paintDialStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDialStroke.setStyle(Paint.Style.STROKE);
        paintDialStroke.setStrokeWidth(strokeWidth);
        paintDialStroke.setColor(colorDialStroke);
        canvas.drawCircle(cx, cy, radius, paintDialStroke);

        // 6. Ručička aktuálního času (24h režim)
        int hours = todayCal.get(Calendar.HOUR_OF_DAY);
        int minutes = todayCal.get(Calendar.MINUTE);
        int seconds = todayCal.get(Calendar.SECOND);
        double handAngleRad = Math.toRadians(270 + hours * 15 + minutes * 0.25 + seconds * (0.25 / 60.0));
        float handLen = radius * 0.84f;
        float hx = (float) (cx + handLen * Math.cos(handAngleRad));
        float hy = (float) (cy + handLen * Math.sin(handAngleRad));

        Paint paintHand = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHand.setStyle(Paint.Style.STROKE);
        paintHand.setStrokeWidth(Math.max(2.5f, size * (compactDial ? 0.028f : 0.020f)));
        paintHand.setStrokeCap(Paint.Cap.ROUND);
        paintHand.setColor(colorHand);
        canvas.drawLine(cx, cy, hx, hy, paintHand);

        Paint paintCenterDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenterDot.setStyle(Paint.Style.FILL);
        paintCenterDot.setColor(colorHand);
        canvas.drawCircle(cx, cy, Math.max(3.5f, size * (compactDial ? 0.036f : 0.028f)), paintCenterDot);

        return bitmap;
    }


    /**
     * Vykreslí horizontální poměrový pruh spotřeby VT / NT respektující uživatelské barvy tarifů.
     *
     * @param palette        aktivní barevná paleta widgetu (respektující dynamické barvy)
     * @param vtKwh          spotřeba ve vysokém tarifu (kWh)
     * @param ntKwh          spotřeba v nízkém tarifu (kWh)
     * @param isSingleTariff {@code true}, pokud se jedná o jednosazbový tarif
     * @param colorVt        uživatelská barva VT
     * @param colorNt        uživatelská barva NT
     * @param widthPx        požadovaná šířka v pixelech
     * @param heightPx       požadovaná výška v pixelech
     * @return vykreslená {@link Bitmap}
     */
    @NonNull
    public static Bitmap renderConsumptionSplitBarBitmap(@NonNull WidgetThemePalette palette,
                                                         double vtKwh,
                                                         double ntKwh,
                                                         boolean isSingleTariff,
                                                         int colorVt,
                                                         int colorNt,
                                                         int widthPx,
                                                         int heightPx) {
        int w = Math.max(MIN_BAR_WIDTH_PX, Math.min(MAX_BAR_WIDTH_PX, widthPx));
        int h = Math.max(12, Math.min(MAX_BAR_HEIGHT_PX, heightPx));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float corner = h / 2f;
        RectF fullRect = new RectF(0f, 0f, w, h);

        Paint paintEmpty = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEmpty.setStyle(Paint.Style.FILL);
        paintEmpty.setColor(palette.outlineVariant());
        canvas.drawRoundRect(fullRect, corner, corner, paintEmpty);

        double cleanVt = Math.max(0.0, vtKwh);
        double cleanNt = isSingleTariff ? 0.0 : Math.max(0.0, ntKwh);
        double total = cleanVt + cleanNt;
        if (total <= 0.0001) {
            return bitmap;
        }

        Path clipPath = new Path();
        clipPath.addRoundRect(fullRect, corner, corner, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clipPath);

        float vtRatio = (float) Math.max(0.0, Math.min(1.0, cleanVt / total));
        boolean hasBoth = vtRatio > 0.005f && vtRatio < 0.995f;
        float gap = hasBoth ? Math.max(3f, h * 0.22f) : 0f;
        float availableWidth = Math.max(0f, w - gap);
        float vtWidth = vtRatio * availableWidth;

        if (vtWidth > 0.5f) {
            Paint paintVt = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintVt.setStyle(Paint.Style.FILL);
            paintVt.setColor(colorVt);
            canvas.drawRect(0f, 0f, vtWidth, h, paintVt);
        }
        float ntLeft = vtWidth + gap;
        if (w > ntLeft + 0.5f) {
            Paint paintNt = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintNt.setStyle(Paint.Style.FILL);
            paintNt.setColor(colorNt);
            canvas.drawRect(ntLeft, 0f, w, h, paintNt);
        }

        canvas.restoreToCount(save);
        return bitmap;
    }


    /**
     * Vykreslí horizontální bilaterální ukazatel finanční bilance se středovou nulovou ryskou
     * výhradně v sémantických barvách (zelená pro přeplatek, červená pro nedoplatek).
     *
     * @param context    kontext aplikace
     * @param palette    aktivní barevná paleta widgetu (respektující dynamické barvy)
     * @param advancesKc uhrazené zálohy v Kč
     * @param costsKc    celkové náklady v Kč
     * @param widthPx    požadovaná šířka v pixelech
     * @param heightPx   požadovaná výška v pixelech
     * @return vykreslená {@link Bitmap}
     */
    @NonNull
    public static Bitmap renderBalanceBarBitmap(@NonNull Context context,
                                                @NonNull WidgetThemePalette palette,
                                                double advancesKc,
                                                double costsKc,
                                                int widthPx,
                                                int heightPx) {
        int w = Math.max(MIN_BAR_WIDTH_PX, Math.min(MAX_BAR_WIDTH_PX, widthPx));
        int h = Math.max(16, Math.min(MAX_BAR_HEIGHT_PX, heightPx));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float trackHeight = Math.max(10f, h * 0.62f);
        float trackTop = (h - trackHeight) / 2f;
        float trackBottom = trackTop + trackHeight;
        float trackCorner = trackHeight / 2f;
        float centerX = w / 2f;
        float halfWidth = w / 2f;

        RectF trackRect = new RectF(0f, trackTop, w, trackBottom);
        Paint paintTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTrack.setStyle(Paint.Style.FILL);
        paintTrack.setColor(palette.outlineVariant());
        canvas.drawRoundRect(trackRect, trackCorner, trackCorner, paintTrack);

        float signedRatio = DashboardBillingBalanceBarView.calculateSignedRatio(advancesKc, costsKc);
        float absRatio = Math.abs(signedRatio);
        if (absRatio > 0.002f) {
            float fillWidth = Math.min(halfWidth, Math.max(4f, halfWidth * absRatio));
            Path clipPath = new Path();
            clipPath.addRoundRect(trackRect, trackCorner, trackCorner, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(clipPath);

            Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintFill.setStyle(Paint.Style.FILL);
            if (signedRatio > 0f) {
                paintFill.setColor(ContextCompat.getColor(context, R.color.dashboard_overpayment_text));
                canvas.drawRect(centerX, trackTop, centerX + fillWidth, trackBottom, paintFill);
            } else {
                paintFill.setColor(ContextCompat.getColor(context, R.color.dashboard_underpayment_text));
                canvas.drawRect(centerX - fillWidth, trackTop, centerX, trackBottom, paintFill);
            }
            canvas.restoreToCount(save);
        }

        // Středová nulová ryska
        Paint paintTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTick.setStyle(Paint.Style.FILL);
        paintTick.setColor(palette.onSurfaceVariant());
        float tickWidth = Math.max(3f, h * 0.14f);
        RectF tickRect = new RectF(centerX - tickWidth / 2f, 0f, centerX + tickWidth / 2f, h);
        canvas.drawRoundRect(tickRect, tickWidth / 2f, tickWidth / 2f, paintTick);

        return bitmap;
    }


    /**
     * Vykreslí malou kruhovou tečku dané barvy (např. pro označení uživatelské barvy VT/NT v legendě widgetu).
     *
     * @param color  ARGB barva tečky
     * @param sizePx průměr v pixelech
     * @return vykreslená {@link Bitmap}
     */
    @NonNull
    public static Bitmap renderColorDotBitmap(int color, int sizePx) {
        int size = Math.max(12, Math.min(48, sizePx));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        float r = size / 2f;
        canvas.drawCircle(r, r, r - 1f, paint);
        return bitmap;
    }


    /**
     * Vykreslí zaoblenou kartu s výplní a jemným rámečkem (využívá se pro barevné zvýraznění
     * aktivního tarifu VT/NT s ohledem na uživatelské barvy).
     *
     * @param fillColor   barva výplně
     * @param strokeColor barva obrysu
     * @param widthPx     šířka bitmapy
     * @param heightPx    výška bitmapy
     * @param radiusPx    poloměr zaoblení rohů
     * @return vykreslená {@link Bitmap}
     */
    @NonNull
    public static Bitmap renderRoundedBoxBitmap(int fillColor,
                                                int strokeColor,
                                                int widthPx,
                                                int heightPx,
                                                float radiusPx) {
        int w = Math.max(48, Math.min(360, widthPx));
        int h = Math.max(32, Math.min(200, heightPx));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float strokeW = 2f;
        RectF rect = new RectF(strokeW / 2f, strokeW / 2f, w - strokeW / 2f, h - strokeW / 2f);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);
        canvas.drawRoundRect(rect, radiusPx, radiusPx, fillPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeW);
        strokePaint.setColor(strokeColor);
        canvas.drawRoundRect(rect, radiusPx, radiusPx, strokePaint);

        return bitmap;
    }


    /**
     * Vybere dobře čitelnou barvu popředí (bílá nebo tmavá) pro text zobrazený na zadané barvě pozadí.
     *
     * @param backgroundColor ARGB barva pozadí
     * @return bílá ({@code 0xFFFFFFFF}) nebo tmavá ({@code 0xFF191C20}) barva textu
     */
    public static int resolveContrastingTextColor(int backgroundColor) {
        double luminance = ColorUtils.calculateLuminance(backgroundColor);
        return luminance < 0.42 ? 0xFFFFFFFF : 0xFF191C20;
    }


    private static void drawTodayHdoSectors(@NonNull Canvas canvas,
                                            @Nullable List<HdoModel> rawModels,
                                            long timeShiftMillis,
                                            @NonNull Calendar dayCal,
                                            float cx,
                                            float cy,
                                            float radius,
                                            int colorTuv,
                                            int colorTar,
                                            int colorPv) {
        if (rawModels == null || rawModels.isEmpty()) {
            return;
        }
        List<HdoModel> weekModels = BuilderHDOStack.build(new ArrayList<>(rawModels), timeShiftMillis);
        if (weekModels.isEmpty()) {
            return;
        }

        Map<WidgetDataService.HdoRelayType, List<HdoModel>> byType = new EnumMap<>(WidgetDataService.HdoRelayType.class);
        for (WidgetDataService.HdoRelayType type : WidgetDataService.HdoRelayType.values()) {
            byType.put(type, new ArrayList<>());
        }
        for (HdoModel model : weekModels) {
            if (isSameDay(model.getCalendarStart(), dayCal)) {
                WidgetDataService.HdoRelayType type = WidgetDataService.classifyRelayType(model.getRele());
                List<HdoModel> list = byType.get(type);
                if (list != null) {
                    list.add(model);
                }
            }
        }

        Paint paintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSector.setStyle(Paint.Style.FILL);
        RectF arcRect = new RectF();
        int activeTypesCount = 0;

        for (WidgetDataService.HdoRelayType type : WidgetDataService.HdoRelayType.values()) {
            List<HdoModel> modelsForType = byType.get(type);
            if (modelsForType == null || modelsForType.isEmpty()) {
                continue;
            }
            float radiusRatio = Math.max(0.2f, 1.0f - (activeTypesCount * RING_STEP_RATIO));
            activeTypesCount++;
            float sectorRadius = radius * radiusRatio;
            int color = switch (type) {
                case TUV -> colorTuv;
                case TAR -> colorTar;
                case PV -> colorPv;
            };
            paintSector.setColor(color);
            arcRect.set(cx - sectorRadius, cy - sectorRadius, cx + sectorRadius, cy + sectorRadius);

            for (HdoModel model : modelsForType) {
                float startAngle = convertTimeToAngle(model.getTimeFrom());
                float endAngle = convertTimeToAngle(model.getTimeUntil());
                float rawSweep = endAngle - startAngle;
                float sweepAngle = rawSweep <= 0f ? rawSweep + 360f : rawSweep;
                if (sweepAngle > 0.05f) {
                    canvas.drawArc(arcRect, startAngle, sweepAngle, true, paintSector);
                }
            }
        }
    }


    private static boolean isSameDay(@Nullable Calendar c1, @Nullable Calendar c2) {
        if (c1 == null || c2 == null) {
            return false;
        }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }


    private static float convertTimeToAngle(@Nullable String time) {
        if (time == null || !time.contains(":")) {
            return 270f;
        }
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            return 270f + (hours * 15f) + (minutes * 0.25f);
        } catch (NumberFormatException e) {
            return 270f;
        }
    }

}
