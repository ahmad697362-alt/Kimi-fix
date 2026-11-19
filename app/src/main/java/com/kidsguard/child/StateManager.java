package com.kidsguard.child;
import android.content.Context;
import android.content.SharedPreferences;
public class StateManager {
    private final Context ctx;
    public StateManager(Context c) { this.ctx = c.getApplicationContext(); }
    public void saveCritical(String key, String value) {
        ctx.getSharedPreferences("state", Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }
    public String get(String key, String def) {
        return ctx.getSharedPreferences("state", Context.MODE_PRIVATE).getString(key, def);
    }
}
