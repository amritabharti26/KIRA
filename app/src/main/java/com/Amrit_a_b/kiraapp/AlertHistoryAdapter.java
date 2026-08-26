package com.Amrit_a_b.kiraapp;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlertHistoryAdapter extends RecyclerView.Adapter<AlertHistoryAdapter.ViewHolder> {

    private final List<Alert> alertList;

    public AlertHistoryAdapter(List<Alert> alertList) {
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Alert alert = alertList.get(position);
        holder.tvTime.setText(alert.getTimestamp());
        holder.tvType.setText(alert.getStatus());

        holder.itemView.setOnClickListener(v -> {
            if (alert.getLocation() != null && alert.getLocation().startsWith("http")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(alert.getLocation()));
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_alert_type);
            tvTime = itemView.findViewById(R.id.tv_alert_time);
        }
    }
}