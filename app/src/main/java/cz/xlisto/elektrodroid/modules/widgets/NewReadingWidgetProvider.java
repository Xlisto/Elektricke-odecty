package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;


/**
 * Provider domovského widgetu č. 5 – Nový odečet.
 */
public class NewReadingWidgetProvider extends BaseElektroWidgetProvider {

    @NonNull
    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.NEW_READING;
    }

}
