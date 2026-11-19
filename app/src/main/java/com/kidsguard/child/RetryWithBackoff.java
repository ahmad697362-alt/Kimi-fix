package com.kidsguard.child;
public class RetryWithBackoff {
    public static long next(int attempt) { return Math.min(1000 * (1 << attempt), 30000); }
}