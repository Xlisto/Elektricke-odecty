package cz.xlisto.elektrodroid.modules.invoice;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;


/**
 * Třída InvoiceViewModel rozšiřuje ViewModel a poskytuje data a metody
 * pro správu stavů zaškrtávacích políček, identifikátoru faktury, pozice
 * a zobrazení zaškrtávacího políčka.
 */
public class InvoiceViewModel extends ViewModel {

    // LiveData objekt obsahující stavy zaškrtávacích políček
    private final MutableLiveData<Map<Integer, Boolean>> checkBoxStates = new MutableLiveData<>(new HashMap<>());
    // LiveData objekt obsahující identifikátor faktury
    private final MutableLiveData<Long> idFak = new MutableLiveData<>();
    // LiveData objekt obsahující pozici
    private final MutableLiveData<Integer> position = new MutableLiveData<>();
    // LiveData objekt určující, zda zobrazit zaškrtávací políčko
    private final MutableLiveData<Boolean> showCheckBoxSelect = new MutableLiveData<>();


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


    /**
     * Vrací LiveData objekt obsahující identifikátor faktury.
     *
     * @return LiveData objekt s identifikátorem faktury
     */
    public LiveData<Long> getIdFak() {
        return idFak;
    }


    /**
     * Nastavuje identifikátor faktury.
     *
     * @param idFak nový identifikátor faktury
     */
    public void setIdFak(Long idFak) {
        this.idFak.setValue(idFak);
    }


    /**
     * Vrací LiveData objekt obsahující pozici.
     *
     * @return LiveData objekt s pozicí
     */
    public LiveData<Integer> getPosition() {
        return position;
    }


    /**
     * Nastavuje pozici.
     *
     * @param position nová pozice
     */
    public void setPosition(Integer position) {
        this.position.setValue(position);
    }


    /**
     * Vrací LiveData objekt určující, zda zobrazit zaškrtávací políčko.
     *
     * @return LiveData objekt určující, zda zobrazit zaškrtávací políčko
     */
    public LiveData<Boolean> getShowCheckBoxSelect() {
        return showCheckBoxSelect;
    }


    /**
     * Nastavuje, zda zobrazit zaškrtávací políčko.
     *
     * @param showCheckBoxSelect true pokud zobrazit zaškrtávací políčko, jinak false
     */
    public void setShowCheckBoxSelect(Boolean showCheckBoxSelect) {
        this.showCheckBoxSelect.setValue(showCheckBoxSelect);
    }

}
