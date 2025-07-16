package cz.xlisto.elektrodroid.modules.monthlyreading;


import static cz.xlisto.elektrodroid.ownview.OwnDatePicker.showDialog;
import static cz.xlisto.elektrodroid.ownview.ViewHelper.parseCalendarFromString;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_DATE_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_DESCRIPTION;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_FIRST_READING_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_NT_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_OTHER_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_PAYMENT_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_SHOW_DESCRIPTION_MONTHLY_READING;
import static cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading.ARG_VT_MONTHLY_READING;
import static cz.xlisto.elektrodroid.utils.FragmentChange.Transaction.MOVE;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataMonthlyReadingSource;
import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PaymentModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.backup.SaveDataToBackupFile;
import cz.xlisto.elektrodroid.modules.invoice.WithOutInvoiceService;
import cz.xlisto.elektrodroid.modules.pricelist.PriceListFragment;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.ownview.ViewHelper;
import cz.xlisto.elektrodroid.shp.ShPAddEditMonthlyReading;
import cz.xlisto.elektrodroid.shp.ShPGoogleDrive;
import cz.xlisto.elektrodroid.shp.ShPInvoice;
import cz.xlisto.elektrodroid.utils.FragmentChange;
import cz.xlisto.elektrodroid.utils.Keyboard;
import cz.xlisto.elektrodroid.utils.NetworkCallbackImpl;
import cz.xlisto.elektrodroid.utils.NetworkUtil;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Abstraktní třída pro přidání a editaci měsíčního odečtu.
 */
public abstract class MonthlyReadingAddEditFragmentAbstract extends Fragment implements NetworkCallbackImpl.NetworkChangeListener {

    private final String TAG = "MonthlyReadingAddEditFragmentAbstract";
    Button btnBack, btnSave, btnDate, btnSelectPriceList;
    LabelEditText labVT, labNT, labPayment, labDescription, labOtherService;
    CheckBox cbAddPayment, cbChangeMeter, cbShowDescription, cbAddBackup, cbSendBackup;
    EditText etDatePayment;
    TextView tvContentAddPayment, tvResultDate, tvLabelLoadFiles;
    LinearLayout lnProgressBAr;
    PriceListModel selectedPriceList;
    PriceListModel priceListFromDatabase;
    SubscriptionPointModel subscriptionPoint;
    RelativeLayout rlRoot;
    ShPAddEditMonthlyReading shPAddEditMonthlyReading;
    boolean isFirstLoad = true;
    boolean isChangeMeter = false;
    boolean internetAvailable;
    private static boolean restoredSharedPreferences = false;
    int countMonthlyReading = 0;
    DocumentFile backupFile;
    String folderId;
    View view;
    protected MonthlyReadingViewModel viewModel;
    //handler pro zálohu na google drive, spouští se po vytvoření záložního ZIP souboru
    protected Handler handlerSaveToGoogleDrive = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            if(!isAdded()) return;
            ShPGoogleDrive shPGoogleDrive = new ShPGoogleDrive(requireContext());
            if (cbSendBackup.isChecked()) {
                if (shPGoogleDrive.get(ShPGoogleDrive.USER_SIGNED, false)) {
                    folderId = shPGoogleDrive.get(ShPGoogleDrive.DEFAULT_FOLDER_ID, "");
                    String accountName = shPGoogleDrive.get(ShPGoogleDrive.USER_NAME, "");
                    backupFile = (DocumentFile) msg.obj;
                    viewModel.showProgressBar();
                    viewModel.uploadFileToGoogleDrive(requireContext(), backupFile, accountName, folderId);
                } else {
                    Snackbar.make(requireView(), requireContext().getString(R.string.no_signs), Snackbar.LENGTH_LONG).show();
                }
            } else {
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            }

        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MonthlyReadingViewModel.class);

        viewModel.getUploadResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result) {
                    Snackbar.make(requireView(), requireContext().getString(R.string.uploaded_file), Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(requireView(), requireContext().getString(R.string.not_uploaded_file), Snackbar.LENGTH_LONG).show();
                }
                Keyboard.hide(requireActivity());
                getParentFragmentManager().popBackStack();
            }
        });

        viewModel.getShowingProgressBar().observe(getViewLifecycleOwner(), showing -> {
            if (showing != null) {
                if (showing) {
                    tvLabelLoadFiles.setText(R.string.uploading_file_on_drive);
                    lnProgressBAr.setVisibility(View.VISIBLE);
                } else {
                    lnProgressBAr.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getChangeMeter().observe(getViewLifecycleOwner(), isChangeMeter -> {
            if (isChangeMeter != null) {
                this.isChangeMeter = isChangeMeter;
                hideItemsForFirstReading(isChangeMeter);
            }
        });

        viewModel.getIsFirst().observe(getViewLifecycleOwner(), isFirst -> {
            if (isFirst != null) {
                this.isFirstLoad = isFirst;
            }
        });

        viewModel.getSelectedPriceList().observe(getViewLifecycleOwner(), selectedPriceList -> {
            if (selectedPriceList != null) {
                this.selectedPriceList = selectedPriceList;
                btnSelectPriceList.setText(selectedPriceList.getName());
            } else
                btnSelectPriceList.setText("");
        });

        viewModel.getWidgetContainer().observe(getViewLifecycleOwner(), monthlyReadingWidgetContainer -> {
            if (monthlyReadingWidgetContainer != null) {
                btnDate.setText(monthlyReadingWidgetContainer.date);
                labVT.setDefaultText(monthlyReadingWidgetContainer.vt);
                labNT.setDefaultText(monthlyReadingWidgetContainer.nt);
                labDescription.setDefaultText(monthlyReadingWidgetContainer.description);
                labPayment.setDefaultText(monthlyReadingWidgetContainer.payment);
                labOtherService.setDefaultText(monthlyReadingWidgetContainer.otherService);
            }
        });

        ConnectivityManager connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCallbackImpl networkCallback = new NetworkCallbackImpl(this);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        return inflater.inflate(R.layout.fragment_monthly_reading_add_edit, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        requireActivity().invalidateOptionsMenu();
        shPAddEditMonthlyReading = new ShPAddEditMonthlyReading(requireActivity());
        internetAvailable = NetworkUtil.isInternetAvailable(requireContext());

        subscriptionPoint = SubscriptionPoint.load(getActivity());
        btnSave = view.findViewById(R.id.btnSaveMonthlyReading);
        btnBack = view.findViewById(R.id.btnBackMonthlyReading);
        btnDate = view.findViewById(R.id.btnDate);
        btnSelectPriceList = view.findViewById(R.id.btnSelectPriceList);
        labVT = view.findViewById(R.id.labVT);
        labNT = view.findViewById(R.id.labNT);
        labPayment = view.findViewById(R.id.labPayment);
        labDescription = view.findViewById(R.id.labDescription);
        labOtherService = view.findViewById(R.id.labOtherServices);
        cbAddPayment = view.findViewById(R.id.cbAddPayment);
        cbChangeMeter = view.findViewById(R.id.cbChangeMeter);
        cbAddBackup = view.findViewById(R.id.cbAddBackup);
        cbSendBackup = view.findViewById(R.id.cbSendBackup);
        cbShowDescription = view.findViewById(R.id.cbShowDescription);
        etDatePayment = view.findViewById(R.id.etDatePayment);
        tvContentAddPayment = view.findViewById(R.id.tvContentAddPayment);
        tvResultDate = view.findViewById(R.id.tvResultDate);
        rlRoot = view.findViewById(R.id.rlRoot);
        lnProgressBAr = view.findViewById(R.id.lnProgressBar);
        tvLabelLoadFiles = view.findViewById(R.id.tvLabelLoadFiles);

        cbShowDescription.setChecked(shPAddEditMonthlyReading.get(ARG_SHOW_DESCRIPTION_MONTHLY_READING, false));

        cbChangeMeter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setIsChangeMeter(isChecked);
            shPAddEditMonthlyReading.set(ARG_FIRST_READING_MONTHLY_READING, cbChangeMeter.isChecked());
        });

        cbShowDescription.setOnCheckedChangeListener((buttonView, isChecked) -> {
            shPAddEditMonthlyReading.set(ARG_SHOW_DESCRIPTION_MONTHLY_READING, cbShowDescription.isChecked());
            setShowDescription();
        });

        btnDate.setOnClickListener(v -> showDialog(getActivity(), day -> {
            btnDate.setText(day);
            onResume();
        }, btnDate.getText().toString()));

        btnSelectPriceList.setOnClickListener(v -> {
            saveSharedPreferences();

            PriceListFragment priceListFragment = PriceListFragment.newInstance(true, selectedPriceList.getId());

            FragmentChange.replace(requireActivity(), priceListFragment, MOVE, true);
        });

        btnBack.setOnClickListener(v -> {
            Keyboard.hide(requireActivity());
            getParentFragmentManager().popBackStack();
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        if (restoredSharedPreferences)
            restoreSharedPreferences();

        setShowDescription();
        setShowAddPayment();
        setShowCbSendBackup();
    }


    /**
     * Sestaví objekt odběrného místa z údajů widgetů
     *
     * @return MonthlyReadingModel Objekt měsíčního odečtu
     */
    MonthlyReadingModel createMonthlyReading() {
        long date = parseCalendarFromString(btnDate.getText().toString()).getTimeInMillis();
        date -= ViewHelper.getOffsetTimeZones(date);
        return new MonthlyReadingModel(date,
                labVT.getDouble(), labNT.getDouble(), labPayment.getDouble(),
                labDescription.getText(), labOtherService.getDouble(),
                selectedPriceList.getId(), cbChangeMeter.isChecked());
    }


    /**
     * Sestaví objekt platby z údajů widgetů
     *
     * @return PaymentModel Objekt platby
     */
    PaymentModel createPayment(long date) {
        date -= ViewHelper.getOffsetTimeZones(date);
        return new PaymentModel(-1L, -1L, date, labPayment.getDouble(), 2);
    }


    /**
     * Uloží hodnoty widgetů do sharedprefences
     */
    void saveSharedPreferences() {
        shPAddEditMonthlyReading.set(ARG_VT_MONTHLY_READING, labVT.getText());
        shPAddEditMonthlyReading.set(ARG_NT_MONTHLY_READING, labNT.getText());
        shPAddEditMonthlyReading.set(ARG_DESCRIPTION, labDescription.getText());
        shPAddEditMonthlyReading.set(ARG_PAYMENT_MONTHLY_READING, labPayment.getText());
        shPAddEditMonthlyReading.set(ARG_OTHER_MONTHLY_READING, labOtherService.getText());
        shPAddEditMonthlyReading.set(ARG_DATE_MONTHLY_READING, btnDate.getText().toString());
        restoredSharedPreferences = true;
    }


    /**
     * Obnoví hodnoty widgetů ze sharedprefences
     */
    void restoreSharedPreferences() {
        labVT.setDefaultText(shPAddEditMonthlyReading.get(ARG_VT_MONTHLY_READING, ""));
        labNT.setDefaultText(shPAddEditMonthlyReading.get(ARG_NT_MONTHLY_READING, ""));
        labDescription.setDefaultText(shPAddEditMonthlyReading.get(ARG_DESCRIPTION, ""));
        labPayment.setDefaultText(shPAddEditMonthlyReading.get(ARG_PAYMENT_MONTHLY_READING, ""));
        labOtherService.setDefaultText(shPAddEditMonthlyReading.get(ARG_OTHER_MONTHLY_READING, ""));
        btnDate.setText(shPAddEditMonthlyReading.get(ARG_DATE_MONTHLY_READING, ""));
        restoredSharedPreferences = false;
    }


    /**
     * Nastaví viditelnost checkboxu "Odeslat zálohu" na základě dostupnosti internetu.
     * Pokud je internet dostupný, checkbox bude viditelný, jinak bude skrytý.
     */
    void setShowCbSendBackup() {
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                //TODO: skrytí Drive
                cbSendBackup.setVisibility(View.GONE);
            /*if (internetAvailable)
                cbSendBackup.setVisibility(View.VISIBLE);
            else
                cbSendBackup.setVisibility(View.GONE);*/
            });
        }
    }


    /**
     * Nastaví zaškrtnutí checkboxu "Přidat platbu" a zobrazí/skryje edittext pro zadání data platby
     */
    void setShowAddPayment() {
        TransitionManager.beginDelayedTransition(rlRoot);
        if (cbAddPayment.isChecked()) {
            etDatePayment.setVisibility(View.VISIBLE);
            tvContentAddPayment.setVisibility(View.VISIBLE);
            tvResultDate.setVisibility(View.VISIBLE);
        } else {
            etDatePayment.setVisibility(View.GONE);
            tvContentAddPayment.setVisibility(View.GONE);
            tvResultDate.setVisibility(View.GONE);
        }
    }


    /**
     * Nastaví zaškrtnutí checkboxu "Zobrazit popis" a zobrazí/skryje labeledity pro zadání poznámky a další doplňkové služby
     */
    void setShowDescription() {
        TransitionManager.beginDelayedTransition(rlRoot);
        if (cbShowDescription.isChecked()) {
            labDescription.setVisibility(View.VISIBLE);
            labOtherService.setVisibility(View.VISIBLE);
        } else {
            labDescription.setVisibility(View.GONE);
            labOtherService.setVisibility(View.GONE);
        }
    }


    /**
     * Upraví poslední záznam v období bez faktury, navazující na měsíční odečet. Vloží další záznam do faktury při výměně elektroměru.
     *
     * @param lastMonthlyReading - poslední záznam měsíčního odečtu podle data
     */
    void updateItemInvoice(MonthlyReadingModel lastMonthlyReading) {
        ShPInvoice shPInvoice = new ShPInvoice(requireActivity());
        if (shPInvoice.get(ShPInvoice.AUTO_GENERATE_INVOICE, true))
            WithOutInvoiceService.updateAllItemsInvoice(requireActivity(), subscriptionPoint.getTableTED(), subscriptionPoint.getTableFAK(), subscriptionPoint.getTableO());
        else
            WithOutInvoiceService.editLastItemInInvoice(requireActivity(), subscriptionPoint.getTableTED(), subscriptionPoint.getTableFAK(), lastMonthlyReading);
    }


    /**
     * Vytvoří zálohu měsíčního odečtu
     */
    void backupMonthlyReading() {
        //handler pro spuštění zálohy na google drive
        SaveDataToBackupFile.saveToZip(requireActivity(), handlerSaveToGoogleDrive);
    }


    /**
     * Skryje/zobrazí widgety pro první odečet
     *
     * @param isChecked Zaškrtnutí checkboxu "První odečet"
     */
    void hideItemsForFirstReading(boolean isChecked) {
        labPayment.setEnabled(!isChecked);
        labOtherService.setEnabled(!isChecked);
        cbAddPayment.setEnabled(!isChecked);
        if (isChecked || !cbChangeMeter.isEnabled())
            btnSelectPriceList.setVisibility(View.GONE);
        else
            btnSelectPriceList.setVisibility(View.VISIBLE);
        cbAddPayment.setChecked(false);
    }


    /**
     * Získá počet záznamů měsíčního odečtu.
     *
     * @return int Počet záznamů měsíčního odečtu
     */
    int getCountMonthlyReading() {
        DataMonthlyReadingSource dataMonthlyReadingSource = new DataMonthlyReadingSource(requireContext());
        dataMonthlyReadingSource.open();
        int countMonthlyReading = dataMonthlyReadingSource.getCount(subscriptionPoint.getTableO());
        dataMonthlyReadingSource.close();
        return countMonthlyReading;
    }


    /**
     * Načte s databáze objekt ceníku.
     */
    void loadPriceList(long id) {
        DataPriceListSource dataPriceListSource = new DataPriceListSource(getActivity());
        dataPriceListSource.open();
        priceListFromDatabase = dataPriceListSource.readPrice(id);
        dataPriceListSource.close();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MonthlyReadingWidgetContainer monthlyReadingWidgetContainer = new MonthlyReadingWidgetContainer();
        monthlyReadingWidgetContainer.date = btnDate.getText().toString();
        monthlyReadingWidgetContainer.vt = labVT.getText();
        monthlyReadingWidgetContainer.nt = labNT.getText();
        monthlyReadingWidgetContainer.payment = labPayment.getText();
        monthlyReadingWidgetContainer.description = labDescription.getText();
        monthlyReadingWidgetContainer.otherService = labOtherService.getText();
        viewModel.setWidgetContainer(monthlyReadingWidgetContainer);
    }

}
