package cz.xlisto.elektrodroid.modules.invoice;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataInvoiceSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.dialogs.SettingsInvoiceDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SettingsViewDialogFragment;
import cz.xlisto.elektrodroid.dialogs.SubscriptionPointDialogFragment;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.models.InvoiceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPInvoice;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;
import cz.xlisto.elektrodroid.utils.UIHelper;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InvoiceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InvoiceListFragment extends Fragment {

    private final String TAG = "InvoiceFragment";
    public static final String INVOICE_NUMBER_EDIT_LISTENER = "invoice_number_edit_listener";
    public static final String INVOICE_NUMBER_ADD_LISTENER = "invoice_number_add_listener";
    public static final String INVOICE_DELETE_LISTENER = "deleteInvoice";
    public static final String NUMBER_INVOICE = "number_invoice";
    public static final String ID_INVOICE = "id_invoice";
    public static final String ID_SUBSCRIPTIONPOINT = "id_subscriptionpoint";
    private RecyclerView rv;
    private long idSubscriptionPoint;
    private InvoiceListAdapter invoiceAdapter;
    private String tablePay;
    private String tableFak;
    private String tableNow;
    private String tableReading;
    private Button btnAddInvoice;
    private FloatingActionButton fab;


    public static InvoiceListFragment newInstance() {
        return new InvoiceListFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //https://developer.android.com/guide/fragments/communicate#fragment-result
        //posluchač pro změnu čísla faktury
        getParentFragmentManager().setFragmentResultListener(INVOICE_NUMBER_EDIT_LISTENER, this, (requestKey, bundle) -> {
            String numberInvoice = bundle.getString(NUMBER_INVOICE);
            long idInvoice = bundle.getLong(ID_INVOICE);
            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
            dataInvoiceSource.open();
            dataInvoiceSource.updateInvoiceList(numberInvoice, idInvoice);
            dataInvoiceSource.close();
            onResume();
        });
        getParentFragmentManager().setFragmentResultListener(INVOICE_NUMBER_ADD_LISTENER, this, (requestKey, bundle) -> {
            String numberInvoice = bundle.getString(NUMBER_INVOICE);
            long idSubscriptionPoint = bundle.getLong(ID_SUBSCRIPTIONPOINT);
            DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireActivity());
            dataInvoiceSource.open();
            dataInvoiceSource.insertInvoiceList(numberInvoice, idSubscriptionPoint);
            dataInvoiceSource.close();
            onResume();
        });
        //posluchač změny odběrného místa
        getParentFragmentManager().setFragmentResultListener(SubscriptionPointDialogFragment.FLAG_UPDATE_SUBSCRIPTION_POINT, this, (requestKey, bundle) -> {
            readIdSubscriptionPoint();
            onResume();
        });
        //posluchač pro smazání faktury
        getParentFragmentManager().setFragmentResultListener(INVOICE_DELETE_LISTENER, this, (requestKey, bundle) -> {
            boolean b = bundle.getBoolean(YesNoDialogFragment.RESULT);
            long idFak = invoiceAdapter.getSelectedIdFak();
            if (b) {
                DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
                dataSubscriptionPointSource.open();
                dataSubscriptionPointSource.deletePayments(idFak, tablePay);
                dataSubscriptionPointSource.close();
                DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireContext());
                dataInvoiceSource.open();
                dataInvoiceSource.deleteInvoiceList(tableFak, idFak);
                dataInvoiceSource.close();
                invoiceAdapter.removeItem();
                ShPInvoice shPInvoice = new ShPInvoice(requireActivity());
                if (shPInvoice.get(ShPInvoice.AUTO_GENERATE_INVOICE, true))
                    WithOutInvoiceService.updateAllItemsInvoice(requireActivity(), tableNow, tableFak, tableReading);
                else
                    WithOutInvoiceService.editFirstItemInInvoice(requireActivity());

                loadDataAndSetAdapter();
                invoiceAdapter.notifyItemRangeChanged(0, invoiceAdapter.getItemCount());
            }
        });
        //posluchač zavření dialogová okna nastavení
        getParentFragmentManager().setFragmentResultListener(SettingsViewDialogFragment.FLAG_UPDATE_SETTINGS, this,
                (requestKey, bundle) -> UIHelper.showButtons(btnAddInvoice, fab, requireActivity(), true)
        );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invoice_list, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readIdSubscriptionPoint();
        btnAddInvoice = view.findViewById(R.id.btnAddPayment);
        fab = view.findViewById(R.id.fab);
        rv = view.findViewById(R.id.recycleViewInvoiceList);
        TextView tvNoInvoice = view.findViewById(R.id.tvAlertInvoiceList);
        btnAddInvoice.setOnClickListener(v -> showAddInvoiceDialog());
        fab.setOnClickListener(v -> showAddInvoiceDialog());
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(requireActivity());
        if (subscriptionPoint == null) {
            tvNoInvoice.setVisibility(View.VISIBLE);
            rv.setVisibility(View.INVISIBLE);
            btnAddInvoice.setEnabled(false);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        UIHelper.showButtons(btnAddInvoice, fab, requireActivity(), true);
        loadDataAndSetAdapter();
    }


    /**
     * Načte data a nastaví adaptér
     */
    private void loadDataAndSetAdapter() {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireActivity());
        dataSubscriptionPointSource.open();
        SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
        if (subscriptionPoint == null) {
            return;
        }
        tablePay = subscriptionPoint.getTablePLATBY();
        tableFak = subscriptionPoint.getTableFAK();
        tableNow = subscriptionPoint.getTableTED();
        tableReading = subscriptionPoint.getTableO();
        dataSubscriptionPointSource.close();
        DataInvoiceSource dataInvoiceSource = new DataInvoiceSource(requireContext());
        dataInvoiceSource.open();
        ArrayList<InvoiceListModel> invoices = dataInvoiceSource.loadInvoiceLists(subscriptionPoint);
        dataInvoiceSource.close();

        if (invoices.isEmpty()) {
            OwnAlertDialog.show(requireContext(), requireContext().getResources().getString(R.string.error), requireContext().getResources().getString(R.string.no_invoice));
        }

        invoiceAdapter = new InvoiceListAdapter(requireContext(), invoices, rv);
        rv.setAdapter(invoiceAdapter);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_invoice, menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_invoice_item_settings) {
            SettingsInvoiceDialogFragment.newInstance().show(requireActivity().getSupportFragmentManager(), SettingsInvoiceDialogFragment.TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void readIdSubscriptionPoint() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(requireActivity());
        idSubscriptionPoint = shPSubscriptionPoint.get(ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG, -1L);
    }


    /**
     * Zobrazí fragment pro přidání faktury
     */
    private void showAddInvoiceDialog() {
        InvoiceListAddDialogFragment invoiceAddDialogFragment = InvoiceListAddDialogFragment.newInstance(idSubscriptionPoint);
        invoiceAddDialogFragment.show(requireActivity().getSupportFragmentManager(), TAG);
    }

}