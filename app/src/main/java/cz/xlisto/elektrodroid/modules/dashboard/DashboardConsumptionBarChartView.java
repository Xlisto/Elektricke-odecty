package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;


/**
 * Skládaný sloupcový graf (VT + NT) pro kartu "Přehled spotřeby"
 * s podporou měsíčního a ročního zobrazení a plynulou animací celého grafu:
 * výšky sloupců, změny počtu sloupců (šířky a horizontálního rozložení)
 * i změny rozsahu hodnot a vodorovných os mřížky.
 */
public class DashboardConsumptionBarChartView extends View {

    private static final long ANIMATION_DURATION_MS = 550L;

    /**
     * Datová položka jednoho sloupce grafu.
     */
    public static class BarEntry {

        public final String label;
        public final double vt;
        public final double nt;


        public BarEntry(@NonNull String label, double vt, double nt) {
            this.label = label;
            this.vt = Math.max(0.0, vt);
            this.nt = Math.max(0.0, nt);
        }


        public double getTotal() {
            return vt + nt;
        }

    }


    private Paint paintVt;
    private Paint paintNt;
    private Paint paintGrid;
    private Paint paintAxisText;
    private Paint paintLabelText;
    private Paint paintLegendText;
    private final RectF rectBuffer = new RectF();
    private final Path barClipPath = new Path();
    private final float[] barCornerRadii = new float[8];

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
        int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);

        paintVt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintVt.setStyle(Paint.Style.FILL);
        paintVt.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_teal_vt));

        paintNt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNt.setStyle(Paint.Style.FILL);
        paintNt.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_teal_nt));

        paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(dpToPx(getContext(), 1f));
        paintGrid.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_divider));

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
        paintLegendText.setTextSize(dpToPx(getContext(), 12f));
    }


    private int resolveAttrColor(int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrRes, tv, true)) {
            return tv.data;
        }
        return fallback;
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

        // 2. Výpočet cílového stavu
        targetEntries.clear();
        if (barEntries != null) {
            targetEntries.addAll(barEntries);
        }

        double maxTotal = 0.0;
        for (BarEntry entry : targetEntries) {
            maxTotal = Math.max(maxTotal, entry.getTotal());
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
                startEntries.add(new BarEntry(target.label, 0.0, 0.0));
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
            String label;
            if (i < targetEntries.size()) {
                label = targetEntries.get(i).label;
            } else if (i < startEntries.size()) {
                label = startEntries.get(i).label;
            } else {
                label = "";
            }
            if (i < targetEntries.size() || vt + nt > 0.5) {
                result.add(new BarEntry(label, vt, nt));
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

        float topPad = dpToPx(getContext(), 10f);
        float bottomPad = dpToPx(getContext(), 22f);
        float legendWidth = dpToPx(getContext(), 54f);
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

        // 2. Vykreslení sloupců (VT dole, NT nahoře) s plynulou animací výšky i počtu sloupců
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

            // Oříznutí oblasti sloupců a popisků osy X, aby při změně počtu sloupců nepřetékaly do legendy
            int chartClipSave = canvas.save();
            canvas.clipRect(chartLeft, 0f, chartRight + dpToPx(getContext(), 4f), h);

            for (int i = 0; i < totalBarsToRender; i++) {
                double vtVal = getInterpolatedVt(i);
                double ntVal = getInterpolatedNt(i);

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
                float totalHeight = Math.min(chartHeight, vtHeight + ntHeight);

                if (totalHeight > 0.5f && barAlpha > 0) {
                    float barTop = chartBottom - totalHeight;
                    rectBuffer.set(barLeft, barTop, barRight, chartBottom);

                    barClipPath.reset();
                    barClipPath.addRoundRect(rectBuffer, barCornerRadii, Path.Direction.CW);

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

                // Popisek na ose X (s plynulým prolínáním při změně textu nebo počtu sloupců)
                drawXAxisLabel(canvas, i, slotCenterX, chartBottom + dpToPx(getContext(), 15f));
            }

            paintVt.setAlpha(255);
            paintNt.setAlpha(255);
            paintLabelText.setAlpha(255);
            canvas.restoreToCount(chartClipSave);
        }

        // 3. Legenda vpravo (VT a NT)
        float legendX = chartRight + dpToPx(getContext(), 10f);
        float boxSize = dpToPx(getContext(), 11f);
        float boxCorner = dpToPx(getContext(), 2.5f);
        float centerY = chartTop + chartHeight * 0.35f;

        // VT legenda
        rectBuffer.set(legendX, centerY, legendX + boxSize, centerY + boxSize);
        canvas.drawRoundRect(rectBuffer, boxCorner, boxCorner, paintVt);
        canvas.drawText(getContext().getString(R.string.kwh_vt), legendX + boxSize + dpToPx(getContext(), 6f), centerY + boxSize - dpToPx(getContext(), 1f), paintLegendText);

        // NT legenda
        float ntLegendY = centerY + dpToPx(getContext(), 22f);
        rectBuffer.set(legendX, ntLegendY, legendX + boxSize, ntLegendY + boxSize);
        canvas.drawRoundRect(rectBuffer, boxCorner, boxCorner, paintNt);
        canvas.drawText(getContext().getString(R.string.kwh_nt), legendX + boxSize + dpToPx(getContext(), 6f), ntLegendY + boxSize - dpToPx(getContext(), 1f), paintLegendText);
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
