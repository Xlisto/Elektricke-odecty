package cz.xlisto.elektrodroid.repositories;


import java.util.HashMap;
import java.util.Map;

import cz.xlisto.elektrodroid.models.DistributionTariffConditions;


/**
 * Centrální úložiště a poskytovatel technických podmínek přiznání distribučních sazeb elektřiny podle oficiálních předpisů ERÚ a distributorů.
 */
public class DistributionTariffConditionsRepository {

    private static final Map<String, DistributionTariffConditions> TARIFF_MAP = new HashMap<>();

    static {
        TARIFF_MAP.put("D01D", new DistributionTariffConditions(
                "D01d",
                "Malá spotřeba",
                "Jednotarifní",
                "Běžné spotřebiče bez náročnějších technických zařízení.",
                "Jednotarifní sazba určená pro odběrná místa s malou spotřebou elektřiny (svícení, běžné spotřebiče, chladnička, TV).\n\nNení požadován žádný konkrétní elektrický topný spotřebič. Pro nové přiznání platí standardní pravidla distributora.",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D02D", new DistributionTariffConditions(
                "D02d",
                "Běžná domácnost",
                "Jednotarifní",
                "Standardní spotřeba bez elektrického vytápění.",
                "Jednotarifní sazba určená pro běžné domácnosti se střední až vyšší spotřebou bez elektrického vytápění a bez akumulačního ohřevu vody.\n\nNení vyžadováno technické blokování spotřebičů ani doložení specifických topných zařízení.",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D25D", new DistributionTariffConditions(
                "D25d",
                "Akumulační ohřev vody",
                "8 h NT denně",
                "Elektrický bojler nebo akumulační vytápění.",
                "Dvoutarifní sazba s operativním řízením doby platnosti nízkého tarifu po dobu 8 hodin denně.\n\nPožadován instalovaný akumulační elektrický spotřebič pro ohřev teplé užitkové vody (bojler) nebo akumulační vytápění. Současně je vyžadováno odpovídající technické blokování spotřebičů v době vysokého tarifu (VT).",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D26D", new DistributionTariffConditions(
                "D26d",
                "Akumulační vytápění",
                "8 h NT denně",
                "Akumulační kamna nebo akumulační vytápění objektu.",
                "Akumulační kamna nebo jiné akumulační elektrické vytápění. Příkon zpravidla nejméně 55 % příkonu odpovídajícího hlavnímu jističi; možné jsou alternativní podmínky podle tepelných ztrát objektu nebo energetické náročnosti budovy.\n\nKonečné přiznání sazby posuzuje distributor. Požadováno odpovídající technické blokování akumulačních spotřebičů.",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D27D", new DistributionTariffConditions(
                "D27d",
                "Elektromobilita",
                "8 h NT denně",
                "Dobíjení elektrického vozidla.",
                "Dvoutarifní sazba s poskytováním nízkého tarifu po dobu 8 hodin denně (především v nočním období).\n\nPožadováno doložení vlastnického nebo užívacího práva k elektromobilu (technický průkaz nebo leasingová smlouva). Sazbu lze využít i pro dobíjecí stanice ve společných garážích.",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D35D", new DistributionTariffConditions(
                "D35d",
                "Hybridní vytápění",
                "16 h NT denně",
                "Kombinované elektrické vytápění objektu.",
                "Historická dvoutarifní sazba s poskytováním nízkého tarifu po dobu 16 hodin denně pro kombinované/hybridní systémy elektrického vytápění.\n\nVyžadováno doložení příkonu topných spotřebičů alespoň 50 % příkonu odpovídajícího jističi a technické blokování v době VT.",
                true,
                "Tuto sazbu již nelze nově přiznat (platí od 1. 4. 2016). Stávajícím odběratelům může zůstat zachována.",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D45D", new DistributionTariffConditions(
                "D45d",
                "Přímotopné vytápění",
                "20 h NT denně",
                "Přímotopná elektrická tělesa nebo sálavé panely.",
                "Historická dvoutarifní sazba s platností nízkého tarifu po dobu 20 hodin denně pro objekty vytápěné přímotopy.\n\nInstalovaný příkon přímotopných topných spotřebičů musel dosahovat alespoň 40 % příkonu jističe. Vyžadováno technické blokování topných okruhů.",
                true,
                "Tuto sazbu již nelze nově přiznat (platí od 1. 4. 2016). Stávajícím odběratelům může zůstat zachována.",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D56D", new DistributionTariffConditions(
                "D56d",
                "Tepelné čerpadlo",
                "22 h NT denně",
                "Vytápění objektu tepelným čerpadlem.",
                "Historická dvoutarifní sazba s platností nízkého tarifu po dobu 22 hodin denně určená pro vytápění tepelným čerpadlem.\n\nPožadován tepelný výkon čerpadla kryjící stanovenou část tepelných ztrát objektu a blokování dohřevu.",
                true,
                "Tuto sazbu již nelze nově přiznat (platí od 1. 4. 2016). Stávajícím odběratelům může zůstat zachována.",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D57D", new DistributionTariffConditions(
                "D57d",
                "Elektrické vytápění",
                "20 h NT denně",
                "Přímotopy, elektrokotel, podlahové topení nebo tepelné čerpadlo.",
                "Moderní dvoutarifní sazba s platností nízkého tarifu po dobu 20 hodin denně pro vytápění objektu elektrickými spotřebiči nebo tepelným čerpadlem.\n\nPodporované technologie zahrnují elektrokotle, přímotopná tělesa, podlahové vytápění i tepelná čerpadla. Součtový příkon topných spotřebičů musí dosahovat alespoň 40 % příkonu jističe (nebo doložení dle tepelných ztrát/PENB). Současně je vyžadováno technické blokování topných spotřebičů v době VT.",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));

        TARIFF_MAP.put("D61D", new DistributionTariffConditions(
                "D61d",
                "Víkendová sazba",
                "Pátek 12:00 – Neděle 22:00",
                "Vhodná pro chataře a chalupáře.",
                "Dvoutarifní sazba s časově souvislým pásmem nízkého tarifu od pátku 12:00 do neděle 22:00 hodin.\n\nNení vyžadován žádný konkrétní elektrický topný spotřebič ani technické blokování. Sazba je ideální pro rekreační objekty s víkendovým provozem.",
                false,
                "",
                "https://www.cezdistribuce.cz/pro-zakazniky/potrebuji-vyresit/ceny-a-podminky/podminky-pro-priznani-distribucni-sazby"
        ));
    }

    public static DistributionTariffConditions getConditionsForTariff(String tariffCode) {
        if (tariffCode == null) return null;
        String key = tariffCode.trim().toUpperCase().replaceAll("\\s+", "");
        return TARIFF_MAP.get(key);
    }

}
