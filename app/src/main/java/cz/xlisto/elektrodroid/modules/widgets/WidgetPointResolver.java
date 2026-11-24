package cz.xlisto.elektrodroid.modules.widgets;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * Bezpečně vyhledává a ověřuje odběrné místo přiřazené konkrétní instanci widgetu.
 * <p>
 * Zajišťuje ochranu proti tichému přeřazení widgetu na jiné odběrné místo po obnově databáze ze zálohy:
 * kromě databázového {@code _id} vždy kontroluje také stabilní identifikátor tabulek {@code milins}.
 * Pokud bylo odběrné místo smazáno nebo v obnovené databázi neexistuje jednoznačná shoda, vrátí stav
 * {@link ResolutionStatus#MISSING} a nikdy se nevrací ke globálně zvolenému odběrnému místu aplikace.
 */
public final class WidgetPointResolver {

    private WidgetPointResolver() {
    }


    /**
     * Stav vyhodnocení vazby widgetu na odběrné místo.
     */
    public enum ResolutionStatus {
        /**
         * Widget ještě nemá uloženou konfiguraci.
         */
        UNCONFIGURED,
        /**
         * Odběrné místo bylo bezpečně nalezeno a ověřeno.
         */
        RESOLVED,
        /**
         * Přiřazené odběrné místo již neexistuje nebo po obnově databáze nesouhlasí jeho identita.
         */
        MISSING
    }


    /**
     * Výsledek vyhledání odběrného místa pro widget.
     *
     * @param status  stav vyhodnocení vazby
     * @param point   nalezené odběrné místo (nenulové pouze při {@link ResolutionStatus#RESOLVED})
     * @param rebound {@code true}, pokud byla shoda nalezena přes {@code milins} pod novým {@code _id}
     *                (např. po obnově databáze ze zálohy) a doporučuje se aktualizovat uložené {@code _id}
     */
    public record ResolutionResult(@NonNull ResolutionStatus status,
                                   @Nullable SubscriptionPointModel point,
                                   boolean rebound) {

        @NonNull
        public static ResolutionResult unconfigured() {
            return new ResolutionResult(ResolutionStatus.UNCONFIGURED, null, false);
        }


        @NonNull
        public static ResolutionResult missing() {
            return new ResolutionResult(ResolutionStatus.MISSING, null, false);
        }


        @NonNull
        public static ResolutionResult resolved(@NonNull SubscriptionPointModel point, boolean rebound) {
            return new ResolutionResult(ResolutionStatus.RESOLVED, point, rebound);
        }

    }


    /**
     * Vyhodnotí vazbu konfigurace widgetu proti předanému seznamu odběrných míst.
     *
     * @param config    konfigurace widgetu (může být {@code null})
     * @param allPoints seznam všech aktuálně existujících odběrných míst v databázi
     * @return výsledek ověření {@link ResolutionResult}
     */
    @NonNull
    public static ResolutionResult resolve(@Nullable WidgetConfig config,
                                           @NonNull List<SubscriptionPointModel> allPoints) {
        if (config == null || config.getSubscriptionPointId() <= 0L) {
            return ResolutionResult.unconfigured();
        }

        long savedId = config.getSubscriptionPointId();
        long savedMilins = config.getSubscriptionPointMilins();

        // 1. Primární kontrola podle databázového _id se současným ověřením milins
        for (SubscriptionPointModel candidate : allPoints) {
            if (candidate.getId() == savedId) {
                if (savedMilins <= 0L || candidate.getMilins() == savedMilins) {
                    return ResolutionResult.resolved(candidate, false);
                }
                // Stejné _id má po obnově databáze jiné odběrné místo – nesmí se použít!
                break;
            }
        }

        // 2. Bezpečné dohledání unikátní shody podle stabilního milins (např. po obnově DB se změnou _id)
        if (savedMilins > 0L) {
            SubscriptionPointModel uniqueMatch = null;
            int matchCount = 0;
            for (SubscriptionPointModel candidate : allPoints) {
                if (candidate.getMilins() == savedMilins) {
                    uniqueMatch = candidate;
                    matchCount++;
                }
            }
            if (matchCount == 1) {
                return ResolutionResult.resolved(uniqueMatch, true);
            }
        }

        return ResolutionResult.missing();
    }


    /**
     * Načte odběrná místa z databáze, vyhodnotí vazbu widgetu a v případě bezpečného znovunapojení
     * přes {@code milins} (nebo přejmenování) aktualizuje uloženou konfiguraci dané instance.
     *
     * @param context aplikační kontext
     * @param store   úložiště konfigurací widgetů
     * @param config  konfigurace konkrétní instance widgetu
     * @return výsledek ověření {@link ResolutionResult}
     */
    @NonNull
    public static ResolutionResult resolveAndRefreshBinding(@NonNull Context context,
                                                            @NonNull WidgetConfigStore store,
                                                            @Nullable WidgetConfig config) {
        if (config == null) {
            return ResolutionResult.unconfigured();
        }

        List<SubscriptionPointModel> allPoints = loadAllSubscriptionPoints(context);
        ResolutionResult result = resolve(config, allPoints);
        if (result.status() == ResolutionStatus.RESOLVED && result.point() != null) {
            SubscriptionPointModel point = result.point();
            boolean needsUpdate = result.rebound()
                    || config.getSubscriptionPointMilins() <= 0L
                    || !config.getSubscriptionPointNameSnapshot().equals(point.getName());
            if (needsUpdate) {
                store.saveConfig(config.withResolvedSubscriptionPoint(
                        point.getId(),
                        point.getMilins(),
                        point.getName()
                ));
            }
        }
        return result;
    }


    /**
     * Ověří přímou existenci odběrného místa zadaného dvojicí {@code pointId} a {@code expectedMilins}
     * (používá se např. při navigaci z widgetu do aplikace před otevřením formuláře nového odečtu).
     *
     * @param context        aplikační kontext
     * @param pointId        databázové ID odběrného místa
     * @param expectedMilins očekávaná časová značka {@code milins} (nebo {@code <= 0}, pokud se neověřuje)
     * @return ověřené {@link SubscriptionPointModel}, nebo {@code null}, pokud již neexistuje
     */
    @Nullable
    public static SubscriptionPointModel findVerifiedPoint(@NonNull Context context,
                                                           long pointId,
                                                           long expectedMilins) {
        if (pointId <= 0L) {
            return null;
        }
        List<SubscriptionPointModel> allPoints = loadAllSubscriptionPoints(context);
        WidgetConfig tempConfig = new WidgetConfig(0, WidgetType.NEW_READING, pointId, expectedMilins, "", true, true);
        ResolutionResult result = resolve(tempConfig, allPoints);
        return result.status() == ResolutionStatus.RESOLVED ? result.point() : null;
    }


    /**
     * Načte všechna odběrná místa z databáze se zaručeným uzavřením zdroje.
     *
     * @param context aplikační kontext
     * @return seznam všech odběrných míst (prázdný při chybě)
     */
    @NonNull
    public static List<SubscriptionPointModel> loadAllSubscriptionPoints(@NonNull Context context) {
        DataSubscriptionPointSource source = new DataSubscriptionPointSource(context.getApplicationContext());
        try {
            source.open();
            ArrayList<SubscriptionPointModel> list = source.loadSubscriptionPoints();
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            source.close();
        }
    }

}
