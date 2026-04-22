package com.example.smarthome.Database.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Map;

@Entity(tableName = "daily_energy_record", indices = {@Index(value = "recordDate", unique = true)})
public class DailyEnergyRecordEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String recordDate;
    
    private double totalEnergy;
    
    private Map<String, Double> deviceBreakdown;
    
    private long createdAt;
    
    private long updatedAt;
    
    public DailyEnergyRecordEntity() {
        this.totalEnergy = 0.0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getRecordDate() {
        return recordDate;
    }
    
    public void setRecordDate(String recordDate) {
        this.recordDate = recordDate;
    }
    
    public double getTotalEnergy() {
        return totalEnergy;
    }
    
    public void setTotalEnergy(double totalEnergy) {
        this.totalEnergy = totalEnergy;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Map<String, Double> getDeviceBreakdown() {
        return deviceBreakdown;
    }
    
    public void setDeviceBreakdown(Map<String, Double> deviceBreakdown) {
        this.deviceBreakdown = deviceBreakdown;
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
    
    public void addDeviceEnergy(String deviceId, double energy) {
        if (deviceBreakdown != null) {
            Double current = deviceBreakdown.get(deviceId);
            if (current != null) {
                deviceBreakdown.put(deviceId, current + energy);
            } else {
                deviceBreakdown.put(deviceId, energy);
            }
            this.totalEnergy += energy;
            this.updatedAt = System.currentTimeMillis();
        }
    }
}
