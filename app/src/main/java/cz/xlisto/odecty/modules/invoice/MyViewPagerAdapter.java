package cz.xlisto.odecty.modules.invoice;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import cz.xlisto.odecty.modules.payment.PaymentFragment;

/**
 * Xlisto 28.02.2023 10:28
 */
public class MyViewPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "MyViewPagerAdapter";
    private final InvoiceFragment invoiceFragment;
    private final InvoiceDetailFragment invoiceDetailFragment;
    private final PaymentFragment paymentFragment;


    public MyViewPagerAdapter(@NonNull Fragment fragment, String tableFak, String tableNow,String tablePay,String tableRead,long idFak, int positionList) {
        super(fragment);
        invoiceFragment = InvoiceFragment.newInstance(tableFak, tableNow,tablePay, tableRead,idFak, positionList);
        invoiceDetailFragment = InvoiceDetailFragment.newInstance(tableFak, tableNow,tablePay, idFak, positionList);
        paymentFragment = PaymentFragment.newInstance(idFak, positionList);
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return invoiceFragment;
            case 1:
                return invoiceDetailFragment;
            case 2:
                return paymentFragment;
            default:
                /*if (typeTabs != TypeTabs.INVOICE)
                    return InvoiceFragment.newInstance(tableFak, tablePay, idFak, positionList);
                else*/
                    return paymentFragment;
        }
    }


    @Override
    public int getItemCount() {
        return 3;
    }

    enum TypeTabs {
        INVOICE,
        INVOICE_DET,
        PAYMENT
    }
}
