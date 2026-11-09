package cz.xlisto.elektrodroid.utils;


import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPSettings;


/**
 * Pomocná třída pro správu viditelnosti UI prvků (tlačítka pro přidání v AppBaru a FAB).
 * <p>
 * Obsahuje metody pro zobrazení/skrytí položky menu v AppBaru a FloatingActionButton
 * v závislosti na vybraném odběrném místě a nastavení aplikace.
 */
public class UIHelper {

    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky (FAB nebo položku menu v AppBaru).
     */
    public static void showButtons(Button btn, FloatingActionButton fab, Context context, boolean checkSubscription) {
        showButtons(btn, fab, null, context, null, checkSubscription);
    }


    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky (FAB nebo položku menu v AppBaru).
     */
    public static void showButtons(Button btn, FloatingActionButton fab, MenuItem addMenuItem, Context context, boolean checkSubscription) {
        showButtons(btn, fab, addMenuItem, context, null, checkSubscription);
    }


    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky.
     *
     * @param btn               klasické tlačítko (vždy se skryje)
     * @param fab               FloatingActionButton
     * @param addMenuItem       položka menu v AppBaru
     * @param context           kontext aplikace
     * @param viewGroup         ViewGroup pro případné přizpůsobení layoutu
     * @param checkSubscription zda kontrolovat vybrané odběrné místo
     */
    public static void showButtons(Button btn, FloatingActionButton fab, MenuItem addMenuItem, Context context, ViewGroup viewGroup, boolean checkSubscription) {
        if (btn != null) {
            btn.setVisibility(View.GONE);
        }
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(context);
        if (subscriptionPoint != null || !checkSubscription) {
            ShPSettings shPSettings = new ShPSettings(context);
            int buttonType = shPSettings.getAddButtonType();
            if (buttonType == ShPSettings.ADD_BUTTON_TYPE_FAB) {
                if (fab != null) {
                    fab.setVisibility(View.VISIBLE);
                }
                if (addMenuItem != null) {
                    addMenuItem.setVisible(false);
                }
            } else {
                if (fab != null) {
                    fab.setVisibility(View.GONE);
                }
                if (addMenuItem != null) {
                    addMenuItem.setVisible(true);
                }
            }
        } else {
            if (fab != null) {
                fab.setVisibility(View.GONE);
            }
            if (addMenuItem != null) {
                addMenuItem.setVisible(false);
            }
        }
    }


    /**
     * Zobrazí/skryje tlačítko pro přidání nové položky.
     */
    public static void showButtons(Button btn, FloatingActionButton fab, Context context, ViewGroup viewGroup, boolean checkSubscription) {
        showButtons(btn, fab, null, context, viewGroup, checkSubscription);
    }

}
