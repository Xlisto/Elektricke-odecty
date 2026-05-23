package cz.xlisto.elektrodroid.modules.invoice;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import cz.xlisto.elektrodroid.modules.payment.PaymentFragment;


/**
 * Adaptér pro ViewPager2 ve fragmentu faktury.
 * Zajišťuje zobrazení záložek se záznamy faktury, detailem a platbami.
 * Xlisto 28.02.2023 10:28
 */
public class MyViewPagerAdapter extends FragmentStateAdapter {

    private final InvoiceFragment invoiceFragment;
    private final InvoiceDetailFragment invoiceDetailFragment;
    private final PaymentFragment paymentFragment;


    /**
     * Vytvoří adaptér ViewPageru a připraví všechny fragmenty záložek.
     *
     * @param fragment     hostitelský fragment
     * @param tableFak     název tabulky faktur
     * @param tableNow     název tabulky období bez faktury
     * @param tablePay     název tabulky plateb
     * @param tableRead    název tabulky odečtů
     * @param idFak        ID faktury
     * @param positionList pozice faktury v seznamu
     */
    public MyViewPagerAdapter(@NonNull Fragment fragment, String tableFak, String tableNow, String tablePay, String tableRead, long idFak, int positionList) {
        super(fragment);
        invoiceFragment = InvoiceFragment.newInstance(tableFak, tableNow, tablePay, tableRead, idFak, positionList);
        invoiceDetailFragment = InvoiceDetailFragment.newInstance(tableFak, tableNow, tablePay, idFak, positionList);
        paymentFragment = PaymentFragment.newInstance(idFak, positionList);
    }


    /**
     * Vrátí fragment podle indexu vybrané záložky.
     *
     * @param position index záložky
     * @return odpovídající fragment
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> invoiceFragment;
            case 1 -> invoiceDetailFragment;
            default -> paymentFragment;
        };
    }


    /**
     * Vrátí počet záložek v pageru.
     *
     * @return počet záložek
     */
    @Override
    public int getItemCount() {
        return 3;
    }


    /** Typy záložek používané při otevírání fragmentu na konkrétní stránce. */
    public enum TypeTabs {
        INVOICE,
        INVOICE_DET,
        PAYMENT
    }

}
