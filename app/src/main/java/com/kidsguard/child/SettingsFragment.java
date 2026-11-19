package com.kidsguard.child;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.fragment.app.Fragment;
public class SettingsFragment extends Fragment implements View.OnClickListener {
    @Override
    public void onViewCreated(View v, Bundle s) {
        super.onViewCreated(v, s);
        v.findViewById(R.id.btn_clear).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_clear) clearData();
    }
    private void clearData() { /* impl */ }
}
