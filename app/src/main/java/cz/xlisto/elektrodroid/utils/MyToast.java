package cz.xlisto.elektrodroid.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import cz.xlisto.elektrodroid.R;

/**
 * Pomocná třída pro zobrazení oznámení uživateli.
 * Využívá Material 3 {@link Snackbar} při dostupnosti zobrazení (View/Activity)
 * nebo standardní systémový {@link Toast}.
 */
public class MyToast extends Toast {

    public MyToast(Context context) {
        super(context);
    }


    /**
     * Zobrazí oznámení pomocí Material 3 Snackbar na základě předaného View.
     */
    public static void show(View view, CharSequence text, int duration) {
        if (view != null) {
            Snackbar.make(view, text, duration).show();
        }
    }


    /**
     * Zobrazí oznámení pomocí Material 3 Snackbar na základě předaného Fragmentu.
     */
    public static void show(Fragment fragment, CharSequence text, int duration) {
        if (fragment != null && fragment.getView() != null) {
            Snackbar.make(fragment.getView(), text, duration).show();
        }
    }


    /**
     * Vytvoří a vrátí objekt Toast. Pokud je předán Activity context a lze získá absolutní zobrazení,
     * zobrazí se Material 3 {@link Snackbar}. V opačném případě se použije standardní Toast.
     */
    @SuppressLint("InflateParams")
    @SuppressWarnings("deprecation")
    public static Toast makeText(Context context, CharSequence text, @BaseTransientBottomBar.Duration int duration) {
        if (context instanceof Activity activity) {
            View contentView = activity.findViewById(android.R.id.content);
            if (contentView != null) {
                Snackbar.make(contentView, text, duration).show();
                return Toast.makeText(context, text, duration);
            }
        }

        Toast toast = Toast.makeText(context, text, duration);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.my_toast, null);
                TextView textView = layout.findViewById(R.id.text);
                if (textView != null) {
                    textView.setText(text);
                }
                toast.setView(layout);
            } catch (Exception ignored) {
                // Pokud z jakéhokoli důvodu inflate selže, použije se výchozí systémový Toast.
            }
        }

        return toast;
    }
}
