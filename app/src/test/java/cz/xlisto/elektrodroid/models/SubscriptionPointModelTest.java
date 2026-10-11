package cz.xlisto.elektrodroid.models;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class SubscriptionPointModelTest {

    @Test
    public void testSubscriptionPointModelGettersWithNonNullValues() {
        SubscriptionPointModel model = new SubscriptionPointModel(1L, "Místo 1", "Popis 1", 123456L, 3, 25, "123", "456");
        assertEquals(1L, model.getId());
        assertEquals("Místo 1", model.getName());
        assertEquals("Popis 1", model.getDescription());
        assertEquals(123456L, model.getMilins());
        assertEquals(3, model.getCountPhaze());
        assertEquals(25, model.getPhaze());
        assertEquals("123", model.getNumberElectricMeter());
        assertEquals("456", model.getNumberSubscriptionPoint());
        assertEquals("O123456", model.getTableO());
        assertEquals("HDO123456", model.getTableHDO());
        assertEquals("FAK123456", model.getTableFAK());
        assertEquals("TED123456", model.getTableTED());
        assertEquals("PLATBY123456", model.getTablePLATBY());
    }


    @Test
    public void testSubscriptionPointModelHandlesNullStrings() {
        SubscriptionPointModel model = new SubscriptionPointModel(1L, null, null, 123456L, 3, 25, null, null);
        assertNotNull(model.getName());
        assertEquals("", model.getName());
        assertNotNull(model.getDescription());
        assertEquals("", model.getDescription());
        assertNotNull(model.getNumberElectricMeter());
        assertEquals("", model.getNumberElectricMeter());
        assertNotNull(model.getNumberSubscriptionPoint());
        assertEquals("", model.getNumberSubscriptionPoint());
    }


    @Test
    public void testSubscriptionPointModelConstructorWithoutIdHandlesNullStrings() {
        SubscriptionPointModel model = new SubscriptionPointModel(null, null, 654321L, 1, 16, null, null);
        assertNotNull(model.getName());
        assertEquals("", model.getName());
        assertNotNull(model.getDescription());
        assertEquals("", model.getDescription());
        assertNotNull(model.getNumberElectricMeter());
        assertEquals("", model.getNumberElectricMeter());
        assertNotNull(model.getNumberSubscriptionPoint());
        assertEquals("", model.getNumberSubscriptionPoint());
    }

}
