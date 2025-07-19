package cz.xlisto.elektrodroid.utils;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.services.HdoNotice;


/**
 * Worker třída pro asynchronní načítání HDO dat a zobrazení notifikace.
 * Spouští se na pozadí pomocí WorkManageru.
 * <p>
 * Xlisto 18.07.2025
 */
public class HdoWorker extends Worker {

    /**
     * Konstruktor pro HdoWorker.
     *
     * @param context Kontext aplikace
     * @param params  Parametry workeru
     */
    public HdoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }


    /**
     * Hlavní metoda workeru - zobrazí notifikaci.
     *
     * @return Výsledek práce workeru (úspěch/neúspěch)
     */
    @NonNull
    @Override
    public ListenableWorker.Result doWork() {

        HdoNotice.setNotice(getApplicationContext(),
                getApplicationContext().getResources().getString(R.string.app_name),
                getApplicationContext().getResources().getString(R.string.hdo_service_name),
                getApplicationContext().getResources().getString(R.string.hdo_click));

        return ListenableWorker.Result.success();
    }

}
