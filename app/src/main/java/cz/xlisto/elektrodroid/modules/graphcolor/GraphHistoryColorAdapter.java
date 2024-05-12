package cz.xlisto.elektrodroid.modules.graphcolor;


import static cz.xlisto.elektrodroid.utils.ColorHelper.htmlToColor;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;

/**
 * Adapter historie použitých barev
 * Xlisto 28.10.2023 10:24
 */
public class GraphHistoryColorAdapter extends RecyclerView.Adapter<GraphHistoryColorAdapter.ViewHolder> {
    private static final String TAG = "GraphHistoryColorAdapter";
    private final ArrayList<String> items;
    private final GraphColorDialogFragment graphColorDialogFragment;


    static class ViewHolder extends RecyclerView.ViewHolder {
        View vVT, vNT;
        TextView tvVT, tvNT;
        RelativeLayout rl;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    public GraphHistoryColorAdapter(GraphColorDialogFragment graphColorDialogFragment, ArrayList<String> items) {
        this.items = items;
        this.graphColorDialogFragment = graphColorDialogFragment;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_graph_history_color, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.vVT = view.findViewById(R.id.vVT);
        viewHolder.vNT = view.findViewById(R.id.vNT);
        viewHolder.tvVT = view.findViewById(R.id.tvVTHistoryColor);
        viewHolder.tvNT = view.findViewById(R.id.tvNTHistoryColor);
        viewHolder.rl = view.findViewById(R.id.rlGraphHistoryItem);
        return viewHolder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String[] colors = items.get(position).split(";");
        holder.vVT.setBackgroundColor(htmlToColor(colors[0]));
        holder.vNT.setBackgroundColor(htmlToColor(colors[1]));
        holder.tvVT.setText(colors[0]);
        holder.tvNT.setText(colors[1]);
        holder.rl.setOnClickListener(v ->
                graphColorDialogFragment.setColors(colors)
        );
    }

    @Override
    public int getItemCount() {
        if (items == null) return 0;
        return items.size();
    }
}
