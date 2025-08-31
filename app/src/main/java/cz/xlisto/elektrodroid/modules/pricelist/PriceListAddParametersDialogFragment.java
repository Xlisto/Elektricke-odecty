package cz.xlisto.elektrodroid.modules.pricelist;


import static cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;


public class PriceListAddParametersDialogFragment extends DialogFragment {
    public static String TAG = "PriceListAddParametersDialogFragment";
    public static String CONSUPTION_CONTAINER = "consuptionContainer";
    private final String VT = "vt";
    private final String NT = "nt";
    private final String MONTH = "month";
    private final String PHAZE = "phaze";
    private final String POWER = "power";
    private final String SERVICES_L = "ser_l";
    private final String SERVICES_R = "ser_r";
    private LabelEditText letVT, letNT, letMonth;
    private EditText letPhaze, letPower, letServicesL, letServicesR;


    public static PriceListAddParametersDialogFragment newInstance(PriceListCompareBoxFragment.ConsuptionContainer container) {
        PriceListAddParametersDialogFragment frag = new PriceListAddParametersDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putDouble(frag.VT, container.vt);
        bundle.putDouble(frag.NT, container.nt);
        bundle.putDouble(frag.MONTH, container.month);
        bundle.putDouble(frag.SERVICES_L, container.servicesL);
        bundle.putDouble(frag.SERVICES_R, container.servicesR);
        frag.setArguments(bundle);
        return frag;
    }


    public PriceListAddParametersDialogFragment() {
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_set_values, null);
        letVT = dialogView.findViewById(R.id.letVT);
        letNT = dialogView.findViewById(R.id.letNT);
        letMonth = dialogView.findViewById(R.id.letMonth);
        letPhaze = dialogView.findViewById(R.id.letPhaze);
        letPower = dialogView.findViewById(R.id.letPower);
        letServicesL = dialogView.findViewById(R.id.letServicesL);
        letServicesR = dialogView.findViewById(R.id.letServicesR);
        Button btnReset = dialogView.findViewById(R.id.btnReset);

        if (getArguments() != null) {
            letVT.setDefaultText(DecimalFormatHelper.df2.format(getArguments().getDouble(VT)));
            letNT.setDefaultText(DecimalFormatHelper.df2.format(getArguments().getDouble(NT)));
            letMonth.setDefaultText(DecimalFormatHelper.df0.format(getArguments().getDouble(MONTH)));
            letServicesL.setText(DecimalFormatHelper.df2.format(getArguments().getDouble(SERVICES_L)));
            letServicesR.setText(DecimalFormatHelper.df2.format(getArguments().getDouble(SERVICES_R)));
        }

        builder.setView(dialogView);
        builder.setTitle(R.string.nastaveni_parametru);
        builder.setNegativeButton(R.string.zrusit, (dialog, which) -> {

        });
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            double vt = letVT.getDouble();
            double nt = letNT.getDouble();
            double month = letMonth.getDouble();
            double phaze = letPhaze.getText().toString().isEmpty() ? 0 : Double.parseDouble(letPhaze.getText().toString().replace(",", "."));
            double power = letPower.getText().toString().isEmpty() ? 0 : Double.parseDouble(letPower.getText().toString().replace(",", "."));
            double servicesL = letServicesL.getText().toString().isEmpty() ? 0 : Double.parseDouble(letServicesL.getText().toString().replace(",", "."));
            double servicesR = letServicesR.getText().toString().isEmpty() ? 0 : Double.parseDouble(letServicesR.getText().toString().replace(",", "."));
            PriceListCompareBoxFragment.ConsuptionContainer container = new PriceListCompareBoxFragment.ConsuptionContainer(vt, nt, month, phaze, power, servicesL, servicesR);
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

        setPhaze();

        if (savedInstanceState != null) {
            letVT.setDefaultText(savedInstanceState.getString(VT));
            letNT.setDefaultText(savedInstanceState.getString(NT));
            letMonth.setDefaultText(savedInstanceState.getString(MONTH));
            letPhaze.setText(savedInstanceState.getString(PHAZE));
            letPower.setText(savedInstanceState.getString(POWER));
            letServicesL.setText(savedInstanceState.getString(SERVICES_L));
            letServicesR.setText(savedInstanceState.getString(SERVICES_R));
        }

        return builder.create();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(VT, letVT.getText());
        outState.putString(NT, letNT.getText());
        outState.putString(MONTH, letMonth.getText());
        outState.putString(PHAZE, letPhaze.getText().toString());
        outState.putString(POWER, letPower.getText().toString());
        outState.putString(SERVICES_L, letServicesL.getText().toString());
        outState.putString(SERVICES_R, letServicesR.getText().toString());
    }


    /**
     * Nastaví výchozí příkon odběrného místa do porovnávání
     */
    private void setPhaze() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        long idSubscriptionPoint = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);

        if (idSubscriptionPoint > 0) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource.open();
            SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
            letPower.setHint(DecimalFormatHelper.df0.format(subscriptionPoint.getPhaze()));
            letPower.setText(DecimalFormatHelper.df0.format(subscriptionPoint.getPhaze()));
            letPhaze.setHint(DecimalFormatHelper.df0.format(subscriptionPoint.getCountPhaze()));
            letPhaze.setText(DecimalFormatHelper.df0.format(subscriptionPoint.getCountPhaze()));
            dataSubscriptionPointSource.close();
        }
    }
}
