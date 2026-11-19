package com.kidsguard.child;
import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
public class CriticalSyncWorker extends Worker {
    public CriticalSyncWorker(Context context, WorkerParameters params) {
        super(context, params);
    }
    @Override
    public Result doWork() {
        BatteryManager bm = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        if (bm != null && !bm.isCharging() && bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 10) {
            Log.w("CritSync", "Low battery — retrying");
            return Result.retry();
        }
        try {
            Log.d("CritSync", "Critical sync executing...");
            return Result.success();
        } catch (Exception e) {
            Log.e("CritSync", "Fail", e);
            return Result.retry();
        }
    }
}
