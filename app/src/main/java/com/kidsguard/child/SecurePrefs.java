package com.kidsguard.child;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePrefs {
    private static final String TAG = "SecurePrefs";
    private static final String SECURE_PREFS_NAME = "kidsguard_secure_prefs";
    private static final String KEY_DEVICE_ID = "device_id";

    private static SharedPreferences getEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences. Falling back to standard.", e);
            return context.getSharedPreferences("kidsguard_prefs", Context.MODE_PRIVATE);
        }
    }

    public static void saveDeviceId(Context context, String deviceId) {
        getEncryptedPrefs(context).edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }

    public static String getDeviceId(Context context) {
        return getEncryptedPrefs(context).getString(KEY_DEVICE_ID, "");
    }
    
    public static void clearAll(Context context) {
        getEncryptedPrefs(context).edit().clear().apply();
    }
}
