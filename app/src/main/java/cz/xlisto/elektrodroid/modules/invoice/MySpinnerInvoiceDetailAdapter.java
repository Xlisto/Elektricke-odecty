package cz.xlisto.elektrodroid.modules.invoice;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SummaryInvoiceModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Adaptér pro zobrazení názvů detailů faktury ve spinneru.
 * Xlisto 21.03.2023 21:04
 */
public class MySpinnerInvoiceDetailAdapter extends ArrayAdapter<SummaryInvoiceModel.Title> {

    private static final String TAG = "MySpinnerInvoiceDetailAdapter";
    private final SummaryInvoiceModel.Title[] titles;
    private Boolean hideNt;
    private final DatesInvoiceContainer datesInvoice;


    public MySpinnerInvoiceDetailAdapter(@NonNull Context context, int resource, SummaryInvoiceModel.Title[] objects, DatesInvoiceContainer datesInvoice) {
        super(context, resource, objects);
        this.titles = objects;
        this.datesInvoice = datesInvoice;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }


    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }


    private View getCustomView(int position, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.spinner_invoice_detail, parent, false);
        TextView tvNumberInvoice = view.findViewById(R.id.tvInvoiceDetailName);
        tvNumberInvoice.setText(titles[position].toString());
        Spinner spinner = (Spinner) parent;

        //skrytí položek NT
        if (hideNt) {
            if (position == 1 || position == 5) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                view = tv;
                if(position == 1) {
                    spinner.setSelection(0);
                } else {
                    spinner.setSelection(4);
                }
            }
        }

        //skrytí položky Provoz nesíťové infrastruktory, pokud je datum faktury menší než 1.7.2024
        if (datesInvoice.getMaxDate() < ViewHelper.parseCalendarFromString("1.07.2024").getTimeInMillis()) {
            if (position == 9) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                view = tv;
                spinner.setSelection(8);
            }
        }

        //skrytí položky cena za Činnost operátora trhu, pokud je datum faktury větší než 1.7.2024
        if (datesInvoice.getMinDate() >= ViewHelper.parseCalendarFromString("1.07.2024").getTimeInMillis()) {
            if (position == 8) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                view = tv;
                spinner.setSelection(9);
            }
        }

        return view;
    }


    @Nullable
    @Override
    public SummaryInvoiceModel.Title getItem(int position) {
        return super.getItem(position);
    }


    public void setHideNt(boolean hideNt) {
        this.hideNt = hideNt;
    }


    /**
     * Třída pro uchování minimálního a maximálního data faktury.
     */
    public static class DatesInvoiceContainer {

        private final long minDate;
        private final long maxDate;


        /**
         * Konstruktor pro vytvoření instance dateInvoiceContainer.
         *
         * @param minDate minimální datum faktury
         * @param maxDate maximální datum faktury
         */
        public DatesInvoiceContainer(long minDate, long maxDate) {
            this.minDate = minDate;
            this.maxDate = maxDate;
        }


        /**
         * Vrátí minimální datum faktury.
         *
         * @return minimální datum faktury
         */
        public long getMinDate() {
            return minDate;
        }


        /**
         * Vrátí maximální datum faktury.
         *
         * @return maximální datum faktury
         */
        public long getMaxDate() {
            return maxDate;
        }

    }

}


