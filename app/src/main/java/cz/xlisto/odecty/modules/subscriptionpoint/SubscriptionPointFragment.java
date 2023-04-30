package cz.xlisto.odecty.modules.subscriptionpoint;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.dialogs.YesNoDialogFragment;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.FragmentChange;

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
    private final String TAG = getClass().getName() + " ";
    private FloatingActionButton fab;
    private Spinner spSubscriptionPoint;
    private TextView tvDescription, tvPhaze, tvNumberElectrometer, tvNumberSubscriptionPoint, tvNewSubscriptionPoint;
    private LinearLayout lnSpinner,lnDescription,lnPhaze,lnNumberElectricmeter,lnNumberSubscriptionPoint;
    private Button btnEdit, btnDelete;
    private long itemId, milins;

    //TODO: Doplnit dalčí detaily o počtu údaju odběrného místa a ukládat id odběrného místa do sharedprefences

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SubscriptionPointFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SubscriptionPointFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SubscriptionPointFragment newInstance(String param1, String param2) {
        SubscriptionPointFragment fragment = new SubscriptionPointFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subscription_point, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fab = view.findViewById(R.id.fab);
        spSubscriptionPoint = view.findViewById(R.id.spSubscriptionPoints);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvPhaze = view.findViewById(R.id.tvPhaze);
        tvNumberElectrometer = view.findViewById(R.id.tvNumberElectrometer);
        tvNumberSubscriptionPoint = view.findViewById(R.id.tvNumberSubscriptionPoint);
        tvNewSubscriptionPoint = view.findViewById(R.id.tvCreateNewSubscriptionPoint);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        lnSpinner = view.findViewById(R.id.lnSpinner);
        lnDescription = view.findViewById(R.id.lnDescription);
        lnPhaze = view.findViewById(R.id.lnPhaze);
        lnNumberElectricmeter = view.findViewById(R.id.lnNumberElectrometer);
        lnNumberSubscriptionPoint = view.findViewById(R.id.lnNumberSubscriptionPoint);


        fab.setOnClickListener(v -> {
            SubscriptionPointAddFragment subscriptionPointAddFragment = new SubscriptionPointAddFragment();
            FragmentChange.replace(getActivity(), subscriptionPointAddFragment, MOVE,true);
        });

        btnEdit.setOnClickListener(v -> edit(itemId));
        btnDelete.setOnClickListener(v -> showDeleteDialog(itemId, milins));
    }

    @Override
    public void onResume() {
        super.onResume();
        ShPSubscriptionPoint shp = new ShPSubscriptionPoint(getActivity());

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPoints = dataSubscriptionPointSource.loadSubscriptionPoints();
        dataSubscriptionPointSource.close();

        spSubscriptionPoint.setAdapter(new MySpinnerSubscriptionPointAdapter(getActivity(), android.R.layout.simple_list_item_1, subscriptionPoints));
        spSubscriptionPoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setText(subscriptionPoints.get(position));
                shp.set(ID_SUBSCRIPTION_POINT,subscriptionPoints.get(position).get_id());
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
        for(int i=0;i<subscriptionPoints.size();i++){
            if(subscriptionPoints.get(i).get_id()==shp.get(ID_SUBSCRIPTION_POINT,0L)) {
                spSubscriptionPoint.setSelection(i);
                break;
            }
        }
    }

    /**
     * Z objektu odběrného místa nastaví popisky do textview
     *
     * @param subscriptionPoint
     */
    private void setText(SubscriptionPointModel subscriptionPoint) {
        itemId = subscriptionPoint.get_id();
        milins = subscriptionPoint.getMilins();
        tvDescription.setText(subscriptionPoint.getDescription());
        tvPhaze.setText(subscriptionPoint.getCountPhaze() + " x " + subscriptionPoint.getPhaze() + "A");
        tvNumberElectrometer.setText(subscriptionPoint.getNumberElectricMeter());
        tvNumberSubscriptionPoint.setText(subscriptionPoint.getNumberSubscriptionPoint());
    }

    /**
     * Zobrazí fragment na editaci odběrného místa
     * @param itemId
     */
    private void edit(long itemId) {
        FragmentChange.replace(getActivity(), SubscriptionPointEditFragment.newInstance(itemId), MOVE,true);
    }

    /**
     * Zobrazí dialogové okno s dotazem na smazání
     * @param itemId
     * @param milins
     */
    private void showDeleteDialog(long itemId, long milins) {
        YesNoDialogFragment yesNoDialogFragment = YesNoDialogFragment.newInstance(new YesNoDialogFragment.OnDialogResult() {
            @Override
            public void onResult(boolean b) {
                if (b)
                    deleteItemSubscriptionPoint(itemId, milins);
            }
        }, getResources().getString(R.string.smazat_odberne_misto2));
        yesNoDialogFragment.show(getActivity().getSupportFragmentManager(), TAG);
    }

    /**
     * smaže odběrné místo
     * @param itemId
     * @param milins
     */
    private void deleteItemSubscriptionPoint(long itemId, long milins) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        dataSubscriptionPointSource.deleteSubscriptionPoint(itemId, milins);
        dataSubscriptionPointSource.close();
        onResume();
    }

    /**
     * Při žádném odběrném místě zobrazí výzvu k založení nového místa
     * @param show
     */
    private void hideAlert(boolean show) {
        if (show) {
            lnSpinner.setVisibility(GONE);
            lnDescription.setVisibility(GONE);
            lnPhaze.setVisibility(GONE);
            lnNumberElectricmeter.setVisibility(GONE);
            lnNumberSubscriptionPoint.setVisibility(GONE);
            btnEdit.setVisibility(GONE);
            btnDelete.setVisibility(GONE);
            tvNewSubscriptionPoint.setVisibility(VISIBLE);
        } else {
            lnSpinner.setVisibility(VISIBLE);
            lnDescription.setVisibility(VISIBLE);
            lnPhaze.setVisibility(VISIBLE);
            lnNumberElectricmeter.setVisibility(VISIBLE);
            lnNumberSubscriptionPoint.setVisibility(VISIBLE);
            btnEdit.setVisibility(VISIBLE);
            btnDelete.setVisibility(VISIBLE);
            tvNewSubscriptionPoint.setVisibility(View.GONE);
        }
    }
}