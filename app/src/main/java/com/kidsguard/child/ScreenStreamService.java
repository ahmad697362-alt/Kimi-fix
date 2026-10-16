package com.kidsguard.child;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class ScreenStreamService extends Service {
    private static final String TAG = "ScreenStreamService";
    private static final String EXTRA_RESULT_CODE = "extra_result_code";
    private static final String EXTRA_RESULT_DATA = "extra_result_data";

    private MediaProjection mediaProjection;

    public static void start(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, ScreenStreamService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_RESULT_DATA, data);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra(EXTRA_RESULT_DATA)) {
            Log.e(TAG, "Missing MediaProjection token");
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
        Intent data = intent.getParcelableExtra(EXTRA_RESULT_DATA);

        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mgr != null) {
            mediaProjection = mgr.getMediaProjection(resultCode, data);
        }

        if (mediaProjection == null) {
            Log.e(TAG, "Failed to acquire MediaProjection");
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
