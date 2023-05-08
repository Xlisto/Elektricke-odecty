package cz.xlisto.odecty.ownview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import cz.xlisto.odecty.R;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static android.text.InputType.TYPE_NUMBER_FLAG_SIGNED;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;

/**
 * Sloučený View TextView s EditTextem.
 * Zjednodušuje vytvoření EditTextu s TextView.
 * Seznam atributů je v xml souboru attrs.xml s name LabelEditText
 */
public class LabelEditText extends RelativeLayout {
    private TextView textView;
    private EditText editText;


    public LabelEditText(Context context) {
        super(context);
    }


    public LabelEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet);
    }


    public LabelEditText(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context, attributeSet);
    }


    public LabelEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }


    /**
     * Inicializace z parametrů xml
     *
     * @param context      kontext aplikace
     * @param attributeSet parametry z xml
     */
    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attributeSet) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.label_edittext_view, this);
        RelativeLayout relativeLayout = findViewById(R.id.relative_layout);

        textView = findViewById(R.id.tvLabelEdit);
        RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        relativeLayoutParams.addRule(RelativeLayout.END_OF,textView.getId());

        //vytvoření EditTextu a přidání do layoutu
        editText = new EditText(context);
        editText.setLayoutParams(relativeLayoutParams);
        editText.setGravity(View.TEXT_ALIGNMENT_VIEW_END);
        editText.setHint("");
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setMinHeight(48);
        editText.setMinWidth(48);
        editText.setId(View.generateViewId());
        relativeLayout.addView(editText);

        setTexts(attributeSet);
        setMaxEms(attributeSet);
        setEms(attributeSet);
        setDirection(attributeSet);
        setImeOptions(attributeSet);
        setInputType(attributeSet);
        setGravity(attributeSet);
        numberFormatHint();
        setEnabled(attributeSet);
    }


    /**
     * Nastaví vzájemné rozvržení mezi TextView a EditText. Vedle sebe nebo pod sebou.
     *
     * @param attributeSet parametry z xml
     */
    private void setDirection(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        int layoutWidth = ta.getInt(R.styleable.LabelEditText_android_layout_width, -1);
        int direction = ta.getInt(R.styleable.LabelEditText_direction, 1);

        LayoutParams paramsTextView;
        LayoutParams paramsEditText;
        if (direction == 1) {
            //rozložení vedle sebe, minimální rozměry nebo přes celou obrazovku - podle rodiče
            paramsTextView = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            paramsEditText = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            paramsTextView.addRule(RelativeLayout.ALIGN_BASELINE, editText.getId());
            paramsTextView.addRule(RelativeLayout.ALIGN_PARENT_START);

            if (layoutWidth == -2) {
                paramsEditText.addRule(RelativeLayout.END_OF, textView.getId());
            } else {
                paramsTextView.addRule(RelativeLayout.START_OF, editText.getId());
                paramsEditText.addRule(RelativeLayout.ALIGN_PARENT_END, textView.getId());
            }
            paramsEditText.setMarginStart(ViewHelper.convertDpToPx(4, getContext()));
        } else {
            //rozložení pod sebou, přes celou šířku
            paramsTextView = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            paramsEditText = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            textView.setPadding(ViewHelper.convertDpToPx(4, getContext()), 0, 0, 0);
            paramsEditText.addRule(RelativeLayout.BELOW, textView.getId());
        }
        textView.setLayoutParams(paramsTextView);
        editText.setLayoutParams(paramsEditText);
        ta.recycle();
    }


    /**
     * Nastaví textové atributy v TextView a EditTextu z XML rozvržení
     *
     * @param attributeSet parametry z xml
     */
    private void setTexts(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        String label = ta.getString(R.styleable.LabelEditText_label);
        String defaultText = ta.getString(R.styleable.LabelEditText_defaultText);
        String hintText = ta.getString(R.styleable.LabelEditText_hintText);
        int inputType = ta.getInt(R.styleable.LabelEditText_android_inputType, 0);
        try {

            if (label != null) {
                setLabel(label);
            }
            if (defaultText != null) {
                setDefaultText(defaultText);
            }

            if (hintText != null) {
                setHintText(hintText);
            }
            editText.setInputType(inputType);
        } finally {
            ta.recycle();
        }
    }


    /**
     * Nastaví typ vstupu do EditTextu zadaný v XML
     *
     * @param attributeSet parametry z xml
     */
    private void setInputType(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        int inputTypeValue = ta.getInt(R.styleable.LabelEditText_android_inputType, 0);
        editText.setInputType(inputTypeValue);
        ta.recycle();
    }


    private void setImeOptions(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        int inputTypeValue = ta.getInt(R.styleable.LabelEditText_android_imeOptions, IME_FLAG_NO_EXTRACT_UI);
        editText.setImeOptions(inputTypeValue);
        ta.recycle();
    }


    /**
     * Nastaví maximální šířku EditTextu v M
     *
     * @param attributeSet parametry z xml
     */
    private void setMaxEms(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        int maxEms = ta.getInt(R.styleable.LabelEditText_android_maxEms, 10);
        editText.setMaxEms(maxEms);
        ta.recycle();
    }


    /**
     * Nastaví šířku EditTextu v M
     *
     * @param attributeSet parametry z xml
     */
    private void setEms(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        int ems = ta.getInt(R.styleable.LabelEditText_android_ems, 3);
        editText.setEms(ems);
        ta.recycle();
    }


    /**
     * Nastaví zarovnání textu vlevo/pravo
     *
     * @param attributeSet parametry z xml
     */
    private void setGravity(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        int gravity = ta.getInt(R.styleable.LabelEditText_android_gravity, 8388659);
        editText.setGravity(gravity);
        ta.recycle();
    }


    /**
     * Povolí/zakáže widget podle parametru v XML
     * @param attributeSet parametry z xml
     */
    public void setEnabled(AttributeSet attributeSet) {
        TypedArray ta = getContext().obtainStyledAttributes(attributeSet, R.styleable.LabelEditText);
        boolean enable = ta.getBoolean(R.styleable.LabelEditText_android_enabled, true);
        setEnabled(enable);
        ta.recycle();
    }


    /**
     * Povolí/zakáže widget programově
     * @param b true = povolit, false = zakázat
     */
    public void setEnabled(boolean b) {
        editText.setEnabled(b);
    }


    /**
     * Kontroluje desetinný oddělovač. Pokud je oddělovač desetinná čárka, napsaný oddělovač jako tečka jej zamění za čárku.
     * Zároveň kontroluje počet desetinných oddělovačů.
     */
    private void changeChar() {

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
                DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
                char decimalSeparator = symbols.getDecimalSeparator();
                //Log.w("TAG", "decimal separator " + decimalSeparator);
                int countCharSeparator = 0;
                char[] chars = s.toString().toCharArray();
                for (char ch : chars) {
                    if (ch == decimalSeparator)
                        countCharSeparator++;
                }
                if (decimalSeparator == ',') {
                    if (s.toString().contains(".")) {
                        String str;
                        if (countCharSeparator == 0)
                            str = s.toString().replace('.', decimalSeparator);
                        else
                            str = s.toString().replace(".", "");
                        editText.setText(str);
                        editText.setSelection(str.length());
                    }
                }
            }
        });
    }


    /**
     * Nastaví hint na nulu, pokud je v EditTextu nastavený inputType number|numberDecimal|numberSigned
     */
    private void numberFormatHint() {
        String hint = "";
        //nastavení hint na 0, jinak se nemění desetinná čárka
        if (editText.getInputType() == TYPE_CLASS_NUMBER || editText.getInputType() == TYPE_CLASS_NUMBER + TYPE_NUMBER_FLAG_DECIMAL || editText.getInputType() == TYPE_CLASS_NUMBER + TYPE_NUMBER_FLAG_SIGNED) {
            hint = "0";
            editText.setHint("0");
        }
        if (editText.getHint() != null)
            hint = editText.getHint().toString();
        editText.setHint(numberFormat(hint));
    }


    /**
     * Převede všechny číselné hodnoty do lokálního formátu
     * Převádí se u InputType number|numberDecimal|numberSigned
     */
    private String numberFormat(String s) {

        int inputTypeValue = editText.getInputType();
        if (s == null)
            return "";
        if (s.equals(""))
            return s;
        if (InputType.TYPE_CLASS_NUMBER == inputTypeValue ||
                (InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER) == inputTypeValue ||
                (InputType.TYPE_NUMBER_FLAG_SIGNED + InputType.TYPE_CLASS_NUMBER) == inputTypeValue) {

            double value = 0.0;
            try {
                value = Double.parseDouble(s);
            } catch (Exception e) {
                e.printStackTrace();
                //return;
            }

            changeChar();
            return NumberFormat.getInstance().format(value);
        }
        return s;
    }


    /**
     * Nastaví textový atribut u TextView
     *
     * @param label text popisku
     */
    public void setLabel(String label) {
        textView.setText(label);

    }


    /**
     * Nastaví přednastavený text u TextEditu
     *
     * @param defaultText přednastavený text
     */
    public void setDefaultText(String defaultText) {
        int position = editText.getSelectionStart();
        editText.setText(defaultText);
        editText.setSelection(position);
    }


    public void setHintText(String hintText) {
        editText.setHint(hintText);
    }


    /**
     * Vrátí obsah EditTextu
     *
     * @return vrátí obsah EditTextu jako String
     */
    public String getText() {
        return editText.getText().toString();
    }


    /**
     * Vrátí obsah EditTextu jako číslo double
     * Převede čárku co by desetinný oddělovač na desetinnou tečku
     *
     * @return vrátí obsah EditTextu jako double, pokud nelze převést, vrátí 0
     */
    public double getDouble() {
        int inputTypeValue = editText.getInputType();
        if (InputType.TYPE_CLASS_NUMBER == inputTypeValue ||
                (InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER) == inputTypeValue ||
                (InputType.TYPE_NUMBER_FLAG_SIGNED + InputType.TYPE_CLASS_NUMBER) == inputTypeValue) {
            try {
                DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
                DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
                char decimalSeparator = symbols.getDecimalSeparator();

                String string = "0";
                if (!editText.getText().toString().isEmpty()) {
                    string = editText.getText().toString().replace(decimalSeparator, '.');
                } else if (!editText.getHint().toString().isEmpty()) {
                    string = editText.getHint().toString().replace(decimalSeparator, '.');
                }
                return Double.parseDouble(string);
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0;
            }
        }

        return 0D;

    }


    public void addTextChangedListener(TextWatcher textWatcher) {
        editText.addTextChangedListener(textWatcher);
    }
}
