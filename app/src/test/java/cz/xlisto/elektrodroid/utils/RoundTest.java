package cz.xlisto.elektrodroid.utils;

import junit.framework.TestCase;

public class RoundTest extends TestCase {

    public void testRound() {
        assertEquals(4.26, Round.round(4.2568));
        assertEquals(4.26, Round.round(4.25689658774));
        assertEquals(4.25, Round.round(4.25489658774));
    }

    public void testTestRound() {
        assertEquals(4.26, Round.round(4.2568,2));
        assertEquals(4.257, Round.round(4.2568,3));
        assertEquals(4.0, Round.round(4,3));
    }
}