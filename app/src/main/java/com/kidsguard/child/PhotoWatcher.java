package com.kidsguard.child;

import android.os.FileObserver;
import android.util.Log;

public class PhotoWatcher {
    private static final String TAG = "PhotoWatcher";
    private FileObserver observer;

    public void startWatching(String path) {
        stopWatching();

        observer = new FileObserver(path) {
            @Override
            public void onEvent(int event, String updatedPath) {
                if ((event & FileObserver.CREATE) != 0) {
                    processNewPhoto(updatedPath);
                }
            }
        };
        observer.startWatching();
    }

    public void stopWatching() {
        if (observer != null) {
            observer.stopWatching();
            observer = null;
            Log.d(TAG, "FileObserver released");
        }
    }

    private void processNewPhoto(String path) {
        Log.d(TAG, "New photo detected at: " + path);
    }
}
