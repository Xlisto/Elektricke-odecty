package cz.xlisto.elektrodroid.utils;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import java.util.Objects;

import cz.xlisto.elektrodroid.R;

/**
 * Pomocník pro jednotné zobrazení widgetu se stavem bilance.
 * <p>
 * Třída centralizuje vykreslení textu i pozadí (přeplatek/nedoplatek/vyrovnáno),
 * aby byl stejný vizuál použit napříč moduly faktur i měsíčních odečtů.
 */
public final class BalanceStatusUiHelper {

    private BalanceStatusUiHelper() {
    }

    /**
     * Zobrazí v předaném TextView stav bilance pomocí stejného vizuálu jako ve fakturách.
     * Text je zkonstruován ze string resources podle znaménka hodnoty bilance.
     *
     * @param textView            cílový widget pro zobrazení stavu
     * @param context             kontext pro načtení string resources
     * @param balance             hodnota bilance (kladná = přeplatek, záporná = nedoplatek)
     * @param overpaymentTextRes  resource textu pro přeplatek (očekává částku)
     * @param underpaymentTextRes resource textu pro nedoplatek (očekává částku)
     * @param balancedTextRes     resource textu pro vyrovnaný stav
     */
    public static void showBalanceStatus(TextView textView,
                                         Context context,
                                         double balance,
                                         @StringRes int overpaymentTextRes,
                                         @StringRes int underpaymentTextRes,
                                         @StringRes int balancedTextRes) {
        InvoiceBalanceHelper.BalanceState state = InvoiceBalanceHelper.getBalanceState(balance);
        String text;

        switch (state) {
            case OVERPAYMENT -> text = context.getString(overpaymentTextRes, InvoiceBalanceHelper.getAbsoluteBalance(balance));
            case UNDERPAYMENT -> text = context.getString(underpaymentTextRes, InvoiceBalanceHelper.getAbsoluteBalance(balance));
            default -> text = context.getString(balancedTextRes);
        }

        showBalanceStatus(textView, state, text);
    }

    /**
     * Zobrazí v předaném TextView stav bilance s již připraveným HTML textem.
     * Vstupní text může obsahovat HTML formátování (např. {@code <b>} nebo {@code <br/>}).
     *
     * @param textView cílový widget pro zobrazení stavu
     * @param state    stav bilance určující barvu pozadí widgetu
     * @param text     výsledný text widgetu (HTML)
     */
    public static void showBalanceStatus(TextView textView,
                                         InvoiceBalanceHelper.BalanceState state,
                                         String text) {
        int backgroundResId;

        if (Objects.requireNonNull(state) == InvoiceBalanceHelper.BalanceState.UNDERPAYMENT) {
            backgroundResId = R.drawable.shape_montly_reading_no;
        } else {
            backgroundResId = R.drawable.shape_monthly_reading_yes;
        }

        textView.setBackgroundResource(backgroundResId);
        textView.setText(Html.fromHtml(text));
        textView.setVisibility(View.VISIBLE);
    }

    /**
     * Skryje widget se stavem bilance.
     *
     * @param textView widget, který se má skrýt
     */
    public static void hideBalanceStatus(TextView textView) {
        textView.setVisibility(View.GONE);
    }
}


