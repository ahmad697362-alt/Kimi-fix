package com.kidsguard.child;
import android.content.Context;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class SyncWorker extends Worker {
    public SyncWorker(Context context, WorkerParameters params) {
        super(context, params);
    }
    @Override
    public Result doWork() {
        return doSync() ? Result.success() : Result.retry();
    }
    private boolean doSync() {
        try {
            Log.d("SyncWorker", "Syncing data...");
            return true;
        } catch (Exception e) {
            Log.e("SyncWorker", "Sync failed", e);
            return false;
        }
    }
}
