package com.kidsguard.child;
import android.content.Context;
import android.media.MediaScannerConnection;
import java.io.File;
public class MediaSaver {
    public void save(Context ctx, File f) {
        MediaScannerConnection.scanFile(ctx, new String[]{f.getAbsolutePath()}, null, null);
    }
}