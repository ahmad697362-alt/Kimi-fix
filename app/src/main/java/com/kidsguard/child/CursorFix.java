package com.kidsguard.child;
import android.database.Cursor;
public class CursorFix {
    public static void close(Cursor c) { if (c != null && !c.isClosed()) c.close(); }
}