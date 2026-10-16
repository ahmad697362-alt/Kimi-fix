package com.kidsguard.child;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import java.io.IOException;

public class AudioCapture {
    private static final String TAG = "AudioCapture";
    private final Context context;
    private MediaRecorder recorder;
    private boolean isRecording = false;

    public AudioCapture(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startRecording(String outputPath) {
        if (isRecording) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(context);
            } else {
                recorder = new MediaRecorder();
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(outputPath);
            
            recorder.prepare();
            recorder.start();
            isRecording = true;
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Recording start failed", e);
            releaseRecorder();
        }
    }

    public void stopRecording() {
        if (!isRecording || recorder == null) return;

        try {
            recorder.stop();
        } catch (RuntimeException e) {
            Log.w(TAG, "Stop failed (no valid data or already stopped)", e);
        } finally {
            releaseRecorder();
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
                recorder.release();
            } catch (Exception ignored) {}
            recorder = null;
        }
        isRecording = false;
    }
}
