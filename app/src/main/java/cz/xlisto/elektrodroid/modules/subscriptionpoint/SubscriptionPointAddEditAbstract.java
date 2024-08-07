package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.utils.Keyboard;

public abstract class SubscriptionPointAddEditAbstract extends Fragment {
    private final String TAG = "SubscriptionPointAddEditAbstract";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ELECTROMETER = "electroMeter";
    private static final String SUBSCRIPTION_POINT = "subcsriptionPoint";
    private static final String COUNT_PHAZE = "count_phaze";
    private static final String PHAZE = "phaze";
    Button btnBack, btnSave;
    LabelEditText letName, letDescription, letNumberEletrometer, letNumberSubscriptionPoint;
    EditText etCountPhaze, etPhaze;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subscription_point_add_edit, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave = view.findViewById(R.id.btnUloz);
        btnBack = view.findViewById(R.id.btnZpet);

        letName = view.findViewById(R.id.letName);
        letDescription = view.findViewById(R.id.letDescription);
        letNumberEletrometer = view.findViewById(R.id.letElektrometer);
        letNumberSubscriptionPoint = view.findViewById(R.id.letSebdescriptionPoint);
        etCountPhaze = view.findViewById(R.id.etCountPhaze);
        etPhaze = view.findViewById(R.id.etPhaze);

        if (savedInstanceState != null) {
            letName.setDefaultText(savedInstanceState.getString(NAME, ""));
            letDescription.setDefaultText(savedInstanceState.getString(DESCRIPTION, ""));
            letNumberEletrometer.setDefaultText(savedInstanceState.getString(ELECTROMETER, ""));
            letNumberSubscriptionPoint.setDefaultText(savedInstanceState.getString(SUBSCRIPTION_POINT, ""));
            etCountPhaze.setText(savedInstanceState.getString(COUNT_PHAZE, "3"));
            etPhaze.setText(savedInstanceState.getString(PHAZE, "25"));
        }

        btnBack.setOnClickListener(v -> {
            Keyboard.hide(requireActivity());
            getParentFragmentManager().popBackStack();
        });
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(NAME, letName.getText());
        outState.putString(DESCRIPTION, letDescription.getText());
        outState.putString(ELECTROMETER, letNumberEletrometer.getText());
        outState.putString(SUBSCRIPTION_POINT, letNumberSubscriptionPoint.getText());
        outState.putString(COUNT_PHAZE, etCountPhaze.getText().toString());
        outState.putString(PHAZE, etPhaze.getText().toString());
    }


    /**
     * Vytvoří objekt odběrné místo, jako jedinečnou identifikaci záznamu v databázi použije aktuální čas (long)
     *
     * @return SubscriptionPointModel - odběrné místo
     */
    protected SubscriptionPointModel createSubscriptionPoint() {
        Calendar calendar = Calendar.getInstance();
        return createSubscriptionPoint(calendar.getTimeInMillis());
    }


    /**
     * Vytvoří objekt odběrné místo, jako jedinečnou identifikaci záznamu v databázi použije zadaný čas (long)
     *
     * @param milins long čas
     * @return SubscriptionPointModel - odběrné místo
     */
    protected SubscriptionPointModel createSubscriptionPoint(long milins) {
        int countPhaze = 3, phaze = 25;
        try {
            countPhaze = Integer.parseInt(etCountPhaze.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            phaze = Integer.parseInt(etPhaze.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SubscriptionPointModel(letName.getText(),
                letDescription.getText(), milins, countPhaze, phaze,
                letNumberEletrometer.getText(), letNumberSubscriptionPoint.getText());
    }
}
