package cz.xlisto.odecty.shp;

import android.content.Context;


/**
 * Pomocná třída pro ukládání pozice stavu zobrazení detailu ceníku v land režimu
 * Xlisto 26.04.2023 20:31
 */
public class ShPPriceList extends ShP {
    public static final String SHOW_BUTTONS_PRICE_LIST = "showButtonsPriceList";
    public static final String SHOW_ID_ITEM_PRICE_LIST = "showIdItemPriceList";


    public ShPPriceList(Context context) {
        this.context = context;
    }
}
