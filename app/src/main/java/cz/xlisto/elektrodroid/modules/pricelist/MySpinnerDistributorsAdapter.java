package cz.xlisto.elektrodroid.modules.pricelist;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.R;


/**
 * Adaptér pro zobrazení položek v Spinneru distributorů a distribučních sazeb.
 * <p>
 * Umožňuje skrýt první titulní položku v rozbalovacím seznamu a podle zadaného roku platnosti
 * dynamicky skrývat nebo zobrazovat položky "E.ON" a "EG.D" bez vytváření prázdných mezer v nabídce.
 */
public class MySpinnerDistributorsAdapter extends ArrayAdapter<String> {

    final int resource;
    final String[] objects;
    final int year;


    /**
     * Vytvoří nový adaptér pro Spinner.
     *
     * @param context  Kontext aplikace
     * @param resource Identifikátor rozvržení položky Spinneru
     * @param objects  Pole řetězců (názvy distribučních území nebo sazeb)
     * @param year     Rok platnosti ceníku určený pro filtrování viditelnosti distributorů
     */
    public MySpinnerDistributorsAdapter(@NonNull Context context, int resource, @NonNull String[] objects, int year) {
        super(context, resource, objects);
        this.resource = resource;
        this.objects = objects;
        this.year = year;
    }


    /**
     * Vrátí zobrazení aktuálně vybrané položky ve sbaleném stavu Spinneru.
     *
     * @param position    Pozice položky
     * @param convertView Znovupoužitelné zobrazení položky
     * @param parent      Rodičovské zobrazení {@link ViewGroup}
     * @return Zobrazení {@link View} pro vybranou položku
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, false, year);
    }


    /**
     * Vrátí zobrazení položky v rozbalovacím seznamu (drop-down) Spinneru.
     *
     * @param position    Pozice položky v seznamu
     * @param convertView Znovupoužitelné zobrazení položky
     * @param parent      Rodičovské zobrazení {@link ViewGroup}
     * @return Zobrazení {@link View} pro položku v rozbalovacím seznamu
     */
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, true, year);
    }


    /**
     * Vytvoří a nakonfiguruje zobrazení pro položku Spinneru.
     * <p>
     * Pravidla viditelnosti:
     * - Pokud je {@code hideFirst} nastaveno na {@code true} a pozice je 0, první titulní položka se skryje a její rozměry se sbalí.
     * - Pokud je rok 2021 a vyšší, položka "E.ON" se skryje a sbalí.
     * - Pokud je rok 2020 a nižší, položka "EG.D" se skryje a sbalí.
     *
     * @param position    Pozice položky v pole {@code objects}
     * @param convertView Znovupoužitelné zobrazení položky
     * @param parent      Rodičovské zobrazení {@link ViewGroup}
     * @param hideFirst   Pokud je {@code true}, skryje titulní položku na pozici 0 v rozbalovací nabídce
     * @param year        Rok platnosti ceníku
     * @return Pripravené zobrazení {@link View} pro danou pozici
     */
    public View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent, boolean hideFirst, int year) {
        View v = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);
        TextView tv = v.findViewById(R.id.tvSpinnerItem);
        String item = objects[position];
        tv.setText(item);

        boolean isHidden = (position == 0 && hideFirst)
                || (year >= 2021 && "E.ON".equals(item))
                || (year < 2021 && "EG.D".equals(item));

        if (isHidden) {
            v.setVisibility(View.GONE);
            v.setLayoutParams(new AbsListView.LayoutParams(1, 1));
        } else {
            v.setVisibility(View.VISIBLE);
        }

        return v;
    }

}
