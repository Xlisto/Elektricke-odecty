package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.utils.BuilderHDOStack;


/**
 * Kompaktní 24hodinové analogové hodiny zobrazující dnešní intervaly HDO a aktuální čas.
 * Slouží jako prostřední dlaždice na nové přehledové obrazovce.
 */
public class DashboardHdoClockView extends View {

    private static final int[] HOURS_8 = {0, 3, 6, 9, 12, 15, 18, 21};
    private static final int[] HOURS_4 = {0, 6, 12, 18};

    private Paint paintDialBg;
    private Paint paintDialStroke;
    private Paint paintHdoSector;
    private Paint paintTick;
    private Paint paintNumbers;
    private Paint paintHand;
    private Paint paintCenterDot;
    private final RectF arcRect = new RectF();
    private final Calendar todayCal = Calendar.getInstance();

    private List<HdoModel> weekModels = new ArrayList<>();
    private long timeShift = 0L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 30_000L);
        }
    };


    public DashboardHdoClockView(Context context) {
        super(context);
        init();
    }


    public DashboardHdoClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public DashboardHdoClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        int colorPrimaryTeal = ContextCompat.getColor(getContext(), R.color.dashboard_teal_vt);
        int colorLightTeal = ContextCompat.getColor(getContext(), R.color.dashboard_teal_nt);
        int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);
        int colorSurfaceBg = ContextCompat.getColor(getContext(), R.color.dashboard_price_card_start);

        paintDialBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDialBg.setStyle(Paint.Style.FILL);
        paintDialBg.setColor(colorSurfaceBg);

        paintDialStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDialStroke.setStyle(Paint.Style.STROKE);
        paintDialStroke.setStrokeWidth(dpToPx(getContext(), 1.5f));
        paintDialStroke.setColor(colorPrimaryTeal);

        paintHdoSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHdoSector.setStyle(Paint.Style.FILL);
        paintHdoSector.setColor(colorLightTeal);
        paintHdoSector.setAlpha(185);

        paintTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTick.setStyle(Paint.Style.STROKE);
        paintTick.setStrokeWidth(dpToPx(getContext(), 1f));
        paintTick.setColor(colorOnSurface);
        paintTick.setAlpha(150);

        paintNumbers = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNumbers.setStyle(Paint.Style.FILL);
        paintNumbers.setColor(colorOnSurface);
        paintNumbers.setTextAlign(Paint.Align.CENTER);
        paintNumbers.setTextSize(dpToPx(getContext(), 7.5f));
        paintNumbers.setFakeBoldText(true);

        paintHand = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHand.setStyle(Paint.Style.STROKE);
        paintHand.setStrokeWidth(dpToPx(getContext(), 1.8f));
        paintHand.setStrokeCap(Paint.Cap.ROUND);
        paintHand.setColor(0xFFD32F2F);

        paintCenterDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenterDot.setStyle(Paint.Style.FILL);
        paintCenterDot.setColor(0xFFD32F2F);
    }


    private int resolveAttrColor(int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrRes, tv, true)) {
            return tv.data;
        }
        return fallback;
    }


    /**
     * Nastaví seznam HDO modelů a časový posun elektroměru.
     */
    public void setHdoData(@Nullable List<HdoModel> models, long timeShiftMillis) {
        this.timeShift = timeShiftMillis;
        ArrayList<HdoModel> rawModels = models != null ? new ArrayList<>(models) : new ArrayList<>();
        if (!rawModels.isEmpty()) {
            this.weekModels = BuilderHDOStack.build(rawModels, timeShiftMillis);
        } else {
            this.weekModels = new ArrayList<>();
        }
        invalidate();
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 30_000L);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
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

        float cx = w / 2f;
        float cy = h / 2f;
        float pad = dpToPx(getContext(), 2f);
        float radius = (size / 2f) - pad;

        // 1. Pozadí ciferníku
        canvas.drawCircle(cx, cy, radius, paintDialBg);

        // 2. Výseče 24h HDO pro dnešní den
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        todayCal.setTimeInMillis(System.currentTimeMillis() + timeShift);

        if (weekModels != null && !weekModels.isEmpty()) {
            for (HdoModel model : weekModels) {
                if (isSameDay(model.getCalendarStart(), todayCal)) {
                    float startAngle = convertTimeToAngle(model.getTimeFrom());
                    float endAngle = convertTimeToAngle(model.getTimeUntil());
                    float rawSweep = endAngle - startAngle;
                    float sweepAngle = rawSweep <= 0 ? rawSweep + 360f : rawSweep;
                    canvas.drawArc(arcRect, startAngle, sweepAngle, true, paintHdoSector);
                }
            }
        }

        // 3. Značky po 1 hodině (24 dílků)
        float majorTickLen = dpToPx(getContext(), 3.5f);
        float minorTickLen = dpToPx(getContext(), 2f);
        for (int i = 0; i < 24; i++) {
            double rad = Math.toRadians(270 + i * 15);
            float tickLen = (i % 3 == 0) ? majorTickLen : minorTickLen;
            float sx = (float) (cx + radius * Math.cos(rad));
            float sy = (float) (cy + radius * Math.sin(rad));
            float ex = (float) (cx + (radius - tickLen) * Math.cos(rad));
            float ey = (float) (cy + (radius - tickLen) * Math.sin(rad));
            canvas.drawLine(sx, sy, ex, ey, paintTick);
        }

        // 4. Čísla ciferníku (0, 6, 12, 18 nebo násobky 3 podle velikosti)
        int[] hoursToShow = (radius >= dpToPx(getContext(), 26)) ? HOURS_8 : HOURS_4;
        float textRadius = radius - majorTickLen - dpToPx(getContext(), 5.5f);
        Paint.FontMetrics fm = paintNumbers.getFontMetrics();
        float textVerticalOffset = -(fm.ascent + fm.descent) / 2f;

        for (int hour : hoursToShow) {
            double rad = Math.toRadians(270 + hour * 15);
            float tx = (float) (cx + textRadius * Math.cos(rad));
            float ty = (float) (cy + textRadius * Math.sin(rad)) + textVerticalOffset;
            canvas.drawText(String.valueOf(hour), tx, ty, paintNumbers);
        }

        // 5. Obrys ciferníku
        canvas.drawCircle(cx, cy, radius, paintDialStroke);

        // 6. Ručička aktuálního času (24h režim)
        int hours = todayCal.get(Calendar.HOUR_OF_DAY);
        int minutes = todayCal.get(Calendar.MINUTE);
        double handAngleRad = Math.toRadians(270 + hours * 15 + minutes * 0.25);
        float handLen = radius - dpToPx(getContext(), 5f);
        float hx = (float) (cx + handLen * Math.cos(handAngleRad));
        float hy = (float) (cy + handLen * Math.sin(handAngleRad));
        canvas.drawLine(cx, cy, hx, hy, paintHand);
        canvas.drawCircle(cx, cy, dpToPx(getContext(), 2.2f), paintCenterDot);
    }


    private boolean isSameDay(@Nullable Calendar c1, @Nullable Calendar c2) {
        if (c1 == null || c2 == null) {
            return false;
        }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }


    private float convertTimeToAngle(@Nullable String time) {
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
