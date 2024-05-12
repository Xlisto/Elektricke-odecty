package cz.xlisto.elektrodroid.shp;


import android.content.Context;


public class ShPSubscriptionPoint extends ShP {

    public static final String ID_SUBSCRIPTION_POINT_LONG = "IDMistaPoziceLong";
    public static final String ID_SUBSCRIPTION_POINT_INT = "IDMistaPozice";


    public ShPSubscriptionPoint(Context context) {
        this.context = context;
        getShp();
        setShp();
        refactoring();
    }


    /**
     * Refaktoring - přesunutí hodnoty z int do long
     * Používá se při přechodu na novou verzi aplikace
     */
    private void refactoring() {
        if (shp.contains(ID_SUBSCRIPTION_POINT_INT)) {
            int id = shp.getInt(ID_SUBSCRIPTION_POINT_INT, -1);
            editor.putLong(ID_SUBSCRIPTION_POINT_LONG, id);
            editor.remove(ID_SUBSCRIPTION_POINT_INT);
            editor.commit();
        }
    }

}
