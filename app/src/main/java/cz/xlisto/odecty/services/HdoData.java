package cz.xlisto.odecty.services;

import android.content.Context;

import java.util.ArrayList;

import cz.xlisto.odecty.R;
import cz.xlisto.odecty.databaze.DataHdoSource;
import cz.xlisto.odecty.databaze.DataSettingsSource;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.models.SubscriptionPointModel;
import cz.xlisto.odecty.utils.SubscriptionPoint;


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
        ArrayList<HdoModel> hdoModels = dataHdoSource.loadHdo(subscriptionPoint.getTableHDO());
        dataHdoSource.close();
        HdoService.setHdoModels(hdoModels);
        HdoService.setTitle(context.getResources().getString(R.string.app_name) + " - " + subscriptionPoint.getName());

        DataSettingsSource dataSettingsSource = new DataSettingsSource(context);
        dataSettingsSource.open();
        long timeShift = dataSettingsSource.loadTimeShift(subscriptionPoint.getId());
        dataSettingsSource.close();
        HdoService.setDifferentTime(timeShift);
    }
}
