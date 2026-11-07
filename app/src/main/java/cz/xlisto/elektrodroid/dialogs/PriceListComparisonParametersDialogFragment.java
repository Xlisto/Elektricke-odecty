package cz.xlisto.elektrodroid.dialogs;


import static cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.modules.pricelist.ConsuptionContainer;
import cz.xlisto.elektrodroid.ownview.LabelEditText;
import cz.xlisto.elektrodroid.shp.ShPSubscriptionPoint;

/**
 * Dialogové okno pro nastavení a úpravu parametrů porovnání ceníků elektřiny.
 *
 * <p>Umožňuje zadat spotřebu ve vysokém a nízkém tarifu (VT, NT), počet měsíců,
 * počet fází, velikost hlavního jističe a ostatní služby pro obě srovnávané strany.
 * Výsledné parametry předává zpět pomocí FragmentResult API a ukládá je do nastavení
 * pro aktuální odběrné místo.</p>
 */
public class PriceListComparisonParametersDialogFragment extends DialogFragment {

    /**
     * Tag pro identifikaci dialogového okna ve FragmentManageru.
     */
    final public static String TAG = "PriceListAddParametersDialogFragment";

    /** Klíč pro předání objektu {@link ConsuptionContainer} ve výsledném {@link Bundle}. */
    final public static String CONSUPTION_CONTAINER = "consuptionContainer";

    private final String VT = "vt";
    private final String NT = "nt";
    private final String MONTH = "month";
    private final String PHAZE = "phaze";
    private final String POWER = "power";
    private final String SERVICES_L = "ser_l";
    private final String SERVICES_R = "ser_r";
    private LabelEditText letVT, letNT, letMonth;
    private EditText letPhaze, letPower, letServicesL, letServicesR;


    /**
     * Vytvoří dialog s předvyplněnými hodnotami z zadaného kontejneru parametrů.
     *
     * @param container kontejner s aktuálními parametry porovnání ({@link ConsuptionContainer})
     * @return nová instance dialogu {@link PriceListComparisonParametersDialogFragment}
     */
    public static PriceListComparisonParametersDialogFragment newInstance(ConsuptionContainer container) {
        PriceListComparisonParametersDialogFragment frag = new PriceListComparisonParametersDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putDouble(frag.VT, container.vt());
        bundle.putDouble(frag.NT, container.nt());
        bundle.putDouble(frag.MONTH, container.month());
        bundle.putDouble(frag.PHAZE, container.phaze());
        bundle.putDouble(frag.POWER, container.power());
        bundle.putDouble(frag.SERVICES_L, container.servicesL());
        bundle.putDouble(frag.SERVICES_R, container.servicesR());
        frag.setArguments(bundle);
        return frag;
    }


    /**
     * Vytvoří novou instanci {@link PriceListComparisonParametersDialogFragment}.
     * Vyžadováno architekturou {@link DialogFragment}.
     */
    public PriceListComparisonParametersDialogFragment() {
    }


    /**
     * Vytvoří a inicializuje dialogové okno s formulářovými prvky pro nastavení parametrů porovnání.
     *
     * @param savedInstanceState uložený stav instance při obnovení (může být {@code null})
     * @return sestavená instance {@link Dialog}
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_price_list_set_values, null);
        letVT = dialogView.findViewById(R.id.letVT);
        letNT = dialogView.findViewById(R.id.letNT);
        letMonth = dialogView.findViewById(R.id.letMonth);
        letPhaze = dialogView.findViewById(R.id.letPhaze);
        letPower = dialogView.findViewById(R.id.letPower);
        letServicesL = dialogView.findViewById(R.id.letServicesL);
        letServicesR = dialogView.findViewById(R.id.letServicesR);
        Button btnReset = dialogView.findViewById(R.id.btnReset);

        if (getArguments() != null) {
            letVT.setDefaultText(DecimalFormatHelper.df2.format(getArguments().getDouble(VT)));
            letNT.setDefaultText(DecimalFormatHelper.df2.format(getArguments().getDouble(NT)));
            letMonth.setDefaultText(DecimalFormatHelper.df0.format(getArguments().getDouble(MONTH)));
            letPhaze.setText(DecimalFormatHelper.df0.format(getArguments().getDouble(PHAZE)));
            letPower.setText(DecimalFormatHelper.df0.format(getArguments().getDouble(POWER)));
            letServicesL.setText(DecimalFormatHelper.df2.format(getArguments().getDouble(SERVICES_L)));
            letServicesR.setText(DecimalFormatHelper.df2.format(getArguments().getDouble(SERVICES_R)));
        }

        builder.setView(dialogView);
        builder.setTitle(R.string.nastaveni_parametru);
        builder.setNegativeButton(R.string.zrusit, (dialog, which) -> {

        });
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            double vt = letVT.getDouble();
            double nt = letNT.getDouble();
            double month = letMonth.getDouble();
            double phaze = parseInput(letPhaze);
            double power = parseInput(letPower);
            double servicesL = parseInput(letServicesL);
            double servicesR = parseInput(letServicesR);
            ConsuptionContainer container = new ConsuptionContainer(vt, nt, month, phaze, power, servicesL, servicesR);
            persistCompareParameters(container);
            Bundle bundle = new Bundle();
            bundle.putSerializable(CONSUPTION_CONTAINER, container);
            getParentFragmentManager().setFragmentResult(TAG, bundle);
        });

        btnReset.setOnClickListener(v -> {
            letVT.setDefaultText("");
            letNT.setDefaultText("");
            letMonth.setDefaultText("");
            letPhaze.setText("");
            letPower.setText("");
            letServicesL.setText("");
            letServicesR.setText("");
        });

        setPhazeAndPowerDefaults();

        if (savedInstanceState != null) {
            letVT.setDefaultText(savedInstanceState.getString(VT));
            letNT.setDefaultText(savedInstanceState.getString(NT));
            letMonth.setDefaultText(savedInstanceState.getString(MONTH));
            letPhaze.setText(savedInstanceState.getString(PHAZE));
            letPower.setText(savedInstanceState.getString(POWER));
            letServicesL.setText(savedInstanceState.getString(SERVICES_L));
            letServicesR.setText(savedInstanceState.getString(SERVICES_R));
        }

        return builder.create();
    }


    /**
     * Uloží aktuálně zadané hodnoty z formulářových polí při změně konfigurace.
     *
     * @param outState cílový {@link Bundle} pro uložení stavu
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(VT, letVT.getText());
        outState.putString(NT, letNT.getText());
        outState.putString(MONTH, letMonth.getText());
        outState.putString(PHAZE, letPhaze.getText().toString());
        outState.putString(POWER, letPower.getText().toString());
        outState.putString(SERVICES_L, letServicesL.getText().toString());
        outState.putString(SERVICES_R, letServicesR.getText().toString());
    }


    /**
     * Načte a nastaví výchozí hodnoty fází a příkonu podle aktuálně vybraného odběrného místa.
     *
     * <p>Hodnoty se zapíší jako nápověda (hint) a do prázdných polí, pokud uživatel dosud nezadal jiné hodnoty.</p>
     */
    private void setPhazeAndPowerDefaults() {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        long idSubscriptionPoint = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);

        if (idSubscriptionPoint > 0) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
            dataSubscriptionPointSource.open();
            SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(idSubscriptionPoint);
            if (subscriptionPoint != null) {
                letPower.setHint(DecimalFormatHelper.df0.format(subscriptionPoint.getPhaze()));
                letPhaze.setHint(DecimalFormatHelper.df0.format(subscriptionPoint.getCountPhaze()));

                if (letPower.getText().toString().trim().isEmpty()) {
                    letPower.setText(DecimalFormatHelper.df0.format(subscriptionPoint.getPhaze()));
                }
                if (letPhaze.getText().toString().trim().isEmpty()) {
                    letPhaze.setText(DecimalFormatHelper.df0.format(subscriptionPoint.getCountPhaze()));
                }
            }
            dataSubscriptionPointSource.close();
        }
    }


    /**
     * Bezpečně převede obsah vstupního pole na číselnou hodnotu typu {@code double}.
     * Nahrazuje čárku za tečku pro podporu českého formátování čísel.
     *
     * @param editText vstupní pole k převedení ({@link EditText})
     * @return číselná hodnota; při prázdném vstupu vrací {@code 0.0}
     */
    private double parseInput(EditText editText) {
        String value = editText.getText().toString().trim();
        if (value.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(value.replace(",", "."));
    }


    /**
     * Uloží parametry porovnání ceníků ve formátu JSON do databáze nastavení
     * svázané s aktuálně vybraným odběrným místem.
     *
     * @param container kontejner uchovávající parametry porovnání ({@link ConsuptionContainer})
     */
    private void persistCompareParameters(ConsuptionContainer container) {
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(getActivity());
        long idSubscriptionPoint = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);
        if (idSubscriptionPoint <= 0) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put(VT, container.vt());
            json.put(NT, container.nt());
            json.put(MONTH, container.month());
            json.put(PHAZE, container.phaze());
            json.put(POWER, container.power());
            json.put(SERVICES_L, container.servicesL());
            json.put(SERVICES_R, container.servicesR());

            DataSettingsSource dataSettingsSource = new DataSettingsSource(getActivity());
            dataSettingsSource.open();
            dataSettingsSource.setPriceListCompareParameters(idSubscriptionPoint, json.toString());
            dataSettingsSource.close();
        } catch (JSONException ignored) {
        }
    }


    /**
     * Po zobrazení dialogu aplikuje jednotný barevný styl na tlačítka dialogu.
     */
    @Override
    public void onStart() {
        super.onStart();
        DialogButtonColorHelper.apply(this);
    }
}
