package cz.xlisto.odecty.utils;

import java.util.ArrayList;
import java.util.Calendar;

import cz.xlisto.odecty.models.HdoModel;


/**
 * Třída sestaví týdenní seznam HDO modelů
 * Xlisto 06.02.2024 12:09
 */
public class BuilderHDOStack {
    private static final String TAG = "BuilderHDOStack";


    /**
     * Sestaví týdenní seznam HDO modelů
     *
     * @param models    seznam modelů
     * @param timeShift posunutí času
     * @return seznam modelů s nastaveným datem
     */
    public static ArrayList<HdoModel> build(ArrayList<HdoModel> models, long timeShift) {
        ArrayList<HdoModel> modelsWithDate = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(calendar.getTimeInMillis() + timeShift);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        int today = calendar.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday, etc.

        for (int i = 0; i < 7; i++) {
            modelsWithDate.addAll(createHDOStack(today, calendar, models));
            today++;
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            if (today > 7) {
                today = 1;
            }
        }
        return modelsWithDate;
    }


    /**
     * Vytvoří seznam modelů pro daný den
     */
    private static ArrayList<HdoModel> createHDOStack(int day, Calendar calendar, ArrayList<HdoModel> models) {
        ArrayList<HdoModel> hdoModels = new ArrayList<>();

        for (HdoModel model : models) {
            // Procházení seznamu a kreslení výsečí
            switch (day) {
                case Calendar.MONDAY:
                    if (model.getMon() == 0)
                        continue;
                    break;
                case Calendar.TUESDAY:
                    if (model.getTue() == 0)
                        continue;
                    break;
                case Calendar.WEDNESDAY:
                    if (model.getWed() == 0)
                        continue;
                    break;
                case Calendar.THURSDAY:
                    if (model.getThu() == 0)
                        continue;
                    break;
                case Calendar.FRIDAY:
                    if (model.getFri() == 0)
                        continue;
                    break;
                case Calendar.SATURDAY:
                    if (model.getSat() == 0)
                        continue;
                    break;
                case Calendar.SUNDAY:
                    if (model.getSun() == 0)
                        continue;
                    break;
            }

            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            //klonuji model a nastavuji mu datum. Z důvodu, i když je jeden záznam tak s platným datem musí být dva záznamy
            HdoModel newHdoModel = model.clone();
            newHdoModel.setCalendar(calendar, 0);
            hdoModels.add(newHdoModel);
        }

        return hdoModels;
    }
}
