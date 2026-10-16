package com.kidsguard.child;

import android.content.Context;
import android.content.SharedPreferences;

public class StateManager {
    private final Context context;

    public StateManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void saveCriticalState(boolean isActive) {
        SharedPreferences prefs = context.getSharedPreferences("state", 0);
        boolean success = prefs.edit().putBoolean("is_active", isActive).commit();
        if (!success) {
            prefs.edit().putBoolean("is_active", isActive).apply();
        }
    }

    public boolean isCriticalStateActive() {
        SharedPreferences prefs = context.getSharedPreferences("state", 0);
        return prefs.getBoolean("is_active", false);
    }
}
