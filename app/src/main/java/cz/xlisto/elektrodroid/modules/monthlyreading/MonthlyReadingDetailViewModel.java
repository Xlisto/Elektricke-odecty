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


    public void setMonthlyReadingCurrently(MonthlyReadingModel monthlyReadingCurrently) {
        this.monthlyReadingCurrently.setValue(monthlyReadingCurrently);
    }

    public void setMonthlyReadingPrevious(MonthlyReadingModel monthlyReadingPrevious) {
        this.monthlyReadingPrevious.setValue(monthlyReadingPrevious);
    }

    public void setPriceList(PriceListModel priceList) {
        this.priceList.setValue(priceList);
    }

    public void setSubscriptionPoint(SubscriptionPointModel subscriptionPoint) {
        this.subscriptionPoint.setValue(subscriptionPoint);
    }

    public void setShowRegulPrice(Boolean showRegulPrice) {
        this.showRegulPrice.setValue(showRegulPrice);
    }

    public MutableLiveData<MonthlyReadingModel> getMonthlyReadingCurrently() {
        return monthlyReadingCurrently;
    }

    public MutableLiveData<MonthlyReadingModel> getMonthlyReadingPrevious() {
        return monthlyReadingPrevious;
    }

    public MutableLiveData<PriceListModel> getPriceList() {
        return priceList;
    }

    public MutableLiveData<SubscriptionPointModel> getSubscriptionPoint() {
        return subscriptionPoint;
    }

    public MutableLiveData<Boolean> getShowRegulPrice() {
        return showRegulPrice;
    }
}
