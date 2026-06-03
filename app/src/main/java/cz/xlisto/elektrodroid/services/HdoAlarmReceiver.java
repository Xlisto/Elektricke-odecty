package cz.xlisto.elektrodroid.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.databaze.DataHdoSource;
import cz.xlisto.elektrodroid.models.HdoModel;

/**
 * BroadcastReceiver pro zpracování naplánovaných HDO alarmů.
 * <p>
 * Po přijetí alarmu ověří stav záznamu v databázi, vytvoří notifikaci
 * a naplánuje další výskyt stejného typu události.
 */
public class HdoAlarmReceiver extends BroadcastReceiver {

    /**
     * Zpracuje alarm pro začátek nebo konec HDO a případně zobrazí notifikaci.
     *
     * @param context kontext aplikace
     * @param intent  přijatý broadcast intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !HdoAlarmScheduler.ACTION_HDO_ALARM.equals(intent.getAction())) {
            return;
        }

        String table = intent.getStringExtra(HdoAlarmScheduler.EXTRA_TABLE);
        long hdoId = intent.getLongExtra(HdoAlarmScheduler.EXTRA_HDO_ID, -1L);
        int type = intent.getIntExtra(HdoAlarmScheduler.EXTRA_TYPE, -1);
        if (table == null || table.isEmpty() || hdoId <= 0 || type < 0) {
            return;
        }

        long scheduledAt = intent.getLongExtra(HdoAlarmScheduler.EXTRA_SCHEDULED_TIME, -1L);
        long now = System.currentTimeMillis();
        if (scheduledAt > 0) {
            Log.d("HdoAlarmReceiver", "Received HDO alarm for " + table + ":" + hdoId + ", type=" + type + ", scheduled=" + scheduledAt + ", now=" + now + ", delayMs=" + (now - scheduledAt));
        } else {
            Log.d("HdoAlarmReceiver", "Received HDO alarm for " + table + ":" + hdoId + ", type=" + type + ", scheduled=unknown, now=" + now);
        }

        DataHdoSource source = new DataHdoSource(context);
        source.open();
        HdoModel model = source.loadHdoById(table, hdoId);
        source.close();
        if (model == null) {
            HdoAlarmScheduler.cancelForModel(context, table, hdoId);
            return;
        }

        boolean enabled = type == HdoAlarmScheduler.TYPE_START ? model.getNotifyStart() == 1 : model.getNotifyEnd() == 1;
        if (!enabled) {
            HdoAlarmScheduler.cancelForType(context, table, hdoId, type);
            return;
        }

        String placeName = HdoAlarmScheduler.findSubscriptionPointNameByTable(context, table);
        long subscriptionPointId = HdoAlarmScheduler.findSubscriptionPointIdByTable(context, table);
        String title;
        String content;
        if (type == HdoAlarmScheduler.TYPE_START) {
            title = context.getString(R.string.hdo_notification_start_title);
            content = placeName.isEmpty()
                    ? context.getString(R.string.hdo_notification_start_message)
                    : context.getString(R.string.hdo_notification_start_message_place, placeName);
        } else {
            title = context.getString(R.string.hdo_notification_end_title);
            content = placeName.isEmpty()
                    ? context.getString(R.string.hdo_notification_end_message)
                    : context.getString(R.string.hdo_notification_end_message_place, placeName);
        }

        HdoNotice.setNotice(context, title, context.getString(R.string.hdo_service_name), content, subscriptionPointId);
        HdoAlarmScheduler.scheduleNextForType(context, table, model, type);
    }
}
