package cz.xlisto.elektrodroid.modules.widgets;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Jednotkové testy pro {@link WidgetConfigStore} a {@link WidgetConfig}.
 * Ověřují plnou nezávislost více instancí téhož widgetu (Domov, Chata, Garáž),
 * selektivní mazání, přemapování ID při systémové obnově i úklid osiřelých konfigurací.
 */
public class WidgetConfigStoreTest {

    private static final class InMemoryStorage implements WidgetConfigStore.KeyValueStorage {

        private final Map<String, String> map = new HashMap<>();


        @Nullable
        @Override
        public String getString(@NonNull String key) {
            return map.get(key);
        }


        @Override
        public void putString(@NonNull String key, @NonNull String value) {
            map.put(key, value);
        }


        @Override
        public void remove(@NonNull String key) {
            map.remove(key);
        }


        @NonNull
        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(map);
        }

    }


    @Test
    public void threeInstancesOfSameWidgetMaintainIndependentConfigurations() {
        WidgetConfigStore store = new WidgetConfigStore(new InMemoryStorage());

        WidgetConfig domov = new WidgetConfig(101, WidgetType.CURRENT_PRICE, 1L, 1700000001L, "Domov", true, true);
        WidgetConfig chata = new WidgetConfig(102, WidgetType.CURRENT_PRICE, 2L, 1700000002L, "Chata", false, true);
        WidgetConfig garaz = new WidgetConfig(103, WidgetType.CURRENT_PRICE, 3L, 1700000003L, "Garáž", true, false);

        store.saveConfig(domov);
        store.saveConfig(chata);
        store.saveConfig(garaz);

        assertEquals(domov, store.loadConfig(101));
        assertEquals(chata, store.loadConfig(102));
        assertEquals(garaz, store.loadConfig(103));
        assertEquals(3, store.loadConfigsByType(WidgetType.CURRENT_PRICE).size());

        // Úprava konfigurace Chaty nesmí ovlivnit Domov ani Garáž
        WidgetConfig chataUpdated = new WidgetConfig(102, WidgetType.CURRENT_PRICE, 2L, 1700000002L, "Chata", true, false);
        store.saveConfig(chataUpdated);

        assertEquals(domov, store.loadConfig(101));
        assertEquals(chataUpdated, store.loadConfig(102));
        assertEquals(garaz, store.loadConfig(103));
        assertTrue(store.loadConfig(101).includeVat());
        assertFalse(store.loadConfig(102).includeVat());
        assertFalse(store.loadConfig(103).includeVat());
    }


    @Test
    public void deletingOneInstancePreservesAllOtherInstances() {
        WidgetConfigStore store = new WidgetConfigStore(new InMemoryStorage());

        WidgetConfig w1 = new WidgetConfig(11, WidgetType.CONSUMPTION, 1L, 1001L, "Domov", true, true);
        WidgetConfig w2 = new WidgetConfig(12, WidgetType.CONSUMPTION, 2L, 1002L, "Chata", true, true);
        WidgetConfig w3 = new WidgetConfig(13, WidgetType.NEW_READING, 1L, 1001L, "Domov", true, true);

        store.saveConfig(w1);
        store.saveConfig(w2);
        store.saveConfig(w3);

        store.deleteConfig(12);

        assertEquals(w1, store.loadConfig(11));
        assertNull(store.loadConfig(12));
        assertEquals(w3, store.loadConfig(13));
    }


    @Test
    public void remapWidgetIdsHandlesOverlappingIdsWithoutDataLoss() {
        WidgetConfigStore store = new WidgetConfigStore(new InMemoryStorage());

        WidgetConfig old10 = new WidgetConfig(10, WidgetType.CURRENT_PRICE, 1L, 1001L, "Domov", true, true);
        WidgetConfig old20 = new WidgetConfig(20, WidgetType.CURRENT_PRICE, 2L, 1002L, "Chata", false, false);
        store.saveConfig(old10);
        store.saveConfig(old20);

        // Překrývající se přemapování: 10 -> 20, 20 -> 30
        store.remapWidgetIds(new int[]{10, 20}, new int[]{20, 30});

        assertNull(store.loadConfig(10));
        WidgetConfig new20 = store.loadConfig(20);
        WidgetConfig new30 = store.loadConfig(30);
        assertNotNull(new20);
        assertNotNull(new30);

        assertEquals(1L, new20.subscriptionPointId());
        assertEquals("Domov", new20.subscriptionPointNameSnapshot());
        assertTrue(new20.showTotalPrice());

        assertEquals(2L, new30.subscriptionPointId());
        assertEquals("Chata", new30.subscriptionPointNameSnapshot());
        assertFalse(new30.showTotalPrice());
    }


    @Test
    public void pruneOrphanedConfigsRemovesOnlyInactiveWidgets() {
        WidgetConfigStore store = new WidgetConfigStore(new InMemoryStorage());

        store.saveConfig(new WidgetConfig(1, WidgetType.HDO_CLOCK, 1L, 1001L, "Domov", true, true));
        store.saveConfig(new WidgetConfig(2, WidgetType.FINANCIAL_BALANCE, 2L, 1002L, "Chata", true, true));
        store.saveConfig(new WidgetConfig(3, WidgetType.NEW_READING, 3L, 1003L, "Garáž", true, true));

        store.pruneOrphanedConfigs(Set.of(1, 3));

        assertNotNull(store.loadConfig(1));
        assertNull(store.loadConfig(2));
        assertNotNull(store.loadConfig(3));
    }


    @Test
    public void loadConfigsForSubscriptionPointMatchesByIdOrMilins() {
        WidgetConfigStore store = new WidgetConfigStore(new InMemoryStorage());

        store.saveConfig(new WidgetConfig(1, WidgetType.CURRENT_PRICE, 10L, 555001L, "Domov", true, true));
        store.saveConfig(new WidgetConfig(2, WidgetType.HDO_CLOCK, 20L, 555002L, "Chata", true, true));
        store.saveConfig(new WidgetConfig(3, WidgetType.NEW_READING, 10L, 555001L, "Domov", true, true));

        List<WidgetConfig> byId = store.loadConfigsForSubscriptionPoint(10L, -1L);
        assertEquals(2, byId.size());

        List<WidgetConfig> byMilins = store.loadConfigsForSubscriptionPoint(-1L, 555002L);
        assertEquals(1, byMilins.size());
        assertEquals(2, byMilins.get(0).appWidgetId());
    }

}
