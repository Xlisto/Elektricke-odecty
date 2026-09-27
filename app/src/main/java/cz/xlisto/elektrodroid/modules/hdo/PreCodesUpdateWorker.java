package cz.xlisto.elektrodroid.modules.hdo;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.databaze.DataSubscriptionPointSource;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.shp.ShPPreCodes;


/**
 * Worker pro asynchronní stahování a aktualizaci kódů povelů PRE (1x týdně na Wi-Fi).
 */
public class PreCodesUpdateWorker extends Worker {

    private static final String LOG_TAG = "PreCodesUpdateWorker";
    private static final String PRE_HDO_URL = "https://www.predistribuce.cz/cs/potrebuji-zaridit/zakaznici/stav-hdo/";


    public PreCodesUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 1. Kontrola, zda je u některého z odběrných míst nastaveno PRE
        if (!isPreDistributionUsed(context)) {
            Log.d(LOG_TAG, "Žádné odběrné místo nepoužívá distribuci PRE. Aktualizace kódů přeskočena.");
            return Result.success();
        }

        // 2. Kontrola připojení k Wi-Fi
        if (!isWifiConnected(context)) {
            Log.d(LOG_TAG, "Zařízení není připojeno k Wi-Fi. Aktualizace kódů odložena.");
            return Result.retry();
        }

        // 3. Načtení a parsování stránek PRE
        try {
            Document doc = Jsoup.connect(PRE_HDO_URL).timeout(15000).get();
            Elements options = doc.getElementsByTag("option");
            ArrayList<String> codes = new ArrayList<>();

            for (int i = 0; i < options.size(); i++) {
                String text = options.get(i).text().trim();
                if (!text.isEmpty() && !text.equalsIgnoreCase("vyberte") && !text.toLowerCase().contains("zvolte")) {
                    codes.add(text);
                }
            }

            if (!codes.isEmpty()) {
                ShPPreCodes shPPreCodes = new ShPPreCodes(context);
                shPPreCodes.savePreCodes(codes);
                Log.d(LOG_TAG, "Seznam kódů PRE byl úspěšně aktualizován (" + codes.size() + " kódů).");
                return Result.success();
            } else {
                Log.w(LOG_TAG, "Ze stránek PRE nebyly načteny žádné kódy.");
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Chyba při stahování kódů PRE ze serveru", e);
            return Result.retry();
        }
    }


    /**
     * Ověří, zda v databázi existuje alespoň jedno odběrné místo s distribucí PRE
     */
    private boolean isPreDistributionUsed(Context context) {
        DataSubscriptionPointSource spSource = new DataSubscriptionPointSource(context);
        spSource.open();
        ArrayList<SubscriptionPointModel> points = spSource.loadSubscriptionPoints();
        spSource.close();

        if (points.isEmpty()) return false;

        DataHdoSource hdoSource = new DataHdoSource(context);
        hdoSource.open();
        for (SubscriptionPointModel point : points) {
            String area = hdoSource.getDistributionArea(point.getTableHDO());
            if ("PRE".equalsIgnoreCase(area)) {
                hdoSource.close();
                return true;
            }
        }
        hdoSource.close();
        return false;
    }


    /**
     * Ověří, zda je zařízení připojeno k neúčtované/Wi-Fi síti
     */
    private boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED));
    }

}
