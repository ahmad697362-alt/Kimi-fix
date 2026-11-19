package com.kidsguard.child;
import android.os.FileObserver;
import android.util.Log;
public class PhotoWatcher {
    private FileObserver obs;
    public void start(String path) {
        stop();
        try {
            obs = new FileObserver(path, FileObserver.CREATE) {
                @Override public void onEvent(int e, String p) {
                    if (p != null) Log.d("Watch", "New: " + p);
                }
            };
            obs.startWatching();
        } catch (Exception e) { Log.e("Watch", "Native limit reached", e); }
    }
    public void stop() { if (obs != null) { obs.stopWatching(); obs = null; } }
}
