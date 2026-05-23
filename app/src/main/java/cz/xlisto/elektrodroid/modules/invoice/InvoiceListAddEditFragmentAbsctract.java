package cz.xlisto.elektrodroid.modules.invoice;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import cz.xlisto.elektrodroid.ownview.LabelEditText;

/**
 * Abstraktní DialogFragment jako základ pro dialogy přidání a úpravy faktury v seznamu faktur.
 * Uchovává společné proměnné (ID odběrného místa, ID faktury, číslo faktury) a
 * obstarává jejich načtení z argumentů a uložení do stavu instance.
 * Xlisto 30.01.2023 19:59
 */
public abstract class InvoiceListAddEditFragmentAbsctract extends DialogFragment {

    /** Klíč pro ID odběrného místa v Bundle argumentů. */
    static final String ID_SUBSCRIPTION_POINT = "idSubscriptionPoint";
    /** Klíč pro ID faktury v Bundle argumentů. */
    static final String ID_INVOICE = "idInvoice";
    /** Klíč pro číslo faktury v Bundle argumentů. */
    static final String NUMBER_INVOICE = "number_invoice";
    /** ID odběrného místa. */
    long idSubsriptionPoint;
    /** ID faktury. */
    long idInvoice;
    /** Číslo faktury zadané uživatelem. */
    String numberInvoice;
    /** Vstupní pole pro číslo faktury. */
    LabelEditText letNumberInvoice;




    /**
     * Načte argumenty předané do fragmentu a inicializuje sdílené proměnné.
     *
     * @param savedInstanceState uložený stav instance (může být null)
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            idSubsriptionPoint = bundle.getLong(ID_SUBSCRIPTION_POINT);
            idInvoice = bundle.getLong(ID_INVOICE);
            numberInvoice = bundle.getString(NUMBER_INVOICE, "");
        }


    }

    /**
     * Uloží aktuální stav proměnných do Bundle při změně konfigurace nebo přechodu do pozadí.
     *
     * @param outState Bundle, do kterého se uloží stav
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ID_SUBSCRIPTION_POINT,idSubsriptionPoint);
        outState.putLong(ID_INVOICE,idInvoice);
        outState.putString(NUMBER_INVOICE, letNumberInvoice.getText());
    }


}
