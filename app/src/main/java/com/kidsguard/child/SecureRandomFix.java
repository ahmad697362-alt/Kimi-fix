package com.kidsguard.child;
import java.security.SecureRandom;
public class SecureRandomFix {
    private static final SecureRandom R = new SecureRandom();
    public static int next(int bound) { return R.nextInt(bound); }
}