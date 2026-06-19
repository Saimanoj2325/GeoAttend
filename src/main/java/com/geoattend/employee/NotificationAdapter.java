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

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private final List<NotificationItem> list;

    public NotificationAdapter(List<NotificationItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = list.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvTime.setText(new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(item.getTimestamp()));
        
        if ("Security".equalsIgnoreCase(item.getCategory())) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            holder.ivIcon.setColorFilter(0xFFEF4444);
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_popup_reminder);
            holder.ivIcon.setColorFilter(0xFF3B82F6);
        }

        holder.itemView.setAlpha(item.isRead() ? 0.6f : 1.0f);
        holder.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (!item.isRead() && item.getId() != null) {
                com.geoattend.utils.FirebaseHelper.getFirestore()
                    .collection("notifications").document(item.getId())
                    .update("read", true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        ImageView ivIcon;
        View unreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvBody = itemView.findViewById(R.id.tv_body);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            unreadDot = itemView.findViewById(R.id.view_unread);
        }
    }

    public static class NotificationItem {
        private String id;
        private String title;
        private String body;
        private String category;
        private Date timestamp;
        private boolean read;

        public NotificationItem() {}
        public NotificationItem(String title, String body, String category, Date timestamp, boolean read) {
            this.title = title; this.body = body; this.category = category; this.timestamp = timestamp; this.read = read;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getCategory() { return category; }
        public Date getTimestamp() { return timestamp; }
        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }
}
