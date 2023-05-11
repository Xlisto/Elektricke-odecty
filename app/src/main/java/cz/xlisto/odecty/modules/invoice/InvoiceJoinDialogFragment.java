package cz.xlisto.odecty.modules.invoice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.format.DecimalFormatHelper;
import cz.xlisto.odecty.models.InvoiceModel;
import cz.xlisto.odecty.ownview.ViewHelper;

/**
 * DialogFragment pro spojení dvou záznamů ve faktuře
 * Xlisto 10.05.2023 10:51
 */
public class InvoiceJoinDialogFragment extends DialogFragment {
    public static final String TAG = "InvoiceJoinDialogFragment";
    private static final String ID_INVOICE_FIRST = "idInvoiceFirst";
    private static final String ID_INVOICE_SECOND = "idInvoiceSecond";
    private static final String POSITION = "position";
    private static final String TABLE = "table";
    public static final String RESULT = "result";
    public static final String RESULT_JOIN_DIALOG_FRAGMENT = "resultJoinDialogFragment";
    private String table;
    private int position;
    private InvoiceModel invoiceFirst, invoiceSecond, invoiceJoined;
    private TextView tvDateFirst, tvDateSecond, tvDateTotal;
    private TextView tvVtStartFirst, tvVtStartSecond, tvVtStartTotal;
    private TextView tvVtEndFirst, tvVtEndSecond, tvVtEndTotal;
    private TextView tvNtStartFirst, tvNtStartSecond, tvNtStartTotal;
    private TextView tvNtEndFirst, tvNtEndSecond, tvNtEndTotal;


    public static InvoiceJoinDialogFragment newInstance(long idInvoiceFirst, long idInvoiceSecond, String table, int position) {
        Bundle bundle = new Bundle();
        bundle.putLong(ID_INVOICE_FIRST, idInvoiceFirst);
        bundle.putLong(ID_INVOICE_SECOND, idInvoiceSecond);
        bundle.putString(TABLE, table);
        bundle.putInt(POSITION, position);
        InvoiceJoinDialogFragment invoiceJoinDialogFragment = new InvoiceJoinDialogFragment();
        invoiceJoinDialogFragment.setArguments(bundle);
        return invoiceJoinDialogFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assert getArguments() != null;
        long idInvoiceFirst = getArguments().getLong(ID_INVOICE_FIRST);
        long idInvoiceSecond = getArguments().getLong(ID_INVOICE_SECOND);
        table = getArguments().getString(TABLE);
        position = getArguments().getInt(POSITION);

        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
        dataSubscriptionPointSource.open();
        invoiceFirst = dataSubscriptionPointSource.loadInvoice(idInvoiceFirst, table);
        invoiceSecond = dataSubscriptionPointSource.loadInvoice(idInvoiceSecond, table);
        dataSubscriptionPointSource.close();

        invoiceJoined = joinInvoices(invoiceFirst, invoiceSecond);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_invoice_join, null);

        tvDateFirst = dialogView.findViewById(R.id.tvJoinDate1);
        tvDateSecond = dialogView.findViewById(R.id.tvJoinDate2);
        tvDateTotal = dialogView.findViewById(R.id.tvJoinDate3);

        tvVtStartFirst = dialogView.findViewById(R.id.tvJoinVTStart1);
        tvVtStartSecond = dialogView.findViewById(R.id.tvJoinVTStart2);
        tvVtStartTotal = dialogView.findViewById(R.id.tvJoinVTStart3);

        tvVtEndFirst = dialogView.findViewById(R.id.tvJoinVTEnd1);
        tvVtEndSecond = dialogView.findViewById(R.id.tvJoinVTEnd2);
        tvVtEndTotal = dialogView.findViewById(R.id.tvJoinVTEnd3);

        tvNtStartFirst = dialogView.findViewById(R.id.tvJoinNTStart1);
        tvNtStartSecond = dialogView.findViewById(R.id.tvJoinNTStart2);
        tvNtStartTotal = dialogView.findViewById(R.id.tvJoinNTStart3);

        tvNtEndFirst = dialogView.findViewById(R.id.tvJoinNTEnd1);
        tvNtEndSecond = dialogView.findViewById(R.id.tvJoinNTEnd2);
        tvNtEndTotal = dialogView.findViewById(R.id.tvJoinNTEnd3);

        Button btnJoin = dialogView.findViewById(R.id.btnJoin);
        Button btnCancel = dialogView.findViewById(R.id.btnJoinCancel);

        btnCancel.setOnClickListener(v -> dismiss());
        btnJoin.setOnClickListener(v -> {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
            dataSubscriptionPointSource.open();
            dataSubscriptionPointSource.updateInvoice(invoiceJoined.getId(), table, invoiceJoined);
            dataSubscriptionPointSource.deleteInvoice(invoiceSecond.getId(), table);
            dataSubscriptionPointSource.close();

            Bundle bundle = new Bundle();
            bundle.putBoolean(RESULT, true);
            bundle.putInt(POSITION, position);
            getParentFragmentManager().setFragmentResult(RESULT_JOIN_DIALOG_FRAGMENT, bundle);

            dismiss();
        });

        builder.setView(dialogView);
        builder.setTitle(getResources().getString(R.string.join_record));
        setViews();
        return builder.create();
    }


    /**
     * Nastaví hodnoty do TextViews
     */
    private void setViews() {
        tvDateFirst.setText(ViewHelper.convertLongToTime(invoiceFirst.getDateFrom()) + " - " + ViewHelper.convertLongToTime(invoiceFirst.getDateTo()));
        tvDateSecond.setText(ViewHelper.convertLongToTime(invoiceSecond.getDateFrom()) + " - " + ViewHelper.convertLongToTime(invoiceSecond.getDateTo()));
        tvDateTotal.setText(ViewHelper.convertLongToTime(invoiceJoined.getDateFrom()) + " - " + ViewHelper.convertLongToTime(invoiceJoined.getDateTo()));

        DecimalFormat df2 = DecimalFormatHelper.df2;
        tvVtStartFirst.setText(df2.format(invoiceFirst.getVtStart()));
        tvVtStartSecond.setText(df2.format(invoiceSecond.getVtStart()));
        tvVtStartTotal.setText(df2.format(invoiceJoined.getVtStart()));

        tvVtEndFirst.setText(df2.format(invoiceFirst.getVtEnd()));
        tvVtEndSecond.setText(df2.format(invoiceSecond.getVtEnd()));
        tvVtEndTotal.setText(df2.format(invoiceJoined.getVtEnd()));

        tvNtStartFirst.setText(df2.format(invoiceFirst.getNtStart()));
        tvNtStartSecond.setText(df2.format(invoiceSecond.getNtStart()));
        tvNtStartTotal.setText(df2.format(invoiceJoined.getNtStart()));

        tvNtEndFirst.setText(df2.format(invoiceFirst.getNtEnd()));
        tvNtEndSecond.setText(df2.format(invoiceSecond.getNtEnd()));
        tvNtEndTotal.setText(df2.format(invoiceJoined.getNtEnd()));
    }


    /**
     * Vytvoří nový InvoiceModel a nastaví mu hodnoty z obou faktur
     *
     * @param invoiceFirst První záznam v seznamu - zůstane zachován, přebere data z druhého záznamu
     * @param invoiceSecond Druhý záznam v seznamu - předá data a bude odstraněn
     * @return Vrací nový InvoiceModel, jehož hodnoty jsou spojením obou záznamů
     */
    private InvoiceModel joinInvoices(InvoiceModel invoiceFirst, InvoiceModel invoiceSecond) {
        invoiceFirst.setDateFrom(invoiceSecond.getDateFrom());
        invoiceFirst.setVtStart(invoiceSecond.getVtStart());
        invoiceFirst.setNtStart(invoiceSecond.getNtStart());

        return invoiceFirst;
    }

}
