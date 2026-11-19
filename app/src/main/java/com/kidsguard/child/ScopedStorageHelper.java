package com.kidsguard.child;
import android.content.Context;
import android.os.Environment;
import java.io.File;
public class ScopedStorageHelper {
    public static File getAppDir(Context c) { return c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS); }
}