package com.kidsguard.child;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.Set;
public class BluetoothManagerFix {
    public static Set<BluetoothDevice> getPairedDevices(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w("BT", "BLUETOOTH_CONNECT missing");
                return null;
            }
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null ? adapter.getBondedDevices() : null;
    }
}
