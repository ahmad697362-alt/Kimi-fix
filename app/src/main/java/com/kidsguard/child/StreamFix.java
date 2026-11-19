package com.kidsguard.child;
import java.io.Closeable;
import java.io.IOException;
public class StreamFix {
    public static void close(Closeable c) { if (c != null) try { c.close(); } catch (IOException ignored) {} }
}