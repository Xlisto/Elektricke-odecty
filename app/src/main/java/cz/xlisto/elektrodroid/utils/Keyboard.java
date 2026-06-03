package cz.xlisto.elektrodroid.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.FragmentActivity;


/**
 * Xlisto 13.03.2023 22:11
 */
public class Keyboard {

    /**
     * Skryje softwarovou klávesnici.
     * <p>
     * Získá aktuálně fokusované View a schová klávesnici spojenou s ním.
     * Pokud není žádné View fokusováno, metoda se beze změny ukončí.
     *
     * @param fragmentActivity aktivita, z níž se má klávesnice skrýt
     * @see <a href="https://www.geeksforgeeks.org/how-to-programmatically-hide-android-soft-keyboard/">Zdroj: geeksforgeeks.org</a>
     */
    public static void hide(FragmentActivity fragmentActivity) {
        View view = fragmentActivity.getCurrentFocus();

        if (view != null) {
            InputMethodManager manager = (InputMethodManager) fragmentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
