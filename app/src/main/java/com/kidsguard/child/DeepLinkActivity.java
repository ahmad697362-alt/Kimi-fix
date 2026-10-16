package com.kidsguard.child;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DeepLinkActivity extends Activity {
    private static final String TAG = "DeepLinkActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent incoming = getIntent();
        if (incoming != null && incoming.hasExtra("forward_to")) {
            Intent forward = incoming.getParcelableExtra("forward_to");
            
            if (forward != null && isIntentSafe(forward)) {
                forward.setFlags(0);
                try {
                    startActivity(forward);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start forwarded intent", e);
                }
            } else {
                Log.w(TAG, "Rejected unsafe or unresolvable forwarded intent");
            }
        }
        finish();
    }

    private boolean isIntentSafe(Intent intent) {
        return intent != null && intent.getPackage() != null && intent.getPackage().equals(getPackageName());
    }
}
