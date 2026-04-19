package com.example.smarthome.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smarthome.Model.DeviceEnergy;
import com.example.smarthome.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 设备能耗列表适配器
 * 用于展示各设备的能耗详情，支持实时更新和按耗电量排序
 */
public class DeviceEnergyAdapter extends RecyclerView.Adapter<DeviceEnergyAdapter.ViewHolder> {
    
    private List<DeviceEnergy> deviceList = new ArrayList<>();
    private EnergyUpdateCallback energyUpdateCallback;
    
    /**
     * 能耗更新回调接口
     * 用于获取设备的实时能耗数据
     */
    public interface EnergyUpdateCallback {
        double getRealtimeEnergy(String deviceId);
        double getRealtimeUsageHours(String deviceId);
    }
    
    /**
     * 设置设备列表并按耗电量降序排序
     */
    public void setDeviceList(List<DeviceEnergy> devices) {
        this.deviceList = devices != null ? new ArrayList<>(devices) : new ArrayList<>();
        sortDevicesByEnergy();
        notifyDataSetChanged();
    }
    
    /**
     * 更新数据并重新排序（用于实时刷新）
     */
    public void updateAndSort() {
        sortDevicesByEnergy();
        notifyDataSetChanged();
    }
    
    /**
     * 按耗电量降序排序设备列表
     */
    private void sortDevicesByEnergy() {
        if (deviceList.isEmpty()) return;
        
        Collections.sort(deviceList, new Comparator<DeviceEnergy>() {
            @Override
            public int compare(DeviceEnergy d1, DeviceEnergy d2) {
                double energy1 = getDeviceRealtimeEnergy(d1);
                double energy2 = getDeviceRealtimeEnergy(d2);
                // 降序排列：能耗高的排前面
                return Double.compare(energy2, energy1);
            }
        });
    }
    
    /**
     * 获取设备的实时能耗
     */
    private double getDeviceRealtimeEnergy(DeviceEnergy device) {
        if (energyUpdateCallback != null) {
            return energyUpdateCallback.getRealtimeEnergy(device.getDeviceId());
        }
        return device.getTodayEnergyKWh();
    }
    
    public void setEnergyUpdateCallback(EnergyUpdateCallback callback) {
        this.energyUpdateCallback = callback;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_energy, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceEnergy device = deviceList.get(position);
        
        // 设置排名标识（前3名显示特殊标识）
        if (position < 3 && getDeviceRealtimeEnergy(device) > 0) {
            String rankIcon = position == 0 ? "🥇" : (position == 1 ? "🥈" : "🥉");
            holder.tvDeviceName.setText(rankIcon + " " + device.getDeviceName());
        } else {
            holder.tvDeviceName.setText(device.getDeviceName());
        }
        
        // 设置设备图标
        holder.ivDeviceIcon.setImageResource(device.getIconResId());
        
        // 设置功率信息
        holder.tvDevicePower.setText(String.format("%.0fW", device.getPowerWatts()));
        
        // 获取实时能耗数据
        double realtimeEnergy = getDeviceRealtimeEnergy(device);
        double realtimeUsage = device.getTodayUsageHours();
        
        if (energyUpdateCallback != null) {
            realtimeUsage = energyUpdateCallback.getRealtimeUsageHours(device.getDeviceId());
        }
        
        // 设置能耗数值
        if (realtimeEnergy < 0.001) {
            holder.tvEnergyValue.setText("0瓦时");
        } else if (realtimeEnergy < 1) {
            holder.tvEnergyValue.setText(String.format("%.1f瓦时", realtimeEnergy * 1000));
        } else {
            holder.tvEnergyValue.setText(String.format("%.3f度", realtimeEnergy));
        }
        
        // 设置使用时长（以分钟为基本单位）
        int totalMinutes = (int) (realtimeUsage * 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        
        if (hours > 0) {
            holder.tvUsageTime.setText(String.format("%d时%d分", hours, minutes));
        } else {
            holder.tvUsageTime.setText(String.format("%d分钟", minutes));
        }
        
        // 如果设备正在运行，显示运行状态指示
        if (device.isRunning()) {
            holder.tvDeviceName.setTextColor(0xFF4CAF50);
        } else {
            holder.tvDeviceName.setTextColor(0xFF333333);
        }
    }
    
    @Override
    public int getItemCount() {
        return deviceList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeviceIcon;
        TextView tvDeviceName;
        TextView tvDevicePower;
        TextView tvEnergyValue;
        TextView tvUsageTime;
        
        ViewHolder(View itemView) {
            super(itemView);
            ivDeviceIcon = itemView.findViewById(R.id.iv_device_icon);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDevicePower = itemView.findViewById(R.id.tv_device_power);
            tvEnergyValue = itemView.findViewById(R.id.tv_energy_value);
            tvUsageTime = itemView.findViewById(R.id.tv_usage_time);
        }
    }
}
