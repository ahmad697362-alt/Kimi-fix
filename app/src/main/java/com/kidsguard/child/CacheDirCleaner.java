package com.kidsguard.child;
import android.content.Context;
import java.io.File;
public class CacheDirCleaner {
    public static void clean(Context c) { File d = c.getCacheDir(); if (d != null) for (File f : d.listFiles()) f.delete(); }
}