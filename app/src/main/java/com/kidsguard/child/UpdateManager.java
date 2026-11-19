package com.kidsguard.child;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
public class UpdateManager {
    public void extract(File zip, File dest) throws IOException {
        String destPath = dest.getCanonicalPath();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(dest, e.getName());
                if (!out.getCanonicalPath().startsWith(destPath + File.separator))
                    throw new IOException("Path traversal blocked");
                if (e.isDirectory()) out.mkdirs();
                else {
                    out.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[4096]; int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                    }
                }
            }
        }
    }
}
