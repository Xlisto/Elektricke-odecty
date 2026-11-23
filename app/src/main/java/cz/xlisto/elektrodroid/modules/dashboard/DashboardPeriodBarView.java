package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import cz.xlisto.elektrodroid.R;


/**
 * Horizontální dvoubarevný pruh zobrazující poměr spotřeby VT a NT v aktuálním období.
 */
public class DashboardPeriodBarView extends View {

    private Paint paintVt;
    private Paint paintNt;
    private Paint paintEmpty;
    private final Path clipPath = new Path();
    private final RectF fullRect = new RectF();
    private final RectF segRect = new RectF();

    private double vt = 0.0;
    private double nt = 0.0;


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
     * Nastaví hodnoty spotřeby VT a NT.
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
        if (w <= 0 || h <= 0) {
            return;
        }

        float corner = h / 2f;
        fullRect.set(0, 0, w, h);

        double total = vt + nt;
        if (total <= 0.0) {
            canvas.drawRoundRect(fullRect, corner, corner, paintEmpty);
            return;
        }

        clipPath.reset();
        clipPath.addRoundRect(fullRect, corner, corner, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clipPath);

        float gap = (vt > 0 && nt > 0) ? dpToPx(getContext(), 2f) : 0f;
        float availableWidth = Math.max(0f, w - gap);
        float vtWidth = (float) ((vt / total) * availableWidth);

        if (vt > 0) {
            segRect.set(0, 0, vtWidth, h);
            canvas.drawRect(segRect, paintVt);
        }
        if (nt > 0) {
            segRect.set(vtWidth + gap, 0, w, h);
            canvas.drawRect(segRect, paintNt);
        }

        canvas.restoreToCount(save);
    }

}
