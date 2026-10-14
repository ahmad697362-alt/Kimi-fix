package com.kidsguard.child;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FirebaseHelper {
    private static final String FIREBASE_URL = "https://easy-earn-971e4-default-rtdb.firebaseio.com/";
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(Exception e);
    }

    public interface Callback {
        void onResponse(String json);
    }

    public static void put(final String path, final String jsonBody) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(FIREBASE_URL + path + ".json");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes("UTF-8"));
                }
                conn.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public static void delete(final String path) {
        put(path, "null");
    }

    public static void getAsync(final String path, final ResponseCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(FIREBASE_URL + path + ".json");
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

    public static void get(final String path, final Callback callback) {
        getAsync(path, new ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                if (callback != null) callback.onResponse(response);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onResponse(null);
            }
        });
    }

    public static String get(final String path) {
        final String[] result = new String[1];
        final CountDownLatch latch = new CountDownLatch(1);
        getAsync(path, new ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                result[0] = response;
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                result[0] = null;
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
