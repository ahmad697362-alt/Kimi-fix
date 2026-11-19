package com.kidsguard.child;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
public class LogManager {
    public void log(Context ctx, String msg) {
        File f = new File(ctx.getExternalFilesDir("logs"), "app.log");
        try (FileOutputStream fos = new FileOutputStream(f, true)) { fos.write((msg + "
").getBytes()); } catch (Exception e) {}
    }
}