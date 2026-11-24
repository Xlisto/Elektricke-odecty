package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;


/**
 * Provider domovského widgetu č. 1 – Aktuální cena elektřiny.
 */
public class CurrentPriceWidgetProvider extends BaseElektroWidgetProvider {

    @NonNull
    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.CURRENT_PRICE;
    }

}
