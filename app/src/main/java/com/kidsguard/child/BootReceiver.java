package com.kidsguard.child;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final PendingResult pr = goAsync();
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                ComponentName cn = new ComponentName(context, LocationService.class);
                JobInfo job = new JobInfo.Builder(1001, cn).setOverrideDeadline(0).build();
                JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (js != null) js.schedule(job);
            } else {
                context.startService(new Intent(context, LocationService.class));
            }
        } catch (Exception e) { Log.w("Boot", "Deferred: " + e.getClass().getSimpleName()); }
        finally { pr.finish(); }
    }
}
