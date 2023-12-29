package cz.xlisto.odecty.modules.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;


/**
 * Xlisto 27.12.2023 9:47
 */
public class GraphDashBoardView extends View {
    private static final String TAG = "GraphDashBoardView";
    private int width;
    float density;
    private double consuption, consuptionMax;
    private String textConsuption;
    private int colorGraph = 0xFF003300;
    private Paint pText, pGraph, pGraphBorder, pGraphBackground;


    public GraphDashBoardView(Context context) {
        super(context);
        init();
    }


    public GraphDashBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    public GraphDashBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    public GraphDashBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }


    private void init() {
        setSaveEnabled(true);
        invalidate();
    }


    private void init(AttributeSet attrs) {
        setSaveEnabled(true);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphDashBoardView);
        consuption = a.getFloat(R.styleable.GraphDashBoardView_consuption, 30);
        consuptionMax = a.getFloat(R.styleable.GraphDashBoardView_consuptionMax, 100);
        colorGraph = a.getColor(R.styleable.GraphDashBoardView_colorGraph, -16701293);
        a.recycle();
        pText = new Paint();
        pGraph = new Paint();
        pGraphBorder = new Paint();
        pGraphBackground = new Paint();
        invalidate();
    }


    /**
     * Voláno, když má zobrazení vykreslit svůj obsah.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        density = getResources().getDisplayMetrics().density;

        pGraphBorder.setStyle(Paint.Style.STROKE);
        pGraphBorder.setStrokeWidth(1 * density);

        pText.setColor(colorGraph);
        pText.setTextSize(11 * density);

        pGraph.setColor(colorGraph);
        pGraphBorder.setColor(darkerColor(colorGraph));
        pGraphBackground.setColor(lighterColor(colorGraph));

        float widthGraph = (int) (consuption * (width / (float) consuptionMax));
        float round = 3 * density;

        canvas.drawRoundRect(4 * density, 13 * density, width, 30 * density, round, round, pGraphBackground);
        canvas.drawRoundRect(4 * density, 13 * density, widthGraph, 30 * density, round, round, pGraph);
        canvas.drawRoundRect(4 * density, 13 * density, widthGraph, 30 * density, round, round, pGraphBorder);
        canvas.drawText(textConsuption, 4 * density, 10 * density, pText);
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
        width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (34 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(width, height);
    }


    /**
     * Nastaví spotřebu
     *
     * @param consuption spotřeba
     */
    public void setConsuption(double consuption) {
        this.consuption = consuption;
        invalidate();
    }


    /**
     * Nastaví popisek textu spotřeby nad grafem
     * @param textConsuption text popisku
     */
    public void setConsuption(String textConsuption){
        this.textConsuption = textConsuption;
        invalidate();
    }


    /**
     * Nastaví maximální spotřebu
     *
     * @param consuptionMax maximální spotřeba
     */
    public void setConsuptionMax(double consuptionMax) {
        this.consuptionMax = consuptionMax;
        invalidate();
    }


    /**
     * Nastaví barvu grafu
     *
     * @param colorGraph barva grafu
     */
    public void setColorGraph(int colorGraph) {
        this.colorGraph = colorGraph;
        invalidate();
    }


    /** Nastaví pozici zarovnání na linku
     *
     * @return pozice zarovnání na linku
     */
    @Override
    public int getBaseline() {
        return (int) (26 * getResources().getDisplayMetrics().density);
    }


    /**
     * Ztmaví barvu
     *
     * @param color barva
     * @return barva
     */
    private int darkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // Snížení hodnoty světlosti o 20%
        return Color.HSVToColor(hsv);
    }


    /**
     * Zesvětlí barvu a nastaví průhlednost
     *
     * @param color barva
     * @return barva
     */
    private int lighterColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 1.2f; // Zvýšení hodnoty světlosti o 20%
        return Color.HSVToColor(30, hsv); //30:natavení průhlednosti
    }
}
