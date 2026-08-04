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
 * Notifikace pro připomenutí provedení měsíčního zápisu.
 */
public final class MonthlyReadingReminderNotice {

    public static final String NOTIFICATION_FRAGMENT = "MonthlyReadingReminder";

    private static final String CHANNEL_ID = "monthly_reading_reminder_channel";
    private static final int NOTIFICATION_ID = 40001;


    private MonthlyReadingReminderNotice() {
    }


    public static void setNotice(Context context, String title, String contentText, long subscriptionPointId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(HdoNotice.ARGS_FRAGMENT, NOTIFICATION_FRAGMENT);
        if (subscriptionPointId > 0) {
            intent.putExtra(HdoNotice.EXTRA_SUBSCRIPTION_POINT_ID, subscriptionPointId);
        }

        int requestCode = subscriptionPointId > 0 ? ("reading_open_" + subscriptionPointId).hashCode() : NOTIFICATION_ID;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
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
                    context.getString(R.string.monthly_reading_reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(CHANNEL_ID);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}

