package cz.xlisto.elektrodroid.shp;

import android.content.Context;


/**
 * Třída pro ukládání a načítání filtrů ceníků do SharedPreferences.
 * Podporuje tři nezávislé kontexty filtrace: obecný filtr ceníků, filtr pro měsíční odečty a filtr pro faktury.
 */
public class ShPFilter extends ShP {
    public static final String RADA = "filtrRada";
    public static final String PRODUKT = "filtrProdukt";
    public static final String SAZBA = "filtrSazba";
    public static final String COMPANY = "filtrDodavatel";
    public static final String AREA = "filtrUzemi";
    public static final String DATE_START = "filtrDateStart";
    public static final String DATE_END = "filtrDateEnd";
    public static final String DEFAULT = "%";


    /**
     * Typ kontextu filtru ceníků.
     */
    public enum FilterType {
        /**
         * Obecný filtr v sekci Ceníky a při porovnávání ceníků.
         */
        GENERAL(""),
        /**
         * Filtr ceníků při výběru v Měsíčních odečtech.
         */
        MONTHLY_READING("_monthly"),
        /**
         * Filtr ceníků při výběru ve Fakturách.
         */
        INVOICE("_invoice");

        private final String suffix;


        FilterType(String suffix) {
            this.suffix = suffix;
        }


        public String getSuffix() {
            return suffix;
        }
    }


    private final FilterType filterType;


    public ShPFilter(Context context) {
        this(context, FilterType.GENERAL);
    }


    public ShPFilter(Context context, FilterType filterType) {
        this.context = context;
        this.filterType = filterType != null ? filterType : FilterType.GENERAL;
    }


    private String resolveKey(String baseKey) {
        return baseKey + filterType.getSuffix();
    }


    @Override
    public void set(String key, String value) {
        super.set(resolveKey(key), value);
    }


    @Override
    public String get(String key, String defValue) {
        return super.get(resolveKey(key), defValue);
    }


    /**
     * Zjistí, zda je v aktuálním kontextu nastaven alespoň jeden aktivní filtr (odlišný od výchozího "%").
     *
     * @return {@code true}, pokud je některé kritérium filtru aktivní
     */
    public boolean isFilterActive() {
        return isValueFiltered(get(RADA, DEFAULT))
                || isValueFiltered(get(PRODUKT, DEFAULT))
                || isValueFiltered(get(SAZBA, DEFAULT))
                || isValueFiltered(get(COMPANY, DEFAULT))
                || isValueFiltered(get(AREA, DEFAULT))
                || isValueFiltered(get(DATE_START, DEFAULT))
                || isValueFiltered(get(DATE_END, DEFAULT));
    }


    private boolean isValueFiltered(String val) {
        return val != null && !val.trim().isEmpty() && !val.equals(DEFAULT) && !val.equalsIgnoreCase("[VŠE]");
    }
}

