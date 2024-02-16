package cz.xlisto.odecty.modules.subscriptionpoint;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSettingsSource;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPDashBoard;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.FragmentChange;
import cz.xlisto.odecty.utils.MainActivityHelper;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static cz.xlisto.odecty.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT;
import static cz.xlisto.odecty.utils.FragmentChange.Transaction.MOVE;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SubscriptionPointFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SubscriptionPointFragment extends Fragment {
    private static final String TAG = "SubscriptionPointFragment";
    private static final String FLAG_DELETE_SUBSCRIPTION_POINT = "flagDeleteSubscriptionPoint";
    private Spinner spSubscriptionPoint;
    private TextView tvDescription, tvPhaze, tvNumberElectricMeter, tvNumberSubscriptionPoint, tvNewSubscriptionPoint;
    private LinearLayout lnSpinner, lnDescription, lnPhaze, lnNumberElectricMeter, lnNumberSubscriptionPoint;
    private Button btnEdit, btnDelete;
    private long itemId, milins;

    //TODO: Doplnit další detaily o počtu údaju odběrného místa a ukládat id odběrného místa do sharedprefences


    public SubscriptionPointFragment() {
    }


    /**
     * Fragment zobrazení odběrných míst
     *
     * @return Nová instance fragmentu SubscriptionPointFragment.
     */

    public static SubscriptionPointFragment newInstance() {
        return new SubscriptionPointFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subscription_point, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().invalidateOptionsMenu();
        FloatingActionButton fab = view.findViewById(R.id.fab);
        spSubscriptionPoint = view.findViewById(R.id.spSubscriptionPoints);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvPhaze = view.findViewById(R.id.tvPhaze);
        tvNumberElectricMeter = view.findViewById(R.id.tvNumberElectrometer);
        tvNumberSubscriptionPoint = view.findViewById(R.id.tvNumberSubscriptionPoint);
        tvNewSubscriptionPoint = view.findViewById(R.id.tvCreateNewSubscriptionPoint);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        lnSpinner = view.findViewById(R.id.lnSpinner);
        lnDescription = view.findViewById(R.id.lnDescription);
        lnPhaze = view.findViewById(R.id.lnPhaze);
        lnNumberElectricMeter = view.findViewById(R.id.lnNumberElectrometer);
        lnNumberSubscriptionPoint = view.findViewById(R.id.lnNumberSubscriptionPoint);


        fab.setOnClickListener(v -> {
            SubscriptionPointAddFragment subscriptionPointAddFragment = new SubscriptionPointAddFragment();
            FragmentChange.replace(requireActivity(), subscriptionPointAddFragment, MOVE, true);
        });

        btnEdit.setOnClickListener(v -> edit());
        btnDelete.setOnClickListener(v -> showDeleteDialog());

        //posluchač na odstranění odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(FLAG_DELETE_SUBSCRIPTION_POINT, this,
                (requestKey, result) -> {
                    if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                        deleteItemSubscriptionPoint();
                    }
                });

        //posluchač na změnu odběrného místa
        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT,
                this,
                (requestKey, result) -> onResume()
        );
    }


    @Override
    public void onResume() {
        super.onResume();
        ShPSubscriptionPoint shp = new ShPSubscriptionPoint(getActivity());

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPoints = dataSubscriptionPointSource.loadSubscriptionPoints();
        dataSubscriptionPointSource.close();

        spSubscriptionPoint.setAdapter(new MySpinnerSubscriptionPointAdapter(requireActivity(), android.R.layout.simple_list_item_1, subscriptionPoints));
        spSubscriptionPoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setText(subscriptionPoints.get(position));
                shp.set(ID_SUBSCRIPTION_POINT, subscriptionPoints.get(position).getId());

                MainActivityHelper.updateToolbarAndLoadData(requireActivity());
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (subscriptionPoints.size() > 0) {
            setText(subscriptionPoints.get(spSubscriptionPoint.getSelectedItemPosition()));
            hideAlert(false);
        } else {
            hideAlert(true);
        }
        //nastavení spinneru podle uloženého id odběrného místa
        for (int i = 0; i < subscriptionPoints.size(); i++) {
            if (subscriptionPoints.get(i).getId() == shp.get(ID_SUBSCRIPTION_POINT, 0L)) {
                spSubscriptionPoint.setSelection(i);
                break;
            }
        }
    }


    /**
     * Z objektu odběrného místa nastaví popisky do textview
     *
     * @param subscriptionPoint Objekt odběrného místa
     */
    private void setText(SubscriptionPointModel subscriptionPoint) {
        itemId = subscriptionPoint.getId();
        milins = subscriptionPoint.getMilins();
        tvDescription.setText(subscriptionPoint.getDescription());
        tvPhaze.setText(getResources().getString(R.string.power, subscriptionPoint.getCountPhaze(), subscriptionPoint.getPhaze()));
        tvNumberElectricMeter.setText(subscriptionPoint.getNumberElectricMeter());
        tvNumberSubscriptionPoint.setText(subscriptionPoint.getNumberSubscriptionPoint());
    }


    /**
     * Zobrazí fragment na editaci odběrného místa
     */
    private void edit() {
        FragmentChange.replace(requireActivity(), SubscriptionPointEditFragment.newInstance(itemId), MOVE, true);
    }


    /**
     * Zobrazí dialogové okno s dotazem na smazání
     */
    private void showDeleteDialog() {
        YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(getResources().getString(R.string.smazat_odberne_misto2), FLAG_DELETE_SUBSCRIPTION_POINT);
        yesNoDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
    }


    /**
     * Smaže odběrné místo
     */
    private void deleteItemSubscriptionPoint() {
        DataSettingsSource dataSettingsSource = new DataSettingsSource(getActivity());
        dataSettingsSource.open();
        dataSettingsSource.deleteTimeShift(Objects.requireNonNull(SubscriptionPoint.load(requireActivity())).getId());
        dataSettingsSource.close();

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deleteSubscriptionPoint(itemId, milins);
        dataSubscriptionPointSource.close();

        //nastavení prvního odběrného místa v DashBoardu
        ShPDashBoard shp = new ShPDashBoard(requireContext());
        shp.set(ShPDashBoard.SHOW_INVOICE_SUM, 0);

        onResume();
    }


    /**
     * Při žádném odběrném místě zobrazí výzvu k založení nového místa
     *
     * @param show true - zobrazí výzvu, false - skryje výzvu
     */
    private void hideAlert(boolean show) {
        if (show) {
            lnSpinner.setVisibility(GONE);
            lnDescription.setVisibility(GONE);
            lnPhaze.setVisibility(GONE);
            lnNumberElectricMeter.setVisibility(GONE);
            lnNumberSubscriptionPoint.setVisibility(GONE);
            btnEdit.setVisibility(GONE);
            btnDelete.setVisibility(GONE);
            tvNewSubscriptionPoint.setVisibility(VISIBLE);
        } else {
            lnSpinner.setVisibility(VISIBLE);
            lnDescription.setVisibility(VISIBLE);
            lnPhaze.setVisibility(VISIBLE);
            lnNumberElectricMeter.setVisibility(VISIBLE);
            lnNumberSubscriptionPoint.setVisibility(VISIBLE);
            btnEdit.setVisibility(VISIBLE);
            btnDelete.setVisibility(VISIBLE);
            tvNewSubscriptionPoint.setVisibility(View.GONE);
        }
    }
}