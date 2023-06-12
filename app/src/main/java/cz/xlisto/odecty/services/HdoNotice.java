package cz.xlisto.odecty.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import cz.xlisto.odecty.MainActivity;
import cz.xlisto.odecty.R;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Xlisto 01.06.2023 22:58
 */
public class HdoNotice {
    private static final String TAG = "HdoNotice";
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    public static final String NOTIFICATION_HDO_SERVICE = "HdoService";
    public static final String ARGS_FRAGMENT = "ARGS_FRAGMENT";
    private final static String default_notification_channel_id = "default";


    public static void setNotice(Service service, int id, String title, String contentText) {

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
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "HdoService", importance);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
        assert notificationManager != null;
        notificationManager.notify(id, builder.build());
        service.startForeground(id, builder.build());
    }


    public static void cancelNotice(Context context, int id) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }
}
