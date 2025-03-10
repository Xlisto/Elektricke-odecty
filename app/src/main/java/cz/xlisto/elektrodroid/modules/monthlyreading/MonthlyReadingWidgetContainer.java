package cz.xlisto.elektrodroid.modules.monthlyreading;


import androidx.annotation.NonNull;


/**
 * Třída pro přenos dat pro widgety pro přidání/editaci měsíčního odečtu.
 */
public class MonthlyReadingWidgetContainer {

    public String date, vt, nt, payment, description, otherService;


    @NonNull
    @Override
    public String toString() {
        return "WidgetContainer{" +
                "date='" + date + '\'' +
                ", vt='" + vt + '\'' +
                ", nt='" + nt + '\'' +
                ", payment='" + payment + '\'' +
                ", description='" + description + '\'' +
                ", otherService='" + otherService + '\'' +
                '}';
    }

}
