package com.kidsguard.child;
import android.content.Context;
import android.content.SharedPreferences;
public class PrefsFix {
    public static void put(Context c, String k, String v) {
        c.getSharedPreferences("fix", Context.MODE_PRIVATE).edit().putString(k, v).apply();
    }
    public static String get(Context c, String k) {
        return c.getSharedPreferences("fix", Context.MODE_PRIVATE).getString(k, "");
    }
}