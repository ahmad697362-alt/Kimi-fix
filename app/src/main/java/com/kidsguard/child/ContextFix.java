package com.kidsguard.child;
import android.content.Context;
public class ContextFix {
    public static Context app;
    public static void init(Context c) { app = c.getApplicationContext(); }
    public static Context get() { return app; }
}