package cz.xlisto.cenik.shp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;

public abstract class ShP {
    Context context;
    SharedPreferences shp;
    SharedPreferences.Editor editor;

    public void setShp() {
        shp = PreferenceManager.getDefaultSharedPreferences(context);
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
        setShp();
        return shp.getString(key,defaultValue);
    }

    public long get(String key, long defaultValue) {
        setShp();
        return shp.getLong(key,defaultValue);
    }

    public int get(String key, int defaultValue) {
        setShp();
        return shp.getInt(key,defaultValue);
    }

    public boolean get(String key, boolean defaultValue) {
        setShp();
        return shp.getBoolean(key,defaultValue);
    }

}
