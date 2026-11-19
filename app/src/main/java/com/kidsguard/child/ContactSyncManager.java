package com.kidsguard.child;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
public class ContactSyncManager {
    public void sync(Context ctx) {
        Cursor c = null;
        try {
            String[] proj = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            c = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null, null);
            if (c != null) while (c.moveToNext()) { /* process */ }
        } catch (Exception e) { Log.e("Contact", "Sync fail", e); }
        finally { if (c != null) c.close(); }
    }
}
