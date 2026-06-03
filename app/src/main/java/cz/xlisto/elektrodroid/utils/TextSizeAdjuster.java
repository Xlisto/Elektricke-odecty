package cz.xlisto.elektrodroid.utils;


import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;


/**
 * Nastaví velikost písma v TextView tak, aby se všechna TextView vešla do šířky parentView
 * Xlisto 14.02.2024 21:11
 */
public class TextSizeAdjuster {

    /**
     * Přizpůsobí velikost písma jednoho TextView rodičovskému ViewGroup.
     * <p>
     * Zmenšuje velikost písma, dokud se text nevejde do šířky rodiče.
     *
     * @param parentView rodiče ViewGroup, do kterého se má text vejít
     * @param textView   TextView, jehož velikost se má přizpůsobit
     * @param context    kontext aplikace
     */
    public static void adjustTextSize(ViewGroup parentView, TextView textView, Context context) {
        List<TextView> textViews = Collections.singletonList(textView);
        adjustTextSize(parentView, textViews, context);
    }


    /**
     * Přizpůsobí velikost písma více TextViewů rodičovskému ViewGroup.
     * <p>
     * Zmenšuje velikost písma všech TextViewů, dokud se všechny texty nevejdou
     * do šířky rodiče, přičemž bere v úvahu marginy a padding všech prvků.
     *
     * @param parentView rodiče ViewGroup, do kterého se mají texty vejít
     * @param textViews  seznam TextViewů, jejichž velikost se má přizpůsobit
     * @param context    kontext aplikace
     */
    public static void adjustTextSize(ViewGroup parentView, List<TextView> textViews, Context context) {
        if (textViews == null || textViews.isEmpty()) return;

        parentView.post(() -> {
            int widthParent = parentView.getWidth();
            float textSize = textViews.get(0).getTextSize();
            Paint paint = new Paint();
            Typeface typeface = textViews.get(0).getTypeface();
            paint.setTypeface(typeface);

            // Převod minimální velikosti písma z 8sp na pixely
            float minTextSize = spToPx(context);

            float totalTextWidth;
            do {
                paint.setTextSize(textSize);
                totalTextWidth = 0;
                for (TextView textView : textViews) {
                    String text = textView.getText().toString();
                    totalTextWidth += paint.measureText(text);

                    // Získání marginů pro každý TextView
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
                    totalTextWidth += lp.leftMargin + lp.rightMargin; // Přidání marginů vlevo a v pravo

                    // Přidání paddingu vlevo a v pravo rodičovského view
                    totalTextWidth += parentView.getPaddingStart() + parentView.getPaddingEnd();

                    // Přidání paddingu vlevo a v pravo TextView
                    totalTextWidth += textView.getPaddingStart() + textView.getPaddingEnd();
                }

                if (totalTextWidth > widthParent && textSize > minTextSize) {
                    textSize -= 2; // Zmenšení velikosti písma
                } else {
                    break;
                }
            } while (totalTextWidth > widthParent && textSize > minTextSize);

            for (TextView textView : textViews) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }

            parentView.requestLayout();
            parentView.invalidate();
        });
    }


    /**
     * Převede minimální velikost písma z jednotek sp na pixely.
     * <p>
     * Minimální jednotka je nastavena na 8sp, což zabraňuje
     * příliš malému písmu.
     *
     * @param context kontext aplikace
     * @return minimální velikost písma v pixelech
     */
    private static float spToPx(Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (float) 8, context.getResources().getDisplayMetrics());
    }

}
