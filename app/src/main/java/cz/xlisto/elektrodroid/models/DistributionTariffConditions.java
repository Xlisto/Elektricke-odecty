package cz.xlisto.elektrodroid.models;


import java.io.Serializable;


/**
 * Datový model reprezentující technické podmínky přiznání distribuční sazby.
 */
public class DistributionTariffConditions implements Serializable {

    private final String tariffCode;
    private final String title;
    private final String lowTariffBadge;
    private final String shortDescription;
    private final String detailedConditions;
    private final boolean isHistorical;
    private final String historicalNotice;
    private final String officialUrl;


    public DistributionTariffConditions(
            String tariffCode,
            String title,
            String lowTariffBadge,
            String shortDescription,
            String detailedConditions,
            boolean isHistorical,
            String historicalNotice,
            String officialUrl
    ) {
        this.tariffCode = tariffCode;
        this.title = title;
        this.lowTariffBadge = lowTariffBadge;
        this.shortDescription = shortDescription;
        this.detailedConditions = detailedConditions;
        this.isHistorical = isHistorical;
        this.historicalNotice = historicalNotice;
        this.officialUrl = officialUrl;
    }


    public String getTariffCode() {
        return tariffCode;
    }


    public String getTitle() {
        return title;
    }


    public String getLowTariffBadge() {
        return lowTariffBadge;
    }


    public String getShortDescription() {
        return shortDescription;
    }


    public String getDetailedConditions() {
        return detailedConditions;
    }


    public boolean isHistorical() {
        return isHistorical;
    }


    public String getHistoricalNotice() {
        return historicalNotice;
    }


    public String getOfficialUrl() {
        return officialUrl;
    }

}
