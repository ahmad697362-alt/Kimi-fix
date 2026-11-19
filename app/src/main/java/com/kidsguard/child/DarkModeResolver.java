package com.kidsguard.child;
import android.content.res.Configuration;
public class DarkModeResolver {
    public static boolean isDark(Configuration c) { return (c.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES; }
}