package cz.xlisto.cenik.utils;

import android.content.Context;

import cz.xlisto.cenik.databaze.DataSubscriptionPointSource;
import cz.xlisto.cenik.models.SubscriptionPointModel;
import cz.xlisto.cenik.shp.ShPSubscriptionPoint;

import static cz.xlisto.cenik.shp.ShPSubscriptionPoint.ID_SUBSCRIPTION_POINT;

public class SubscriptionPoint {
    /**
     * Načte odběrné místo podle id mista uloženého ve sharedpreferences
     *
     * @param context
     * @return
     */
    static public SubscriptionPointModel load(Context context) {
        long id = -1L;
        ShPSubscriptionPoint shPSubscriptionPoint = new ShPSubscriptionPoint(context);
        id = shPSubscriptionPoint.get(ID_SUBSCRIPTION_POINT, -1L);

        if (id > 0) {
            DataSubscriptionPointSource dataSubscriptionPointSource = new DataSubscriptionPointSource(context);
            dataSubscriptionPointSource.open();
            SubscriptionPointModel subscriptionPoint = dataSubscriptionPointSource.loadSubscriptionPoint(id);
            dataSubscriptionPointSource.close();
            return subscriptionPoint;
        }
        return null;
    }
}
