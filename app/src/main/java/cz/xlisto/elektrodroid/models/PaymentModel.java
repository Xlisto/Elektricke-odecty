package cz.xlisto.elektrodroid.models;


import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Calendar;

import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Reprezentuje jednu platbu ve faktuře.
 * Obsahuje identifikátory, datum, částku a typ platby spolu s pomocnými metodami
 * pro převod typu na text a výpočet případné slevy na DPH pro listopad a prosinec 2021.
 */
public class PaymentModel {

    private static final String TAG = "PaymentModel";
    private final long id;
    private final long idFak;
    private final long date;
    private final double payment;
    private final int typePayment;


    /**
     * Konstruktor modelu platby.
     *
     * @param id          unikátní identifikátor platby
     * @param idFak       identifikátor faktury, ke které platba patří
     * @param date        datum platby v milisekundách od epochy
     * @param payment     částka platby
     * @param typePayment typ platby (číselná konstanta používaná v aplikaci)
     */
    public PaymentModel(long id, long idFak, long date, double payment, int typePayment) {
        this.id = id;
        this.idFak = idFak;
        this.date = date;
        this.payment = payment;
        this.typePayment = typePayment;
    }


    /**
     * Vrací unikátní ID platby.
     *
     * @return id platby
     */
    public long getId() {
        return id;
    }


    /**
     * Vrací ID faktury, ke které platba patří.
     *
     * @return id faktury
     */
    public long getIdFak() {
        return idFak;
    }


    /**
     * Vrací datum platby ve formátu milisekund od epochy.
     *
     * @return datum platby (long)
     */
    public long getDate() {
        return date;
    }


    /**
     * Vrací částku platby.
     *
     * @return částka platby jako double
     */
    public double getPayment() {
        return payment;
    }


    /**
     * Vrací typ platby jako číselnou konstantu.
     *
     * @return typ platby (int)
     */
    public int getTypePayment() {
        return typePayment;
    }


    /**
     * Vrací popis typu platby jako čitelný řetězec.
     * Mapuje internální číselné hodnoty na lokalizované popisky.
     *
     * @return textový popisek typu platby
     */
    public String getTypePaymentString() {
        if (typePayment == 1) return "Doplatek";
        if (typePayment == 2) return "Automatická";
        if (typePayment == 3) return "Sleva bez DPH";
        if (typePayment == 4) return "Podpora státu (2000 nebo 3500)";
        if (typePayment == 5) return "Přeplatek, vrácení peněz";
        return "Měsíční záloha";
    }


    /**
     * Výpočet slevy na DPH za měsíce listopad a prosinec v roce 2021.
     * Pokud datum platby spadá do listopadu nebo prosince 2021, vypočte aproximovanou
     * hodnotu DPH (21 %) z částky platby.
     *
     * @return sleva na DPH (double) nebo 0.0 pokud se jedná o jiné období
     */
    //TODO: sleva na DPH za měsíce listopad a prosinec v roce 2021 ve fakturách. Vypočet z plateb
    public double getDiscountDPH() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        if (year == 2021 && month >= 10) {
            //return (sum / 121 * 21);
            return (payment * 0.21);
        }
        return 0.0;
    }


    /**
     * Vrací textovou reprezentaci objektu PaymentModel.
     * Zahrnuje id, částku, id faktury, datum (formátované) a typ platby.
     *
     * @return řetězcová reprezentace platby
     */
    @NonNull
    @Override
    public String toString() {
        return "PaymentModel\nID: " + getId() + "\nČástka: " + getPayment() + "\nID_faktury: " + getIdFak() + "\nDatum: " + ViewHelper.convertLongToDate(getDate()) + " (" + getDate() + ")\nType payment: " + getTypePayment();
    }


    /**
     * Pomocná statická metoda pro zobrazení slevy na DPH v TextView.
     * Pokud je hodnota slevy větší než 0, nastaví text a viditelnost TextView,
     * jinak TextView skryje.
     *
     * @param discount sleva na DPH (double)
     * @param tv       TextView, do kterého se má sleva zobrazit
     */
    public static void getDiscountDPHText(double discount, TextView tv) {
        if (discount > 0) {
            tv.setText(tv.getContext().getResources().getString(cz.xlisto.elektrodroid.R.string.discount_number, DecimalFormatHelper.df2.format(discount)));
            tv.setVisibility(View.VISIBLE);
        } else
            tv.setVisibility(View.GONE);
    }

}
