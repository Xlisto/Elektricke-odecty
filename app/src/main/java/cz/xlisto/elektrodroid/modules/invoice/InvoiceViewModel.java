package cz.xlisto.elektrodroid.modules.invoice;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;


/**
 * ViewModel pro správu stavu zaškrtávacích políček v záznamu faktur.
 */
public class InvoiceViewModel extends ViewModel {

    private final MutableLiveData<Map<Integer, Boolean>> checkBoxStates = new MutableLiveData<>(new HashMap<>());


    /**
     * Vrací LiveData objekt obsahující stavy zaškrtávacích políček.
     *
     * @return LiveData objekt s mapou stavů zaškrtávacích políček
     */
    public LiveData<Map<Integer, Boolean>> getCheckBoxStates() {
        return checkBoxStates;
    }


    /**
     * Nastavuje stav zaškrtávacího políčka na dané pozici.
     *
     * @param position  pozice zaškrtávacího políčka
     * @param isChecked nový stav zaškrtávacího políčka (true pokud je zaškrtnuté, jinak false)
     */
    public void setCheckBoxState(int position, boolean isChecked) {
        Map<Integer, Boolean> currentStates = checkBoxStates.getValue();
        if (currentStates != null) {
            currentStates.put(position, isChecked);
            checkBoxStates.setValue(currentStates);
        }
    }

}
