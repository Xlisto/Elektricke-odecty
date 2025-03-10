package cz.xlisto.elektrodroid.shp;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;


/**
 * Abstraktní třída `ShP`, která poskytuje metody pro práci s `SharedPreferences`.
 */
public abstract class ShP {

    Context context;
    SharedPreferences shp;
    SharedPreferences.Editor editor;


    public void getShp() {
        shp = PreferenceManager.getDefaultSharedPreferences(context);
    }


    public void setShp() {
        getShp();
        editor = shp.edit();
    }


    public void set(String key, long l) {
        setShp();
        editor.putLong(key, l);
        editor.commit();
    }


    public void set(String key, int i) {
        setShp();
        editor.putInt(key, i);
        editor.commit();
    }


    public void set(String key, String s) {
        setShp();
        editor.putString(key, s);
        editor.commit();
    }


    public void set(String key, boolean b) {
        setShp();
        editor.putBoolean(key, b);
        editor.commit();
    }


    public String get(String key, String defaultValue) {
        getShp();
        try {
            return shp.getString(key, defaultValue);
        } catch (Exception e) {
            return "";
        }
    }


    public long get(String key, long defaultValue) {
        getShp();
        try {
            return shp.getLong(key, defaultValue);
        } catch (Exception e) {
            return 0;
        }
    }


    public int get(String key, int defaultValue) {
        getShp();
        try {
            return shp.getInt(key, defaultValue);
        } catch (Exception e) {
            return 0;
        }
    }


    public boolean get(String key, boolean defaultValue) {
        setShp();
        try {
            return shp.getBoolean(key, defaultValue);
        } catch (Exception e) {
            return false;
        }
    }

}
