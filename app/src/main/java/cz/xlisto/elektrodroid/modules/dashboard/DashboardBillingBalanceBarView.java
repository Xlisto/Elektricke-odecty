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
 * Horizontální bilaterální ukazatel se středovou nulovou ryskou zobrazující poměr mezi
 * zaplacenými zálohami a spotřebovanými náklady.
 * <p>
 * Od středu ({@code 0}) roste zelený pruh doprava při přeplatku (zálohy &gt; náklady)
 * a červený pruh doleva při nedoplatku (náklady &gt; zálohy), s plynulou animací při změně dat.
 */
public class DashboardBillingBalanceBarView extends View {

    private static final long ANIMATION_DURATION_MS = 550L;
    private static final double EPSILON = 0.0001;

    private Paint paintTrack;
    private Paint paintOverpayment;
    private Paint paintUnderpayment;
    private Paint paintCenterTick;

    private final Path clipPath = new Path();
    private final Path fillPath = new Path();
    private final RectF trackRect = new RectF();
    private final RectF fillRect = new RectF();
    private final RectF centerTickRect = new RectF();
    private final float[] rightRoundedRadii = new float[8];
    private final float[] leftRoundedRadii = new float[8];

    /**
     * Aktuálně vykreslený znaménkový poměr v intervalu {@code [-1f, +1f]}.
     * Kladné hodnoty značí přeplatek (vpravo), záporné nedoplatek (vlevo), {@code 0f} vyrovnáno.
     */
    private float displayedSignedRatio = 0f;
    private ValueAnimator animator;


    public DashboardBillingBalanceBarView(Context context) {
        super(context);
        init();
    }


    public DashboardBillingBalanceBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public DashboardBillingBalanceBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        paintTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTrack.setStyle(Paint.Style.FILL);
        paintTrack.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_divider));

        paintOverpayment = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintOverpayment.setStyle(Paint.Style.FILL);
        paintOverpayment.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_overpayment_text));

        paintUnderpayment = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintUnderpayment.setStyle(Paint.Style.FILL);
        paintUnderpayment.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_underpayment_text));

        paintCenterTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenterTick.setStyle(Paint.Style.FILL);
        paintCenterTick.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_chart_percent_neutral));
    }


    /**
     * Vypočte normalizovaný znaménkový poměr salda vůči větší z obou částek v intervalu {@code [-1f, +1f]}.
     * <ul>
     *     <li>{@code +1f}: nulové náklady při kladných zálohách (100 % přeplatek)</li>
     *     <li>{@code 0f}: vyrovnáno nebo obě částky nulové</li>
     *     <li>{@code -1f}: nulové zálohy při kladných nákladech (100 % nedoplatek)</li>
     * </ul>
     *
     * @param advancesKc uhrazené zálohy v Kč
     * @param costsKc    celkové náklady v Kč
     * @return normalizovaný znaménkový poměr v rozmezí {@code -1f..+1f}
     */
    public static float calculateSignedRatio(double advancesKc, double costsKc) {
        double cleanAdvances = Math.max(0.0, advancesKc);
        double cleanCosts = Math.max(0.0, costsKc);
        double maxVal = Math.max(cleanAdvances, cleanCosts);
        if (maxVal <= EPSILON) {
            return 0f;
        }
        double ratio = (cleanAdvances - cleanCosts) / maxVal;
        return (float) Math.max(-1.0, Math.min(1.0, ratio));
    }


    /**
     * Vypočte procentuální podíl nákladů vůči zaplaceným zálohám (zaokrouhleno na celá procenta).
     *
     * @param advancesKc uhrazené zálohy v Kč
     * @param costsKc    celkové náklady v Kč
     * @return procento nákladů vůči zálohám, {@code 0} při nulových obou částkách,
     * nebo {@code -1}, pokud jsou náklady kladné při nulových zálohách
     */
    public static int calculateCostsToAdvancesPercent(double advancesKc, double costsKc) {
        double cleanAdvances = Math.max(0.0, advancesKc);
        double cleanCosts = Math.max(0.0, costsKc);
        if (cleanAdvances <= EPSILON) {
            return cleanCosts > EPSILON ? -1 : 0;
        }
        return (int) Math.round((cleanCosts / cleanAdvances) * 100.0);
    }


    /**
     * Vypočte znaménkové procento přeplatku ({@code > 0}) či nedoplatku ({@code < 0})
     * vůči zaplaceným zálohám tak, aby bylo vždy konzistentní s {@link #calculateCostsToAdvancesPercent}.
     *
     * @param advancesKc uhrazené zálohy v Kč
     * @param costsKc    celkové náklady v Kč
     * @return kladné procento pro přeplatek, záporné pro nedoplatek, {@code 0} pro vyrovnáno
     * nebo {@code -100}, pokud jsou náklady kladné při nulových zálohách
     */
    public static int calculateBalancePercent(double advancesKc, double costsKc) {
        double cleanAdvances = Math.max(0.0, advancesKc);
        double cleanCosts = Math.max(0.0, costsKc);
        if (cleanAdvances <= EPSILON) {
            return cleanCosts > EPSILON ? -100 : 0;
        }
        return 100 - calculateCostsToAdvancesPercent(cleanAdvances, cleanCosts);
    }


    /**
     * Nastaví hodnoty záloh a nákladů a plynule odanimuje posun ukazatele od středu.
     *
     * @param advancesKc uhrazené zálohy v Kč
     * @param costsKc    celkové náklady v Kč
     */
    public void setValues(double advancesKc, double costsKc) {
        setValues(advancesKc, costsKc, true);
    }


    /**
     * Nastaví hodnoty záloh a nákladů s možností volby animace.
     *
     * @param advancesKc uhrazené zálohy v Kč
     * @param costsKc    celkové náklady v Kč
     * @param animate    {@code true} pro plynulou animaci, {@code false} pro okamžité překreslení
     */
    public void setValues(double advancesKc, double costsKc, boolean animate) {
        final float targetRatio = calculateSignedRatio(advancesKc, costsKc);

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (!animate || !isAttachedToWindow()) {
            displayedSignedRatio = targetRatio;
            invalidate();
            return;
        }

        final float startRatio = displayedSignedRatio;
        if (Math.abs(targetRatio - startRatio) < 0.0005f) {
            displayedSignedRatio = targetRatio;
            invalidate();
            return;
        }

        animator = ValueAnimator.ofFloat(startRatio, targetRatio);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            displayedSignedRatio = (float) anim.getAnimatedValue();
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

        float trackHeight = Math.min(h, dpToPx(getContext(), 10f));
        float trackTop = (h - trackHeight) / 2f;
        float trackBottom = trackTop + trackHeight;
        float trackCorner = trackHeight / 2f;
        float centerX = w / 2f;
        float halfWidth = w / 2f;

        // Podkladový pruh
        trackRect.set(0f, trackTop, w, trackBottom);
        canvas.drawRoundRect(trackRect, trackCorner, trackCorner, paintTrack);

        // Vyplnění od středu doprava (přeplatek) nebo doleva (nedoplatek)
        float clampedRatio = Math.max(-1f, Math.min(1f, displayedSignedRatio));
        float absRatio = Math.abs(clampedRatio);
        if (absRatio > 0.002f) {
            float minFillWidth = dpToPx(getContext(), 2.5f);
            float fillWidth = Math.min(halfWidth, Math.max(minFillWidth, halfWidth * absRatio));

            clipPath.reset();
            clipPath.addRoundRect(trackRect, trackCorner, trackCorner, Path.Direction.CW);

            int save = canvas.save();
            canvas.clipPath(clipPath);

            fillPath.reset();
            float outerRadius = Math.min(trackCorner, fillWidth / 2f);
            if (clampedRatio > 0f) {
                fillRect.set(centerX, trackTop, centerX + fillWidth, trackBottom);
                rightRoundedRadii[0] = 0f;
                rightRoundedRadii[1] = 0f;
                rightRoundedRadii[2] = outerRadius;
                rightRoundedRadii[3] = outerRadius;
                rightRoundedRadii[4] = outerRadius;
                rightRoundedRadii[5] = outerRadius;
                rightRoundedRadii[6] = 0f;
                rightRoundedRadii[7] = 0f;
                fillPath.addRoundRect(fillRect, rightRoundedRadii, Path.Direction.CW);
                canvas.drawPath(fillPath, paintOverpayment);
            } else {
                fillRect.set(centerX - fillWidth, trackTop, centerX, trackBottom);
                leftRoundedRadii[0] = outerRadius;
                leftRoundedRadii[1] = outerRadius;
                leftRoundedRadii[2] = 0f;
                leftRoundedRadii[3] = 0f;
                leftRoundedRadii[4] = 0f;
                leftRoundedRadii[5] = 0f;
                leftRoundedRadii[6] = outerRadius;
                leftRoundedRadii[7] = outerRadius;
                fillPath.addRoundRect(fillRect, leftRoundedRadii, Path.Direction.CW);
                canvas.drawPath(fillPath, paintUnderpayment);
            }

            canvas.restoreToCount(save);
        }

        // Středová nulová ryska vykreslená nad pruhem
        float tickWidth = dpToPx(getContext(), 2f);
        float tickCorner = tickWidth / 2f;
        centerTickRect.set(centerX - tickWidth / 2f, 0f, centerX + tickWidth / 2f, h);
        canvas.drawRoundRect(centerTickRect, tickCorner, tickCorner, paintCenterTick);
    }

}
