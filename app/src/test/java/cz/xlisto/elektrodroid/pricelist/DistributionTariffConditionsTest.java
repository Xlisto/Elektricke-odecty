package cz.xlisto.elektrodroid.pricelist;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cz.xlisto.elektrodroid.models.DistributionTariffConditions;
import cz.xlisto.elektrodroid.repositories.DistributionTariffConditionsRepository;


public class DistributionTariffConditionsTest {

    @Test
    public void testStandardTariffs() {
        DistributionTariffConditions d25 = DistributionTariffConditionsRepository.getConditionsForTariff("D25d");
        assertNotNull(d25);
        assertEquals("D25d", d25.tariffCode());
        assertEquals("Akumulační ohřev vody", d25.title());
        assertEquals("8 h NT denně", d25.lowTariffBadge());
        assertFalse(d25.isHistorical());

        DistributionTariffConditions d26 = DistributionTariffConditionsRepository.getConditionsForTariff("D26d");
        assertNotNull(d26);
        assertEquals("D26d", d26.tariffCode());
        assertEquals("Akumulační vytápění", d26.title());
        assertEquals("8 h NT denně", d26.lowTariffBadge());
        assertFalse(d26.isHistorical());
    }


    @Test
    public void testHistoricalTariffs() {
        DistributionTariffConditions d35 = DistributionTariffConditionsRepository.getConditionsForTariff("D35d");
        assertNotNull(d35);
        assertTrue(d35.isHistorical());
        assertTrue(d35.historicalNotice().contains("Tuto sazbu již nelze nově přiznat"));

        DistributionTariffConditions d45 = DistributionTariffConditionsRepository.getConditionsForTariff("D45d");
        assertNotNull(d45);
        assertTrue(d45.isHistorical());

        DistributionTariffConditions d56 = DistributionTariffConditionsRepository.getConditionsForTariff("D56d");
        assertNotNull(d56);
        assertTrue(d56.isHistorical());
    }


    @Test
    public void testCaseInsensitiveMatching() {
        DistributionTariffConditions d57Lower = DistributionTariffConditionsRepository.getConditionsForTariff("d57d");
        assertNotNull(d57Lower);
        assertEquals("D57d", d57Lower.tariffCode());
        assertEquals("20 h NT denně", d57Lower.lowTariffBadge());

        DistributionTariffConditions d02WithSpaces = DistributionTariffConditionsRepository.getConditionsForTariff(" D02d ");
        assertNotNull(d02WithSpaces);
        assertEquals("D02d", d02WithSpaces.tariffCode());
    }

}
