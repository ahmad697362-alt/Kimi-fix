package com.kidsguard.child;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
public class NetworkClient {
    public String get(String urlStr) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
            if (is == null) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = r.readLine()) != null) sb.append(l);
            return sb.toString();
        } finally { c.disconnect(); }
    }
}