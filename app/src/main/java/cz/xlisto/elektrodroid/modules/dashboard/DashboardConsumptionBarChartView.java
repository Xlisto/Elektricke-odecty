package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;


/**
 * Skládaný sloupcový graf (VT + NT) pro kartu "Přehled spotřeby"
 * s podporou měsíčního a ročního zobrazení, plynulou animací celého grafu,
 * čárkovaným dokreslením předpokládané spotřeby aktuálního neúplného období
 * a procentní změnou oproti předchozímu sloupci (od 2. sloupce dále).
 */
public class DashboardConsumptionBarChartView extends View {

    private static final long ANIMATION_DURATION_MS = 550L;


    /**
     * Datová položka jednoho sloupce grafu.
     */
    public record BarEntry(String label, double vt, double nt, double projectedTotal,
                           @Nullable Double changePercent) {

        public BarEntry(@NonNull String label, double vt, double nt) {
            this(label, vt, nt, 0.0, null);
        }


        public BarEntry(@NonNull String label,
                        double vt,
                        double nt,
                        double projectedTotal,
                        @Nullable Double changePercent) {
            this.label = label;
            this.vt = Math.max(0.0, vt);
            this.nt = Math.max(0.0, nt);
            double actual = this.vt + this.nt;
            this.projectedTotal = Math.max(actual, projectedTotal);
            this.changePercent = changePercent;
        }


        public double getTotal() {
            return vt + nt;
        }


        /**
         * Vrátí efektivní celkovou výšku sloupce (včetně případné předpokládané spotřeby).
         */
        public double getEffectiveTotal() {
            return Math.max(getTotal(), projectedTotal);
        }


        /**
         * Vrátí {@code true}, pokud má sloupec nenulový předpokládaný dodatek nad skutečnou spotřebou.
         */
        public boolean hasForecast() {
            return projectedTotal > getTotal() + 0.5;
        }

    }


    private Paint paintVt;
    private Paint paintNt;
    private Paint paintForecastFill;
    private Paint paintForecastStroke;
    private Paint paintGrid;
    private Paint paintAxisText;
    private Paint paintLabelText;
    private Paint paintLegendText;
    private Paint paintPercentText;

    private int colorPercentDecrease;
    private int colorPercentIncrease;
    private int colorPercentNeutral;
    private int colorForecastFillBase;

    private final RectF rectBuffer = new RectF();
    private final Path barClipPath = new Path();
    private final Path forecastPath = new Path();
    private final float[] barCornerRadii = new float[8];
    private final float[] flatCornerRadii = new float[8];

    private final List<BarEntry> startEntries = new ArrayList<>();
    private final List<BarEntry> targetEntries = new ArrayList<>();

    private int startStep = 200;
    private int targetStep = 200;
    private float startMaxAxisValue = 800f;
    private float targetMaxAxisValue = 800f;
    private float startBarCount = 0f;
    private float targetBarCount = 0f;
    private boolean hasInitializedOnce = false;

    private float animFraction = 1f;
    private ValueAnimator animator;


    public DashboardConsumptionBarChartView(Context context) {
        super(context);
        init();
    }


    public DashboardConsumptionBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public DashboardConsumptionBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        int colorOnSurface = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                0xFF1C1B1F
        );
        int colorPrimary = MaterialColors.getColor(
                this,
                androidx.appcompat.R.attr.colorPrimary,
                ContextCompat.getColor(getContext(), R.color.dashboard_teal_nt)
        );
        int colorOutline = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOutline,
                ContextCompat.getColor(getContext(), R.color.dashboard_chart_forecast_stroke)
        );
        int colorOutlineVariant = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOutlineVariant,
                ContextCompat.getColor(getContext(), R.color.dashboard_divider)
        );

        colorPercentDecrease = MaterialColors.harmonizeWithPrimary(
                getContext(),
                ContextCompat.getColor(getContext(), R.color.dashboard_chart_percent_decrease)
        );
        colorPercentIncrease = MaterialColors.harmonizeWithPrimary(
                getContext(),
                ContextCompat.getColor(getContext(), R.color.dashboard_chart_percent_increase)
        );
        colorPercentNeutral = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                ContextCompat.getColor(getContext(), R.color.dashboard_chart_percent_neutral)
        );
        colorForecastFillBase = ColorUtils.setAlphaComponent(colorPrimary, 36);

        paintVt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintVt.setStyle(Paint.Style.FILL);
        paintVt.setColor(MaterialColors.harmonizeWithPrimary(
                getContext(),
                ContextCompat.getColor(getContext(), R.color.dashboard_teal_vt)
        ));

        paintNt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNt.setStyle(Paint.Style.FILL);
        paintNt.setColor(colorPrimary);

        paintForecastFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintForecastFill.setStyle(Paint.Style.FILL);
        paintForecastFill.setColor(colorForecastFillBase);

        paintForecastStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintForecastStroke.setStyle(Paint.Style.STROKE);
        paintForecastStroke.setStrokeWidth(dpToPx(getContext(), 1.5f));
        paintForecastStroke.setStrokeCap(Paint.Cap.ROUND);
        paintForecastStroke.setColor(colorOutline);
        paintForecastStroke.setPathEffect(new DashPathEffect(
                new float[]{dpToPx(getContext(), 4f), dpToPx(getContext(), 3f)},
                0f
        ));

        paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(dpToPx(getContext(), 1f));
        paintGrid.setColor(colorOutlineVariant);

        paintAxisText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAxisText.setStyle(Paint.Style.FILL);
        paintAxisText.setColor(colorOnSurface);
        paintAxisText.setAlpha(180);
        paintAxisText.setTextAlign(Paint.Align.RIGHT);
        paintAxisText.setTextSize(dpToPx(getContext(), 10.5f));

        paintLabelText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabelText.setStyle(Paint.Style.FILL);
        paintLabelText.setColor(colorOnSurface);
        paintLabelText.setTextAlign(Paint.Align.CENTER);
        paintLabelText.setTextSize(dpToPx(getContext(), 11.5f));

        paintLegendText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLegendText.setStyle(Paint.Style.FILL);
        paintLegendText.setColor(colorOnSurface);
        paintLegendText.setTextAlign(Paint.Align.LEFT);
        paintLegendText.setTextSize(dpToPx(getContext(), 11.5f));

        paintPercentText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPercentText.setStyle(Paint.Style.FILL);
        paintPercentText.setTextAlign(Paint.Align.CENTER);
        paintPercentText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintPercentText.setTextSize(dpToPx(getContext(), 9.5f));
    }


    /**
     * Nastaví uživatelské barvy pro vysoký (VT) a nízký (NT) tarif.
     */
    public void setColors(int colorVt, int colorNt) {
        paintVt.setColor(colorVt);
        paintNt.setColor(colorNt);
        invalidate();
    }


    /**
     * Nastaví položky sloupcového grafu a plynule odanimuje přechod celého grafu.
     */
    public void setEntries(@Nullable List<BarEntry> barEntries) {
        setEntries(barEntries, true);
    }


    /**
     * Nastaví položky sloupcového grafu s možností volby animace.
     */
    public void setEntries(@Nullable List<BarEntry> barEntries, boolean animate) {
        // 1. Zachycení aktuálně vykresleného mezistavu (pro plynulé navázání při rychlém přepnutí)
        List<BarEntry> snapshotEntries = captureCurrentInterpolatedEntries();
        float snapshotMaxAxis = getDisplayedMaxAxisValue();
        float snapshotBarCount = getDisplayedBarCount();
        int snapshotStep = (animFraction < 0.5f) ? startStep : targetStep;

        // 2. Výpočet cílového stavu (včetně případného předpokladu pro škálování osy Y)
        targetEntries.clear();
        if (barEntries != null) {
            targetEntries.addAll(barEntries);
        }

        double maxTotal = 0.0;
        for (BarEntry entry : targetEntries) {
            maxTotal = Math.max(maxTotal, entry.getEffectiveTotal());
        }
        targetStep = computeNiceStep(maxTotal);
        targetMaxAxisValue = targetStep * 4f;
        targetBarCount = targetEntries.size();

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (!animate || !isAttachedToWindow() || (targetEntries.isEmpty() && snapshotEntries.isEmpty())) {
            startEntries.clear();
            startEntries.addAll(targetEntries);
            startStep = targetStep;
            startMaxAxisValue = targetMaxAxisValue;
            startBarCount = targetBarCount;
            hasInitializedOnce = !targetEntries.isEmpty();
            animFraction = 1f;
            invalidate();
            return;
        }

        startEntries.clear();
        if (!hasInitializedOnce || snapshotEntries.isEmpty()) {
            // Při prvotním zobrazení je osa Y i počet sloupců připraven a sloupce vyrostou od nuly
            startStep = targetStep;
            startMaxAxisValue = targetMaxAxisValue;
            startBarCount = targetBarCount;
            for (BarEntry target : targetEntries) {
                startEntries.add(new BarEntry(target.label, 0.0, 0.0, 0.0, target.changePercent));
            }
        } else {
            startEntries.addAll(snapshotEntries);
            startStep = snapshotStep;
            startMaxAxisValue = snapshotMaxAxis > 0f ? snapshotMaxAxis : targetMaxAxisValue;
            startBarCount = snapshotBarCount > 0f ? snapshotBarCount : targetBarCount;
        }
        hasInitializedOnce = !targetEntries.isEmpty();

        animFraction = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            animFraction = (float) anim.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }


    @NonNull
    private List<BarEntry> captureCurrentInterpolatedEntries() {
        int maxCount = Math.max(startEntries.size(), targetEntries.size());
        List<BarEntry> result = new ArrayList<>(maxCount);
        for (int i = 0; i < maxCount; i++) {
            double vt = getInterpolatedVt(i);
            double nt = getInterpolatedNt(i);
            double projected = getInterpolatedProjectedTotal(i);
            String label;
            Double pct;
            if (i < targetEntries.size()) {
                label = targetEntries.get(i).label;
                pct = (animFraction >= 0.5f || i >= startEntries.size())
                        ? targetEntries.get(i).changePercent
                        : startEntries.get(i).changePercent;
            } else if (i < startEntries.size()) {
                label = startEntries.get(i).label;
                pct = startEntries.get(i).changePercent;
            } else {
                label = "";
                pct = null;
            }
            if (i < targetEntries.size() || Math.max(vt + nt, projected) > 0.5) {
                result.add(new BarEntry(label, vt, nt, projected, pct));
            }
        }
        return result;
    }


    private float getDisplayedMaxAxisValue() {
        return Math.max(1f, startMaxAxisValue + (targetMaxAxisValue - startMaxAxisValue) * animFraction);
    }


    private float getDisplayedBarCount() {
        float raw = startBarCount + (targetBarCount - startBarCount) * animFraction;
        return Math.max(1f, raw);
    }


    private double getInterpolatedVt(int index) {
        double from = (index < startEntries.size()) ? startEntries.get(index).vt : 0.0;
        double to = (index < targetEntries.size()) ? targetEntries.get(index).vt : 0.0;
        return from + (to - from) * animFraction;
    }


    private double getInterpolatedNt(int index) {
        double from = (index < startEntries.size()) ? startEntries.get(index).nt : 0.0;
        double to = (index < targetEntries.size()) ? targetEntries.get(index).nt : 0.0;
        return from + (to - from) * animFraction;
    }


    private double getInterpolatedProjectedTotal(int index) {
        double from = (index < startEntries.size()) ? startEntries.get(index).getEffectiveTotal() : 0.0;
        double to = (index < targetEntries.size()) ? targetEntries.get(index).getEffectiveTotal() : 0.0;
        return from + (to - from) * animFraction;
    }


    private boolean hasAnyDisplayedForecast() {
        for (BarEntry entry : targetEntries) {
            if (entry.hasForecast()) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // Rezervujeme dostatečný prostor nahoře, aby popisek % nikdy nevyjel mimo viditelnou oblast ani u max. sloupce
        float defaultPercentTextSize = dpToPx(getContext(), 9.5f);
        paintPercentText.setTextSize(defaultPercentTextSize);
        Paint.FontMetrics pctFm = paintPercentText.getFontMetrics();
        float pctTextHeight = pctFm.descent - pctFm.ascent;
        float pctGap = dpToPx(getContext(), 4f);

        float topPad = pctTextHeight + pctGap + dpToPx(getContext(), 4f);
        float bottomPad = dpToPx(getContext(), 22f);
        float legendWidth = dpToPx(getContext(), 58f);
        float minAxisPad = dpToPx(getContext(), 32f);
        float textExtraPad = dpToPx(getContext(), 10f);

        // Plynulá interpolace šířky levé osy Y, aby graf při změně řádu hodnot (např. 800 -> 6000) neposkočil
        float startLeftWidth = Math.max(minAxisPad, paintAxisText.measureText(String.valueOf(startStep * 4)) + textExtraPad);
        float targetLeftWidth = Math.max(minAxisPad, paintAxisText.measureText(String.valueOf(targetStep * 4)) + textExtraPad);
        float leftAxisWidth = startLeftWidth + (targetLeftWidth - startLeftWidth) * animFraction;

        float chartLeft = leftAxisWidth;
        float chartRight = w - legendWidth - dpToPx(getContext(), 8f);
        float chartTop = topPad;
        float chartBottom = h - bottomPad;
        float chartHeight = Math.max(1f, chartBottom - chartTop);
        float chartWidth = Math.max(1f, chartRight - chartLeft);

        float displayedMaxAxis = getDisplayedMaxAxisValue();

        // 1. Vodorovné linky mřížky a popisky osy Y (s plynulým posunem při změně rozsahu)
        Paint.FontMetrics axisFm = paintAxisText.getFontMetrics();
        float axisTextOffset = -(axisFm.ascent + axisFm.descent) / 2f;

        // Základní linka 0 je vždy pevně dole
        paintGrid.setAlpha(255);
        paintAxisText.setAlpha(180);
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paintGrid);
        canvas.drawText("0", chartLeft - dpToPx(getContext(), 6f), chartBottom + axisTextOffset, paintAxisText);

        if (startStep == targetStep || animFraction >= 0.999f) {
            for (int i = 1; i <= 4; i++) {
                int val = i * targetStep;
                float ratio = val / displayedMaxAxis;
                float y = chartBottom - ratio * chartHeight;
                drawGridLineAndLabel(canvas, chartLeft, chartRight, chartTop, chartBottom, y, val, axisTextOffset, 1f);
            }
        } else {
            // Staré vodorovné osy se plynule posouvají podle aktuálního měřítka a mizí
            float oldAlpha = 1f - animFraction;
            for (int i = 1; i <= 4; i++) {
                int val = i * startStep;
                float ratio = val / displayedMaxAxis;
                float y = chartBottom - ratio * chartHeight;
                drawGridLineAndLabel(canvas, chartLeft, chartRight, chartTop, chartBottom, y, val, axisTextOffset, oldAlpha);
            }
            // Nové vodorovné osy se plynule posouvají na své cílové pozice a objevují se
            float newAlpha = animFraction;
            for (int i = 1; i <= 4; i++) {
                int val = i * targetStep;
                float ratio = val / displayedMaxAxis;
                float y = chartBottom - ratio * chartHeight;
                drawGridLineAndLabel(canvas, chartLeft, chartRight, chartTop, chartBottom, y, val, axisTextOffset, newAlpha);
            }
        }
        paintGrid.setAlpha(255);
        paintAxisText.setAlpha(180);

        // 2. Vykreslení sloupců (VT dole, NT nahoře, případný čárkovaný odhad a % změna)
        int totalBarsToRender = Math.max(startEntries.size(), targetEntries.size());
        if (totalBarsToRender > 0) {
            float displayedBarCount = getDisplayedBarCount();
            float slotWidth = chartWidth / displayedBarCount;
            float barWidth = Math.min(dpToPx(getContext(), 34f), slotWidth * 0.58f);
            float cornerRadius = dpToPx(getContext(), 4f);
            barCornerRadii[0] = cornerRadius;
            barCornerRadii[1] = cornerRadius;
            barCornerRadii[2] = cornerRadius;
            barCornerRadii[3] = cornerRadius;
            barCornerRadii[4] = 0f;
            barCornerRadii[5] = 0f;
            barCornerRadii[6] = 0f;
            barCornerRadii[7] = 0f;

            // Oříznutí oblasti sloupců a popisků, aby při změně počtu sloupců nepřetékaly do legendy
            int chartClipSave = canvas.save();
            canvas.clipRect(chartLeft - dpToPx(getContext(), 2f), 0f, chartRight + dpToPx(getContext(), 4f), h);

            for (int i = 0; i < totalBarsToRender; i++) {
                double vtVal = getInterpolatedVt(i);
                double ntVal = getInterpolatedNt(i);
                double projectedVal = getInterpolatedProjectedTotal(i);

                float slotCenterX = chartLeft + (i + 0.5f) * slotWidth;
                float barLeft = slotCenterX - barWidth / 2f;
                float barRight = slotCenterX + barWidth / 2f;

                if (barLeft > chartRight + dpToPx(getContext(), 4f)) {
                    continue;
                }

                // Průhlednost sloupce při přidávání nebo ubírání počtu sloupců
                float barVisibility = 1f;
                if (i >= targetEntries.size()) {
                    barVisibility = 1f - animFraction;
                } else if (i >= startEntries.size() && !startEntries.isEmpty()) {
                    barVisibility = animFraction;
                }
                int barAlpha = Math.max(0, Math.min(255, Math.round(255 * barVisibility)));

                float vtHeight = Math.max(0f, (float) ((vtVal / displayedMaxAxis) * chartHeight));
                float ntHeight = Math.max(0f, (float) ((ntVal / displayedMaxAxis) * chartHeight));
                float actualHeight = Math.min(chartHeight, vtHeight + ntHeight);
                float projectedHeight = Math.min(
                        chartHeight,
                        Math.max(actualHeight, (float) ((projectedVal / displayedMaxAxis) * chartHeight))
                );
                boolean hasProjectedExtension = projectedHeight - actualHeight > dpToPx(getContext(), 1.5f);

                float actualTop = chartBottom - actualHeight;
                float visualTop = chartBottom - projectedHeight;

                // 2a. Skutečná spotřeba (VT + NT)
                if (actualHeight > 0.5f && barAlpha > 0) {
                    rectBuffer.set(barLeft, actualTop, barRight, chartBottom);

                    barClipPath.reset();
                    barClipPath.addRoundRect(
                            rectBuffer,
                            hasProjectedExtension ? flatCornerRadii : barCornerRadii,
                            Path.Direction.CW
                    );

                    int save = canvas.save();
                    canvas.clipPath(barClipPath);

                    paintVt.setAlpha(barAlpha);
                    paintNt.setAlpha(barAlpha);

                    // VT spodní část
                    if (vtHeight > 0) {
                        float vtTop = chartBottom - vtHeight;
                        canvas.drawRect(barLeft, vtTop, barRight, chartBottom, paintVt);
                    }
                    // NT horní část
                    if (ntHeight > 0) {
                        float ntBottom = chartBottom - vtHeight;
                        float ntTop = ntBottom - ntHeight;
                        canvas.drawRect(barLeft, ntTop, barRight, ntBottom, paintNt);
                    }
                    canvas.restoreToCount(save);
                }

                // 2b. Čárkované doplnění předpokládané spotřeby aktuálního období
                if (hasProjectedExtension && barAlpha > 0) {
                    drawForecastExtension(
                            canvas,
                            barLeft,
                            barRight,
                            visualTop,
                            actualTop,
                            actualHeight,
                            chartBottom,
                            cornerRadius,
                            barVisibility
                    );
                }

                // 2c. Procentní změna nad sloupcem (na prvním sloupci se nikdy nezobrazuje)
                if (i > 0 && barAlpha > 0) {
                    drawPercentLabel(
                            canvas,
                            i,
                            slotCenterX,
                            slotWidth,
                            visualTop,
                            chartLeft,
                            chartRight,
                            pctGap,
                            defaultPercentTextSize,
                            barVisibility
                    );
                }

                // 2d. Popisek na ose X (s plynulým prolínáním při změně textu nebo počtu sloupců)
                drawXAxisLabel(canvas, i, slotCenterX, chartBottom + dpToPx(getContext(), 15f));
            }

            paintVt.setAlpha(255);
            paintNt.setAlpha(255);
            paintLabelText.setAlpha(255);
            canvas.restoreToCount(chartClipSave);
        }

        // 3. Legenda vpravo (VT, NT a při přítomnosti odhadu také čárkovaná položka "Odhad")
        drawLegend(canvas, chartRight, chartTop, chartHeight);
    }


    private void drawForecastExtension(@NonNull Canvas canvas,
                                       float barLeft,
                                       float barRight,
                                       float projectedTop,
                                       float actualTop,
                                       float actualHeight,
                                       float chartBottom,
                                       float cornerRadius,
                                       float visibility) {
        float bottomY = actualHeight > 0.5f ? actualTop : chartBottom;
        if (bottomY - projectedTop <= 0.5f) {
            return;
        }

        // Jemná průsvitná výplň odhadované části
        rectBuffer.set(barLeft, projectedTop, barRight, bottomY);
        barClipPath.reset();
        barClipPath.addRoundRect(rectBuffer, barCornerRadii, Path.Direction.CW);

        int baseFillColor = colorForecastFillBase;
        int baseFillAlpha = (baseFillColor >>> 24) & 0xFF;
        paintForecastFill.setColor(baseFillColor);
        paintForecastFill.setAlpha(Math.round(baseFillAlpha * visibility));
        canvas.drawPath(barClipPath, paintForecastFill);

        // Čárkovaný obrys horní a bočních hran
        float halfStroke = paintForecastStroke.getStrokeWidth() / 2f;
        float fl = barLeft + halfStroke;
        float fr = barRight - halfStroke;
        float ft = projectedTop + halfStroke;
        float fb = actualHeight > 0.5f ? actualTop : chartBottom - halfStroke;
        if (fr <= fl || fb <= ft) {
            return;
        }

        float r = Math.min(cornerRadius, Math.min((fr - fl) / 2f, fb - ft));
        forecastPath.reset();
        forecastPath.moveTo(fl, fb);
        forecastPath.lineTo(fl, ft + r);
        if (r > 0.5f) {
            rectBuffer.set(fl, ft, fl + 2f * r, ft + 2f * r);
            forecastPath.arcTo(rectBuffer, 180f, 90f, false);
        }
        forecastPath.lineTo(fr - r, ft);
        if (r > 0.5f) {
            rectBuffer.set(fr - 2f * r, ft, fr, ft + 2f * r);
            forecastPath.arcTo(rectBuffer, 270f, 90f, false);
        }
        forecastPath.lineTo(fr, fb);

        paintForecastStroke.setAlpha(Math.max(0, Math.min(255, Math.round(230 * visibility))));
        canvas.drawPath(forecastPath, paintForecastStroke);
    }


    private void drawPercentLabel(@NonNull Canvas canvas,
                                  int index,
                                  float slotCenterX,
                                  float slotWidth,
                                  float visualBarTop,
                                  float chartLeft,
                                  float chartRight,
                                  float pctGap,
                                  float defaultTextSize,
                                  float barVisibility) {
        if (index <= 0) {
            return;
        }

        Double oldPct = (index < startEntries.size()) ? startEntries.get(index).changePercent : null;
        Double newPct = (index < targetEntries.size()) ? targetEntries.get(index).changePercent : null;

        String oldText = formatChangePercent(oldPct);
        String newText = formatChangePercent(newPct);

        if (oldText != null && oldText.equals(newText)) {
            drawSinglePercentText(
                    canvas,
                    newText,
                    resolvePercentColor(newPct),
                    slotCenterX,
                    slotWidth,
                    visualBarTop,
                    chartLeft,
                    chartRight,
                    pctGap,
                    defaultTextSize,
                    barVisibility
            );
            return;
        }

        if (oldText != null && animFraction < 1f) {
            float alpha = (1f - animFraction) * barVisibility;
            if (alpha > 0.02f) {
                drawSinglePercentText(
                        canvas,
                        oldText,
                        resolvePercentColor(oldPct),
                        slotCenterX,
                        slotWidth,
                        visualBarTop,
                        chartLeft,
                        chartRight,
                        pctGap,
                        defaultTextSize,
                        alpha
                );
            }
        }

        if (newText != null && animFraction > 0f) {
            float alpha = animFraction * barVisibility;
            if (alpha > 0.02f) {
                drawSinglePercentText(
                        canvas,
                        newText,
                        resolvePercentColor(newPct),
                        slotCenterX,
                        slotWidth,
                        visualBarTop,
                        chartLeft,
                        chartRight,
                        pctGap,
                        defaultTextSize,
                        alpha
                );
            }
        }
    }


    private void drawSinglePercentText(@NonNull Canvas canvas,
                                       @NonNull String text,
                                       int color,
                                       float slotCenterX,
                                       float slotWidth,
                                       float visualBarTop,
                                       float chartLeft,
                                       float chartRight,
                                       float pctGap,
                                       float defaultTextSize,
                                       float alphaFraction) {
        paintPercentText.setTextSize(defaultTextSize);
        float maxTextWidth = Math.max(dpToPx(getContext(), 22f), slotWidth - dpToPx(getContext(), 2f));
        float measuredWidth = paintPercentText.measureText(text);
        if (measuredWidth > maxTextWidth && measuredWidth > 0f) {
            float minTextSize = dpToPx(getContext(), 7.5f);
            float scaledSize = Math.max(minTextSize, defaultTextSize * (maxTextWidth / measuredWidth));
            paintPercentText.setTextSize(scaledSize);
            measuredWidth = paintPercentText.measureText(text);
        }

        Paint.FontMetrics fm = paintPercentText.getFontMetrics();
        // Ochrana horního okraje: i při maximální výšce sloupce zůstane popisek vždy uvnitř grafu
        float minSafeBaselineY = dpToPx(getContext(), 2f) - fm.ascent;
        float desiredBaselineY = visualBarTop - pctGap - fm.descent;
        float baselineY = Math.max(minSafeBaselineY, desiredBaselineY);

        // Ochrana bočních okrajů oblasti grafu
        float halfW = measuredWidth / 2f;
        float minX = chartLeft + halfW;
        float maxX = chartRight - halfW;
        float x = (minX <= maxX)
                ? Math.max(minX, Math.min(maxX, slotCenterX))
                : slotCenterX;

        paintPercentText.setColor(color);
        paintPercentText.setAlpha(Math.max(0, Math.min(255, Math.round(255 * alphaFraction))));
        canvas.drawText(text, x, baselineY, paintPercentText);
    }


    @Nullable
    private String formatChangePercent(@Nullable Double changePercent) {
        if (changePercent == null || Double.isNaN(changePercent) || Double.isInfinite(changePercent)) {
            return null;
        }
        long rounded = Math.round(changePercent);
        if (rounded > 999L) {
            return ">999%";
        }
        if (rounded < -999L) {
            return "<-999%";
        }
        if (rounded > 0L) {
            return "+" + rounded + "%";
        }
        return rounded + "%";
    }


    private int resolvePercentColor(@Nullable Double changePercent) {
        if (changePercent == null) {
            return colorPercentNeutral;
        }
        long rounded = Math.round(changePercent);
        if (rounded > 0L) {
            return colorPercentIncrease;
        }
        if (rounded < 0L) {
            return colorPercentDecrease;
        }
        return colorPercentNeutral;
    }


    private void drawLegend(@NonNull Canvas canvas, float chartRight, float chartTop, float chartHeight) {
        float legendX = chartRight + dpToPx(getContext(), 10f);
        float boxSize = dpToPx(getContext(), 11f);
        float boxCorner = dpToPx(getContext(), 2.5f);
        boolean showForecastLegend = hasAnyDisplayedForecast();
        float rowSpacing = dpToPx(getContext(), 21f);
        int itemCount = showForecastLegend ? 3 : 2;
        float blockHeight = (itemCount - 1) * rowSpacing + boxSize;
        float startY = chartTop + Math.max(0f, (chartHeight - blockHeight) * 0.45f);

        // VT legenda
        paintVt.setAlpha(255);
        rectBuffer.set(legendX, startY, legendX + boxSize, startY + boxSize);
        canvas.drawRoundRect(rectBuffer, boxCorner, boxCorner, paintVt);
        canvas.drawText(
                getContext().getString(R.string.kwh_vt),
                legendX + boxSize + dpToPx(getContext(), 6f),
                startY + boxSize - dpToPx(getContext(), 1f),
                paintLegendText
        );

        // NT legenda
        float ntLegendY = startY + rowSpacing;
        paintNt.setAlpha(255);
        rectBuffer.set(legendX, ntLegendY, legendX + boxSize, ntLegendY + boxSize);
        canvas.drawRoundRect(rectBuffer, boxCorner, boxCorner, paintNt);
        canvas.drawText(
                getContext().getString(R.string.kwh_nt),
                legendX + boxSize + dpToPx(getContext(), 6f),
                ntLegendY + boxSize - dpToPx(getContext(), 1f),
                paintLegendText
        );

        // Legenda čárkovaného odhadu (jen pokud je odhad v grafu zobrazen)
        if (showForecastLegend) {
            float forecastLegendY = ntLegendY + rowSpacing;
            paintForecastFill.setColor(colorForecastFillBase);
            rectBuffer.set(legendX, forecastLegendY, legendX + boxSize, forecastLegendY + boxSize);
            canvas.drawRoundRect(rectBuffer, boxCorner, boxCorner, paintForecastFill);

            paintForecastStroke.setAlpha(230);
            float halfStroke = paintForecastStroke.getStrokeWidth() / 2f;
            rectBuffer.set(
                    legendX + halfStroke,
                    forecastLegendY + halfStroke,
                    legendX + boxSize - halfStroke,
                    forecastLegendY + boxSize - halfStroke
            );
            canvas.drawRoundRect(rectBuffer, boxCorner, boxCorner, paintForecastStroke);
            canvas.drawText(
                    getContext().getString(R.string.dashboard_chart_forecast_legend),
                    legendX + boxSize + dpToPx(getContext(), 6f),
                    forecastLegendY + boxSize - dpToPx(getContext(), 1f),
                    paintLegendText
            );
        }
    }


    private void drawGridLineAndLabel(@NonNull Canvas canvas,
                                      float chartLeft,
                                      float chartRight,
                                      float chartTop,
                                      float chartBottom,
                                      float y,
                                      int value,
                                      float axisTextOffset,
                                      float visibilityFraction) {
        if (y < chartTop - dpToPx(getContext(), 8f) || y > chartBottom - dpToPx(getContext(), 2f)) {
            return;
        }
        // Plynulé utlumení linek, které při přiblížení vyjíždějí nad horní okraj grafu
        float edgeFade = 1f;
        if (y < chartTop) {
            edgeFade = Math.max(0f, 1f - (chartTop - y) / dpToPx(getContext(), 8f));
        }
        float effectiveVisibility = Math.max(0f, Math.min(1f, visibilityFraction * edgeFade));
        int gridAlpha = Math.round(255 * effectiveVisibility);
        int textAlpha = Math.round(180 * effectiveVisibility);
        if (gridAlpha <= 2) {
            return;
        }
        paintGrid.setAlpha(gridAlpha);
        paintAxisText.setAlpha(textAlpha);
        canvas.drawLine(chartLeft, y, chartRight, y, paintGrid);
        canvas.drawText(String.valueOf(value), chartLeft - dpToPx(getContext(), 6f), y + axisTextOffset, paintAxisText);
    }


    private void drawXAxisLabel(@NonNull Canvas canvas, int index, float x, float y) {
        String oldLabel = (index < startEntries.size()) ? startEntries.get(index).label : null;
        String newLabel = (index < targetEntries.size()) ? targetEntries.get(index).label : null;

        if (oldLabel != null && oldLabel.equals(newLabel)) {
            paintLabelText.setAlpha(255);
            canvas.drawText(newLabel, x, y, paintLabelText);
            return;
        }

        if (oldLabel != null && !oldLabel.isEmpty() && animFraction < 1f) {
            int oldAlpha = Math.max(0, Math.min(255, Math.round(255 * (1f - animFraction))));
            if (oldAlpha > 2) {
                paintLabelText.setAlpha(oldAlpha);
                canvas.drawText(oldLabel, x, y, paintLabelText);
            }
        }

        if (newLabel != null && !newLabel.isEmpty() && animFraction > 0f) {
            int newAlpha = Math.max(0, Math.min(255, Math.round(255 * animFraction)));
            if (newAlpha > 2) {
                paintLabelText.setAlpha(newAlpha);
                canvas.drawText(newLabel, x, y, paintLabelText);
            }
        }
    }


    private int computeNiceStep(double maxTotal) {
        if (maxTotal <= 0) {
            return 200;
        }
        double rawStep = maxTotal / 4.0;
        int[] niceSteps = {10, 20, 25, 50, 100, 150, 200, 250, 300, 400, 500, 750, 1000, 1500, 2000, 2500, 5000, 10000};
        for (int candidate : niceSteps) {
            if (candidate >= rawStep) {
                return candidate;
            }
        }
        return (int) (Math.ceil(rawStep / 5000.0) * 5000);
    }

}
