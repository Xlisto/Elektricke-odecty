package cz.xlisto.elektrodroid.utils;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;


/**
 * Unit testy pro dynamické zjišťování dostupných roků regulovaných cen v {@link ReadRawJSON}.
 */
public class ReadRawJSONTest {

    @Test
    public void getAvailableYearsContainsExpectedYearsFromOstatniJson() {
        ReadRawJSON reader = new ReadRawJSON(null);
        Set<Integer> years = reader.getAvailableYears();

        assertFalse("Seznam dostupných roků nesmí být prázdný", years.isEmpty());
        assertTrue(years.contains(2021));
        assertTrue(years.contains(2026));
    }


    @Test
    public void isYearAvailableReturnsTrueForStoredYearsAndFalseOutsideRange() {
        ReadRawJSON reader = new ReadRawJSON(null);

        assertTrue(reader.isYearAvailable(2021));
        assertTrue(reader.isYearAvailable(2026));
        assertFalse(reader.isYearAvailable(2020));
        assertFalse(reader.isYearAvailable(2099));
    }

}
