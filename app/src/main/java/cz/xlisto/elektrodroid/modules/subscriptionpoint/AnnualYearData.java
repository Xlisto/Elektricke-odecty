package cz.xlisto.elektrodroid.modules.subscriptionpoint;


/**
 * Datový model jednoho roku pro přehledový kombinovaný graf roční spotřeby a průměrné ceny.
 *
 * @param consumptionVT         v kWh
 * @param consumptionNT         v kWh
 * @param costVT                var. náklady VT v Kč s DPH
 * @param costNT                var. náklady NT v Kč s DPH
 * @param fixedCostTotal        celkové fixní náklady za rok v Kč s DPH
 * @param totalCost             celkové náklady (var + fix) v Kč s DPH
 * @param avgPriceVT            var. cena VT v Kč/MWh s DPH
 * @param avgPriceNT            var. cena NT v Kč/MWh s DPH
 * @param avgPriceTotal         průměrná cena celkem v Kč/MWh s DPH
 * @param avgMonthlyFixedCost   průměrný měsíční fixní náklad v Kč s DPH
 * @param monthsCount           počet měsíců v období
 * @param currentYearRangeLabel např. "(leden–srpen)" nebo null
 */
public record AnnualYearData(int year, double consumptionVT, double consumptionNT, double costVT,
                             double costNT, double fixedCostTotal, double totalCost,
                             double avgPriceVT, double avgPriceNT, double avgPriceTotal,
                             double avgMonthlyFixedCost, double monthsCount, boolean isCurrentYear,
                             boolean isEstimated, String currentYearRangeLabel,
                             boolean isDualTariff) {

    public double getTotalConsumption() {
        return consumptionVT + consumptionNT;
    }

}
