package cz.xlisto.odecty.modules.graphcolor;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;

import static cz.xlisto.odecty.utils.ColorHelper.*;

/**
 * View pro výběr barev VT a NT.
 * Xlisto 17.10.2023 21:27
 */
public class GraphColorView extends View {

    private static final boolean DEBUG = false;

    private static final String TAG = "GraphColorView";
    private final int[] GRAD_COLORS = new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
    private final int[] GRAD_ALPHA = new int[]{Color.WHITE, Color.TRANSPARENT};
    private final DisplayMetrics metrics = getResources().getDisplayMetrics();
    private static int selectedWindow = 0; //0 nic; 1 barva VT; 3 barva NT
    private int height, width;
    private final RectF rectVT = new RectF();
    private final RectF rectVTBorder = new RectF();
    private final RectF rectNT = new RectF();
    private final RectF rectNTBorder = new RectF();
    private final RectF rectVTSatur = new RectF();
    private final RectF rectVTSaturBorder = new RectF();
    private final RectF rectNTSatur = new RectF();
    private final RectF rectNTSaturBorder = new RectF();
    private final int[] mSelectedColorGradientVT = new int[]{Color.WHITE, Color.BLACK};
    private final int[] mSelectedColorGradientNT = new int[]{Color.WHITE, Color.BLACK};
    private final Paint pVT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintVT_B = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pNT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNT_B = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPointerOut = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPointerIn = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint debugPaint = new Paint();
    Paint debugPaint2 = new Paint();
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private final float[] mHSV_VT = new float[]{1f, 1f, 1f};
    private final float[] mHSV_NT = new float[]{1f, 1f, 1f};
    private final int pointerRadius = (int) (10 * metrics.density);
    private int orientation = 0;
    Shader mShaderVT, mShaderNT, mShaderVT_B, mShaderNT_B;


    public GraphColorView(Context context) {
        super(context);
        init();
    }


    public GraphColorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        pVT.setColor(Color.RED);
        pNT.setColor(Color.GREEN);
        pText.setColor(getResources().getColor(R.color.color_axis));
        pText.setTextSize(15 * (int) metrics.density);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pBorder.setColor(getResources().getColor(R.color.color_axis));
        pBackground.setColor(getResources().getColor(R.color.color_graph_background));
        setLayerType(View.LAYER_TYPE_SOFTWARE, isInEditMode() ? null : pVT);
        selectedWindow = 0;
        orientation = getResources().getConfiguration().orientation;


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        //minimální výška
        int minContentHeight = 1000;
        int minHeight = minContentHeight + getPaddingTop() + getPaddingBottom();
        int height = resolveSizeAndState(minHeight, heightMeasureSpec, 0);

        //minimální šířka
        int minContentWidth = 800;
        int minWidth = minContentWidth - getPaddingLeft() - getPaddingRight();
        int width = resolveSizeAndState(minWidth, widthMeasureSpec, 0);

        this.width = width;
        this.height = height;

        setMeasuredDimension(width, height);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int border = (int) (1 * metrics.density);
        int quarterVertically = (height - getPaddingTop() - getPaddingBottom()) / 4;//pro portrait
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //VT top je 0, bottom je 1/4*1,5
            rectVTBorder.set(getPaddingLeft(), getPaddingTop() + (30 * metrics.density), width - getPaddingRight(), (int) (quarterVertically * 1.5));//rozměr obdélníka VT - okraj
            rectVT.set(getPaddingLeft() + border, getPaddingTop() + (30 * metrics.density) + border, width - getPaddingRight() - border, (int) (quarterVertically * 1.5 - border));//rozměr obdélníka VT - gradient

            //NT top je 1/2, bottom je 1/2+1/4*1,5
            rectNTBorder.set(getPaddingLeft(), quarterVertically * 2 + (30 * metrics.density), width - getPaddingRight(), (int) (quarterVertically * 2 + quarterVertically * 1.5));//rozměr obdélníka VT - okraj
            rectNT.set(getPaddingLeft() + border, quarterVertically * 2 + getPaddingTop() + (30 * metrics.density) + border, width - getPaddingRight() - border, (int) (quarterVertically * 2 + quarterVertically * 1.5 - border));//rozměr obdélníka VT - gradient

            //VT saturation top je 1/2+1/4*1,5+(15*metrics.density), bottom je 1/2-(20* metrics.density)
            rectVTSaturBorder.set(getPaddingLeft(), (int) (quarterVertically * 1.5) + (15 * metrics.density), width - getPaddingRight(), quarterVertically * 2 - (20 * metrics.density));
            rectVTSatur.set(getPaddingLeft() + border, (int) (quarterVertically * 1.5) + border + (15 * metrics.density), width - getPaddingRight() - border, quarterVertically * 2 - border - (20 * metrics.density));

            //NT saturation top je 3/4+1/4*1,5+(15*metrics.density), bottom je height-(20* metrics.density)
            rectNTSaturBorder.set(getPaddingLeft(), quarterVertically * 2 + (int) (quarterVertically * 1.5) + (15 * metrics.density), width - getPaddingRight(), height - (20 * metrics.density));
            rectNTSatur.set(getPaddingLeft() + border, quarterVertically * 2 + (int) (quarterVertically * 1.5) + border + (15 * metrics.density), width - getPaddingRight() - border, height - border - (20 * metrics.density));
        } else {
            //VT top je 0, bottom je 1/4*1,5, left je 0 + padding, right je 1/2
            rectVTBorder.set(getPaddingLeft(), getPaddingTop() + (30 * metrics.density), (int) (width / 2) - (15 * metrics.density), quarterVertically * 3);//rozměr obdélníka VT - okraj
            rectVT.set(getPaddingLeft() + border, getPaddingTop() + (30 * metrics.density) + border, (int) (width / 2) - (15 * metrics.density) - border, quarterVertically * 3 - border);//rozměr obdélníka VT - gradient

            //NT top je 0, bottom je 1/2+1/4*1,5
            rectNTBorder.set((int) (width / 2) + (15 * metrics.density), getPaddingTop() + (30 * metrics.density), width - getPaddingRight(), quarterVertically * 3);//rozměr obdélníka VT - okraj
            rectNT.set((int) (width / 2) + (15 * metrics.density) + border, getPaddingTop() + (30 * metrics.density) + border, width - getPaddingRight() - border, (int) (quarterVertically * 3 - border));//rozměr obdélníka VT - gradient

            //VT saturation top je 1/2+1/4*1,5+(15*metrics.density), bottom je 1/2-(20* metrics.density)
            rectVTSaturBorder.set(getPaddingLeft(), (quarterVertically * 3) + (30 * metrics.density), (int) (width / 2) - (15 * metrics.density), height - getPaddingBottom());
            rectVTSatur.set(getPaddingLeft() + border, (quarterVertically * 3) + border + (30 * metrics.density), (int) (width / 2) - (15 * metrics.density) - border, height - getPaddingBottom() - border);

            //NT saturation top je 3/4+1/4*1,5+(15*metrics.density), bottom je height-(20* metrics.density)
            rectNTSaturBorder.set((int) (width / 2) + (15 * metrics.density), quarterVertically * 3 + (30 * metrics.density), width - getPaddingRight(), height - getPaddingBottom());
            rectNTSatur.set((int) (width / 2) + (15 * metrics.density) + border, quarterVertically * 3 + border + (30 * metrics.density), width - getPaddingRight() - border, height - getPaddingBottom() - border);

        }

        mSelectedColorGradientVT[0] = getColorForGradient(mHSV_VT);
        mSelectedColorGradientNT[0] = getColorForGradient(mHSV_NT);


        LinearGradient gradientShaderVT = new LinearGradient(rectVT.left, rectVT.top, rectVT.right, rectVT.top /* simple line gradient*/, GRAD_COLORS, null, Shader.TileMode.CLAMP);
        LinearGradient alphaShaderVT = new LinearGradient(0, rectVT.top + (rectVT.height() / 3) /* don't start at 0px*/, 0, rectVT.bottom, GRAD_ALPHA, null, Shader.TileMode.CLAMP);
        mShaderVT = new ComposeShader(alphaShaderVT, gradientShaderVT, PorterDuff.Mode.MULTIPLY);
        LinearGradient gradientShaderNT = new LinearGradient(rectNT.left, rectNT.top, rectNT.right, rectNT.top /* simple line gradient*/, GRAD_COLORS, null, Shader.TileMode.CLAMP);
        LinearGradient alphaShaderNT = new LinearGradient(0, rectNT.top + (rectNT.height() / 3) /* don't start at 0px*/, 0, rectNT.bottom, GRAD_ALPHA, null, Shader.TileMode.CLAMP);
        mShaderNT = new ComposeShader(alphaShaderNT, gradientShaderNT, PorterDuff.Mode.MULTIPLY);
        //spodní okno Brightnes
        /* simple line gradient*/
        mShaderVT_B = new LinearGradient(rectVTSatur.left, rectVTSatur.top, rectVTSatur.right, rectVTSatur.top /* simple line gradient*/, mSelectedColorGradientVT, null, Shader.TileMode.CLAMP);
        /* simple line gradient*/
        mShaderNT_B = new LinearGradient(rectNTSatur.left, rectNTSatur.top, rectNTSatur.right, rectNTSatur.top /* simple line gradient*/, mSelectedColorGradientNT, null, Shader.TileMode.CLAMP);


        pVT.setShader(mShaderVT);
        pNT.setShader(mShaderNT);
        paintVT_B.setShader(mShaderVT_B);
        paintNT_B.setShader(mShaderNT_B);
        canvas.drawRect(rectVTBorder, pBorder);
        canvas.drawRect(rectNTBorder, pBorder);
        canvas.drawRect(rectVTSaturBorder, pBorder);
        canvas.drawRect(rectNTSaturBorder, pBorder);
        canvas.drawRect(rectVT, pBackground);
        canvas.drawRect(rectNT, pBackground);
        canvas.drawRect(rectVT, pVT);
        canvas.drawRect(rectNT, pNT);
        canvas.drawRect(rectVTSatur, paintVT_B);
        canvas.drawRect(rectNTSatur, paintNT_B);


        if (DEBUG) {
            debugPaint.setColor(Color.BLACK);
            debugPaint.setTextSize(16f * metrics.density);
            canvas.drawText("Souřadnice tapnutí: X " + lastX + "  Y " + lastY, getPaddingLeft(), rectNT.top + (20 * metrics.density), debugPaint);
            canvas.drawText("Souřadnice VT: H " + rectVT.top + "  D " + rectVT.bottom, getPaddingLeft(), rectNT.top + (40 * metrics.density), debugPaint);
            canvas.drawText("Souřadnice NT: H " + rectNT.top + "  D " + rectNT.bottom, getPaddingLeft(), rectNT.top + (60 * metrics.density), debugPaint);
            canvas.drawText("Vybrané okno:" + selectedWindow, getPaddingLeft(), rectNT.top + (80 * metrics.density), debugPaint);
            canvas.drawText("Vybraná barva VT:" + Color.HSVToColor(mHSV_VT) + " | " + mHSV_VT[0] + " | " + mHSV_VT[1] + " | " + mHSV_VT[2], getPaddingLeft(), rectNT.top + (100 * metrics.density), debugPaint);
            canvas.drawText("Vybraná barva NT:" + Color.HSVToColor(mHSV_NT) + " | " + mHSV_NT[0] + " | " + mHSV_NT[1] + " | " + mHSV_NT[2], getPaddingLeft(), rectNT.top + (120 * metrics.density), debugPaint);
            debugPaint2.setStrokeWidth(10 * metrics.density);
            debugPaint2.setColor(Color.HSVToColor(mHSV_VT));
            canvas.drawLine(getPaddingLeft(), rectNT.top + (130 * metrics.density), 200 * metrics.density, rectNT.top + (130 * metrics.density),
                    debugPaint2);
            debugPaint2.setColor(Color.HSVToColor(mHSV_NT));
            canvas.drawLine(getPaddingLeft(), rectNT.top + (140 * metrics.density), 200 * metrics.density, rectNT.top + (140 * metrics.density),
                    debugPaint2);
        }
        onDrawPointer(canvas);
        canvas.drawText("Barva sazby VT (" + colorToHtml(getColorVT()) + ")", rectVT.left, rectVT.top - (12 * metrics.density), pText);
        canvas.drawText("Barva sazby NT (" + colorToHtml(getColorNT()) + ")", rectNT.left, rectNT.top - (12 * metrics.density), pText);
    }


    private void onDrawPointer(Canvas canvas) {
        //vykreslení ukazatele
        pPointerOut.setColor(Color.BLACK);
        pPointerIn.setColor(Color.YELLOW);
        pPointerOut.setStyle(Paint.Style.STROKE);//nastavení linky
        pPointerIn.setStyle(Paint.Style.STROKE);//nastavení linky
        pPointerOut.setStrokeWidth(3 * metrics.density);//nastavení tloušťky čáry
        pPointerIn.setStrokeWidth(1 * metrics.density);//nastavení tloušťky čáry
        canvas.drawCircle(hueToPoint(mHSV_VT[0], 1), saturationToPoint(mHSV_VT[1], 1), pointerRadius, pPointerOut);//ukazatel barvy VT
        canvas.drawCircle(hueToPoint(mHSV_VT[0], 1), saturationToPoint(mHSV_VT[1], 1), pointerRadius + 2, pPointerIn);//ukazatel barvy VT
        canvas.drawCircle(hueToPoint(mHSV_NT[0], 3), saturationToPoint(mHSV_NT[1], 3), pointerRadius, pPointerOut);//ukazatel barvy NT
        canvas.drawCircle(hueToPoint(mHSV_NT[0], 3), saturationToPoint(mHSV_NT[1], 3), pointerRadius + 2, pPointerIn);//ukazatel barvy NT
        canvas.drawCircle(brightnessToPoint(mHSV_VT[2], 2), rectVTSatur.bottom - (rectVTSatur.height() / 2), pointerRadius, pPointerOut);//ukazatel brightnes VT
        canvas.drawCircle(brightnessToPoint(mHSV_VT[2], 2), rectVTSatur.bottom - (rectVTSatur.height() / 2), pointerRadius + 2, pPointerIn);//ukazatel brightnes VT
        canvas.drawCircle(brightnessToPoint(mHSV_NT[2], 4), rectNTSatur.bottom - (rectNTSatur.height() / 2), pointerRadius, pPointerOut);//ukazatel brightnes NT
        canvas.drawCircle(brightnessToPoint(mHSV_NT[2], 4), rectNTSatur.bottom - (rectNTSatur.height() / 2), pointerRadius + 2, pPointerIn);//ukazatel brightnes NT
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //pozice tápnutí
        lastX = (int) event.getX();
        lastY = (int) event.getY();
        if ((lastY > rectVTBorder.top) && (lastY < rectVTBorder.bottom) && (lastX < rectVTBorder.right)) {
            selectedWindow = 1;
        } else if ((lastY > rectNTBorder.top) && (lastY < rectNTBorder.bottom) && (lastX > rectNTBorder.left)) {
            selectedWindow = 3;

        } else if ((lastY > rectVTSaturBorder.top) && (lastY < rectVTSaturBorder.bottom) && (lastX < rectVTBorder.right)) {
            selectedWindow = 2;

        } else if ((lastY > rectNTSaturBorder.top) && (lastY < rectNTSaturBorder.bottom) && (lastX > rectNTBorder.left)) {
            selectedWindow = 4;

        } else selectedWindow = 0;
        selectColor(lastX, lastY);


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        invalidate();
        return true;
    }


    /**
     * Vyberte barvu na základě poskytnutých souřadnic (x, y) ve zobrazení.
     *
     * @param x X-souřadnice dotyku
     * @param y Y-souřadnice dotyku
     */
    private void selectColor(int x, int y) {
        //vrátí souřadnice kliknutí nebo minimální či maximální hodnotu

        //jsou obsaženy dva obdélníky, tedy budu muset rozlišovat ke kterému se přiřadit
        if (selectedWindow == 1) {
            y = (int) Math.max(rectVT.top, Math.min(y, rectVT.bottom));//porovnává souřadnice kliknutí s kliknutím do obdelníku
            //souřadnice x (vodorovné) porovnává dvě čísla a vrátí menší či větší podle min nebo max; zde se porovnávají souřadnice kliknutí s souřadnicemi obdélníku
            x = (int) Math.max(rectVT.left, Math.min(x, rectVT.right));//porovnávání čísel, funkci ještě odzkoušet na externím projektu
        }
        if (selectedWindow == 3) {
            y = (int) Math.max(rectNT.top, Math.min(y, rectNT.bottom));//porovnává souřadnice kliknutí s kliknutím do obdelníku
            //souřadnice x (vodorovné) porovnává dvě čísla a vrátí menší či větší podle min nebo max; zde se porovnávají souřadnice kliknutí s souřadnicemi obdélníku
            x = (int) Math.max(rectNT.left, Math.min(x, rectNT.right));//porovnávání čísel, funkci ještě odzkoušet na externím projektu
        }
        if (selectedWindow == 2) {
            y = (int) Math.max(rectVTSatur.top, Math.min(y, rectVTSatur.bottom));//porovnává souřadnice kliknutí s kliknutím do obdelníku
            //souřadnice x (vodorovné) porovnává dvě čísla a vrátí menší či větší podle min nebo max; zde se porovnávají souřadnice kliknutí s souřadnicemi obdélníku
            x = (int) Math.max(rectVTSatur.left, Math.min(x, rectVTSatur.right));//porovnávání čísel, funkci ještě odzkoušet na externím projektu
        }
        if (selectedWindow == 4) {
            y = (int) Math.max(rectNTSatur.top, Math.min(y, rectNTSatur.bottom));//porovnává souřadnice kliknutí s kliknutím do obdelníku
            //souřadnice x (vodorovné) porovnává dvě čísla a vrátí menší či větší podle min nebo max; zde se porovnávají souřadnice kliknutí s souřadnicemi obdélníku

            x = (int) Math.max(rectNTSatur.left, Math.min(x, rectNTSatur.right));//porovnávání čísel, funkci ještě odzkoušet na externím projektu

        }

        if (selectedWindow == 1) {
            float hue = pointToHue(x, 1);//vrací číslo 0 - 360
            float sat = pointToSaturation(y);
            mHSV_VT[0] = hue;
            mHSV_VT[1] = sat;
            //mHSV_VT[2] = pointToValueBrightness(x);
        }
        if (selectedWindow == 3) {
            float hue = pointToHue(x, 3);//vrací číslo 0 - 360
            float sat = pointToSaturation(y);
            mHSV_NT[0] = hue;
            mHSV_NT[1] = sat;
            //mHSV_NT[2] = 1.f;
        }
        if (selectedWindow == 2) {
            float b = pointToValueBrightness(x, 2);
            mHSV_VT[2] = b;
        }
        if (selectedWindow == 4) {
            float b = pointToValueBrightness(x, 4);
            mHSV_NT[2] = b;
        }


    }


    /**
     * Převádí X-ovou souřadnici na hodnotu odstínu v zadaném okně.
     *
     * @param x              X-ová souřadnice dotyku.
     * @param selectedWindow Vybrané okno (1 pro VT, 3 pro NT).
     * @return Pohotovostní hodnota v rozsahu [0, 360].
     */
    private float pointToHue(float x, int selectedWindow) {

        if (selectedWindow == 1) {
            x = x - rectVT.left;
            return x * 360f / rectVT.width();
        }
        if (selectedWindow == 3) {
            x = x - rectNT.left;
            return x * 360f / rectNT.width();
        }
        return 0;
    }


    /**
     * Převádí hodnotu odstínu na X-ovou souřadnici v zadaném okně.
     *
     * @param hue            Hodnota odstínu.
     * @param selectedWindow Vybrané okno (1 pro VT, 3 pro NT).
     * @return X-ová souřadnice v zadaném okně.
     */
    private int hueToPoint(float hue, int selectedWindow) {
        if (selectedWindow == 1)
            return (int) (rectVT.left + ((hue * rectVT.width()) / 360));
        if (selectedWindow == 3)
            return (int) (rectNT.left + ((hue * rectNT.width()) / 360));
        return 0;
    }


    /**
     * Nastaví barvu VT a NT v RGB formátu.
     *
     * @param colors Pole obsahující barvy VT a NT v RGB formátu.
     */
    protected void setColors(int[] colors) {
        setColor(colors[0], colors[1]);
    }


    /**
     * Nastaví barvu VT a NT v RGB formátu.
     *
     * @param colorVT Barva VT v RGB formátu.
     * @param colorNT Barva NT v RGB formátu.
     */
    private void setColor(int colorVT, int colorNT) {
        Color.colorToHSV(colorVT, mHSV_VT);
        Color.colorToHSV(colorNT, mHSV_NT);
        mSelectedColorGradientVT[0] = colorVT;//nastavuji brightnes u okna 2
        mSelectedColorGradientNT[0] = colorNT;
        invalidate();
    }


    /**
     * Vrátí barvu VT v RGB formátu.
     *
     * @return Barva VT v RGB formátu.
     */
    int getColorVT() {
        return Color.HSVToColor(mHSV_VT);
    }


    /**
     * Vrátí barvu NT v RGB formátu.
     *
     * @return Barva NT v RGB formátu.
     */
    int getColorNT() {
        return Color.HSVToColor(mHSV_NT);
    }


    /**
     * Vrátí barvu VT v HTML formátu.
     *
     * @return Barva VT v HTML formátu.
     */
    public String getColorVTHTML() {
        return colorToHtml(Color.HSVToColor(mHSV_VT));
    }


    /**
     * Nastaví barvu VT a NT v HTML formátu.
     *
     * @param colorVT Barva VT v HTML formátu.
     * @param colorNT Barva NT v HTML formátu.
     */
    public void setColorsHTML(String colorVT, String colorNT) {
        setColor(htmlToColor(colorVT), htmlToColor(colorNT));
    }


    /**
     * Vrátí barvu NT v HTML formátu.
     *
     * @return Barva NT v HTML formátu.
     */
    public String getColorNTHTML() {
        return colorToHtml(Color.HSVToColor(mHSV_NT));
    }


    /**
     * Vrátí barvy VT a NT v poli.
     *
     * @return Pole obsahující barvy VT a NT v RGB formátu.
     */
    int[] getColors() {
        return new int[]{getColorVT(), getColorNT()};
    }


    /**
     * Vrátí barvu v RGB formátu.
     *
     * @param x           souřadnice X
     * @param vybraneOkno vybrané okno
     * @return Barva v RGB formátu.
     */
    private float pointToValueBrightness(float x, int vybraneOkno) {
        if (vybraneOkno == 2) {
            x = x - rectVTSaturBorder.left;
            return 1 - (1.f / rectVTSaturBorder.width() * x);
        }
        if (vybraneOkno == 4) {
            x = x - rectNTSaturBorder.left;
            return 1 - (1.f / rectNTSaturBorder.width() * x);
        }
        return 1.f;
    }


    /**
     * Vrátí barvu v RGB formátu.
     *
     * @param val   hodnota
     * @param sazba vybrané okno
     * @return Barva v RGB formátu.
     */
    private int brightnessToPoint(float val, int sazba) {
        val = 1 - val;
        if (sazba == 2 || sazba == 1)
            return (int) (rectVTSatur.left + (rectVTSatur.width() * val));
        if (sazba == 4)
            return (int) (rectNTSatur.left + (rectNTSatur.width() * val));
        return 0;
    }


    //transparentnost do svisla

    /**
     * Vrátí barvu v RGB formátu.
     *
     * @param y souřadnice Y
     * @return Barva v RGB formátu.
     */
    private float pointToSaturation(float y) {
        if (selectedWindow == 1) {
            y = y - rectVT.top;
            return 1 - (1.f / rectVT.height() * y);
        }
        if (selectedWindow == 3) {
            y = y - rectNT.top;
            return 1 - (1.f / rectNT.height() * y);
        }
        return 0.5f;
    }


    /**
     * Vrátí Y-souřadnici na základě hodnoty saturace.
     *
     * @param sat   Hodnota saturace.
     * @param sazba Vybrané okno (1 pro VT, 3 pro NT).
     * @return Y-souřadnice v zadaném okně.
     */
    private int saturationToPoint(float sat, int sazba) {
        sat = 1 - sat;
        if (sazba == 1)
            return (int) (rectVT.top + (rectVT.height() * sat));
        else if (sazba == 3)
            return (int) (rectNT.top + (rectNT.height() * sat));
        else return 1;
    }


    /**
     * Převede barvu v RGB formátu na číselný formát.
     * @param hsv barva v RGB formátu
     * @return Barva v číselném formátu.
     */
    private int getColorForGradient(float[] hsv) {
        if (hsv[2] != 1f) {
            float oldV = hsv[2];
            hsv[2] = 1;
            int color = Color.HSVToColor(hsv);
            hsv[2] = oldV;
            return color;
        } else {
            return Color.HSVToColor(hsv);
        }
    }


    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.colorVT = getColorVT();
        state.colorNT = getColorNT();
        state.colorGradientVT = mSelectedColorGradientVT[0];
        state.colorGradientNT = mSelectedColorGradientNT[0];
        return state;
    }


    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setColor(ss.colorVT, ss.colorNT);
        mSelectedColorGradientVT[0] = ss.colorGradientVT;
        mSelectedColorGradientNT[0] = ss.colorGradientNT;
    }


    private static class SavedState extends BaseSavedState {
        int colorVT, colorNT, colorGradientVT, colorGradientNT;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            colorVT = in.readInt();
            colorNT = in.readInt();
            colorGradientVT = in.readInt();
            colorGradientNT = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(colorVT);
            out.writeInt(colorNT);
            out.writeInt(colorGradientVT);
            out.writeInt(colorGradientNT);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
