package com.kidsguard.child;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
public class FirebaseHelper {
    private static final String BASE_URL = "https://easy-earn-971e4-default-rtdb.firebaseio.com/";
    private static final ExecutorService io = Executors.newFixedThreadPool(3);
    public static void put(final String p, final String d) { io.execute(() -> write("PUT", p, d)); }
    public static void post(final String p, final String d) { io.execute(() -> write("POST", p, d)); }
    public static void delete(final String p) { io.execute(() -> {
        HttpURLConnection c = null; try { c = (HttpURLConnection) new URL(BASE_URL + p).openConnection(); c.setRequestMethod("DELETE"); c.setConnectTimeout(15000); c.setReadTimeout(15000); c.getResponseCode(); } catch (Exception e) {} finally { if (c != null) c.disconnect(); }
    }); }
    public static String get(final String p) {
        HttpURLConnection c = null; try { c = (HttpURLConnection) new URL(BASE_URL + p).openConnection(); c.setRequestMethod("GET"); c.setConnectTimeout(15000); c.setReadTimeout(15000); if (c.getResponseCode() != 200) return null; BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8")); StringBuilder sb = new StringBuilder(); String l; while ((l = r.readLine()) != null) sb.append(l); r.close(); return sb.toString(); } catch (Exception e) { return null; } finally { if (c != null) c.disconnect(); }
    }
    public static String escapeJson(String s) {
        if (s == null) return ""; try { JSONObject o = new JSONObject(); o.put("v", s); String f = o.toString(); int a = f.indexOf(":"") + 2; int b = f.lastIndexOf("""); return (a > 1 && b > a) ? f.substring(a, b) : s; } catch (Exception e) { return s; }
    }
    public static void sendCommand(String dc, String cmd, String arg) { try { JSONObject j = new JSONObject(); j.put("cmd", cmd); j.put("arg", arg != null ? arg : ""); put("devices/" + dc + "/commands.json", j.toString()); } catch (Exception e) {} }
    private static void write(String m, String p, String d) {
        HttpURLConnection c = null; try { c = (HttpURLConnection) new URL(BASE_URL + p).openConnection(); c.setRequestMethod(m); c.setRequestProperty("Content-Type", "application/json"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(15000); try (OutputStream os = c.getOutputStream()) { os.write(d.getBytes("UTF-8")); os.flush(); } c.getResponseCode(); } catch (Exception e) {} finally { if (c != null) c.disconnect(); }
    }
}
