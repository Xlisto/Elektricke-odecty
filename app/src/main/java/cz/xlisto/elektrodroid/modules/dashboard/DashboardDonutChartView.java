package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;


/**
 * Kruhový prstencový graf (donut chart) pro kartu "Spotřeba období" se zobrazením celkové spotřeby v kWh uprostřed.
 */
public class DashboardDonutChartView extends View {

    private Paint paintVt;
    private Paint paintNt;
    private Paint paintEmpty;
    private Paint paintValueText;
    private Paint paintUnitText;
    private final RectF arcRect = new RectF();

    private double vt = 0.0;
    private double nt = 0.0;
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
        int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);

        paintVt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintVt.setStyle(Paint.Style.STROKE);
        paintVt.setStrokeWidth(strokeWidth);
        paintVt.setStrokeCap(Paint.Cap.BUTT);
        paintVt.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_teal_vt));

        paintNt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNt.setStyle(Paint.Style.STROKE);
        paintNt.setStrokeWidth(strokeWidth);
        paintNt.setStrokeCap(Paint.Cap.BUTT);
        paintNt.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_teal_nt));

        paintEmpty = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEmpty.setStyle(Paint.Style.STROKE);
        paintEmpty.setStrokeWidth(strokeWidth);
        paintEmpty.setColor(ContextCompat.getColor(getContext(), R.color.dashboard_divider));

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


    private int resolveAttrColor(int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrRes, tv, true)) {
            return tv.data;
        }
        return fallback;
    }


    /**
     * Nastaví spotřebu VT a NT v kWh.
     */
    public void setConsumption(double vtValue, double ntValue) {
        this.vt = Math.max(0.0, vtValue);
        this.nt = Math.max(0.0, ntValue);
        invalidate();
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

        double total = vt + nt;
        if (total <= 0.0) {
            canvas.drawArc(arcRect, 0f, 360f, false, paintEmpty);
        } else {
            float vtSweep = (float) ((vt / total) * 360.0);
            float ntSweep = 360f - vtSweep;
            float gapDegrees = (vt > 0 && nt > 0) ? 3f : 0f;

            if (vt > 0) {
                canvas.drawArc(arcRect, -90f + gapDegrees / 2f, Math.max(0f, vtSweep - gapDegrees), false, paintVt);
            }
            if (nt > 0) {
                canvas.drawArc(arcRect, -90f + vtSweep + gapDegrees / 2f, Math.max(0f, ntSweep - gapDegrees), false, paintNt);
            }
        }

        String totalStr = integerFormat.format(Math.round(total));
        canvas.drawText(totalStr, cx, cy - dpToPx(getContext(), 1f), paintValueText);
        canvas.drawText("kWh", cx, cy + dpToPx(getContext(), 12f), paintUnitText);
    }

}
