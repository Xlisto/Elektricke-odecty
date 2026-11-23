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
 * s podporou měsíčního a ročního zobrazení a plynulou animací výšky sloupců při změně dat.
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


    /**
     * Normalizované poměry výšky VT a NT jednoho sloupce vůči výšce grafu (0..1).
     */
    private static class BarHeightRatio {

        final float vtRatio;
        final float ntRatio;


        BarHeightRatio(float vtRatio, float ntRatio) {
            this.vtRatio = vtRatio;
            this.ntRatio = ntRatio;
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

    private final List<BarEntry> entries = new ArrayList<>();
    private final List<BarHeightRatio> startRatios = new ArrayList<>();
    private final List<BarHeightRatio> targetRatios = new ArrayList<>();
    private int currentStep = 200;
    private int currentMaxAxisValue = 800;
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
     * Nastaví položky sloupcového grafu a plynule odanimuje přechod výšky sloupců.
     */
    public void setEntries(@Nullable List<BarEntry> barEntries) {
        setEntries(barEntries, true);
    }


    /**
     * Nastaví položky sloupcového grafu s možností volby animace.
     */
    public void setEntries(@Nullable List<BarEntry> barEntries, boolean animate) {
        List<BarHeightRatio> currentDisplayed = getCurrentInterpolatedRatios();

        entries.clear();
        if (barEntries != null) {
            entries.addAll(barEntries);
        }

        double maxTotal = 0.0;
        for (BarEntry entry : entries) {
            maxTotal = Math.max(maxTotal, entry.getTotal());
        }
        currentStep = computeNiceStep(maxTotal);
        currentMaxAxisValue = currentStep * 4;

        targetRatios.clear();
        startRatios.clear();
        for (int i = 0; i < entries.size(); i++) {
            BarEntry entry = entries.get(i);
            float vtR = currentMaxAxisValue > 0 ? (float) (entry.vt / currentMaxAxisValue) : 0f;
            float ntR = currentMaxAxisValue > 0 ? (float) (entry.nt / currentMaxAxisValue) : 0f;
            targetRatios.add(new BarHeightRatio(vtR, ntR));

            if (i < currentDisplayed.size()) {
                startRatios.add(currentDisplayed.get(i));
            } else {
                startRatios.add(new BarHeightRatio(0f, 0f));
            }
        }

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (!animate || !isAttachedToWindow() || entries.isEmpty()) {
            animFraction = 1f;
            invalidate();
            return;
        }

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
    private List<BarHeightRatio> getCurrentInterpolatedRatios() {
        int count = targetRatios.size();
        List<BarHeightRatio> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new BarHeightRatio(getInterpolatedVtRatio(i), getInterpolatedNtRatio(i)));
        }
        return result;
    }


    private float getInterpolatedVtRatio(int index) {
        if (index < 0 || index >= targetRatios.size()) {
            return 0f;
        }
        float to = targetRatios.get(index).vtRatio;
        float from = (index < startRatios.size()) ? startRatios.get(index).vtRatio : 0f;
        return from + (to - from) * animFraction;
    }


    private float getInterpolatedNtRatio(int index) {
        if (index < 0 || index >= targetRatios.size()) {
            return 0f;
        }
        float to = targetRatios.get(index).ntRatio;
        float from = (index < startRatios.size()) ? startRatios.get(index).ntRatio : 0f;
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

        int step = currentStep;
        int maxAxisValue = currentMaxAxisValue;

        String maxLabel = String.valueOf(maxAxisValue);
        float leftAxisWidth = Math.max(dpToPx(getContext(), 32f), paintAxisText.measureText(maxLabel) + dpToPx(getContext(), 10f));

        float chartLeft = leftAxisWidth;
        float chartRight = w - legendWidth - dpToPx(getContext(), 8f);
        float chartTop = topPad;
        float chartBottom = h - bottomPad;
        float chartHeight = Math.max(1f, chartBottom - chartTop);
        float chartWidth = Math.max(1f, chartRight - chartLeft);

        // 1. Vodorovné linky mřížky a popisky osy Y
        Paint.FontMetrics axisFm = paintAxisText.getFontMetrics();
        float axisTextOffset = -(axisFm.ascent + axisFm.descent) / 2f;
        for (int i = 0; i <= 4; i++) {
            int val = i * step;
            float ratio = (float) val / (float) maxAxisValue;
            float y = chartBottom - ratio * chartHeight;
            canvas.drawLine(chartLeft, y, chartRight, y, paintGrid);
            canvas.drawText(String.valueOf(val), chartLeft - dpToPx(getContext(), 6f), y + axisTextOffset, paintAxisText);
        }

        // 2. Vykreslení sloupců (VT dole, NT nahoře) s plynulou animací výšky
        if (!entries.isEmpty()) {
            int count = entries.size();
            float slotWidth = chartWidth / count;
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

            for (int i = 0; i < count; i++) {
                BarEntry entry = entries.get(i);
                float vtRatio = getInterpolatedVtRatio(i);
                float ntRatio = getInterpolatedNtRatio(i);

                float slotCenterX = chartLeft + (i + 0.5f) * slotWidth;
                float barLeft = slotCenterX - barWidth / 2f;
                float barRight = slotCenterX + barWidth / 2f;

                float vtHeight = Math.max(0f, vtRatio * chartHeight);
                float ntHeight = Math.max(0f, ntRatio * chartHeight);
                float totalHeight = Math.min(chartHeight, vtHeight + ntHeight);

                if (totalHeight > 0.5f) {
                    float barTop = chartBottom - totalHeight;
                    rectBuffer.set(barLeft, barTop, barRight, chartBottom);

                    barClipPath.reset();
                    barClipPath.addRoundRect(rectBuffer, barCornerRadii, Path.Direction.CW);

                    int save = canvas.save();
                    canvas.clipPath(barClipPath);

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

                // Popisek na ose X
                canvas.drawText(entry.label, slotCenterX, chartBottom + dpToPx(getContext(), 15f), paintLabelText);
            }
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
