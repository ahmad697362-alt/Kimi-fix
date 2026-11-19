package com.kidsguard.child;
import android.net.Uri;
public class UriSanitizer {
    public static String sanitize(String u) { return u != null ? u.replaceAll("[^a-zA-Z0-9._~:/?#\[\]@!$&'()*+,;=-]", "") : ""; }
}