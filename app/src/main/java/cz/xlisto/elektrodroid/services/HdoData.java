package cz.xlisto.elektrodroid.services;


import android.content.Context;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataSettingsSource;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPHdo;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Třída pro načtení dat z databáze pro službu HdoService
 * Xlisto 13.06.2023 10:22
 */
public class HdoData {

    private static final String TAG = "HdoLoadData";


    /**
     * Načte data z databáze a nastaví je do služby HdoService
     *
     * @param context Kontext aplikace
     */
    public static void loadHdoData(Context context) {
        SubscriptionPointModel subscriptionPoint = SubscriptionPoint.load(context);

        if (subscriptionPoint == null) {
            return;
        }
        DataHdoSource dataHdoSource = new DataHdoSource(context);
        dataHdoSource.open();
        ShPHdo shPHdo = new ShPHdo(context);
        String rele = shPHdo.get(ShPHdo.ARG_RELE + subscriptionPoint.getTableHDO(), "");
        String title = context.getResources().getString(R.string.app_name) + " - " + subscriptionPoint.getName();
        if (rele.isEmpty()) {

            ArrayList<String> reles = dataHdoSource.getReles(subscriptionPoint.getTableHDO());

            if (!reles.isEmpty())
                rele = reles.get(0);
        }
        if (!rele.isEmpty()) {
            title += " - " + rele;
        }

        ArrayList<HdoModel> hdoModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO(), null, null, rele);
        dataHdoSource.close();
        HdoService.setHdoModels(hdoModels);
        HdoService.setTitle(title);

        DataSettingsSource dataSettingsSource = new DataSettingsSource(context);
        dataSettingsSource.open();
        long timeShift = dataSettingsSource.loadTimeShift(subscriptionPoint.getId());
        dataSettingsSource.close();
        HdoService.setDifferentTime(timeShift);
    }

}
