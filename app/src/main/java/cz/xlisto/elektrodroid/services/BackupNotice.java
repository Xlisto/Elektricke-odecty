package cz.xlisto.elektrodroid.services;


import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;


/**
 * Notifikace pro zálohování (čekající upload, úspěšný upload).
 */
public final class BackupNotice {

    public static final String NOTIFICATION_FRAGMENT = "Backup";
    private static final String CHANNEL_ID = "backup_notification_channel";
    private static final int PENDING_UPLOAD_NOTIFICATION_ID = 50001;
    private static final int SUCCESS_UPLOAD_NOTIFICATION_ID = 50002;


    private BackupNotice() {
    }


    /**
     * Zobrazí notifikaci o čekajícím odeslání zálohy (nedostupný internet).
     */
    public static void showPendingUploadNotice(Context context, String contentText) {
        showNotice(context, context.getString(R.string.wifi_required_title), contentText, PENDING_UPLOAD_NOTIFICATION_ID);
    }


    /**
     * Zobrazí notifikaci o úspěšném odeslání zálohy.
     */
    public static void showUploadSuccessNotice(Context context, String contentText) {
        showNotice(context, context.getString(R.string.upload_completed), contentText, SUCCESS_UPLOAD_NOTIFICATION_ID);
    }


    private static void showNotice(Context context, String title, String contentText, int notificationId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(HdoNotice.ARGS_FRAGMENT, NOTIFICATION_FRAGMENT);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Backup Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(CHANNEL_ID);
        }

        notificationManager.notify(notificationId, builder.build());
    }

}
