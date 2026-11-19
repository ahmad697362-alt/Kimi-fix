package com.kidsguard.child;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
public class CameraCapture {
    public static void takePicture(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) return;
        try {
            File file = File.createTempFile("cap_", ".jpg", dir);
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.startActivity(i);
        } catch (IOException e) {}
    }
}
