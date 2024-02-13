package cz.xlisto.odecty.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;
import cz.xlisto.odecty.utils.MainActivityHelper;
import cz.xlisto.odecty.utils.SubscriptionPoint;

import static cz.xlisto.odecty.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT;


/**
 * Xlisto 12.02.2024 19:17
 */
public class SubscriptionPointDialogFragment extends DialogFragment {
    private static final String TAG = "InvoiceDialogFragment";
    public static final String FLAG_UPDATE_SUBSCRIPTION_POINT = "invoiceDialogFragment";
    private final Button[] buttons = new Button[4];


    public static SubscriptionPointDialogFragment newInstance() {
        return new SubscriptionPointDialogFragment();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
        dataSubscriptionPointSource.open();
        ArrayList<SubscriptionPointModel> subscriptionPoints = dataSubscriptionPointSource.loadSubscriptionPoints();

        dataSubscriptionPointSource.close();

        SubscriptionPointModel selectedSubscription = SubscriptionPoint.load(requireContext());

        View view = View.inflate(requireContext(), R.layout.dialog_select_subscription_point, null);
        Spinner spinner = view.findViewById(R.id.spSubscriptionPoints);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getResources().getString(R.string.selecting_subscription_point));

        if (subscriptionPoints.size() > 4) {
            builder.setPositiveButton(getResources().getString(R.string.ok), null);
        } else
            spinner.setVisibility(View.GONE);


        spinner.setAdapter(new Adapter(requireContext(), subscriptionPoints));
        builder.setView(view);

        if (selectedSubscription != null) {
            for (int i = 0; i < subscriptionPoints.size(); i++) {
                if (subscriptionPoints.get(i).getId() == selectedSubscription.getId()) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSubscriptionPoint(subscriptionPoints.get(position));
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        buttons[0] = view.findViewById(R.id.btn1);
        buttons[1] = view.findViewById(R.id.btn2);
        buttons[2] = view.findViewById(R.id.btn3);
        buttons[3] = view.findViewById(R.id.btn4);

        for (int i = 0; i < 4; i++) {
            int index = i;
            if (i < subscriptionPoints.size()) {
                buttons[i].setText(subscriptionPoints.get(i).getName());
                buttons[i].setVisibility(View.VISIBLE);
                buttons[i].setOnClickListener(v -> {
                    //spinner.setSelection(index);
                    setSubscriptionPoint(subscriptionPoints.get(index));
                    dismiss();
                });
            } else {
                buttons[i].setVisibility(View.GONE);
            }
        }

        return builder.create();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SUBSCRIPTION_POINT, new Bundle());
    }


    private void setSubscriptionPoint(SubscriptionPointModel subscriptionPoint) {
        ShPSubscriptionPoint shp = new ShPSubscriptionPoint(requireContext());
        shp.set(ID_SUBSCRIPTION_POINT, subscriptionPoint.getId());

        MainActivityHelper.updateToolbarAndLoadData(requireActivity());
    }


    static class Adapter extends ArrayAdapter<SubscriptionPointModel> {
        public Adapter(Context context, ArrayList<SubscriptionPointModel> subscriptionPoints) {
            super(context, android.R.layout.simple_spinner_item, subscriptionPoints);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }


        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }


        private View getCustomView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }
            TextView textView = convertView.findViewById(android.R.id.text1);
            SubscriptionPointModel subscriptionPointModel = getItem(position);
            if (subscriptionPointModel == null) return convertView;
            textView.setText(subscriptionPointModel.getName());
            return convertView;
        }
    }
}
