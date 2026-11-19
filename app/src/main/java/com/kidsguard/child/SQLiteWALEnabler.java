package com.kidsguard.child;
import android.database.sqlite.SQLiteDatabase;
public class SQLiteWALEnabler {
    public static void enable(SQLiteDatabase db) { db.enableWriteAheadLogging(); }
}