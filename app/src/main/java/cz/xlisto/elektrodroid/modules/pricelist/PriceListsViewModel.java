package cz.xlisto.elektrodroid.modules.pricelist;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.modules.pricelist.engine.PriceListCalculationEngine;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonScenario;

/**
 * ViewModel pro porovnávání ceníků.
 * Uchovává vybrané ceníky, aktivní scénář a vypočtené výsledky pro všechny záložky.
 */
public class PriceListsViewModel extends ViewModel {

    private final MutableLiveData<PriceListModel> priceListLeft = new MutableLiveData<>();
    private final MutableLiveData<PriceListModel> priceListRight = new MutableLiveData<>();
    private final MutableLiveData<ConsuptionContainer> consuptionContainer = new MutableLiveData<>();

    private final MutableLiveData<PriceListComparisonScenario> scenarioLiveData = new MutableLiveData<>(new PriceListComparisonScenario());
    private final MutableLiveData<List<PriceListModel>> selectedPriceListsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<PriceListComparisonResult>> comparisonResultsLiveData = new MutableLiveData<>(new ArrayList<>());


    /**
     * Nastaví kontejner se zadanou spotřebou a parametry odběrného místa.
     * Přepočítá odpovídající scénář porovnání (převod MWh na kWh) a vyvolá nový výpočet výsledků.
     *
     * @param container kontejner se zadanými hodnotami spotřeby a vlastnostmi jističe a fází
     */
    public void setConsuptionContainer(ConsuptionContainer container) {
        this.consuptionContainer.setValue(container);
        if (container != null) {
            PriceListComparisonScenario scenario = scenarioLiveData.getValue();
            if (scenario == null) scenario = new PriceListComparisonScenario();
            scenario.setVtKwh(container.vt() * 1000.0); // Container holds MWh
            scenario.setNtKwh(container.nt() * 1000.0);
            scenario.setMonths(container.month());
            scenario.setCountPhaze(container.phaze());
            scenario.setPowerBreaker(container.power());
            scenario.setServicesLeft(container.servicesL());
            scenario.setServicesRight(container.servicesR());
            setScenario(scenario);
        }
    }


    /**
     * Nastaví scénář porovnání ceníků a vyvolá přepočet výsledků.
     *
     * @param scenario nový scénář s parametry spotřeby a služeb
     */
    public void setScenario(PriceListComparisonScenario scenario) {
        this.scenarioLiveData.setValue(scenario);
        reevaluateResults();
    }


    /**
     * Nastaví seznam vybraných ceníků pro porovnání.
     * Automaticky přiřadí první ceník jako levý a případný druhý ceník jako pravý,
     * a následně vyvolá přepočet výsledků.
     *
     * @param priceLists seznam ceníků určených k porovnání
     */
    public void setSelectedPriceLists(List<PriceListModel> priceLists) {
        this.selectedPriceListsLiveData.setValue(priceLists != null ? priceLists : new ArrayList<>());
        if (priceLists != null && !priceLists.isEmpty()) {
            priceListLeft.setValue(priceLists.get(0));
            if (priceLists.size() > 1) {
                priceListRight.setValue(priceLists.get(1));
            }
        }
        reevaluateResults();
    }


    /**
     * Provede přepočet výsledků porovnání ceníků na základě aktuálního scénáře a seznamu vybraných ceníků.
     */
    public void reevaluateResults() {
        PriceListComparisonScenario scenario = scenarioLiveData.getValue();
        List<PriceListModel> priceLists = selectedPriceListsLiveData.getValue();

        if (scenario != null && priceLists != null) {
            List<PriceListComparisonResult> results = PriceListCalculationEngine.evaluateAll(priceLists, scenario);
            comparisonResultsLiveData.setValue(results);
        }
    }


    /**
     * Vrátí {@link LiveData} s aktuálním kontejnerem spotřeby a parametrů.
     *
     * @return {@link LiveData} obsahující {@link ConsuptionContainer}
     */
    public LiveData<ConsuptionContainer> getConsuptionContainer() {
        return consuptionContainer;
    }


    /**
     * Vrátí {@link LiveData} s aktuálním scénářem porovnání ceníků.
     *
     * @return {@link LiveData} obsahující {@link PriceListComparisonScenario}
     */
    public LiveData<PriceListComparisonScenario> getScenario() {
        return scenarioLiveData;
    }


    /**
     * Vrátí {@link LiveData} se seznamem ceníků vybraných k porovnání.
     *
     * @return {@link LiveData} obsahující seznam {@link PriceListModel}
     */
    public LiveData<List<PriceListModel>> getSelectedPriceLists() {
        return selectedPriceListsLiveData;
    }


    /**
     * Vrátí {@link LiveData} s vypočtenými výsledky porovnání pro jednotlivé ceníky.
     *
     * @return {@link LiveData} obsahující seznam {@link PriceListComparisonResult}
     */
    public LiveData<List<PriceListComparisonResult>> getComparisonResults() {
        return comparisonResultsLiveData;
    }
}
