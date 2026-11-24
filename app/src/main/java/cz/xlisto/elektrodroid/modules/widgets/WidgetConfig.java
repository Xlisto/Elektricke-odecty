package cz.xlisto.elektrodroid.modules.widgets;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;


/**
 * Neměnná konfigurace jedné konkrétní instance widgetu na domovské obrazovce,
 * jednoznačně identifikované pomocí {@code appWidgetId}.
 * <p>
 * Kromě databázového ID odběrného místa ukládá také stabilní časovou značku {@code milins}
 * z {@link cz.xlisto.elektrodroid.models.SubscriptionPointModel}, díky níž lze po obnově databáze
 * ze zálohy bezpečně ověřit totožnost odběrného místa a zabránit tichému přiřazení k jinému místu.
 */
public final class WidgetConfig {

    private static final String JSON_WIDGET_TYPE = "widgetType";
    private static final String JSON_SUBSCRIPTION_POINT_ID = "subscriptionPointId";
    private static final String JSON_SUBSCRIPTION_POINT_MILINS = "subscriptionPointMilins";
    private static final String JSON_POINT_NAME_SNAPSHOT = "pointNameSnapshot";
    private static final String JSON_SHOW_TOTAL_PRICE = "showTotalPrice";
    private static final String JSON_INCLUDE_VAT = "includeVat";

    private final int appWidgetId;
    @NonNull
    private final WidgetType widgetType;
    private final long subscriptionPointId;
    private final long subscriptionPointMilins;
    @NonNull
    private final String subscriptionPointNameSnapshot;
    private final boolean showTotalPrice;
    private final boolean includeVat;


    /**
     * Vytvoří konfiguraci pro jednu instanci widgetu.
     *
     * @param appWidgetId                   systémové ID instance widgetu
     * @param widgetType                    typ widgetu
     * @param subscriptionPointId           databázové ID odběrného místa ({@code _id})
     * @param subscriptionPointMilins       stabilní identifikátor tabulek odběrného místa ({@code milins})
     * @param subscriptionPointNameSnapshot poslední známý název odběrného místa (pouze orientační)
     * @param showTotalPrice                {@code true} pro celkovou cenu, {@code false} pro obchodní cenu
     * @param includeVat                    {@code true} pro zobrazení ceny s DPH, {@code false} bez DPH
     */
    public WidgetConfig(int appWidgetId,
                        @NonNull WidgetType widgetType,
                        long subscriptionPointId,
                        long subscriptionPointMilins,
                        @Nullable String subscriptionPointNameSnapshot,
                        boolean showTotalPrice,
                        boolean includeVat) {
        this.appWidgetId = appWidgetId;
        this.widgetType = Objects.requireNonNull(widgetType, "widgetType nesmí být null");
        this.subscriptionPointId = subscriptionPointId;
        this.subscriptionPointMilins = subscriptionPointMilins;
        this.subscriptionPointNameSnapshot = subscriptionPointNameSnapshot != null ? subscriptionPointNameSnapshot : "";
        this.showTotalPrice = showTotalPrice;
        this.includeVat = includeVat;
    }


    public int getAppWidgetId() {
        return appWidgetId;
    }


    @NonNull
    public WidgetType getWidgetType() {
        return widgetType;
    }


    public long getSubscriptionPointId() {
        return subscriptionPointId;
    }


    public long getSubscriptionPointMilins() {
        return subscriptionPointMilins;
    }


    @NonNull
    public String getSubscriptionPointNameSnapshot() {
        return subscriptionPointNameSnapshot;
    }


    public boolean isShowTotalPrice() {
        return showTotalPrice;
    }


    public boolean isIncludeVat() {
        return includeVat;
    }


    /**
     * Vytvoří kopii konfigurace přiřazenou novému {@code appWidgetId} (např. při systémovém přemapování v {@code onRestored}).
     *
     * @param newAppWidgetId nové systémové ID widgetu
     * @return nová instance {@link WidgetConfig} se zachovaným nastavením
     */
    @NonNull
    public WidgetConfig withAppWidgetId(int newAppWidgetId) {
        return new WidgetConfig(
                newAppWidgetId,
                widgetType,
                subscriptionPointId,
                subscriptionPointMilins,
                subscriptionPointNameSnapshot,
                showTotalPrice,
                includeVat
        );
    }


    /**
     * Vytvoří kopii konfigurace s aktualizovaným ID a názvem odběrného místa
     * (např. po obnově databáze, kdy se shoduje stabilní {@code milins}, ale změnilo se {@code _id}).
     *
     * @param newPointId   aktuální databázové {@code _id} odběrného místa
     * @param pointMilins  stabilní {@code milins} odběrného místa
     * @param newPointName aktuální název odběrného místa
     * @return aktualizovaná instance {@link WidgetConfig}
     */
    @NonNull
    public WidgetConfig withResolvedSubscriptionPoint(long newPointId, long pointMilins, @Nullable String newPointName) {
        return new WidgetConfig(
                appWidgetId,
                widgetType,
                newPointId,
                pointMilins,
                newPointName,
                showTotalPrice,
                includeVat
        );
    }


    /**
     * Serializuje konfiguraci widgetu do JSON řetězce pro bezpečné uložení v {@link android.content.SharedPreferences}.
     *
     * @return JSON reprezentace konfigurace
     */
    @NonNull
    public String toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put(JSON_WIDGET_TYPE, widgetType.getKey());
            obj.put(JSON_SUBSCRIPTION_POINT_ID, subscriptionPointId);
            obj.put(JSON_SUBSCRIPTION_POINT_MILINS, subscriptionPointMilins);
            obj.put(JSON_POINT_NAME_SNAPSHOT, subscriptionPointNameSnapshot);
            obj.put(JSON_SHOW_TOTAL_PRICE, showTotalPrice);
            obj.put(JSON_INCLUDE_VAT, includeVat);
        } catch (JSONException e) {
            throw new IllegalStateException("Nepodařilo se serializovat konfiguraci widgetu.", e);
        }
        return obj.toString();
    }


    /**
     * Deserializuje konfiguraci widgetu z uloženého JSON řetězce.
     *
     * @param appWidgetId systémové ID widgetu, pod kterým byl JSON uložen
     * @param json        uložený JSON řetězec
     * @return načtená instance {@link WidgetConfig}, nebo {@code null} při poškozených či neplatných datech
     */
    @Nullable
    public static WidgetConfig fromJson(int appWidgetId, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(json);
            WidgetType type = WidgetType.fromKey(obj.optString(JSON_WIDGET_TYPE, ""));
            if (type == null) {
                return null;
            }
            long pointId = obj.optLong(JSON_SUBSCRIPTION_POINT_ID, -1L);
            long pointMilins = obj.optLong(JSON_SUBSCRIPTION_POINT_MILINS, -1L);
            String nameSnapshot = obj.optString(JSON_POINT_NAME_SNAPSHOT, "");
            boolean showTotal = obj.optBoolean(JSON_SHOW_TOTAL_PRICE, true);
            boolean includeVat = obj.optBoolean(JSON_INCLUDE_VAT, true);
            return new WidgetConfig(appWidgetId, type, pointId, pointMilins, nameSnapshot, showTotal, includeVat);
        } catch (JSONException e) {
            return null;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WidgetConfig that)) return false;
        return appWidgetId == that.appWidgetId
                && subscriptionPointId == that.subscriptionPointId
                && subscriptionPointMilins == that.subscriptionPointMilins
                && showTotalPrice == that.showTotalPrice
                && includeVat == that.includeVat
                && widgetType == that.widgetType
                && subscriptionPointNameSnapshot.equals(that.subscriptionPointNameSnapshot);
    }


    @Override
    public int hashCode() {
        return Objects.hash(
                appWidgetId,
                widgetType,
                subscriptionPointId,
                subscriptionPointMilins,
                subscriptionPointNameSnapshot,
                showTotalPrice,
                includeVat
        );
    }

}
