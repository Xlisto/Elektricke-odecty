package cz.xlisto.elektrodroid.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;

import cz.xlisto.elektrodroid.R;

/**
 * Vlastní Toast s možností zobrazení delšího textu.
 * <p>
 * Xlisto 08.12.2023 13:50
 * <p>
 * Řešení podle
 *
 * @see <a href="https://stackoverflow.com/questions/6888664/android-toast-doesnt-fit-text">stackoverflow.com</a>
 */
public class MyToast extends Toast {
    private static final String TAG = "MyToast";

    /**
     * Construct an empty Toast object.  You must call {@link #setView} before you
     * can call {@link #show}.
     *
     * @param context The context to use.  Usually your {@link Application}
     *                or {@link Activity} object.
     */
    public MyToast(Context context) {
        super(context);
    }

    public static Toast makeText(Context context, CharSequence text, @BaseTransientBottomBar.Duration int duration) {
        Toast toast = Toast.makeText(context, text, duration);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.my_toast, null);

        TextView textView = (TextView) layout.findViewById(R.id.text);
        textView.setText(text);

        toast.setView(layout);


        return toast;
    }
}
