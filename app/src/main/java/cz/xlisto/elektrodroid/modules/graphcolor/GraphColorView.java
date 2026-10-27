package cz.xlisto.elektrodroid.modules.graphcolor;


import static cz.xlisto.elektrodroid.utils.ColorHelper.colorToHtml;
import static cz.xlisto.elektrodroid.utils.ColorHelper.htmlToColor;

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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.R;


/**
 * View pro výběr barev VT a NT.
 * Vykresluje gradienty pro výběr odstínu, saturace a jasu pro obě sazby s Material 3 stylizací.
 * Xlisto 17.10.2023 21:27
 */
public class GraphColorView extends View {

    public interface OnColorChangeListener {

        void onColorChanged(int colorVT, int colorNT);

    }

    private static final boolean DEBUG = false;

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
    private final Paint pNT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNT_B = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPointerOut = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPointerIn = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPointerShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint debugPaint = new Paint();
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private final float[] mHSV_VT = new float[]{1f, 1f, 1f};
    private final float[] mHSV_NT = new float[]{1f, 1f, 1f};
    private final int pointerRadius = (int) (12 * metrics.density);
    private final float cornerRadius = 12 * metrics.density;
    private int orientation = 0;
    Shader mShaderVT, mShaderNT, mShaderVT_B, mShaderNT_B;
    private OnColorChangeListener onColorChangeListener;


    public GraphColorView(Context context) {
        super(context);
        init();
    }


    public GraphColorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.onColorChangeListener = listener;
    }


    private void init() {
        pVT.setColor(Color.RED);
        pNT.setColor(Color.GREEN);

        // Získání barev z Material 3 tématu
        TypedValue typedValue = new TypedValue();
        int outlineColor = Color.GRAY;
        int surfaceColor = Color.TRANSPARENT;

        if (getContext().getTheme().resolveAttribute(R.attr.colorOutline, typedValue, true)) {
            outlineColor = typedValue.data;
        }
        if (getContext().getTheme().resolveAttribute(R.attr.colorSurface, typedValue, true)) {
            surfaceColor = typedValue.data;
        }

        pBorder.setColor(outlineColor);
        pBorder.setStyle(Paint.Style.STROKE);
        pBorder.setStrokeWidth(1.5f * metrics.density);

        pBackground.setColor(surfaceColor);

        pPointerOut.setStyle(Paint.Style.STROKE);
        pPointerOut.setStrokeWidth(3 * metrics.density);
        pPointerOut.setColor(Color.WHITE);

        pPointerIn.setStyle(Paint.Style.STROKE);
        pPointerIn.setStrokeWidth(1.5f * metrics.density);
        pPointerIn.setColor(Color.BLACK);

        pPointerShadow.setStyle(Paint.Style.FILL);
        pPointerShadow.setColor(Color.argb(80, 0, 0, 0));

        setLayerType(View.LAYER_TYPE_SOFTWARE, isInEditMode() ? null : pVT);
        selectedWindow = 0;
        orientation = getResources().getConfiguration().orientation;
        updateSelectedGradients();
    }


    /**
     * Reaguje na změnu velikosti view, přepočítá obdélníky a znovu připraví shadery.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        orientation = getResources().getConfiguration().orientation;
        updateLayoutRects();
        rebuildShaders();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int minContentHeight = (int) (280 * metrics.density);
        int minHeight = minContentHeight + getPaddingTop() + getPaddingBottom();
        int height = resolveSizeAndState(minHeight, heightMeasureSpec, 0);

        int minContentWidth = (int) (280 * metrics.density);
        int minWidth = minContentWidth - getPaddingLeft() - getPaddingRight();
        int width = resolveSizeAndState(minWidth, widthMeasureSpec, 0);

        this.width = width;
        this.height = height;

        setMeasuredDimension(width, height);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        pVT.setShader(mShaderVT);
        pNT.setShader(mShaderNT);
        paintVT_B.setShader(mShaderVT_B);
        paintNT_B.setShader(mShaderNT_B);

        // Vykreslení pozadí a gradientů se zaoblenými rohy pro M3 vzhled
        canvas.drawRoundRect(rectVT, cornerRadius, cornerRadius, pBackground);
        canvas.drawRoundRect(rectNT, cornerRadius, cornerRadius, pBackground);

        canvas.drawRoundRect(rectVT, cornerRadius, cornerRadius, pVT);
        canvas.drawRoundRect(rectNT, cornerRadius, cornerRadius, pNT);
        canvas.drawRoundRect(rectVTSatur, cornerRadius, cornerRadius, paintVT_B);
        canvas.drawRoundRect(rectNTSatur, cornerRadius, cornerRadius, paintNT_B);

        // Obrysy karet s M3 outline barvou
        canvas.drawRoundRect(rectVTBorder, cornerRadius, cornerRadius, pBorder);
        canvas.drawRoundRect(rectNTBorder, cornerRadius, cornerRadius, pBorder);
        canvas.drawRoundRect(rectVTSaturBorder, cornerRadius, cornerRadius, pBorder);
        canvas.drawRoundRect(rectNTSaturBorder, cornerRadius, cornerRadius, pBorder);

        if (DEBUG) {
            debugPaint.setColor(Color.BLACK);
            debugPaint.setTextSize(16f * metrics.density);
            canvas.drawText("Souřadnice tapnutí: X " + lastX + "  Y " + lastY, getPaddingLeft(), rectNT.top + (20 * metrics.density), debugPaint);
        }
        onDrawPointer(canvas);
    }


    /**
     * Vypočítá pozice všech obdélníků pro výběr barev podle orientace a rozměrů view.
     */
    private void updateLayoutRects() {
        int padding = (int) (4 * metrics.density);
        int sectionHeight = (height - getPaddingTop() - getPaddingBottom()) / 2;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            float vtTop = getPaddingTop() + padding;
            float vtHueHeight = sectionHeight * 0.65f;
            float vtSaturTop = vtTop + vtHueHeight + padding * 2;
            float vtSaturHeight = sectionHeight * 0.25f;

            rectVTBorder.set(getPaddingLeft(), vtTop, width - getPaddingRight(), vtTop + vtHueHeight);
            rectVT.set(rectVTBorder);

            rectVTSaturBorder.set(getPaddingLeft(), vtSaturTop, width - getPaddingRight(), vtSaturTop + vtSaturHeight);
            rectVTSatur.set(rectVTSaturBorder);

            float ntTop = getPaddingTop() + sectionHeight + padding;
            float ntHueHeight = sectionHeight * 0.65f;
            float ntSaturTop = ntTop + ntHueHeight + padding * 2;
            float ntSaturHeight = sectionHeight * 0.25f;

            rectNTBorder.set(getPaddingLeft(), ntTop, width - getPaddingRight(), ntTop + ntHueHeight);
            rectNT.set(rectNTBorder);

            rectNTSaturBorder.set(getPaddingLeft(), ntSaturTop, width - getPaddingRight(), ntSaturTop + ntSaturHeight);
            rectNTSatur.set(rectNTSaturBorder);

        } else {
            float halfWidth = (width - getPaddingLeft() - getPaddingRight()) / 2f;

            float vtTop = getPaddingTop() + padding;
            float vtHueHeight = (height - getPaddingTop() - getPaddingBottom()) * 0.65f;
            float vtSaturTop = vtTop + vtHueHeight + padding * 2;
            float vtSaturHeight = (height - getPaddingTop() - getPaddingBottom()) * 0.25f;

            rectVTBorder.set(getPaddingLeft(), vtTop, getPaddingLeft() + halfWidth - padding, vtTop + vtHueHeight);
            rectVT.set(rectVTBorder);

            rectVTSaturBorder.set(getPaddingLeft(), vtSaturTop, getPaddingLeft() + halfWidth - padding, vtSaturTop + vtSaturHeight);
            rectVTSatur.set(rectVTSaturBorder);

            rectNTBorder.set(getPaddingLeft() + halfWidth + padding, vtTop, width - getPaddingRight(), vtTop + vtHueHeight);
            rectNT.set(rectNTBorder);

            rectNTSaturBorder.set(getPaddingLeft() + halfWidth + padding, vtSaturTop, width - getPaddingRight(), vtSaturTop + vtSaturHeight);
            rectNTSatur.set(rectNTSaturBorder);
        }
    }


    /**
     * Aktualizuje barvy použitých gradientů podle aktuálních HSV hodnot VT a NT.
     */
    private void updateSelectedGradients() {
        mSelectedColorGradientVT[0] = getColorForGradient(mHSV_VT);
        mSelectedColorGradientNT[0] = getColorForGradient(mHSV_NT);
    }


    /**
     * Znovu vytvoří shadery pro horní i spodní výběrové plochy.
     */
    private void rebuildShaders() {
        LinearGradient gradientShaderVT = new LinearGradient(rectVT.left, rectVT.top, rectVT.right, rectVT.top, GRAD_COLORS, null, Shader.TileMode.CLAMP);
        LinearGradient alphaShaderVT = new LinearGradient(0, rectVT.top + (rectVT.height() / 3), 0, rectVT.bottom, GRAD_ALPHA, null, Shader.TileMode.CLAMP);
        mShaderVT = new ComposeShader(alphaShaderVT, gradientShaderVT, PorterDuff.Mode.MULTIPLY);

        LinearGradient gradientShaderNT = new LinearGradient(rectNT.left, rectNT.top, rectNT.right, rectNT.top, GRAD_COLORS, null, Shader.TileMode.CLAMP);
        LinearGradient alphaShaderNT = new LinearGradient(0, rectNT.top + (rectNT.height() / 3), 0, rectNT.bottom, GRAD_ALPHA, null, Shader.TileMode.CLAMP);
        mShaderNT = new ComposeShader(alphaShaderNT, gradientShaderNT, PorterDuff.Mode.MULTIPLY);

        mShaderVT_B = new LinearGradient(rectVTSatur.left, rectVTSatur.top, rectVTSatur.right, rectVTSatur.top, mSelectedColorGradientVT, null, Shader.TileMode.CLAMP);
        mShaderNT_B = new LinearGradient(rectNTSatur.left, rectNTSatur.top, rectNTSatur.right, rectNTSatur.top, mSelectedColorGradientNT, null, Shader.TileMode.CLAMP);
    }


    private void onDrawPointer(Canvas canvas) {
        // Souřadnice ukazatelů pro VT a NT
        float vtX = hueToPoint(mHSV_VT[0], 1);
        float vtY = saturationToPoint(mHSV_VT[1], 1);

        float ntX = hueToPoint(mHSV_NT[0], 3);
        float ntY = saturationToPoint(mHSV_NT[1], 3);

        float vtSaturX = brightnessToPoint(mHSV_VT[2], 2);
        float vtSaturY = rectVTSatur.centerY();

        float ntSaturX = brightnessToPoint(mHSV_NT[2], 4);
        float ntSaturY = rectNTSatur.centerY();

        drawPointerCircle(canvas, vtX, vtY);
        drawPointerCircle(canvas, ntX, ntY);
        drawPointerCircle(canvas, vtSaturX, vtSaturY);
        drawPointerCircle(canvas, ntSaturX, ntSaturY);
    }


    private void drawPointerCircle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y + 2 * metrics.density, pointerRadius, pPointerShadow);
        canvas.drawCircle(x, y, pointerRadius, pPointerOut);
        canvas.drawCircle(x, y, pointerRadius - 2 * metrics.density, pPointerIn);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        lastX = (int) event.getX();
        lastY = (int) event.getY();

        if ((lastY >= rectVTBorder.top) && (lastY <= rectVTBorder.bottom) && (lastX >= rectVTBorder.left) && (lastX <= rectVTBorder.right)) {
            selectedWindow = 1;
        } else if ((lastY >= rectNTBorder.top) && (lastY <= rectNTBorder.bottom) && (lastX >= rectNTBorder.left) && (lastX <= rectNTBorder.right)) {
            selectedWindow = 3;
        } else if ((lastY >= rectVTSaturBorder.top) && (lastY <= rectVTSaturBorder.bottom) && (lastX >= rectVTSaturBorder.left) && (lastX <= rectVTSaturBorder.right)) {
            selectedWindow = 2;
        } else if ((lastY >= rectNTSaturBorder.top) && (lastY <= rectNTSaturBorder.bottom) && (lastX >= rectNTSaturBorder.left) && (lastX <= rectNTSaturBorder.right)) {
            selectedWindow = 4;
        }

        selectColor(lastX, lastY);
        updateSelectedGradients();
        rebuildShaders();

        notifyColorChanged();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                break;
        }
        invalidate();
        return true;
    }


    private void notifyColorChanged() {
        if (onColorChangeListener != null) {
            onColorChangeListener.onColorChanged(getColorVT(), getColorNT());
        }
    }


    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }


    private void selectColor(int x, int y) {
        if (selectedWindow == 1) {
            y = (int) Math.max(rectVT.top, Math.min(y, rectVT.bottom));
            x = (int) Math.max(rectVT.left, Math.min(x, rectVT.right));
        }
        if (selectedWindow == 3) {
            y = (int) Math.max(rectNT.top, Math.min(y, rectNT.bottom));
            x = (int) Math.max(rectNT.left, Math.min(x, rectNT.right));
        }
        if (selectedWindow == 2) {
            y = (int) Math.max(rectVTSatur.top, Math.min(y, rectVTSatur.bottom));
            x = (int) Math.max(rectVTSatur.left, Math.min(x, rectVTSatur.right));
        }
        if (selectedWindow == 4) {
            y = (int) Math.max(rectNTSatur.top, Math.min(y, rectNTSatur.bottom));
            x = (int) Math.max(rectNTSatur.left, Math.min(x, rectNTSatur.right));
        }

        if (selectedWindow == 1) {
            float hue = pointToHue(x, 1);
            float sat = pointToSaturation(y);
            mHSV_VT[0] = hue;
            mHSV_VT[1] = sat;
        }
        if (selectedWindow == 3) {
            float hue = pointToHue(x, 3);
            float sat = pointToSaturation(y);
            mHSV_NT[0] = hue;
            mHSV_NT[1] = sat;
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


    private float pointToHue(float x, int selectedWindow) {
        if (selectedWindow == 1) {
            x = x - rectVT.left;
            return x * 360f / Math.max(1, rectVT.width());
        }
        if (selectedWindow == 3) {
            x = x - rectNT.left;
            return x * 360f / Math.max(1, rectNT.width());
        }
        return 0;
    }


    private int hueToPoint(float hue, int selectedWindow) {
        if (selectedWindow == 1)
            return (int) (rectVT.left + ((hue * rectVT.width()) / 360));
        if (selectedWindow == 3)
            return (int) (rectNT.left + ((hue * rectNT.width()) / 360));
        return 0;
    }


    protected void setColors(int[] colors) {
        setColor(colors[0], colors[1]);
    }


    private void setColor(int colorVT, int colorNT) {
        Color.colorToHSV(colorVT, mHSV_VT);
        Color.colorToHSV(colorNT, mHSV_NT);
        mSelectedColorGradientVT[0] = colorVT;
        mSelectedColorGradientNT[0] = colorNT;
        rebuildShaders();
        invalidate();
        notifyColorChanged();
    }


    int getColorVT() {
        return Color.HSVToColor(mHSV_VT);
    }


    int getColorNT() {
        return Color.HSVToColor(mHSV_NT);
    }


    public String getColorVTHTML() {
        return colorToHtml(Color.HSVToColor(mHSV_VT));
    }


    public void setColorsHTML(String colorVT, String colorNT) {
        setColor(htmlToColor(colorVT), htmlToColor(colorNT));
    }


    public String getColorNTHTML() {
        return colorToHtml(Color.HSVToColor(mHSV_NT));
    }


    int[] getColors() {
        return new int[]{getColorVT(), getColorNT()};
    }


    private float pointToValueBrightness(float x, int vybraneOkno) {
        if (vybraneOkno == 2) {
            x = x - rectVTSaturBorder.left;
            return 1 - (1.f / Math.max(1, rectVTSaturBorder.width()) * x);
        }
        if (vybraneOkno == 4) {
            x = x - rectNTSaturBorder.left;
            return 1 - (1.f / Math.max(1, rectNTSaturBorder.width()) * x);
        }
        return 1.f;
    }


    private int brightnessToPoint(float val, int sazba) {
        val = 1 - val;
        if (sazba == 2 || sazba == 1)
            return (int) (rectVTSatur.left + (rectVTSatur.width() * val));
        if (sazba == 4)
            return (int) (rectNTSatur.left + (rectNTSatur.width() * val));
        return 0;
    }


    private float pointToSaturation(float y) {
        if (selectedWindow == 1) {
            y = y - rectVT.top;
            return 1 - (1.f / Math.max(1, rectVT.height()) * y);
        }
        if (selectedWindow == 3) {
            y = y - rectNT.top;
            return 1 - (1.f / Math.max(1, rectNT.height()) * y);
        }
        return 0.5f;
    }


    private int saturationToPoint(float sat, int sazba) {
        sat = 1 - sat;
        if (sazba == 1)
            return (int) (rectVT.top + (rectVT.height() * sat));
        else if (sazba == 3)
            return (int) (rectNT.top + (rectNT.height() * sat));
        else return 1;
    }


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
        if (!(state instanceof SavedState ss)) {
            super.onRestoreInstanceState(state);
            return;
        }

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


        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }


                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

    }

}
