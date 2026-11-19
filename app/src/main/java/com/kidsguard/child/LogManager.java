package com.kidsguard.child;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.util.List;
public class LogManager {
    public void bulkInsert(Context ctx, List<ContentValues> valuesList) {
        SQLiteDatabase db = LocalDatabase.getInstance(ctx).getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues values : valuesList) {
                db.insert("logs", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
