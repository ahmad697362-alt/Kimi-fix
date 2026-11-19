package com.kidsguard.child;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class LocalDatabase extends SQLiteOpenHelper {
    private static volatile LocalDatabase instance;
    public static synchronized LocalDatabase getInstance(Context ctx) {
        if (instance == null) instance = new LocalDatabase(ctx.getApplicationContext());
        return instance;
    }
    private LocalDatabase(Context c) { super(c, "kidsguard.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY, data TEXT)"); }
    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) { db.execSQL("DROP TABLE IF EXISTS logs"); onCreate(db); }
}