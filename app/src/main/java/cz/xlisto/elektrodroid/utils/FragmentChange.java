package cz.xlisto.elektrodroid.utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;


/**
 * Pomocná třída pro výměnu fragmentů s animacemi.
 * <p>
 * Poskytuje metody pro nahradit fragmenty v kontejneru s animačními efekty,
 * včetně volby, zda se má fragment přidat do back stacku.
 */
public class FragmentChange {


    /**
     * Vymění fragment v hlavním kontejneru a výměnu animuje bez přidání do back stacku.
     *
     * @param fa          Aktivita obsahující kontejner fragmentů
     * @param fragment    Nový fragment, který se má zobrazit
     * @param transaction Typ animačního přechodu ({@link Transaction#MOVE} nebo {@link Transaction#ALPHA})
     */
    public static void replace(FragmentActivity fa, Fragment fragment, Transaction transaction) {
        replace(fa, fragment, transaction, false);
    }


    /**
     * Vymění fragment v hlavním kontejneru, nastaví animační přechod a volitelně přidá transakci do back stacku.
     * <p>
     * Při přechodu typu {@link Transaction#ALPHA} vymaže celý back stack a v {@link MainActivity}
     * zruší kompaktní režim spodní plovoucí navigace.
     *
     * @param fa          Aktivita obsahující kontejner fragmentů
     * @param fragment    Nový fragment, který se má zobrazit
     * @param transaction Typ animačního přechodu ({@link Transaction#MOVE} nebo {@link Transaction#ALPHA})
     * @param add         {@code true}, pokud se má transakce přidat do back stacku
     */
    public static void replace(FragmentActivity fa, Fragment fragment, Transaction transaction, boolean add) {
        FragmentManager fragmentManager = fa.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        switch (transaction) {
            case MOVE ->
                    fragmentTransaction.setCustomAnimations(R.anim.from_right, R.anim.to_left, R.anim.from_left, R.anim.to_right);
            case ALPHA -> {
                fragmentTransaction.setCustomAnimations(R.anim.show, R.anim.hide, R.anim.show, R.anim.hide);
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);//přesun na začátek
                if (fa instanceof MainActivity mainActivity) {
                    mainActivity.setBottomNavCompact(false);
                }
            }
        }

        if (fragment != null)
            fragmentTransaction.replace(R.id.fragmentContainerView, fragment, fragment.getClass().getSimpleName());

        if (add) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();

        fa.invalidateOptionsMenu();
    }


    /**
     * Typ animace pro výměnu fragmentů.
     */
    public enum Transaction {
        /**
         * Animace pohybu fragmentu (zleva doprava a opačně).
         */
        MOVE,
        /**
         * Animace prolínání (alfa kanál).
         */
        ALPHA
    }
}
