package com.kidsguard.child;
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
