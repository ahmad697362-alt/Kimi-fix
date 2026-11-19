package com.kidsguard.child;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class DashboardFragment extends Fragment {
    private TextView tv;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    @Override
    public void onViewCreated(View v, Bundle s) {
        super.onViewCreated(v, s);
        tv = v.findViewById(R.id.device_id_text);
        io.execute(() -> {
            String id = requireActivity().getSharedPreferences("prefs", 0).getString("id", "Unknown");
            tv.post(() -> { if (isAdded()) tv.setText("Device: " + id); });
        });
    }
    @Override public void onDestroyView() { super.onDestroyView(); io.shutdownNow(); }
}
