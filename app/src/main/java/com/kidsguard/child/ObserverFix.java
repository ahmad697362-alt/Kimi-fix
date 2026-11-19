package com.kidsguard.child;
import android.content.ContentResolver;
import android.database.ContentObserver;
public class ObserverFix {
    public static void unreg(ContentResolver cr, ContentObserver o) { if (o != null) cr.unregisterContentObserver(o); }
}