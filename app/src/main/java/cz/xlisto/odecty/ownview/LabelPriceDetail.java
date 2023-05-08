package cz.xlisto.odecty.ownview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cz.xlisto.odecty.R;

/**
 * Sloučený View TextView (2x) s EditTextem.
 * Zjednodušuje zobrazení detailu položky ceníku.
 * Seznam atributů je v xml souboru attrs.xml s name LabelPriceDetail
 */
public class LabelPriceDetail extends RelativeLayout {
    private TextView tvLabel,tvPrice, tvLabelItemUnit;


    public LabelPriceDetail(Context context) {
        super(context);
    }


    public LabelPriceDetail(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }


    public LabelPriceDetail(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }


    /**
     * Inicializace z parametrů xml
     *
     * @param context kontext aplikace
     * @param attributeSet  parametry z xml
     */
    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attributeSet) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.label_price_detail, this);

        tvLabel = findViewById(R.id.tvPriceDetailLabel);
        tvPrice = findViewById(R.id.tvlPriceDetailPrice);
        tvLabelItemUnit = findViewById(R.id.tvPricedetailUnit);

        setTexts(attributeSet);

    }


    /**
     * Nastaví textové atributy v TextView a EditTextu z XML rozvržení
     *
     * @param attributeSet parametry z xml
     */
    private void setTexts(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelPriceDetail);
        String label = ta.getString(R.styleable.LabelPriceDetail_label_price_item);
        String price = ta.getString(R.styleable.LabelPriceDetail_price);
        String item = ta.getString(R.styleable.LabelPriceDetail_item);
        try {

            if (label != null) {
                setLabel(label);
            }
            if (price != null) {
                setPrice(price);
            }
            if (item != null) {
                setItem(item);
            }
        } finally {
            ta.recycle();
        }
    }


    /**
     * Nastaví textový atribut u TextView
     *
     * @param label text popisku
     */
    public void setLabel(String label) {
        tvLabel.setText(label);
    }


    /**
     * Nastaví textový atribut u TextView
     *
     * @param label text popisku
     */
    public void setPrice(String label) {
        tvPrice.setText(label);
    }


    /**
     * Nastaví textový atribut u TextView
     *
     * @param label text popisku
     */
    public void setItem(String label) {
        tvLabelItemUnit.setText(label);
    }


    /**
     * Vrátí obsah TextView
     *
     * @return obsah popisku
     */
    public String getLabel() {
        return tvLabel.getText().toString();
    }


    /**
     * Vrátí obsah TextView
     *
     * @return cena
     */
    public String getPrice() {
        return tvPrice.getText().toString();
    }
}
