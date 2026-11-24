package cz.xlisto.elektrodroid.models;


import java.io.Serializable;


/**
 * Datový model reprezentující technické podmínky přiznání distribuční sazby.
 */
public record DistributionTariffConditions(String tariffCode, String title, String lowTariffBadge,
                                           String shortDescription, String detailedConditions,
                                           boolean isHistorical, String historicalNotice,
                                           String officialUrl) implements Serializable {

}
