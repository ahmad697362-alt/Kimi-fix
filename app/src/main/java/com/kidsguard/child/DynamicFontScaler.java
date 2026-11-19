package com.kidsguard.child;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
public class DynamicFontScaler {
    public static float getScale(Configuration c) { return c.fontScale; }
}