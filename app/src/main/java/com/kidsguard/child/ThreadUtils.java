package com.kidsguard.child;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ThreadUtils {
    public static final ExecutorService IO = Executors.newFixedThreadPool(4);
    public static final ExecutorService SINGLE = Executors.newSingleThreadExecutor();
}