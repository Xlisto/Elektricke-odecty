package cz.xlisto.odecty.modules.pricelist;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.ownview.LabelEditText;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;

import static cz.xlisto.odecty.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT;

public class PriceListAddParametersDialogFragment extends DialogFragment {
    private static String TAG = "PriceListAddParametersDialogFragment";
    private CloseDialogWithPositiveButtonListener closeDialogWithPositiveButtonListener;
    private final String VT = "vt";
    private final String NT = "nt";
    private final String MONTH = "month";
    private final String PHAZE = "phaze";
    private final String POWER = "power";
    private final String SERVICES_L = "ser_l";
    private final String SERVICES_R = "ser_r";
    private LabelEditText letVT, letNT, letMonth, letPhaze, letPower, letServicesL, letServicesR;
    private Button btnReset;


    public static PriceListAddParametersDialogFragment newInstance(CloseDialogWithPositiveButtonListener closeDialogWithPositiveButtonListener,
                                                                   double vt, double nt, double month, double servicesL, double servicesR) {
        PriceListAddParametersDialogFragment frag = new PriceListAddParametersDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putDouble(frag.VT, vt);
        bundle.putDouble(frag.NT, nt);
        bundle.putDouble(frag.MONTH, month);
        bundle.putDouble(frag.SERVICES_L, servicesL);
        bundle.putDouble(frag.SERVICES_R, servicesR);
        frag.setArguments(bundle);
        frag.closeDialogWithPositiveButtonListener = closeDialogWithPositiveButtonListener;
        return frag;
    }

    public PriceListAddParametersDialogFragment() {
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_set_values, null);
        letVT = dialogView.findViewById(R.id.letVT);
        letNT = dialogView.findViewById(R.id.letNT);
        letMonth = dialogView.findViewById(R.id.letMonth);
        letPhaze = dialogView.findViewById(R.id.letPhaze);
        letPower = dialogView.findViewById(R.id.letPower);
        letServicesL = dialogView.findViewById(R.id.letServicesL);
        letServicesR = dialogView.findViewById(R.id.letServicesR);
        btnReset = dialogView.findViewById(R.id.btnReset);

        builder.setView(dialogView);
        builder.setTitle(R.string.nastaveni_parametru);
        builder.setNegativeButton(R.string.zrusit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeDialogWithPositiveButtonListener.onCloseDialogWithPositiveButton(
                        letVT.getDouble(), letNT.getDouble(),
                        letMonth.getDouble(),
                        letPhaze.getDouble(), letPower.getDouble(),
                        letServicesL.getDouble(), letServicesR.getDouble());
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                letVT.setDefaultText("");
                letNT.setDefaultText("");
                letMonth.setDefaultText("");
                letPhaze.setDefaultText("");
                letPower.setDefaultText("");
                letServicesL.setDefaultText("");
                letServicesR.setDefaultText("");
            }
        });

        setPhaze();
        Bundle bundle = getArguments();
        if (bundle.getDouble(VT) != 1)
            letVT.setDefaultText("" + bundle.getDouble(VT));
        if (bundle.getDouble(NT) != 1)
            letNT.setDefaultText("" + bundle.getDouble(NT));
        if (bundle.getDouble(MONTH) != 1)
            letMonth.setDefaultText("" + bundle.getDouble(MONTH));
        if (bundle.getDouble(SERVICES_L) != 0)
            letServicesL.setDefaultText("" + bundle.getDouble(SERVICES_L));
        if (bundle.getDouble(SERVICES_R) != 0)
            letServicesR.setDefaultText("" + bundle.getDouble(SERVICES_R));

        if (savedInstanceState != null) {
            letVT.setDefaultText(savedInstanceState.getString(VT));
            letNT.setDefaultText(savedInstanceState.getString(NT));
            letMonth.setDefaultText(savedInstanceState.getString(MONTH));
            letPhaze.setDefaultText(savedInstanceState.getString(PHAZE));
            letPower.setDefaultText(savedInstanceState.getString(POWER));
            letServicesL.setDefaultText(savedInstanceState.getString(SERVICES_L));
            letServicesR.setDefaultText(savedInstanceState.getString(SERVICES_R));
        }

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(VT, letVT.getText());
        outState.putString(NT, letNT.getText());
        outState.putString(MONTH, letMonth.getText());
        outState.putString(PHAZE, letPhaze.getText());
        outState.putString(POWER, letPower.getText());
        outState.putString(SERVICES_L, letServicesL.getText());
        outState.putString(SERVICES_R, letServicesR.getText());
    }

    /**
     * Nastaví výchozí příkon odběrného místa do porovnávání
     */
    private void setPhaze() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        long idSubscriptionPoint = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT, -1l);

        if (idSubscriptionPoint > 0) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource.open();
            SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
            letPower.setHintText("" + subscriptionPoint.getPhaze());
            letPhaze.setHintText("" + subscriptionPoint.getCountPhaze());
            dataSubscriptionPointSource.close();
        }
    }

    public interface CloseDialogWithPositiveButtonListener {
        void onCloseDialogWithPositiveButton(double vt, double nt, double month, double phaze, double power, double servicesL, double servicesR);
    }
}
