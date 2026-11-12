package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.dialogs.OwnAlertDialog;
import cz.xlisto.elektrodroid.dialogs.YesNoDialogFragment;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;


/**
 * Fragment pro náhled a výběr sazeb načtených z PDF ceníku.
 */
public class PdfImportSelectionFragment extends Fragment {

    public static final String TAG = "PdfImportSelectionFragment";
    private static final String ARG_PARSED_PRICELISTS = "ARG_PARSED_PRICELISTS";

    private List<PriceListModel> parsedPriceLists;
    private PdfRateSelectionAdapter adapter;


    public PdfImportSelectionFragment() {
        // Vyžadovaný prázdný konstruktor
    }


    public static PdfImportSelectionFragment newInstance(List<PriceListModel> priceLists) {
        PdfImportSelectionFragment fragment = new PdfImportSelectionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARSED_PRICELISTS, (Serializable) priceLists);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //noinspection unchecked
            parsedPriceLists = (List<PriceListModel>) BundleCompat.getSerializable(getArguments(), ARG_PARSED_PRICELISTS, ArrayList.class);
        }
        if (parsedPriceLists == null) {
            parsedPriceLists = new ArrayList<>();
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_import_selection, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvSupplier = view.findViewById(R.id.tvPdfSupplier);
        TextView tvProduct = view.findViewById(R.id.tvPdfProduct);
        TextView tvValidity = view.findViewById(R.id.tvPdfValidity);
        TextView tvValidityWarning = view.findViewById(R.id.tvPdfValidityWarning);
        Button btnSelectAll = view.findViewById(R.id.btnSelectAll);
        Button btnDeselectAll = view.findViewById(R.id.btnDeselectAll);
        Button btnBack = view.findViewById(R.id.btnBackPdfImport);
        Button btnSave = view.findViewById(R.id.btnSaveSelectedPdfRates);
        RecyclerView rv = view.findViewById(R.id.rvPdfRates);

        if (!parsedPriceLists.isEmpty()) {
            PriceListModel first = parsedPriceLists.get(0);
            tvSupplier.setText(first.getFirma());
            tvProduct.setText(first.getProdukt());

            String validFrom = ViewHelper.convertLongToDate(first.getPlatnostOD());
            String validUntil = ViewHelper.convertLongToDate(first.getPlatnostDO());
            String validityTitle = getString(R.string.pdf_import_select_rates_title) + " (" + validFrom + " – " + validUntil + ")";
            tvValidity.setText(validityTitle);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(first.getPlatnostOD() > 0 ? first.getPlatnostOD() : System.currentTimeMillis());
            int year = cal.get(Calendar.YEAR);

            boolean hasRegulatedData = first.getDistVT() > 0 || first.getJ0() > 0;
            boolean hasMissingCommercial = false;
            for (PriceListModel model : parsedPriceLists) {
                if (model.getCenaVT() == 0.0 || model.getMesicniPlat() == 0.0) {
                    hasMissingCommercial = true;
                    break;
                }
            }

            StringBuilder warningText = new StringBuilder();
            if (hasMissingCommercial) {
                warningText.append(getString(R.string.pdf_import_missing_commercial_warning));
            }
            if (!hasRegulatedData) {
                if (warningText.length() > 0) warningText.append("\n\n");
                warningText.append(getString(R.string.pdf_import_missing_regulated_warning, year, first.getDistribuce()));
            }
            if (!hasMissingCommercial && hasRegulatedData) {
                warningText.append(getString(R.string.pdf_import_validity_warning, year));
            }

            tvValidityWarning.setText(warningText.toString());
        }

        adapter = new PdfRateSelectionAdapter(parsedPriceLists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        btnDeselectAll.setOnClickListener(v -> adapter.deselectAll());

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnSave.setOnClickListener(v -> saveSelectedPriceLists());
    }


    /**
     * Uloží vybrané ceníkové položky do databáze s kontrolou duplicity.
     */
    private void saveSelectedPriceLists() {
        List<PriceListModel> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), R.string.pdf_import_no_rates_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasAnyCommercialPrice = false;
        for (PriceListModel model : selected) {
            if (model.getCenaVT() > 0 || model.getCenaNT() > 0 || model.getMesicniPlat() > 0) {
                hasAnyCommercialPrice = true;
                break;
            }
        }

        if (!hasAnyCommercialPrice) {
            OwnAlertDialog.showDialog(
                    requireActivity(),
                    getString(R.string.pdf_import_cannot_read_title),
                    getString(R.string.pdf_import_cannot_read_message)
            );
            return;
        }

        DataPriceListSource db = new DataPriceListSource(requireActivity());
        db.open();

        List<PriceListModel> toSave = new ArrayList<>();
        List<String> duplicatesDiff = new ArrayList<>();

        for (PriceListModel model : selected) {
            String dateFromStr = String.valueOf(model.getPlatnostOD());
            int count = db.countPriceListItems(model.getRada(), model.getProdukt(), model.getSazba(), model.getFirma(), dateFromStr, model.getDistribuce());
            if (count > 0) {
                long existingId = db.idPriceListItem(model.getRada(), model.getProdukt(), model.getSazba(), model.getFirma(), dateFromStr, model.getDistribuce());
                if (existingId > 0) {
                    PriceListModel existing = db.readPrice(existingId);
                    if (existing != null) {
                        String diff = buildDiffString(existing, model);
                        duplicatesDiff.add("Sazba " + model.getSazba() + " (" + model.getRada() + "):\n" + diff);
                    }
                }
            }
            toSave.add(model);
        }

        if (!duplicatesDiff.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.pdf_import_duplicate_diff_header)).append("\n\n");
            for (String d : duplicatesDiff) {
                sb.append(d).append("\n");
            }
            sb.append("Chcete tyto ceníky aktualizovat/přepsat?");

            YesNoDialogFragment dialog = YesNoDialogFragment.newInstance(
                    getString(R.string.pdf_import_duplicate_title),
                    "FLAG_PDF_DUPLICATE_OVERWRITE",
                    sb.toString()
            );

            getParentFragmentManager().setFragmentResultListener("FLAG_PDF_DUPLICATE_OVERWRITE", getViewLifecycleOwner(), (requestKey, result) -> {
                if (result.getBoolean(YesNoDialogFragment.RESULT)) {
                    performDbSave(db, toSave);
                } else {
                    db.close();
                }
            });

            dialog.show(getParentFragmentManager(), YesNoDialogFragment.TAG);
        } else {
            performDbSave(db, toSave);
        }
    }


    private void performDbSave(DataPriceListSource db, List<PriceListModel> toSave) {
        int savedCount = 0;
        for (PriceListModel model : toSave) {
            String dateFromStr = String.valueOf(model.getPlatnostOD());
            long existingId = db.idPriceListItem(model.getRada(), model.getProdukt(), model.getSazba(), model.getFirma(), dateFromStr, model.getDistribuce());
            if (existingId > 0) {
                db.updatePriceList(model, existingId);
                savedCount++;
            } else {
                long id = db.insertPriceList(model);
                if (id > 0) savedCount++;
            }
        }
        db.close();

        Toast.makeText(requireContext(), "Uloženo " + savedCount + " sazeb ceníku", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }


    private String buildDiffString(PriceListModel oldModel, PriceListModel newModel) {
        StringBuilder diff = new StringBuilder();
        if (!oldModel.getCenaVT().equals(newModel.getCenaVT())) {
            diff.append("  • VT: ").append(DecimalFormatHelper.df2.format(oldModel.getCenaVT()))
                    .append(" ➔ ").append(DecimalFormatHelper.df2.format(newModel.getCenaVT())).append(" Kč\n");
        }
        if (!oldModel.getCenaNT().equals(newModel.getCenaNT())) {
            diff.append("  • NT: ").append(DecimalFormatHelper.df2.format(oldModel.getCenaNT()))
                    .append(" ➔ ").append(DecimalFormatHelper.df2.format(newModel.getCenaNT())).append(" Kč\n");
        }
        if (!oldModel.getMesicniPlat().equals(newModel.getMesicniPlat())) {
            diff.append("  • Plat: ").append(DecimalFormatHelper.df2.format(oldModel.getMesicniPlat()))
                    .append(" ➔ ").append(DecimalFormatHelper.df2.format(newModel.getMesicniPlat())).append(" Kč/měs\n");
        }
        // CharSequence.isEmpty() vyžaduje API level 35+ (minSdk je 24)
        //noinspection SizeReplaceableByIsEmpty
        if (diff.length() == 0) {
            diff.append("  • Bez rozdílu (identické hodnoty)\n");
        }
        return diff.toString().trim();
    }

}
