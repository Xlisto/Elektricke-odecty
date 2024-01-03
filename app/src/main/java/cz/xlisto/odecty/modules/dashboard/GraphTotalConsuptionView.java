package cz.xlisto.odecty.modules.dashboard;

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
import cz.xlisto.odecty.utils.DetectScreenMode;

import static cz.xlisto.odecty.utils.ColorUtils.*;
import static cz.xlisto.odecty.utils.DensityUtils.*;
import static java.lang.String.format;

/**
 * Grafické zobrazení plateb období bez faktury
 * Xlisto 01.01.2024 14:09
 */
public class GraphTotalConsuptionView extends View {
    private static final String TAG = "GraphTotalConsuptionView";
    private int size, colorText, paymentAngle = 170;
    private final int colorStart = Color.parseColor("#808080"); // Šedá barva
    private final int colorPaymentEnd = Color.parseColor("#05b2fa"); // Modrá barva
    private double maxConsuption = 35000, consuption = 0, payment = 45000,
            previousConsuption = 0, previousPayment = 1000,
            previousMax;
    private final Paint pPayment = new Paint();
    private final Paint pPaymentBorder = new Paint();
    private final Paint pPaymentBackground = new Paint();
    private final Paint pLine = new Paint();
    private final Paint pTextPayment = new Paint();
    private final Paint pTextConsuption = new Paint();
    private double consuptionAngle;

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
        colorText = a.getColor(R.styleable.GraphTotalConsuptionView_colorText, Color.BLACK);
        setMax();
        a.recycle();
        startAnimated();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        pPayment.setColor(colorPaymentEnd);
        pPayment.setStyle(Paint.Style.STROKE);
        pPayment.setStrokeWidth(dpToPx(getContext(), 14));

        pPaymentBackground.setColor(lighterColor(colorPaymentEnd));
        pPaymentBackground.setStyle(Paint.Style.STROKE);
        pPaymentBackground.setStrokeWidth(dpToPx(getContext(), 14));

        pPaymentBorder.setStyle(Paint.Style.STROKE);
        pPaymentBorder.setColor(darkerColor(colorPaymentEnd));
        pPaymentBorder.setStrokeWidth(dpToPx(getContext(), 2));

        pLine.setStrokeWidth(dpToPx(getContext(), 2));
        pLine.setColor(Color.RED);
        pLine.setStrokeCap(Paint.Cap.ROUND);

        pTextPayment.setColor(colorText);
        pTextPayment.setTextSize(dpToPx(getContext(), 12));

        pTextConsuption.setColor(colorText);
        pTextConsuption.setTextSize(dpToPx(getContext(), 12));


        drawPayment(canvas);
        drawConsuption(canvas);
        drawTexts(canvas);
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

        // Výpočet koncového bodu úsečky
        //double angle = 80; // úhel ve stupních
        double radians = Math.toRadians(consuptionAngle);
        int xEnd = (int) (x + r * Math.cos(radians));
        int yEnd = (int) (y - r * Math.sin(radians));

        // Kreslení úsečky
        canvas.drawLine(x, y, xEnd, yEnd, pLine);
        canvas.drawCircle(x, y, dpToPx(getContext(), 5), pLine);
    }


    /**
     * Kreslení textů
     *
     * @param canvas plátno
     */
    private void drawTexts(Canvas canvas) {
        // Kreslení textu
        String text = "%.2f Kč";
        int widthTextPayment = (int) pTextPayment.measureText(format(Locale.getDefault(), text, payment));
        int textXPayment = dpToPx(getContext(), 5);
        int textYPayment = dpToPx(getContext(), 13);

        int widthTextConsuption = (int) pTextConsuption.measureText(format(Locale.getDefault(), text, consuption));
        int textXConsuption = size - (widthTextConsuption) - dpToPx(getContext(), 5);
        int textYConsuption = dpToPx(getContext(), 13);


        canvas.drawText(format(Locale.getDefault(), text, payment), textXPayment, textYPayment, pTextPayment);
        canvas.drawText(format(Locale.getDefault(), text, consuption), textXConsuption, textYConsuption, pTextConsuption);
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
        if (DetectScreenMode.isLandscape(getContext()))
            setMeasuredDimension(size, size / 2);
        else
            setMeasuredDimension(size / 2, size / 2);
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
}

