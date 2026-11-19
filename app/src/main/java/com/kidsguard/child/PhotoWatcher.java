package com.kidsguard.child;
import android.os.FileObserver;
import android.util.Log;
public class PhotoWatcher {
    private FileObserver obs;
    public void start(String path) {
        stop();
        obs = new FileObserver(path) {
            @Override public void onEvent(int e, String p) {
                if ((e & FileObserver.CREATE) != 0) Log.d("Watch", "New: " + p);
            }
        };
        obs.startWatching();
    }
    public void stop() {
        if (obs != null) { obs.stopWatching(); obs = null; }
    }
}
