package cz.xlisto.elektrodroid.modules.graphmonth;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.ConsuptionModel;
import cz.xlisto.elektrodroid.utils.ColorUtils;
import cz.xlisto.elektrodroid.utils.DensityUtils;


/**
 * Vlastní view pro vykreslení měsíčního, ročního a porovnávacího grafu spotřeby VT/NT.
 * Umožňuje přepínat typ grafu, posouvat data gestem, zobrazení posuvníku a měnit přesnost mřížky.
 * Xlisto 20.08.2023 22:01
 */
public class GraphMonthView extends View {

    private final String ARG_X_MOVE_GRAPH_MONTH = "xMoveGraphMonth";
    private final String ARG_X_MOVE_GRAPH_YEAR = "xMoveGraphYear";
    private final String ARG_IS_SHOW_PERIOD = "isShowYear";
    private final String ARG_COMPARE_MONTH = "compareMonth";
    private final String ARG_IS_SHOW_VT = "isShowVT";
    private final String ARG_IS_SHOW_NT = "isShowNT";
    private final String ARG_TYPE_GRAPH = "isShowLineGraph";
    private final String ARG_COEFICIENT = "cof";
    private int height;
    private int heightGraph;
    private int width;
    private int multiple = 10;
    private int left;
    private int textMeasure;
    private double steep, multipleSteep;
    private int period = 1; // 0 = měsíční, 1 = roční, 2 = porovnání daného měsíce
    private boolean graphType = true;
    private double maxMonthly = 0;
    private double maxYear = 0;
    private int colorVT = Color.RED;
    private int colorNT = Color.BLUE;
    int xClick = 0; // souřadnice při dotyku na obrazovku
    int xMoveGraphYear = 0; // počet o kolik se má posunout roční/porovnávací graf
    int xMoveGraphAfterYear = 0; // počet posunutí ročního grafu při předchozím posunu
    int xMoveGraphMaxYear = 0; // maximální posun ročního grafu
    int xMoveGraphMonth = 0; // počet o kolik se má posunout měsíční graf
    int xMoveGraphAfterMonth = 0; // počet posunutí měsíčního grafu při předchozím posunu
    int xMoveGraphMaxMonth = 0; // maximální posun měsíčního grafu
    int compareMonth = 0; // vybraný měsíc pro porovnání
    private ArrayList<ConsuptionModel> monthlyConsuption = new ArrayList<>();
    private ArrayList<ConsuptionModel> yearConsuption = new ArrayList<>();
    private ArrayList<ArrayList<ConsuptionModel>> monthsConsuption = new ArrayList<>();
    private boolean showVT = true;
    private boolean showNT = true;
    private int cof = 60; // koeficient zvětšení/zmenšení přesnosti grafu
    private int moveConsuption = 0; // posunutí sloupcového grafu při animaci
    private boolean shouldScrollToEndMonth = true;
    private boolean shouldScrollToEndYear = true;
    private boolean multipleNeedsInit = true;
    private ValueAnimator valueAnimator;


    public GraphMonthView(Context context) {
        super(context);
        init();
    }


    public GraphMonthView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public GraphMonthView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public GraphMonthView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        setSaveEnabled(true);
        animated();
    }


    /**
     * Voláno, když má zobrazení vykreslit svůj obsah.
     *
     * @param canvas plátno pro kreslení
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        double currentMax = getEffectiveMax();
        setSteep((int) currentMax);
        ensureValidMultiple();

        setAxisHorizontalDescription(canvas); // první vykreslení pro získání šířky
        setAxisHorizontal(canvas);
        drawLineVTNT(canvas);
        setMaxMoveGraph();
        setAxisVerticalDescription(canvas);
        setAxisHorizontalDescription(canvas);
        drawScrollBar(canvas);
    }


    /**
     * Vrátí maximální hodnotu spotřeby odpovídající aktuálnímu režimu zobrazení (0 = měsíční, 1 = roční, 2 = porovnání měsíce).
     */
    private double getEffectiveMax() {
        if (period == 1) {
            return maxYear;
        } else if (period == 2) {
            return getMaxCompareMonth();
        } else {
            return maxMonthly;
        }
    }


    /**
     * Vypočítá maximální spotřebu přímo pro vybraný porovnávaný měsíc (period == 2).
     */
    private double getMaxCompareMonth() {
        if (monthsConsuption.isEmpty() || compareMonth >= monthsConsuption.size()) {
            return maxMonthly;
        }
        ArrayList<ConsuptionModel> compareList = monthsConsuption.get(compareMonth);
        double maxVTVal = 0;
        double maxNTVal = 0;
        for (ConsuptionModel month : compareList) {
            if (showVT) {
                maxVTVal = Math.max(maxVTVal, month.getConsuptionVT());
            }
            if (showNT) {
                maxNTVal = Math.max(maxNTVal, month.getConsuptionNT());
            }
        }
        double max = Math.max(maxVTVal, maxNTVal);
        if (max <= 0) {
            return 10.0;
        } else {
            return max + (max * 0.15);
        }
    }


    /**
     * Nastavení velikosti widgetu a získání aktuálních rozměrů.
     *
     * @param widthMeasureSpec  šířka
     * @param heightMeasureSpec výška
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        height = getMeasuredHeight();
        width = getMeasuredWidth();
        setHeightGraph();

        setMeasuredDimension(width, height);
    }


    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putParcelable("superstate", super.onSaveInstanceState());
        bundle.putInt(ARG_IS_SHOW_PERIOD, period);
        bundle.putInt(ARG_X_MOVE_GRAPH_MONTH, xMoveGraphMonth);
        bundle.putInt(ARG_X_MOVE_GRAPH_YEAR, xMoveGraphYear);
        bundle.putInt(ARG_COMPARE_MONTH, compareMonth);
        bundle.putBoolean(ARG_IS_SHOW_VT, showVT);
        bundle.putBoolean(ARG_IS_SHOW_NT, showNT);
        bundle.putBoolean(ARG_TYPE_GRAPH, graphType);
        bundle.putInt(ARG_COEFICIENT, cof);
        bundle.putInt("multiple", multiple);
        return bundle;
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        period = bundle.getInt(ARG_IS_SHOW_PERIOD);
        xMoveGraphMonth = bundle.getInt(ARG_X_MOVE_GRAPH_MONTH);
        xMoveGraphYear = bundle.getInt(ARG_X_MOVE_GRAPH_YEAR);
        xMoveGraphAfterYear = xMoveGraphYear;
        xMoveGraphAfterMonth = xMoveGraphMonth;
        compareMonth = bundle.getInt(ARG_COMPARE_MONTH);
        showVT = bundle.getBoolean(ARG_IS_SHOW_VT);
        showNT = bundle.getBoolean(ARG_IS_SHOW_NT);
        graphType = bundle.getBoolean(ARG_TYPE_GRAPH);
        cof = bundle.getInt(ARG_COEFICIENT);
        multiple = bundle.getInt("multiple", multiple);
        shouldScrollToEndMonth = false;
        shouldScrollToEndYear = false;
        multipleNeedsInit = false;
        Parcelable superState = androidx.core.os.BundleCompat.getParcelable(bundle, "superstate", Parcelable.class);
        super.onRestoreInstanceState(superState);
    }


    /**
     * Nastaví výšku grafu s odečtením na pozici dolního popisku os.
     */
    private void setHeightGraph() {
        if (period == 1) {
            heightGraph = height - dipToPx(20);
        } else {
            heightGraph = height - dipToPx(30);
        }
    }


    /**
     * Nastaví vstupní data spotřeby pro všechny podporované režimy grafu.
     *
     * @param consuption kontejner s měsíčními, ročními a porovnávacími daty
     */
    public void setConsuption(ConsuptionContainer consuption) {
        if (consuption != null) {
            this.monthlyConsuption = consuption.getMonthlyConsuption();
            this.yearConsuption = consuption.getYearConsuption();
            this.monthsConsuption = consuption.getMonthsConsuptionsArray();
        }
        shouldScrollToEndMonth = true;
        shouldScrollToEndYear = true;
        setMinMaxMonthly();
        setMinMaxYear();
        initMultipleForDataset();
        init();
    }


    /**
     * Nastaví maximální a minimální hodnotu pro měsíční graf.
     */
    private void setMinMaxMonthly() {
        double maxMonthlyVT = 0;
        double maxMonthlyNT = 0;

        for (ConsuptionModel month : monthlyConsuption) {
            if (showVT && month.getConsuptionVT() > maxMonthlyVT) {
                maxMonthlyVT = month.getConsuptionVT();
            }
            if (showNT && month.getConsuptionNT() > maxMonthlyNT) {
                maxMonthlyNT = month.getConsuptionNT();
            }
        }

        maxMonthly = Math.max(maxMonthlyVT, maxMonthlyNT);
        if (maxMonthly <= 0) {
            maxMonthly = 10.0;
        } else {
            maxMonthly = maxMonthly + (maxMonthly * 0.15);
        }
    }


    /**
     * Nastaví maximální a minimální hodnotu pro roční graf.
     */
    private void setMinMaxYear() {
        double maxYearVT = 0;
        double maxYearNT = 0;

        for (ConsuptionModel year : yearConsuption) {
            if (showVT && year.getConsuptionVT() > maxYearVT) {
                maxYearVT = year.getConsuptionVT();
            }
            if (showNT && year.getConsuptionNT() > maxYearNT) {
                maxYearNT = year.getConsuptionNT();
            }
        }

        maxYear = Math.max(maxYearVT, maxYearNT);
        if (maxYear <= 0) {
            maxYear = 10.0;
        } else {
            maxYear = maxYear + (maxYear * 0.15);
        }
    }


    /**
     * Vykreslí vodorovné osy.
     *
     * @param canvas plátno
     */
    private void setAxisHorizontal(Canvas canvas) {
        Paint axisHorizontal = new Paint();

        axisHorizontal.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        axisHorizontal.setStrokeWidth(1);
        axisHorizontal.setStyle(Paint.Style.FILL);

        int stepPx = (int) Math.round(steep * multiple / multipleSteep);
        if (stepPx <= 0) return;

        int stopY = heightGraph;
        int i = 1;

        if (steep > 0 && multiple > 0) {
            while (stopY > 0) {
                drawLine(canvas, axisHorizontal, 0, stopY, width, stopY);
                stopY = heightGraph - (i * stepPx);
                i++;
            }
        }
    }


    /**
     * Nakreslí popisky k vodorovným osám na vykresleném pozadí.
     *
     * @param canvas plátno
     */
    private void setAxisHorizontalDescription(Canvas canvas) {
        Paint background = new Paint();
        Paint text = new Paint();

        background.setColor(ContextCompat.getColor(getContext(), R.color.color_graph_background));
        text.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        text.setTextSize(dipToPx(10));

        int stepPx = (int) Math.round(steep * multiple / multipleSteep);

        textMeasure = 0;
        if (steep > 0 && multiple > 0 && stepPx > 0) {
            textMeasure = (int) text.measureText(multiple * (Math.ceil((double) heightGraph / stepPx)) + " kWh") + dipToPx(5);
            left = textMeasure; // nastavení odsazení grafu zleva
        }

        canvas.drawRect(0, 0, textMeasure, height, background);

        if (steep > 0 && multiple > 0 && stepPx > 0) {
            int stopY = heightGraph;
            int description = 0;
            while (stopY > 0) {
                drawText(canvas, text, 0, stopY, description + " kWh");
                stopY = stopY - stepPx;
                description += multiple;
            }
        }
    }


    /**
     * Vypíše popisy k svislým osám.
     *
     * @param canvas plátno
     */
    private void setAxisVerticalDescription(Canvas canvas) {
        Paint text = new Paint();
        Paint background = new Paint();

        text.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        text.setTextSize(dipToPx(10));

        background.setColor(ContextCompat.getColor(getContext(), R.color.color_graph_background));
        ArrayList<ConsuptionModel> consuption = new ArrayList<>();
        int xMoveGraph = switch (period) {
            case 0 -> {
                consuption = monthlyConsuption;
                yield xMoveGraphMonth;
            }
            case 1 -> {
                consuption = yearConsuption;
                yield xMoveGraphYear;
            }
            case 2 -> {
                if (compareMonth < monthsConsuption.size())
                    consuption = monthsConsuption.get(compareMonth);
                yield xMoveGraphYear;
            }
            default -> 0;
        };

        if (period == 0 || period == 2)
            canvas.drawRect(0, heightGraph, width, height, background);
        else if (period == 1)
            canvas.drawRect(0, heightGraph, width, height, background);

        for (int i = 0; i < consuption.size(); i++) {
            if (period == 0 || period == 2) {
                int textMeasureMonth = (int) text.measureText((consuption.get(i)).getDateMonthAsStringShort());
                drawText(canvas, text, (dipToPx(30) * (i + 1)) + left + xMoveGraph - (textMeasureMonth / 2), height - dipToPx(18), (consuption.get(i)).getDateMonthAsStringShort());
                int textMeasureYear = (int) text.measureText(consuption.get(i).getYearAsString());
                drawText(canvas, text, (dipToPx(30) * (i + 1)) + left + xMoveGraph - (textMeasureYear / 2), height - dipToPx(7), consuption.get(i).getYearAsString());
            } else if (period == 1) {
                int textMeasureYear = (int) text.measureText(consuption.get(i).getYearAsString());
                drawText(canvas, text, (dipToPx(30) * (i + 1)) + left + xMoveGraph - (textMeasureYear / 2), height - dipToPx(7), consuption.get(i).getYearAsString());
            }
        }
    }


    /**
     * Vykreslí graf VT a NT spotřeby.
     *
     * @param canvas plátno
     */
    private void drawLineVTNT(Canvas canvas) {
        int strokeWidth = 3;
        Paint.Style style = Paint.Style.FILL;
        Paint lineVT = new Paint();
        Paint lineNT = new Paint();
        Paint lineVTBorder = new Paint();
        Paint lineNTBorder = new Paint();
        Paint line = new Paint();
        lineVT.setColor(colorVT);
        lineNT.setColor(colorNT);
        lineVTBorder.setColor(ColorUtils.darkerColor(colorVT));
        lineNTBorder.setColor(ColorUtils.darkerColor(colorNT));
        line.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        lineVT.setStrokeWidth(strokeWidth);
        lineNT.setStrokeWidth(strokeWidth);
        int borderGraph = DensityUtils.dpToPx(getContext(), 1);
        lineVTBorder.setStrokeWidth(borderGraph);
        lineNTBorder.setStrokeWidth(borderGraph);
        line.setStrokeWidth(1);
        lineVT.setStyle(style);
        lineNT.setStyle(style);
        line.setStyle(style);
        lineVTBorder.setStyle(Paint.Style.STROKE);
        lineNTBorder.setStyle(Paint.Style.STROKE);
        line.setPathEffect(new DashPathEffect(new float[]{15f, 30f}, 0f));

        ArrayList<ConsuptionModel> consuption = new ArrayList<>();
        int xMoveGraph = switch (period) {
            case 0 -> {
                consuption = monthlyConsuption;
                yield xMoveGraphMonth;
            }
            case 1 -> {
                consuption = yearConsuption;
                yield xMoveGraphYear;
            }
            case 2 -> {
                if (compareMonth < monthsConsuption.size())
                    consuption = monthsConsuption.get(compareMonth);
                yield xMoveGraphYear;
            }
            default -> 0;
        };

        int startX, stopX, lastX = dipToPx(30) * consuption.size() + left;
        for (int i = 0; i < consuption.size() - 1; i++) {
            startX = (dipToPx(30) * (i + 1)) + left + xMoveGraph;
            stopX = (dipToPx(30) * (i + 1)) + left + xMoveGraph;
            lastX = (dipToPx(30) * (consuption.size())) + left + xMoveGraph;
            if (graphType)
                stopX = (dipToPx(30) * (i + 2)) + left + xMoveGraph;

            int stopVTStart = consuption.get(i).getConsuptionVTAnimace();
            int stopVTEnd = consuption.get(i + 1).getConsuptionVTAnimace();
            int stopNTStart = consuption.get(i).getConsuptionNTAnimace();
            int stopNTEnd = consuption.get(i + 1).getConsuptionNTAnimace();

            if (startX > (width + 50) || startX < 0) {
                stopVTStart = consuption.get(i).getConsuptionVT().intValue();
                stopVTEnd = consuption.get(i + 1).getConsuptionVT().intValue();
                stopNTStart = consuption.get(i).getConsuptionNT().intValue();
                stopNTEnd = consuption.get(i + 1).getConsuptionNT().intValue();
            }

            if (showVT) {
                if (graphType) {
                    drawLine(canvas, lineVT, startX, heightGraph - (int) (stopVTStart * steep / multipleSteep), stopX, heightGraph - (int) (stopVTEnd * steep / multipleSteep));
                    drawCircle(canvas, lineVT, startX, heightGraph - (int) (stopVTStart * steep / multipleSteep), dipToPx(3));
                } else {
                    int leftBar = stopX - dipToPx(12) + borderGraph;
                    int bottom = heightGraph;
                    int top = heightGraph - (int) (stopVTStart * steep / multipleSteep);
                    drawRect(canvas, lineVT, leftBar, top, startX, bottom);
                    drawRect(canvas, lineVTBorder, leftBar, top, startX, bottom);
                }
            }
            if (showNT) {
                if (graphType) {
                    drawLine(canvas, lineNT, startX, heightGraph - (int) (stopNTStart * steep / multipleSteep), stopX, heightGraph - (int) (stopNTEnd * steep / multipleSteep));
                    drawCircle(canvas, lineNT, startX, heightGraph - (int) (stopNTStart * steep / multipleSteep), dipToPx(3));

                } else {
                    int leftBar = startX + borderGraph;
                    int bottom = heightGraph;
                    int right = stopX + dipToPx(12);
                    int top = heightGraph - (int) (stopNTStart * steep / multipleSteep);
                    drawRect(canvas, lineNT, leftBar, top, right, bottom);
                    drawRect(canvas, lineNTBorder, leftBar, top, right, bottom);
                }
            }
            int minConsuption = Math.min(stopVTStart, stopNTStart);
            if (!showNT) minConsuption = stopVTStart;
            if (!showVT) minConsuption = stopNTStart;

            if (graphType) {
                // vykreslení pomocných svislých os
                int startY = heightGraph - dipToPx(10);
                int stopY = heightGraph - (int) (minConsuption * steep / multipleSteep) + dipToPx(10);
                if (startY > stopY)
                    drawLine(canvas, line, startX, startY, startX, stopY);
            } else {
                // vykreslení spotřeby ve spodní části sloupců
                drawConsuption(canvas, startX + borderGraph / 2, consuption.get(i).getConsuptionVT(), consuption.get(i).getConsuptionNT());
            }
        }

        // poslední záznam
        if (!consuption.isEmpty()) {
            ConsuptionModel lastMonthlyConsuption = consuption.get(consuption.size() - 1);
            int stopVTStart = lastMonthlyConsuption.getConsuptionVTAnimace();
            int stopNTStart = lastMonthlyConsuption.getConsuptionNTAnimace();
            if (showVT) {
                if (graphType)
                    drawCircle(canvas, lineVT, lastX, heightGraph - (int) (stopVTStart * steep / multipleSteep), dipToPx(3));
                else {
                    int bottom = heightGraph;
                    int leftBar = lastX - dipToPx(12) + borderGraph;
                    int top = heightGraph - (int) (stopVTStart * steep / multipleSteep);
                    drawRect(canvas, lineVT, leftBar, top, lastX, bottom);
                    drawRect(canvas, lineVTBorder, leftBar, top, lastX, bottom);
                }
            }
            if (showNT) {
                if (graphType)
                    drawCircle(canvas, lineNT, lastX, heightGraph - (int) (stopNTStart * steep / multipleSteep), dipToPx(3));
                else {
                    int leftBar = lastX + borderGraph;
                    int right = lastX + dipToPx(12);
                    int bottom = heightGraph;
                    int top = heightGraph - (int) (stopNTStart * steep / multipleSteep);
                    drawRect(canvas, lineNT, leftBar, top, right, bottom);
                    drawRect(canvas, lineNTBorder, leftBar, top, right, bottom);
                }
            }
            if (!graphType)
                drawConsuption(canvas, lastX, consuption.get(consuption.size() - 1).getConsuptionVT(), consuption.get(consuption.size() - 1).getConsuptionNT());

            int minConsuption = Math.min(stopVTStart, stopNTStart);
            if (!showNT) minConsuption = stopVTStart;
            if (!showVT) minConsuption = stopNTStart;
            int startY = heightGraph - dipToPx(10);
            int stopY = heightGraph - (int) (minConsuption * steep / multipleSteep) + dipToPx(10);
            if (startY > stopY && graphType)
                drawLine(canvas, line, lastX, startY, lastX, stopY);

            drawLegend(canvas, lineVT, lineNT);
        }
    }


    /**
     * Vykreslí vodorovný posuvník (scrollbar), pokud je obsah grafu širší než obrazovka.
     *
     * @param canvas plátno
     */
    private void drawScrollBar(Canvas canvas) {
        int xMoveGraph = switch (period) {
            case 0 -> xMoveGraphMonth;
            case 1, 2 -> xMoveGraphYear;
            default -> 0;
        };

        int xMoveGraphMax = switch (period) {
            case 0 -> xMoveGraphMaxMonth;
            case 1, 2 -> xMoveGraphMaxYear;
            default -> 0;
        };

        if (xMoveGraphMax < 0) {
            int trackLeft = left + dipToPx(4);
            int trackRight = width - dipToPx(4);
            int trackWidth = trackRight - trackLeft;

            if (trackWidth <= 0) return;

            int trackHeight = dipToPx(4);
            int trackTop = height - trackHeight - dipToPx(1);
            int trackBottom = height - dipToPx(1);

            Paint trackPaint = new Paint();
            trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
            trackPaint.setAlpha(40);
            trackPaint.setStyle(Paint.Style.FILL);

            Paint thumbPaint = new Paint();
            thumbPaint.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
            thumbPaint.setAlpha(160);
            thumbPaint.setStyle(Paint.Style.FILL);

            float visibleWidth = width - left;
            int sizeList = switch (period) {
                case 0 -> monthlyConsuption.size();
                case 1 -> yearConsuption.size();
                case 2 ->
                        (monthsConsuption.isEmpty() || compareMonth >= monthsConsuption.size()) ? 0 : monthsConsuption.get(compareMonth).size();
                default -> 0;
            };
            float totalGraphWidth = (dipToPx(30) * (sizeList + 1)) + left;

            if (totalGraphWidth <= visibleWidth) return;

            float thumbWidth = (visibleWidth / totalGraphWidth) * trackWidth;
            float minThumbWidth = dipToPx(24);
            if (thumbWidth < minThumbWidth) {
                thumbWidth = minThumbWidth;
            }

            float scrollProgress = (float) (-xMoveGraph) / (float) (-xMoveGraphMax);
            if (scrollProgress < 0f) scrollProgress = 0f;
            if (scrollProgress > 1f) scrollProgress = 1f;

            float thumbLeft = trackLeft + scrollProgress * (trackWidth - thumbWidth);
            float thumbRight = thumbLeft + thumbWidth;

            float radius = trackHeight / 2f;
            canvas.drawRoundRect(trackLeft, trackTop, trackRight, trackBottom, radius, radius, trackPaint);
            canvas.drawRoundRect(thumbLeft, trackTop, thumbRight, trackBottom, radius, radius, thumbPaint);
        }
    }


    private void drawLine(Canvas canvas, Paint paint, int startX, int startY, int stopX, int stopY) {
        canvas.drawLine(startX, startY, stopX, stopY, paint);
    }


    private void drawCircle(Canvas canvas, Paint paint, int x, int y, int radius) {
        canvas.drawCircle(x, y, radius, paint);
    }


    private void drawText(Canvas canvas, Paint paint, int x, int y, String text) {
        canvas.drawText(text, x, y, paint);
    }


    private void drawRect(Canvas canvas, Paint paint, int left, int top, int right, int bottom) {
        canvas.drawRect(left, top, right, bottom, paint);
    }


    /**
     * Vykreslí rámeček s legendou.
     *
     * @param canvas  plátno
     * @param paintVT barva VT
     * @param paintNT barva NT
     */
    private void drawLegend(Canvas canvas, Paint paintVT, Paint paintNT) {
        Paint background = new Paint();
        Paint border = new Paint();
        Paint text = new Paint();
        Paint textBold = new Paint();

        background.setColor(ContextCompat.getColor(getContext(), R.color.color_graph_background));
        border.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        text.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        textBold.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        text.setTextSize(dipToPx(10));
        textBold.setTextSize(dipToPx(10));
        textBold.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dipToPx(1));

        paintNT.setStrokeWidth(dipToPx(3));
        paintVT.setStrokeWidth(dipToPx(3));

        int legendLeft = textMeasure + dipToPx(2);
        int legendRight = legendLeft + dipToPx(50);
        int bottom = dipToPx(30);
        if ((showVT && !showNT) || (!showVT && showNT))
            bottom = dipToPx(20);

        canvas.drawRoundRect(legendLeft, 0, legendRight, bottom, dipToPx(4), dipToPx(4), background);
        canvas.drawRoundRect(legendLeft, 0, legendRight, bottom, dipToPx(4), dipToPx(4), border);

        canvas.drawRoundRect(width - dipToPx(105), 0, width - dipToPx(2), dipToPx(30), dipToPx(4), dipToPx(4), background);
        canvas.drawRoundRect(width - dipToPx(105), 0, width - dipToPx(2), dipToPx(30), dipToPx(4), dipToPx(4), border);

        if (showNT) {
            int move = 0;
            if (!showVT)
                move = 11;
            canvas.drawText("NT", legendLeft + dipToPx(5), dipToPx(25 - move), text);
            canvas.drawLine(legendLeft + dipToPx(25), dipToPx(22 - move), legendLeft + dipToPx(40), dipToPx(22 - move), paintNT);
        }
        if (showVT) {
            canvas.drawText("VT", legendLeft + dipToPx(5), dipToPx(15), text);
            canvas.drawLine(legendLeft + dipToPx(25), dipToPx(11), legendLeft + dipToPx(40), dipToPx(11), paintVT);
        }

        String title = "";
        int moveText = 0;
        float measureText;
        if (period == 0) {
            title = "Měsíční spotřeba";
            measureText = textBold.measureText(title);
            moveText = dipToPx(102) - ((dipToPx(100) - ((int) measureText)) / 2);
        }
        if (period == 1) {
            title = "Roční spotřeba";
            measureText = textBold.measureText(title);
            moveText = dipToPx(102) - ((dipToPx(100) - ((int) measureText)) / 2);
        }
        canvas.drawText(title, width - moveText, dipToPx(15), textBold);

        if (period == 2) {
            if (!monthsConsuption.isEmpty() && compareMonth < monthsConsuption.size() && !monthsConsuption.get(compareMonth).isEmpty()) {
                canvas.drawText("Porovnávaný měsíc:", width - dipToPx(100), dipToPx(15), text);
                canvas.drawText(monthsConsuption.get(compareMonth).get(0).getDateMonthAsStringLong(), width - dipToPx(100), dipToPx(25), textBold);
            }
        }
    }


    /**
     * Vypíše spotřebu VT a NT ve spodní části sloupcového grafu.
     *
     * @param canvas plátno
     * @param x      pozice x
     * @param vt     spotřeba VT
     * @param nt     spotřeba NT
     */
    private void drawConsuption(Canvas canvas, int x, double vt, double nt) {
        Paint text = new Paint();
        Paint background = new Paint();
        Paint border = new Paint();
        background.setColor(ContextCompat.getColor(getContext(), R.color.color_graph_background));
        border.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        border.setStyle(Paint.Style.STROKE);
        text.setColor(ContextCompat.getColor(getContext(), R.color.color_axis));
        text.setTextSize(dipToPx(8));
        int y = heightGraph - dipToPx(2);
        int textMeasureVT = (int) text.measureText("VT: " + DecimalFormatHelper.df2.format(vt));
        int textMeasureNT = (int) text.measureText("NT: " + DecimalFormatHelper.df2.format(nt));
        canvas.rotate(-90, x, y);
        int measureText = Math.max(textMeasureVT, textMeasureNT);
        if (x > width + 50)
            moveConsuption = 0;

        int round = dipToPx(4);
        if (showVT) {
            int leftBox = x - dipToPx(1) - moveConsuption;
            int rightBox = x + measureText + dipToPx(5) - moveConsuption;
            int top = y - dipToPx(1);
            int bottom = y - dipToPx(11);
            canvas.drawRoundRect(leftBox, bottom, rightBox, top, round, round, background);
            canvas.drawRoundRect(leftBox, bottom, rightBox, top, round, round, border);
            canvas.drawText("VT: " + DecimalFormatHelper.df2.format(vt), x + dipToPx(2) - moveConsuption, y - dipToPx(3), text);
        }
        if (showNT) {
            int leftBox = x - dipToPx(1) - moveConsuption;
            int rightBox = x + measureText + dipToPx(5) - moveConsuption;
            int top = y + dipToPx(1);
            int bottom = y + dipToPx(11);
            canvas.drawRoundRect(leftBox, top, rightBox, bottom, round, round, background);
            canvas.drawRoundRect(leftBox, top, rightBox, bottom, round, round, border);
            canvas.drawText("NT: " + DecimalFormatHelper.df2.format(nt), x + dipToPx(2) - moveConsuption, y + dipToPx(9), text);
        }
        canvas.rotate(90, x, y);
    }


    /**
     * Výpočet velikosti v px 1 kWh na grafu.
     *
     * @param max maximální hodnota spotřeby
     */
    private void setSteep(int max) {
        multipleSteep = 1;
        if (max <= 0) {
            steep = 0;
            return;
        }
        steep = (heightGraph / (double) max);
        int guard = 0;
        while (steep == 0 && guard < 10) {
            multipleSteep = multipleSteep * 1000;
            steep = Math.round((heightGraph / (double) max) * multipleSteep);
            guard++;
        }
    }


    private static final int[] NICE_STEPS = {
            1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500,
            1000, 2000, 2500, 5000, 10000, 20000, 25000, 50000, 100000
    };


    /**
     * Vrátí nejbližší krok z pole NICE_STEPS pro zadanou hodnotu.
     */
    private int getNiceStep(double val) {
        for (int step : NICE_STEPS) {
            if (step >= val) {
                return step;
            }
        }
        return NICE_STEPS[NICE_STEPS.length - 1];
    }


    /**
     * Inicializuje výchozí rozestup mřížky pro načtená data/periodu.
     */
    private void initMultipleForDataset() {
        multipleNeedsInit = true;
    }


    /**
     * Ujistí se, že rozestup multiple je při startu/změně nastaven na výchozí přirozený krok (např. 250 kWh u ročního grafu).
     */
    private void ensureValidMultiple() {
        if (steep <= 0) {
            if (multiple <= 0) multiple = 10;
            return;
        }

        double effectiveSteep = steep / multipleSteep;
        int minLabelSpacingPx = dipToPx(20);
        double minAllowedStep = minLabelSpacingPx / effectiveSteep;

        if (multipleNeedsInit) {
            double targetStep = dipToPx(32) / effectiveSteep;
            double finalTarget = Math.max(targetStep, minAllowedStep);
            multiple = getNiceStep(finalTarget);
            multipleNeedsInit = false;
        } else if (multiple < minAllowedStep) {
            multiple = getNiceStep(minAllowedStep);
        }
    }





    /**
     * Nastaví typ zobrazení grafu.
     *
     * @param b true = zobrazení čárového grafu; false = zobrazení sloupcového grafu
     */
    public void setTypeShowGraph(boolean b) {
        this.graphType = b;
        invalidate();
    }


    /**
     * Posluchač dotyku.
     *
     * @param event událost dotyku
     * @return true pokud byla událost zpracována
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int xTouch;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xTouch = (int) event.getX();
                xClick = xTouch;
                break;

            case MotionEvent.ACTION_MOVE:
                xTouch = (int) event.getX();
                switch (period) {
                    case 0:
                        xMoveGraphMonth = xTouch - xClick + xMoveGraphAfterMonth;
                        if (xMoveGraphMonth > 0)
                            xMoveGraphMonth = 0;
                        if (xMoveGraphMonth < xMoveGraphMaxMonth)
                            xMoveGraphMonth = xMoveGraphMaxMonth;
                        break;
                    case 1:
                    case 2:
                        xMoveGraphYear = xTouch - xClick + xMoveGraphAfterYear;
                        if (xMoveGraphYear > 0)
                            xMoveGraphYear = 0;
                        if (xMoveGraphYear < xMoveGraphMaxYear)
                            xMoveGraphYear = xMoveGraphMaxYear;
                        break;
                }
                break;

            case MotionEvent.ACTION_UP:
                performClick();
                switch (period) {
                    case 0:
                        xMoveGraphAfterMonth = xMoveGraphMonth;
                        break;
                    case 1:
                    case 2:
                        xMoveGraphAfterYear = xMoveGraphYear;
                        break;
                }
                break;

            default:
                break;

        }
        invalidate();
        return true;
    }


    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }


    /**
     * Vypočítá maximální šířku grafu a nastaví maximální posun.
     */
    private void setMaxMoveGraph() {
        int sizeList = 0;
        switch (period) {
            case 0:
                sizeList = Math.max(0, monthlyConsuption.size() - 1);
                break;
            case 1:
                sizeList = yearConsuption.size();
                break;
            case 2:
                if (monthsConsuption.isEmpty() || compareMonth >= monthsConsuption.size()) break;
                sizeList = monthsConsuption.get(compareMonth).size();
                break;
        }

        int graphSize = (dipToPx(30) * (sizeList + 2)) + textMeasure;
        switch (period) {
            case 0:
                if (width < graphSize)
                    xMoveGraphMaxMonth = width - graphSize;
                else
                    xMoveGraphMaxMonth = 0;
                break;
            case 1:
            case 2:
                if (width < graphSize)
                    xMoveGraphMaxYear = width - graphSize;
                else
                    xMoveGraphMaxYear = 0;
                break;
        }

        if (shouldScrollToEndMonth && period == 0) {
            xMoveGraphMonth = xMoveGraphMaxMonth;
            xMoveGraphAfterMonth = xMoveGraphMaxMonth;
            if (width > 0 && !monthlyConsuption.isEmpty()) {
                shouldScrollToEndMonth = false;
            }
        } else {
            if (xMoveGraphMonth < xMoveGraphMaxMonth) {
                xMoveGraphMonth = xMoveGraphMaxMonth;
                xMoveGraphAfterMonth = xMoveGraphMaxMonth;
            }
            if (xMoveGraphMonth > 0) {
                xMoveGraphMonth = 0;
                xMoveGraphAfterMonth = 0;
            }
        }

        if (shouldScrollToEndYear && (period == 1 || period == 2)) {
            xMoveGraphYear = xMoveGraphMaxYear;
            xMoveGraphAfterYear = xMoveGraphMaxYear;
            if (width > 0 && (!yearConsuption.isEmpty() || !monthsConsuption.isEmpty())) {
                shouldScrollToEndYear = false;
            }
        } else {
            if (xMoveGraphYear < xMoveGraphMaxYear) {
                xMoveGraphYear = xMoveGraphMaxYear;
                xMoveGraphAfterYear = xMoveGraphMaxYear;
            }
            if (xMoveGraphYear > 0) {
                xMoveGraphYear = 0;
                xMoveGraphAfterYear = 0;
            }
        }
    }


    /**
     * Nastaví proměnnou pro zobrazení/skrytí VT.
     *
     * @param showVT true=zobrazit; false=skrýt
     */
    public void setShowVT(boolean showVT) {
        this.showVT = showVT;
        setMinMaxMonthly();
        setMinMaxYear();
        invalidate();
    }


    /**
     * Nastaví proměnnou pro zobrazení/skrytí NT.
     *
     * @param showNT true=zobrazit; false=skrýt
     */
    public void setShowNT(boolean showNT) {
        this.showNT = showNT;
        setMinMaxMonthly();
        setMinMaxYear();
        invalidate();
    }


    /**
     * Nastaví periodu pro zobrazení měsíčního, ročního nebo porovnávacího grafu.
     *
     * @param period 0=měsíční; 1=roční; 2=porovnávací
     */
    public void changePeriod(int period) {
        this.period = period;
        if (period == 0) {
            shouldScrollToEndMonth = true;
        } else {
            shouldScrollToEndYear = true;
        }
        setHeightGraph();
        setMinMaxMonthly();
        setMinMaxYear();
        setMaxMoveGraph();
        initMultipleForDataset();
        animated();
    }


    /**
     * Nastaví délku a krok animace při zobrazování grafu.
     */
    private void animated() {
        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }
        final int count = 1000;
        valueAnimator = ValueAnimator.ofInt(0, count);
        valueAnimator.setDuration(1000L);

        valueAnimator.addUpdateListener((ValueAnimator valueAnimator1) -> {
            switch (period) {
                case 0:
                    for (int j = 0; j < monthlyConsuption.size(); j++) {
                        monthlyConsuption.get(j).setAnimate((int) valueAnimator1.getAnimatedValue(), count);
                    }
                    break;
                case 1:
                    for (int j = 0; j < yearConsuption.size(); j++) {
                        yearConsuption.get(j).setAnimate((int) valueAnimator1.getAnimatedValue(), count);
                    }
                    break;
                case 2:
                    if (monthsConsuption.isEmpty() || compareMonth >= monthsConsuption.size())
                        break;
                    for (int j = 0; j < monthsConsuption.get(compareMonth).size(); j++) {
                        monthsConsuption.get(compareMonth).get(j).setAnimate((int) valueAnimator1.getAnimatedValue(), count);
                    }
                    break;
            }
            moveConsuption = count - (int) valueAnimator1.getAnimatedValue();
            invalidate();
        });

        valueAnimator.start();
    }


    /**
     * Nastaví barvy pro VT a NT graf.
     *
     * @param VT barva VT
     * @param NT barva NT
     */
    public void setColors(int VT, int NT) {
        this.colorVT = VT;
        this.colorNT = NT;
        invalidate();
    }


    /**
     * Nastaví měsíc pro porovnání.
     *
     * @param compareMonth index porovnávaného měsíce
     */
    public void setCompareMonth(int compareMonth) {
        this.compareMonth = compareMonth;
        if (this.compareMonth < 0)
            this.compareMonth = 0;
        if (this.compareMonth > 11)
            this.compareMonth = 11;
        shouldScrollToEndYear = true;
        initMultipleForDataset();
        animated();
    }


    /**
     * Zvětší hustotu mřížky (přejde okamžitě na nejbližší menší krok v mřížce).
     */
    public void setCofUp() {
        double currentMax = getEffectiveMax();
        setSteep((int) currentMax);
        if (steep <= 0) return;

        double effectiveSteep = steep / multipleSteep;
        int minLabelSpacingPx = dipToPx(22);
        double minAllowedStep = minLabelSpacingPx / effectiveSteep;

        int currentMultiple = multiple;
        int nextSmallerStep = currentMultiple;
        for (int i = NICE_STEPS.length - 1; i >= 0; i--) {
            if (NICE_STEPS[i] < currentMultiple) {
                nextSmallerStep = NICE_STEPS[i];
                break;
            }
        }

        if (nextSmallerStep >= minAllowedStep) {
            multiple = nextSmallerStep;
        } else {
            multiple = getNiceStep(minAllowedStep);
        }
        invalidate();
    }


    /**
     * Zmenší hustotu mřížky (přejde okamžitě na nejbližší větší krok v mřížce).
     */
    public void setCofDown() {
        double currentMax = getEffectiveMax();
        setSteep((int) currentMax);
        if (steep <= 0) return;

        int currentMultiple = multiple;
        int nextLargerStep = currentMultiple;
        for (int step : NICE_STEPS) {
            if (step > currentMultiple) {
                nextLargerStep = step;
                break;
            }
        }

        multiple = nextLargerStep;
        invalidate();
    }


    /**
     * Převod dp na px.
     *
     * @param dpValue hodnota v dp
     * @return hodnota v px
     */
    private int dipToPx(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
