package cz.xlisto.elektrodroid.services;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;
import cz.xlisto.elektrodroid.utils.SubscriptionPoint;


/**
 * Receiver pro připomínku měsíčního odečtu.
 */
public class MonthlyReadingReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "MonthlyReadReminderRc";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !MonthlyReadingReminderScheduler.ACTION_MONTHLY_READING_REMINDER.equals(intent.getAction())) {
            return;
        }

        long scheduledAt = intent.getLongExtra(MonthlyReadingReminderScheduler.EXTRA_SCHEDULED_TIME, -1L);
        long subscriptionPointId = intent.getLongExtra(MonthlyReadingReminderScheduler.EXTRA_SUBSCRIPTION_POINT_ID, -1L);
        long now = System.currentTimeMillis();
        Log.d(TAG, "Received monthly reading reminder, scheduled=" + scheduledAt + ", now=" + now + ", delayMs=" + (scheduledAt > 0 ? now - scheduledAt : -1));

        SubscriptionPointModel currentSubscriptionPoint = SubscriptionPoint.load(context);
        if (currentSubscriptionPoint == null) {
            MonthlyReadingReminderScheduler.cancel(context);
            return;
        }

        if (subscriptionPointId > 0 && currentSubscriptionPoint.getId() != subscriptionPointId) {
            MonthlyReadingReminderScheduler.rescheduleCurrentAsync(context);
            return;
        }

        if (MonthlyReadingReminderScheduler.hasReadingForCurrentPeriod(context, currentSubscriptionPoint)) {
            MonthlyReadingReminderScheduler.rescheduleCurrentAsync(context);
            return;
        }

        String title = context.getString(R.string.monthly_reading_reminder_title);
        String placeName = currentSubscriptionPoint.getName();
        String content = placeName == null || placeName.trim().isEmpty()
                ? context.getString(R.string.monthly_reading_reminder_message)
                : context.getString(R.string.monthly_reading_reminder_message_place, placeName);
        MonthlyReadingReminderNotice.setNotice(context, title, content, currentSubscriptionPoint.getId());
        MonthlyReadingReminderScheduler.rescheduleCurrentAsync(context);
    }

}


