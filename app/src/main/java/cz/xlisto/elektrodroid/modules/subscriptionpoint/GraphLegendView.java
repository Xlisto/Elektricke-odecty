package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.utils.DensityUtils;
import cz.xlisto.elektrodroid.utils.DetectNightMode;


/**
 * Vlastní View komponenta pro samostatnou pevnou legendu ročního kombinovaného grafu.
 */
public class GraphLegendView extends View {

    private int colorVT = 0xFF2E7D32;
    private int colorNT = 0xFF512DA8;
    private int colorFixedCost = 0xFF00897B;

    private Paint paintBarVT;
    private Paint paintBarNT;
    private Paint paintBarFixed;
    private Paint paintBarBorder;
    private Paint paintLineVT;
    private Paint paintLineNT;
    private Paint paintLineTotal;
    private Paint paintMarkerVT;
    private Paint paintMarkerNT;
    private Paint paintMarkerTotal;
    private Paint paintText;
    private Paint paintBgCard;

    private boolean isMWh = true;
    private boolean isDualTariff = true;

    private final Path pathDiamond = new Path();
    private final RectF bgCardRect = new RectF();
    private final RectF tempRect = new RectF();


    public GraphLegendView(Context context) {
        super(context);
        init(context);
    }


    public GraphLegendView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public GraphLegendView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        DataSettingsSource settingsSource = new DataSettingsSource(context);
        settingsSource.open();
        try {
            int[] colors = settingsSource.loadColorVTNT();
            if (colors != null && colors.length >= 2) {
                colorVT = colors[0];
                colorNT = colors[1];
            }
        } catch (Exception e) {
            Log.e("GraphLegendView", "Error loading colors from database", e);
        } finally {
            settingsSource.close();
        }

        boolean isDark = DetectNightMode.isNightMode(context);
        int bgColor = isDark ? 0xFF1E1E1E : 0xFFF5F5F5;

        colorFixedCost = isDark ? 0xFF26A69A : 0xFF00796B;
        AnnualGraphColorHelper.LineColors lineColors = AnnualGraphColorHelper.selectLineColors(context, colorVT, colorNT, bgColor);

        paintBarVT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarVT.setColor(colorVT);
        paintBarVT.setStyle(Paint.Style.FILL);

        paintBarNT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarNT.setColor(colorNT);
        paintBarNT.setStyle(Paint.Style.FILL);

        paintBarFixed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarFixed.setColor(colorFixedCost);
        paintBarFixed.setStyle(Paint.Style.FILL);

        paintBarBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBarBorder.setStyle(Paint.Style.STROKE);
        paintBarBorder.setStrokeWidth(DensityUtils.dpToPx(context, 1f));
        paintBarBorder.setColor(isDark ? 0xFF888888 : 0xFF757575);

        paintLineVT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineVT.setColor(lineColors.colorPriceVT());
        paintLineVT.setStrokeWidth(DensityUtils.dpToPx(context, 1.4f));
        paintLineVT.setStyle(Paint.Style.STROKE);

        paintLineNT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineNT.setColor(lineColors.colorPriceNT());
        paintLineNT.setStrokeWidth(DensityUtils.dpToPx(context, 1.4f));
        paintLineNT.setStyle(Paint.Style.STROKE);
        paintLineNT.setPathEffect(new DashPathEffect(new float[]{DensityUtils.dpToPx(context, 8), DensityUtils.dpToPx(context, 6)}, 0));

        paintLineTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLineTotal.setColor(lineColors.colorPriceTotal());
        paintLineTotal.setStrokeWidth(DensityUtils.dpToPx(context, 2.2f));
        paintLineTotal.setStyle(Paint.Style.STROKE);

        paintMarkerVT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarkerVT.setColor(lineColors.colorPriceVT());
        paintMarkerVT.setStyle(Paint.Style.FILL);

        paintMarkerNT = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarkerNT.setColor(lineColors.colorPriceNT());
        paintMarkerNT.setStyle(Paint.Style.STROKE);
        paintMarkerNT.setStrokeWidth(DensityUtils.dpToPx(context, 1.8f));

        paintMarkerTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintMarkerTotal.setColor(lineColors.colorPriceTotal());
        paintMarkerTotal.setStyle(Paint.Style.FILL);

        int textColor = isDark ? 0xFFEEEEEE : 0xFF212121;
        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(textColor);
        paintText.setTextSize(DensityUtils.dpToPx(context, 9.5f));

        paintBgCard = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBgCard.setColor(bgColor);
        paintBgCard.setStyle(Paint.Style.FILL);
    }


    public void setLegendData(boolean isMWh, boolean isDualTariff) {
        this.isMWh = isMWh;
        this.isDualTariff = isDualTariff;
        invalidate();
    }


    private boolean needsOutlineBorder(int color, int cardBgColor) {
        return ColorUtils.calculateContrast(color, cardBgColor) < 1.6;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = DensityUtils.dpToPx(getContext(), 48);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        boolean isDark = DetectNightMode.isNightMode(getContext());
        int cardBgColor = isDark ? 0xFF1E1E1E : 0xFFF5F5F5;

        // Pozadí karty legendy
        float rx = DensityUtils.dpToPx(getContext(), 5);
        bgCardRect.set(0, 0, width, height);
        canvas.drawRoundRect(bgCardRect, rx, rx, paintBgCard);

        float row1Y = height * 0.35f;
        float row2Y = height * 0.75f;
        float startX = DensityUtils.dpToPx(getContext(), 8);
        float totalW = width - DensityUtils.dpToPx(getContext(), 16);
        int numCols = isDualTariff ? 3 : 2;
        float colW = totalW / numCols;

        float boxSize = DensityUtils.dpToPx(getContext(), 9);
        float sampleLength = DensityUtils.dpToPx(getContext(), 26);
        float markerR = DensityUtils.dpToPx(getContext(), 3.5f);

        paintText.setTextAlign(Paint.Align.LEFT);

        // --- Řada 1: Sloupce ---
        float c0X = startX + 0 * colW;
        tempRect.set(c0X, row1Y - boxSize + 2, c0X + boxSize, row1Y + 2);
        canvas.drawRect(tempRect, paintBarVT);
        if (needsOutlineBorder(colorVT, cardBgColor)) {
            canvas.drawRect(tempRect, paintBarBorder);
        }
        String labelVT = getContext().getString(R.string.graph_legend_vt, isMWh ? "MWh" : "kWh");
        canvas.drawText(labelVT, c0X + boxSize + 4, row1Y, paintText);

        if (isDualTariff) {
            float c1X = startX + 1 * colW;
            tempRect.set(c1X, row1Y - boxSize + 2, c1X + boxSize, row1Y + 2);
            canvas.drawRect(tempRect, paintBarNT);
            if (needsOutlineBorder(colorNT, cardBgColor)) {
                canvas.drawRect(tempRect, paintBarBorder);
            }
            String labelNT = getContext().getString(R.string.graph_legend_nt, isMWh ? "MWh" : "kWh");
            canvas.drawText(labelNT, c1X + boxSize + 4, row1Y, paintText);

            float c2X = startX + 2 * colW;
            tempRect.set(c2X, row1Y - boxSize + 2, c2X + (boxSize * 0.5f), row1Y + 2);
            canvas.drawRect(tempRect, paintBarFixed);
            if (needsOutlineBorder(colorFixedCost, cardBgColor)) {
                canvas.drawRect(tempRect, paintBarBorder);
            }
            String labelFixed = getContext().getString(R.string.graph_legend_fixed_cost);
            canvas.drawText(labelFixed, c2X + (boxSize * 0.5f) + 4, row1Y, paintText);
        } else {
            float c1X = startX + 1 * colW;
            tempRect.set(c1X, row1Y - boxSize + 2, c1X + (boxSize * 0.5f), row1Y + 2);
            canvas.drawRect(tempRect, paintBarFixed);
            if (needsOutlineBorder(colorFixedCost, cardBgColor)) {
                canvas.drawRect(tempRect, paintBarBorder);
            }
            String labelFixed = getContext().getString(R.string.graph_legend_fixed_cost);
            canvas.drawText(labelFixed, c1X + (boxSize * 0.5f) + 4, row1Y, paintText);
        }

        // --- Řada 2: Cenové čáry ---
        if (isDualTariff) {
            canvas.drawLine(c0X, row2Y - 3, c0X + sampleLength, row2Y - 3, paintLineVT);
            canvas.drawCircle(c0X + sampleLength / 2f, row2Y - 3, markerR, paintMarkerVT);
            canvas.drawText("Cena VT", c0X + sampleLength + 6, row2Y, paintText);

            float c1X = startX + 1 * colW;
            canvas.drawLine(c1X, row2Y - 3, c1X + sampleLength, row2Y - 3, paintLineNT);
            canvas.drawCircle(c1X + sampleLength / 2f, row2Y - 3, markerR, paintMarkerNT);
            canvas.drawText("Cena NT", c1X + sampleLength + 6, row2Y, paintText);

            float c2X = startX + 2 * colW;
            canvas.drawLine(c2X, row2Y - 3, c2X + sampleLength, row2Y - 3, paintLineTotal);
            pathDiamond.reset();
            float midX = c2X + sampleLength / 2f;
            float midY = row2Y - 3;
            pathDiamond.moveTo(midX, midY - markerR * 1.2f);
            pathDiamond.lineTo(midX + markerR * 1.2f, midY);
            pathDiamond.lineTo(midX, midY + markerR * 1.2f);
            pathDiamond.lineTo(midX - markerR * 1.2f, midY);
            pathDiamond.close();
            canvas.drawPath(pathDiamond, paintMarkerTotal);
            canvas.drawText("Cena celkem", c2X + sampleLength + 6, row2Y, paintText);
        } else {
            canvas.drawLine(c0X, row2Y - 3, c0X + sampleLength, row2Y - 3, paintLineTotal);
            pathDiamond.reset();
            float midX = c0X + sampleLength / 2f;
            float midY = row2Y - 3;
            pathDiamond.moveTo(midX, midY - markerR * 1.2f);
            pathDiamond.lineTo(midX + markerR * 1.2f, midY);
            pathDiamond.lineTo(midX, midY + markerR * 1.2f);
            pathDiamond.lineTo(midX - markerR * 1.2f, midY);
            pathDiamond.close();
            canvas.drawPath(pathDiamond, paintMarkerTotal);
            canvas.drawText("Cena celkem", c0X + sampleLength + 6, row2Y, paintText);
        }
    }

}
