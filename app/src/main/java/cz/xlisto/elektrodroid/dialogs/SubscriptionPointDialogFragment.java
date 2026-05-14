package cz.xlisto.elektrodroid.dialogs;


import static cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.MainActivityHelper;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Dialog pro výběr aktivního odběrného místa.
 * <p>
 * Načte dostupná odběrná místa z databáze, umožní volbu přes spinner nebo
 * rychlá tlačítka a po výběru přepne aktivní odběrné místo v aplikaci.
 */
public class SubscriptionPointDialogFragment extends DialogFragment {

    public static final String FLAG_UPDATE_SUBSCRIPTION_POINT = "invoiceDialogFragment";
    private final Button[] buttons = new Button[4];


    /**
     * Vytvoří novou instanci dialogu.
     *
     * @return instance fragmentu
     */
    public static SubscriptionPointDialogFragment newInstance() {
        return new SubscriptionPointDialogFragment();
    }


    /**
     * Sestaví dialog a připraví seznam odběrných míst k výběru.
     *
     * @param savedInstanceState uložený stav dialogu, může být {@code null}
     * @return vytvořený dialog
     */
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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
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

    /**
     * Lifecycle callback po zobrazení dialogu.
     * Aplikuje jednotné barvy tlačítek.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }

    /**
     * Lifecycle callback při odpojení fragmentu.
     * Oznámí volajícímu, že může obnovit zobrazená data.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        getParentFragmentManager().setFragmentResult(FLAG_UPDATE_SUBSCRIPTION_POINT, new Bundle());
    }


    /**
     * Uloží zvolené odběrné místo do preference a obnoví hlavní obrazovku.
     *
     * @param subscriptionPoint vybrané odběrné místo
     */
    private void setSubscriptionPoint(SubscriptionPointModel subscriptionPoint) {
        ShPSubscriptionPoint shp = new ShPSubscriptionPoint(requireContext());
        shp.set(ID_SUBSCRIPTION_POINT_LONG, subscriptionPoint.getId());

        MainActivityHelper.updateToolbarAndLoadData(requireActivity());
    }


    /**
     * Adapter pro zobrazení odběrných míst ve spinneru.
     */
    static class Adapter extends ArrayAdapter<SubscriptionPointModel> {

        /**
         * Vytvoří adapter dat odběrných míst.
         *
         * @param context            kontext pro inflaci layoutů
         * @param subscriptionPoints data odběrných míst
         */
        public Adapter(Context context, ArrayList<SubscriptionPointModel> subscriptionPoints) {
            super(context, android.R.layout.simple_spinner_item, subscriptionPoints);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }


        /**
         * Vrací view pro aktuálně zvolenou položku spinneru.
         */
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }


        /**
         * Vrací view pro položku v rozbaleném seznamu spinneru.
         */
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }


        /**
         * Vytvoří nebo zrecykluje view položky a naplní ho daty odběrného místa.
         *
         * @param position    pozice položky
         * @param convertView recyklovaný view, může být {@code null}
         * @param parent      rodičovský kontejner
         * @return naplněné view položky
         */
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
