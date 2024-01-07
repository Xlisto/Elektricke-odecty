package cz.xlisto.odecty.modules.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.utils.DetectScreenMode;

import static cz.xlisto.odecty.utils.DensityUtils.*;

/**
 * Xlisto 03.01.2024 12:18
 */
public class GraphTotalHdoView extends View {
    private static final String TAG = "GraphTotalHdoView";
    private int size;
    private int padding;
    private int centerX;
    private int centerY;
    private int radius;
    private Paint pTimeTUV, pTimeTAR, pTimePV, pNumbers, pTick, pClock, pLegend;
    private ArrayList<HdoModel> models;
    private long timeShift;
    private boolean showTAR, showPV, showTUV;


    public GraphTotalHdoView(Context context) {
        super(context);
        init();
    }


    public GraphTotalHdoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    public GraphTotalHdoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    public GraphTotalHdoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }


    private void init() {
        //colorClock = Color.BLACK;
    }


    private void init(AttributeSet attrs) {
        setSaveEnabled(true);

        TypedArray a = getContext().obtainStyledAttributes(attrs, cz.xlisto.odecty.R.styleable.GraphTotalHdoView);
        int colorClock = a.getColor(cz.xlisto.odecty.R.styleable.GraphTotalHdoView_colorClock, Color.BLACK);
        int colorTimeTUV = a.getColor(R.styleable.GraphTotalHdoView_colorTimeTUV, Color.GREEN);
        int colorTimeTAR = a.getColor(R.styleable.GraphTotalHdoView_colorTimeTAR, Color.BLUE);
        int colorTimePV = a.getColor(R.styleable.GraphTotalHdoView_colorTimePV, Color.CYAN);
        a.recycle();

        pTimeTUV = new Paint();
        pTimeTUV.setColor(colorTimeTUV);
        pTimeTUV.setStyle(Paint.Style.FILL);
        pTimeTUV.setStrokeWidth(dpToPx(getContext(), 1));

        pTimeTAR = new Paint();
        pTimeTAR.setColor(colorTimeTAR);
        pTimeTAR.setStyle(Paint.Style.FILL);
        pTimeTAR.setStrokeWidth(dpToPx(getContext(), 1));

        pTimePV = new Paint();
        pTimePV.setColor(colorTimePV);
        pTimePV.setStyle(Paint.Style.FILL);
        pTimePV.setStrokeWidth(dpToPx(getContext(), 1));

        pNumbers = new Paint();
        pNumbers.setColor(colorClock);
        pNumbers.setStyle(Paint.Style.FILL);
        pNumbers.setStrokeWidth(dpToPx(getContext(), 1));
        pNumbers.setTextSize(dpToPx(getContext(), 8)); // Nastavení velikosti textu
        pNumbers.setTextAlign(Paint.Align.CENTER); // Zarovnání textu na střed

        pLegend = new Paint();
        pLegend.setColor(colorClock);
        pLegend.setStyle(Paint.Style.FILL);
        pLegend.setStrokeWidth(dpToPx(getContext(), 1));
        pLegend.setTextSize(dpToPx(getContext(), 10)); // Nastavení velikosti textu
        pLegend.setTextAlign(Paint.Align.LEFT); // Zarovnání textu na střed

        pTick = new Paint();
        pTick.setColor(Color.RED);
        pTick.setStyle(Paint.Style.FILL);
        pTick.setStrokeWidth(dpToPx(getContext(), 2));
        pTick.setStrokeCap(Paint.Cap.ROUND);

        pClock = new Paint();
        pClock.setColor(colorClock);
        pClock.setStyle(Paint.Style.STROKE);
        pClock.setStrokeWidth(dpToPx(getContext(), 2));

        invalidate();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        padding = dpToPx(getContext(), 5);

        //ciferník
        centerX = size / 4; // Střed kruhu
        centerY = size / 4;
        radius = size / 4 - padding; // Poloměr kruhu
        if (DetectScreenMode.isLandscape(getContext())) {
            radius = size / 2 - padding;
            centerX = size / 2; // Střed kruhu
            centerY = size / 2;
        }

        drawTime(canvas);
        drawNumbers(canvas);
        drawTicks(canvas);
        drawClock(canvas);
        drawLegend(canvas);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        size = Math.min(width, height);
        if (DetectScreenMode.isLandscape(getContext()))
            setMeasuredDimension(size * 2, size);
        else
            setMeasuredDimension(size / 2, size / 2);
    }


    /**
     * Vykreslí časové intervaly
     *
     * @param canvas plátno
     */
    private void drawTime(Canvas canvas) {
        //vykreslení výseče času
        if (models == null) return;

        // Procházení seznamu a kreslení výsečí
        for (HdoModel model : models) {
            // Převod času na úhly
            float startAngle = convertTimeToAngle(model.getTimeFrom()); // Převod času začátku na úhel
            float endAngle = convertTimeToAngle(model.getTimeUntil()); // Převod času konce na úhel

            // Výpočet rozpětí úhlů pro výseč
            float sweepAngle = endAngle - startAngle;
            if (sweepAngle < 0) sweepAngle += 360; // Korekce pro přesah přes půlnoc
            int smaller = dpToPx(getContext(), 13);
            if (DetectScreenMode.isLandscape(getContext()))
                smaller = dpToPx(getContext(), 16);
            Log.w(TAG, "drawTime: " + model.getRele());

            if (model.getRele().contains("TUV") || !model.getRele().contains("TAR") && !model.getRele().contains("PV")) {
                // Vykreslení výseče
                RectF oval = new RectF(padding, padding, (float) size / 2 - padding, (float) size / 2 - padding);
                if (DetectScreenMode.isLandscape(getContext()))
                    oval = new RectF(padding, padding, size - padding, size - padding);
                canvas.drawArc(oval, startAngle, sweepAngle, true, pTimeTUV);
            } else if (model.getRele().contains("TAR")) {
                // Vykreslení výseče
                RectF oval = new RectF(padding + smaller, padding + smaller, (float) size / 2 - padding - smaller, (float) size / 2 - padding - smaller);
                if (DetectScreenMode.isLandscape(getContext()))
                    oval = new RectF(padding + smaller, padding + smaller, size - padding - smaller, size - padding - smaller);
                canvas.drawArc(oval, startAngle, sweepAngle, true, pTimeTAR);
            } else if (model.getRele().contains("PV")) {
                // Vykreslení výseče
                smaller *= 2;
                RectF oval = new RectF(padding + smaller, padding + smaller, (float) size / 2 - padding - smaller, (float) size / 2 - padding - smaller);
                if (DetectScreenMode.isLandscape(getContext()))
                    oval = new RectF(padding + smaller, padding + smaller, size - padding - smaller, size - padding - smaller);
                canvas.drawArc(oval, startAngle, sweepAngle, true, pTimePV);
            }

            if (model.getRele().equals("")) {
                showTUV = false;
                showTAR = false;
                showPV = false;
            }
            if (model.getRele().contains("TUV"))
                showTUV = true;
            if (model.getRele().contains("TAR"))
                showTAR = true;
            if (model.getRele().contains("PV"))
                showPV = true;
        }
    }


    /**
     * Vykreslí čísla ciferníku
     *
     * @param canvas plátno
     */
    private void drawNumbers(Canvas canvas) {
        //vykreslení čísel
        // Poloměr pro umístění textu (mírně menší než poloměr ciferníku)
        int textRadius = radius - 30; // Můžete upravit podle potřeby

        // Čísla a jejich odpovídající hodiny
        int[] hoursToShow = {0, 3, 6, 9, 12, 15, 18, 21};
        String[] numbersToShow = {"0", "3", "6", "9", "12", "15", "18", "21"};

        for (int i = 0; i < hoursToShow.length; i++) {
            // Výpočet úhlu pro každé číslo
            double angle = Math.toRadians(270 + hoursToShow[i] * 15);
            // Výpočet souřadnic pro text
            float x = (float) (centerX + textRadius * Math.cos(angle));
            float y = (float) (centerY + textRadius * Math.sin(angle)) + (pNumbers.getTextSize() / 2); // Posun pro vertikální zarovnání
            // Kreslení textu
            canvas.drawText(numbersToShow[i], x, y, pNumbers);
        }
    }


    /**
     * Vykreslí ručičku
     *
     * @param canvas plátno
     */
    private void drawTicks(Canvas canvas) {
        // ručička
        // Předpokládejme, že 'hours' je hodina ve formátu 0 až 23
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + timeShift);
        int hours = calendar.get(Calendar.HOUR_OF_DAY); // Aktuální hodina dne
        double minutes = calendar.get(Calendar.MINUTE); // Aktuální minuta

        // Výpočet úhlu pro hodinovou ručičku
        double hourAngle = 270 + hours * 15 + minutes * 0.25f;

        // Výpočet souřadnic koncového bodu ručičky
        int handLength = radius - dpToPx(getContext(), 8); // Délka ručičky, můžete upravit
        double hourRadians = Math.toRadians(hourAngle);
        int handEndX = (int) (centerX + handLength * Math.cos(hourRadians));
        int handEndY = (int) (centerY + handLength * Math.sin(hourRadians));

        // Kreslení hodinové ručičky
        canvas.drawLine(centerX, centerY, handEndX, handEndY, pTick);
        canvas.drawCircle(centerX, centerY, dpToPx(getContext(), 3), pTick);
    }


    /**
     * Vykreslí ciferník
     *
     * @param canvas plátno
     */
    private void drawClock(Canvas canvas) {

        int tickLength = dpToPx(getContext(), 3); // Délka každé úsečky (tick)
        int tickCount = 24; // Počet úseček (například 12 pro hodinové značky)

        for (int i = 0; i < tickCount; i++) {
            // Výpočet úhlu pro každou úsečku
            double angle = Math.toRadians(i * ((double) 360 / tickCount));
            int startX = (int) (centerX + radius * Math.cos(angle));
            int startY = (int) (centerY + radius * Math.sin(angle));
            int endX = (int) (centerX + (radius - tickLength) * Math.cos(angle));
            int endY = (int) (centerY + (radius - tickLength) * Math.sin(angle));

            // Kreslení úseček
            canvas.drawLine(startX, startY, endX, endY, pClock);
        }

        if (DetectScreenMode.isLandscape(getContext()))
            canvas.drawArc(padding, padding, size - padding, size - padding, 0, 360, false, pClock);
        else
            canvas.drawArc(padding, padding, (float) size / 2 - padding, (float) size / 2 - padding, 0, 360, false, pClock);
    }


    /**
     * Vykreslí legendu
     *
     * @param canvas plátno
     */
    private void drawLegend(Canvas canvas) {
        // Legenda
        int legendSize = dpToPx(getContext(), 11);
        if (DetectScreenMode.isLandscape(getContext()))
            legendSize = dpToPx(getContext(), 15);
        int legendPadding = dpToPx(getContext(), 8);
        int legendTextSize = dpToPx(getContext(), 8);
        int legendTextPadding = dpToPx(getContext(), 7);

        int legendX = size / 2 + padding;
        int legendY = padding;

        if (DetectScreenMode.isLandscape(getContext())) {
            legendX = size + padding;
        }

        // Vykreslení legendy
        RectF oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
        canvas.drawArc(oval, 0, 360, true, pClock);
        canvas.drawText("Čas VT", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);

        legendY += legendSize + legendPadding;

        oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
        canvas.drawArc(oval, 0, 360, true, pTick);
        canvas.drawText("Aktuální čas", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);

        legendY += legendSize + legendPadding;

        oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
        canvas.drawArc(oval, 0, 360, true, pTimeTUV);
        if (showTUV)
            canvas.drawText("Čas TUV", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);
        else
            canvas.drawText("Čas NT", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);

        if (showTAR) {
            legendY += legendSize + legendPadding;

            oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
            canvas.drawArc(oval, 0, 360, true, pTimeTAR);
            canvas.drawText("Čas TAR", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);
        }

        if (showPV) {
            legendY += legendSize + legendPadding;

            oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
            canvas.drawArc(oval, 0, 360, true, pTimePV);
            canvas.drawText("Čas PV", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);
        }
    }


    /**
     * Převede čas na úhel
     *
     * @param time čas ve formátu HH:mm
     * @return úhel
     */
    private float convertTimeToAngle(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return 270 + (hours * 15) + (minutes * 0.25f); // 15 stupňů na hodinu, 0.25 stupně na minutu
    }


    /**
     * Nastaví modely pro vykreslení časových intervalů a posun hodinové ručičky
     *
     * @param models    modely pro vykreslení časových intervalů
     * @param timeShift posun hodinové ručičky
     */
    public void setHdoModels(ArrayList<HdoModel> models, long timeShift) {
        this.models = models;
        this.timeShift = timeShift;
        invalidate();
    }
}
