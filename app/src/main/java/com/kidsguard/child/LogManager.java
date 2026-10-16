package com.kidsguard.child;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogManager {
    private final Context context;

    public LogManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void writeLog(String message) {
        File dir = context.getExternalFilesDir("logs");
        if (dir == null) {
            dir = new File(context.getFilesDir(), "logs");
        }
        
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e("LogManager", "Failed to create log directory");
            return;
        }

        File file = new File(dir, "app_log.txt");
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write((System.currentTimeMillis() + " | " + message + "\n").getBytes("UTF-8"));
        } catch (IOException e) {
            Log.e("LogManager", "Failed to write log", e);
        }
    }
}
