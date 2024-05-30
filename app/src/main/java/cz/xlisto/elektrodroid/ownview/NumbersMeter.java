package cz.xlisto.elektrodroid.ownview;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.utils.DensityUtils;


/** Zobrazení stavu elektroměru pomocí obrázkových čísel
 * Xlisto 18.01.2024 8:58
 */
public class NumbersMeter extends View {
    private static final String TAG = "NumbersMeter";

    private Bitmap[] numbers;
    int countNumber = 7;
    int currentNumber = 0;
    int lastNumber = 0;
    int tempNumber = 0;
    int width = 0;
    private final Paint pBackgroundNumberWhole = new Paint();
    private final Paint pBackgroundNumberDecimal = new Paint();
    private int space = 0;
    private int padding = 0;
    int[] digits;


    public NumbersMeter(Context context) {
        super(context);
        init();
    }


    public NumbersMeter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public NumbersMeter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public NumbersMeter(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init() {
        Bitmap number1, number2, number3, number4, number5, number6, number7, number8, number9, number0;
        number0 = BitmapFactory.decodeResource(getResources(), R.drawable.number_0);
        number1 = BitmapFactory.decodeResource(getResources(), R.drawable.number_1);
        number2 = BitmapFactory.decodeResource(getResources(), R.drawable.number_2);
        number3 = BitmapFactory.decodeResource(getResources(), R.drawable.number_3);
        number4 = BitmapFactory.decodeResource(getResources(), R.drawable.number_4);
        number5 = BitmapFactory.decodeResource(getResources(), R.drawable.number_5);
        number6 = BitmapFactory.decodeResource(getResources(), R.drawable.number_6);
        number7 = BitmapFactory.decodeResource(getResources(), R.drawable.number_7);
        number8 = BitmapFactory.decodeResource(getResources(), R.drawable.number_8);
        number9 = BitmapFactory.decodeResource(getResources(), R.drawable.number_9);
        numbers = new Bitmap[]{number0, number1, number2, number3, number4, number5, number6, number7, number8, number9};

        digits = new int[countNumber];

        space = DensityUtils.dpToPx(getContext(), 2);
        padding = DensityUtils.dpToPx(getContext(), 5);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int[] scaledSize = getScaledSize();

        drawBackgroundNumbers(canvas, scaledSize[0], scaledSize[1]);
        tempNumber = currentNumber;
        Log.w(TAG, "onDraw: " + currentNumber + " " + countNumber);
        if (countNumber > 7)
            countNumber = 7;

        for (int i = countNumber - 1; i >= 0; i--) {
            digits[i] = tempNumber % 10;
            tempNumber /= 10;
            drawNumber(canvas, digits[i], i, scaledSize[0], scaledSize[1]);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        if (width > 0) {
            int[] scaledSize = getScaledSize();
            int scaledHeight = scaledSize[1];
            int height = padding + scaledHeight + padding;
            setMeasuredDimension(width, height);
        }
    }


    /**
     * Vrátí bitmapu číslice
     *
     * @param digit číslice
     * @return bitmaps
     */
    private Bitmap getDigitBitmap(int digit) {
        switch (digit) {
            case 0:
                return numbers[0];
            case 1:
                return numbers[1];
            case 2:
                return numbers[2];
            case 3:
                return numbers[3];
            case 4:
                return numbers[4];
            case 5:
                return numbers[5];
            case 6:
                return numbers[6];
            case 7:
                return numbers[7];
            case 8:
                return numbers[8];
            case 9:
                return numbers[9];
            default:
                return numbers[0];
        }
    }


    /**
     * Vrátí rozměry obrázků číslic zmenšený podle displeje
     *
     * @return int[] {width, height}
     */
    private int[] getScaledSize() {
        int originalWidth = Objects.requireNonNull(getDigitBitmap(0)).getWidth();
        int originalHeight = Objects.requireNonNull(getDigitBitmap(0)).getHeight();

        int targetWidth = width / (countNumber + 3);

        float scale = (float) targetWidth / originalWidth;
        int scaledWidth = Math.round(originalWidth * scale);
        int scaledHeight = Math.round(originalHeight * scale);

        return new int[]{scaledWidth, scaledHeight};
    }


    /**
     * Vykreslí pozadí pro čísla
     *
     * @param canvas       canvas
     * @param scaledWidth  šířka
     * @param scaledHeight výška
     */
    private void drawBackgroundNumbers(Canvas canvas, int scaledWidth, int scaledHeight) {

        int borderBackground = DensityUtils.dpToPx(getContext(), 3);
        RectF rectFWhole = new RectF(padding - borderBackground, padding - borderBackground, (scaledWidth + space) * countNumber + borderBackground + padding - space, padding + scaledHeight + borderBackground);
        RectF rectFDecimal = new RectF((scaledWidth + space) * (countNumber - 1) + padding - space, padding - borderBackground, (scaledWidth + space) * countNumber + borderBackground + padding - space, padding + scaledHeight + borderBackground);
        pBackgroundNumberWhole.setColor(getResources().getColor(R.color.color_yes));
        pBackgroundNumberDecimal.setColor(getResources().getColor(R.color.color_red_alert));
        pBackgroundNumberWhole.setStyle(Paint.Style.FILL);
        pBackgroundNumberDecimal.setStyle(Paint.Style.FILL);
        boolean antiAliasing = true;
        pBackgroundNumberWhole.setAntiAlias(antiAliasing);
        pBackgroundNumberDecimal.setAntiAlias(antiAliasing);
        canvas.drawRoundRect(rectFWhole, 10, 10, pBackgroundNumberWhole);
        canvas.drawRoundRect(rectFDecimal, 10, 10, pBackgroundNumberDecimal);
    }


    /**
     * Vykreslí číslici na danou pozici
     *
     * @param canvas       canvas
     * @param number       číslice
     * @param position     pozice
     * @param scaledWidth  šířka
     * @param scaledHeight výška
     */
    private void drawNumber(Canvas canvas, int number, int position, int scaledWidth, int scaledHeight) {

        if (width == 0) return;

        assert getDigitBitmap(number) != null;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(Objects.requireNonNull(getDigitBitmap(number)), scaledWidth, scaledHeight, true);

        int x = position * (scaledWidth + space) + padding;

        canvas.drawBitmap(scaledBitmap, x, padding, null);
    }


    /**
     * Animace čísla
     *
     * @param start začátek
     * @param end   konec
     */
    public void animace(int start, int end) {
        int duration = 1000;

        ValueAnimator va = ValueAnimator.ofInt(start, end);
        va.setDuration(duration);
        va.setInterpolator(new LinearInterpolator());

        va.addUpdateListener(animation -> {
            currentNumber = (int) animation.getAnimatedValue();
            invalidate();
        });
        va.start();

    }


    /**
     * Nastaví hodnotu měřiče
     *
     * @param currentNumber hodnota měřiče
     */
    public void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
        countNumber = Math.max(((int) Math.floor(Math.log10(currentNumber)) + 2), 7);
        animace(lastNumber, currentNumber);
        lastNumber = currentNumber;
    }


    /**
     * Vrátí aktuální hodnotu měřiče
     *
     * @return aktuální hodnota
     */
    @Override
    public int getBaseline() {
        int[] scaledSize = getScaledSize();
        return scaledSize[1] + padding;
    }
}
