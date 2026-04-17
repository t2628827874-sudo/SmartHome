package com.example.smarthome.Model;

import java.io.Serializable;

/**
 * 设备能耗数据模型
 * 用于存储单个设备的能耗参数和统计数据
 */
public class DeviceEnergy implements Serializable {
    private String deviceId;          // 设备唯一标识
    private String deviceName;        // 设备名称
    private String deviceType;        // 设备类型（如：空调、灯光等）
    private double powerWatts;        // 额定功率（瓦特）
    private double todayUsageHours;   // 今日使用时长（小时）
    private double todayEnergyKWh;    // 今日耗电量（千瓦时）
    private int iconResId;            // 设备图标资源ID
    private boolean isRunning;        // 当前运行状态

    public DeviceEnergy() {
    }

    public DeviceEnergy(String deviceId, String deviceName, String deviceType, 
                       double powerWatts, int iconResId) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.powerWatts = powerWatts;
        this.iconResId = iconResId;
        this.todayUsageHours = 0;
        this.todayEnergyKWh = 0;
        this.isRunning = false;
    }

    /**
     * 计算指定使用时长下的耗电量
     * @param hours 使用时长（小时）
     * @return 耗电量（千瓦时）
     */
    public double calculateEnergy(double hours) {
        return (powerWatts * hours) / 1000.0;
    }

    /**
     * 添加使用时长并更新耗电量
     * @param hours 新增使用时长（小时）
     */
    public void addUsageTime(double hours) {
        this.todayUsageHours += hours;
        this.todayEnergyKWh = calculateEnergy(todayUsageHours);
    }

    /**
     * 获取格式化的今日耗电量显示
     * @return 格式化字符串
     */
    public String getFormattedTodayEnergy() {
        if (todayEnergyKWh < 1) {
            return String.format("%.1f瓦时", todayEnergyKWh * 1000);
        } else {
            return String.format("%.2f度", todayEnergyKWh);
        }
    }

    /**
     * 获取格式化的今日使用时长显示
     * @return 格式化字符串
     */
    public String getFormattedTodayUsage() {
        if (todayUsageHours < 1) {
            return String.format("%.0f分钟", todayUsageHours * 60);
        } else {
            int hours = (int) todayUsageHours;
            int minutes = (int) ((todayUsageHours - hours) * 60);
            if (minutes > 0) {
                return String.format("%d小时%d分钟", hours, minutes);
            }
            return String.format("%d小时", hours);
        }
    }

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public double getPowerWatts() { return powerWatts; }
    public void setPowerWatts(double powerWatts) { this.powerWatts = powerWatts; }
    
    public double getTodayUsageHours() { return todayUsageHours; }
    public void setTodayUsageHours(double todayUsageHours) { this.todayUsageHours = todayUsageHours; }
    
    public double getTodayEnergyKWh() { return todayEnergyKWh; }
    public void setTodayEnergyKWh(double todayEnergyKWh) { this.todayEnergyKWh = todayEnergyKWh; }
    
    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { isRunning = running; }
}
