package com.kidsguard.child;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, scheduling service start");

            OneTimeWorkRequest startRequest = new OneTimeWorkRequest.Builder(ServiceStarterWorker.class)
                    .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                    "start_kidsguard_service",
                    ExistingWorkPolicy.KEEP,
                    startRequest
            );
        }
    }

    public static class ServiceStarterWorker extends Worker {
        public ServiceStarterWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
            super(ctx, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            Context ctx = getApplicationContext();
            Intent serviceIntent = new Intent(ctx, LocationService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(ctx, serviceIntent);
                } else {
                    ctx.startService(serviceIntent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service from worker", e);
                return Result.retry();
            }
            return Result.success();
        }
    }
}
