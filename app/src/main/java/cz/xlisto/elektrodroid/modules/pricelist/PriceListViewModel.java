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


public class PriceListViewModel extends AndroidViewModel {

    private static final String TAG = "PriceListViewModel";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Event<Boolean>> saveResultEvent = new MutableLiveData<>();


    public PriceListViewModel(@NonNull Application application) {
        super(application);
    }


    public LiveData<Event<Boolean>> getSaveResultEvent() {
        return saveResultEvent;
    }


    /**
     * Připraví a uloží dvě položky ceníku asynchronně.
     * <p>
     * Pokud je {@code isAdd} true, obě položky se vloží jako nové záznamy.
     * Pokud je {@code isAdd} false, první položka se aktualizuje (její id se nastaví na {@code itemId})
     * a druhá položka se vloží jako nový záznam.
     * <p>
     * Operace probíhá v samostatném vlákně a používá {@code DataPriceListSource} pro přístup k databázi.
     * Po dokončení se do {@code saveResultEvent} pošle {@code Event<Boolean>}:
     * true = úspěch (oba vrácené id > 0), false = selhání nebo výjimka.
     *
     * @param first  první model ceníku
     * @param second druhý model ceníku
     * @param isAdd  true = přidat obě položky, false = aktualizovat první a přidat druhou
     * @param itemId id položky k aktualizaci (pouze při {@code isAdd == false})
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
                    idSecond = dataSource.insertPriceList(secondMerged);
                } else {
                    first.setId(itemId);
                    idFirst = dataSource.updatePriceList(firstMerged, itemId);
                    idSecond = dataSource.insertPriceList(secondMerged);
                }
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
     * Sloučí zadaný PriceListModel s regulovanými hodnotami načtenými z ReadRawJSON.
     * Vytvoří interní Calendar objekty z polí platnostOD a platnostDO, zavolá ReadRawJSON.read(...)
     * a zkopíruje vypočtené/regulované hodnoty (systemSluzby, cinnost, poze1/2, DPH, dan, distVT/distNT, J0..J14)
     * do předaného objektu.
     * Metoda modifikuje vstupní objekt a zároveň jej vrací pro pohodlné řetězení.
     *
     * @param priceList vstupní PriceListModel, jehož hodnoty budou aktualizovány
     * @return stejný objekt PriceListModel s aktualizovanými regulovanými hodnotami
     */
    private PriceListModel mergePriceLists(PriceListModel priceList) {
        ReadRawJSON reader = new ReadRawJSON(getApplication());
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTimeInMillis(priceList.getPlatnostOD());
        endCal.setTimeInMillis(priceList.getPlatnostDO());

        PriceListModel priceListWithRegulated = reader.read(startCal, endCal, priceList.getDistribuce(), priceList.getSazba());
        priceList.setSystemSluzby(priceListWithRegulated.getSystemSluzby());
        priceList.setCinnost(priceListWithRegulated.getCinnost());
        priceList.setPoze1(priceListWithRegulated.getPoze1());
        priceList.setPoze2(priceListWithRegulated.getPoze2());
        priceList.setDph(priceListWithRegulated.getDph());
        priceList.setCinnost(priceListWithRegulated.getCinnost());
        priceList.setDan(priceListWithRegulated.getDan());

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


    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

}
