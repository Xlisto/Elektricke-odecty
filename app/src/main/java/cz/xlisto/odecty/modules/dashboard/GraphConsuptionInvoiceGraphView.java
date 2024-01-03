package cz.xlisto.odecty.modules.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;

import static cz.xlisto.odecty.utils.ColorUtils.*;
import static cz.xlisto.odecty.utils.ColorUtils.darkerColor;
import static cz.xlisto.odecty.utils.DensityUtils.*;


/**
 * Grafické zobrazení spotřeby faktury
 * Xlisto 27.12.2023 9:47
 */
public class GraphConsuptionInvoiceGraphView extends View {
    private static final String TAG = "GraphDashBoardView";
    private int width;
    private double consuption, consuptionMax;
    private String textConsuption;
    private int colorGraph = 0xFF003300;
    private Paint pText, pGraph, pGraphBorder, pGraphBackground;


    public GraphConsuptionInvoiceGraphView(Context context) {
        super(context);
        init();
    }


    public GraphConsuptionInvoiceGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }


    public GraphConsuptionInvoiceGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    public GraphConsuptionInvoiceGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }


    private void init() {
        setSaveEnabled(true);
        invalidate();
    }


    private void init(AttributeSet attrs) {
        setSaveEnabled(true);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GraphConsuptionInvoiceGraphView);
        consuption = a.getFloat(R.styleable.GraphConsuptionInvoiceGraphView_consuption, 30);
        consuptionMax = a.getFloat(R.styleable.GraphConsuptionInvoiceGraphView_consuptionMax, 100);
        colorGraph = a.getColor(R.styleable.GraphConsuptionInvoiceGraphView_colorGraph, -16701293);
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

        pGraphBorder.setStyle(Paint.Style.STROKE);
        pGraphBorder.setStrokeWidth(dpToPx(getContext(), 1));

        pText.setColor(colorGraph);
        pText.setTextSize(dpToPx(getContext(), 11));

        pGraph.setColor(colorGraph);
        pGraphBorder.setColor(darkerColor(colorGraph));
        pGraphBackground.setColor(lighterColor(colorGraph));

        float widthGraph = (int) (consuption * (width / (float) consuptionMax));
        float round = dpToPx(getContext(), 3);

        canvas.drawRoundRect(dpToPx(getContext(), 4), dpToPx(getContext(), 13), width, dpToPx(getContext(), 30), round, round, pGraphBackground);
        canvas.drawRoundRect(dpToPx(getContext(), 4), dpToPx(getContext(), 13), widthGraph, dpToPx(getContext(), 30), round, round, pGraph);
        canvas.drawRoundRect(dpToPx(getContext(), 4), dpToPx(getContext(), 13), widthGraph, dpToPx(getContext(), 30), round, round, pGraphBorder);
        canvas.drawText(textConsuption, dpToPx(getContext(), 4), dpToPx(getContext(), 10), pText);
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
        int height = dpToPx(getContext(), 34);
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
     *
     * @param textConsuption text popisku
     */
    public void setConsuption(String textConsuption) {
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


    /**
     * Nastaví pozici zarovnání na linku
     *
     * @return pozice zarovnání na linku
     */
    @Override
    public int getBaseline() {
        return dpToPx(getContext(), 26);
    }


}
