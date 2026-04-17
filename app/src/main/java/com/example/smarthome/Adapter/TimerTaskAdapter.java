package com.example.smarthome.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class TimerTaskAdapter extends RecyclerView.Adapter<TimerTaskAdapter.ViewHolder> {
    
    private List<DeviceTimerTask> tasks = new ArrayList<>();
    private OnTaskActionListener listener;
    
    public interface OnTaskActionListener {
        void onTaskEnabledChanged(String taskId, boolean enabled);
        void onTaskDeleted(String taskId);
    }
    
    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }
    
    public void setTasks(List<DeviceTimerTask> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timer_task, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceTimerTask task = tasks.get(position);
        
        holder.tvDeviceName.setText(task.getDeviceName());
        holder.tvStartTime.setText(task.getFormattedStartTime());
        holder.tvDuration.setText(task.getFormattedDuration());
        holder.swEnabled.setChecked(task.isEnabled());
        
        if (task.isEnabled()) {
            holder.tvStatus.setText("已启用");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.status_on));
        } else {
            holder.tvStatus.setText("已禁用");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.text_hint));
        }
        
        holder.swEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskEnabledChanged(task.getTaskId(), isChecked);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDeleted(task.getTaskId());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return tasks.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvStartTime;
        TextView tvDuration;
        TextView tvStatus;
        SwitchMaterial swEnabled;
        ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvStartTime = itemView.findViewById(R.id.tv_start_time);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvStatus = itemView.findViewById(R.id.tv_status);
            swEnabled = itemView.findViewById(R.id.sw_enabled);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
