package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import cz.xlisto.elektrodroid.R;


/**
 * Horizontální dvoubarevný pruh zobrazující poměr spotřeby VT a NT v aktuálním období
 * s plynulou animací posunu poměru při změně dat.
 */
public class DashboardPeriodBarView extends View {

    private static final long ANIMATION_DURATION_MS = 550L;

    private Paint paintVt;
    private Paint paintNt;
    private Paint paintEmpty;
    private final Path clipPath = new Path();
    private final RectF fullRect = new RectF();
    private final RectF segRect = new RectF();

    private float displayedVtRatio = 0.5f;
    private float displayedFillFraction = 0f;
    private boolean hasInitializedRatio = false;
    private ValueAnimator animator;


    public DashboardPeriodBarView(Context context) {
        super(context);
        init();
    }


    public DashboardPeriodBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public DashboardPeriodBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        paintVt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintVt.setStyle(Paint.Style.FILL);
        paintVt.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_teal_vt));

        paintNt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNt.setStyle(Paint.Style.FILL);
        paintNt.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_teal_nt));

        paintEmpty = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEmpty.setStyle(Paint.Style.FILL);
        paintEmpty.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_divider));
    }


    /**
     * Nastaví hodnoty spotřeby VT a NT a plynule odanimuje změnu poměru.
     */
    public void setConsumption(double vtValue, double ntValue) {
        setConsumption(vtValue, ntValue, true);
    }


    /**
     * Nastaví hodnoty spotřeby VT a NT s možností volby animace.
     */
    public void setConsumption(double vtValue, double ntValue, boolean animate) {
        double cleanVt = Math.max(0.0, vtValue);
        double cleanNt = Math.max(0.0, ntValue);
        double total = cleanVt + cleanNt;

        final float targetFill = total > 0.0 ? 1f : 0f;
        final float targetVtRatio = total > 0.0 ? (float) (cleanVt / total) : displayedVtRatio;

        if (!hasInitializedRatio && total > 0.0) {
            displayedVtRatio = targetVtRatio;
            hasInitializedRatio = true;
        }

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (!animate || !isAttachedToWindow()) {
            displayedVtRatio = targetVtRatio;
            displayedFillFraction = targetFill;
            hasInitializedRatio = total > 0.0;
            invalidate();
            return;
        }

        final float startVtRatio = displayedVtRatio;
        final float startFill = displayedFillFraction;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            displayedVtRatio = startVtRatio + (targetVtRatio - startVtRatio) * fraction;
            displayedFillFraction = startFill + (targetFill - startFill) * fraction;
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
        if (w <= 0 || h <= 0) {
            return;
        }

        float corner = h / 2f;
        fullRect.set(0, 0, w, h);

        // Podklad prázdného pruhu
        canvas.drawRoundRect(fullRect, corner, corner, paintEmpty);

        if (displayedFillFraction <= 0.005f) {
            return;
        }

        clipPath.reset();
        clipPath.addRoundRect(fullRect, corner, corner, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clipPath);

        float filledWidth = w * Math.min(1f, Math.max(0f, displayedFillFraction));
        float clampedVtRatio = Math.min(1f, Math.max(0f, displayedVtRatio));
        boolean hasBoth = clampedVtRatio > 0.005f && clampedVtRatio < 0.995f;
        float gap = hasBoth ? dpToPx(getContext(), 2f) : 0f;
        float availableWidth = Math.max(0f, filledWidth - gap);
        float vtWidth = clampedVtRatio * availableWidth;

        if (vtWidth > 0.5f) {
            segRect.set(0, 0, vtWidth, h);
            canvas.drawRect(segRect, paintVt);
        }
        float ntLeft = vtWidth + gap;
        if (filledWidth > ntLeft + 0.5f) {
            segRect.set(ntLeft, 0, filledWidth, h);
            canvas.drawRect(segRect, paintNt);
        }

        canvas.restoreToCount(save);
    }

}
