package cz.xlisto.cenik.models;

import android.view.View;
import android.widget.TextView;

import java.util.Calendar;

import androidx.annotation.NonNull;
import cz.xlisto.cenik.format.DecimalFormatHelper;
import cz.xlisto.cenik.ownview.ViewHelper;

/**
 * Xlisto 15.02.2023 21:59
 */
public class PaymentModel {
    private static final String TAG = "PaymentModel";

    private long id;
    private long idFak;
    private long date;
    private double payment;
    private int typePayment;

    public PaymentModel(long id, long idFak, long date, double payment, int typePayment) {
        this.id = id;
        this.idFak = idFak;
        this.date = date;
        this.payment = payment;
        this.typePayment = typePayment;
    }

    public long getId() {
        return id;
    }

    public long getIdFak() {
        return idFak;
    }

    public long getDate() {
        return date;
    }

    public double getPayment() {
        return payment;
    }

    public int getTypePayment() {
        return typePayment;
    }

    public String getTypePaymentString(){
        if(typePayment==1) return "Doplatek";
        if(typePayment==2) return "Automatická";
        if(typePayment==3) return "Sleva";
        return "Měsíční záloha";
    }

    /**
     * Výpočet slevy na DPH za měsíce listopad a prosinec v roce 2021
     */
    //TODO: sleva na DPH za měsíce listopad a prosinec v roce 2021 ve fakturách
    public double getDiscountDPH() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        if(year == 2021 && month >=10) {
            //return (sum / 121 * 21);
            return (payment * 0.21);
        }
        return 0.0;
    }

    @NonNull
    @Override
    public String toString() {
        return "PaymentModel\nID: "+getId()+"\nČástka: "+getPayment()+"\nID_faktury: "+getIdFak()+"\nDatum: "+ ViewHelper.convertLongToTime(getDate())+" ("+getDate()+")\nType payment: "+getTypePayment();
    }

    public static void getDiscountDPHText(double discount, TextView tv) {
        if(discount>0) {
            tv.setText("Sleva na DPH ve výši " + DecimalFormatHelper.df2.format(discount) + " Kč");
            tv.setVisibility(View.VISIBLE);
        } else
            tv.setVisibility(View.GONE);
    }
}
