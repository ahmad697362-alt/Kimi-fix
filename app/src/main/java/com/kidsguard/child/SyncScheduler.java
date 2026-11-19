package com.kidsguard.child;
import android.content.Context;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
public class SyncScheduler {
    private final Context ctx;
    public SyncScheduler(Context c) { this.ctx = c.getApplicationContext(); }
    public void schedule() {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(ctx).enqueueUniqueWork("data_sync", ExistingWorkPolicy.KEEP, req);
    }
}
