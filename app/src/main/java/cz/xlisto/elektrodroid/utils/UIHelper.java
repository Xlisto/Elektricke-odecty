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
    public static void showButtons(Button btn, FloatingActionButton fab, Context context, boolean checkSubscription) {
        showButtons(btn, fab, context, null, checkSubscription);
    }


    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky. Pokud není vybrané odběrné místo, žádné se nezobrazí.
     *
     * @param viewGroup ViewGroup, který se má posunout, pokud je tlačítko skryté
     */
    public static void showButtons(Button btn, FloatingActionButton fab, Context context, ViewGroup viewGroup, boolean checkSubscription) {
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(context);
        if (subscriptionPoint != null || !checkSubscription) {
            ShPSettings shPSettings = new ShPSettings(context);
            if (shPSettings.get(ShPSettings.SHOW_FAB, true)) {
                fab.setVisibility(View.VISIBLE);
                btn.setVisibility(View.GONE);
            } else {
                if (viewGroup != null) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    params.addRule(RelativeLayout.ABOVE, btn.getId());
                    viewGroup.setLayoutParams(params);
                }
                fab.setVisibility(View.GONE);
                btn.setVisibility(View.VISIBLE);
            }
        } else {
            fab.setVisibility(View.GONE);
            btn.setVisibility(View.GONE);
        }
    }

}
