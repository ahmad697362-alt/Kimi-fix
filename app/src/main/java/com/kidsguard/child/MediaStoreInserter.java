package com.kidsguard.child;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
public class MediaStoreInserter {
    public static Uri insert(Context c, ContentValues v) { return c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v); }
}