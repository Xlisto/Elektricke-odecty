package cz.xlisto.elektrodroid.utils;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPSettings;


public class UIHelper {

    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky. Pokud není vybrané odběrné místo, žádné se nezobrazí.
     */
    public static void showButtons(Button btn, FloatingActionButton fab, Context context) {
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(context);
        if (subscriptionPoint != null) {
            ShPSettings shPSettings = new ShPSettings(context);
            if (shPSettings.get(ShPSettings.SHOW_FAB, true)) {
                fab.setVisibility(View.VISIBLE);
                btn.setVisibility(View.GONE);
            } else {
                fab.setVisibility(View.GONE);
                btn.setVisibility(View.VISIBLE);
            }
        } else {
            fab.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
        }
    }


    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky. Pokud není vybrané odběrné místo, žádné se nezobrazí.
     *
     * @param view ViewGroup, který se má posunout, pokud je tlačítko skryté
     */
    public static void showButtons(Button btn, FloatingActionButton fab, Context context, ViewGroup view) {
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(context);
        if (subscriptionPoint != null) {
            ShPSettings shPSettings = new ShPSettings(context);
            if (shPSettings.get(ShPSettings.SHOW_FAB, true)) {
                fab.setVisibility(View.VISIBLE);
                btn.setVisibility(View.GONE);
            } else {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                params.addRule(RelativeLayout.ABOVE, btn.getId());
                view.setLayoutParams(params);
                fab.setVisibility(View.GONE);
                btn.setVisibility(View.VISIBLE);
            }
        } else {
            fab.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
        }
    }

}
