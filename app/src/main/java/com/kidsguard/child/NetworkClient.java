package com.kidsguard.child;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkClient {

    public String sendRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            int responseCode = conn.getResponseCode();

            InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
                if (inputStream == null) {
                    throw new IOException("HTTP Error: " + responseCode + " (No error body)");
                }
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            if (responseCode >= 300) {
                throw new IOException("HTTP Error " + responseCode + ": " + result.toString());
            }

            return result.toString();
        } finally {
            conn.disconnect();
        }
    }
}
