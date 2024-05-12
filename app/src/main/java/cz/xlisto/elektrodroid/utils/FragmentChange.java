package cz.xlisto.elektrodroid.utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cz.xlisto.elektrodroid.R;


/**
 * Vymění fragmenty
 */
public class FragmentChange {


    /**
     * Vymění fragment, výměnu animuje
     * Do BackStacku se nepřidá
     *
     * @param fa       FragmentActivity
     * @param fragment Fragment
     */
    public static void replace(FragmentActivity fa, Fragment fragment, Transaction transaction) {
        replace(fa, fragment, transaction, false);
    }


    /**
     * Vymění fragment, výměnu animuje
     *
     * @param fa       FragmentActivity
     * @param fragment Fragment
     * @param add      true pro přidání do backstacku
     */
    public static void replace(FragmentActivity fa, Fragment fragment, Transaction transaction, boolean add) {
        FragmentManager fragmentManager = fa.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (transaction == Transaction.MOVE) {
            fragmentTransaction.setCustomAnimations(R.anim.from_right, R.anim.to_left, R.anim.from_left, R.anim.to_right);
        }

        if (transaction == Transaction.ALPHA) {
            fragmentTransaction.setCustomAnimations(R.anim.show, R.anim.hide, R.anim.show, R.anim.hide);
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);//přesun na začátek
        }

        if (fragment != null)
            fragmentTransaction.replace(R.id.fragmentContainerView, fragment, fragment.getClass().getSimpleName());

        if (add) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();

        fa.invalidateOptionsMenu();
    }


    public enum Transaction {
        MOVE,
        ALPHA
    }
}
