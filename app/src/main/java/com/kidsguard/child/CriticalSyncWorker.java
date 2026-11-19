package com.kidsguard.child;
import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class CriticalSyncWorker extends Worker {
    public CriticalSyncWorker(Context c, WorkerParameters p) { super(c, p); }
    @Override public Result doWork() { return Result.success(); }
}