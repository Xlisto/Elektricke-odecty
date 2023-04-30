package cz.xlisto.odecty.ownview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cz.xlisto.odecty.R;

public class LabelPriceDetail extends RelativeLayout {
    private RelativeLayout relativeLayout;
    private TextView tvLabel,tvPrice,tvItem;


    public LabelPriceDetail(Context context) {
        super(context);
    }

    public LabelPriceDetail(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    /**
     * Inicializace z paramterů xml
     *
     * @param context
     * @param attributeSet
     */
    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attributeSet) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.label_price_detail, this);

        relativeLayout = findViewById(R.id.relative_layout);
        tvLabel = findViewById(R.id.tvLabel);
        tvPrice = findViewById(R.id.tvPrice);
        tvItem = findViewById(R.id.tvItem);

        setTexts(attributeSet);
        //setGravity(attributeSet);
        //numberFormat();

    }

    /**
     * Nastaví textové atributy v TextView a EditTextu z XML rozvržení
     *
     * @param attributeSet
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
     * @param label
     */
    public void setLabel(String label) {
        tvLabel.setText(label);
    }

    /**
     * Nastaví textový atribut u TextView
     *
     * @param label
     */
    public void setPrice(String label) {
        tvPrice.setText(label);
    }

    /**
     * Nastaví textový atribut u TextView
     *
     * @param label
     */
    public void setItem(String label) {
        tvItem.setText(label);
    }

    /**
     * Vrátí obsah TextView
     *
     * @return
     */
    public String getLabel() {
        return tvLabel.getText().toString();
    }
    /**
     * Vrátí obsah TextView
     *
     * @return
     */
    public String getPrice() {
        return tvPrice.getText().toString();
    }
    /**
     * Vrátí obsah TextView
     *
     * @return
     */
    public String getItem() {
        return tvItem.getText().toString();
    }

}
