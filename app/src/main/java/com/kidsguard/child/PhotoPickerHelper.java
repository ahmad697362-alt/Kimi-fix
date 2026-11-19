package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
public class PhotoPickerHelper {
    public static void openPicker(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= 33) {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            intent.setType("image/*");
            activity.startActivityForResult(intent, requestCode);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
