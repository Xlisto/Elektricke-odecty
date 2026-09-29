package cz.xlisto.elektrodroid.shp;


import android.content.Context;


/**
 * Třída pro ukládání stavu výzvy k hodnocení aplikace v SharedPreferences.
 */
public class ShPReview extends ShP {

    public static final String LAUNCH_COUNT = "reviewLaunchCount";
    public static final String FIRST_LAUNCH_TIME = "reviewFirstLaunchTime";
    public static final String REVIEW_PROMPTED = "reviewPrompted";
    public static final String LAST_PROMPT_TIME = "reviewLastPromptTime";


    public ShPReview(Context context) {
        this.context = context;
    }

}
