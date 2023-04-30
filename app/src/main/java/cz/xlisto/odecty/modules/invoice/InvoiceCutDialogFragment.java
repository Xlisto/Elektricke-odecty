package cz.xlisto.odecty.modules.invoice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.ownview.LabelEditText;
import cz.xlisto.odecty.ownview.ViewHelper;

/**
 * DialogFragment pro rozdělení záznamu v období bez faktury
 * Xlisto 10.04.2023 17:47
 */
public class InvoiceCutDialogFragment extends DialogFragment {
    private static final String TAG = "InvoiceCutDialogFragment";
    private static String MIN_DATE = "min_date";
    private static String MAX_DATE = "max_date";
    private static String MAX_VT = "max_vt";
    private static String MIN_VT = "min_vt";
    private static String MAX_NT = "max_nt";
    private static String MIN_NT = "min_nt";
    private static String SHOW_NT = "show_nt";
    private static String ID_PRICE_LIST = "id_price_list";
    private static String ID = "id";
    private static String OTHER_SERVICES = "other_services";
    private static String TABLE = "table";
    private double maxVT, minVT, maxNT, minNT;
    private long minDate, maxDate, idPriceList, id;
    private double otherServices;
    private LabelEditText labVT, labNT;
    private Slider sliderVT, sliderNT, sliderDate;
    private Button btnDate, btnCut, btnCancel;
    private boolean showNT;
    private RelativeLayout rlNT;
    private int million = 1000000;
    private String table;
    private OnCutListener onCutListener;

    private TextView tvDate;

    public static InvoiceCutDialogFragment newInstance(long minDate, long maxDate, double minVT, double maxVT, double minNT, double maxNT,
                                                       boolean showNT, long idPriceList, long id,double otherServices, String table) {
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
            table = getArguments().getString(TABLE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_invoice_cut, null);

        btnDate = dialogView.findViewById(R.id.btnDate);
        tvDate = dialogView.findViewById(R.id.tvDateInvoices);
        labVT = dialogView.findViewById(R.id.labVT);
        labNT = dialogView.findViewById(R.id.labNT);
        sliderDate = dialogView.findViewById(R.id.sliderDate);
        sliderVT = dialogView.findViewById(R.id.sliderVT);
        sliderNT = dialogView.findViewById(R.id.sliderNT);
        rlNT = dialogView.findViewById(R.id.rlNT);
        btnCut = dialogView.findViewById(R.id.btnCut);
        btnCancel = dialogView.findViewById(R.id.btnCancel);

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

        sliderDate.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                btnDate.setText(ViewHelper.convertLongToTime(((long) value) * million));
                setTextView();
            }
        });

        sliderVT.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                labVT.setDefaultText(DecimalFormatHelper.df2.format(value));
                setTextView();
            }
        });

        sliderNT.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                labNT.setDefaultText(DecimalFormatHelper.df2.format(value));
                setTextView();
            }
        });

        btnCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (getActivity() instanceof InvoiceCutDialogListener) {
                    ((InvoiceCutDialogListener) getActivity()).onCutInvoice(sliderDate.getValue(), sliderVT.getValue(), sliderNT.getValue());
                }*/
                cut(idPriceList, id,otherServices);
                onCutListener.onCut(true);
                dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCutListener.onCut(false);
                dismiss();
            }
        });

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
        long differenceDate = maxDate - minDate;
        //převádím rozpůlený long na datum ve stringu a potom zpět na long, abych získal celou hodnotu dne v 00:00 hodin
        //long halfDate = ViewHelper.parseCalendarFromString(ViewHelper.convertLongToTime(minDate + (differenceDate / 2))).getTimeInMillis();
        long halfDate = minDate + (differenceDate / 2);
        //Long longDate = new Long(halfDate);
        //longDate.floatValue(halfDate);
        /*float halfDateFloat = (float) halfDate/1000000;
        Log.w(TAG, "halfDate: " + halfDate);
        Log.w(TAG, "halfDate: " + halfDateFloat);
        Log.w(TAG, "halfDate: " + ((long)halfDateFloat)*1000000);*/
        double differenceVT = maxVT - minVT;
        double halfVT = minVT + (differenceVT / 2);
        double differenceNT = maxNT - minNT;
        double halfNT = minNT + (differenceNT / 2);

        sliderDate.setValueFrom((minDate + (25 * 60 * 60 * 1000)) / million);
        sliderDate.setValueTo((maxDate ) / million);
        sliderDate.setValue(halfDate / million);

        sliderVT.setValueFrom((float) minVT);
        sliderVT.setValueTo((float) maxVT);
        sliderVT.setValue((float) halfVT);

        sliderNT.setValueFrom((float) minNT);
        sliderNT.setValueTo((float) maxNT);
        sliderNT.setValue((float) halfNT);

        labVT.setLabel(DecimalFormatHelper.df2.format(minVT) + " - " + DecimalFormatHelper.df2.format(maxVT));
        labVT.setDefaultText(DecimalFormatHelper.df2.format(halfVT));

        labNT.setLabel(DecimalFormatHelper.df2.format(minNT) + " - " + DecimalFormatHelper.df2.format(maxNT));
        labNT.setDefaultText(DecimalFormatHelper.df2.format(halfNT));

        tvDate.setText(ViewHelper.convertLongToTime(minDate) + " - " + ViewHelper.convertLongToTime(maxDate));

        // Nastavuje formát popisků pro datum na textový řetězec ve formátu HH:mm:ss.
        sliderDate.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                return ViewHelper.convertLongToTime(((long) value)*million);
            }
        });
    }

    private void setSlider(Slider sl, CharSequence s, double min, double max) {

        try {
            double number = Double.valueOf(String.valueOf(s).replace(',', '.'));
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

    private void setTextView() {
        tvDate.setText(ViewHelper.convertLongToTime(minDate) + " - " + ViewHelper.convertLongToTime((((long) sliderDate.getValue()) * million) - (23 * 60 * 60 * 1000)) + "\n"
                + ViewHelper.convertLongToTime(((long) sliderDate.getValue()) * million) + " - " + ViewHelper.convertLongToTime(maxDate));

        labVT.setLabel(DecimalFormatHelper.df2.format(minVT) + " - " + DecimalFormatHelper.df2.format(sliderVT.getValue()) + "\n"
                + DecimalFormatHelper.df2.format(sliderVT.getValue()) + " - " + DecimalFormatHelper.df2.format(maxVT));

        labNT.setLabel(DecimalFormatHelper.df2.format(minNT) + " - " + DecimalFormatHelper.df2.format(sliderNT.getValue()) + "\n"
                + DecimalFormatHelper.df2.format(sliderNT.getValue()) + " - " + DecimalFormatHelper.df2.format(maxNT));
    }

    public void setOnCutListener(OnCutListener onCutListener) {
        this.onCutListener = onCutListener;
    }

    private void cut(long idPriceList, long id,double otherServices) {
        InvoiceModel firstInvoice = new InvoiceModel(id,minDate, ((long) (sliderDate.getValue())*million)-(23*60*60*1000),
                minVT, sliderVT.getValue(), minNT, sliderNT.getValue(), -1L,
                idPriceList, otherServices, "");
        InvoiceModel secondInvoice = new InvoiceModel(((long) sliderDate.getValue())*million, maxDate,
                sliderVT.getValue(), maxVT, sliderNT.getValue(), maxNT, -1L,
                idPriceList, otherServices, "");

        /*Log.w(TAG, "cut first: " + ViewHelper.convertLongToTime(firstInvoice.getDateOf()) + " " + ViewHelper.convertLongToTime(firstInvoice.getDateTo()));
        Log.w(TAG, "cut secon: " + ViewHelper.convertLongToTime(secondInvoice.getDateOf()) + " " + ViewHelper.convertLongToTime(secondInvoice.getDateTo()));
        Log.w(TAG, "cut first: " + firstInvoice.getVtStart() + " " + firstInvoice.getVtEnd());
        Log.w(TAG, "cut secon: " + secondInvoice.getVtStart()  + " " + secondInvoice.getVtEnd());
        Log.w(TAG, "cut id: " + firstInvoice.getId());*/

        WithOutInvoiceService.cutInvoice(getActivity(), firstInvoice, secondInvoice);
    }

    public interface OnCutListener {
        void onCut(boolean b);
    }
}
