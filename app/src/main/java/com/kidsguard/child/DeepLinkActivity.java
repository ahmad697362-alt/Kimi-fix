package com.kidsguard.child;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
public class DeepLinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent incoming = getIntent();
        if (incoming != null && incoming.hasExtra("forward_to")) {
            Intent forward = incoming.getParcelableExtra("forward_to");
            if (forward != null && isIntentSafe(forward)) {
                forward.setFlags(0);
                startActivity(forward);
            } else { Log.w("DeepLink", "Rejected unsafe intent"); }
        }
    }
    private boolean isIntentSafe(Intent intent) {
        return intent.getPackage() != null && intent.getPackage().equals(getPackageName());
    }
}
