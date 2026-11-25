package cz.xlisto.elektrodroid.models;

import androidx.annotation.NonNull;

import cz.xlisto.elektrodroid.utils.Round;


/**
 * Kontejner pro zobrazení jednotlivých detailu faktury
 * Xlisto 19.03.2023 13:49
 */
public record SummaryInvoiceModel(long dateOf, long dateTo, double amount, double unitPrice,
                                  Unit unit, Title title) {

    @Override
    public double amount() {
        return Round.round(amount, 3);
    }


    public double getTotalPrice() {
        int round = 2;
        return Round.round(unitPrice * amount, round);
    }


    public enum Unit {
        MWH("MWh"),
        MONTH("měsíc");

        private final String text;


        Unit(final String text) {
            this.text = text;
        }


        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }


    public enum Title {
        VT("Dodané množství vysoký tarif"),
        NT("Dodané množství nízký tarif"),
        PAY("Stálý plat"),
        TAX("Daň z elektřiny"),
        VT_DIST("Cena za distrib. množství elektřiny ve vysokém tarifu"),
        NT_DIST("Cena za distrib. množství elektřiny v nízkém tarifu"),
        CIRCUIT_BREAKER("Cena za příkon podle hodnoty hl. jističe před elekt."),
        SYS_SERVICES("Cena za systémové služby"),
        OTE("Cena za činnosti operátora trhu"),
        INF("Provoz nesíťové infrastruktury"),
        POZE("Složka ceny na podporu el. z podpor. zdrojů energie");

        private final String text;


        Title(final String text) {
            this.text = text;
        }


        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }

}
