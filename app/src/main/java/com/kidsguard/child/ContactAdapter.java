package com.kidsguard.child;
import androidx.recyclerview.widget.DiffUtil;
import java.util.List;
public class ContactAdapter {
    static class Diff extends DiffUtil.ItemCallback<Object> {
        @Override public boolean areItemsTheSame(Object o, Object n) { return o == n; }
        @Override public boolean areContentsTheSame(Object o, Object n) { return o.equals(n); }
    }
}