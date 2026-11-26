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
    public static final String ACTION_SKIP_FILE = "ACTION_SKIP_FILE";
    public static final String ACTION_STOP_IMPORT = "ACTION_STOP_IMPORT";

    private static final String ARG_FAILURE_REASON = "argFailureReason";
    private static final String ARG_DETECTED_SUPPLIER_ID = "argDetectedSupplierId";
    private static final String ARG_COMPLETED_FILES = "completedFiles";
    private static final String ARG_TOTAL_FILES = "totalFiles";


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
     * @param completedFiles     Počet již vyřízených souborů
     * @param totalFiles         Počet souborů při hromadném importu
     * @return Nová instance {@link PdfImportErrorDialogFragment}
     */
    @NonNull
    public static PdfImportErrorDialogFragment newInstance(
            @NonNull FailureReason reason,
            @Nullable String detectedSupplierId,
            int completedFiles,
            int totalFiles
    ) {
        PdfImportErrorDialogFragment fragment = new PdfImportErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FAILURE_REASON, reason.name());
        args.putInt(ARG_COMPLETED_FILES, completedFiles);
        args.putInt(ARG_TOTAL_FILES, totalFiles);
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

        int totalFiles = args != null ? args.getInt(ARG_TOTAL_FILES) : 0;
        if (totalFiles > 1) {
            setCancelable(false);
            TextView tvMessage = view.findViewById(R.id.tvPdfImportErrorMessage);
            int completedFiles = args.getInt(ARG_COMPLETED_FILES);
            boolean hasNextFile = completedFiles + 1 < totalFiles;
            String progress = getString(R.string.pdf_import_batch_progress, completedFiles, totalFiles);
            tvMessage.setText(hasNextFile ? progress + "\n\n" + getString(R.string.pdf_import_error_message_batch) : progress);
            btnShowSupported.setVisibility(View.GONE);
            btnChooseAnother.setText(R.string.pdf_import_error_action_skip_file);
            btnChooseAnother.setOnClickListener(v -> finishBatchDialog(ACTION_SKIP_FILE));
            btnClose.setText(R.string.pdf_import_error_action_stop_import);
            btnClose.setVisibility(hasNextFile ? View.VISIBLE : View.GONE);
            btnClose.setOnClickListener(v -> finishBatchDialog(ACTION_STOP_IMPORT));
        } else {
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
        }

        return new MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme)
                .setTitle(R.string.pdf_import_error_title)
                .setIcon(R.drawable.ic_warning_png)
                .setView(view)
                .create();
    }


    private void finishBatchDialog(String action) {
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, action);
        dismiss();
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
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
                    String supplierName = getString(supported.supplierNameResId());
                    yield getString(R.string.pdf_import_error_detail_known_supplier, supplierName);
                }
                yield null;
            }
        };
    }

}
