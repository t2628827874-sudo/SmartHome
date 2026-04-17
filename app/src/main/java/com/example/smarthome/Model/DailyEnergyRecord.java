package com.example.smarthome.Model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 每日能耗记录模型
 * 用于存储单日的能耗统计数据
 */
public class DailyEnergyRecord implements Serializable {
    private String date;                              // 日期字符串（格式：yyyy-MM-dd）
    private long timestamp;                           // 时间戳
    private double totalEnergyKWh;                    // 当日总耗电量（千瓦时）
    private Map<String, Double> deviceEnergyMap;      // 各设备耗电量映射

    public DailyEnergyRecord() {
        this.deviceEnergyMap = new HashMap<>();
    }

    public DailyEnergyRecord(String date, long timestamp) {
        this.date = date;
        this.timestamp = timestamp;
        this.totalEnergyKWh = 0;
        this.deviceEnergyMap = new HashMap<>();
    }

    /**
     * 添加设备耗电量
     * @param deviceId 设备ID
     * @param energyKWh 耗电量（千瓦时）
     */
    public void addDeviceEnergy(String deviceId, double energyKWh) {
        double current = deviceEnergyMap.getOrDefault(deviceId, 0.0);
        deviceEnergyMap.put(deviceId, current + energyKWh);
        totalEnergyKWh += energyKWh;
    }

    /**
     * 设置设备耗电量（覆盖）
     * @param deviceId 设备ID
     * @param energyKWh 耗电量（千瓦时）
     */
    public void setDeviceEnergy(String deviceId, double energyKWh) {
        double oldEnergy = deviceEnergyMap.getOrDefault(deviceId, 0.0);
        deviceEnergyMap.put(deviceId, energyKWh);
        totalEnergyKWh = totalEnergyKWh - oldEnergy + energyKWh;
    }

    /**
     * 获取设备耗电量
     * @param deviceId 设备ID
     * @return 耗电量（千瓦时）
     */
    public double getDeviceEnergy(String deviceId) {
        return deviceEnergyMap.getOrDefault(deviceId, 0.0);
    }

    /**
     * 获取格式化的总耗电量显示
     * @return 格式化字符串
     */
    public String getFormattedTotalEnergy() {
        if (totalEnergyKWh < 1) {
            return String.format("%.1f瓦时", totalEnergyKWh * 1000);
        } else {
            return String.format("%.2f度", totalEnergyKWh);
        }
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public double getTotalEnergyKWh() { return totalEnergyKWh; }
    public void setTotalEnergyKWh(double totalEnergyKWh) { this.totalEnergyKWh = totalEnergyKWh; }
    
    public Map<String, Double> getDeviceEnergyMap() { return deviceEnergyMap; }
    public void setDeviceEnergyMap(Map<String, Double> deviceEnergyMap) { 
        this.deviceEnergyMap = deviceEnergyMap; 
    }
}
