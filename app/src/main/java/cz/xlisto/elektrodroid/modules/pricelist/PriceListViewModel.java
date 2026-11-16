package cz.xlisto.elektrodroid.modules.pricelist;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.databaze.DataPriceListSource;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.utils.Event;
import cz.xlisto.elektrodroid.utils.ReadRawJSON;


/**
 * ViewModel pro správu a uložení ceníků elektřiny.
 * Zajišťuje asynchronní doplňování regulovaných složek cen z JSON konfigurace
 * a jejich uložení do databáze ceníků.
 */
public class PriceListViewModel extends AndroidViewModel {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Event<Boolean>> saveResultEvent = new MutableLiveData<>();


    /**
     * Vytvoří novou instanci {@link PriceListViewModel}.
     *
     * @param application aplikační kontext
     */
    public PriceListViewModel(@NonNull Application application) {
        super(application);
    }


    /**
     * Vrací {@link LiveData} s událostí výstupního stavu operace uložení ceníků.
     *
     * @return {@link LiveData} obsahující {@link Event} s hodnotou {@code true} při úspěchu, {@code false} při chybě
     */
    public LiveData<Event<Boolean>> getSaveResultEvent() {
        return saveResultEvent;
    }


    /**
     * Připraví a uloží dvě položky ceníku asynchronně.
     * <p>
     * Před uložením sloučí obě položky s regulovanými cenami načtenými z JSON konfigurace.
     * Pokud je {@code isAdd} {@code true}, obě položky se vloží jako nové záznamy.
     * Pokud je {@code isAdd} {@code false}, první položka se aktualizuje (její ID se nastaví na {@code itemId})
     * a druhá položka se vloží jako nový záznam.
     * <p>
     * Operace probíhá na pozadí a používá {@link DataPriceListSource} pro přístup k databázi.
     * Po dokončení publikuje do {@link #getSaveResultEvent()} událost {@link Event}:
     * {@code true} = úspěch (obě ID > 0), {@code false} = selhání nebo výjimka.
     *
     * @param first  první model ceníku {@link PriceListModel}
     * @param second druhý model ceníku {@link PriceListModel}
     * @param isAdd  {@code true} pro vložení obou položek, {@code false} pro aktualizaci první a vložení druhé
     * @param itemId ID položky k aktualizaci (použije se pouze při {@code isAdd == false})
     */
    public void preparePriceLists(PriceListModel first, PriceListModel second, boolean isAdd, Long itemId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PriceListModel firstMerged = mergePriceLists(first);
            PriceListModel secondMerged = mergePriceLists(second);

            DataPriceListSource dataSource = new DataPriceListSource(getApplication());
            try {
                dataSource.open();
                long idFirst, idSecond;
                if (isAdd) {
                    idFirst = dataSource.insertPriceList(firstMerged);
                } else {
                    first.setId(itemId);
                    idFirst = dataSource.updatePriceList(firstMerged, itemId);
                }
                idSecond = dataSource.insertPriceList(secondMerged);
                boolean success = idFirst > 0 && idSecond > 0;
                saveResultEvent.postValue(new Event<>(success));
            } catch (Exception e) {
                saveResultEvent.postValue(new Event<>(false));
            } finally {
                dataSource.close();
                executor.shutdown();
            }
        });
    }


    /**
     * Sloučí zadaný {@link PriceListModel} s regulovanými hodnotami načtenými pomocí {@link ReadRawJSON}.
     * Vytvoří interní objekty {@link Calendar} z časů platnosti ceníku, načte regulované ceny
     * a zkopíruje je (systémové služby, činnost OTE, POZE, DPH, daň z elektřiny, distribuce VT/NT, jističe J0-J14)
     * do předaného objektu.
     * Metoda modifikuje vstupní objekt a zároveň jej vrací.
     *
     * @param priceList vstupní {@link PriceListModel}, jehož hodnoty budou aktualizovány
     * @return stejný objekt {@link PriceListModel} s doplněnými regulovanými hodnotami
     */
    private PriceListModel mergePriceLists(PriceListModel priceList) {
        ReadRawJSON reader = new ReadRawJSON(getApplication());
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTimeInMillis(priceList.getValidFrom());
        endCal.setTimeInMillis(priceList.getValidTo());

        PriceListModel priceListWithRegulated = reader.read(startCal, endCal, priceList.getDistribution(), priceList.getRate());
        priceList.setSystemServices(priceListWithRegulated.getSystemServices());
        priceList.setActivity(priceListWithRegulated.getActivity());
        priceList.setPoze1(priceListWithRegulated.getPoze1());
        priceList.setPoze2(priceListWithRegulated.getPoze2());
        priceList.setVat(priceListWithRegulated.getVat());
        priceList.setActivity(priceListWithRegulated.getActivity());
        priceList.setTax(priceListWithRegulated.getTax());

        priceList.setDistVT(priceListWithRegulated.getDistVT());
        priceList.setDistNT(priceListWithRegulated.getDistNT());

        priceList.setJ0(priceListWithRegulated.getJ0());
        priceList.setJ1(priceListWithRegulated.getJ1());
        priceList.setJ2(priceListWithRegulated.getJ2());
        priceList.setJ3(priceListWithRegulated.getJ3());
        priceList.setJ4(priceListWithRegulated.getJ4());
        priceList.setJ5(priceListWithRegulated.getJ5());
        priceList.setJ6(priceListWithRegulated.getJ6());
        priceList.setJ7(priceListWithRegulated.getJ7());
        priceList.setJ8(priceListWithRegulated.getJ8());
        priceList.setJ9(priceListWithRegulated.getJ9());
        priceList.setJ10(priceListWithRegulated.getJ10());
        priceList.setJ11(priceListWithRegulated.getJ11());
        priceList.setJ12(priceListWithRegulated.getJ12());
        priceList.setJ13(priceListWithRegulated.getJ13());
        priceList.setJ14(priceListWithRegulated.getJ14());

        return priceList;
    }


    /**
     * Uvolní zdroje při zničení ViewModelu a ukončí probíhající úlohy v {@link ExecutorService}.
     */
    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }

}
