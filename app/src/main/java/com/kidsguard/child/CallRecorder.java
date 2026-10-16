package com.kidsguard.child;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import java.io.File;

public class CallRecorder {
    private static final String TAG = "CallRecorder";
    private final Context context;
    private MediaRecorder recorder;

    public CallRecorder(Context context) {
        this.context = context.getApplicationContext();
    }

    public void recordCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(context);
            } else {
                recorder = new MediaRecorder();
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            } else {
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            }
            
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(getOutputPath());
            
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Call recording failed due to OS restrictions", e);
            releaseRecorder();
        }
    }

    public void stopRecording() {
        releaseRecorder();
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) {}
            try { recorder.release(); } catch (Exception ignored) {}
            recorder = null;
        }
    }
    
    private String getOutputPath() {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) dir = context.getFilesDir();
        return new File(dir, "call_rec_" + System.currentTimeMillis() + ".3gp").getAbsolutePath();
    }
}
