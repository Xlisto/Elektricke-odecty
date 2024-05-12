package cz.xlisto.odecty.utils;


import static cz.xlisto.odecty.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT_LONG;

import android.content.Context;

import cz.xlisto.odecty.databaze.DataSubscriptionPointSource;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.shp.ShPSubscriptionPoint;


public class SubscriptionPoint {
    /**
     * Načte odběrné místo podle id mista uloženého ve sharedpreferences
     *
     * @param context Kontext aplikace
     * @return SubscriptionPointModel - aktuálně zvolené vybrané místo
     */
    static public SubscriptionPointModel load(Context context) {
        long id;
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
        id = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT_LONG, -1L);

        if (id > 0) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
            dataSubscriptionPointSource.open();
            SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(id);
            dataSubscriptionPointSource.close();
            return subscriptionPoint;
        }
        return null;
    }


    /**
     * Načte počet odběrných míst v databázi
     *
     * @param context Kontext aplikace
     * @return int - počet odběrných míst v databázi
     */
    static public int count(Context context) {
        DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
        dataSubscriptionPointSource.open();
        int count = dataSubscriptionPointSource.countSubscriptionPoints();
        dataSubscriptionPointSource.close();
        return count;
    }
}
