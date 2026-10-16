package com.kidsguard.child;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactSyncManager {
    private static final String TAG = "ContactSync";
    private final Context context;

    public ContactSyncManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void syncContacts() {
        Cursor phones = null;
        try {
            phones = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            if (phones != null) {
                int nameIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                JSONArray arr = new JSONArray();

                while (phones.moveToNext()) {
                    String name = nameIndex >= 0 ? phones.getString(nameIndex) : "";
                    String number = numberIndex >= 0 ? phones.getString(numberIndex) : "";
                    
                    if (name != null && number != null) {
                        JSONObject obj = new JSONObject();
                        obj.put("name", name);
                        obj.put("phone", number);
                        arr.put(obj);
                    }
                }
                String devId = LocationService.getSafeDeviceId(context);
                FirebaseHelper.put("devices/" + devId + "/contacts.json", arr.toString());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "READ_CONTACTS permission missing or revoked", e);
        } catch (Exception e) {
            Log.e(TAG, "Contact sync failed", e);
        } finally {
            if (phones != null) {
                try {
                    phones.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
