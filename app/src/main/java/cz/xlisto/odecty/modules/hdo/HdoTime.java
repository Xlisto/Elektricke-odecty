package cz.xlisto.odecty.modules.hdo;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.odecty.models.HdoModel;
import cz.xlisto.odecty.ownview.ViewHelper;

/**
 * Třída pro zjištění, zda je v daný den a čas aktivní HDO
 * Xlisto 10.06.2023 13:33
 */
public class HdoTime {
    private static final String TAG = "Hdo";


    /**
     * Zjistí, zda je v daný den a čas aktivní HDO
     *
     * @param hdoModels seznam HDO časů
     * @param calendar  kalendář hodin
     * @return boolean true - je HDO, false - není HDO
     */
    public static boolean checkHdo(ArrayList<HdoModel> hdoModels, Calendar calendar) {
        boolean isHdo = false;
        for (int i = 0; i < hdoModels.size(); i++) {
            if (hdoModels.get(i).getDistributionArea().equals(Connections.ResultType.PRE.toString())) {
                Calendar day = Calendar.getInstance();
                String dayString = ViewHelper.convertLongToDate(day.getTimeInMillis());
                String dateHdo = hdoModels.get(i).getDateFrom();
                if (!dayString.equals(dateHdo)) {
                    continue;
                }
            } else {
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == 1 && hdoModels.get(i).getSun() == 0) {
                    continue;
                }
                if (dayOfWeek == 2 && hdoModels.get(i).getMon() == 0) {
                    continue;
                }
                if (dayOfWeek == 3 && hdoModels.get(i).getTue() == 0) {
                    continue;
                }
                if (dayOfWeek == 4 && hdoModels.get(i).getWed() == 0) {
                    continue;
                }
                if (dayOfWeek == 5 && hdoModels.get(i).getThu() == 0) {
                    continue;
                }
                if (dayOfWeek == 6 && hdoModels.get(i).getFri() == 0) {
                    continue;
                }
                if (dayOfWeek == 7 && hdoModels.get(i).getSat() == 0) {
                    continue;
                }
            }

            int hourFrom = Integer.parseInt(hdoModels.get(i).getTimeFrom().split(":")[0]);
            int minuteFrom = Integer.parseInt(hdoModels.get(i).getTimeFrom().split(":")[1]);
            int from = hourFrom * 60 + minuteFrom;
            int hourUntil = Integer.parseInt(hdoModels.get(i).getTimeUntil().split(":")[0]);
            int minuteUntil = Integer.parseInt(hdoModels.get(i).getTimeUntil().split(":")[1]);
            int until = hourUntil * 60 + minuteUntil;

            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int now = hour * 60 + minute;

            if (until < from) {
                if (from <= now || now < until) {
                    isHdo = true;
                    break;
                }
            }

            if (from <= now && now < until) {
                isHdo = true;
                break;
            }
        }
        return isHdo;
    }
}
