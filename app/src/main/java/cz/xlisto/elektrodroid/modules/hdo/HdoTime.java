package cz.xlisto.elektrodroid.modules.hdo;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.ownview.ViewHelper;

/**
 * Třída pro zjištění, zda je v daný den a čas aktivní HDO
 * Xlisto 10.06.2023 13:33
 */
public class HdoTime {


    /**
     * Zjistí, zda je v daný den a čas aktivní HDO
     *
     * @param hdoModels seznam HDO časů
     * @param calendar  kalendář hodin
     * @return boolean true - je HDO, false - není HDO
     */
    public static boolean checkHdo(ArrayList<HdoModel> hdoModels, Calendar calendar) {
        boolean isHdo = false;
        boolean isHolidayToday = Connections.isCzechHoliday(calendar);

        for (int i = 0; i < hdoModels.size(); i++) {
            HdoModel model = hdoModels.get(i);
            boolean modelIsHoliday = model.getSv() == 1 || "SVÁTEK".equalsIgnoreCase(model.getDateFrom());

            if (isHolidayToday) {
                if (!modelIsHoliday) {
                    continue;
                }
            } else {
                if (modelIsHoliday) {
                    continue;
                }
                if (model.getDistributionArea().equals(Connections.ResultType.PRE.toString())
                        && model.getMon() == 0 && model.getTue() == 0 && model.getWed() == 0
                        && model.getThu() == 0 && model.getFri() == 0 && model.getSat() == 0 && model.getSun() == 0) {
                    Calendar day = Calendar.getInstance();
                    String dayString = ViewHelper.convertLongToDate(day.getTimeInMillis());
                    String dateHdo = model.getDateFrom();
                    if (!dayString.equals(dateHdo)) {
                        continue;
                    }
                } else {
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == 1 && model.getSun() == 0) {
                        continue;
                    }
                    if (dayOfWeek == 2 && model.getMon() == 0) {
                        continue;
                    }
                    if (dayOfWeek == 3 && model.getTue() == 0) {
                        continue;
                    }
                    if (dayOfWeek == 4 && model.getWed() == 0) {
                        continue;
                    }
                    if (dayOfWeek == 5 && model.getThu() == 0) {
                        continue;
                    }
                    if (dayOfWeek == 6 && model.getFri() == 0) {
                        continue;
                    }
                    if (dayOfWeek == 7 && model.getSat() == 0) {
                        continue;
                    }
                }
            }

            int hourFrom = Integer.parseInt(model.getTimeFrom().split(":")[0]);
            int minuteFrom = Integer.parseInt(model.getTimeFrom().split(":")[1]);
            int from = hourFrom * 60 + minuteFrom;
            int hourUntil = Integer.parseInt(model.getTimeUntil().split(":")[0]);
            int minuteUntil = Integer.parseInt(model.getTimeUntil().split(":")[1]);
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
