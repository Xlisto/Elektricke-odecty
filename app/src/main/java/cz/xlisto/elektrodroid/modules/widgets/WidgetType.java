package cz.xlisto.elektrodroid.modules.widgets;


import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import cz.xlisto.elektrodroid.R;


/**
 * Výčet všech podporovaných typů widgetů domovské obrazovky aplikace ElektroDroid.
 * <p>
 * Každý typ nese stabilní textový klíč pro uložení konfigurace, odkaz na příslušnou třídu
 * {@link AppWidgetProvider}, název pro konfigurační obrazovku a informaci o tom, zda vyžaduje
 * volby zobrazení ceny (celková/obchodní a DPH).
 */
public enum WidgetType {

    /**
     * Widget zobrazující aktuální cenu elektřiny (VT/NT), aktivní sazbu a čas dalšího přepnutí HDO.
     */
    CURRENT_PRICE(
            "current_price",
            CurrentPriceWidgetProvider.class,
            R.string.widget_current_price_name,
            R.string.widget_current_price_description,
            true
    ),

    /**
     * Widget s analogovými 24hodinovými hodinami zobrazujícími dnešní intervaly HDO.
     */
    HDO_CLOCK(
            "hdo_clock",
            HdoClockWidgetProvider.class,
            R.string.widget_hdo_clock_name,
            R.string.widget_hdo_clock_description,
            false
    ),

    /**
     * Widget zobrazující spotřebu elektřiny (VT, NT, celkem) za sledované období.
     */
    CONSUMPTION(
            "consumption",
            ConsumptionWidgetProvider.class,
            R.string.widget_consumption_name,
            R.string.widget_consumption_description,
            false
    ),

    /**
     * Widget zobrazující finanční bilanci (zálohy, náklady a předpokládaný přeplatek/nedoplatek).
     */
    FINANCIAL_BALANCE(
            "financial_balance",
            FinancialBalanceWidgetProvider.class,
            R.string.widget_financial_balance_name,
            R.string.widget_financial_balance_description,
            false
    ),

    /**
     * Kompaktní akční widget pro rychlé přidání nového odečtu pro konkrétní odběrné místo.
     */
    NEW_READING(
            "new_reading",
            NewReadingWidgetProvider.class,
            R.string.widget_new_reading_name,
            R.string.widget_new_reading_description,
            false
    );

    @NonNull
    private final String key;
    @NonNull
    private final Class<? extends AppWidgetProvider> providerClass;
    @StringRes
    private final int labelResId;
    @StringRes
    private final int descriptionResId;
    private final boolean supportsPriceOptions;


    WidgetType(@NonNull String key,
               @NonNull Class<? extends AppWidgetProvider> providerClass,
               @StringRes int labelResId,
               @StringRes int descriptionResId,
               boolean supportsPriceOptions) {
        this.key = key;
        this.providerClass = providerClass;
        this.labelResId = labelResId;
        this.descriptionResId = descriptionResId;
        this.supportsPriceOptions = supportsPriceOptions;
    }


    /**
     * Vrátí stabilní textový klíč typu widgetu pro perzistenci.
     *
     * @return stabilní klíč
     */
    @NonNull
    public String getKey() {
        return key;
    }


    /**
     * Vrátí třídu {@link AppWidgetProvider} obsluhující tento typ widgetu.
     *
     * @return třída provideru widgetu
     */
    @NonNull
    public Class<? extends AppWidgetProvider> getProviderClass() {
        return providerClass;
    }


    /**
     * Vrátí ID textového zdroje s názvem widgetu.
     *
     * @return ID stringového zdroje
     */
    @StringRes
    public int getLabelResId() {
        return labelResId;
    }


    /**
     * Vrátí ID textového zdroje se stručným popisem widgetu.
     *
     * @return ID stringového zdroje
     */
    @StringRes
    public int getDescriptionResId() {
        return descriptionResId;
    }


    /**
     * Určuje, zda widget podporuje nastavení typu ceny (celková/obchodní) a započítání DPH.
     *
     * @return {@code true} pro widget aktuální ceny, jinak {@code false}
     */
    public boolean supportsPriceOptions() {
        return supportsPriceOptions;
    }


    /**
     * Najde typ widgetu podle jeho uloženého textového klíče.
     *
     * @param key uložený textový klíč
     * @return odpovídající {@link WidgetType}, nebo {@code null}, pokud klíč není známý
     */
    @Nullable
    public static WidgetType fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        for (WidgetType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }


    /**
     * Najde typ widgetu podle jména komponenty provideru ze systémového {@code AppWidgetProviderInfo}.
     *
     * @param provider komponenta provideru widgetu
     * @return odpovídající {@link WidgetType}, nebo {@code null}, pokud komponenta neodpovídá žádnému widgetu
     */
    @Nullable
    public static WidgetType fromComponentName(@Nullable ComponentName provider) {
        if (provider == null) {
            return null;
        }
        String className = provider.getClassName();
        for (WidgetType type : values()) {
            if (type.providerClass.getName().equals(className)) {
                return type;
            }
        }
        return null;
    }

}
