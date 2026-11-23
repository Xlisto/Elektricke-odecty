package cz.xlisto.elektrodroid.modules.dashboard;


import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Jednotkové testy výpočtu znaménkového poměru a procent pro {@link DashboardBillingBalanceBarView}.
 */
public class DashboardBillingBalanceBarViewTest {

    @Test
    public void overpayment_returnsPositiveRatioAndConsistentPercentages() {
        double advances = 18_000.0;
        double costs = 7_748.0;

        float ratio = DashboardBillingBalanceBarView.calculateSignedRatio(advances, costs);
        int costsPct = DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(advances, costs);
        int balancePct = DashboardBillingBalanceBarView.calculateBalancePercent(advances, costs);

        assertEquals(0.56955f, ratio, 0.001f);
        assertEquals(43, costsPct);
        assertEquals(57, balancePct);
        assertEquals(100, costsPct + balancePct);
    }


    @Test
    public void underpayment_returnsNegativeRatioAndConsistentPercentages() {
        double advances = 18_000.0;
        double costs = 22_000.0;

        float ratio = DashboardBillingBalanceBarView.calculateSignedRatio(advances, costs);
        int costsPct = DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(advances, costs);
        int balancePct = DashboardBillingBalanceBarView.calculateBalancePercent(advances, costs);

        assertEquals(-0.1818f, ratio, 0.001f);
        assertEquals(122, costsPct);
        assertEquals(-22, balancePct);
        assertEquals(100, costsPct + balancePct);
    }


    @Test
    public void balanced_returnsZeroRatioAndHundredPercentCosts() {
        assertEquals(0f, DashboardBillingBalanceBarView.calculateSignedRatio(12_500.0, 12_500.0), 0.0001f);
        assertEquals(100, DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(12_500.0, 12_500.0));
        assertEquals(0, DashboardBillingBalanceBarView.calculateBalancePercent(12_500.0, 12_500.0));
    }


    @Test
    public void zeroCostsWithPositiveAdvances_returnsFullPositiveRatio() {
        assertEquals(1f, DashboardBillingBalanceBarView.calculateSignedRatio(10_000.0, 0.0), 0.0001f);
        assertEquals(0, DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(10_000.0, 0.0));
        assertEquals(100, DashboardBillingBalanceBarView.calculateBalancePercent(10_000.0, 0.0));
    }


    @Test
    public void zeroAdvancesWithPositiveCosts_returnsFullNegativeRatio() {
        assertEquals(-1f, DashboardBillingBalanceBarView.calculateSignedRatio(0.0, 5_000.0), 0.0001f);
        assertEquals(-1, DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(0.0, 5_000.0));
        assertEquals(-100, DashboardBillingBalanceBarView.calculateBalancePercent(0.0, 5_000.0));
    }


    @Test
    public void bothZeroOrNegative_returnsNeutralZeros() {
        assertEquals(0f, DashboardBillingBalanceBarView.calculateSignedRatio(0.0, 0.0), 0.0001f);
        assertEquals(0, DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(0.0, 0.0));
        assertEquals(0, DashboardBillingBalanceBarView.calculateBalancePercent(0.0, 0.0));

        assertEquals(0f, DashboardBillingBalanceBarView.calculateSignedRatio(-100.0, -50.0), 0.0001f);
        assertEquals(0, DashboardBillingBalanceBarView.calculateCostsToAdvancesPercent(-100.0, -50.0));
        assertEquals(0, DashboardBillingBalanceBarView.calculateBalancePercent(-100.0, -50.0));
    }

}
