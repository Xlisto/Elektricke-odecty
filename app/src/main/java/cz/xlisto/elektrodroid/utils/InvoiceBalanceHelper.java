package cz.xlisto.elektrodroid.utils;

/**
 * Pomocná utilitní třída pro vyhodnocení stavu bilance faktury.
 * Slouží k jednotnému určení, zda jde o přeplatek, nedoplatek nebo vyrovnaný stav,
 * a k převodu hodnoty bilance na absolutní částku pro zobrazení v UI.
 */
public final class InvoiceBalanceHelper {

    /**
     * Soukromý konstruktor zabraňuje vytvoření instance utilitní třídy.
     */
    private InvoiceBalanceHelper() {
    }

    /**
     * Stav bilance faktury podle znaménka výsledné částky.
     */
    public enum BalanceState {
        /** Faktura končí přeplatkem (kladná bilance). */
        OVERPAYMENT,
        /** Faktura končí nedoplatkem (záporná bilance). */
        UNDERPAYMENT,
        /** Faktura je vyrovnaná (nulová bilance). */
        BALANCED
    }

    /**
     * Určí stav bilance podle znaménka částky.
     *
     * @param balance hodnota bilance
     * @return stav bilance (přeplatek, nedoplatek nebo vyrovnáno)
     */
    public static BalanceState getBalanceState(double balance) {
        if (balance > 0) {
            return BalanceState.OVERPAYMENT;
        }
        if (balance < 0) {
            return BalanceState.UNDERPAYMENT;
        }
        return BalanceState.BALANCED;
    }

    /**
     * Vrátí absolutní hodnotu bilance pro přehledné zobrazení částky v textu.
     *
     * @param balance hodnota bilance
     * @return absolutní hodnota bilance
     */
    public static double getAbsoluteBalance(double balance) {
        return Math.abs(balance);
    }
}


