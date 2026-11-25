package cz.xlisto.elektrodroid.dialogs;


import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.modules.pricelist.pdf.PdfParserFactory;
import cz.xlisto.elektrodroid.modules.pricelist.pdf.SupportedPriceList;


/**
 * Dialogové okno zobrazované při neúspěšném automatickém importu PDF ceníku.
 * <p>
 * Informuje uživatele o důvodu selhání (pokud jej existující parser dokáže spolehlivě rozlišit)
 * a nabízí akce pro zobrazení nápovědy podporovaných ceníků (případně s posunutím na rozpoznaného
 * dodavatele), výběr jiného PDF souboru nebo zavření dialogu.
 */
public class PdfImportErrorDialogFragment extends DialogFragment {

    /**
     * Tag pro zobrazení dialogu ve {@code FragmentManager}.
     */
    public static final String TAG = "PdfImportErrorDialogFragment";

    /**
     * Klíč požadavku pro předání zvolené akce přes {@code FragmentResultListener}.
     */
    public static final String REQUEST_KEY = "PdfImportErrorDialogFragment_REQUEST_KEY";

    /**
     * Klíč zvolené akce v výsledkovém {@link Bundle}.
     */
    public static final String RESULT_ACTION = "RESULT_ACTION";

    /**
     * Klíč identifikátoru rozpoznaného dodavatele v výsledkovém {@link Bundle}.
     */
    public static final String RESULT_DETECTED_SUPPLIER_ID = "RESULT_DETECTED_SUPPLIER_ID";

    /**
     * Akce pro otevření obrazovky Podporované ceníky.
     */
    public static final String ACTION_SHOW_SUPPORTED = "ACTION_SHOW_SUPPORTED";

    /**
     * Akce pro opětovné spuštění výběru PDF souboru.
     */
    public static final String ACTION_CHOOSE_ANOTHER = "ACTION_CHOOSE_ANOTHER";

    private static final String ARG_FAILURE_REASON = "argFailureReason";
    private static final String ARG_DETECTED_SUPPLIER_ID = "argDetectedSupplierId";


    /**
     * Rozlišitelné příčiny selhání importu PDF z existujícího zpracování.
     */
    public enum FailureReason {
        /**
         * Nepodařilo se otevřít soubor PDF nebo z něj extrahovat text.
         */
        READ_FAILED,
        /**
         * Dodavatel z textu PDF nebyl rozpoznán mezi podporovanými parsery.
         */
        UNKNOWN_SUPPLIER,
        /**
         * Dodavatel byl rozpoznán, ale z dokumentu se nepodařilo načíst cenové údaje.
         */
        KNOWN_SUPPLIER_PARSE_FAILED
    }


    /**
     * Vytvoří novou instanci dialogu pro zobrazení chyby importu PDF.
     *
     * @param reason             Příčina selhání rozlišená při zpracování PDF
     * @param detectedSupplierId Identifikátor rozpoznaného dodavatele, nebo {@code null}
     * @return Nová instance {@link PdfImportErrorDialogFragment}
     */
    @NonNull
    public static PdfImportErrorDialogFragment newInstance(
            @NonNull FailureReason reason,
            @Nullable String detectedSupplierId
    ) {
        PdfImportErrorDialogFragment fragment = new PdfImportErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FAILURE_REASON, reason.name());
        if (detectedSupplierId != null && !detectedSupplierId.isEmpty()) {
            args.putString(ARG_DETECTED_SUPPLIER_ID, detectedSupplierId);
        }
        fragment.setArguments(args);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_pdf_import_error, null);

        TextView tvDetail = view.findViewById(R.id.tvPdfImportErrorDetail);
        MaterialButton btnShowSupported = view.findViewById(R.id.btnShowSupportedPriceLists);
        MaterialButton btnChooseAnother = view.findViewById(R.id.btnChooseAnotherPdf);
        MaterialButton btnClose = view.findViewById(R.id.btnClosePdfError);

        Bundle args = getArguments();
        String detectedSupplierId = args != null ? args.getString(ARG_DETECTED_SUPPLIER_ID) : null;
        FailureReason reason = parseReason(args != null ? args.getString(ARG_FAILURE_REASON) : null);

        String detailText = buildDetailText(reason, detectedSupplierId);
        if (detailText != null && !detailText.isEmpty()) {
            tvDetail.setText(detailText);
            tvDetail.setVisibility(View.VISIBLE);
        } else {
            tvDetail.setVisibility(View.GONE);
        }

        btnShowSupported.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_ACTION, ACTION_SHOW_SUPPORTED);
            if (detectedSupplierId != null && !detectedSupplierId.isEmpty()) {
                result.putString(RESULT_DETECTED_SUPPLIER_ID, detectedSupplierId);
            }
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismissAllowingStateLoss();
        });

        btnChooseAnother.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString(RESULT_ACTION, ACTION_CHOOSE_ANOTHER);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismissAllowingStateLoss();
        });

        btnClose.setOnClickListener(v -> dismissAllowingStateLoss());

        return new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.pdf_import_error_title)
                .setIcon(R.drawable.ic_warning_png)
                .setView(view)
                .create();
    }


    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }


    @NonNull
    private FailureReason parseReason(@Nullable String rawReason) {
        if (rawReason == null) {
            return FailureReason.READ_FAILED;
        }
        try {
            return FailureReason.valueOf(rawReason);
        } catch (IllegalArgumentException e) {
            return FailureReason.READ_FAILED;
        }
    }


    @Nullable
    private String buildDetailText(@NonNull FailureReason reason, @Nullable String detectedSupplierId) {
        return switch (reason) {
            case UNKNOWN_SUPPLIER -> getString(R.string.pdf_import_error_detail_unknown_supplier);
            case READ_FAILED -> getString(R.string.pdf_import_error_detail_read_failed);
            case KNOWN_SUPPLIER_PARSE_FAILED -> {
                SupportedPriceList supported = PdfParserFactory.findSupportedPriceListBySupplierId(detectedSupplierId);
                if (supported != null) {
                    String supplierName = getString(supported.getSupplierNameResId());
                    yield getString(R.string.pdf_import_error_detail_known_supplier, supplierName);
                }
                yield null;
            }
        };
    }

}
