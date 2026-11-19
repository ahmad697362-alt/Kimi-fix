package com.kidsguard.child;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
public class ContactSyncManager {
    public void sync(Context ctx) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c != null) while (c.moveToNext()) { /* process */ }
        } finally { if (c != null) c.close(); }
    }
}