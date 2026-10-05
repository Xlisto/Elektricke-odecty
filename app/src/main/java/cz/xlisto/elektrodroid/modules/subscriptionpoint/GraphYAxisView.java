package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;

import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.utils.DensityUtils;
import cz.xlisto.elektrodroid.utils.DetectNightMode;


/**
 * Vlastní View komponenta pro pevné zobrazení svislé osy Y (levá Y-osa pro spotřebu MWh/kWh nebo pravá Y-osa pro ceny Kč/MWh).
 */
public class GraphYAxisView extends View {

    public static final int MODE_LEFT_CONSUMPTION = 0;
    public static final int MODE_RIGHT_PRICE = 1;

    private int mode = MODE_LEFT_CONSUMPTION;
    private boolean isMWh = true;
    private double maxConsumption = 4.0;
    private double maxPrice = 10000.0;

    private Paint paintText;
    private Paint paintAxis;
    private Paint paintBgCard;

    private final RectF chartArea = new RectF();
    private final RectF bgCardRect = new RectF();


    public GraphYAxisView(Context context) {
        super(context);
        init(context);
    }


    public GraphYAxisView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public GraphYAxisView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        boolean isDark = DetectNightMode.isNightMode(context);
        int bgColor = isDark ? 0xFF1E1E1E : 0xFFF5F5F5;
        int textColor = isDark ? 0xFFEEEEEE : 0xFF212121;

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(textColor);
        paintText.setTextSize(DensityUtils.dpToPx(context, 11));

        paintAxis = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAxis.setColor(isDark ? 0xFF666666 : 0xFFCCCCCC);
        paintAxis.setStrokeWidth(DensityUtils.dpToPx(context, 1));

        paintBgCard = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBgCard.setColor(bgColor);
        paintBgCard.setStyle(Paint.Style.FILL);
    }


    public void setAxisMode(int mode) {
        this.mode = mode;
        invalidate();
    }


    public void updateFromData(ArrayList<AnnualYearData> yearDataList) {
        if (yearDataList == null || yearDataList.isEmpty()) {
            return;
        }

        double maxConsKwh = 0;
        double maxPriceVal = 0;

        for (AnnualYearData d : yearDataList) {
            double totalKwh = d.getTotalConsumption();
            if (totalKwh > maxConsKwh) {
                maxConsKwh = totalKwh;
            }
            maxPriceVal = Math.max(maxPriceVal, d.avgPriceTotal());
            maxPriceVal = Math.max(maxPriceVal, d.avgPriceVT());
            if (d.isDualTariff()) {
                maxPriceVal = Math.max(maxPriceVal, d.avgPriceNT());
            }
        }

        this.isMWh = maxConsKwh >= 1000.0;
        double maxVal = isMWh ? (maxConsKwh / 1000.0) : maxConsKwh;
        if (maxVal <= 0) maxVal = 1.0;
        this.maxConsumption = Math.ceil(maxVal * 1.15);

        if (maxPriceVal <= 0) maxPriceVal = 5000;
        this.maxPrice = Math.ceil((maxPriceVal * 1.15) / 1000.0) * 1000.0;
        if (this.maxPrice < 2000) this.maxPrice = 2000;

        invalidate();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = DensityUtils.dpToPx(getContext(), 280);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int calculatedHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            calculatedHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            calculatedHeight = Math.min(desiredHeight, heightSize);
        } else {
            calculatedHeight = desiredHeight;
        }

        int defaultWidth = DensityUtils.dpToPx(getContext(), mode == MODE_LEFT_CONSUMPTION ? 40 : 52);
        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(widthMeasureSpec) : defaultWidth;

        setMeasuredDimension(width, calculatedHeight);
    }


    private boolean drawCardBackground = true;


    public void setDrawCardBackground(boolean drawCardBackground) {
        this.drawCardBackground = drawCardBackground;
        invalidate();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Pozadí karty (pouze pokud je povolené)
        if (drawCardBackground) {
            float rx = DensityUtils.dpToPx(getContext(), 5);
            bgCardRect.set(0, 0, width, height);
            canvas.drawRoundRect(bgCardRect, rx, rx, paintBgCard);
        }

        float padTop = DensityUtils.dpToPx(getContext(), 42);
        float padBottom = DensityUtils.dpToPx(getContext(), 40);

        chartArea.set(0, padTop, width, height - padBottom);

        int steps = 4;
        Locale locale = getContext().getResources().getConfiguration().getLocales().get(0);
        float axisGap = DensityUtils.dpToPx(getContext(), 6);

        if (mode == MODE_LEFT_CONSUMPTION) {
            // Název jednotky nahoře
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setFakeBoldText(true);
            canvas.drawText(isMWh ? "MWh" : "kWh", 6, padTop - DensityUtils.dpToPx(getContext(), 20), paintText);
            paintText.setFakeBoldText(false);

            // Popisky osy Y - spotřeba
            float axisX = width - 1;
            canvas.drawLine(axisX, chartArea.top, axisX, chartArea.bottom, paintAxis);

            for (int i = 0; i <= steps; i++) {
                float y = chartArea.bottom - (chartArea.height() * i / steps);
                float textY;
                if (i == steps) {
                    textY = y + DensityUtils.dpToPx(getContext(), 10);
                } else if (i == 0) {
                    textY = y - DensityUtils.dpToPx(getContext(), 2);
                } else {
                    textY = y + DensityUtils.dpToPx(getContext(), 4);
                }

                double consVal = (maxConsumption * i / steps);
                paintText.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(DecimalFormatHelper.df1.format(consVal), axisX - axisGap, textY, paintText);
            }
        } else {
            // Název jednotky nahoře
            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setFakeBoldText(true);
            canvas.drawText("Kč/MWh", width - 6, padTop - DensityUtils.dpToPx(getContext(), 20), paintText);
            paintText.setFakeBoldText(false);

            // Popisky osy Y - cena
            float axisX = 1;
            canvas.drawLine(axisX, chartArea.top, axisX, chartArea.bottom, paintAxis);

            for (int i = 0; i <= steps; i++) {
                float y = chartArea.bottom - (chartArea.height() * i / steps);
                float textY;
                if (i == steps) {
                    textY = y + DensityUtils.dpToPx(getContext(), 10);
                } else if (i == 0) {
                    textY = y - DensityUtils.dpToPx(getContext(), 2);
                } else {
                    textY = y + DensityUtils.dpToPx(getContext(), 4);
                }

                double priceVal = (maxPrice * i / steps);
                paintText.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(String.format(locale, "%.0f", priceVal), axisX + axisGap, textY, paintText);
            }
        }
    }

}
