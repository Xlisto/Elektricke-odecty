package cz.xlisto.cenik.modules.subscriptionpoint;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.cenik.models.SubscriptionPointModel;

public class MySpinnerSubscriptionPointAdapter extends ArrayAdapter<SubscriptionPointModel> {
    private final String TAG = getClass().getName() + " ";
    private int resource;

    private ArrayList<SubscriptionPointModel> subscriptionPoints;

    public MySpinnerSubscriptionPointAdapter(@NonNull Context context, int resource, @NonNull ArrayList<SubscriptionPointModel> objects) {
        super(context, resource, objects);
        this.resource = resource;
        this.subscriptionPoints = objects;
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

    private View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView tv = (TextView) LayoutInflater.from(parent.getContext()).inflate(resource, null);
        tv.setText(subscriptionPoints.get(position).getName());
        return tv;
    }
}
