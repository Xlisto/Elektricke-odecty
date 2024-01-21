package cz.xlisto.odecty.modules.graphmonth;

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

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.ConsuptionModel;
import cz.xlisto.odecty.utils.ColorHelper;
import cz.xlisto.odecty.utils.ColorUtils;
import cz.xlisto.odecty.utils.DensityUtils;


/**
 * Xlisto 20.08.2023 22:01
 */
public class GraphMonthView extends View {
    private static final String TAG = "GraphMonthView";
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
    private int multiple;
    private int left;
    private int textMeasure;
    private double steep, multipleSteep;
    private int period = 1;
    private boolean graphType = true;
    private double maxMonthly = 0;
    private double maxYear = 0;
    private int colorVT = Color.RED;
    private int colorNT = Color.BLUE;
    int xClick = 0;//souřadnice při dotyku na obrazovku
    int xMoveGraphYear = 0;//počet o kolik se má posunout roční graf
    int xMoveGraphAfterYear = 0;//počet posunutí ročního grafu při předchozím posunu;
    int xMoveGraphMaxYear = 0;//maximální posun grafu ročního grafu
    int xMoveGraphMonth = 0;//počet o kolik se má posunout měsíční graf
    int xMoveGraphAfterMonth = 0;//počet posunutí měsíční grafu při předchozím posunu;
    int xMoveGraphMaxMonth = 0;//maximální posun grafu měsíčního grafu
    int compareMonth = 0;//vybraný měsíc pro porovnání
    private ArrayList<ConsuptionModel> monthlyConsuption = new ArrayList<>();
    private ArrayList<ConsuptionModel> yearConsuption = new ArrayList<>();
    private ArrayList<ArrayList<ConsuptionModel>> monthsConsuption = new ArrayList<>();
    private boolean showVT = true;
    private boolean showNT = true;
    private int cof = 60;//koeficient zvětšení/změnšení přesnosti grafu
    private int moveConsuption = 0;//posunutí sloupcového grafu


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
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (period == 0 || period == 2) {
            setSteep((int) maxMonthly);
            setMultiple();
        } else if (period == 1) {
            setSteep((int) maxYear);
            setMultiple();

        }
        setAxisHorizontalDescription(canvas);//první vykleslení je pro získání šířky
        setAxisHorizontal(canvas);
        drawLineVTNT(canvas);
        setAxisHorizontalDescription(canvas);
        setMaxMoveGraph();
        setAxisVerticalDescription(canvas);

    }


    /**
     * Nastavení velikosti widgetu a získání aktuálních rozměrů
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
        init();
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
        state = bundle.getParcelable("superstate");
        super.onRestoreInstanceState(state);
    }


    /**
     * Nastaví výšku grafu s odečtením na pozici dolního popisku os
     */
    private void setHeightGraph() {
        if (period == 1) {
            heightGraph = height - dipToPx(12);
        } else {
            heightGraph = height - dipToPx(22);
        }
    }


    public void setConsuption(ConsuptionContainer consuption) {
        if (consuption != null) {
            this.monthlyConsuption = consuption.getMonthlyConsuption();
            this.yearConsuption = consuption.getYearConsuption();
            this.monthsConsuption = consuption.getMonthsConsuptionsArray();
        }
        setMinMaxMonthly();
        setMinMaxYear();
        init();
    }


    /**
     * Nastaví maximální a minimální hodnotu pro měsíční graf
     */
    private void setMinMaxMonthly() {
        double maxMonthlyVT = Double.MIN_VALUE;
        double minMonthlyVT = Double.MAX_VALUE;
        double maxMonthlyNT = Double.MIN_VALUE;
        double minMonthlyNT = Double.MAX_VALUE;
        maxMonthly = Double.MIN_VALUE;


        for (ConsuptionModel month : monthlyConsuption) {
            if (showVT) {
                if (month.getConsuptionVT() > maxMonthlyVT)
                    maxMonthlyVT = month.getConsuptionVT();
                if (month.getConsuptionVT() < minMonthlyVT)
                    minMonthlyVT = month.getConsuptionVT();
            }
            if (showNT) {
                if (month.getConsuptionNT() > maxMonthlyNT)
                    maxMonthlyNT = month.getConsuptionNT();
                if (month.getConsuptionNT() < minMonthlyNT)
                    minMonthlyNT = month.getConsuptionNT();
            }
        }

        maxMonthly = Math.max(maxMonthlyVT, maxMonthlyNT);
        maxMonthly = maxMonthly + (maxMonthly * 0.15);
    }


    /**
     * Nastaví maximální a minimální hodnotu pro roční graf
     */
    private void setMinMaxYear() {
        double maxYearVT = Double.MIN_VALUE;
        double minYearVT = Double.MAX_VALUE;
        double maxYearNT = Double.MIN_VALUE;
        double minYearNT = Double.MAX_VALUE;

        for (ConsuptionModel year : yearConsuption) {
            if (year.getConsuptionVT() > maxYearVT)
                maxYearVT = year.getConsuptionVT();
            if (year.getConsuptionVT() < minYearVT)
                minYearVT = year.getConsuptionVT();
            if (year.getConsuptionNT() > maxYearNT)
                maxYearNT = year.getConsuptionNT();
            if (year.getConsuptionNT() < minYearNT)
                minYearNT = year.getConsuptionNT();
        }

        maxYear = Math.max(maxYearVT, maxYearNT);
        maxYear = maxYear + (maxYear * 0.15);
    }


    /**
     * Vykreslí vodorovné osy
     *
     * @param canvas plátno
     */
    private void setAxisHorizontal(Canvas canvas) {
        Paint axisHorizontal = new Paint();

        axisHorizontal.setColor(getResources().getColor(R.color.color_axis));
        axisHorizontal.setStrokeWidth(1);
        axisHorizontal.setStyle(Paint.Style.FILL);

        int stopY = heightGraph;
        int i = 1;

        //pokud nejsou žádná data, steep je 0
        if (steep > 0) {
            while (stopY > 0) {
                //vodorovná osa
                drawLine(canvas, axisHorizontal, 0, stopY, width, stopY);
                stopY = heightGraph - (int) (i * (steep * multiple / multipleSteep));
                i++;
            }
        }
    }


    /**
     * Nakreslí popisky k vodorovným osám na vykresleném pozadí, které skryje graf při posunu
     *
     * @param canvas plátno
     */
    private void setAxisHorizontalDescription(Canvas canvas) {
        Paint background = new Paint();
        Paint text = new Paint();

        background.setColor(getResources().getColor(R.color.color_graph_background));
        text.setColor(getResources().getColor(R.color.color_axis));
        text.setTextSize(dipToPx(10));

        int stopY = heightGraph;
        int description = 0;
        canvas.drawRect(0, 0, textMeasure, height, background);
        //šířka textu
        textMeasure = 0;
        if (steep > 0) {
            //vypočet maximální hodnoty (délky textu) grafu
            textMeasure = (int) text.measureText(multiple * (Math.ceil(heightGraph / steep / multiple * multipleSteep)) + " kWh") + dipToPx(5);
            left = textMeasure;//nastavení odsazení grafu zleva
        }

        //pokud nejsou žádná data, steep je 0
        if (steep > 0) {
            while (stopY > 0) {
                //popisek vodorovné osy
                drawText(canvas, text, 0, stopY, description + " kWh");
                stopY = stopY - (int) (steep * multiple / multipleSteep);
                description += multiple;
            }
        }
    }


    /**
     * Vypíše popisy k svislým osám
     *
     * @param canvas plátno
     */
    private void setAxisVerticalDescription(Canvas canvas) {
        Paint text = new Paint();
        Paint background = new Paint();

        text.setColor(getResources().getColor(R.color.color_axis));
        text.setTextSize(dipToPx(10));

        background.setColor(getResources().getColor(R.color.color_graph_background));

        ArrayList<ConsuptionModel> consuption = new ArrayList<>();
        int xMoveGraph = 0;
        switch (period) {
            case 0:
                consuption = monthlyConsuption;
                xMoveGraph = xMoveGraphMonth;
                break;
            case 1:
                consuption = yearConsuption;
                xMoveGraph = xMoveGraphYear;
                break;
            case 2:
                if (compareMonth < monthsConsuption.size())
                    consuption = monthsConsuption.get(compareMonth);
                xMoveGraph = xMoveGraphYear;
                break;
        }

        if (period == 0 || period == 2)
            canvas.drawRect(0, height - dipToPx(21), width, height, background);
        else if (period == 1)
            canvas.drawRect(0, height - dipToPx(11), width, height, background);

        for (int i = 0; i < consuption.size(); i++) {
            if (period == 0 || period == 2) {

                int textMeasureMonth = (int) text.measureText((consuption.get(i)).getDateMonthAsStringShort());
                drawText(canvas, text, (dipToPx(30) * (i + 1)) + left + xMoveGraph - (textMeasureMonth / 2), height - dipToPx(10), (consuption.get(i)).getDateMonthAsStringShort());
                int textMeasureYear = (int) text.measureText(consuption.get(i).getYearAsString());
                drawText(canvas, text, (dipToPx(30) * (i + 1)) + left + xMoveGraph - (textMeasureYear / 2), height, consuption.get(i).getYearAsString());
            } else if (period == 1) {

                int textMeasureYear = (int) text.measureText(consuption.get(i).getYearAsString());
                drawText(canvas, text, (dipToPx(30) * (i + 1)) + left + xMoveGraph - (textMeasureYear / 2), height, consuption.get(i).getYearAsString());
            }
        }


    }


    /**
     * Vykreslí graf VT a NT spotřeby
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
        line.setColor(getResources().getColor(R.color.color_axis));
        lineVT.setStrokeWidth(strokeWidth);
        lineNT.setStrokeWidth(strokeWidth);
        int borderGraph=DensityUtils.dpToPx(getContext(), 1);
        lineVTBorder.setStrokeWidth(borderGraph);
        lineNTBorder.setStrokeWidth(borderGraph);
        line.setStrokeWidth(1);
        lineVT.setStyle(style);
        lineNT.setStyle(style);
        line.setStyle(style);
        lineVTBorder.setStyle(Paint.Style.STROKE);
        lineNTBorder.setStyle(Paint.Style.STROKE);
        line.setPathEffect(new DashPathEffect(new float[]{10f, 20f}, 0f));


        ArrayList<ConsuptionModel> consuption = new ArrayList<>();
        int xMoveGraph = 0;
        switch (period) {
            case 0:
                consuption = monthlyConsuption;
                xMoveGraph = xMoveGraphMonth;
                break;
            case 1:
                consuption = yearConsuption;
                xMoveGraph = xMoveGraphYear;
                break;
            case 2:
                if (compareMonth < monthsConsuption.size())
                    consuption = monthsConsuption.get(compareMonth);
                xMoveGraph = xMoveGraphYear;
                break;
        }

        int startX, stopX, lastX = 0;
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
                    drawRect(canvas, lineVT, startX, heightGraph, stopX - dipToPx(12)+borderGraph, heightGraph - (int) (stopVTStart * steep / multipleSteep));
                    drawRect(canvas, lineVTBorder, startX, heightGraph, stopX - dipToPx(12)+borderGraph, heightGraph - (int) (stopVTStart * steep / multipleSteep));
                }
            }
            if (showNT) {
                if (graphType) {
                    drawLine(canvas, lineNT, startX, heightGraph - (int) (stopNTStart * steep / multipleSteep), stopX, heightGraph - (int) (stopNTEnd * steep / multipleSteep));
                    drawCircle(canvas, lineNT, startX, heightGraph - (int) (stopNTStart * steep / multipleSteep), dipToPx(3));

                } else {

                    drawRect(canvas, lineNT, startX+borderGraph, heightGraph, stopX + dipToPx(12), heightGraph - (int) (stopNTStart * steep / multipleSteep));
                    drawRect(canvas, lineNTBorder, startX+borderGraph, heightGraph, stopX + dipToPx(12), heightGraph - (int) (stopNTStart * steep / multipleSteep));
                }
            }
            int minConsuption = Math.min(stopVTStart, stopNTStart);
            if (!showNT) minConsuption = stopVTStart;
            if (!showVT) minConsuption = stopNTStart;

            if (graphType)
                //vykreslení pomocných svislých os
                drawLine(canvas, line, startX, heightGraph - dipToPx(10), startX, heightGraph - (int) (minConsuption * steep / multipleSteep) + dipToPx(10));
            else
                //vykreslení spotřeby ve spodní části sloupců
                drawConsuption(canvas, startX+borderGraph/2, consuption.get(i).getConsuptionVT(), consuption.get(i).getConsuptionNT());
        }

        //poslední záznam
        if (consuption.size() > 1) {
            ConsuptionModel lastMonthlyConsuption = consuption.get(consuption.size() - 1);
            int stopVTStart = lastMonthlyConsuption.getConsuptionVTAnimace();
            int stopNTStart = lastMonthlyConsuption.getConsuptionNTAnimace();
            if (showVT) {
                if (graphType)
                    drawCircle(canvas, lineVT, lastX, heightGraph - (int) (stopVTStart * steep / multipleSteep), dipToPx(3));
                else {
                    drawRect(canvas, lineVT, lastX, heightGraph, lastX - dipToPx(12)+borderGraph, heightGraph - (int) (stopVTStart * steep / multipleSteep));
                    drawRect(canvas, lineVTBorder, lastX, heightGraph, lastX - dipToPx(12)+borderGraph, heightGraph - (int) (stopVTStart * steep / multipleSteep));
                }
            }
            if (showNT) {
                if (graphType)
                    drawCircle(canvas, lineNT, lastX, heightGraph - (int) (stopNTStart * steep / multipleSteep), dipToPx(3));
                else {
                    drawRect(canvas, lineNT, lastX+borderGraph, heightGraph, lastX + dipToPx(12), heightGraph - (int) (stopNTStart * steep / multipleSteep));
                    drawRect(canvas, lineNTBorder, lastX+borderGraph, heightGraph, lastX + dipToPx(12), heightGraph - (int) (stopNTStart * steep / multipleSteep));
                }
            }
            //vykreslení spotřeby ve spodní části sloupců
            if (!graphType)
                drawConsuption(canvas, lastX, consuption.get(consuption.size() - 1).getConsuptionVT(), consuption.get(consuption.size() - 1).getConsuptionNT());

            //poslední záznam
            int minConsuption = Math.min(stopVTStart, stopNTStart);
            if (!showNT) minConsuption = stopVTStart;
            if (!showVT) minConsuption = stopNTStart;
            drawLine(canvas, line, lastX, heightGraph - dipToPx(10), lastX, heightGraph - (int) (minConsuption * steep / multipleSteep) + dipToPx(10));

            drawLegend(canvas, lineVT, lineNT);
        }
    }


    /**
     * Vykreslí čáru
     *
     * @param canvas plátno
     * @param paint  barva
     * @param startX počáteční x
     * @param startY počáteční y
     * @param stopX  koncové x
     * @param stopY  koncové y
     */
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
     * Vykreslí rámeček s legendou
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

        background.setColor(getResources().getColor(R.color.color_graph_background));
        border.setColor(getResources().getColor(R.color.color_axis));
        text.setColor(getResources().getColor(R.color.color_axis));
        textBold.setColor(getResources().getColor(R.color.color_axis));
        text.setTextSize(dipToPx(10));
        textBold.setTextSize(dipToPx(10));
        textBold.setTypeface(Typeface.create("Arial", Typeface.BOLD));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dipToPx(1));

        paintNT.setStrokeWidth(dipToPx(3));
        paintVT.setStrokeWidth(dipToPx(3));

        int left = textMeasure + dipToPx(2);
        int right = left + dipToPx(50);
        int bottom = dipToPx(30);
        if ((showVT && !showNT) || (!showVT && showNT))
            bottom = dipToPx(20);
        //zobrazení popisu tarifů
        canvas.drawRoundRect(left, 0, right, bottom, dipToPx(4), dipToPx(4), background);
        canvas.drawRoundRect(left, 0, right, bottom, dipToPx(4), dipToPx(4), border);
        //zobrazení typu grafu
        canvas.drawRoundRect(width - dipToPx(105), 0, width - dipToPx(2), dipToPx(30), dipToPx(4), dipToPx(4), background);
        canvas.drawRoundRect(width - dipToPx(105), 0, width - dipToPx(2), dipToPx(30), dipToPx(4), dipToPx(4), border);

        if (showNT) {
            //posun na místo VT
            int move = 0;
            if (!showVT)
                move = 11;
            canvas.drawText("NT", left + dipToPx(5), dipToPx(25 - move), text);
            canvas.drawLine(left + dipToPx(25), dipToPx(22 - move), left + dipToPx(40), dipToPx(22 - move), paintNT);
        }
        if (showVT) {
            canvas.drawText("VT", left + dipToPx(5), dipToPx(15), text);
            canvas.drawLine(left + dipToPx(25), dipToPx(11), left + dipToPx(40), dipToPx(11), paintVT);
        }

        //vykreslení popisu grafu - nadpis pro měsíční a roční graf
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

        //vykreslení pro nadpisu pro porovnávací graf
        if (period == 2) {
            canvas.drawText("Porovnávaný měsíc:", width - dipToPx(100), dipToPx(15), text);
            canvas.drawText(monthlyConsuption.get(compareMonth).getDateMonthAsStringLong(), width - dipToPx(100), dipToPx(25), textBold);
        }
    }


    /**
     * Vypíše spotřebu VT a NT ve spodní části sloupcového grafu
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
        background.setColor(getResources().getColor(R.color.color_graph_background));
        border.setColor(getResources().getColor(R.color.color_axis));
        border.setStyle(Paint.Style.STROKE);
        text.setColor(getResources().getColor(R.color.color_axis));
        text.setTextSize(dipToPx(8));
        int y = heightGraph - dipToPx(2);
        int textMeasureVT = (int) text.measureText("VT: " + DecimalFormatHelper.df2.format(vt));
        int textMeasureNT = (int) text.measureText("NT: " + DecimalFormatHelper.df2.format(nt));
        canvas.rotate(-90, x, y);
        int textMeasure = Math.max(textMeasureVT, textMeasureNT);
        if (x > width + 50)
            moveConsuption = 0;//deaktivuje animaci u spotřeby ve spodním části sloupcového grafu
        //VT
        if (showVT) {
            canvas.drawRoundRect(x - dipToPx(1) - moveConsuption, y - dipToPx(1), x + textMeasure + dipToPx(5) - moveConsuption, y - dipToPx(11), dipToPx(4), dipToPx(4), background);
            canvas.drawRoundRect(x - dipToPx(1) - moveConsuption, y - dipToPx(1), x + textMeasure + dipToPx(5) - moveConsuption, y - dipToPx(11), dipToPx(4), dipToPx(4), border);
            canvas.drawText("VT: " + DecimalFormatHelper.df2.format(vt), x + dipToPx(2) - moveConsuption, y - dipToPx(3), text);


        }
        //NT
        if (showNT) {
            canvas.drawRoundRect(x - dipToPx(1) - moveConsuption, y + dipToPx(1), x + textMeasure + dipToPx(5) - moveConsuption, y + dipToPx(11), dipToPx(4), dipToPx(4), background);
            canvas.drawRoundRect(x - dipToPx(1) - moveConsuption, y + dipToPx(1), x + textMeasure + dipToPx(5) - moveConsuption, y + dipToPx(11), dipToPx(4), dipToPx(4), border);
            canvas.drawText("NT: " + DecimalFormatHelper.df2.format(nt), x + dipToPx(2) - moveConsuption, y + dipToPx(9), text);

        }
        canvas.rotate(90, x, y);

    }


    /**
     * Výpočet velikosti v px 1 kWh na grafu a zaokrouhleno na celé číslo
     *
     * @param max maximální hodnota spotřeby
     */
    private void setSteep(int max) {
        multipleSteep = 1;
        steep = (heightGraph / (double) max);
        //pokud je max 0 - nejsou data - ukončí se
        if (max == 0) {
            steep = 0;
            return;
        }
        //pokud by vycházel steep 0, musí se použít násobek
        while (steep == 0) {
            multipleSteep = multipleSteep * 1000;
            steep = Math.round((heightGraph / (double) max) * multipleSteep);
        }
    }


    /**
     * Výpočet násobku pro rozestup mřížky
     */
    private void setMultiple() {
        //kontrola minimálního koeficientu pro přesnost grafu
        if (cof < 40)
            cof = 40;

        if (steep == 0)
            return;
        //kontrola rozsahu přesnosti grafu, koeficientem lze přesnost upravovat
        while ((steep * multiple) > cof) {
            multiple -= 5;
        }

        while ((steep * multiple) < cof) {
            multiple += 5;
        }
    }


    /**
     * Nastaví typ zobrazení grafu
     *
     * @param b true = zobrazení čárového grafu; false = zobrazení sloupcového grafu
     */
    public void setTypeShowGraph(boolean b) {
        this.graphType = b;
        init();
    }


    /**
     * Posluchač dotyku
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
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
                        if (xMoveGraphMonth > 0) //zamezení posunutí začátku grafu doprava
                            xMoveGraphMonth = 0;
                        if (xMoveGraphMonth < xMoveGraphMaxMonth) // zamezení posunutí konce grafu doleva
                            xMoveGraphMonth = xMoveGraphMaxMonth;
                        break;
                    case 1:
                    case 2:
                        xMoveGraphYear = xTouch - xClick + xMoveGraphAfterYear;
                        if (xMoveGraphYear > 0) //zamezení posunutí začátku grafu doprava
                            xMoveGraphYear = 0;
                        if (xMoveGraphYear < xMoveGraphMaxYear) // zamezení posunutí konce grafu doleva
                            xMoveGraphYear = xMoveGraphMaxYear;
                        break;
                }
                break;

            case MotionEvent.ACTION_UP:
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


    /**
     * Vypočítá maximální šířku grafu a nastaví maximální posun
     */
    private void setMaxMoveGraph() {
        int sizeList = 0;
        int sizeGraph = 0;
        //násobek
        int sizeGraphMonth = 1;
        int sizeGraphYear = 1;
        switch (period) {
            case 0:
                sizeList = monthlyConsuption.size() - 1;
                sizeGraph = sizeGraphMonth;
                break;
            case 1:
                sizeList = yearConsuption.size();
                sizeGraph = sizeGraphYear;
                break;
            case 2:
                //Při prvním průchodu cyklu po rotaci obrazovky je monthsConsuption.size() = 0
                if (monthsConsuption.size() == 0) break;
                sizeList = monthsConsuption.get(compareMonth).size();
                sizeGraph = sizeGraphYear;
                break;
        }

        int graphSize = (dipToPx(30) * (sizeList + 2)) * sizeGraph + textMeasure; //poslední souřadnice sloupce NT
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

        if (xMoveGraphMonth < xMoveGraphMaxMonth) {
            xMoveGraphMonth = xMoveGraphMaxMonth;
            xMoveGraphAfterMonth = xMoveGraphMaxMonth;
        }
        if (xMoveGraphYear < xMoveGraphMaxYear) {
            xMoveGraphYear = xMoveGraphMaxYear;
            xMoveGraphAfterYear = xMoveGraphMaxYear;
        }
    }


    /**
     * Nastaví proměnnou pro zobrazení/skrytí VT a vypočítá maximální velikost grafu
     *
     * @param showVT true=zobrazit; false=skrýt
     */
    public void setShowVT(boolean showVT) {
        this.showVT = showVT;
        setMinMaxMonthly();
        setMinMaxYear();
        init();
    }


    /**
     * Nastaví proměnnou pro zobrazení/skrytí NT a vypočítá maximální velikost grafu
     *
     * @param showNT true=zobrazit; false=skrýt
     */
    public void setShowNT(boolean showNT) {
        this.showNT = showNT;
        setMinMaxMonthly();
        setMinMaxYear();
        init();
    }


    /**
     * Nastaví periodu pro zobrazení měsíčního, ročního nebo porovnávacího grafu
     *
     * @param period 0=měsíční; 1=roční; 2=porovnávací
     */
    public void changePeriod(int period) {
        this.period = period;
        setHeightGraph();
        setMinMaxMonthly();
        setMinMaxYear();
        setMaxMoveGraph();
        init();
    }


    /**
     * Nastaví délku a krok animace při zobrazování grafu
     */
    private void animated() {

        final int count = 1000;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, count);
        //TODO: Nastavení duration na 0 se vypne animace
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
                    //Při prvním průchodu cyklu po rotaci obrazovky je monthsConsuption.size() = 0
                    if (monthsConsuption.size() == 0) break;
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
     * Nastaví barvy pro VT a NT graf
     *
     * @param VT barva VT
     * @param NT barva NT
     */
    public void setColors(int VT, int NT) {
        this.colorVT = VT;
        this.colorNT = NT;
        init();
    }


    /**
     * Nastaví měsíc pro porovnání
     *
     * @param compareMonth index porovnávaného měsíce
     */
    public void setCompareMonth(int compareMonth) {
        this.compareMonth = compareMonth;
        if (this.compareMonth < 0)
            this.compareMonth = 0;
        if (this.compareMonth > 11)
            this.compareMonth = 11;
        init();
    }


    /**
     * Nastaví koeficient pro přesnost grafu - čím menší číslo, tím je graf stručnější
     */
    public void setCofUp() {
        cof -= 20;
        multiple -= 5; //aktualizace při kliknutí, se pokaždé hodnota zvýšila, zatím nevím proč

        while ((steep * multiple) < (cof)) {
            cof -= 20;
        }
        init();
    }


    /**
     * Nastaví koeficient pro přesnost grafu - čím větší číslo, tím je graf přesnější
     */
    public void setCofDown() {
        cof += 20;

        while ((steep * multiple) > cof) {
            cof += 20;
        }

        init();
    }


    /**
     * Převod dp na px
     *
     * @param dpValue hodnota v dp
     * @return hodnota v px
     */
    private int dipToPx(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
