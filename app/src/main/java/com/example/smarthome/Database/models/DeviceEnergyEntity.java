package com.example.smarthome.Database.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_energy", indices = {@Index(value = "deviceId", unique = true)})
public class DeviceEnergyEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String deviceId;
    
    private String deviceName;
    
    private String deviceType;
    
    private double powerRating;
    
    private double totalEnergy;
    
    private long totalRunningTime;
    
    private boolean running;
    
    private long startTime;
    
    private long lastUpdateTime;
    
    private long createdAt;
    
    public DeviceEnergyEntity() {
        this.totalEnergy = 0.0;
        this.totalRunningTime = 0L;
        this.running = false;
        this.startTime = 0L;
        this.lastUpdateTime = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public double getPowerRating() {
        return powerRating;
    }
    
    public void setPowerRating(double powerRating) {
        this.powerRating = powerRating;
    }
    
    public double getTotalEnergy() {
        return totalEnergy;
    }
    
    public void setTotalEnergy(double totalEnergy) {
        this.totalEnergy = totalEnergy;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public long getTotalRunningTime() {
        return totalRunningTime;
    }
    
    public void setTotalRunningTime(long totalRunningTime) {
        this.totalRunningTime = totalRunningTime;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public void addEnergy(double energy) {
        this.totalEnergy += energy;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    public void addRunningTime(long time) {
        this.totalRunningTime += time;
        this.lastUpdateTime = System.currentTimeMillis();
    }
}
