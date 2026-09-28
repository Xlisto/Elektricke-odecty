package cz.xlisto.elektrodroid.models;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;


public class HdoModelTest {

    @Test
    public void testEqualsAndHashCodeWithNullCalendarStart() {
        HdoModel model1 = new HdoModel(1, "Relé1", "01.09.2026", "25.10.2026", "00:00", "06:00", 1, 1, 1, 1, 1, 0, 0, "CEZ");
        HdoModel model2 = new HdoModel(1, "Relé1", "01.09.2026", "25.10.2026", "00:00", "06:00", 1, 1, 1, 1, 1, 0, 0, "CEZ");

        assertEquals("Models with null calendarStart should equal if ID and fields match", model1, model2);
        assertEquals("HashCode should not throw NPE when calendarStart is null", model1.hashCode(), model2.hashCode());
    }


    @Test
    public void testEqualsWithDifferentIds() {
        HdoModel model1 = new HdoModel(1, "Relé1", "01.09.2026", "25.10.2026", "00:00", "06:00", 1, 1, 1, 1, 1, 0, 0, "CEZ");
        HdoModel model2 = new HdoModel(2, "Relé1", "01.09.2026", "25.10.2026", "00:00", "06:00", 1, 1, 1, 1, 1, 0, 0, "CEZ");

        assertNotEquals("Models with different IDs should not be equal", model1, model2);
    }

}
