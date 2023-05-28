package cz.xlisto.odecty.modules.subscriptionpoint;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cz.xlisto.odecty.models.SubscriptionPointModel;

public class MySpinnerSubscriptionPointAdapter extends ArrayAdapter<SubscriptionPointModel> {
    private final String TAG = "MySpinnerSubscriptionPointAdapter";
    private final int resource;

    private final ArrayList<SubscriptionPointModel> subscriptionPoints;

    public MySpinnerSubscriptionPointAdapter(@NonNull Context context, int resource, @NonNull ArrayList<SubscriptionPointModel> objects) {
        super(context, resource, objects);
        this.resource = resource;
        this.subscriptionPoints = objects;
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
        TextView tv = (TextView) LayoutInflater.from(parent.getContext()).inflate(resource, null);
        tv.setText(subscriptionPoints.get(position).getName());
        return tv;
    }
}
