package com.kidsguard.child;
import androidx.work.ExistingPeriodicWorkPolicy;
public class WorkManagerUniquePolicy {
    public static final ExistingPeriodicWorkPolicy POLICY = ExistingPeriodicWorkPolicy.UPDATE;
}