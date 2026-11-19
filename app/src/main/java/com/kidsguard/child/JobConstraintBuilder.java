package com.kidsguard.child;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
public class JobConstraintBuilder {
    public static JobInfo build(Context c, Class<?> svc, int id) {
        return new JobInfo.Builder(id, new ComponentName(c, svc)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).build();
    }
}