package cz.xlisto.elektrodroid.dialogs;

import static cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.pricelist.ConsuptionContainer;
import cz.xlisto.elektrodroid.modules.pricelist.engine.HistoricalReadingConsumptionHelper;
import cz.xlisto.elektrodroid.modules.pricelist.engine.HistoricalReadingConsumptionHelper.HistoricalConsumptionResult;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;

/**
 * Dialogové okno pro nastavení a úpravu parametrů porovnání ceníků elektřiny.
 * Umožňuje zadat spotřebu ručně nebo ji automaticky načíst z odečtů elektroměru za vybraný rok či průměr z historie.
 */
public class PriceListComparisonParametersDialogFragment extends DialogFragment {

    public static final String TAG = "PriceListAddParametersDialogFragment";
    public static final String CONSUPTION_CONTAINER = "consuptionContainer";

    private final String VT = "vt";
    private final String NT = "nt";
    private final String MONTH = "month";
    private final String PHAZE = "phaze";
    private final String POWER = "power";
    private final String SERVICES_L = "ser_l";
    private final String SERVICES_R = "ser_r";

    private RadioButton rbSourceManual;
    private RadioButton rbSourceHistoryYear;
    private RadioButton rbSourceHistoryAvg;

    private LinearLayout llYearSelector;
    private Spinner spHistoryYear;
    private TextView tvDataQualityNote;

    private LabelEditText letVT, letNT, letMonth;
    private EditText letPhaze, letPower, letServicesL, letServicesR;

    private SubscriptionPointModel subscriptionPoint;
    private List<Integer> availableYears = new ArrayList<>();

    public static PriceListComparisonParametersDialogFragment newInstance(ConsuptionContainer container) {
        PriceListComparisonParametersDialogFragment frag = new PriceListComparisonParametersDialogFragment();
        Bundle bundle = new Bundle();
        if (container != null) {
            bundle.putDouble(frag.VT, container.vt());
            bundle.putDouble(frag.NT, container.nt());
            bundle.putDouble(frag.MONTH, container.month());
            bundle.putDouble(frag.PHAZE, container.phaze());
            bundle.putDouble(frag.POWER, container.power());
            bundle.putDouble(frag.SERVICES_L, container.servicesL());
            bundle.putDouble(frag.SERVICES_R, container.servicesR());
        }
        frag.setArguments(bundle);
        return frag;
    }

    public PriceListComparisonParametersDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_set_values, null);

        RadioGroup rgSource = dialogView.findViewById(R.id.rgSource);
        rbSourceManual = dialogView.findViewById(R.id.rbSourceManual);
        rbSourceHistoryYear = dialogView.findViewById(R.id.rbSourceHistoryYear);
        rbSourceHistoryAvg = dialogView.findViewById(R.id.rbSourceHistoryAvg);

        llYearSelector = dialogView.findViewById(R.id.llYearSelector);
        spHistoryYear = dialogView.findViewById(R.id.spHistoryYear);
        tvDataQualityNote = dialogView.findViewById(R.id.tvDataQualityNote);

        letVT = dialogView.findViewById(R.id.letVT);
        letNT = dialogView.findViewById(R.id.letNT);
        letMonth = dialogView.findViewById(R.id.letMonth);
        letPhaze = dialogView.findViewById(R.id.letPhaze);
        letPower = dialogView.findViewById(R.id.letPower);
        letServicesL = dialogView.findViewById(R.id.letServicesL);
        letServicesR = dialogView.findViewById(R.id.letServicesR);
        Button btnReset = dialogView.findViewById(R.id.btnReset);

        loadSubscriptionPoint();

        if (getArguments() != null) {
            letVT.setDefaultText(DecimalFormatHelper.df2.format(getArguments().getDouble(VT)));
            letNT.setDefaultText(DecimalFormatHelper.df2.format(getArguments().getDouble(NT)));
            letMonth.setDefaultText(DecimalFormatHelper.df0.format(getArguments().getDouble(MONTH)));
            letPhaze.setText(DecimalFormatHelper.df0.format(getArguments().getDouble(PHAZE)));
            letPower.setText(DecimalFormatHelper.df0.format(getArguments().getDouble(POWER)));
            letServicesL.setText(DecimalFormatHelper.df2.format(getArguments().getDouble(SERVICES_L)));
            letServicesR.setText(DecimalFormatHelper.df2.format(getArguments().getDouble(SERVICES_R)));
        }

        rgSource.setOnCheckedChangeListener((group, checkedId) -> onSourceChanged());

        spHistoryYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (rbSourceHistoryYear.isChecked()) {
                    loadHistoricalDataForSelectedYear();
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setView(dialogView);
        builder.setTitle(R.string.nastaveni_parametru);
        builder.setNegativeButton(R.string.zrusit, (dialog, which) -> {
        });

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            double vt = letVT.getDouble();
            double nt = letNT.getDouble();
            double month = letMonth.getDouble();
            double phaze = parseInput(letPhaze);
            double power = parseInput(letPower);
            double servicesL = parseInput(letServicesL);
            double servicesR = parseInput(letServicesR);

            ConsuptionContainer container = new ConsuptionContainer(vt, nt, month, phaze, power, servicesL, servicesR);
            persistCompareParameters(container);

            Bundle bundle = new Bundle();
            bundle.putSerializable(CONSUPTION_CONTAINER, container);
            getParentFragmentManager().setFragmentResult(TAG, bundle);
        });

        btnReset.setOnClickListener(v -> {
            letVT.setDefaultText("");
            letNT.setDefaultText("");
            letMonth.setDefaultText("");
            letPhaze.setText("");
            letPower.setText("");
            letServicesL.setText("");
            letServicesR.setText("");
        });

        setPhazeAndPowerDefaults();

        return builder.create();
    }


    private void loadSubscriptionPoint() {
        ShPSubscriptionPoint shP = new ShPSubscriptionPoint(getActivity());
        long idSub = shP.get(ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSub > 0) {
            DataSubscriptionPointSource dataSource = new DataSubscriptionPointSource(getActivity());
            dataSource.open();
            subscriptionPoint = dataSource.loadSubscriptionPoint(idSub);
            dataSource.close();
        }

        if (subscriptionPoint != null) {
            availableYears = HistoricalReadingConsumptionHelper.getAvailableYears(requireContext(), subscriptionPoint);
            if (!availableYears.isEmpty()) {
                ArrayAdapter<Integer> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, availableYears);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spHistoryYear.setAdapter(adapter);
            }
        }
    }


    private void onSourceChanged() {
        if (rbSourceManual.isChecked()) {
            llYearSelector.setVisibility(View.GONE);
            tvDataQualityNote.setVisibility(View.GONE);
        } else if (rbSourceHistoryYear.isChecked()) {
            llYearSelector.setVisibility(View.VISIBLE);
            tvDataQualityNote.setVisibility(View.VISIBLE);
            loadHistoricalDataForSelectedYear();
        } else if (rbSourceHistoryAvg.isChecked()) {
            llYearSelector.setVisibility(View.GONE);
            tvDataQualityNote.setVisibility(View.VISIBLE);
            loadHistoricalDataForAverage();
        }
    }


    private void loadHistoricalDataForSelectedYear() {
        if (subscriptionPoint == null || availableYears.isEmpty()) return;
        int selectedIndex = spHistoryYear.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= availableYears.size()) selectedIndex = 0;
        int year = availableYears.get(selectedIndex);

        HistoricalConsumptionResult res = HistoricalReadingConsumptionHelper.calculateConsumptionForYear(requireContext(), subscriptionPoint, year);
        applyHistoricalResult(res);
    }


    private void loadHistoricalDataForAverage() {
        if (subscriptionPoint == null) return;
        HistoricalConsumptionResult res = HistoricalReadingConsumptionHelper.calculateAverageAnnualConsumption(requireContext(), subscriptionPoint);
        applyHistoricalResult(res);
    }


    private void applyHistoricalResult(HistoricalConsumptionResult res) {
        if (res == null) return;
        letVT.setDefaultText(DecimalFormatHelper.df2.format(res.vtKwh / 1000.0)); // Convert to MWh for Container input
        letNT.setDefaultText(DecimalFormatHelper.df2.format(res.ntKwh / 1000.0));
        letMonth.setDefaultText("12");
        tvDataQualityNote.setText(res.qualityNote);
    }

    private void setPhazeAndPowerDefaults() {
        if (subscriptionPoint != null) {
            letPower.setHint(DecimalFormatHelper.df0.format(subscriptionPoint.getPhaze()));
            letPhaze.setHint(DecimalFormatHelper.df0.format(subscriptionPoint.getCountPhaze()));

            if (letPower.getText().toString().trim().isEmpty()) {
                letPower.setText(DecimalFormatHelper.df0.format(subscriptionPoint.getPhaze()));
            }
            if (letPhaze.getText().toString().trim().isEmpty()) {
                letPhaze.setText(DecimalFormatHelper.df0.format(subscriptionPoint.getCountPhaze()));
            }
        }
    }

    private double parseInput(EditText editText) {
        String value = editText.getText().toString().trim();
        if (value.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(value.replace(",", "."));
    }

    private void persistCompareParameters(ConsuptionContainer container) {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        long idSubscriptionPoint = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSubscriptionPoint <= 0) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put(VT, container.vt());
            json.put(NT, container.nt());
            json.put(MONTH, container.month());
            json.put(PHAZE, container.phaze());
            json.put(POWER, container.power());
            json.put(SERVICES_L, container.servicesL());
            json.put(SERVICES_R, container.servicesR());

            DataSettingsSource dataSettingsSource = new DataSettingsSource(getActivity());
            dataSettingsSource.open();
            dataSettingsSource.setPriceListCompareParameters(idSubscriptionPoint, json.toString());
            dataSettingsSource.close();
        } catch (JSONException ignored) {
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }
}
