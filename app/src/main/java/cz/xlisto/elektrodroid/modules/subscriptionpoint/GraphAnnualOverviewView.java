package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.utils.DensityUtils;
import cz.xlisto.elektrodroid.utils.DetectNightMode;


/**
 * Vlastní View komponenta pro přehledový kombinovaný graf roční spotřeby a průměrné ceny elektřiny.
 */
public class GraphAnnualOverviewView extends View {

    private ArrayList<AnnualYearData> yearDataList = new ArrayList<>();
    private int colorVT = 0xFF2E7D32; // Výchozí tmavě zelená
    private int colorNT = 0xFF512DA8; // Výchozí fialová
    private int colorFixedCost = 0xFF00897B; // Výchozí barva pro tenký sloupec fixních nákladů

    private AnnualGraphColorHelper.LineColors lineColors;

    private Paint paintBarVT;
    private Paint paintBarNT;
    private Paint paintBarFixed;
    private Paint paintBarBorder;
    private Paint paintLineVT;
    private Paint paintLineNT;
    private Paint paintLineTotal;
    private Paint paintMarkerVT;
    private Paint paintMarkerNT;
    private Paint paintMarkerTotal;
    private Paint paintText;
    private Paint paintAxis;
    private Paint paintBadgeBg;
    private Paint paintBgCard;
    private Paint paintLeaderLine;

    private boolean isMWh = true;
    private double maxConsumption = 4.0;
    private double maxPrice = 10000.0;
    private double maxMonthlyFixed = 500.0;
    private boolean isDualTariff = true;

    private final RectF chartArea = new RectF();

    // Předalokované objekty pro zabránění alokací během vykreslování (onDraw)
    private final RectF bgCardRect = new RectF();
    private final Path pathVT = new Path();
    private final Path pathNT = new Path();
    private final Path pathTotal = new Path();
    private final Path pathDiamond = new Path();

    private float[] columnXCenters = new float[0];
    private float[] markerXCoords = new float[0];
    private float[] calloutXCoords = new float[0];
    private float[] consTextXCoords = new float[0];
    private RectF[] vtBarRects = new RectF[0];
    private RectF[] ntBarRects = new RectF[0];
    private RectF[] fixedBarRects = new RectF[0];
    private boolean[] hasDataYear = new boolean[0];
    private float[] yPtsVT = new float[0];
    private float[] yPtsNT = new float[0];
    private float[] yPtsTotal = new float[0];

    private final ArrayList<GraphCallout> leftCallouts = new ArrayList<>();
    private final ArrayList<GraphCallout> rightCallouts = new ArrayList<>();
    private final ArrayList<GraphCallout> calloutPool = new ArrayList<>();
    private int calloutPoolIndex = 0;

    private final ArrayList<RectF> placedTextDigitsRects = new ArrayList<>();
    private final ArrayList<RectF> rectPool = new ArrayList<>();
    private int rectPoolIndex = 0;

    private final RectF tempBoxRect = new RectF();


    private static class GraphCallout {

        String text;
        float targetY;
        float currentY;
        int accentColor;
        boolean isBold;
        boolean isSmall;


        GraphCallout(String text, float targetY, int accentColor, boolean isBold, boolean isSmall) {
            reset(text, targetY, accentColor, isBold, isSmall);
        }


        void reset(String text, float targetY, int accentColor, boolean isBold, boolean isSmall) {
            this.text = text;
            this.targetY = targetY;
            this.currentY = targetY;
            this.accentColor = accentColor;
            this.isBold = isBold;
            this.isSmall = isSmall;
        }

    }


    public GraphAnnualOverviewView(Context context) {
        super(context);
        init(context);
    }


    public GraphAnnualOverviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public GraphAnnualOverviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        // Načteme nastavené barvy z DataSettingsSource
        DataSettingsSource settingsSource = new DataSettingsSource(context);
        settingsSource.open();
        try {
            int[] colors = settingsSource.loadColorVTNT();
            if (colors != null && colors.length >= 2) {
                colorVT = colors[0];
                colorNT = colors[1];
            }
        } catch (Exception e) {
            Log.e("GraphAnnualOverviewView", "Chyba při načítání barev VT/NT: " + e.getMessage());
        } finally {
            settingsSource.close();
        }

        boolean isDark = DetectNightMode.isNightMode(context);
        int bgColor = isDark ? 0xFF1E1E1E : 0xFFF5F5F5;

        colorFixedCost = isDark ? 0xFF26A69A : 0xFF00796B; // Tyrkysově-zelená pro tenký sloupec fixních nákladů

        lineColors = AnnualGraphColorHelper.selectLineColors(context, colorVT, colorNT, bgColor);

        paintBarVT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarVT.setColor(colorVT);
        paintBarVT.setStyle(Paint.Style.FILL);

        paintBarNT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarNT.setColor(colorNT);
        paintBarNT.setStyle(Paint.Style.FILL);

        paintBarFixed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarFixed.setColor(colorFixedCost);
        paintBarFixed.setStyle(Paint.Style.FILL);

        paintBarBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarBorder.setStyle(Paint.Style.STROKE);
        paintBarBorder.setStrokeWidth(DensityUtils.dpToPx(context, 1f));
        paintBarBorder.setColor(isDark ? 0xFF888888 : 0xFF757575);

        // Ztenčení cenových čar o cca 30–40 % pro jemnější technický vzhled
        paintLineVT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineVT.setColor(lineColors.colorPriceVT());
        paintLineVT.setStrokeWidth(DensityUtils.dpToPx(context, 1.4f));
        paintLineVT.setStyle(Paint.Style.STROKE);

        paintLineNT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineNT.setColor(lineColors.colorPriceNT());
        paintLineNT.setStrokeWidth(DensityUtils.dpToPx(context, 1.4f));
        paintLineNT.setStyle(Paint.Style.STROKE);
        paintLineNT.setPathEffect(new DashPathEffect(new float[]{DensityUtils.dpToPx(context, 8), DensityUtils.dpToPx(context, 6)}, 0));

        paintLineTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineTotal.setColor(lineColors.colorPriceTotal());
        paintLineTotal.setStrokeWidth(DensityUtils.dpToPx(context, 2.2f));
        paintLineTotal.setStyle(Paint.Style.STROKE);

        paintMarkerVT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarkerVT.setColor(lineColors.colorPriceVT());
        paintMarkerVT.setStyle(Paint.Style.FILL);

        paintMarkerNT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarkerNT.setColor(lineColors.colorPriceNT());
        paintMarkerNT.setStyle(Paint.Style.STROKE);
        paintMarkerNT.setStrokeWidth(DensityUtils.dpToPx(context, 1.8f));

        paintMarkerTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarkerTotal.setColor(lineColors.colorPriceTotal());
        paintMarkerTotal.setStyle(Paint.Style.FILL);

        int textColor = isDark ? 0xFFEEEEEE : 0xFF212121;
        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(textColor);
        paintText.setTextSize(DensityUtils.dpToPx(context, 11));

        paintAxis = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAxis.setColor(isDark ? 0xFF666666 : 0xFFCCCCCC);
        paintAxis.setStrokeWidth(DensityUtils.dpToPx(context, 1));

        paintBadgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBadgeBg.setStyle(Paint.Style.FILL);

        paintBgCard = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBgCard.setColor(bgColor);
        paintBgCard.setStyle(Paint.Style.FILL);

        paintLeaderLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLeaderLine.setColor(isDark ? 0xFF888888 : 0xFFAAAAAA);
        paintLeaderLine.setStrokeWidth(DensityUtils.dpToPx(context, 1));
        paintLeaderLine.setStyle(Paint.Style.STROKE);
    }


    public void setData(ArrayList<AnnualYearData> data) {
        if (data == null) {
            this.yearDataList.clear();
        } else {
            this.yearDataList = new ArrayList<>(data);
        }

        recalculateScales();
        invalidate();
    }


    private void recalculateScales() {
        if (yearDataList.isEmpty()) {
            return;
        }

        double maxConsKwh = 0;
        double maxPriceVal = 0;
        double maxFixedVal = 0;
        boolean hasNT = false;

        for (AnnualYearData d : yearDataList) {
            double totalKwh = d.getTotalConsumption();
            if (totalKwh > maxConsKwh) {
                maxConsKwh = totalKwh;
            }
            if (d.consumptionNT() > 0) {
                hasNT = true;
            }

            maxPriceVal = Math.max(maxPriceVal, d.avgPriceTotal());
            maxPriceVal = Math.max(maxPriceVal, d.avgPriceVT());
            if (hasNT) {
                maxPriceVal = Math.max(maxPriceVal, d.avgPriceNT());
            }

            maxFixedVal = Math.max(maxFixedVal, d.avgMonthlyFixedCost());
        }

        isDualTariff = hasNT;

        // Pokud je max kwh < 1000, použijeme kWh, jinak MWh
        isMWh = maxConsKwh >= 1000.0;

        // Nastavíme rozsah osy Y pro spotřebu
        double maxVal = isMWh ? (maxConsKwh / 1000.0) : maxConsKwh;
        if (maxVal <= 0) maxVal = 1.0;
        maxConsumption = Math.ceil(maxVal * 1.15); // Add 15% margin

        // Nastavíme rozsah osy Y pro cenu
        if (maxPriceVal <= 0) maxPriceVal = 5000;
        maxPrice = Math.ceil((maxPriceVal * 1.15) / 1000.0) * 1000.0;
        if (maxPrice < 2000) maxPrice = 2000;

        // Nastavíme rozsah pro fixní měsíční náklady
        if (maxFixedVal <= 0) maxFixedVal = 200;
        maxMonthlyFixed = Math.ceil(maxFixedVal * 1.25);
    }


    private boolean needsOutlineBorder(int color, int cardBgColor) {
        double contrast = ColorUtils.calculateContrast(color, cardBgColor);
        return contrast < 1.6; // Pokud barva splývá s pozadím (kontrast < 1.6:1), zvýrazníme obrys
    }


    private void ensureArraysSized(int count) {
        if (columnXCenters.length < count) {
            columnXCenters = new float[count];
            markerXCoords = new float[count];
            calloutXCoords = new float[count];
            consTextXCoords = new float[count];
            vtBarRects = new RectF[count];
            ntBarRects = new RectF[count];
            fixedBarRects = new RectF[count];
            hasDataYear = new boolean[count];
            yPtsVT = new float[count];
            yPtsNT = new float[count];
            yPtsTotal = new float[count];

            for (int i = 0; i < count; i++) {
                vtBarRects[i] = new RectF();
                ntBarRects[i] = new RectF();
                fixedBarRects[i] = new RectF();
            }
        }
    }


    private GraphCallout obtainGraphCallout(String text, float targetY, int accentColor, boolean isBold, boolean isSmall) {
        if (calloutPoolIndex < calloutPool.size()) {
            GraphCallout c = calloutPool.get(calloutPoolIndex++);
            c.reset(text, targetY, accentColor, isBold, isSmall);
            return c;
        } else {
            GraphCallout c = new GraphCallout(text, targetY, accentColor, isBold, isSmall);
            calloutPool.add(c);
            calloutPoolIndex++;
            return c;
        }
    }


    private RectF obtainRectF(float left, float top, float right, float bottom) {
        if (rectPoolIndex < rectPool.size()) {
            RectF r = rectPool.get(rectPoolIndex++);
            r.set(left, top, right, bottom);
            return r;
        } else {
            RectF r = new RectF(left, top, right, bottom);
            rectPool.add(r);
            rectPoolIndex++;
            return r;
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = DensityUtils.dpToPx(getContext(), 280);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Resetování čítačů fondu objektů pro zamezení novým alokacím během onDraw
        calloutPoolIndex = 0;
        rectPoolIndex = 0;
        placedTextDigitsRects.clear();
        leftCallouts.clear();
        rightCallouts.clear();

        pathVT.reset();
        pathNT.reset();
        pathTotal.reset();
        pathDiamond.reset();

        boolean isDark = DetectNightMode.isNightMode(getContext());
        int cardBgColor = isDark ? 0xFF1E1E1E : 0xFFF5F5F5;

        // Nakreslit zaoblený rámeček grafu (shodný s poloměrem 5dp a šířkou jako u kartiček shape_item)
        float rx = DensityUtils.dpToPx(getContext(), 5);
        bgCardRect.set(0, 0, width, height);
        canvas.drawRoundRect(bgCardRect, rx, rx, paintBgCard);

        if (yearDataList == null || yearDataList.isEmpty()) {
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getContext().getString(R.string.no_data_available), width / 2f, height / 2f, paintText);
            return;
        }

        // Okraje grafu
        float padLeft = DensityUtils.dpToPx(getContext(), 40);
        float padRight = DensityUtils.dpToPx(getContext(), 52);
        float padTop = DensityUtils.dpToPx(getContext(), 42); // Horní okraj pro celkovou spotřebu v jedné řadě
        float padBottom = DensityUtils.dpToPx(getContext(), 80);

        chartArea.set(padLeft, padTop, width - padRight, height - padBottom);

        // Názvy jednotek VÝŠE nad mřížkovým polem grafu
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setFakeBoldText(true);
        canvas.drawText(isMWh ? "MWh" : "kWh", padLeft, padTop - DensityUtils.dpToPx(getContext(), 20), paintText);

        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Kč/MWh", width - padRight, padTop - DensityUtils.dpToPx(getContext(), 20), paintText);
        paintText.setFakeBoldText(false);

        // Mřížka a popisky os Y
        int steps = 4;
        Locale locale = getContext().getResources().getConfiguration().getLocales().get(0);
        float axisGap = DensityUtils.dpToPx(getContext(), 10); // Odstup čísel osy od svislé čáry osy Y

        for (int i = 0; i <= steps; i++) {
            float y = chartArea.bottom - (chartArea.height() * i / steps);

            // Mřížková čára
            canvas.drawLine(chartArea.left, y, chartArea.right, y, paintAxis);

            float textY = (i == steps) ? (y + DensityUtils.dpToPx(getContext(), 10)) : (y + 4);

            // Leva osa - spotřeba
            double consVal = (maxConsumption * i / steps);
            paintText.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(DecimalFormatHelper.df1.format(consVal), chartArea.left - axisGap, textY, paintText);

            // Prava osa - cena
            double priceVal = (maxPrice * i / steps);
            paintText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(String.format(locale, "%.0f", priceVal), chartArea.right + axisGap, textY, paintText);
        }

        // Osy X a Y
        canvas.drawLine(chartArea.left, chartArea.bottom, chartArea.right, chartArea.bottom, paintAxis);

        int count = yearDataList.size();
        ensureArraysSized(count);

        float slotWidth = chartArea.width() / count;
        float wMain = DensityUtils.dpToPx(getContext(), 11);  // Hlavní sloupec spotřeby (11dp)
        float wFixed = DensityUtils.dpToPx(getContext(), 4);   // Tenký sloupec fixních nákladů (4dp)
        float gap = DensityUtils.dpToPx(getContext(), 2.5f);  // Mezera mezi sloupci

        boolean borderVT = needsOutlineBorder(colorVT, cardBgColor);
        boolean borderNT = needsOutlineBorder(colorNT, cardBgColor);
        boolean borderFixed = needsOutlineBorder(colorFixedCost, cardBgColor);

        // 1. Vykreslení sloupců spotřeby a tenkého sloupce fixních nákladů
        for (int i = 0; i < count; i++) {
            AnnualYearData data = yearDataList.get(i);
            hasDataYear[i] = (data.getTotalConsumption() > 0 || data.fixedCostTotal() > 0 || data.avgPriceTotal() > 0);

            float slotLeft = chartArea.left + i * slotWidth;

            // Vyhradíme levý prostor slotu pro hodnoty spotřeby
            float leftMain = slotLeft + DensityUtils.dpToPx(getContext(), 16);
            float rightMain = leftMain + wMain;
            float leftFixed = rightMain + gap;
            float rightFixed = leftFixed + wFixed;

            // Střed hlavního sloupce pro polohu datového bodu (markeru)
            float cxMarker = (leftMain + rightMain) / 2f;
            markerXCoords[i] = cxMarker;

            // X Pozice pro levý okraj pravých cenových rámečků
            float xCallout = rightFixed + DensityUtils.dpToPx(getContext(), 2.5f);
            calloutXCoords[i] = xCallout;

            // X Pozice pro pravý okraj levých popisků spotřeby VT a NT
            float xConsText = leftMain - DensityUtils.dpToPx(getContext(), 2.5f);
            consTextXCoords[i] = xConsText;

            // Střed celé roční skupiny pro popisek roku na ose X
            float cxYearGroup = slotLeft + slotWidth / 2f;
            columnXCenters[i] = cxYearGroup;

            double vtVal = isMWh ? (data.consumptionVT() / 1000.0) : data.consumptionVT();
            double ntVal = isMWh ? (data.consumptionNT() / 1000.0) : data.consumptionNT();

            float hVT = (float) (chartArea.height() * (vtVal / maxConsumption));
            float hNT = isDualTariff ? (float) (chartArea.height() * (ntVal / maxConsumption)) : 0;

            float yBottom = chartArea.bottom;
            float yVtTop = yBottom - hVT;
            float yNtTop = yVtTop - hNT;

            // Sloupce kreslíme pouze pro roky, které mají data
            if (hasDataYear[i]) {
                // Průhlednost pro aktuální rok
                int alphaVT = paintBarVT.getAlpha();
                int alphaNT = paintBarNT.getAlpha();
                int alphaFixed = paintBarFixed.getAlpha();

                if (data.isCurrentYear()) {
                    paintBarVT.setAlpha(180);
                    paintBarNT.setAlpha(180);
                    paintBarFixed.setAlpha(180);
                }

                // Sloupec VT
                RectF rectVT = vtBarRects[i];
                rectVT.set(leftMain, yVtTop, rightMain, yBottom);
                canvas.drawRect(rectVT, paintBarVT);
                if (borderVT) {
                    canvas.drawRect(rectVT, paintBarBorder);
                }

                // Sloupec NT (pokud je dvoutarif)
                if (isDualTariff && hNT > 0) {
                    RectF rectNT = ntBarRects[i];
                    rectNT.set(leftMain, yNtTop, rightMain, yVtTop);
                    canvas.drawRect(rectNT, paintBarNT);
                    if (borderNT) {
                        canvas.drawRect(rectNT, paintBarBorder);
                    }
                }

                // Tenký sloupec fixních nákladů (v Kč/měs)
                float hFixed = (float) (chartArea.height() * 0.65f * (data.avgMonthlyFixedCost() / maxMonthlyFixed));
                if (hFixed < DensityUtils.dpToPx(getContext(), 4)) {
                    hFixed = DensityUtils.dpToPx(getContext(), 4);
                }
                float yFixedTop = yBottom - hFixed;
                RectF rectFixed = fixedBarRects[i];
                rectFixed.set(leftFixed, yFixedTop, rightFixed, yBottom);
                canvas.drawRect(rectFixed, paintBarFixed);
                if (borderFixed) {
                    canvas.drawRect(rectFixed, paintBarBorder);
                }

                // Obnovení alpha
                paintBarVT.setAlpha(alphaVT);
                paintBarNT.setAlpha(alphaNT);
                paintBarFixed.setAlpha(alphaFixed);
            }

            // Popisek roku pod osou X (vycentrován vůči celé skupině roku) – kreslí se VŽDY pro všech 4 let!
            int defaultTextColor = isDark ? 0xFFEEEEEE : 0xFF212121;
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(defaultTextColor);

            String yearText = String.valueOf(data.year());
            if (data.isEstimated() && hasDataYear[i]) {
                yearText += "*";
            }
            canvas.drawText(yearText, cxYearGroup, chartArea.bottom + DensityUtils.dpToPx(getContext(), 16), paintText);

            if (data.isCurrentYear() && data.currentYearRangeLabel() != null) {
                paintText.setTextSize(DensityUtils.dpToPx(getContext(), 9));
                paintText.setColor(0xFF888888);
                canvas.drawText(data.currentYearRangeLabel(), cxYearGroup, chartArea.bottom + DensityUtils.dpToPx(getContext(), 28), paintText);
                paintText.setTextSize(DensityUtils.dpToPx(getContext(), 11));
                paintText.setColor(defaultTextColor);
            }
        }

        // 2. Vykreslení čar průměrné ceny (pouze pro roky s daty)
        for (int i = 0; i < count; i++) {
            if (!hasDataYear[i]) continue;
            AnnualYearData data = yearDataList.get(i);

            yPtsVT[i] = (float) (chartArea.bottom - (chartArea.height() * (data.avgPriceVT() / maxPrice)));
            yPtsNT[i] = (float) (chartArea.bottom - (chartArea.height() * (data.avgPriceNT() / maxPrice)));
            yPtsTotal[i] = (float) (chartArea.bottom - (chartArea.height() * (data.avgPriceTotal() / maxPrice)));
        }

        boolean firstVT = true, firstNT = true, firstTotal = true;
        for (int i = 0; i < count; i++) {
            if (!hasDataYear[i]) continue;
            float mx = markerXCoords[i];

            if (firstVT) {
                pathVT.moveTo(mx, yPtsVT[i]);
                firstVT = false;
            } else {
                pathVT.lineTo(mx, yPtsVT[i]);
            }

            if (firstNT) {
                pathNT.moveTo(mx, yPtsNT[i]);
                firstNT = false;
            } else {
                pathNT.lineTo(mx, yPtsNT[i]);
            }

            if (firstTotal) {
                pathTotal.moveTo(mx, yPtsTotal[i]);
                firstTotal = false;
            } else {
                pathTotal.lineTo(mx, yPtsTotal[i]);
            }
        }

        if (isDualTariff) {
            canvas.drawPath(pathVT, paintLineVT);
            canvas.drawPath(pathNT, paintLineNT);
        }
        canvas.drawPath(pathTotal, paintLineTotal);

        // 3. Vykreslení datových bodů (markerů) pouze pro roky s daty
        float markerRadius = DensityUtils.dpToPx(getContext(), 4.5f);

        for (int i = 0; i < count; i++) {
            if (!hasDataYear[i]) continue;
            float mx = markerXCoords[i];

            if (isDualTariff) {
                // Cena VT marker
                canvas.drawCircle(mx, yPtsVT[i], markerRadius, paintMarkerVT);

                // Cena NT marker (prázdné kolečko)
                canvas.drawCircle(mx, yPtsNT[i], markerRadius, paintMarkerNT);
            }

            // Cena Celkem marker (kosočtverec)
            pathDiamond.reset();
            pathDiamond.moveTo(mx, yPtsTotal[i] - markerRadius * 1.2f);
            pathDiamond.lineTo(mx + markerRadius * 1.2f, yPtsTotal[i]);
            pathDiamond.lineTo(mx, yPtsTotal[i] + markerRadius * 1.2f);
            pathDiamond.lineTo(mx - markerRadius * 1.2f, yPtsTotal[i]);
            pathDiamond.close();
            canvas.drawPath(pathDiamond, paintMarkerTotal);
        }

        // 4. VYKRESLENÍ VŠECH 7 VÝSLEDNÝCH RÁMEČKŮ S GLOBÁLNÍ OCHRANOU ČÍSEL PROTI PŘEKRYTÍ (pouze pro roky s daty)
        float minCalloutGap = DensityUtils.dpToPx(getContext(), 14);
        float topCalloutLimit = chartArea.top + DensityUtils.dpToPx(getContext(), 4);
        float bottomCalloutLimit = chartArea.bottom - DensityUtils.dpToPx(getContext(), 4);

        int defaultTextColor = isDark ? 0xFFEEEEEE : 0xFF212121;

        for (int i = 0; i < count; i++) {
            if (!hasDataYear[i]) continue;

            AnnualYearData data = yearDataList.get(i);
            float cxMarker = markerXCoords[i];
            float xCallout = calloutXCoords[i];
            float xConsText = consTextXCoords[i];

            double vtVal = isMWh ? (data.consumptionVT() / 1000.0) : data.consumptionVT();
            double ntVal = isMWh ? (data.consumptionNT() / 1000.0) : data.consumptionNT();
            double totalVal = vtVal + ntVal;

            RectF rectVT = vtBarRects[i];
            RectF rectNT = ntBarRects[i];

            float yBottom = chartArea.bottom;
            float yVtTop = rectVT != null ? rectVT.top : yBottom;
            float yNtTop = rectNT != null ? rectNT.top : yVtTop;

            // A) Celková spotřeba - umístěna do horní části nad sloupci v jedné čisté horizontální řadě
            float yTotalCons = chartArea.top + DensityUtils.dpToPx(getContext(), 8);
            RectF textRectTotal = drawCalloutBox(canvas, cxMarker, yTotalCons, DecimalFormatHelper.df1.format(totalVal), defaultTextColor, true, false, true, placedTextDigitsRects);
            placedTextDigitsRects.add(textRectTotal);

            // B) Levé rámečky (Spotřeba VT a Spotřeba NT)
            leftCallouts.clear();

            // VT spotřeba
            float prefY_VT = (yVtTop + yBottom) / 2f + DensityUtils.dpToPx(getContext(), 3.5f);
            leftCallouts.add(obtainGraphCallout(DecimalFormatHelper.df1.format(vtVal), prefY_VT, colorVT, false, false));

            // NT spotřeba (pokud je dvoutarif)
            if (isDualTariff && ntVal > 0) {
                float prefY_NT = (yNtTop + yVtTop) / 2f + DensityUtils.dpToPx(getContext(), 3.5f);
                leftCallouts.add(obtainGraphCallout(DecimalFormatHelper.df1.format(ntVal), prefY_NT, colorNT, false, false));
            }

            // Seřadíme levé rámečky spotřeby podle Y pozice
            leftCallouts.sort(Comparator.comparingDouble(c -> c.targetY));

            // Rozvolnění vertikálních přesahů na levé straně
            for (int pass = 0; pass < 3; pass++) {
                for (int k = 0; k < leftCallouts.size() - 1; k++) {
                    GraphCallout c1 = leftCallouts.get(k);
                    GraphCallout c2 = leftCallouts.get(k + 1);
                    float diff = c2.currentY - c1.currentY;
                    if (diff < minCalloutGap) {
                        float shift = (minCalloutGap - diff) / 2f;
                        c1.currentY -= shift;
                        c2.currentY += shift;
                    }
                }
            }

            for (GraphCallout c : leftCallouts) {
                if (c.currentY < topCalloutLimit + 14) c.currentY = topCalloutLimit + 14;
                if (c.currentY > bottomCalloutLimit) c.currentY = bottomCalloutLimit;

                // Kontrola kolize s číslicemi předchozích rámečků a případný jemný vertikální posun
                RectF digitsRect = drawCalloutBoxRightAligned(canvas, xConsText, c.currentY, c.text, c.accentColor, c.isBold, c.isSmall, placedTextDigitsRects);
                placedTextDigitsRects.add(digitsRect);
            }

            // C) Pravé rámečky (Cena celkem, Cena VT, Cena NT, Fixní náklady)
            rightCallouts.clear();
            if (isDualTariff) {
                rightCallouts.add(obtainGraphCallout(String.format(locale, "%.0f", data.avgPriceVT()), yPtsVT[i], lineColors.colorPriceVT(), true, false));
                rightCallouts.add(obtainGraphCallout(String.format(locale, "%.0f", data.avgPriceNT()), yPtsNT[i], lineColors.colorPriceNT(), true, false));
            }
            rightCallouts.add(obtainGraphCallout(String.format(locale, "%.0f", data.avgPriceTotal()), yPtsTotal[i], lineColors.colorPriceTotal(), true, false));

            // Fixní měsíční náklady umístěné POD cenovými kótami
            float yFixedVal = (float) (chartArea.bottom - (chartArea.height() * 0.65f * (data.avgMonthlyFixedCost() / maxMonthlyFixed)));
            rightCallouts.add(obtainGraphCallout(String.format(locale, "%.0f Kč", data.avgMonthlyFixedCost()), yFixedVal, colorFixedCost, false, true));

            // Seřadíme pravé rámečky cen podle Y pozice
            rightCallouts.sort(Comparator.comparingDouble(c -> c.targetY));

            // Rozvolnění vertikálních přesahů na pravé straně
            for (int pass = 0; pass < 5; pass++) {
                for (int k = 0; k < rightCallouts.size() - 1; k++) {
                    GraphCallout c1 = rightCallouts.get(k);
                    GraphCallout c2 = rightCallouts.get(k + 1);
                    float diff = c2.currentY - c1.currentY;
                    if (diff < minCalloutGap) {
                        float shift = (minCalloutGap - diff) / 2f;
                        c1.currentY -= shift;
                        c2.currentY += shift;
                    }
                }
            }

            for (GraphCallout c : rightCallouts) {
                if (c.currentY < topCalloutLimit + 14) c.currentY = topCalloutLimit + 14;
                if (c.currentY > bottomCalloutLimit) c.currentY = bottomCalloutLimit;

                // Spojovací čárka, pokud byl pravý rámeček výrazně posunut od datového bodu
                if (!c.isSmall && Math.abs(c.currentY - c.targetY) > DensityUtils.dpToPx(getContext(), 8)) {
                    canvas.drawLine(cxMarker, c.targetY, xCallout, c.currentY, paintLeaderLine);
                }
                RectF digitsRect = drawCalloutBox(canvas, xCallout, c.currentY, c.text, c.accentColor, c.isBold, c.isSmall, false, placedTextDigitsRects);
                placedTextDigitsRects.add(digitsRect);
            }
        }

        // 5. Legenda pod grafem
        drawLegend(canvas, height);
    }


    private RectF drawCalloutBox(Canvas canvas, float leftX, float cy, String text, int accentColor, boolean isBold, boolean isSmall, boolean isCentered, ArrayList<RectF> existingDigitsRects) {
        if (isSmall) {
            paintText.setTextSize(DensityUtils.dpToPx(getContext(), 8.5f));
            paintText.setFakeBoldText(false);
        } else {
            paintText.setTextSize(DensityUtils.dpToPx(getContext(), 9.5f));
            paintText.setFakeBoldText(isBold);
        }

        Paint.FontMetrics fontMetrics = paintText.getFontMetrics();
        float textWidth = paintText.measureText(text);
        float padH = DensityUtils.dpToPx(getContext(), 2.5f);
        float padV = DensityUtils.dpToPx(getContext(), 1.5f);

        float drawY = cy;
        float rectLeft = isCentered ? (leftX - textWidth / 2f - padH) : leftX;
        float rectRight = isCentered ? (leftX + textWidth / 2f + padH) : (leftX + textWidth + 2 * padH);

        // Kontrola kolize s číslicemi předchozích rámečků
        if (existingDigitsRects != null) {
            float shiftStep = DensityUtils.dpToPx(getContext(), 6);
            for (int pass = 0; pass < 4; pass++) {
                float rectTop = drawY + fontMetrics.ascent - padV;
                float rectBottom = drawY + fontMetrics.descent + padV;

                tempBoxRect.set(rectLeft, rectTop, rectRight, rectBottom);
                boolean collidesWithDigits = false;
                for (RectF prevDigits : existingDigitsRects) {
                    if (RectF.intersects(tempBoxRect, prevDigits)) {
                        collidesWithDigits = true;
                        break;
                    }
                }

                if (!collidesWithDigits) {
                    break;
                }
                drawY += (pass % 2 == 0) ? shiftStep * (pass + 1) : -shiftStep * (pass + 1);
            }
        }

        float rectTop = drawY + fontMetrics.ascent - padV;
        float rectBottom = drawY + fontMetrics.descent + padV;

        tempBoxRect.set(rectLeft, rectTop, rectRight, rectBottom);

        boolean isDark = DetectNightMode.isNightMode(getContext());
        int boxBgColor = isDark ? 0xFF1E1E1E : 0xFFFFFFFF;

        int strokeColor = accentColor;
        int textColor = accentColor;

        if (needsOutlineBorder(accentColor, boxBgColor)) {
            strokeColor = isDark ? 0xFF888888 : 0xFF757575;
            textColor = isDark ? 0xFFEEEEEE : 0xFF212121;
        }

        // 1. Plně neprůhledná výplň pozadí rámečku
        paintBadgeBg.setColor(boxBgColor);
        paintBadgeBg.setAlpha(255);
        paintBadgeBg.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(tempBoxRect, 3, 3, paintBadgeBg);

        // 2. Jemný okraj v barvě akcentu (nebo kontrastní barvě při splynutí)
        paintBadgeBg.setColor(strokeColor);
        paintBadgeBg.setAlpha(255);
        paintBadgeBg.setStyle(Paint.Style.STROKE);
        paintBadgeBg.setStrokeWidth(DensityUtils.dpToPx(getContext(), isSmall ? 0.8f : 1.1f));
        canvas.drawRoundRect(tempBoxRect, 3, 3, paintBadgeBg);

        // 3. Kontrastní text
        paintText.setColor(textColor);
        if (isCentered) {
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, leftX, drawY, paintText);
        } else {
            paintText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(text, leftX + padH, drawY, paintText);
        }
        paintText.setFakeBoldText(false);

        // Obnovení výchozí velikosti písma
        paintText.setTextSize(DensityUtils.dpToPx(getContext(), 11));

        // Vrátíme zarezervovanou plochu samotných číslic bez vnějšího paddingu z fondu objektů
        return obtainRectF(
                isCentered ? (leftX - textWidth / 2f) : (leftX + padH),
                drawY + fontMetrics.ascent,
                isCentered ? (leftX + textWidth / 2f) : (leftX + padH + textWidth),
                drawY + fontMetrics.descent
        );
    }


    private RectF drawCalloutBoxRightAligned(Canvas canvas, float rightX, float cy, String text, int accentColor, boolean isBold, boolean isSmall, ArrayList<RectF> existingDigitsRects) {
        if (isSmall) {
            paintText.setTextSize(DensityUtils.dpToPx(getContext(), 8.5f));
            paintText.setFakeBoldText(false);
        } else {
            paintText.setTextSize(DensityUtils.dpToPx(getContext(), 9.5f));
            paintText.setFakeBoldText(isBold);
        }

        Paint.FontMetrics fontMetrics = paintText.getFontMetrics();
        float textWidth = paintText.measureText(text);
        float padH = DensityUtils.dpToPx(getContext(), 2.5f);
        float padV = DensityUtils.dpToPx(getContext(), 1.5f);

        float drawY = cy;
        float rectLeft = rightX - textWidth - 2 * padH;

        // Kontrola kolize s číslicemi předchozích rámečků
        if (existingDigitsRects != null) {
            float shiftStep = DensityUtils.dpToPx(getContext(), 6);
            for (int pass = 0; pass < 4; pass++) {
                float rectTop = drawY + fontMetrics.ascent - padV;
                float rectBottom = drawY + fontMetrics.descent + padV;

                tempBoxRect.set(rectLeft, rectTop, rightX, rectBottom);
                boolean collidesWithDigits = false;
                for (RectF prevDigits : existingDigitsRects) {
                    if (RectF.intersects(tempBoxRect, prevDigits)) {
                        collidesWithDigits = true;
                        break;
                    }
                }

                if (!collidesWithDigits) {
                    break;
                }
                drawY += (pass % 2 == 0) ? shiftStep * (pass + 1) : -shiftStep * (pass + 1);
            }
        }

        float rectTop = drawY + fontMetrics.ascent - padV;
        float rectBottom = drawY + fontMetrics.descent + padV;

        tempBoxRect.set(rectLeft, rectTop, rightX, rectBottom);

        boolean isDark = DetectNightMode.isNightMode(getContext());
        int boxBgColor = isDark ? 0xFF1E1E1E : 0xFFFFFFFF;

        int strokeColor = accentColor;
        int textColor = accentColor;

        if (needsOutlineBorder(accentColor, boxBgColor)) {
            strokeColor = isDark ? 0xFF888888 : 0xFF757575;
            textColor = isDark ? 0xFFEEEEEE : 0xFF212121;
        }

        // 1. Plně neprůhledná výplň pozadí rámečku
        paintBadgeBg.setColor(boxBgColor);
        paintBadgeBg.setAlpha(255);
        paintBadgeBg.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(tempBoxRect, 3, 3, paintBadgeBg);

        // 2. Jemný okraj v barvě akcentu (nebo kontrastní barvě při splynutí)
        paintBadgeBg.setColor(strokeColor);
        paintBadgeBg.setAlpha(255);
        paintBadgeBg.setStyle(Paint.Style.STROKE);
        paintBadgeBg.setStrokeWidth(DensityUtils.dpToPx(getContext(), isSmall ? 0.8f : 1.1f));
        canvas.drawRoundRect(tempBoxRect, 3, 3, paintBadgeBg);

        // 3. Kontrastní text
        paintText.setColor(textColor);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(text, rightX - padH, drawY, paintText);
        paintText.setFakeBoldText(false);

        // Obnovení výchozí velikosti písma
        paintText.setTextSize(DensityUtils.dpToPx(getContext(), 11));

        // Vrátíme zarezervovanou plochu samotných číslic bez vnějšího paddingu z fondu objektů
        return obtainRectF(
                rightX - padH - textWidth,
                drawY + fontMetrics.ascent,
                rightX - padH,
                drawY + fontMetrics.descent
        );
    }


    private void drawLegend(Canvas canvas, int height) {
        float row1Y = height - DensityUtils.dpToPx(getContext(), 34);
        float row2Y = height - DensityUtils.dpToPx(getContext(), 14);
        float startX = DensityUtils.dpToPx(getContext(), 8);
        float totalW = getWidth() - DensityUtils.dpToPx(getContext(), 16);
        int numCols = isDualTariff ? 3 : 2;
        float colW = totalW / numCols;

        float boxSize = DensityUtils.dpToPx(getContext(), 9);
        float sampleLength = DensityUtils.dpToPx(getContext(), 26);
        float markerR = DensityUtils.dpToPx(getContext(), 3.5f);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(DensityUtils.dpToPx(getContext(), 9.5f));
        boolean isDark = DetectNightMode.isNightMode(getContext());
        int cardBgColor = isDark ? 0xFF1E1E1E : 0xFFF5F5F5;
        int defaultTextColor = isDark ? 0xFFEEEEEE : 0xFF212121;
        paintText.setColor(defaultTextColor);

        // --- Řada 1: Sloupce (Sloupec 0: VT, Sloupec 1: NT, Sloupec 2: Fixní) ---
        float c0X = startX + 0 * colW;
        RectF rVT = obtainRectF(c0X, row1Y - boxSize + 2, c0X + boxSize, row1Y + 2);
        canvas.drawRect(rVT, paintBarVT);
        if (needsOutlineBorder(colorVT, cardBgColor)) {
            canvas.drawRect(rVT, paintBarBorder);
        }
        String labelVT = getContext().getString(R.string.graph_legend_vt, isMWh ? "MWh" : "kWh");
        canvas.drawText(labelVT, c0X + boxSize + 4, row1Y, paintText);

        if (isDualTariff) {
            float c1X = startX + 1 * colW;
            RectF rNT = obtainRectF(c1X, row1Y - boxSize + 2, c1X + boxSize, row1Y + 2);
            canvas.drawRect(rNT, paintBarNT);
            if (needsOutlineBorder(colorNT, cardBgColor)) {
                canvas.drawRect(rNT, paintBarBorder);
            }
            String labelNT = getContext().getString(R.string.graph_legend_nt, isMWh ? "MWh" : "kWh");
            canvas.drawText(labelNT, c1X + boxSize + 4, row1Y, paintText);

            float c2X = startX + 2 * colW;
            RectF rFixed = obtainRectF(c2X, row1Y - boxSize + 2, c2X + (boxSize * 0.5f), row1Y + 2);
            canvas.drawRect(rFixed, paintBarFixed);
            if (needsOutlineBorder(colorFixedCost, cardBgColor)) {
                canvas.drawRect(rFixed, paintBarBorder);
            }
            String labelFixed = getContext().getString(R.string.graph_legend_fixed_cost);
            canvas.drawText(labelFixed, c2X + (boxSize * 0.5f) + 4, row1Y, paintText);
        } else {
            float c1X = startX + 1 * colW;
            RectF rFixed = obtainRectF(c1X, row1Y - boxSize + 2, c1X + (boxSize * 0.5f), row1Y + 2);
            canvas.drawRect(rFixed, paintBarFixed);
            if (needsOutlineBorder(colorFixedCost, cardBgColor)) {
                canvas.drawRect(rFixed, paintBarBorder);
            }
            String labelFixed = getContext().getString(R.string.graph_legend_fixed_cost);
            canvas.drawText(labelFixed, c1X + (boxSize * 0.5f) + 4, row1Y, paintText);
        }

        // --- Řada 2: Cenové čáry ---
        if (isDualTariff) {
            canvas.drawLine(c0X, row2Y - 3, c0X + sampleLength, row2Y - 3, paintLineVT);
            canvas.drawCircle(c0X + sampleLength / 2f, row2Y - 3, markerR, paintMarkerVT);
            canvas.drawText("Cena VT", c0X + sampleLength + 6, row2Y, paintText);

            float c1X = startX + 1 * colW;
            canvas.drawLine(c1X, row2Y - 3, c1X + sampleLength, row2Y - 3, paintLineNT);
            canvas.drawCircle(c1X + sampleLength / 2f, row2Y - 3, markerR, paintMarkerNT);
            canvas.drawText("Cena NT", c1X + sampleLength + 6, row2Y, paintText);

            float c2X = startX + 2 * colW;
            canvas.drawLine(c2X, row2Y - 3, c2X + sampleLength, row2Y - 3, paintLineTotal);
            pathDiamond.reset();
            float midX = c2X + sampleLength / 2f;
            float midY = row2Y - 3;
            pathDiamond.moveTo(midX, midY - markerR * 1.2f);
            pathDiamond.lineTo(midX + markerR * 1.2f, midY);
            pathDiamond.lineTo(midX, midY + markerR * 1.2f);
            pathDiamond.lineTo(midX - markerR * 1.2f, midY);
            pathDiamond.close();
            canvas.drawPath(pathDiamond, paintMarkerTotal);
            canvas.drawText("Cena celkem", c2X + sampleLength + 6, row2Y, paintText);
        } else {
            canvas.drawLine(c0X, row2Y - 3, c0X + sampleLength, row2Y - 3, paintLineTotal);
            pathDiamond.reset();
            float midX = c0X + sampleLength / 2f;
            float midY = row2Y - 3;
            pathDiamond.moveTo(midX, midY - markerR * 1.2f);
            pathDiamond.lineTo(midX + markerR * 1.2f, midY);
            pathDiamond.lineTo(midX, midY + markerR * 1.2f);
            pathDiamond.lineTo(midX - markerR * 1.2f, midY);
            pathDiamond.close();
            canvas.drawPath(pathDiamond, paintMarkerTotal);
            canvas.drawText("Cena celkem", c0X + sampleLength + 6, row2Y, paintText);
        }

        paintText.setTextSize(DensityUtils.dpToPx(getContext(), 11));
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            float touchX = event.getX();
            if (columnXCenters != null && yearDataList != null) {
                float minDistance = Float.MAX_VALUE;
                int selectedIndex = -1;
                for (int i = 0; i < columnXCenters.length; i++) {
                    float dist = Math.abs(touchX - columnXCenters[i]);
                    if (dist < minDistance && dist < DensityUtils.dpToPx(getContext(), 35)) {
                        minDistance = dist;
                        selectedIndex = i;
                    }
                }

                if (selectedIndex >= 0 && selectedIndex < yearDataList.size()) {
                    showDetailDialog(yearDataList.get(selectedIndex));
                    return true;
                }
            }
        }
        return true;
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }


    private void showDetailDialog(AnnualYearData data) {
        String unit = isMWh ? "MWh" : "kWh";
        double factor = isMWh ? 1000.0 : 1.0;

        StringBuilder sb = new StringBuilder();
        sb.append(data.year());
        if (data.isCurrentYear() && data.currentYearRangeLabel() != null) {
            sb.append(" ").append(data.currentYearRangeLabel());
        }
        if (data.isEstimated()) {
            sb.append(" (odhad)");
        }
        sb.append("\n\n");

        sb.append("VT: ").append(DecimalFormatHelper.df2.format(data.consumptionVT() / factor)).append(" ").append(unit).append("\n");
        if (isDualTariff) {
            sb.append("NT: ").append(DecimalFormatHelper.df2.format(data.consumptionNT() / factor)).append(" ").append(unit).append("\n");
        }
        sb.append("Celkem spotřeba: ").append(DecimalFormatHelper.df2.format(data.getTotalConsumption() / factor)).append(" ").append(unit).append("\n\n");

        sb.append("Fixní měsíční náklady: ").append(DecimalFormatHelper.df2.format(data.avgMonthlyFixedCost())).append(" Kč/měsíc\n");
        sb.append("(celkem za rok: ").append(DecimalFormatHelper.df2.format(data.fixedCostTotal())).append(" Kč vč. DPH)\n\n");

        if (isDualTariff) {
            sb.append("Průměrná cena VT: ").append(DecimalFormatHelper.df2.format(data.avgPriceVT())).append(" Kč/MWh\n");
            sb.append("Průměrná cena NT: ").append(DecimalFormatHelper.df2.format(data.avgPriceNT())).append(" Kč/MWh\n");
        } else {
            sb.append("Průměrná cena VT: ").append(DecimalFormatHelper.df2.format(data.avgPriceVT())).append(" Kč/MWh\n");
        }
        sb.append("Průměrná cena celkem: ").append(DecimalFormatHelper.df2.format(data.avgPriceTotal())).append(" Kč/MWh");

        if (data.isEstimated()) {
            sb.append("\n\n* Poznámka: Údaje obsahují vypočtený odhad stavu k 31.12./1.1.");
        }

        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) {
                OwnAlertDialog.showDialog((FragmentActivity) context, getContext().getString(R.string.detail_title_year, data.year()), sb.toString());
                return;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
    }

}
