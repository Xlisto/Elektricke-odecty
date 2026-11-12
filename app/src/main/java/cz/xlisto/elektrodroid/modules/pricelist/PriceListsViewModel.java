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


    public void setScenario(PriceListComparisonScenario scenario) {
        this.scenarioLiveData.setValue(scenario);
        reevaluateResults();
    }


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


    public void reevaluateResults() {
        PriceListComparisonScenario scenario = scenarioLiveData.getValue();
        List<PriceListModel> priceLists = selectedPriceListsLiveData.getValue();

        if (scenario != null && priceLists != null) {
            List<PriceListComparisonResult> results = PriceListCalculationEngine.evaluateAll(priceLists, scenario);
            comparisonResultsLiveData.setValue(results);
        }
    }


    public LiveData<ConsuptionContainer> getConsuptionContainer() {
        return consuptionContainer;
    }


    public LiveData<PriceListComparisonScenario> getScenario() {
        return scenarioLiveData;
    }


    public LiveData<List<PriceListModel>> getSelectedPriceLists() {
        return selectedPriceListsLiveData;
    }


    public LiveData<List<PriceListComparisonResult>> getComparisonResults() {
        return comparisonResultsLiveData;
    }
}
