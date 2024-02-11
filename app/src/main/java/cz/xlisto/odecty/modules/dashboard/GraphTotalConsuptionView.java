package cz.xlisto.odecty.modules.dashboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;

import static cz.xlisto.odecty.utils.ColorUtils.*;
import static cz.xlisto.odecty.utils.DensityUtils.*;
import static java.lang.String.format;


/**
 * Grafické zobrazení plateb období bez faktury
 * Xlisto 01.01.2024 14:09
 */
public class GraphTotalConsuptionView extends View {
    private static final String TAG = "GraphTotalConsuptionView";
    private int size;
    private int paymentAngle = 170;
    private final int colorStart = Color.parseColor("#808080"); // Šedá barva
    private final int colorPaymentEnd = Color.parseColor("#05b2fa"); // Modrá barva
    private double maxConsuption = 35000, consuption = 0, payment = 45000,
            previousConsuption = 0, previousPayment = 1000, currentPayment = 0, currentConsuption = 0,
            previousMax;
    private final Paint pPayment = new Paint();
    private final Paint pPaymentBorder = new Paint();
    private final Paint pPaymentBackground = new Paint();
    private final Paint pLine = new Paint();
    private final Paint pBorder = new Paint();
    private final Paint pTextPayment = new Paint();
    private final Paint pTextConsuption = new Paint();
    private final Paint pTextResult = new Paint();
    private double consuptionAngle, difference;
    private String textPrice = "0";
    private String textPayment = "0";
    private String textConsuption = "0";
    private String result = "0";


    public GraphTotalConsuptionView(Context context) {
        super(context);
    }


    public GraphTotalConsuptionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    public GraphTotalConsuptionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    public GraphTotalConsuptionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }


    private void init(AttributeSet attrs) {
        setSaveEnabled(true);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphTotalConsuptionView);
        int colorText = a.getColor(R.styleable.GraphTotalConsuptionView_colorText, Color.BLACK);
        setMax();
        a.recycle();

        pPayment.setColor(colorPaymentEnd);
        pPayment.setStyle(Paint.Style.STROKE);
        pPayment.setStrokeWidth(dpToPx(getContext(), 14));
        boolean antiAliasing = true;
        pPayment.setAntiAlias(antiAliasing);

        pPaymentBackground.setColor(lighterColor(colorPaymentEnd));
        pPaymentBackground.setStyle(Paint.Style.STROKE);
        pPaymentBackground.setStrokeWidth(dpToPx(getContext(), 14));
        pPaymentBackground.setAntiAlias(antiAliasing);

        pPaymentBorder.setStyle(Paint.Style.STROKE);
        pPaymentBorder.setColor(darkerColor(colorPaymentEnd));
        pPaymentBorder.setStrokeWidth(dpToPx(getContext(), 2));
        pPaymentBorder.setAntiAlias(antiAliasing);

        pLine.setStrokeWidth(dpToPx(getContext(), 2));
        pLine.setColor(Color.RED);
        pLine.setStrokeCap(Paint.Cap.ROUND);
        pLine.setAntiAlias(antiAliasing);

        pBorder.setStrokeWidth(dpToPx(getContext(), 2));
        pBorder.setColor(colorText);
        pBorder.setStrokeCap(Paint.Cap.ROUND);
        pBorder.setAntiAlias(antiAliasing);

        pTextPayment.setColor(colorText);
        pTextPayment.setTextSize(dpToPx(getContext(), 15));
        pTextPayment.setAntiAlias(antiAliasing);

        pTextConsuption.setColor(colorText);
        pTextConsuption.setTextSize(dpToPx(getContext(), 15));
        pTextConsuption.setAntiAlias(antiAliasing);

        pTextResult.setTextSize(dpToPx(getContext(), 15));
        pTextResult.setAntiAlias(antiAliasing);

        startAnimated();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawPayment(canvas);
        drawTexts(canvas);
        drawConsuption(canvas);
    }


    /**
     * Kreslení platby
     *
     * @param canvas plátno
     */
    private void drawPayment(Canvas canvas) {
        RectF rPayment = new RectF(dpToPx(getContext(), 20), dpToPx(getContext(), 20),
                size - dpToPx(getContext(), 20), size - dpToPx(getContext(), 20));
        RectF rPaymentBorderOut = new RectF(dpToPx(getContext(), 13), dpToPx(getContext(), 13),
                size - dpToPx(getContext(), 13), size - dpToPx(getContext(), 13));
        RectF rPaymentBorderIn = new RectF(dpToPx(getContext(), 27), dpToPx(getContext(), 27),
                size - dpToPx(getContext(), 27), size - dpToPx(getContext(), 27));

        int startAngle = 190;
        int sweepAngleMax = 160;


        int y = (int) (rPayment.top + (rPayment.bottom - rPayment.top) / 2);
        LinearGradient lgPayment = new LinearGradient(rPayment.left, y, rPayment.right, y, colorStart, colorPaymentEnd, Shader.TileMode.CLAMP);
        LinearGradient lgPaymentBorder = new LinearGradient(rPayment.left, y, rPayment.right, y, darkerColor(colorStart), darkerColor(colorPaymentEnd), Shader.TileMode.CLAMP);
        pPayment.setShader(lgPayment);
        pPaymentBorder.setShader(lgPaymentBorder);

        canvas.drawArc(rPayment, startAngle, sweepAngleMax, false, pPaymentBackground);
        canvas.drawArc(rPayment, startAngle, paymentAngle, false, pPayment);
        canvas.drawArc(rPaymentBorderOut, startAngle, paymentAngle, false, pPaymentBorder);
        canvas.drawArc(rPaymentBorderIn, startAngle, paymentAngle, false, pPaymentBorder);


        // Výpočet středů pro oblouky
        //PointF centerPayment = new PointF((rPayment.left + rPayment.right) / 2, (rPayment.top + rPayment.bottom) / 2);
        PointF centerPaymentBorderOut = new PointF((rPaymentBorderOut.left + rPaymentBorderOut.right) / 2, (rPaymentBorderOut.top + rPaymentBorderOut.bottom) / 2);
        PointF centerPaymentBorderIn = new PointF((rPaymentBorderIn.left + rPaymentBorderIn.right) / 2, (rPaymentBorderIn.top + rPaymentBorderIn.bottom) / 2);

        // Výpočet poloměrů
        float radiusPaymentBorderOut = ((rPaymentBorderOut.right - rPaymentBorderOut.left) / 2) + dpToPx(getContext(), 1);
        float radiusPaymentBorderIn = ((rPaymentBorderIn.right - rPaymentBorderIn.left) / 2) - dpToPx(getContext(), 1);

        // Výpočet počátečních a koncových bodů pro každý oblouk
        PointF startPointPaymentBorderOut = calculatePoint(centerPaymentBorderOut, radiusPaymentBorderOut, startAngle);
        PointF endPointPaymentBorderOut = calculatePoint(centerPaymentBorderOut, radiusPaymentBorderOut, startAngle + paymentAngle);
        PointF startPointPaymentBorderIn = calculatePoint(centerPaymentBorderIn, radiusPaymentBorderIn, startAngle);
        PointF endPointPaymentBorderIn = calculatePoint(centerPaymentBorderIn, radiusPaymentBorderIn, startAngle + paymentAngle);

        // Kreslení úseček
        canvas.drawLine(startPointPaymentBorderIn.x, startPointPaymentBorderIn.y, startPointPaymentBorderOut.x, startPointPaymentBorderOut.y, pPaymentBorder);
        canvas.drawLine(endPointPaymentBorderIn.x, endPointPaymentBorderIn.y, endPointPaymentBorderOut.x, endPointPaymentBorderOut.y, pPaymentBorder);

    }


    /**
     * Kreslení spotřeby
     *
     * @param canvas plátno
     */
    private void drawConsuption(Canvas canvas) {
        int x = size / 2;
        int y = size / 2;
        int r = size / 2 - dpToPx(getContext(), 20);
        int paddingLine = dpToPx(getContext(), 10);

        // Výpočet koncového bodu úsečky
        double radians = Math.toRadians(consuptionAngle);
        int xEnd = (int) (x + r * Math.cos(radians));
        int yEnd = (int) (y - r * Math.sin(radians));

        // Kreslení úsečky
        canvas.drawLine(x, y - getPaddingBottom() * 2, xEnd, yEnd, pLine);
        //canvas.drawCircle(x, y - getPaddingBottom(), dpToPx(getContext(), 5), pLine);
        float radius = dpToPx(getContext(), 5);
        float centerY = y - getPaddingBottom();
        RectF rect = new RectF((float) x - radius, centerY - radius, (float) x + radius, centerY + radius);

        canvas.drawArc(rect, 180, 180, false, pLine);
        //oddělovací čára
        canvas.drawLine(getPaddingStart() + paddingLine, y - 2, size - getPaddingEnd() - paddingLine, y - 2, pBorder);
    }


    /**
     * Kreslení textů s odečtem časů
     *
     * @param canvas plátno
     */
    private void drawTexts(Canvas canvas) {
        // Kreslení textu
        autoSizeText(textPayment, format(Locale.getDefault(), textPrice, currentPayment),
                canvas, size, 1, pTextPayment);
        autoSizeText(textConsuption, format(Locale.getDefault(), textPrice, currentConsuption),
                canvas, size, 2, pTextConsuption);
        autoSizeText(result, format(Locale.getDefault(), textPrice, difference),
                canvas, size, 3, pTextResult);
    }


    /**
     * Nastaví platby
     *
     * @param payment platby v kč
     */
    public void setPaymentAndConsuption(double payment, double previousPayment, double consuption, double previousConsuption) {
        this.payment = payment;
        this.previousPayment = previousPayment;
        this.consuption = consuption;
        this.previousConsuption = previousConsuption;
        setMax();
        startAnimated();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        size = Math.min(width, height);
        setMeasuredDimension(size, size / 2 + dpToPx(getContext(), 60));
    }


    /**
     * Pomocná metoda pro výpočet bodu pro ohraničení platby
     *
     * @param center střed
     * @param radius poloměr
     * @param angle  úhel
     * @return bod
     */
    private PointF calculatePoint(PointF center, float radius, float angle) {
        float radians = (float) Math.toRadians(angle);
        float x = center.x + radius * (float) Math.cos(radians);
        float y = center.y + radius * (float) Math.sin(radians);
        return new PointF(x, y);
    }


    /**
     * Nastaví maximální hodnotu (buď spotřeby nebo platby)
     */
    private void setMax() {
        maxConsuption = Math.max(payment, consuption);
        previousMax = Math.max(previousPayment, previousConsuption);
    }


    /**
     * Spustí animace platby a spotřeby
     */
    private void startAnimated() {
        animatedPayment();
        animatedConsuption();
        animatedOutText();
    }


    /**
     * Animace spotřeby
     */
    private void animatedConsuption() {
        int startAngle = (int) (170 - (160 / previousMax * previousConsuption));
        int endAngle = (int) (170 - (160 / maxConsuption * consuption));
        //při prvním zobrazení je startovní úhel 170
        if (previousMax == 0)
            startAngle = 170;
        ValueAnimator va = ValueAnimator.ofInt(startAngle, endAngle);
        va.setDuration(1000); // doba trvání animace v milisekundách
        va.addUpdateListener(v ->
        {
            consuptionAngle = (int) v.getAnimatedValue();
            invalidate();
        });
        va.start();
    }


    /**
     * Animace platby
     */
    private void animatedPayment() {
        int startAngle = (int) ((160 / previousMax * previousPayment));
        int endAngle = (int) ((160 / maxConsuption * payment));
        //při prvním zobrazení je startovní úhel 0
        if (previousMax == 0)
            startAngle = 0;
        ValueAnimator va = ValueAnimator.ofInt(startAngle, endAngle);
        va.setDuration(1000); // doba trvání animace v milisekundách
        va.addUpdateListener(v ->

        {
            paymentAngle = (int) v.getAnimatedValue();
            invalidate();
        });
        va.start();
    }


    /**
     * Animace zmizení textu
     */
    private void animatedOutText() {
        //nastavuji původní text
        currentConsuption = previousConsuption;
        currentPayment = previousPayment;
        ValueAnimator fadeOutAnimator = ValueAnimator.ofInt(255, 0);
        fadeOutAnimator.setDuration(300);
        fadeOutAnimator.addUpdateListener(animation -> {
            pTextConsuption.setAlpha((Integer) animation.getAnimatedValue());
            pTextPayment.setAlpha((Integer) animation.getAnimatedValue());
            pTextResult.setAlpha((Integer) animation.getAnimatedValue());
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
        currentConsuption = consuption;
        currentPayment = payment;
        setTexts();

        ValueAnimator fadeInAnimator = ValueAnimator.ofInt(0, 255);
        fadeInAnimator.setDuration(300);
        fadeInAnimator.addUpdateListener(animation -> {
            pTextConsuption.setAlpha((Integer) animation.getAnimatedValue());
            pTextPayment.setAlpha((Integer) animation.getAnimatedValue());
            pTextResult.setAlpha((Integer) animation.getAnimatedValue());
            invalidate();
        });
        fadeInAnimator.start();
    }


    /**
     * Nastaví texty pro Zálohy, Spotřebu a Výsledek
     */
    private void setTexts() {
        textPrice = getContext().getResources().getString(R.string.float_price);
        textPayment = getContext().getResources().getString(R.string.deposits);
        textConsuption = getContext().getResources().getString(R.string.consuption_price);

        difference = (payment - consuption);
        if (payment > consuption) {
            result = getContext().getResources().getString(R.string.overpayment);
            pTextResult.setColor(Color.parseColor("#2b9e42"));
        } else {
            result = getContext().getResources().getString(R.string.underpayment);
            pTextResult.setColor(Color.parseColor("#9d0505"));
        }

    }


    /**
     * Automaticky upraví velikost textu, aby se vešel do dané šířky a vykreslí jej
     *
     * @param text1        text na levý straně
     * @param text2        text na pravé straně
     * @param maxTextWidth maximální šířka textu
     * @param line         řádek
     * @param canvas       plátno
     * @param paint        barva a styl textu
     */
    private void autoSizeText(String text1, String text2, Canvas canvas, float maxTextWidth, int line, Paint paint) {
        int textPadding = dpToPx(getContext(), 10);
        maxTextWidth -= getPaddingStart() + getPaddingEnd() + 2 * textPadding;

        float textSize = paint.getTextSize();
        float originalTextSize = textSize;
        float textWidth1 = paint.measureText(text1);
        float textWidth2 = paint.measureText(text2);
        float maxTextWidthNeeded = textWidth1 + textWidth2;

        // Pokud je text širší než dostupný prostor, snižuje velikost textu
        while (maxTextWidthNeeded > maxTextWidth && textSize > 0) {
            textSize--;
            paint.setTextSize(textSize);
            textWidth1 = paint.measureText(text1);
            textWidth2 = paint.measureText(text2);
            maxTextWidthNeeded = textWidth1 + textWidth2;
        }

        int textY = size / 2 + (dpToPx(getContext(), 17) * line);
        int textX1 = getPaddingStart() + textPadding;
        int textX2 = (int) (size - getPaddingEnd() - textWidth2 - textPadding); // Vypočítá X pozici pro text2 tak, aby byl zarovnán doprava

        // Kreslí text s upravenou velikostí
        canvas.drawText(text1, textX1, textY, paint);
        canvas.drawText(text2, textX2, textY, paint);

        paint.setTextSize(originalTextSize); // Vrátí velikost textu na původní hodnotu, pokud je potřeba
    }
}

