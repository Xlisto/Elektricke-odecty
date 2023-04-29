package cz.xlisto.cenik.modules.invoice;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import cz.xlisto.cenik.modules.payment.PaymentFragment;

/**
 * Xlisto 28.02.2023 10:28
 */
public class MyViewPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "MyViewPagerAdapter";
    private String tableFak,tableNow, tablePay;
    private long idFak;
    private int positionList;
    private TypeTabs typeTabs;
    private InvoiceFragment invoiceFragment;
    private InvoiceDetailFragment invoiceDetailFragment;
    private PaymentFragment paymentFragment;


    public MyViewPagerAdapter(@NonNull Fragment fragment, String tableFak, String tableNow,String tablePay, long idFak, int positionList, TypeTabs typeTabs) {
        super(fragment);
        this.tableFak = tableFak;
        this.tableNow = tableNow;
        this.tablePay = tablePay;
        this.idFak = idFak;
        this.positionList = positionList;
        this.typeTabs = typeTabs;
        invoiceFragment = InvoiceFragment.newInstance(tableFak, tableNow,tablePay, idFak, positionList);
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
