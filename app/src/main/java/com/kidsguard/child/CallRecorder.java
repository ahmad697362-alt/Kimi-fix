package com.kidsguard.child;
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
