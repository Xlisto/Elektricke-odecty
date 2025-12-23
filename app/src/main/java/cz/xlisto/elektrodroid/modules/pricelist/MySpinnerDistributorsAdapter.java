package cz.xlisto.elektrodroid.modules.pricelist;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;


/**
 * Adapter pro zobrazení položek v Spinneru distributorů.
 * Umožňuje skrýt první položku v rozbalovacím seznamu a podle roku
 * přepínat viditelnost položek "E.ON" a "EG.D".
 */
public class MySpinnerDistributorsAdapter extends ArrayAdapter<String> {

    int resource;
    String[] objects;
    int year;


    /**
     * Vytvoří nový adapter pro Spinner.
     *
     * @param context  kontext aplikace
     * @param resource layout resource pro položku Spinneru
     * @param objects  pole řetězců, které budou zobrazeny
     * @param year     rok použitý pro rozhodnutí o viditelnosti distributorů
     */
    public MySpinnerDistributorsAdapter(@NonNull Context context, int resource, @NonNull String[] objects, int year) {
        super(context, resource, objects);
        this.resource = resource;
        this.objects = objects;
        this.year = year;
    }


    /**
     * Vrací zobrazení aktuálně vybrané položky (krátká forma Spinneru).
     * Deleguje tvorbu na {@link #getCustomView(int, View, ViewGroup, boolean, int)}.
     *
     * @param position    pozice položky
     * @param convertView případné znovupoužitelné View
     * @param parent      rodičovský ViewGroup
     * @return View použité pro zobrazení vybrané položky
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, false, year);
    }


    /**
     * Vrací rozbalovací zobrazení položky (drop-down).
     * Deleguje tvorbu na {@link #getCustomView(int, View, ViewGroup, boolean, int)}.
     *
     * @param position    pozice položky v seznamu
     * @param convertView případné znovupoužitelné View
     * @param parent      rodičovský ViewGroup
     * @return View použité v rozbalovacím seznamu
     */
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, true, year);
    }


    /**
     * Vytvoří a nakonfiguruje View pro položku Spinneru.
     * - Pokud je parametr {@code hideFirst} true a pozice je 0, první položka se skryje.
     * - Podle parametru {@code year} se přepíná viditelnost textů "E.ON" a "EG.D":
     * od roku 2021 se zobrazuje "EG.D" a skrývá "E.ON", do roku 2020 opačně.
     *
     * @param position    pozice položky
     * @param convertView případné znovupoužitelné View (nevyužívá se zde re-use)
     * @param parent      rodičovský ViewGroup
     * @param hideFirst   pokud true, první položka (index 0) bude skryta v dropdownu
     * @param year        rok použitý pro rozhodnutí o viditelnosti distributorů
     * @return připravené View pro danou pozici
     */
    public View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent, boolean hideFirst, int year) {
        //od 2021 zobrazovat EG.D; do 2020 zobrazovat E.ON
        //E.ON = 2; EG.D = 3

        View v = LayoutInflater.from(parent.getContext()).inflate(resource, null);
        TextView tv = v.findViewById(R.id.tvSpinnerItem);
        String eon = "E.ON";
        String egd = "EG.D";
        tv.setText(objects[position]);
        if (position == 0 && hideFirst) {
            tv.setVisibility(View.GONE);
        }

        if (year >= 2021) {
            if (objects[position].equals(eon))
                tv.setVisibility(View.GONE);
            if (objects[position].equals(egd))
                tv.setVisibility(View.VISIBLE);
        } else {
            if (objects[position].equals(egd))
                tv.setVisibility(View.GONE);
            if (objects[position].equals(eon))
                tv.setVisibility(View.VISIBLE);
        }

        return v;
    }

}
