package com.kidsguard.child;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FirebaseHelper {
    private static final String FIREBASE_URL = "https://easy-earn-971e4-default-rtdb.firebaseio.com/";

    public interface Callback {
        void onResponse(String json);
    }

    public static void put(final String path, final String jsonBody) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(FIREBASE_URL + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public static void delete(final String path) {
        put(path, "null");
    }

    public static void get(final String path, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(FIREBASE_URL + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    if (callback != null) {
                        callback.onResponse(sb.toString());
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onResponse(null);
                    }
                }
            }
        }).start();
    }

    public static String get(final String path) {
        final String[] result = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        get(path, new Callback() {
            @Override
            public void onResponse(String json) {
                result[0] = json;
                latch.countDown();
            }
        });
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return result[0];
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
