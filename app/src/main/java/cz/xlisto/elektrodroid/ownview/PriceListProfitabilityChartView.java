package cz.xlisto.elektrodroid.ownview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.BreakEvenResult;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.Point2D;
import cz.xlisto.elektrodroid.modules.pricelist.engine.BreakEvenCalculator.ResultStatus;

/**
 * Custom View pro závislost celkových ročních nákladů na spotřebě nebo podílu NT se zvýrazněným bodem zvratu.
 * Automaticky zalamuje dlouhé názvy ceníků v legendě tak, aby se celý název vešel na obrazovku.
 * Dynamicky přizpůsobuje barvy textu a mřížky pro světlý i tmavý režim (Light/Dark mode).
 */
public class PriceListProfitabilityChartView extends View {

    public enum Mode {
        COST_VS_CONSUMPTION,
        COST_VS_NT_RATIO
    }


    public record SeriesData(String title, int color, List<Point2D> points) {

        public SeriesData(String title, int color, List<Point2D> points) {
            this.title = title;
            this.color = color;
            this.points = points != null ? points : new ArrayList<>();
        }

    }

    private Mode mode = Mode.COST_VS_CONSUMPTION;
    private List<SeriesData> seriesList = new ArrayList<>();
    private BreakEvenResult breakEvenResult;
    private double currentReferenceValue = 3.0; // např. 3 MWh nebo 60% NT

    private Paint axisPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint linePaint;
    private Paint referenceLinePaint;
    private Paint breakEvenPointPaint;
    private final Path linePath = new Path();

    private float density;

    public PriceListProfitabilityChartView(Context context) {
        super(context);
        init(context);
    }

    public PriceListProfitabilityChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PriceListProfitabilityChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStrokeWidth(2 * density);
        axisPaint.setColor(ContextCompat.getColor(context, R.color.color_axis));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setColor(0x22000000);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(11 * density);
        textPaint.setColor(ContextCompat.getColor(context, R.color.color_axis));

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3 * density);

        referenceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        referenceLinePaint.setStyle(Paint.Style.STROKE);
        referenceLinePaint.setStrokeWidth(2 * density);
        referenceLinePaint.setColor(0xFFD32F2F); // Červená přerušovaná čára
        referenceLinePaint.setPathEffect(new DashPathEffect(new float[]{8 * density, 6 * density}, 0));

        breakEvenPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        breakEvenPointPaint.setStyle(Paint.Style.FILL);
        breakEvenPointPaint.setColor(0xFF2E7D32); // Zelený bod zvratu
    }

    public void setChartData(Mode mode, List<SeriesData> seriesList, BreakEvenResult breakEvenResult, double currentReferenceValue) {
        this.mode = mode;
        this.seriesList = seriesList != null ? seriesList : new ArrayList<>();
        this.breakEvenResult = breakEvenResult;
        this.currentReferenceValue = currentReferenceValue;

        requestLayout();
        invalidate();
    }


    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private List<String> splitTextToLines(Paint paint, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;

        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        // Přednostní rozdělení před závorkou " (" pokud existuje
        int parenIdx = text.indexOf(" (");
        if (parenIdx > 0) {
            String part1 = text.substring(0, parenIdx).trim();
            String part2 = text.substring(parenIdx).trim();
            if (paint.measureText(part1) <= maxWidth && paint.measureText(part2) <= maxWidth) {
                lines.add(part1);
                lines.add(part2);
                return lines;
            }
        }

        // Obecné zalamování po slovech
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // CharSequence.isEmpty() vyžaduje API level 35+ (minSdk je 24), proto je použit length() == 0
            //noinspection SizeReplaceableByIsEmpty
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }
        }
        // CharSequence.isEmpty() vyžaduje API level 35+ (minSdk je 24), proto je použit length() > 0
        //noinspection SizeReplaceableByIsEmpty
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float paddingLeft = 55 * density;
        float legendHeight = measureLegendHeight(width, paddingLeft, 16 * density);
        int desiredHeight = (int) (legendHeight + (230 * density));
        setMeasuredDimension(width, desiredHeight);
    }

    private float measureLegendHeight(int width, float startX, float startY) {
        if (seriesList == null || seriesList.isEmpty()) return 0;

        float y = startY;
        float lineGap = 16 * density;
        float rightBound = Math.max(100 * density, width - (16 * density));
        float maxTitleWidth = Math.max(60 * density, rightBound - startX - (22 * density));

        for (SeriesData series : seriesList) {
            List<String> titleLines = splitTextToLines(textPaint, series.title, maxTitleWidth);
            int lineCount = Math.max(1, titleLines.size());
            y += (lineCount * lineGap);
        }

        return y - startY + (4 * density);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int textColorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(getContext(), R.color.color_axis));
        int textColorSecondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF757575);
        int gridColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, 0x22000000);

        gridPaint.setColor(gridColor);

        if (seriesList == null || seriesList.isEmpty()) {
            textPaint.setColor(textColorSecondary);
            canvas.drawText("Žádné datové řady k zobrazení", 20 * density, 40 * density, textPaint);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        float paddingLeft = 55 * density;
        float paddingRight = 20 * density;

        // Vykreslení legendy nahoře a zjištění potřebné výšky
        float legendBottomY = drawLegend(canvas, paddingLeft, 16 * density, textColorPrimary);

        float paddingTop = Math.max(30 * density, legendBottomY + (10 * density));
        float paddingBottom = 40 * density;

        float chartW = width - paddingLeft - paddingRight;
        float chartH = height - paddingTop - paddingBottom;

        if (chartW <= 0 || chartH <= 0) return;

        // Určení rozsahů os X a Y
        double maxX = getMaxX();
        double maxY = getMaxY();
        if (maxX <= 0) maxX = 10.0;
        if (maxY <= 0) maxY = 10000.0;

        // Vykreslení mřížky a popisků Y
        int yTicks = 4;
        textPaint.setColor(textColorSecondary);
        for (int i = 0; i <= yTicks; i++) {
            float y = paddingTop + chartH - (i * (chartH / yTicks));
            double yVal = (maxY / yTicks) * i;

            canvas.drawLine(paddingLeft, y, paddingLeft + chartW, y, gridPaint);
            canvas.drawText(DecimalFormatHelper.df0.format(yVal) + " Kč", 4 * density, y + (4 * density), textPaint);
        }

        // Vykreslení mřížky a popisků X
        int xTicks = 5;
        for (int i = 0; i <= xTicks; i++) {
            float x = paddingLeft + (i * (chartW / xTicks));
            double xVal = (maxX / xTicks) * i;

            canvas.drawLine(x, paddingTop, x, paddingTop + chartH, gridPaint);
            String xLabel = (mode == Mode.COST_VS_CONSUMPTION) ? DecimalFormatHelper.df1.format(xVal) + " MWh" : DecimalFormatHelper.df0.format(xVal) + " %";
            canvas.drawText(xLabel, x - (12 * density), paddingTop + chartH + (18 * density), textPaint);
        }

        // Osy
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + chartH, axisPaint);
        canvas.drawLine(paddingLeft, paddingTop + chartH, paddingLeft + chartW, paddingTop + chartH, axisPaint);

        // Vykreslení řad
        for (SeriesData series : seriesList) {
            if (series.points == null || series.points.size() < 2) continue;

            linePaint.setColor(series.color);
            linePath.reset();

            for (int i = 0; i < series.points.size(); i++) {
                Point2D p = series.points.get(i);
                float px = paddingLeft + (float) ((p.x() / maxX) * chartW);
                float py = paddingTop + chartH - (float) ((p.y() / maxY) * chartH);

                if (i == 0) {
                    linePath.moveTo(px, py);
                } else {
                    linePath.lineTo(px, py);
                }
            }
            canvas.drawPath(linePath, linePaint);
        }

        // Vykreslení svislé referenční čáry pro aktuální hodnotu
        if (currentReferenceValue >= 0 && currentReferenceValue <= maxX) {
            float refX = paddingLeft + (float) ((currentReferenceValue / maxX) * chartW);
            canvas.drawLine(refX, paddingTop, refX, paddingTop + chartH, referenceLinePaint);

            textPaint.setColor(0xFFD32F2F);
            String refLabel = (mode == Mode.COST_VS_CONSUMPTION) ? "Aktuální: " + DecimalFormatHelper.df1.format(currentReferenceValue) + " MWh" : "Aktuální: " + DecimalFormatHelper.df0.format(currentReferenceValue) + " % NT";
            canvas.drawText(refLabel, refX + (4 * density), paddingTop + (12 * density), textPaint);
        }

        // Vykreslení bodů zvratu (pokud existují)
        if (breakEvenResult != null && breakEvenResult.status == ResultStatus.EXACT_INTERSECTION) {
            List<Double> bePoints = (breakEvenResult.allBreakEvenMwhPoints != null && !breakEvenResult.allBreakEvenMwhPoints.isEmpty())
                    ? breakEvenResult.allBreakEvenMwhPoints
                    : (breakEvenResult.breakEvenMwh > 0 ? java.util.Collections.singletonList(breakEvenResult.breakEvenMwh) : java.util.Collections.emptyList());

            for (double breakEvenX : bePoints) {
                if (breakEvenX >= 0 && breakEvenX <= maxX) {
                    float beX = paddingLeft + (float) ((breakEvenX / maxX) * chartW);

                    double yAtBreakEven = getYValueAtX(breakEvenX);
                    float beY = paddingTop + chartH - (float) ((yAtBreakEven / maxY) * chartH);

                    canvas.drawCircle(beX, beY, 6 * density, breakEvenPointPaint);

                    textPaint.setColor(0xFF2E7D32);
                    textPaint.setFakeBoldText(true);
                    String beLabel = "Bod zvratu: " + DecimalFormatHelper.df1.format(breakEvenX) + (mode == Mode.COST_VS_CONSUMPTION ? " MWh" : " %");
                    canvas.drawText(beLabel, beX + (8 * density), beY - (6 * density), textPaint);
                    textPaint.setFakeBoldText(false);
                }
            }
        }
    }

    private float drawLegend(Canvas canvas, float startX, float startY, int textColor) {
        if (seriesList == null || seriesList.isEmpty()) return startY;

        float y = startY;
        float lineLength = 16 * density;
        float lineGap = 16 * density;
        float rightBound = Math.max(100 * density, getWidth() - (16 * density));
        float maxTitleWidth = Math.max(60 * density, rightBound - startX - lineLength - (6 * density));

        for (SeriesData series : seriesList) {
            List<String> titleLines = splitTextToLines(textPaint, series.title, maxTitleWidth);

            linePaint.setColor(series.color);
            canvas.drawLine(startX, y - (4 * density), startX + lineLength, y - (4 * density), linePaint);

            textPaint.setColor(textColor);
            textPaint.setFakeBoldText(false);

            float lineY = y;
            for (String titleLine : titleLines) {
                canvas.drawText(titleLine, startX + lineLength + (6 * density), lineY, textPaint);
                lineY += lineGap;
            }

            y = lineY + (2 * density);
        }

        return y;
    }

    private double getMaxX() {
        double max = 0;
        for (SeriesData s : seriesList) {
            for (Point2D p : s.points) {
                if (p.x() > max) max = p.x();
            }
        }
        return max;
    }

    private double getMaxY() {
        double max = 0;
        for (SeriesData s : seriesList) {
            for (Point2D p : s.points) {
                if (p.y() > max) max = p.y();
            }
        }
        return max * 1.1; // 10% rezerva nahoře
    }

    private double getYValueAtX(double x) {
        if (seriesList.isEmpty()) return 0;
        SeriesData series = seriesList.get(0);
        for (int i = 0; i < series.points.size() - 1; i++) {
            Point2D p1 = series.points.get(i);
            Point2D p2 = series.points.get(i + 1);
            if (x >= p1.x() && x <= p2.x()) {
                double ratio = (p2.x() - p1.x()) > 0 ? (x - p1.x()) / (p2.x() - p1.x()) : 0;
                return p1.y() + ratio * (p2.y() - p1.y());
            }
        }
        return series.points.get(0).y();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
