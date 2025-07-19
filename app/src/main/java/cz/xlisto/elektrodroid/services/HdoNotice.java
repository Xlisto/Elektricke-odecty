package cz.xlisto.elektrodroid.services;


import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;


/**
 * Pomocná třída pro správu notifikací v aplikaci.
 * Umožňuje zobrazit dočasnou notifikaci, notifikaci pro foreground službu
 * a rušit notifikace podle ID.
 * <p>
 * Xlisto 01.06.2023 22:58
 */
public class HdoNotice {

    private static final String TAG = "HdoNotice";
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final String NOTIFICATION_HDO_SERVICE = "HdoService";
    public static final String ARGS_FRAGMENT = "ARGS_FRAGMENT";
    private final static String default_notification_channel_id = "default";
    private final static int TEMP_SERVICE_NOTIFICATION_ID = 10001;


    /**
     * Zobrazí dočasnou notifikaci s daným titulkem a textem. Vhodná pro vyžádání pozornosti uživatele pro znovuspuštění služby
     *
     * @param context     Kontext aplikace
     * @param title       Titulek notifikace
     * @param noticeTitle Název kanálu notifikace
     * @param contentText Text notifikace
     */
    public static void setNotice(Context context, String title, String noticeTitle, String contentText) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, TEMP_SERVICE_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, default_notification_channel_id)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, noticeTitle, importance);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }

        assert notificationManager != null;
        notificationManager.notify(TEMP_SERVICE_NOTIFICATION_ID, builder.build());
    }


    /**
     * Zobrazí notifikaci pro foreground službu a nastaví ji jako aktivní.
     *
     * @param service     Instance služby
     * @param id          ID notifikace
     * @param title       Titulek notifikace
     * @param noticeTitle Název kanálu notifikace
     * @param contentText Text notifikace
     */
    public static void setNotice(Service service, int id, String title, String noticeTitle, String contentText) {

        Intent notificationIntent = new Intent(service, MainActivity.class);
        notificationIntent.putExtra(ARGS_FRAGMENT, NOTIFICATION_HDO_SERVICE);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 10000 + id, notificationIntent, PendingIntent.FLAG_MUTABLE);
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, default_notification_channel_id)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSound(null)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, noticeTitle, importance);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
        assert notificationManager != null;
        notificationManager.notify(id, builder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            service.startForeground(id, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
        else
            service.startForeground(id, builder.build());
        cancelNotice(service.getApplicationContext(), TEMP_SERVICE_NOTIFICATION_ID);
    }


    /**
     * Zruší notifikaci podle zadaného ID.
     *
     * @param context Kontext aplikace
     * @param id      ID notifikace ke zrušení
     */
    public static void cancelNotice(Context context, int id) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

}
