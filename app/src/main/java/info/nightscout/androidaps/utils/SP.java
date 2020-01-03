package info.nightscout.androidaps.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.database.AppRepository;
import info.nightscout.androidaps.database.transactions.preferences.CopyMissingPreferencesToDatabaseTransaction;
import info.nightscout.androidaps.database.transactions.preferences.SafePreferenceChangeTransaction;

/**
 * Created by mike on 17.02.2017.
 */

public class SP {
    public static SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());

    private static SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        AppRepository.INSTANCE.runTransaction(new SafePreferenceChangeTransaction(key, sharedPreferences.getAll().get(key))).subscribe();
    };

    static {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void copyMissingValuesToDatabase() {
        AppRepository.INSTANCE.runTransaction(new CopyMissingPreferencesToDatabaseTransaction(sharedPreferences.getAll())).subscribe();
    }

    static public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    static public boolean contains(int resourceId) {
        return sharedPreferences.contains(MainApp.gs(resourceId));
    }

    static public String getString(int resourceID, String defaultValue) {
        return sharedPreferences.getString(MainApp.gs(resourceID), defaultValue);
    }

    static public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    static public boolean getBoolean(int resourceID, Boolean defaultValue) {
        try {
            return sharedPreferences.getBoolean(MainApp.gs(resourceID), defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static public boolean getBoolean(String key, Boolean defaultValue) {
        try {
            return sharedPreferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static public Double getDouble(int resourceID, Double defaultValue) {
        return SafeParse.stringToDouble(sharedPreferences.getString(MainApp.gs(resourceID), defaultValue.toString()));
    }

    static public Double getDouble(String key, Double defaultValue) {
        return SafeParse.stringToDouble(sharedPreferences.getString(key, defaultValue.toString()));
    }

    static public int getInt(int resourceID, Integer defaultValue) {
        try {
            return sharedPreferences.getInt(MainApp.gs(resourceID), defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToInt(sharedPreferences.getString(MainApp.gs(resourceID), defaultValue.toString()));
        }
    }

    static public int getInt(String key, Integer defaultValue) {
        try {
            return sharedPreferences.getInt(key, defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToInt(sharedPreferences.getString(key, defaultValue.toString()));
        }
    }

    static public long getLong(int resourceID, Long defaultValue) {
        try {
            return sharedPreferences.getLong(MainApp.gs(resourceID), defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToLong(sharedPreferences.getString(MainApp.gs(resourceID), defaultValue.toString()));
        }
    }

    static public long getLong(String key, Long defaultValue) {
        try {
            return sharedPreferences.getLong(key, defaultValue);
        } catch (Exception e) {
            return SafeParse.stringToLong(sharedPreferences.getString(key, defaultValue.toString()));
        }
    }

    static public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    static public void putBoolean(int resourceID, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putDouble(String key, double value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, Double.toString(value));
        editor.apply();
    }

    static public void putLong(String key, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    static public void putLong(int resourceID, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putInt(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    static public void putInt(int resourceID, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void incInt(int resourceID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int value = SP.getInt(resourceID, 0) + 1;
        editor.putInt(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putString(int resourceID, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MainApp.gs(resourceID), value);
        editor.apply();
    }

    static public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static public void remove(int resourceID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MainApp.gs(resourceID));
        editor.apply();
    }

    static public void remove(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }
}
