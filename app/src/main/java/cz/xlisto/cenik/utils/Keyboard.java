package cz.xlisto.cenik.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.FragmentActivity;


/**
 * Xlisto 13.03.2023 22:11
 */
public class Keyboard {
    private static final String TAG = "Keyboard";

    /**
     * Skryje klávesnici
     * @param fragmentActivity
     * https://www.geeksforgeeks.org/how-to-programmatically-hide-android-soft-keyboard/
     */
    public static void hide(FragmentActivity fragmentActivity) {
        View view = fragmentActivity.getCurrentFocus();

        if (view != null) {
            InputMethodManager manager = (InputMethodManager) fragmentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
