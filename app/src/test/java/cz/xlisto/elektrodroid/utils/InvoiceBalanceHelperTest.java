package cz.xlisto.elektrodroid.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Jednotkové testy pro {@link InvoiceBalanceHelper}.
 * Ověřují správné určení stavu bilance i převod na absolutní hodnotu.
 */
public class InvoiceBalanceHelperTest {

    /**
     * Kladná bilance musí znamenat přeplatek.
     */
    @Test
    public void positiveBalanceMeansOverpayment() {
        assertEquals(InvoiceBalanceHelper.BalanceState.OVERPAYMENT, InvoiceBalanceHelper.getBalanceState(125.50));
        assertEquals(125.50, InvoiceBalanceHelper.getAbsoluteBalance(125.50), 0.0001);
    }

    /**
     * Záporná bilance musí znamenat nedoplatek.
     */
    @Test
    public void negativeBalanceMeansUnderpayment() {
        assertEquals(InvoiceBalanceHelper.BalanceState.UNDERPAYMENT, InvoiceBalanceHelper.getBalanceState(-99.99));
        assertEquals(99.99, InvoiceBalanceHelper.getAbsoluteBalance(-99.99), 0.0001);
    }

    /**
     * Nulová bilance musí znamenat vyrovnaný stav.
     */
    @Test
    public void zeroBalanceMeansBalanced() {
        assertEquals(InvoiceBalanceHelper.BalanceState.BALANCED, InvoiceBalanceHelper.getBalanceState(0.0));
        assertEquals(0.0, InvoiceBalanceHelper.getAbsoluteBalance(0.0), 0.0001);
    }
}

