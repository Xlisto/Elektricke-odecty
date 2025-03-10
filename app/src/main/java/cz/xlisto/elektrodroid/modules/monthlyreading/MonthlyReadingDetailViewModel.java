package cz.xlisto.elektrodroid.modules.monthlyreading;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import cz.xlisto.elektrodroid.models.MonthlyReadingModel;
import cz.xlisto.elektrodroid.models.PriceListModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * ViewModel pro detail měsíčního odečtu
 * Xlisto 17.03.2024 18:54
 */
public class MonthlyReadingDetailViewModel extends ViewModel {

    private static final String TAG = "MonthlyReadingDetailViewModel";

    private final MutableLiveData<MonthlyReadingModel> monthlyReadingCurrently = new MutableLiveData<>();
    private final MutableLiveData<MonthlyReadingModel> monthlyReadingPrevious = new MutableLiveData<>();
    private final MutableLiveData<PriceListModel> priceList = new MutableLiveData<>();
    private final MutableLiveData<SubscriptionPointModel> subscriptionPoint = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showRegulPrice = new MutableLiveData<>(false);


    /**
     * Nastaví objekt `MutableLiveData` na zadanou hodnotu, která obsahuje informace o aktuálním měsíčním odečtu.
     *
     * @param monthlyReadingCurrently Objekt `MonthlyReadingModel`, který obsahuje informace o aktuálním měsíčním odečtu
     */
    public void setMonthlyReadingCurrently(MonthlyReadingModel monthlyReadingCurrently) {
        this.monthlyReadingCurrently.setValue(monthlyReadingCurrently);
    }


    /**
     * Nastaví objekt `MutableLiveData` na zadanou hodnotu, která obsahuje informace o předchozím měsíčním odečtu.
     *
     * @param monthlyReadingPrevious Objekt `MonthlyReadingModel`, který obsahuje informace o předchozím měsíčním odečtu
     */
    public void setMonthlyReadingPrevious(MonthlyReadingModel monthlyReadingPrevious) {
        this.monthlyReadingPrevious.setValue(monthlyReadingPrevious);
    }


    /**
     * Nastaví objekt `MutableLiveData` na zadanou hodnotu, která obsahuje informace o ceníku.
     *
     * @param priceList Objekt `PriceListModel`, který obsahuje informace o ceníku
     */
    public void setPriceList(PriceListModel priceList) {
        this.priceList.setValue(priceList);
    }


    /**
     * Nastaví objekt `MutableLiveData` na zadanou hodnotu, která obsahuje informace o odběrném místě.
     *
     * @param subscriptionPoint Objekt `SubscriptionPointModel`, který obsahuje informace o odběrném místě
     */
    public void setSubscriptionPoint(SubscriptionPointModel subscriptionPoint) {
        this.subscriptionPoint.setValue(subscriptionPoint);
    }


    /**
     * Nastaví hodnotu `showRegulPrice`.
     * <p>
     * Tato metoda nastaví objekt `MutableLiveData` na zadanou hodnotu, která určuje, zda se má zobrazit regulovaná cena.
     *
     * @param showRegulPrice Boolean hodnota, která určuje, zda se má zobrazit regulovaná cena
     */
    public void setShowRegulPrice(Boolean showRegulPrice) {
        this.showRegulPrice.setValue(showRegulPrice);
    }


    /**
     * Vrací objekt `MutableLiveData`, který obsahuje informace o aktuálním měsíčním odečtu.
     *
     * @return Objekt `MutableLiveData` obsahující informace o aktuálním měsíčním odečtu
     */
    public MutableLiveData<MonthlyReadingModel> getMonthlyReadingCurrently() {
        return monthlyReadingCurrently;
    }


    /**
     * Vrací objekt `MutableLiveData`, který obsahuje informace o předchozím měsíčním odečtu.
     *
     * @return Objekt `MutableLiveData` obsahující informace o předchozím měsíčním odečtu
     */
    public MutableLiveData<MonthlyReadingModel> getMonthlyReadingPrevious() {
        return monthlyReadingPrevious;
    }


    /**
     * Vrací objekt `MutableLiveData`, který obsahuje informace o ceníku.
     *
     * @return Objekt `MutableLiveData` obsahující informace o ceníku
     */
    public MutableLiveData<PriceListModel> getPriceList() {
        return priceList;
    }


    /**
     * Vrací objekt `MutableLiveData`, který obsahuje informace o odběrném místě.
     *
     * @return Objekt `MutableLiveData` obsahující informace o odběrném místě
     */
    public MutableLiveData<SubscriptionPointModel> getSubscriptionPoint() {
        return subscriptionPoint;
    }


    /**
     * Vrací hodnotu `showRegulPrice`.
     * <p>
     * Tato metoda vrací objekt `MutableLiveData`, který obsahuje informaci, zda se má zobrazit regulovaná cena.
     *
     * @return Objekt `MutableLiveData` obsahující hodnotu `showRegulPrice`
     */
    public MutableLiveData<Boolean> getShowRegulPrice() {
        return showRegulPrice;
    }

}
