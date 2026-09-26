package cz.xlisto.elektrodroid.modules.hdo;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;


public class ConnectionsTest {

    @Test
    public void testEasterHolidays2024() {
        // Velikonoční neděle 2024 je 31. března
        // Velký pátek 2024 = 29. března
        // Velikonoční pondělí 2024 = 1. dubna

        Calendar goodFriday = Calendar.getInstance();
        goodFriday.set(2024, Calendar.MARCH, 29);
        assertTrue("29.3.2024 by měl být Velký pátek", Connections.isCzechHoliday(goodFriday));

        Calendar easterMonday = Calendar.getInstance();
        easterMonday.set(2024, Calendar.APRIL, 1);
        assertTrue("1.4.2024 by mělo být Velikonoční pondělí", Connections.isCzechHoliday(easterMonday));

        Calendar regularDay = Calendar.getInstance();
        regularDay.set(2024, Calendar.MARCH, 30);
        assertFalse("30.3.2024 je Bílá sobota, ne státní svátek", Connections.isCzechHoliday(regularDay));
    }


    @Test
    public void testEasterHolidays2026() {
        // Velikonoční neděle 2026 je 5. dubna
        // Velký pátek 2026 = 3. dubna
        // Velikonoční pondělí 2026 = 6. dubna

        Calendar goodFriday = Calendar.getInstance();
        goodFriday.set(2026, Calendar.APRIL, 3);
        assertTrue("3.4.2026 by měl být Velký pátek", Connections.isCzechHoliday(goodFriday));

        Calendar easterMonday = Calendar.getInstance();
        easterMonday.set(2026, Calendar.APRIL, 6);
        assertTrue("6.4.2026 by mělo být Velikonoční pondělí", Connections.isCzechHoliday(easterMonday));
    }


    @Test
    public void testFixedHolidays() {
        Calendar christmas = Calendar.getInstance();
        christmas.set(2026, Calendar.DECEMBER, 24);
        assertTrue("24.12.2026 by měl být Štědrý den", Connections.isCzechHoliday(christmas));

        Calendar nationalDay = Calendar.getInstance();
        nationalDay.set(2026, Calendar.OCTOBER, 28);
        assertTrue("28.10.2026 by měl být státní svátek", Connections.isCzechHoliday(nationalDay));
    }

}
