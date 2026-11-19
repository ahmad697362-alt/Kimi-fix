package com.kidsguard.child;
import java.io.*;
public class FileFix {
    public static String read(File f) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder(); String l;
            while ((l = r.readLine()) != null) sb.append(l).append("
");
            return sb.toString();
        }
    }
}