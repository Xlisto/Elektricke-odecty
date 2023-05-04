package cz.xlisto.odecty.models;

import androidx.annotation.NonNull;
import cz.xlisto.odecty.utils.Round;

/**
 * Kontejner pro zobrazení jednotlivých detailu faktury
 * Xlisto 19.03.2023 13:49
 */
public class SummaryInvoiceModel {
    private static final String TAG = "SummaryInvoiceModel";
    private final long dateOf, dateTo;
    private double amount;
    private final double unitPrice;
    private final Unit unit;
    private final Title title;


    public SummaryInvoiceModel(long dateOf, long dateTo, double amount, double unitPrice, Unit unit, Title title) {
        this.dateOf = dateOf;
        this.dateTo = dateTo;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.unit = unit;
        this.title = title;
    }


    public long getDateOf() {
        return dateOf;
    }

    public long getDateTo() {
        return dateTo;
    }


    public double getAmount() {
        return Round.round(amount,3);
    }


    public double getUnitPrice() {
        return unitPrice;
    }


    public double getTotalPrice() {
        int round = 2;
        return Round.round(unitPrice*amount, round);
    }


    public Unit getUnit() {
        return unit;
    }


    public Title getTitle() {
        return title;
    }


    public void addAmount(double amount) {
        this.amount = this.amount+amount;
    }


    public enum Unit {
        MWH("MWh"),
        MONTH("Měsíc");

        private final String text;
        Unit(final String text){
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }
    

    public enum Title{
        VT("Dodané množství vysoký tarif"),
        NT("Dodané množství nízký tarif"),
        PAY("Stálý plat"),
        TAX("Daň z elektřiny"),
        VT_DIST("Cena za distrib. množství elektřiny ve vysokém tarifu"),
        NT_DIST("Cena za distrib. množství elektřiny v nízkém tarifu"),
        CIRCUIT_BREAKER("Cena za příkon podle hodnoty hl. jističe před elekt."),
        SYS_SERVICES("Pevná cena za systémové služby"),
        OTE("Cena za činnosti operátora trhu"),
        POZE("Složka ceny na podporu el. z podpor. zdrojů energie");

        private final String text;
        Title(final String text){
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }

}
