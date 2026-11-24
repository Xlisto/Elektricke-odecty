package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;


/**
 * Provider domovského widgetu č. 4 – Finanční bilance.
 */
public class FinancialBalanceWidgetProvider extends BaseElektroWidgetProvider {

    @NonNull
    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FINANCIAL_BALANCE;
    }

}
