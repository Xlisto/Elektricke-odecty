package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.utils.BuilderHDOStack;


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
    private int currentTime, lastTime, showTime;
    private Paint pTimeTUV, pTimeTAR, pTimePV, pNumbers, pTick, pTickLegend, pClock, pClockLegend, pLegend, pTimeLeft, pTextTime;
    private ArrayList<HdoModel> modelsFromDatabase;
    private ArrayList<HdoModel> modelsForAllWeek;
    private long timeShift;
    private TimeHdo timeHdoNT;
    private TimeHdo timeHdoTUV;
    private TimeHdo timeHdoTAR;
    private TimeHdo timeHdoPV;
    private boolean showTAR, showPV, showTUV;
    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 1000);
        }
    };


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
        runnable.run();
        builderHDOLists();

        TypedArray a = getContext().obtainStyledAttributes(attrs, cz.xlisto.elektrodroid.R.styleable.GraphTotalHdoView);
        int colorClock = a.getColor(cz.xlisto.elektrodroid.R.styleable.GraphTotalHdoView_colorClock, Color.BLACK);
        int colorTimeTUV = a.getColor(R.styleable.GraphTotalHdoView_colorTimeTUV, Color.GREEN);
        int colorTimeTAR = a.getColor(R.styleable.GraphTotalHdoView_colorTimeTAR, Color.BLUE);
        int colorTimePV = a.getColor(R.styleable.GraphTotalHdoView_colorTimePV, Color.CYAN);
        a.recycle();

        boolean antiAliasing = true;

        pTimeTUV = new Paint();
        pTimeTUV.setColor(colorTimeTUV);
        pTimeTUV.setStyle(Paint.Style.FILL);
        pTimeTUV.setStrokeWidth(dpToPx(getContext(), 1));
        pTimeTUV.setAntiAlias(antiAliasing);

        pTimeTAR = new Paint();
        pTimeTAR.setColor(colorTimeTAR);
        pTimeTAR.setStyle(Paint.Style.FILL);
        pTimeTAR.setStrokeWidth(dpToPx(getContext(), 1));
        pTimeTAR.setAntiAlias(antiAliasing);

        pTimePV = new Paint();
        pTimePV.setColor(colorTimePV);
        pTimePV.setStyle(Paint.Style.FILL);
        pTimePV.setStrokeWidth(dpToPx(getContext(), 1));
        pTimePV.setAntiAlias(antiAliasing);

        pNumbers = new Paint();
        pNumbers.setColor(colorClock);
        pNumbers.setStyle(Paint.Style.FILL);
        pNumbers.setStrokeWidth(dpToPx(getContext(), 1));
        pNumbers.setTextSize(dpToPx(getContext(), 10)); // Nastavení velikosti textu
        pNumbers.setTextAlign(Paint.Align.CENTER); // Zarovnání textu na střed
        pNumbers.setAntiAlias(antiAliasing);

        pLegend = new Paint();
        pLegend.setColor(colorClock);
        pLegend.setStyle(Paint.Style.FILL);
        pLegend.setStrokeWidth(dpToPx(getContext(), 1));
        pLegend.setTextSize(dpToPx(getContext(), 10)); // Nastavení velikosti textu
        pLegend.setTextAlign(Paint.Align.LEFT); // Zarovnání textu doleva
        pLegend.setAntiAlias(antiAliasing);

        pTickLegend = new Paint();
        pTickLegend.setColor(Color.RED);
        pTickLegend.setStyle(Paint.Style.FILL);
        pTickLegend.setStrokeWidth(dpToPx(getContext(), 2));
        pTickLegend.setStrokeCap(Paint.Cap.ROUND);
        pTickLegend.setAntiAlias(antiAliasing);

        pTimeLeft = new Paint();
        pTimeLeft.setColor(colorClock);
        pTimeLeft.setStyle(Paint.Style.FILL);
        pTimeLeft.setStrokeWidth(dpToPx(getContext(), 1));
        pTimeLeft.setTextSize(dpToPx(getContext(), 14)); // Nastavení velikosti textu
        pTimeLeft.setTextAlign(Paint.Align.LEFT); // Zarovnání textu doleva
        pTimeLeft.setAntiAlias(antiAliasing);

        pTextTime = new Paint();
        pTextTime.setColor(colorClock);
        pTextTime.setStyle(Paint.Style.FILL);
        pTextTime.setStrokeWidth(dpToPx(getContext(), 1));
        pTextTime.setTextSize(dpToPx(getContext(), 14)); // Nastavení velikosti textu
        pTextTime.setTextAlign(Paint.Align.LEFT); // Zarovnání textu doleva
        pTextTime.setAntiAlias(antiAliasing);

        pTick = new Paint();
        pTick.setColor(Color.RED);
        pTick.setStyle(Paint.Style.FILL);
        pTick.setStrokeWidth(dpToPx(getContext(), 2));
        pTick.setStrokeCap(Paint.Cap.ROUND);
        pTick.setAntiAlias(antiAliasing);

        pClock = new Paint();
        pClock.setColor(colorClock);
        pClock.setStyle(Paint.Style.STROKE);
        pClock.setStrokeWidth(dpToPx(getContext(), 1));
        pClock.setAntiAlias(antiAliasing);

        pClockLegend = new Paint();
        pClockLegend.setColor(colorClock);
        pClockLegend.setStyle(Paint.Style.STROKE);
        pClockLegend.setStrokeWidth(dpToPx(getContext(), 1));
        pClockLegend.setAntiAlias(antiAliasing);

        lastTime = 0;
        getCurrentTime();

        animateTick();
        animatedOutText();

    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        padding = dpToPx(getContext(), 5);

        //ciferník
        centerX = size / 4; // Střed kruhu
        centerY = size / 4;
        radius = size / 4 - padding; // Poloměr kruhu


        drawTime(canvas);
        drawNumbers(canvas);
        drawTick(canvas);
        drawClock(canvas);
        drawLegend(canvas);
        drawTimeLeft(canvas);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        size = Math.min(width, height);
        setMeasuredDimension(size, size / 2 + dpToPx(getContext(), 60));//stačí na 20
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(runnable);
    }


    /**
     * Vykreslí časové intervaly
     *
     * @param canvas plátno
     */
    private void drawTime(Canvas canvas) {
        //vykreslení výseče času

        if (modelsForAllWeek == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + timeShift);

        for (HdoModel model : modelsForAllWeek) {
            // Přeskočí modely, které nejsou pro dnešní den
            if (model.getCalendarStart().get(Calendar.DAY_OF_MONTH) != calendar.get(Calendar.DAY_OF_MONTH))
                continue;

            // Převod času na úhly, úhly se vykreslují podle textové hodnoty timeFrom a timeUntil. Pokud zasahuje do druhého dne, bude výseč překrytá
            float startAngle = convertTimeToAngle(model.getTimeFrom()); // Převod času začátku na úhel
            float endAngle = convertTimeToAngle(model.getTimeUntil()); // Převod času konce na úhel

            // Výpočet rozpětí úhlů pro výseč
            float sweepAngle = endAngle - startAngle;
            if (sweepAngle < 0) sweepAngle += 360; // Korekce pro přesah přes půlnoc
            int smaller = (int) (radius * 0.3);

            if (model.getRele().contains("TUV") || !model.getRele().contains("TAR") && !model.getRele().contains("PV")) {
                // Vykreslení výseče
                RectF oval = new RectF(padding, padding, (float) size / 2 - padding, (float) size / 2 - padding);
                canvas.drawArc(oval, startAngle, sweepAngle, true, pTimeTUV);
            } else if (model.getRele().contains("TAR")) {
                // Vykreslení výseče
                RectF oval = new RectF(padding + smaller, padding + smaller, (float) size / 2 - padding - smaller, (float) size / 2 - padding - smaller);
                canvas.drawArc(oval, startAngle, sweepAngle, true, pTimeTAR);
            } else if (model.getRele().contains("PV")) {
                // Vykreslení výseče
                smaller *= 2;
                RectF oval = new RectF(padding + smaller, padding + smaller, (float) size / 2 - padding - smaller, (float) size / 2 - padding - smaller);
                canvas.drawArc(oval, startAngle, sweepAngle, true, pTimePV);
            }

            if (model.getRele().isEmpty()) {
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
        int textRadius = radius - dpToPx(getContext(), 9); // Rádius pro umístění číslic ciferníku
        pNumbers.setTextSize(dpToPx(getContext(), 10));// Nastavení velikosti textu číslic ciferníku
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
    private void drawTick(Canvas canvas) {
        // ručička
        int hours = showTime / 60;// Aktuální hodina dne
        double minutes = showTime % 60;// Aktuální minuta

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

        canvas.drawArc(padding, padding, (float) size / 2 - padding, (float) size / 2 - padding, 0, 360, false, pClock);
    }


    /**
     * Vykreslí legendu
     *
     * @param canvas plátno
     */
    private void drawLegend(Canvas canvas) {
        // Legenda
        int legendSize = radius / 6;
        int legendPadding = dpToPx(getContext(), 8);
        int legendTextPadding = dpToPx(getContext(), 6);

        int legendX = (radius + padding) * 2;
        int legendY = (int) (1.5 * padding);

        float sizeText = pLegend.getTextSize();
        int maxSizeLegend = getWidth() - getPaddingEnd() - padding - legendX - legendSize - legendTextPadding;
        while (pLegend.measureText("Aktuální čas") > maxSizeLegend) {
            sizeText--;
            pLegend.setTextSize(sizeText);
        }


        // Vykreslení legendy
        RectF oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
        canvas.drawArc(oval, 0, 360, true, pClockLegend);
        canvas.drawText("Čas VT", legendX + legendSize + legendTextPadding, legendY + legendSize, pLegend);


        legendY += legendSize + legendPadding;

        oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
        canvas.drawArc(oval, 0, 360, true, pTickLegend);
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
     * Napíše čas zbývající do konce intervalu
     *
     * @param canvas plátno
     */
    private void drawTimeLeft(Canvas canvas) {
        //vykreslení výseče času
        int x = 0;
        int textPadding = dpToPx(getContext(), 10);
        x += textPadding;
        int line = dpToPx(getContext(), 17);
        int y = size / 2 + line;


        if (modelsForAllWeek == null || modelsForAllWeek.isEmpty()) {
            canvas.drawText("Nenalezen žádný čas HDO", x, y, pTimeLeft);
            return;
        }

        String text;
        if (showTUV)
            text = buildTextHdoState(timeHdoTUV, "TUV");
        else
            text = buildTextHdoState(timeHdoNT, "NT");
        autoSizeTextAndDraw(text, getWidth() - 2 * textPadding, canvas, x, y, pTextTime);

        if (showTAR)
            autoSizeTextAndDraw(buildTextHdoState(timeHdoTAR, "TAR"), getWidth() - 2 * textPadding, canvas, x, y + line, pTextTime);

        if (showPV)
            autoSizeTextAndDraw(buildTextHdoState(timeHdoPV, "PV  "), getWidth() - 2 * textPadding, canvas, x, y + line * 2, pTextTime);
    }


    /**
     * Provede výpočet doby časů začátku a konce časů k současnému času
     */
    private void buildTimeHDO() {
        //detekce dnešního dne
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + timeShift);
        timeHdoNT = new TimeHdo();
        timeHdoTUV = new TimeHdo();
        timeHdoTAR = new TimeHdo();
        timeHdoPV = new TimeHdo();
        for (int i = 0; i < modelsForAllWeek.size(); i++) {
            if (i == modelsForAllWeek.size() - 1)
                break;
            HdoModel hdoModelCurrent = modelsForAllWeek.get(i);

            if (!(hdoModelCurrent.getRele().contains("TUV") || !(hdoModelCurrent.getRele().contains("TAR") || !(hdoModelCurrent.getRele().contains("PV")))))
                compareTime(hdoModelCurrent, calendar, timeHdoNT);

            if (hdoModelCurrent.getRele().contains("TUV"))
                compareTime(hdoModelCurrent, calendar, timeHdoTUV);

            if ((hdoModelCurrent.getRele().contains("TAR")))
                compareTime(hdoModelCurrent, calendar, timeHdoTAR);

            if ((hdoModelCurrent.getRele().contains("PV")))
                compareTime(hdoModelCurrent, calendar, timeHdoPV);
        }
    }


    /**
     * Porovnává začátek a konec časů HDO s aktuálním časem. Pokud je větší než 0 a menší než předchozí, tak se uloží.
     *
     * @param hdoModel model HDO
     * @param calendar aktuální čas
     * @param timeHdo  kontejner pro čas začátku a konce HDO
     */
    private void compareTime(HdoModel hdoModel, Calendar calendar, TimeHdo timeHdo) {
        long startCurrent = hdoModel.getCalendarStart().getTimeInMillis() / 1000 / 60; //začátek NT v minutách
        long endCurrent = hdoModel.getCalendarEnd().getTimeInMillis() / 1000 / 60; //konec NT v minutách
        long timeCurrent = calendar.getTimeInMillis() / 1000 / 60; //aktuální čas v minutách

        //nastavení minut do konce platnosti HDO, pokud je větší než 0 a menší než předchozí, tak se uloží
        int minutesEnd = (int) (endCurrent - timeCurrent);
        if (minutesEnd > 0) {
            timeHdo.end = Math.min(timeHdo.end, minutesEnd);
        }

        //nastavení minut do začátku platnosti HDO, pokud je větší než 0 a menší než předchozí, tak se uloží
        int minutesStart = (int) (startCurrent - timeCurrent);
        if (minutesStart > 0) {
            timeHdo.start = Math.min(timeHdo.start, minutesStart);
        }
    }


    /**
     * Sestaví texty odpočtů konce/začátku HDO
     *
     * @param timeHdo kontejner pro čas začátku a konce HDO
     * @param label   popisek typu HDO
     */
    private String buildTextHdoState(TimeHdo timeHdo, String label) {
        String text;
        if (timeHdo.start < timeHdo.end) {
            text = label + " začíná za: " + builderStringTimeLeft(timeHdo.start);
        } else {
            text = label + " končí za: " + builderStringTimeLeft(timeHdo.end);
        }
        return text;
    }


    /**
     * Automaticky upraví velikost textu, aby se vešel do dané šířky a vykreslí jej
     *
     * @param text     text
     * @param maxWidth maximální šířka
     * @param canvas   plátno
     * @param x        x-ová souřadnice
     * @param y        y-ová souřadnice
     * @param paint    barva a styl textu
     */
    private void autoSizeTextAndDraw(String text, float maxWidth, Canvas canvas, float x, float y, Paint paint) {
        float textSize = paint.getTextSize();
        float textWidth = paint.measureText(text);

        // Pokud je text širší než dostupný prostor, snižuje velikost textu
        while (textWidth > maxWidth && textSize > 0) {
            textSize--;
            paint.setTextSize(textSize);
            textWidth = paint.measureText(text);
        }

        // Kreslí text s upravenou velikostí
        canvas.drawText(text, x, y, paint);

        // Vrátí velikost textu na původní hodnotu, pokud je potřeba
        paint.setTextSize(textSize);
    }


    /**
     * Vytvoří řetězec pro zobrazení zbývajícího času
     *
     * @param minutes počet minut
     * @return řetězec pro zobrazení zbývajícího času
     */
    private String builderStringTimeLeft(int minutes) {
        int hours = minutes / 60;
        minutes = minutes % 60;
        if (hours == 0)
            return String.format(Locale.GERMANY, "%d min.", minutes);
        else
            return String.format(Locale.GERMANY, "%d:%02d hod.", hours, minutes);
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
        this.modelsFromDatabase = models;
        this.timeShift = timeShift;
        getCurrentTime();
        animateTick();
        animatedOutText();
        lastTime = currentTime;
    }


    /**
     * Nastaví aktuální čas hodinové ručičky
     */
    private void getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + timeShift);
        currentTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }


    /**
     * Vytvoří seznam modelů pro celý týden, počínaje včerejším dnem
     */
    private void builderHDOLists() {
        if (modelsFromDatabase == null) return;

        modelsForAllWeek = new ArrayList<>();
        modelsForAllWeek.addAll(BuilderHDOStack.build(modelsFromDatabase, timeShift));
        buildTimeHDO();
    }


    /**
     * Animace hodinové ručičky
     */
    private void animateTick() {
        int startTime = lastTime;
        int endTime = currentTime;

        ValueAnimator va = ValueAnimator.ofInt(startTime, endTime);
        va.setDuration(1000);
        va.addUpdateListener(animation -> {
            showTime = (int) animation.getAnimatedValue();
            invalidate();
        });
        va.start();
    }


    /**
     * Animace zmizení textu
     */
    private void animatedOutText() {
        //nastavuji původní text
        ValueAnimator fadeOutAnimator = ValueAnimator.ofInt(255, 0);
        fadeOutAnimator.setDuration(300);
        fadeOutAnimator.addUpdateListener(animation -> {
            pTextTime.setAlpha((Integer) animation.getAnimatedValue());
            pTimeTAR.setAlpha((Integer) animation.getAnimatedValue());
            pTimeTUV.setAlpha((Integer) animation.getAnimatedValue());
            pTimePV.setAlpha((Integer) animation.getAnimatedValue());
            pLegend.setAlpha((Integer) animation.getAnimatedValue());
            pTickLegend.setAlpha((Integer) animation.getAnimatedValue());
            pClockLegend.setAlpha((Integer) animation.getAnimatedValue());

            invalidate();
        });
        fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //spouští další animaci
                animateTextAppearance();
            }
        });
        fadeOutAnimator.start();
    }


    /**
     * Animace zobrazení textu
     */
    private void animateTextAppearance() {
        //nastavuji nový text
        builderHDOLists();
        ValueAnimator fadeInAnimator = ValueAnimator.ofInt(0, 255);
        fadeInAnimator.setDuration(300);
        fadeInAnimator.addUpdateListener(animation -> {
            pTextTime.setAlpha((Integer) animation.getAnimatedValue());
            pTimeTAR.setAlpha((Integer) animation.getAnimatedValue());
            pTimeTUV.setAlpha((Integer) animation.getAnimatedValue());
            pTimePV.setAlpha((Integer) animation.getAnimatedValue());
            pLegend.setAlpha((Integer) animation.getAnimatedValue());
            pTickLegend.setAlpha((Integer) animation.getAnimatedValue());
            pClockLegend.setAlpha((Integer) animation.getAnimatedValue());
            invalidate();
        });
        fadeInAnimator.start();
    }


    /**
     * Kontejner pro čas začátku a konce HDO
     */
    static class TimeHdo {
        int start, end;


        TimeHdo() {
            start = Integer.MAX_VALUE;
            end = Integer.MAX_VALUE;
        }
    }
}
