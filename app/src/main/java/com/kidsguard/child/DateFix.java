package com.kidsguard.child;
import java.text.SimpleDateFormat;
import java.util.Locale;
public class DateFix {
    private static final ThreadLocal<SimpleDateFormat> FMT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US));
    public static String now() { return FMT.get().format(new java.util.Date()); }
}