package com.geoattend.employee;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.geoattend.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
    private final List<TimelineItem> items;

    public TimelineAdapter(List<TimelineItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvTime.setText(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(item.getTimestamp()));
        holder.ivIcon.setImageResource(item.getIconRes());
        
        if (position == items.size() - 1) {
            holder.line.setVisibility(View.GONE);
        } else {
            holder.line.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        ImageView ivIcon;
        View line;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            line = itemView.findViewById(R.id.timeline_line);
        }
    }

    public static class TimelineItem {
        private final String title;
        private final Date timestamp;
        private final int iconRes;

        public TimelineItem(String title, Date timestamp, int iconRes) {
            this.title = title;
            this.timestamp = timestamp;
            this.iconRes = iconRes;
        }

        public String getTitle() { return title; }
        public Date getTimestamp() { return timestamp; }
        public int getIconRes() { return iconRes; }
    }
}
