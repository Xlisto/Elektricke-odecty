package cz.xlisto.elektrodroid.modules.invoice;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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


    /**
     * Konstruktor pro vytvoření instance MySpinnerInvoiceDetailAdapter.
     *
     * @param context      Kontext aplikace.
     * @param resource     ID rozložení pro jednotlivé položky.
     * @param objects      Pole názvů detailů faktury.
     * @param datesInvoice Kontejner pro minimální a maximální datum faktury.
     */
    public MySpinnerInvoiceDetailAdapter(@NonNull Context context, int resource, SummaryInvoiceModel.Title[] objects, DatesInvoiceContainer datesInvoice) {
        super(context, resource, objects);
        this.titles = objects;
        this.datesInvoice = datesInvoice;
    }


    /**
     * Vrací vlastní zobrazení pro zadanou pozici ve spinneru.
     *
     * @param position    Pozice položky ve spinneru.
     * @param convertView Recyklovaný pohled.
     * @param parent      Rodičovská ViewGroup.
     * @return Vlastní zobrazení pro zadanou pozici.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }


    /**
     * Vrací vlastní zobrazení pro zadanou pozici v rozbalovacím seznamu spinneru.
     *
     * @param position    Pozice položky v rozbalovacím seznamu.
     * @param convertView Recyklovaný pohled.
     * @param parent      Rodičovská ViewGroup.
     * @return Vlastní zobrazení pro zadanou pozici.
     */
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }


    /**
     * Vrací vlastní zobrazení pro zadanou pozici.
     *
     * @param position Pozice položky.
     * @param parent   Rodičovská ViewGroup.
     * @return Vlastní zobrazení pro zadanou pozici.
     */
    private View getCustomView(int position, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.spinner_invoice_detail, parent, false);
        TextView tvNumberInvoice = view.findViewById(R.id.tvInvoiceDetailName);
        tvNumberInvoice.setText(titles[position].toString());

        //skrytí položek NT
        if (hideNt) {
            if (position == 1 || position == 5) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                view = tv;
                if (position == 1) {
                    setAdapterViewSelection(parent, 0);
                } else {
                    setAdapterViewSelection(parent, 4);
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
                setAdapterViewSelection(parent, 8);
            }
        }

        //skrytí položky cena za Činnost operátora trhu, pokud je datum faktury větší než 1.7.2024
        if (datesInvoice.getMinDate() >= ViewHelper.parseCalendarFromString("1.07.2024").getTimeInMillis()) {
            if (position == 8) {
                TextView tv = new TextView(getContext());
                tv.setVisibility(View.GONE);
                tv.setHeight(0);
                view = tv;
                setAdapterViewSelection(parent, 9);
            }
        }

        return view;
    }


    /**
     * Nastaví výběr AdapterView na zadanou pozici.
     *
     * @param parent   Rodičovská ViewGroup, která by měla být instancí AdapterView.
     * @param position Pozice, na kterou se má výběr nastavit.
     */
    private void setAdapterViewSelection(ViewGroup parent, int position) {
        if (parent instanceof AdapterView) {
            AdapterView<?> adapterView = (AdapterView<?>) parent;
            adapterView.setSelection(position);
        }
    }


    /**
     * Vrací položku na zadané pozici.
     *
     * @param position Pozice položky.
     * @return Položka na zadané pozici.
     */
    @Nullable
    @Override
    public SummaryInvoiceModel.Title getItem(int position) {
        return super.getItem(position);
    }


    /**
     * Nastaví, zda se mají skrýt položky NT.
     *
     * @param hideNt True, pokud se mají skrýt položky NT, jinak false.
     */
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


