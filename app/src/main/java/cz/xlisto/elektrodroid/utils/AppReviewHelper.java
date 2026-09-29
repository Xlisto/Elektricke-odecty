package cz.xlisto.elektrodroid.utils;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;

import cz.xlisto.elektrodroid.BuildConfig;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.shp.ShPReview;


/**
 * Pomocná třída pro správu žádostí o hodnocení aplikace přes Google Play In-App Review API.
 */
public class AppReviewHelper {

    private static final String TAG = "AppReviewHelper";
    private static final int MIN_LAUNCH_COUNT = 5;
    private static final long MIN_DAYS_SINCE_FIRST_LAUNCH = 3 * 24 * 60 * 60 * 1000L; // 3 dny v ms
    private static final long MIN_DAYS_BETWEEN_PROMPTS = 30 * 24 * 60 * 60 * 1000L; // 30 dní v ms


    /**
     * Zkontroluje podmínky a případně automaticky zobrazí dialog pro hodnocení.
     * Zvyšuje počítadlo spustění aplikace.
     *
     * @param activity Aktuální aktivita
     */
    public static void checkAndPromptReview(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        ShPReview shPReview = new ShPReview(activity.getApplicationContext());
        long now = System.currentTimeMillis();

        // Počítadlo spuštění
        int launchCount = shPReview.get(ShPReview.LAUNCH_COUNT, 0) + 1;
        shPReview.set(ShPReview.LAUNCH_COUNT, launchCount);

        // První spuštění
        long firstLaunchTime = shPReview.get(ShPReview.FIRST_LAUNCH_TIME, 0L);
        if (firstLaunchTime == 0L) {
            firstLaunchTime = now;
            shPReview.set(ShPReview.FIRST_LAUNCH_TIME, firstLaunchTime);
        }

        boolean reviewPrompted = shPReview.get(ShPReview.REVIEW_PROMPTED, false);
        long lastPromptTime = shPReview.get(ShPReview.LAST_PROMPT_TIME, 0L);

        boolean daysSinceFirstLaunchMet = (now - firstLaunchTime) >= MIN_DAYS_SINCE_FIRST_LAUNCH;
        boolean cooldownMet = (now - lastPromptTime) >= MIN_DAYS_BETWEEN_PROMPTS;

        if (launchCount >= MIN_LAUNCH_COUNT && daysSinceFirstLaunchMet && (!reviewPrompted || cooldownMet)) {
            Log.d(TAG, "Podmínky pro zobrazení In-App Review splněny.");
            requestReview(activity, false);
        }
    }


    /**
     * Spustí In-App Review flow.
     * Pokud se nepodaří In-App Review načíst a požadavek byl vyvolán ručně uživatelem,
     * otevře stránku aplikace v Google Play.
     *
     * @param activity             Aktivita, ze které je požadavek volán
     * @param openFallbackIfFailed Zda v případě neúspěchu otevřít Google Play
     */
    public static void requestReview(Activity activity, boolean openFallbackIfFailed) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        Context context = activity.getApplicationContext();
        ShPReview shPReview = new ShPReview(context);

        ReviewManager manager;
        if (BuildConfig.DEBUG) {
            manager = new FakeReviewManager(activity);
        } else {
            manager = ReviewManagerFactory.create(activity);
        }

        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {
                    // Flow dokončeno (uživatel mohl ohodnotit nebo zavřít)
                    shPReview.set(ShPReview.REVIEW_PROMPTED, true);
                    shPReview.set(ShPReview.LAST_PROMPT_TIME, System.currentTimeMillis());
                    if (BuildConfig.DEBUG && openFallbackIfFailed) {
                        openPlayStore(activity);
                    }
                });
            } else {
                Log.w(TAG, "Requesting review flow failed", task.getException());
                if (openFallbackIfFailed) {
                    openPlayStore(activity);
                }
            }
        });
    }


    /**
     * Otevře stránku aplikace v aplikaci Google Play nebo ve webovém prohlížeči.
     *
     * @param context Kontext aplikace
     */
    public static void openPlayStore(Context context) {
        if (context == null) {
            return;
        }
        String packageName = context.getPackageName();
        if (packageName.endsWith(".debug")) {
            packageName = packageName.substring(0, packageName.length() - ".debug".length());
        }
        if (packageName.endsWith(".releasetest")) {
            packageName = packageName.substring(0, packageName.length() - ".releasetest".length());
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ex) {
                MyToast.makeText(context, context.getString(R.string.error_opening_play_store), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
