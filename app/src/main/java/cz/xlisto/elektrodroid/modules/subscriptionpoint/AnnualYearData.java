package cz.xlisto.elektrodroid.modules.subscriptionpoint;

/**
 * Datový model jednoho roku pro přehledový kombinovaný graf roční spotřeby a průměrné ceny.
 *
 * @param year                  rok
 * @param consumptionVT         spotřeba VT v kWh
 * @param consumptionNT         spotřeba NT v kWh
 * @param costVT                variabilní náklady VT v Kč s DPH
 * @param costNT                variabilní náklady NT v Kč s DPH
 * @param fixedCostTotal        celkové fixní náklady za rok v Kč s DPH
 * @param totalCost             celkové náklady (variabilní + fixní) v Kč s DPH
 * @param avgPriceVT            variabilní vážená průměrná cena VT v Kč/MWh s DPH
 * @param avgPriceNT            variabilní vážená průměrná cena NT v Kč/MWh s DPH
 * @param avgPriceTotal         celková vážená průměrná cena celkem v Kč/MWh s DPH
 * @param avgMonthlyFixedCost   průměrný stálý měsíční plat v Kč/měsíc s DPH
 * @param monthsCount           počet měsíců v daném období
 * @param isCurrentYear         příznak, zda se jedná o aktuální probíhající rok
 * @param isEstimated           příznak, zda data obsahují interpolovaný odhad k 31.12./1.1.
 * @param currentYearRangeLabel např. "(leden–září)" nebo null
 * @param isDualTariff          příznak, zda odběrné místo využívá dvoutarifní sazbu
 */
public record AnnualYearData(int year, double consumptionVT, double consumptionNT, double costVT,
                             double costNT, double fixedCostTotal, double totalCost,
                             double avgPriceVT, double avgPriceNT, double avgPriceTotal,
                             double avgMonthlyFixedCost, double monthsCount, boolean isCurrentYear,
                             boolean isEstimated, String currentYearRangeLabel,
                             boolean isDualTariff) {

    /**
     * Vrací celkovou roční spotřebu jako součet VT a NT v kWh.
     *
     * @return Celková spotřeba v kWh
     */
    public double getTotalConsumption() {
        return consumptionVT + consumptionNT;
    }
}
