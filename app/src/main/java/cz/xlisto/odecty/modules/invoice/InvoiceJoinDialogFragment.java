package cz.xlisto.odecty.modules.invoice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
    private static final String TABLE = "table";
    private long idInvoiceFirst, idInvoiceSecond;
    private String table;
    private InvoiceModel invoiceFirst, invoiceSecond, invoiceJoined;
    private TextView tvDateFirst, tvDateSecond, tvDateTotal;
    private TextView tvVtStartFirst, tvVtStartSecond, tvVtStartTotal;
    private TextView tvVtEndFirst, tvVtEndSecond, tvVtEndTotal;
    private TextView tvNtStartFirst, tvNtStartSecond, tvNtStartTotal;
    private TextView tvNtEndFirst, tvNtEndSecond, tvNtEndTotal;
    private Button btnJoin, btnCancel;
    private OnJoinListener onJoinListener;


    public static InvoiceJoinDialogFragment newInstance(long idInvoiceFirst, long idInvoiceSecond, String table) {
        Bundle bundle = new Bundle();
        bundle.putLong(ID_INVOICE_FIRST, idInvoiceFirst);
        bundle.putLong(ID_INVOICE_SECOND, idInvoiceSecond);
        bundle.putString(TABLE, table);
        InvoiceJoinDialogFragment invoiceJoinDialogFragment = new InvoiceJoinDialogFragment();
        invoiceJoinDialogFragment.setArguments(bundle);
        return invoiceJoinDialogFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        idInvoiceFirst = getArguments().getLong(ID_INVOICE_FIRST);
        idInvoiceSecond = getArguments().getLong(ID_INVOICE_SECOND);
        table = getArguments().getString(TABLE);

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
        InvoiceJoinDialogFragment invoiceJoinDialogFragment = (InvoiceJoinDialogFragment) getParentFragmentManager().findFragmentByTag(TAG);
        Log.w(TAG, "onCreateDialog: " + invoiceJoinDialogFragment);
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

        btnJoin = dialogView.findViewById(R.id.btnJoin);
        btnCancel = dialogView.findViewById(R.id.btnJoinCancel);

        btnCancel.setOnClickListener(v -> dismiss());
        btnJoin.setOnClickListener(v -> {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(requireContext());
            dataSubscriptionPointSource.open();
            dataSubscriptionPointSource.updateInvoice(invoiceJoined.getId(), table, invoiceJoined);
            dataSubscriptionPointSource.deleteInvoice(invoiceSecond.getId(), table);
            dataSubscriptionPointSource.close();
            //onJoinListener.onJoin(true);
            dismiss();
        });

        builder.setView(dialogView);
        builder.setTitle(getResources().getString(R.string.join_record));
        setViews();
        return builder.create();
    }


    public void setOnJoinListener(OnJoinListener onJoinListener) {
        this.onJoinListener = onJoinListener;
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
     * @param invoiceFirst
     * @param invoiceSecond
     * @return
     */
    private InvoiceModel joinInvoices(InvoiceModel invoiceFirst, InvoiceModel invoiceSecond) {
        invoiceFirst.setDateFrom(invoiceSecond.getDateFrom());
        invoiceFirst.setVtStart(invoiceSecond.getVtStart());
        invoiceFirst.setNtStart(invoiceSecond.getNtStart());

        return invoiceFirst;
    }


    public interface OnJoinListener {
        void onJoin(boolean b);
    }
}
