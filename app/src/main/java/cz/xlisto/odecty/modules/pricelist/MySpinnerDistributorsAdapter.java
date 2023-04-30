package cz.xlisto.odecty.modules.pricelist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;

public class MySpinnerDistributorsAdapter extends ArrayAdapter<String> {
    int resource;
    String[] objects;
    int year;

    public MySpinnerDistributorsAdapter(@NonNull Context context, int resource, @NonNull String[] objects, int year) {
        super(context, resource, objects);
        this.resource = resource;
        this.objects = objects;
        this.year = year;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, false, year);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent, true, year);
    }

    public View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent, boolean hideFirst, int year) {
        //od 2021 zobrazovat EG.D; do 2020 zobrazovat E.ON
        //E.ON = 2; EG.D = 3

        View v = LayoutInflater.from(parent.getContext()).inflate(resource, null);
        TextView tv = v.findViewById(R.id.tvSpinnerItem);
        String eon = "E.ON";
        String egd = "EG.D";
        tv.setText(objects[position]);
        if (position == 0 && hideFirst) {
            //tv.setText("");
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
