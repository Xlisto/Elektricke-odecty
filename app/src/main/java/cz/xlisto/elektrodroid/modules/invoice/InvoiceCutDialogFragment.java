package cz.xlisto.elektrodroid.modules.invoice;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.slider.Slider;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.utils.TextSizeAdjuster;


/**
 * DialogFragment pro rozdělení záznamu v období bez faktury
 * Xlisto 10.04.2023 17:47
 */
public class InvoiceCutDialogFragment extends DialogFragment {

    public static final String TAG = "InvoiceCutDialogFragment";
    private final static String MIN_DATE = "minDate";
    private final static String MAX_DATE = "maxDate";
    private final static String MAX_VT = "maxVt";
    private final static String MIN_VT = "minVt";
    private final static String MAX_NT = "maxNt";
    private final static String MIN_NT = "minNt";
    private final static String SHOW_NT = "showNt";
    private final static String ID_PRICE_LIST = "idPriceList";
    private final static String ID = "id";
    private final static String OTHER_SERVICES = "otherServices";
    private final static String TABLE = "table";
    public static final String RESULT = "result";
    public static final String RESULT_CUT_DIALOG_FRAGMENT = "resultCutDialogFragment";
    private Context context;
    private double maxVT, minVT, maxNT, minNT, otherServices;
    private long minDate, maxDate, idPriceList, id;
    private long dateDayStart, dateDayEnd; //konce prvního záznamu a začátek druhého záznamu
    private boolean showNT;
    private LabelEditText labVT, labNT;
    private Slider sliderVT, sliderNT, sliderDate;
    private Button btnDate;
    private TextView tvDateItem1, tvDateItem2, tvVTItem1, tvVTItem2, tvNTItem1, tvNTItem2;
    private RelativeLayout rlItem1In, rlItem2In;


    public static InvoiceCutDialogFragment newInstance(long minDate, long maxDate, double minVT, double maxVT, double minNT, double maxNT,
                                                       boolean showNT, long idPriceList, long id, double otherServices, String table) {
        InvoiceCutDialogFragment frag = new InvoiceCutDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(MIN_DATE, minDate);
        bundle.putLong(MAX_DATE, maxDate);
        bundle.putDouble(MIN_VT, minVT);
        bundle.putDouble(MAX_VT, maxVT);
        bundle.putDouble(MIN_NT, minNT);
        bundle.putDouble(MAX_NT, maxNT);
        bundle.putBoolean(SHOW_NT, showNT);
        bundle.putLong(ID_PRICE_LIST, idPriceList);
        bundle.putLong(ID, id);
        bundle.putDouble(OTHER_SERVICES, otherServices);
        bundle.putString(TABLE, table);
        frag.setArguments(bundle);

        return frag;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            minDate = getArguments().getLong(MIN_DATE);
            maxDate = getArguments().getLong(MAX_DATE);
            minVT = getArguments().getDouble(MIN_VT);
            maxVT = getArguments().getDouble(MAX_VT);
            minNT = getArguments().getDouble(MIN_NT);
            maxNT = getArguments().getDouble(MAX_NT);
            showNT = getArguments().getBoolean(SHOW_NT);
            idPriceList = getArguments().getLong(ID_PRICE_LIST);
            id = getArguments().getLong(ID);
            otherServices = getArguments().getDouble(OTHER_SERVICES);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invoice_cut, null);

        btnDate = dialogView.findViewById(R.id.btnDate);
        tvDateItem1 = dialogView.findViewById(R.id.tvDateItem1);
        tvDateItem2 = dialogView.findViewById(R.id.tvDateItem2);
        tvVTItem1 = dialogView.findViewById(R.id.tvVTItem1);
        tvVTItem2 = dialogView.findViewById(R.id.tvVTItem2);
        tvNTItem1 = dialogView.findViewById(R.id.tvNTItem1);
        tvNTItem2 = dialogView.findViewById(R.id.tvNTItem2);
        rlItem1In = dialogView.findViewById(R.id.rlItem1In);
        rlItem2In = dialogView.findViewById(R.id.rlItem2In);
        labVT = dialogView.findViewById(R.id.labVT);
        labNT = dialogView.findViewById(R.id.labNT);
        sliderDate = dialogView.findViewById(R.id.sliderDate);
        sliderVT = dialogView.findViewById(R.id.sliderVT);
        sliderNT = dialogView.findViewById(R.id.sliderNT);
        RelativeLayout rlNT = dialogView.findViewById(R.id.rlNT);
        Button btnCut = dialogView.findViewById(R.id.btnCut);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (showNT) {
            rlNT.setVisibility(View.VISIBLE);
        } else {
            rlNT.setVisibility(View.GONE);
        }

        labVT.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSlider(sliderVT, s, minVT, maxVT);
            }


            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        labNT.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSlider(sliderNT, s, minNT, maxNT);
            }


            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sliderDate.addOnChangeListener((slider, value, fromUser) -> {
            long selectedDate = (long) (value * (maxDate - minDate)) + minDate;
            btnDate.setText(ViewHelper.convertLongToDate(selectedDate));
            setTextView();
        });

        sliderVT.addOnChangeListener((slider, value, fromUser) -> {
            labVT.setDefaultText(DecimalFormatHelper.df2.format(value));
            setTextView();
        });

        sliderNT.addOnChangeListener((slider, value, fromUser) -> {
            labNT.setDefaultText(DecimalFormatHelper.df2.format(value));
            setTextView();
        });

        btnCut.setOnClickListener(v -> {
            cut(idPriceList, id, otherServices);
            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            getParentFragmentManager().setFragmentResult(RESULT_CUT_DIALOG_FRAGMENT, bundle);

            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(dialogView);
        builder.setTitle(getResources().getString(R.string.cut_record));
        settingSlidersLabs();
        setTextView();

        return builder.create();
    }


    /**
     * Nastavuje posuvníky a popisky pro hodnoty VT (Vysoký tarif) a NT (Nízský tarif) a pro datum.
     * Vypočítává polohu středové hodnoty pro každý posuvník pomocí minimální a maximální hodnoty.
     * Nastavuje minimální a maximální hodnoty a polohu posuvníků pro VT, NT a datum.
     * Nastavuje popisky pro VT a NT včetně minimální a maximální hodnoty a středovou hodnotu pro VT a NT.
     * Nastavuje formát popisků pro datum na textový řetězec ve formátu HH:mm:ss.
     */
    private void settingSlidersLabs() {
        double differenceVT = maxVT - minVT;
        double halfVT = minVT + (differenceVT / 2);
        double differenceNT = maxNT - minNT;
        double halfNT = minNT + (differenceNT / 2);

        float dateMax = (float) ((maxDate - minDate) / (double) (maxDate - minDate));
        if (0 == dateMax) {
            dateMax = 1;
        }
        sliderDate.setValueFrom(0);
        sliderDate.setValueTo(dateMax);
        sliderDate.setValue(dateMax / 2);

        if (minVT < maxVT) {
            sliderVT.setValueFrom((float) minVT);
            sliderVT.setValueTo((float) maxVT);
            sliderVT.setValue((float) halfVT);
        } else {
            sliderVT.setEnabled(false);
            labVT.setEnabled(false);
        }

        if (minNT < maxNT) {
            sliderNT.setValueFrom((float) minNT);
            sliderNT.setValueTo((float) maxNT);
            sliderNT.setValue((float) halfNT);
        } else {
            sliderNT.setEnabled(false);
            labNT.setEnabled(false);
        }
        labVT.setLabel(context.getResources().getString(R.string.string_dash_string,
                DecimalFormatHelper.df2.format(minVT), DecimalFormatHelper.df2.format(maxVT)));
        labVT.setDefaultText(DecimalFormatHelper.df2.format(halfVT));

        labNT.setLabel(context.getResources().getString(R.string.string_dash_string,
                DecimalFormatHelper.df2.format(minNT), DecimalFormatHelper.df2.format(maxNT)));
        labNT.setDefaultText(DecimalFormatHelper.df2.format(halfNT));

        tvDateItem1.setText(context.getResources().getString(R.string.string_dash_string,
                ViewHelper.convertLongToDate(minDate), ViewHelper.convertLongToDate(maxDate)));

        // Nastavuje formát popisků pro datum na textový řetězec ve formátu HH:mm:ss.
        sliderDate.setLabelFormatter(value -> ViewHelper.convertLongToDate(((long) (value * (maxDate - minDate)) + minDate)));
    }


    /**
     * Nastavuje posuvník na základě textového řetězce.
     *
     * @param sl  posuvník
     * @param s   textový řetězec
     * @param min minimální hodnota
     * @param max maximální hodnota
     */
    private void setSlider(Slider sl, CharSequence s, double min, double max) {

        try {
            double number = Double.parseDouble(String.valueOf(s).replace(',', '.'));
            if (number < min) {
                return;
            }
            if (number > max) {
                return;
            }

            sl.setValue((float) number);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Nastavuje textový řetězec pro popisky VT, NT a datum.
     */
    private void setTextView() {
        dateDayEnd = (long) (sliderDate.getValue() * (maxDate - minDate)) + minDate;
        dateDayStart = addDay((long) (sliderDate.getValue() * (maxDate - minDate)) + minDate);//přidávám 1 den
        dateDayEnd = dateDayEnd / 100000 * 100000;//zaokrouhlení
        dateDayStart = dateDayStart / 100000 * 100000;
        if (dateDayStart > maxDate) {
            dateDayStart = maxDate;
        }

        if (dateDayStart <= dateDayEnd) {
            dateDayEnd = subDay(dateDayEnd);
        }
        //datum
        tvDateItem1.setText(context.getResources().getString(R.string.string_dash_string,
                ViewHelper.convertLongToDate(minDate), ViewHelper.convertLongToDate(dateDayEnd)));
        tvDateItem2.setText(context.getResources().getString(R.string.string_dash_string,
                ViewHelper.convertLongToDate(dateDayStart), ViewHelper.convertLongToDate(maxDate)));

        //VT
        labVT.setLabel(getResources().getString(R.string.vt3));
        String vtItem1 = context.getResources().getString(R.string.string_dash_string_kwh,
                DecimalFormatHelper.df2.format(minVT), DecimalFormatHelper.df2.format(sliderVT.getValue()),
                DecimalFormatHelper.df2.format(sliderVT.getValue() - minVT));
        String vtItem2 = context.getResources().getString(R.string.string_dash_string_kwh,
                DecimalFormatHelper.df2.format(sliderVT.getValue()), DecimalFormatHelper.df2.format(maxVT),
                DecimalFormatHelper.df2.format(maxVT - sliderVT.getValue()));
        tvVTItem1.setText(vtItem1);
        tvVTItem2.setText(vtItem2);

        //NT
        labNT.setLabel(getResources().getString(R.string.nt3));
        String ntItem1 = context.getResources().getString(R.string.string_dash_string_kwh,
                DecimalFormatHelper.df2.format(minNT), DecimalFormatHelper.df2.format(sliderNT.getValue()),
                DecimalFormatHelper.df2.format(sliderNT.getValue() - minNT));
        String ntItem2 = context.getResources().getString(R.string.string_dash_string_kwh,
                DecimalFormatHelper.df2.format(sliderNT.getValue()), DecimalFormatHelper.df2.format(maxNT),
                DecimalFormatHelper.df2.format(maxNT - sliderNT.getValue()));
        tvNTItem1.setText(ntItem1);
        tvNTItem2.setText(ntItem2);
        TextSizeAdjuster.adjustTextSize(rlItem1In, tvVTItem1, requireContext());
        TextSizeAdjuster.adjustTextSize(rlItem2In, tvVTItem2, requireContext());
        TextSizeAdjuster.adjustTextSize(rlItem1In, tvNTItem1, requireContext());
        TextSizeAdjuster.adjustTextSize(rlItem2In, tvNTItem2, requireContext());

        float sizeVtItem1 = tvVTItem1.getTextSize();
        float sizeVtItem2 = tvVTItem2.getTextSize();
        float sizeNtItem1 = tvNTItem1.getTextSize();
        float sizeNtItem2 = tvNTItem2.getTextSize();

        float minVTSize = Math.min(sizeVtItem1, sizeVtItem2);
        float minNTSize = Math.min(sizeNtItem1, sizeNtItem2);
        float minSize = Math.min(minVTSize, minNTSize);

        tvVTItem1.setTextSize(TypedValue.COMPLEX_UNIT_PX, minSize);
        tvVTItem2.setTextSize(TypedValue.COMPLEX_UNIT_PX, minSize);
        tvNTItem1.setTextSize(TypedValue.COMPLEX_UNIT_PX, minSize);
        tvNTItem2.setTextSize(TypedValue.COMPLEX_UNIT_PX, minSize);
    }


    //todo: doplnit detekci výměny elektroměru
    private void cut(long idPriceList, long id, double otherServices) {
        InvoiceModel firstInvoice = new InvoiceModel(id, minDate, dateDayEnd,
                minVT, sliderVT.getValue(), minNT, sliderNT.getValue(), -1L,
                idPriceList, otherServices, "", false);
        InvoiceModel secondInvoice = new InvoiceModel(dateDayStart, maxDate,
                sliderVT.getValue(), maxVT, sliderNT.getValue(), maxNT, -1L,
                idPriceList, otherServices, "", false);

        WithOutInvoiceService.cutInvoice(requireContext(), firstInvoice, secondInvoice);
    }


    /**
     * Přidá jeden den k datumu.
     *
     * @param date datum
     * @return datum zvýšený o 1 den
     */
    private long addDay(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }


    /**
     * Odečte jeden den od data.
     *
     * @param date datum
     * @return datum snížený o 1 den
     */
    private long subDay(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTimeInMillis();
    }

}
