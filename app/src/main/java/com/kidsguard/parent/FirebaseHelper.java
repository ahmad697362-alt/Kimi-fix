package com.kidsguard.parent;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String BASE_URL = "https://easy-earn-971e4-default-rtdb.firebaseio.com/";
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(Exception e);
    }

    public static void getAsync(final String path, final ResponseCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + path + ".json");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    if (callback != null) callback.onSuccess(sb.toString());
                } else {
                    if (callback != null) callback.onError(new IOException("HTTP error: " + responseCode));
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static void putAsync(final String path, final JSONObject jsonData, final ResponseCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + path + ".json");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = (jsonData != null ? jsonData.toString() : "null").getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    if (callback != null) callback.onSuccess("Success");
                } else {
                    if (callback != null) callback.onError(new IOException("HTTP error: " + responseCode));
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static void sendCommand(String deviceCode, String cmd, String arg) {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("cmd", cmd);
            json.put("arg", arg != null ? arg : "");
            putAsync("devices/" + deviceCode + "/commands", json, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
