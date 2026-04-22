package com.example.smarthome.Database.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Map;

@Entity(tableName = "mode_config", indices = {@Index(value = "modeName", unique = true)})
public class ModeConfigEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String modeName;
    
    private boolean active;
    
    private Map<String, String> deviceConfiguration;
    
    private long createdAt;
    
    private long updatedAt;
    
    public ModeConfigEntity() {
        this.active = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getModeName() {
        return modeName;
    }
    
    public void setModeName(String modeName) {
        this.modeName = modeName;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Map<String, String> getDeviceConfiguration() {
        return deviceConfiguration;
    }
    
    public void setDeviceConfiguration(Map<String, String> deviceConfiguration) {
        this.deviceConfiguration = deviceConfiguration;
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
    
    public void updateDeviceConfig(String deviceId, String action) {
        if (deviceConfiguration != null) {
            deviceConfiguration.put(deviceId, action);
            this.updatedAt = System.currentTimeMillis();
        }
    }
}
