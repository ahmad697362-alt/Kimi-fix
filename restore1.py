import os
b = "app/src/main/java/com/kidsguard/child"

files = {}

files["BlockAccessibility.java"] = """package com.kidsguard.child;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class BlockAccessibility extends AccessibilityService {
    public static final List<String> blockedPkgs = Collections.synchronizedList(new ArrayList<>());
    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() != null && blockedPkgs.contains(e.getPackageName().toString())) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }
    @Override public void onInterrupt() {}
}
"""

files["ActionReceiver.java"] = """package com.kidsguard.child;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, final Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try { handleAction(ctx, intent); }
            catch (Exception e) { Log.e("Action", "Fail", e); }
            finally { pr.finish(); }
        }).start();
    }
    private void handleAction(Context ctx, Intent intent) {
        String action = intent.getStringExtra("action");
        Log.d("Action", "Received: " + action);
    }
}
"""

files["AppBlockedActivity.java"] = """package com.kidsguard.child;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
public class AppBlockedActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(this::finish, 2000);
    }
}
"""

files["BlockWarningActivity.java"] = """package com.kidsguard.child;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
public class BlockWarningActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(this::finish, 3000);
    }
}
"""

files["CameraActivity.java"] = """package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
public class CameraActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            File f = File.createTempFile("cap_", ".jpg", getExternalFilesDir(null));
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(i, 1001);
        } catch (IOException e) { finish(); }
    }
}
"""

for name, code in files.items():
    with open(f"{b}/{name}", "w") as f:
        f.write(code)
    print(f"Fixed {name}")

print("Chunk 1 done — 5 files.")
