package com.kidsguard.child;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
public class BackgroundStarter {
    public void start(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            WorkManager.getInstance(ctx).enqueueUniqueWork("start", ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(LocationService.class).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build());
        } else {
            ctx.startForegroundService(new Intent(ctx, LocationService.class));
        }
    }
}