package com.kidsguard.child;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
public class CameraCapture {
    public static Uri takePicture(Context context) {
        try {
            File f = File.createTempFile("cap_", ".jpg", context.getExternalFilesDir(null));
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return uri;
        } catch (IOException e) { return null; }
    }
    public static void revokePermission(Context context, Uri uri) {
        if (uri != null) context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
}
