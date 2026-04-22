package com.example.smarthome.Database.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "device_timer_task",
    indices = {
        @Index(value = "taskId", unique = true),
        @Index(value = "deviceId")
    },
    foreignKeys = {
        @ForeignKey(
            entity = DeviceEnergyEntity.class,
            parentColumns = "deviceId",
            childColumns = "deviceId",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    }
)
public class DeviceTimerTaskEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String taskId;
    
    private String deviceId;
    
    private String taskName;
    
    private String action;
    
    private String executionTime;
    
    private boolean enabled;
    
    private String repeatPattern;
    
    private long createdAt;
    
    private long updatedAt;
    
    public DeviceTimerTaskEntity() {
        this.enabled = true;
        this.repeatPattern = "once";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getRepeatPattern() {
        return repeatPattern;
    }
    
    public void setRepeatPattern(String repeatPattern) {
        this.repeatPattern = repeatPattern;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
