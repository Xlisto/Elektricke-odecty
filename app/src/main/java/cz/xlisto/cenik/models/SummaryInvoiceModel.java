package cz.xlisto.cenik.models;

import androidx.annotation.NonNull;
import cz.xlisto.cenik.utils.Round;

/**
 * Xlisto 19.03.2023 13:49
 */
public class SummaryInvoiceModel {
    private static final String TAG = "SummaryInvoiceModel";
    private int round = 2;
    private long dateOf, dateTo;
    private double amount, unitPrice, totalPrice;
    private Unit unit;
    private Title title;

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
        return Round.round(unitPrice*amount,round);
    }

    public Unit getUnit() {
        return unit;
    }

    public Title getTitle() {
        return title;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public void addAmount(double amount) {
        this.amount = this.amount+amount;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
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
        CIRCUT_BREAKER("Cena za příkon podle hodnoty hl. jističe před elekt."),
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
