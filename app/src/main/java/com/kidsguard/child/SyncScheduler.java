package com.kidsguard.child;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncScheduler {
    private final Context context;

    public SyncScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    public void scheduleDataSync() {
        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "data_sync", 
                ExistingWorkPolicy.KEEP, 
                syncWork
        );
    }

    public static class SyncWorker extends Worker {
        public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            return Result.success();
        }
    }
}
