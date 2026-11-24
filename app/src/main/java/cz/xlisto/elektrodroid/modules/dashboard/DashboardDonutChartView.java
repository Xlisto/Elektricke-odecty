package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;


/**
 * Kruhový prstencový graf (donut chart) pro kartu "Spotřeba období"
 * se zobrazením celkové spotřeby v kWh uprostřed a plynulou animací výsečí při změně dat.
 */
public class DashboardDonutChartView extends View {

    private static final long ANIMATION_DURATION_MS = 550L;

    private Paint paintVt;
    private Paint paintNt;
    private Paint paintEmpty;
    private Paint paintValueText;
    private Paint paintUnitText;
    private final RectF arcRect = new RectF();

    private float displayedVtRatio = 0.5f;
    private float displayedTotalSweep = 0f;
    private double displayedTotalValue = 0.0;
    private boolean hasInitializedRatio = false;
    private ValueAnimator animator;

    private final DecimalFormat integerFormat;


    public DashboardDonutChartView(Context context) {
        super(context);
        integerFormat = createFormat();
        init();
    }


    public DashboardDonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        integerFormat = createFormat();
        init();
    }


    public DashboardDonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        integerFormat = createFormat();
        init();
    }


    private DecimalFormat createFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("cs-CZ"));
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        df.setGroupingUsed(true);
        return df;
    }


    private void init() {
        float strokeWidth = dpToPx(getContext(), 11f);
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
        int colorOutlineVariant = MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOutlineVariant,
                ContextCompat.getColor(getContext(), R.color.dashboard_divider)
        );

        paintVt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintVt.setStyle(Paint.Style.STROKE);
        paintVt.setStrokeWidth(strokeWidth);
        paintVt.setStrokeCap(Paint.Cap.BUTT);
        paintVt.setColor(MaterialColors.harmonizeWithPrimary(
                getContext(),
                ContextCompat.getColor(getContext(), R.color.dashboard_teal_vt)
        ));

        paintNt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNt.setStyle(Paint.Style.STROKE);
        paintNt.setStrokeWidth(strokeWidth);
        paintNt.setStrokeCap(Paint.Cap.BUTT);
        paintNt.setColor(colorPrimary);

        paintEmpty = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEmpty.setStyle(Paint.Style.STROKE);
        paintEmpty.setStrokeWidth(strokeWidth);
        paintEmpty.setColor(colorOutlineVariant);

        paintValueText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintValueText.setStyle(Paint.Style.FILL);
        paintValueText.setColor(colorOnSurface);
        paintValueText.setTextAlign(Paint.Align.CENTER);
        paintValueText.setFakeBoldText(true);
        paintValueText.setTextSize(dpToPx(getContext(), 14f));

        paintUnitText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintUnitText.setStyle(Paint.Style.FILL);
        paintUnitText.setColor(colorOnSurface);
        paintUnitText.setAlpha(180);
        paintUnitText.setTextAlign(Paint.Align.CENTER);
        paintUnitText.setTextSize(dpToPx(getContext(), 11f));
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
     * Nastaví spotřebu VT a NT v kWh a plynule odanimuje přechod výsečí i středové hodnoty.
     */
    public void setConsumption(double vtValue, double ntValue) {
        setConsumption(vtValue, ntValue, true);
    }


    /**
     * Nastaví spotřebu VT a NT v kWh s možností volby animace.
     */
    public void setConsumption(double vtValue, double ntValue, boolean animate) {
        double cleanVt = Math.max(0.0, vtValue);
        double cleanNt = Math.max(0.0, ntValue);
        final double targetTotal = cleanVt + cleanNt;
        final float targetSweep = targetTotal > 0.0 ? 360f : 0f;
        final float targetVtRatio = targetTotal > 0.0 ? (float) (cleanVt / targetTotal) : displayedVtRatio;

        if (!hasInitializedRatio && targetTotal > 0.0) {
            displayedVtRatio = targetVtRatio;
            hasInitializedRatio = true;
        }

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (!animate || !isAttachedToWindow()) {
            displayedVtRatio = targetVtRatio;
            displayedTotalSweep = targetSweep;
            displayedTotalValue = targetTotal;
            hasInitializedRatio = targetTotal > 0.0;
            invalidate();
            return;
        }

        final float startVtRatio = displayedVtRatio;
        final float startSweep = displayedTotalSweep;
        final double startTotal = displayedTotalValue;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            displayedVtRatio = startVtRatio + (targetVtRatio - startVtRatio) * fraction;
            displayedTotalSweep = startSweep + (targetSweep - startSweep) * fraction;
            displayedTotalValue = startTotal + (targetTotal - startTotal) * fraction;
            invalidate();
        });
        animator.start();
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
        int size = Math.min(w, h);
        if (size <= 0) {
            return;
        }

        float stroke = paintVt.getStrokeWidth();
        float pad = stroke / 2f + dpToPx(getContext(), 2f);
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = (size / 2f) - pad;

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // Podkladový kruh
        canvas.drawArc(arcRect, 0f, 360f, false, paintEmpty);

        if (displayedTotalSweep > 0.5f) {
            float clampedRatio = Math.min(1f, Math.max(0f, displayedVtRatio));
            float vtSweep = displayedTotalSweep * clampedRatio;
            float ntSweep = displayedTotalSweep - vtSweep;
            boolean hasBoth = vtSweep > 1.5f && ntSweep > 1.5f;
            float gapDegrees = hasBoth ? 3f : 0f;

            if (vtSweep > 0.5f) {
                canvas.drawArc(
                        arcRect,
                        -90f + gapDegrees / 2f,
                        Math.max(0f, vtSweep - gapDegrees),
                        false,
                        paintVt
                );
            }
            if (ntSweep > 0.5f) {
                canvas.drawArc(
                        arcRect,
                        -90f + vtSweep + gapDegrees / 2f,
                        Math.max(0f, ntSweep - gapDegrees),
                        false,
                        paintNt
                );
            }
        }

        String totalStr = integerFormat.format(Math.round(Math.max(0.0, displayedTotalValue)));
        canvas.drawText(totalStr, cx, cy - dpToPx(getContext(), 1f), paintValueText);
        canvas.drawText("kWh", cx, cy + dpToPx(getContext(), 12f), paintUnitText);
    }

}
