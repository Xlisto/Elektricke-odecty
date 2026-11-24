package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;


/**
 * Provider domovského widgetu č. 2 – Analogové hodiny HDO.
 */
public class HdoClockWidgetProvider extends BaseElektroWidgetProvider {

    @NonNull
    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.HDO_CLOCK;
    }

}
