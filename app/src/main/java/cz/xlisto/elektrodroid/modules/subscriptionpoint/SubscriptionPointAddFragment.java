package cz.xlisto.elektrodroid.modules.subscriptionpoint;


import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.Keyboard;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;

/**
 * Fragment pro vytvoření nového odběrného místa.
 *
 * <p>Rozšiřuje {@link SubscriptionPointAddEditAbstract} třídu a implementuje scénář vytvoření
 * zcela nového odběrného místa. Formulář umožňuje zadat všechny potřebné informace
 * (název, popis, počet fází, číslo elektroměru, atd.).</p>
 *
 * <p>Klíčová vlastnost - Automatické nastavení jako aktuální:</p>
 * <ul>
 *   <li>Po úspěšném uložení nového místa se automaticky nastaví jako aktuálně vybrané</li>
 *   <li>Uloží se do SharedPreferences pro okamžitý přístup</li>
 *   <li>Uloží se do databáze (settings table) pro persistenci přes backup/restore</li>
 * </ul>
 * </p>
 *
 * <p>Postup:</p>
 * <ol>
 *   <li>Uživatel vyplní formulář a klikne "Uložit"</li>
 *   <li>Nové místo se vytvoří a vloží do databáze, vrátí se ID</li>
 *   <li> uloží toto ID</li>
 *   <li>Klávesnice se skryje a fragment se zavře</li>
 * </ol>
 * </p>
 *
 * @see SubscriptionPointAddEditAbstract
 */
public class SubscriptionPointAddFragment extends SubscriptionPointAddEditAbstract {

    public SubscriptionPointAddFragment() {
        // Required empty public constructor
    }


    public static SubscriptionPointAddFragment newInstance() {
        return new SubscriptionPointAddFragment();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnSave.setOnClickListener(v -> {
            long id = save(createSubscriptionPoint());

            // Uloží nové místo jako aktuální i do tabulky nastavení.
            SubscriptionPoint.setCurrentSelection(requireContext(), id);

            Keyboard.hide(requireActivity());
            getParentFragmentManager().popBackStack();
        });
    }


    /**
     * Vytvoří objekt SubscriptionPointModel z dat z formuláře a uloží ho do databáze.
     *
     * <p>Tato metoda:</p>
     * <ol>
     *   <li>Otevře připojení k databázi</li>
     *   <li>Vloží nový záznam odběrného místa</li>
     *   <li>Vrátí ID nově vytvořeného záznamu</li>
     *   <li>Zavře připojení k databázi</li>
     * </ol>
     * </p>
     *
     * @param subscriptionPoint objekt SubscriptionPointModel s údaji z formuláře
     * @return long - ID nově vytvořeného záznamu v databázi
     *
     * @see cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource#insertSubscriptionPoint(cz.xlisto.elektrodroid.models.SubscriptionPointModel)
     */
    private long save(SubscriptionPointModel subscriptionPoint) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(getActivity());
        dataSubscriptionPointSource.open();
        long id = dataSubscriptionPointSource.insertSubscriptionPoint(subscriptionPoint);
        dataSubscriptionPointSource.close();
        return id;
    }
}