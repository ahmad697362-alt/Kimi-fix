package com.kidsguard.child;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

public class CameraCapture {

    public static void takePicture(Context context) {
        File photoDir = context.getExternalFilesDir(null);
        if (photoDir == null) return;

        File photoFile;
        try {
            photoFile = File.createTempFile("capture_", ".jpg", photoDir);
        } catch (IOException e) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                photoFile
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(intent);
        } catch (Exception ignored) {}
    }
}
