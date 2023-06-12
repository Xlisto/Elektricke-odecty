package cz.xlisto.odecty.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import cz.xlisto.odecty.R;
import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.modules.hdo.HdoTime;
import cz.xlisto.odecty.ownview.ViewHelper;
import cz.xlisto.odecty.utils.ScreenReceiver;


/**
 * Xlisto 01.06.2023 21:40
 */
public class HdoService extends Service {
    private static final String TAG = "HdoService";
    private final int ID = 10;
    private final Runnable timerTick = this::setNotice;
    private static Timer timer;
    private static boolean runningTimer = false;
    private static boolean runningService = false;
    private static long differentTime = 0;
    private static ArrayList<HdoModel> hdoModels = new ArrayList<>();
    private static String title = "";
    private BroadcastReceiver mReceiver;


    @Override
    public void onCreate() {
        super.onCreate();
        //registrace intentu zapnutí/vypnutí obrazovky
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        HdoNotice.cancelNotice(this, ID);
        runningTimer = false;
        unregisterReceiver(mReceiver);
        runningService = false;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setNotice();
        runningService = true;

        if (!runningTimer) {
            timer = new Timer();
        }

        //výsledek intentu
        if (intent.getBooleanExtra("screen_state", true)) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerTick.run();
                }
            }, 0, 1000);
            runningTimer = true;
        } else {
            timer.cancel();
            runningTimer = false;
        }

        return Service.START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Nastaví upozornění
     */
    private void setNotice() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(calendar.getTimeInMillis() + differentTime);//nastavení kalendáře na aktuální čas + časový posun
        long milins = calendar.getTimeInMillis();
        String time = ViewHelper.convertLongToTime(milins);
        boolean isHdo = HdoTime.checkHdo(hdoModels, calendar);
        if(isHdo) {
            time = time + "   ** Nízský tarif **";
        } else {
            time = time + "   ** Vysoký tarif **";
        }
        HdoNotice.setNotice(HdoService.this, ID, title,time);
    }


    /**
     * Nastaví časový posun
     * @param time časový posun
     */
    public static void setDifferentTime(long time) {
        differentTime = time;
    }


    /**
     * Vrátí časový posun
     * @return časový posun
     */
    public static boolean isRunningService() {
        return runningService;
    }


    /**
     * Nastaví modely HDO
     * @param hdo modely HDO
     */
    public static void setHdoModels(ArrayList<HdoModel> hdo) {
        hdoModels = hdo;
    }


    /**
     * Nastaví titulek
     * @param title titulek
     */
    public static void setTitle(String title) {
        HdoService.title = title;
    }
}
