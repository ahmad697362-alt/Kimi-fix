package com.kidsguard.child;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
public class SecurePrefs {
    public static SharedPreferences get(Context ctx) {
        try {
            MasterKey mk = new MasterKey.Builder(ctx).build();
            return EncryptedSharedPreferences.create(ctx, "sec_prefs", mk,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) { return ctx.getSharedPreferences("sec_prefs", 0); }
    }
}