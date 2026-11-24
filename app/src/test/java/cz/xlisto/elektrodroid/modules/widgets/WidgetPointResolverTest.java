package cz.xlisto.elektrodroid.modules.widgets;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * Jednotkové testy pro {@link WidgetPointResolver} a parsování názvů tabulek v {@link WidgetUpdateManager}.
 * Ověřují bezpečné dohledání odběrného místa, přejmenování, smazání i obnovu databáze ze zálohy.
 */
public class WidgetPointResolverTest {

    private static SubscriptionPointModel point(long id, String name, long milins) {
        return new SubscriptionPointModel(id, name, "", milins, 3, 25, "123456", "859182400000");
    }


    @Test
    public void resolvesMatchingIdAndMilinsAndPreservesRename() {
        WidgetConfig config = new WidgetConfig(5, WidgetType.CURRENT_PRICE, 2L, 1700002L, "Starý název", true, true);
        List<SubscriptionPointModel> points = Arrays.asList(
                point(1L, "Domov", 1700001L),
                point(2L, "Chata po rekonstrukci", 1700002L)
        );

        WidgetPointResolver.ResolutionResult result = WidgetPointResolver.resolve(config, points);

        assertEquals(WidgetPointResolver.ResolutionStatus.RESOLVED, result.status());
        assertNotNull(result.point());
        assertEquals("Chata po rekonstrukci", result.point().getName());
        assertFalse(result.rebound());
    }


    @Test
    public void returnsMissingWhenPointDeletedNeverFallsBackToAnotherPoint() {
        WidgetConfig config = new WidgetConfig(7, WidgetType.NEW_READING, 2L, 1700002L, "Chata", true, true);
        List<SubscriptionPointModel> points = Collections.singletonList(
                point(1L, "Domov", 1700001L)
        );

        WidgetPointResolver.ResolutionResult result = WidgetPointResolver.resolve(config, points);

        assertEquals(WidgetPointResolver.ResolutionStatus.MISSING, result.status());
        assertNull(result.point());
    }


    @Test
    public void rejectsReusedIdWithDifferentMilinsAfterBackupRestore() {
        // Widget byl vázán na ID=1, milins=1700001 ("Domov")
        WidgetConfig config = new WidgetConfig(9, WidgetType.FINANCIAL_BALANCE, 1L, 1700001L, "Domov", true, true);
        // Po obnově jiné zálohy má ID=1 úplně jiné odběrné místo s jiným milins!
        List<SubscriptionPointModel> restoredPoints = Collections.singletonList(
                point(1L, "Cizí garáž", 9999999L)
        );

        WidgetPointResolver.ResolutionResult result = WidgetPointResolver.resolve(config, restoredPoints);

        assertEquals(WidgetPointResolver.ResolutionStatus.MISSING, result.status());
        assertNull(result.point());
    }


    @Test
    public void safelyRebindsByUniqueMilinsWhenDatabaseIdChanges() {
        WidgetConfig config = new WidgetConfig(12, WidgetType.CONSUMPTION, 2L, 1700002L, "Chata", true, true);
        // Po obnově databáze má původní Chata (stejné milins) nové _id = 5
        List<SubscriptionPointModel> restoredPoints = Arrays.asList(
                point(1L, "Domov", 1700001L),
                point(5L, "Chata", 1700002L)
        );

        WidgetPointResolver.ResolutionResult result = WidgetPointResolver.resolve(config, restoredPoints);

        assertEquals(WidgetPointResolver.ResolutionStatus.RESOLVED, result.status());
        assertTrue(result.rebound());
        assertNotNull(result.point());
        assertEquals(5L, result.point().getId());
    }


    @Test
    public void returnsUnconfiguredWhenConfigIsNull() {
        WidgetPointResolver.ResolutionResult result = WidgetPointResolver.resolve(
                null,
                Collections.singletonList(point(1L, "Domov", 1700001L))
        );

        assertEquals(WidgetPointResolver.ResolutionStatus.UNCONFIGURED, result.status());
        assertNull(result.point());
    }


    @Test
    public void parseMilinsFromDynamicTableNames() {
        assertEquals(1712345678901L, WidgetUpdateManager.parseMilinsFromTableName("O1712345678901"));
        assertEquals(1712345678901L, WidgetUpdateManager.parseMilinsFromTableName("HDO1712345678901"));
        assertEquals(1712345678901L, WidgetUpdateManager.parseMilinsFromTableName("FAK1712345678901"));
        assertEquals(1712345678901L, WidgetUpdateManager.parseMilinsFromTableName("TED1712345678901"));
        assertEquals(1712345678901L, WidgetUpdateManager.parseMilinsFromTableName("PLATBY1712345678901"));
        assertEquals(-1L, WidgetUpdateManager.parseMilinsFromTableName("odberna_mista"));
        assertEquals(-1L, WidgetUpdateManager.parseMilinsFromTableName(null));
    }

}
