package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;


/**
 * Provider domovského widgetu č. 3 – Spotřeba elektřiny.
 */
public class ConsumptionWidgetProvider extends BaseElektroWidgetProvider {

    @NonNull
    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.CONSUMPTION;
    }

}
