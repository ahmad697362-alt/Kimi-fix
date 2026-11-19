package com.kidsguard.child;
import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class SyncWorker extends Worker {
    public SyncWorker(Context c, WorkerParameters p) { super(c, p); }
    @Override public Result doWork() { return Result.success(); }
}