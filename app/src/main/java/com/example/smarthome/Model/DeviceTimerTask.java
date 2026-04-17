package com.example.smarthome.Model;

import java.io.Serializable;

public class DeviceTimerTask implements Serializable {
    private String taskId;
    private String deviceType;
    private String deviceName;
    private long startTimeMillis;
    private int durationMinutes;
    private boolean enabled;
    private boolean isRepeating;
    private long createdAt;
    
    public static final String DEVICE_ROBOT = "bedroom_robot";
    public static final String DEVICE_DEHUMIDIFIER = "bedroom_dehumidifier";
    
    public DeviceTimerTask() {
        this.enabled = true;
        this.isRepeating = false;
        this.createdAt = System.currentTimeMillis();
    }
    
    public DeviceTimerTask(String taskId, String deviceType, String deviceName, 
                          long startTimeMillis, int durationMinutes) {
        this.taskId = taskId;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
        this.startTimeMillis = startTimeMillis;
        this.durationMinutes = durationMinutes;
        this.enabled = true;
        this.isRepeating = false;
        this.createdAt = System.currentTimeMillis();
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public long getStartTimeMillis() {
        return startTimeMillis;
    }
    
    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isRepeating() {
        return isRepeating;
    }
    
    public void setRepeating(boolean repeating) {
        isRepeating = repeating;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getEndTimeMillis() {
        return startTimeMillis + (durationMinutes * 60 * 1000L);
    }
    
    public String getFormattedStartTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(startTimeMillis));
    }
    
    public String getFormattedDuration() {
        if (durationMinutes >= 60) {
            int hours = durationMinutes / 60;
            int mins = durationMinutes % 60;
            if (mins == 0) {
                return hours + "小时";
            }
            return hours + "小时" + mins + "分钟";
        }
        return durationMinutes + "分钟";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceTimerTask that = (DeviceTimerTask) o;
        return taskId != null && taskId.equals(that.taskId);
    }
    
    @Override
    public int hashCode() {
        return taskId != null ? taskId.hashCode() : 0;
    }
}
