package cz.xlisto.elektrodroid.modules.widgets;


import static cz.xlisto.elektrodroid.models.PriceListModel.NEW_POZE_YEAR;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.InvoiceListModel;
import cz.xlisto.elektrodroid.models.InvoiceModel;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PozeModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.PriceListRegulBuilder;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.dashboard.DashboardHdoFormatHelper;
import cz.xlisto.elektrodroid.modules.hdo.HdoTime;
import cz.xlisto.elektrodroid.modules.invoice.InvoiceAbstract;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.services.HdoAlarmScheduler;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.utils.Calculation;
import cz.xlisto.elektrodroid.utils.DifferenceDate;
import cz.xlisto.elektrodroid.utils.InvoiceBalanceHelper;


/**
 * Sdílená servisní vrstva pro načítání dat a výpočty pro všechny typy domovských widgetů.
 * <p>
 * Znovupoužívá existující výpočetní logiku aplikace ({@link Calculation}, {@link PriceListRegulBuilder},
 * {@link HdoTime}, {@link HdoAlarmScheduler}, {@link DashboardHdoFormatHelper}, {@link InvoiceBalanceHelper})
 * a zajišťuje vláknově bezpečný přístup k {@link Calculation} i spolehlivé uzavírání databázových zdrojů.
 */
public final class WidgetDataService {

    /**
     * Synchronizační zámek chránící sdílený statický stav {@link Calculation#calculatePriceForPriceListKwh}
     * a {@link Calculation#calculatePriceWithoutPozeMwH} při volání z vlákna na pozadí.
     */
    private static final Object CALCULATION_LOCK = new Object();


    private WidgetDataService() {
    }


    /**
     * Stav platnosti přiřazeného ceníku k aktuálnímu datu.
     */
    public enum PriceListValidityStatus {
        /**
         * Ceník existuje a je platný k aktuálnímu datu.
         */
        VALID,
        /**
         * Odběrné místo nemá přiřazený žádný ceník nebo ceník nebyl nalezen.
         */
        MISSING,
        /**
         * Platnost přiřazeného ceníku již vypršela ({@code now > validTo}).
         */
        EXPIRED,
        /**
         * Přiřazený ceník ještě nevstoupil v platnost ({@code now < validFrom}).
         */
        NOT_YET_VALID
    }


    /**
     * Datový snímek pro widget „Aktuální cena elektřiny“.
     *
     * @param priceListStatus          stav platnosti ceníku
     * @param priceListValidFromMillis začátek platnosti přiřazeného ceníku v ms (nebo {@code 0L})
     * @param priceListValidToMillis   konec platnosti přiřazeného ceníku v ms (nebo {@code 0L})
     * @param vtPriceKwh               vypočtená cena VT v Kč/kWh (dostupná při {@link PriceListValidityStatus#VALID}
     *                                 a orientačně i při {@link PriceListValidityStatus#EXPIRED})
     * @param ntPriceKwh               vypočtená cena NT v Kč/kWh (dostupná při {@link PriceListValidityStatus#VALID}
     *                                 a orientačně i při {@link PriceListValidityStatus#EXPIRED})
     * @param isSingleTariff           {@code true}, pokud má ceník jednosazbovou distribuční sazbu (D01d/D02d)
     * @param showTotalPrice           {@code true} pro celkovou cenu, {@code false} pro obchodní cenu
     * @param includeVat               {@code true}, pokud cena zahrnuje DPH
     * @param hdoSnapshot              aktuální stav HDO a čas dalšího přepnutí
     * @param colorVt                  uživatelská barva vysokého tarifu (VT)
     * @param colorNt                  uživatelská barva nízkého tarifu (NT)
     */
    public record CurrentPriceSnapshot(@NonNull PriceListValidityStatus priceListStatus,
                                       long priceListValidFromMillis,
                                       long priceListValidToMillis,
                                       double vtPriceKwh,
                                       double ntPriceKwh,
                                       boolean isSingleTariff,
                                       boolean showTotalPrice,
                                       boolean includeVat,
                                       @NonNull HdoSnapshot hdoSnapshot,
                                       int colorVt,
                                       int colorNt) {

        /**
         * Vrací {@code true}, pokud má snímek vypočtené ceny VT/NT k zobrazení
         * (buď z platného ceníku, nebo orientačně z posledního přiřazeného ceníku po skončení platnosti).
         */
        public boolean hasDisplayablePrices() {
            return priceListStatus == PriceListValidityStatus.VALID
                    || priceListStatus == PriceListValidityStatus.EXPIRED;
        }


        /**
         * Vrací {@code true}, pokud jsou zobrazené ceny pouze orientační z ceníku, jehož platnost již vypršela.
         */
        public boolean isPriceListExpired() {
            return priceListStatus == PriceListValidityStatus.EXPIRED;
        }

    }


    /**
     * Maximální počet relé zobrazených současně v popiscích widgetu hodin HDO.
     */
    public static final int MAX_WIDGET_HDO_RELAYS = 3;

    private static final int MAX_SHORT_RELAY_LABEL_LENGTH = 8;


    /**
     * Vizuální kategorie relé HDO odpovídající barevným prstencům ciferníku od vnějšího ke středu:
     * {@link #TUV} (oranžový vnější), {@link #TAR} (zelený střední), {@link #PV} (modrý vnitřní).
     */
    public enum HdoRelayType {
        TUV,
        TAR,
        PV
    }


    /**
     * Datový snímek jednoho relé HDO pro zobrazení v popiscích widgetu hodin HDO.
     *
     * @param relayName         původní název relé z databáze
     * @param displayLabel      stručný čitelný popisek relé pro widget (např. {@code TUV}, {@code TAR}, {@code PV})
     * @param relayType         kategorie relé určující barvu prstence a bodu legendy
     * @param isActive          {@code true}, pokud je relé v daném okamžiku sepnuté (aktivní)
     * @param isMainRelay       {@code true}, pokud jde o hlavní zvolené relé odběrného místa
     * @param nextTriggerMillis systémový čas příštího přepnutí tohoto relé v ms (nebo {@code -1L})
     * @param formattedSwitch   naformátovaný čas příštího přepnutí a odpočet pro toto relé
     */
    public record HdoRelaySnapshot(@NonNull String relayName,
                                   @NonNull String displayLabel,
                                   @NonNull HdoRelayType relayType,
                                   boolean isActive,
                                   boolean isMainRelay,
                                   long nextTriggerMillis,
                                   @NonNull DashboardHdoFormatHelper.FormattedHdoSwitch formattedSwitch) {

    }


    /**
     * Datový snímek stavu HDO a intervalů pro widget „Analogové hodiny HDO“ i zvýraznění sazby ve widgetu ceny.
     *
     * @param allHdoModels      všechny HDO záznamy odběrného místa (včetně všech relé TUV/TAR/PV pro ciferník)
     * @param hasHdo            {@code true}, pokud má odběrné místo nastavené alespoň jedno HDO pravidlo
     * @param isNtActive        {@code true}, pokud právě běží nízký tarif (NT) na hlavním relé
     * @param nextTriggerMillis systémový čas dalšího přepnutí hlavního relé HDO v ms (nebo {@code -1L})
     * @param timeShiftMillis   časový posun elektroměru v ms
     * @param formattedSwitch   naformátovaný čas dalšího přepnutí a odpočet hlavního relé
     * @param relays            snímky jednotlivých relé (max. {@link #MAX_WIDGET_HDO_RELAYS}), seřazené podle prstenců ciferníku
     */
    public record HdoSnapshot(@NonNull List<HdoModel> allHdoModels,
                              boolean hasHdo,
                              boolean isNtActive,
                              long nextTriggerMillis,
                              long timeShiftMillis,
                              @NonNull DashboardHdoFormatHelper.FormattedHdoSwitch formattedSwitch,
                              @NonNull List<HdoRelaySnapshot> relays) {

        /**
         * Zpětně kompatibilní konstruktor bez detailního seznamu relé.
         */
        public HdoSnapshot(@NonNull List<HdoModel> allHdoModels,
                           boolean hasHdo,
                           boolean isNtActive,
                           long nextTriggerMillis,
                           long timeShiftMillis,
                           @NonNull DashboardHdoFormatHelper.FormattedHdoSwitch formattedSwitch) {
            this(allHdoModels, hasHdo, isNtActive, nextTriggerMillis, timeShiftMillis, formattedSwitch, Collections.emptyList());
        }


        /**
         * Vrací {@code true}, pokud má odběrné místo více než jedno relé HDO (2 až 3 relé).
         */
        public boolean hasMultipleRelays() {
            return relays.size() > 1;
        }


        /**
         * Vrátí nejbližší budoucí čas přepnutí napříč hlavním relé i všemi zobrazenými relé.
         *
         * @param nowMillis referenční systémový čas v ms
         * @return nejbližší budoucí čas přepnutí v ms, nebo {@code -1L}
         */
        public long earliestNextTriggerMillis(long nowMillis) {
            long earliest = Long.MAX_VALUE;
            if (nextTriggerMillis > nowMillis) {
                earliest = nextTriggerMillis;
            }
            for (HdoRelaySnapshot relay : relays) {
                if (relay.nextTriggerMillis() > nowMillis) {
                    earliest = Math.min(earliest, relay.nextTriggerMillis());
                }
            }
            return earliest == Long.MAX_VALUE ? -1L : earliest;
        }

    }


    /**
     * Datový snímek pro widgety „Spotřeba elektřiny“ a „Finanční bilance“ za aktivní sledované období.
     *
     * @param hasPeriod      {@code true}, pokud existuje platné sledované fakturační období
     * @param minDateMillis  počáteční datum sledovaného období v ms
     * @param maxDateMillis  koncové datum sledovaného období v ms
     * @param vtKwh          spotřeba ve vysokém tarifu v kWh
     * @param ntKwh          spotřeba v nízkém tarifu v kWh
     * @param totalKwh       celková spotřeba v kWh
     * @param isSingleTariff {@code true}, pokud jde o jednosazbový tarif
     * @param advancesKc     součet uhrazených záloh v Kč
     * @param costsWithTaxKc vypočtené celkové náklady s DPH po slevách v Kč
     * @param balanceKc      rozdíl záloh a nákladů v Kč ({@code advancesKc - costsWithTaxKc})
     * @param balanceState   vyhodnocený stav bilance (přeplatek / nedoplatek / vyrovnáno)
     * @param colorVt        uživatelská barva vysokého tarifu (VT)
     * @param colorNt        uživatelská barva nízkého tarifu (NT)
     */
    public record PeriodSnapshot(boolean hasPeriod,
                                 long minDateMillis,
                                 long maxDateMillis,
                                 double vtKwh,
                                 double ntKwh,
                                 double totalKwh,
                                 boolean isSingleTariff,
                                 double advancesKc,
                                 double costsWithTaxKc,
                                 double balanceKc,
                                 @NonNull InvoiceBalanceHelper.BalanceState balanceState,
                                 int colorVt,
                                 int colorNt) {

    }


    /**
     * Ověří časovou platnost ceníku v zadaném časovém pásmu.
     *
     * @param priceList ceník k ověření (může být {@code null})
     * @param nowMillis referenční čas v milisekundách
     * @param timeZone  časové pásmo
     * @return stav platnosti {@link PriceListValidityStatus}
     */
    @NonNull
    public static PriceListValidityStatus evaluatePriceListValidity(@Nullable PriceListModel priceList,
                                                                    long nowMillis,
                                                                    @NonNull TimeZone timeZone) {
        if (priceList == null || priceList.isEmpty() || priceList.getId() <= 0L) {
            return PriceListValidityStatus.MISSING;
        }
        long validFrom = priceList.getValidFrom();
        long validTo = priceList.getValidTo();
        if (validFrom > 0L && nowMillis < startOfDayMillis(validFrom, timeZone)) {
            return PriceListValidityStatus.NOT_YET_VALID;
        }
        if (validTo > 0L && nowMillis > endOfDayMillis(validTo, timeZone)) {
            return PriceListValidityStatus.EXPIRED;
        }
        return PriceListValidityStatus.VALID;
    }


    /**
     * Vypočítá jednotkovou cenu VT a NT v Kč/kWh pro zadaný ceník a odběrné místo pomocí sdílené logiky
     * {@link PriceListRegulBuilder} a {@link Calculation#calculatePriceForPriceListKwh}.
     *
     * @param priceList      platný ceník
     * @param point          odběrné místo
     * @param showTotalPrice {@code true} pro celkovou cenu, {@code false} pro obchodní cenu
     * @param includeVat     {@code true} pro započítání DPH
     * @return dvouprvkové pole {@code [vtPriceKwh, ntPriceKwh]}
     */
    @NonNull
    public static double[] computeUnitPrices(@NonNull PriceListModel priceList,
                                             @NonNull SubscriptionPointModel point,
                                             boolean showTotalPrice,
                                             boolean includeVat) {
        PriceListModel effectivePriceList = new PriceListRegulBuilder(priceList).getRegulPriceList();
        double vtPriceKwh;
        double ntPriceKwh;

        if (showTotalPrice) {
            double[] totalPrices;
            synchronized (CALCULATION_LOCK) {
                totalPrices = Calculation.calculatePriceForPriceListKwh(effectivePriceList, point);
            }
            vtPriceKwh = totalPrices[0];
            ntPriceKwh = totalPrices[1];
        } else {
            vtPriceKwh = effectivePriceList.getPriceVT() / 1000.0;
            ntPriceKwh = effectivePriceList.getPriceNT() / 1000.0;
        }

        if (includeVat) {
            double vatMultiplier = 1.0 + (effectivePriceList.getVat() / 100.0);
            vtPriceKwh *= vatMultiplier;
            ntPriceKwh *= vatMultiplier;
        }

        return new double[]{Math.max(0.0, vtPriceKwh), Math.max(0.0, ntPriceKwh)};
    }


    /**
     * Rozpozná, zda název distribuční sazby odpovídá jednosazbovému tarifu ({@code D 01d} nebo {@code D 02d}).
     *
     * @param rate název distribuční sazby
     * @return {@code true} pro jednosazbový tarif
     */
    public static boolean isSingleTariffRate(@Nullable String rate) {
        if (rate == null || rate.trim().isEmpty()) {
            return false;
        }
        String trimmed = rate.trim();
        if (InvoiceAbstract.D01.equalsIgnoreCase(trimmed) || InvoiceAbstract.D02.equalsIgnoreCase(trimmed)) {
            return true;
        }
        String compact = trimmed.replace(" ", "");
        return "D01d".equalsIgnoreCase(compact) || "D02d".equalsIgnoreCase(compact);
    }


    /**
     * Načte a vypočítá kompletní datový snímek pro widget „Aktuální cena elektřiny“.
     *
     * @param context aplikační kontext
     * @param point   ověřené odběrné místo widgetu
     * @param config  konfigurace instance widgetu (celková/obchodní cena, DPH)
     * @return datový snímek {@link CurrentPriceSnapshot}
     */
    @NonNull
    public static CurrentPriceSnapshot loadCurrentPriceSnapshot(@NonNull Context context,
                                                                @NonNull SubscriptionPointModel point,
                                                                @NonNull WidgetConfig config) {
        Context appContext = context.getApplicationContext();
        int[] userColors = loadUserVtNtColors(appContext);
        long nowMillis = System.currentTimeMillis();

        PriceListModel priceList = loadCurrentPriceList(appContext, point);
        HdoSnapshot hdoSnapshot = loadHdoSnapshot(appContext, point, nowMillis);
        return buildCurrentPriceSnapshot(
                priceList,
                point,
                config,
                hdoSnapshot,
                userColors[0],
                userColors[1],
                nowMillis,
                TimeZone.getDefault()
        );
    }


    /**
     * Sestaví {@link CurrentPriceSnapshot} z načteného ceníku a stavu HDO.
     * <p>
     * Při vypršeném ceníku ({@link PriceListValidityStatus#EXPIRED}, např. přechodné období na začátku
     * nového roku před zadáním nového odečtu a ceníku) zachová příznak {@code EXPIRED} pro jasné
     * varování uživatele a zároveň vypočítá orientační ceny z posledního přiřazeného ceníku.
     *
     * @param priceList   přiřazený ceník odběrného místa (může být {@code null})
     * @param point       odběrné místo
     * @param config      konfigurace instance widgetu
     * @param hdoSnapshot stav HDO
     * @param colorVt     uživatelská barva VT
     * @param colorNt     uživatelská barva NT
     * @param nowMillis   referenční čas v milisekundách
     * @param timeZone    časové pásmo pro vyhodnocení platnosti ceníku
     * @return sestavený datový snímek {@link CurrentPriceSnapshot}
     */
    @NonNull
    public static CurrentPriceSnapshot buildCurrentPriceSnapshot(@Nullable PriceListModel priceList,
                                                                 @NonNull SubscriptionPointModel point,
                                                                 @NonNull WidgetConfig config,
                                                                 @NonNull HdoSnapshot hdoSnapshot,
                                                                 int colorVt,
                                                                 int colorNt,
                                                                 long nowMillis,
                                                                 @NonNull TimeZone timeZone) {
        PriceListValidityStatus status = evaluatePriceListValidity(priceList, nowMillis, timeZone);
        long validFrom = (priceList != null) ? priceList.getValidFrom() : 0L;
        long validTo = (priceList != null) ? priceList.getValidTo() : 0L;

        double vtPrice = 0.0;
        double ntPrice = 0.0;
        boolean isSingleTariff = (priceList != null) && isSingleTariffRate(priceList.getRate());

        if (priceList != null && (status == PriceListValidityStatus.VALID || status == PriceListValidityStatus.EXPIRED)) {
            double[] prices = computeUnitPrices(priceList, point, config.showTotalPrice(), config.includeVat());
            vtPrice = prices[0];
            ntPrice = prices[1];
        }

        return new CurrentPriceSnapshot(
                status,
                validFrom,
                validTo,
                vtPrice,
                ntPrice,
                isSingleTariff,
                config.showTotalPrice(),
                config.includeVat(),
                hdoSnapshot,
                colorVt,
                colorNt
        );
    }


    /**
     * Klasifikuje název relé do jedné ze tří barevných kategorií ciferníku HDO ({@link HdoRelayType}).
     *
     * @param rele název relé (může být {@code null} nebo prázdný)
     * @return odpovídající {@link HdoRelayType}
     */
    @NonNull
    public static HdoRelayType classifyRelayType(@Nullable String rele) {
        if (rele == null || rele.trim().isEmpty()) {
            return HdoRelayType.TAR;
        }
        String upper = rele.toUpperCase(Locale.ROOT);
        boolean isTuv = upper.contains("TUV") || upper.contains("AKU") || upper.contains("BOJLER");
        boolean isPv = !isTuv && (upper.contains("PV") || upper.contains("FVE") || upper.contains("FOTOVOLT"));
        if (isPv) {
            return HdoRelayType.PV;
        }
        if (isTuv) {
            return HdoRelayType.TUV;
        }
        return HdoRelayType.TAR;
    }


    /**
     * Načte datový snímek HDO (všechny intervaly, stav hlavního relé, čas příštího přepnutí
     * i samostatné snímky až pro tři relé) pro konkrétní odběrné místo.
     *
     * @param context   aplikační kontext
     * @param point     odběrné místo
     * @param nowMillis referenční systémový čas v milisekundách
     * @return datový snímek {@link HdoSnapshot}
     */
    @NonNull
    public static HdoSnapshot loadHdoSnapshot(@NonNull Context context,
                                              @NonNull SubscriptionPointModel point,
                                              long nowMillis) {
        Context appContext = context.getApplicationContext();
        long timeShift = loadTimeShift(appContext, point.getId());

        ArrayList<HdoModel> allHdoModels = new ArrayList<>();
        String selectedMainRelay = "";

        DataHdoSource hdoSource = new DataHdoSource(appContext);
        try {
            hdoSource.open();
            allHdoModels = hdoSource.loadHdo(point.getTableHDO());
            ArrayList<String> allReles = hdoSource.getReles(point.getTableHDO());
            ShPHdo shPHdo = new ShPHdo(appContext);
            selectedMainRelay = shPHdo.get(ShPHdo.ARG_MAIN_HDO_RELAY, "");
            if ((selectedMainRelay.isEmpty() || !allReles.contains(selectedMainRelay)) && !allReles.isEmpty()) {
                selectedMainRelay = allReles.get(0);
            }
        } catch (Exception ignored) {
        } finally {
            hdoSource.close();
        }

        return buildHdoSnapshot(allHdoModels, selectedMainRelay, nowMillis, timeShift, TimeZone.getDefault());
    }


    /**
     * Sestaví datový snímek HDO z načtených modelů včetně vyhodnocení stavu až tří relé.
     *
     * @param allHdoModels      všechny HDO modely odběrného místa
     * @param selectedMainRelay vybrané hlavní relé (může být prázdné nebo {@code null})
     * @param nowMillis         referenční systémový čas v ms
     * @param timeShiftMillis   časový posun elektroměru v ms
     * @param timeZone          časové pásmo pro formátování času přepnutí
     * @return sestavený {@link HdoSnapshot}
     */
    @NonNull
    public static HdoSnapshot buildHdoSnapshot(@Nullable List<HdoModel> allHdoModels,
                                               @Nullable String selectedMainRelay,
                                               long nowMillis,
                                               long timeShiftMillis,
                                               @NonNull TimeZone timeZone) {
        List<HdoModel> safeModels = (allHdoModels != null) ? new ArrayList<>(allHdoModels) : Collections.emptyList();
        if (safeModels.isEmpty()) {
            return new HdoSnapshot(
                    Collections.emptyList(),
                    false,
                    false,
                    -1L,
                    timeShiftMillis,
                    DashboardHdoFormatHelper.FormattedHdoSwitch.invalid(),
                    Collections.emptyList()
            );
        }

        Map<String, ArrayList<HdoModel>> modelsByRelay = new LinkedHashMap<>();
        for (HdoModel model : safeModels) {
            String key = model.getRele() != null ? model.getRele() : "";
            ArrayList<HdoModel> list = modelsByRelay.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(model);
        }

        String resolvedMainRelay = (selectedMainRelay != null && modelsByRelay.containsKey(selectedMainRelay))
                ? selectedMainRelay
                : modelsByRelay.keySet().iterator().next();

        long meterNowMillis = nowMillis + timeShiftMillis;
        Calendar meterCal = Calendar.getInstance(timeZone);
        meterCal.setTimeInMillis(meterNowMillis);

        // Seřadíme relé shodně s pořadím prstenců ciferníku (TUV -> TAR -> PV)
        List<String> orderedRelayNames = new ArrayList<>(modelsByRelay.keySet());
        orderedRelayNames.sort(Comparator
                .comparingInt((String name) -> classifyRelayType(name).ordinal())
                .thenComparing((String name) -> !name.equals(resolvedMainRelay))
                .thenComparing(name -> name.toLowerCase(Locale.ROOT)));

        if (orderedRelayNames.size() > MAX_WIDGET_HDO_RELAYS) {
            List<String> capped = new ArrayList<>(orderedRelayNames.subList(0, MAX_WIDGET_HDO_RELAYS));
            if (!capped.contains(resolvedMainRelay)) {
                capped.set(MAX_WIDGET_HDO_RELAYS - 1, resolvedMainRelay);
                capped.sort(Comparator
                        .comparingInt((String name) -> classifyRelayType(name).ordinal())
                        .thenComparing((String name) -> !name.equals(resolvedMainRelay))
                        .thenComparing(name -> name.toLowerCase(Locale.ROOT)));
            }
            orderedRelayNames = capped;
        }

        Map<HdoRelayType, Integer> typeCounts = new EnumMap<>(HdoRelayType.class);
        for (String relayName : orderedRelayNames) {
            typeCounts.merge(classifyRelayType(relayName), 1, Integer::sum);
        }

        List<HdoRelaySnapshot> relaySnapshots = new ArrayList<>(orderedRelayNames.size());
        HdoRelaySnapshot mainRelaySnapshot = null;

        for (String relayName : orderedRelayNames) {
            ArrayList<HdoModel> relayModels = modelsByRelay.get(relayName);
            if (relayModels == null || relayModels.isEmpty()) {
                continue;
            }
            HdoRelayType relayType = classifyRelayType(relayName);
            boolean isMain = relayName.equals(resolvedMainRelay);
            boolean isActive = HdoTime.checkHdo(relayModels, meterCal);
            int nextType = isActive ? HdoAlarmScheduler.TYPE_END : HdoAlarmScheduler.TYPE_START;
            long nextTriggerMillis = HdoAlarmScheduler.findNextTriggerForModels(relayModels, nextType, nowMillis, timeShiftMillis);
            DashboardHdoFormatHelper.FormattedHdoSwitch formatted = (nextTriggerMillis > 0L)
                    ? DashboardHdoFormatHelper.formatNextSwitch(nextTriggerMillis + timeShiftMillis, meterNowMillis, timeZone)
                    : DashboardHdoFormatHelper.FormattedHdoSwitch.invalid();

            boolean uniqueType = Integer.valueOf(1).equals(typeCounts.get(relayType));
            String displayLabel = formatRelayDisplayLabel(relayName, relayType, uniqueType);

            HdoRelaySnapshot snapshot = new HdoRelaySnapshot(
                    relayName,
                    displayLabel,
                    relayType,
                    isActive,
                    isMain,
                    nextTriggerMillis,
                    formatted
            );
            relaySnapshots.add(snapshot);
            if (isMain) {
                mainRelaySnapshot = snapshot;
            }
        }

        if (mainRelaySnapshot == null && !relaySnapshots.isEmpty()) {
            mainRelaySnapshot = relaySnapshots.get(0);
        }

        if (mainRelaySnapshot == null) {
            return new HdoSnapshot(
                    safeModels,
                    false,
                    false,
                    -1L,
                    timeShiftMillis,
                    DashboardHdoFormatHelper.FormattedHdoSwitch.invalid(),
                    Collections.emptyList()
            );
        }

        return new HdoSnapshot(
                safeModels,
                true,
                mainRelaySnapshot.isActive(),
                mainRelaySnapshot.nextTriggerMillis(),
                timeShiftMillis,
                mainRelaySnapshot.formattedSwitch(),
                Collections.unmodifiableList(relaySnapshots)
        );
    }


    /**
     * Sestaví stručný a jednoznačný popisek relé pro omezený prostor widgetu.
     *
     * @param relayName  surový název relé z databáze
     * @param relayType  klasifikovaný typ relé
     * @param uniqueType {@code true}, pokud je tento typ relé mezi zobrazenými relé jedinečný
     * @return čitelný popisek relé
     */
    @NonNull
    public static String formatRelayDisplayLabel(@Nullable String relayName,
                                                 @NonNull HdoRelayType relayType,
                                                 boolean uniqueType) {
        String trimmed = relayName != null ? relayName.trim() : "";
        if (trimmed.isEmpty()) {
            return relayType.name();
        }
        if (trimmed.length() <= MAX_SHORT_RELAY_LABEL_LENGTH) {
            return trimmed;
        }
        if (!uniqueType) {
            return trimmed;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return switch (relayType) {
            case TUV -> {
                if (upper.contains("TUV")) {
                    yield "TUV";
                }
                if (upper.contains("AKU")) {
                    yield "AKU";
                }
                if (upper.contains("BOJLER")) {
                    yield "Bojler";
                }
                yield "TUV";
            }
            case PV -> {
                if (upper.contains("PV")) {
                    yield "PV";
                }
                if (upper.contains("FVE")) {
                    yield "FVE";
                }
                yield "PV";
            }
            case TAR -> "TAR";
        };
    }


    /**
     * Načte a vypočítá datový snímek spotřeby a finanční bilance pro aktivní fakturační období
     * konkrétního odběrného místa (shodně s novým přehledem: upřednostní nevyfakturované období
     * od poslední faktury {@code idFak == -1L}, případně nejnovější fakturu).
     *
     * @param context aplikační kontext
     * @param point   odběrné místo
     * @return datový snímek {@link PeriodSnapshot}
     */
    @NonNull
    public static PeriodSnapshot loadPeriodSnapshot(@NonNull Context context,
                                                    @NonNull SubscriptionPointModel point) {
        Context appContext = context.getApplicationContext();
        int[] userColors = loadUserVtNtColors(appContext);
        PriceListModel fallbackPriceList = loadCurrentPriceList(appContext, point);

        DataInvoiceSource invoiceSource = new DataInvoiceSource(appContext);
        DataPriceListSource priceListSource = new DataPriceListSource(appContext);
        try {
            invoiceSource.open();
            priceListSource.open();

            ArrayList<InvoiceListModel> invoiceLists = invoiceSource.loadInvoiceLists(point);
            InvoiceListModel activePeriod = selectActivePeriod(invoiceLists);
            if (activePeriod == null) {
                boolean isSingle = fallbackPriceList != null && isSingleTariffRate(fallbackPriceList.getRate());
                return new PeriodSnapshot(
                        false,
                        0L,
                        0L,
                        0.0,
                        0.0,
                        0.0,
                        isSingle,
                        0.0,
                        0.0,
                        0.0,
                        InvoiceBalanceHelper.BalanceState.BALANCED,
                        userColors[0],
                        userColors[1]
                );
            }

            long idFak = activePeriod.getIdFak();
            String table = (idFak == -1L) ? point.getTableTED() : point.getTableFAK();
            return computePeriodSummary(
                    appContext,
                    invoiceSource,
                    priceListSource,
                    point,
                    idFak,
                    table,
                    activePeriod.getMinDate(),
                    activePeriod.getMaxDate(),
                    fallbackPriceList,
                    userColors[0],
                    userColors[1]
            );
        } catch (Exception e) {
            return new PeriodSnapshot(
                    false,
                    0L,
                    0L,
                    0.0,
                    0.0,
                    0.0,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    InvoiceBalanceHelper.BalanceState.BALANCED,
                    userColors[0],
                    userColors[1]
            );
        } finally {
            priceListSource.close();
            invoiceSource.close();
        }
    }


    /**
     * Vyhledá aktuální ceník přiřazený k zadanému odběrnému místu (poslední odečet -> TED -> FAK).
     *
     * @param context aplikační kontext
     * @param point   odběrné místo
     * @return nalezený {@link PriceListModel}, nebo {@code null}
     */
    @Nullable
    public static PriceListModel loadCurrentPriceList(@NonNull Context context,
                                                      @NonNull SubscriptionPointModel point) {
        long priceListId = resolveCurrentPriceListId(context, point);
        if (priceListId <= 0L) {
            return null;
        }
        DataPriceListSource priceListSource = new DataPriceListSource(context.getApplicationContext());
        try {
            priceListSource.open();
            return priceListSource.readPrice(priceListId);
        } catch (Exception e) {
            return null;
        } finally {
            priceListSource.close();
        }
    }


    /**
     * Zjistí ID aktuálního ceníku odběrného místa (poslední odečet -> TED -> FAK).
     *
     * @param context aplikační kontext
     * @param point   odběrné místo
     * @return ID ceníku nebo {@code -1L}
     */
    public static long resolveCurrentPriceListId(@NonNull Context context,
                                                 @NonNull SubscriptionPointModel point) {
        Context appContext = context.getApplicationContext();
        long priceListId = getPriceListId(point, appContext);

        if (priceListId > 0L) {
            return priceListId;
        }

        DataInvoiceSource invoiceSource = new DataInvoiceSource(appContext);
        try {
            invoiceSource.open();
            InvoiceModel lastTed = invoiceSource.loadLastInvoiceByDateFromAll(point.getTableTED());
            if (lastTed != null && lastTed.getIdPriceList() > 0L) {
                return lastTed.getIdPriceList();
            }
            InvoiceModel lastFak = invoiceSource.loadLastInvoiceByDateFromAll(point.getTableFAK());
            if (lastFak != null && lastFak.getIdPriceList() > 0L) {
                return lastFak.getIdPriceList();
            }
        } catch (Exception ignored) {
        } finally {
            invoiceSource.close();
        }

        return -1L;
    }


    private static long getPriceListId(@NonNull SubscriptionPointModel point, Context appContext) {
        long priceListId = -1L;

        DataMonthlyReadingSource readingSource = new DataMonthlyReadingSource(appContext);
        try {
            readingSource.open();
            MonthlyReadingModel lastReading = readingSource.loadLastMonthlyReadingByDate(point.getTableO());
            if (lastReading != null && lastReading.getPriceListId() > 0L) {
                priceListId = lastReading.getPriceListId();
            }
        } catch (Exception ignored) {
        } finally {
            readingSource.close();
        }
        return priceListId;
    }


    /**
     * Načte uživatelské barvy VT a NT z databáze nastavení (se záložními barvami motivu
     * respektujícími přepínač dynamických barev).
     *
     * @param context aplikační kontext
     * @return dvouprvkové pole {@code [colorVt, colorNt]}
     */
    @NonNull
    public static int[] loadUserVtNtColors(@NonNull Context context) {
        WidgetThemePalette palette = WidgetThemePalette.resolve(context);
        int baseFallbackVt = ContextCompat.getColor(context, R.color.dashboard_teal_vt);
        int fallbackVt = palette.isDynamicColorsActive()
                ? MaterialColors.harmonize(baseFallbackVt, palette.primary())
                : baseFallbackVt;
        int fallbackNt = palette.isDynamicColorsActive()
                ? palette.primary()
                : ContextCompat.getColor(context, R.color.dashboard_teal_nt);
        DataSettingsSource settingsSource = new DataSettingsSource(context.getApplicationContext());
        try {
            settingsSource.open();
            int[] loaded = settingsSource.loadColorVTNT();
            if (loaded != null && loaded.length >= 2) {
                return loaded;
            }
        } catch (Exception ignored) {
        } finally {
            settingsSource.close();
        }
        return new int[]{fallbackVt, fallbackNt};
    }


    private static long loadTimeShift(@NonNull Context context, long subscriptionPointId) {
        DataSettingsSource settingsSource = new DataSettingsSource(context.getApplicationContext());
        try {
            settingsSource.open();
            return settingsSource.loadTimeShift(subscriptionPointId);
        } catch (Exception ignored) {
            return 0L;
        } finally {
            settingsSource.close();
        }
    }


    @Nullable
    private static InvoiceListModel selectActivePeriod(@Nullable ArrayList<InvoiceListModel> invoiceLists) {
        if (invoiceLists == null || invoiceLists.isEmpty()) {
            return null;
        }
        for (InvoiceListModel item : invoiceLists) {
            if (item.getIdFak() == -1L && item.getMinDate() > 0L && item.getMaxDate() > 0L) {
                return item;
            }
        }
        for (InvoiceListModel item : invoiceLists) {
            if (item.getMinDate() > 0L && item.getMaxDate() > 0L) {
                return item;
            }
        }
        return null;
    }


    @NonNull
    private static PeriodSnapshot computePeriodSummary(@NonNull Context appContext,
                                                       @NonNull DataInvoiceSource invoiceSource,
                                                       @NonNull DataPriceListSource priceListSource,
                                                       @NonNull SubscriptionPointModel point,
                                                       long idFak,
                                                       @NonNull String table,
                                                       long minDate,
                                                       long maxDate,
                                                       @Nullable PriceListModel fallbackPriceList,
                                                       int colorVt,
                                                       int colorNt) {
        ArrayList<InvoiceModel> records = invoiceSource.loadInvoices(idFak, table);
        double advances = invoiceSource.sumAdvancePaymentsOnly(idFak, point.getTablePLATBY());
        if (advances <= 0.0) {
            advances = invoiceSource.sumPayment(idFak, point.getTablePLATBY());
        }
        double discountWithTax = invoiceSource.sumDiscountWithTax(idFak, point.getTablePLATBY());

        if (records == null || records.isEmpty()) {
            boolean isSingle = fallbackPriceList != null && isSingleTariffRate(fallbackPriceList.getRate());
            long roundedBal = Math.round(advances);
            return new PeriodSnapshot(
                    true,
                    minDate,
                    maxDate,
                    0.0,
                    0.0,
                    0.0,
                    isSingle,
                    advances,
                    0.0,
                    advances,
                    InvoiceBalanceHelper.getBalanceState(roundedBal),
                    colorVt,
                    colorNt
            );
        }

        PozeModel poze = Calculation.getPoze(records, point.getCountPhaze(), point.getPhaze(), appContext);
        double totalDph = 0.0;
        double totalVtMwh = 0.0;
        double totalNtMwh = 0.0;
        boolean hasAnyRate = false;
        boolean allSingleTariff = true;

        for (int i = 0; i < records.size(); i++) {
            InvoiceModel invoice = records.get(i);
            PriceListModel priceList = priceListSource.readPrice(invoice.getIdPriceList());
            if (priceList == null) {
                priceList = new PriceListModel();
            }
            if (priceList.getRate() != null && !priceList.getRate().trim().isEmpty()) {
                hasAnyRate = true;
                if (!isSingleTariffRate(priceList.getRate())) {
                    allSingleTariff = false;
                }
            }
            PriceListModel regulPriceList = new PriceListRegulBuilder(priceList, invoice).getRegulPriceList();
            String dateOf = ViewHelper.convertLongToDate(invoice.getDateFrom());
            String dateTo = ViewHelper.convertLongToDate(invoice.getDateTo());
            double differentDate = Calculation.differentMonth(dateOf, dateTo, DifferenceDate.TypeDate.INVOICE);

            double[] price;
            synchronized (CALCULATION_LOCK) {
                price = Calculation.calculatePriceWithoutPozeMwH(regulPriceList, point);
            }

            double vtMwh = Math.max(0.0, (invoice.getVtEnd() - invoice.getVtStart()) / 1000.0);
            double ntMwh = Math.max(0.0, (invoice.getNtEnd() - invoice.getNtStart()) / 1000.0);
            double ntVtMwh = vtMwh + ntMwh;

            totalVtMwh += vtMwh;
            totalNtMwh += ntMwh;

            price[0] *= vtMwh;
            price[1] *= ntMwh;
            price[2] *= differentDate;

            if (poze.getTypePoze() == PozeModel.TypePoze.POZE2) {
                if (priceList.getValidityYear() < NEW_POZE_YEAR) {
                    price[3] = ntVtMwh * regulPriceList.getOze();
                } else {
                    price[3] = ntVtMwh * (priceList.getValidityYear() == 2026
                            ? regulPriceList.getPoze1()
                            : regulPriceList.getPoze2());
                }
            } else {
                price[3] = point.getCountPhaze() * point.getPhaze() * differentDate * regulPriceList.getPoze1();
            }

            double otherServices = invoice.getOtherServices() * differentDate;
            for (double p : price) {
                totalDph += p + (p * priceList.getVat() / 100.0);
            }
            totalDph += otherServices + (otherServices * priceList.getVat() / 100.0);

            if (i == records.size() - 1) {
                double discountWithoutTax = invoiceSource.sumDiscountWithoutTax(idFak, point.getTablePLATBY());
                double discountWithTaxInTotal = invoiceSource.sumDiscountWithTaxInTotal(idFak, point.getTablePLATBY());
                totalDph -= discountWithoutTax + (discountWithoutTax * priceList.getVat() / 100.0) + discountWithTaxInTotal;
            }
        }

        boolean isSingleTariff = hasAnyRate && allSingleTariff && (totalNtMwh <= 0.0);
        if (!hasAnyRate && totalNtMwh <= 0.0) {
            isSingleTariff = fallbackPriceList != null && isSingleTariffRate(fallbackPriceList.getRate());
        }

        double vtKwh = totalVtMwh * 1000.0;
        double ntKwh = isSingleTariff ? 0.0 : totalNtMwh * 1000.0;
        double totalKwh = vtKwh + ntKwh;
        double costsWithTax = Math.max(0.0, totalDph - discountWithTax);
        double balanceKc = advances - costsWithTax;
        InvoiceBalanceHelper.BalanceState balanceState = InvoiceBalanceHelper.getBalanceState(Math.round(balanceKc));

        return new PeriodSnapshot(
                true,
                minDate,
                maxDate,
                vtKwh,
                ntKwh,
                totalKwh,
                isSingleTariff,
                advances,
                costsWithTax,
                balanceKc,
                balanceState,
                colorVt,
                colorNt
        );
    }


    private static long startOfDayMillis(long millis, @NonNull TimeZone timeZone) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    private static long endOfDayMillis(long millis, @NonNull TimeZone timeZone) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

}
