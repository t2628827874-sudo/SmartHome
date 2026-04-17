package com.example.smarthome.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.smarthome.Model.DailyEnergyRecord;
import com.example.smarthome.Model.DeviceEnergy;
import com.example.smarthome.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
     * 能耗管理服务
 * 负责设备能耗数据的记录、统计和管理
 * 支持数据持久化存储，确保APP重启后数据不丢失
 */
public class EnergyManager {
    private static final String TAG = "EnergyManager";
    private static final String PREFS_NAME = "energy_data";
    private static final String KEY_DEVICE_ENERGY = "device_energy";
    private static final String KEY_DAILY_RECORDS = "daily_records";
    private static final String KEY_LAST_UPDATE_DATE = "last_update_date";
    private static final String KEY_DEVICE_START_TIMES = "device_start_times";
    private static final String KEY_INITIALIZED = "data_initialized";
    
    private static EnergyManager instance;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    
    private Map<String, DeviceEnergy> deviceEnergyMap;      // 设备能耗配置
    private List<DailyEnergyRecord> dailyRecords;           // 每日能耗记录
    private Map<String, Long> deviceStartTimeMap;           // 设备启动时间记录
    private boolean isInitialized;                          // 是否已初始化数据

    private EnergyManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.deviceEnergyMap = new HashMap<>();
        this.dailyRecords = new ArrayList<>();
        this.deviceStartTimeMap = new HashMap<>();
        
        loadData();
        initializeDefaultDevices();
        checkAndUpdateDate();
        restoreRunningDevices();
    }

    public static synchronized EnergyManager getInstance(Context context) {
        if (instance == null) {
            instance = new EnergyManager(context);
        }
        return instance;
    }

    /**
     * 初始化默认设备能耗配置
     * 功率设置参考实际家用电器额定功率
     */
    private void initializeDefaultDevices() {
        if (deviceEnergyMap.isEmpty()) {
            // 客厅设备
            deviceEnergyMap.put("living_led", new DeviceEnergy("living_led", "客厅灯", "灯光", 15, R.drawable.light));
            deviceEnergyMap.put("living_camera", new DeviceEnergy("living_camera", "摄像头", "安防", 10, R.drawable.camera));
            
            // 餐厅设备
            deviceEnergyMap.put("dining_led", new DeviceEnergy("dining_led", "餐厅灯", "灯光", 15, R.drawable.light));
            deviceEnergyMap.put("dining_fridge", new DeviceEnergy("dining_fridge", "冰箱", "电器", 120, R.drawable.ic_fridge));
            deviceEnergyMap.put("dining_aircon", new DeviceEnergy("dining_aircon", "餐厅空调", "空调", 1500, R.drawable.ic_aircon));
            
            // 卧室设备
            deviceEnergyMap.put("bedroom_dehumidifier", new DeviceEnergy("bedroom_dehumidifier", "除湿器", "电器", 250, R.drawable.ic_dehumidifier));
            deviceEnergyMap.put("bedroom_aircon", new DeviceEnergy("bedroom_aircon", "卧室空调", "空调", 1200, R.drawable.ic_aircon));
            deviceEnergyMap.put("bedroom_curtain", new DeviceEnergy("bedroom_curtain", "窗帘", "其他", 30, R.drawable.curtain));
            deviceEnergyMap.put("bedroom_robot", new DeviceEnergy("bedroom_robot", "扫地机器人", "电器", 45, R.drawable.robot));
            deviceEnergyMap.put("bedroom_projector", new DeviceEnergy("bedroom_projector", "投影仪", "电器", 200, R.drawable.ic_projector));
            deviceEnergyMap.put("bedroom_speaker", new DeviceEnergy("bedroom_speaker", "智能音响", "电器", 20, R.drawable.ic_speaker));
            
            saveData();
        }
    }

    /**
     * 检查并更新日期，如果是新的一天则创建新的记录
     */
    private void checkAndUpdateDate() {
        String today = getTodayDateString();
        String lastUpdateDate = sharedPreferences.getString(KEY_LAST_UPDATE_DATE, "");
        
        if (!today.equals(lastUpdateDate)) {
            // 新的一天，重置今日使用时长
            for (DeviceEnergy device : deviceEnergyMap.values()) {
                device.setTodayUsageHours(0);
                device.setTodayEnergyKWh(0);
            }
            
            // 确保有最近7天的记录
            ensureRecentRecords();
            
            sharedPreferences.edit().putString(KEY_LAST_UPDATE_DATE, today).apply();
            saveData();
            
            Log.d(TAG, "日期更新，已重置今日能耗数据");
        }
    }

    /**
     * 确保有最近7天的记录
     */
    private void ensureRecentRecords() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        List<String> recentDates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            recentDates.add(sdf.format(calendar.getTime()));
        }
        
        // 移除超过7天的旧记录
        dailyRecords.removeIf(record -> !recentDates.contains(record.getDate()));
        
        // 添加缺失的日期记录
        for (String date : recentDates) {
            boolean found = false;
            for (DailyEnergyRecord record : dailyRecords) {
                if (record.getDate().equals(date)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Calendar cal = Calendar.getInstance();
                try {
                    cal.setTime(sdf.parse(date));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dailyRecords.add(new DailyEnergyRecord(date, cal.getTimeInMillis()));
            }
        }
        
        // 按日期排序
        dailyRecords.sort((r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));
    }

    /**
     * 设备状态变化时调用
     * @param deviceId 设备ID
     * @param isRunning 是否运行中
     */
    public void onDeviceStateChanged(String deviceId, boolean isRunning) {
        DeviceEnergy device = deviceEnergyMap.get(deviceId);
        if (device == null) {
            Log.w(TAG, "未知设备: " + deviceId);
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (isRunning && !device.isRunning()) {
            // 设备启动，记录开始时间
            deviceStartTimeMap.put(deviceId, currentTime);
            device.setRunning(true);
            Log.d(TAG, "设备启动: " + device.getDeviceName());
            
        } else if (!isRunning && device.isRunning()) {
            // 设备停止，计算使用时长
            Long startTime = deviceStartTimeMap.get(deviceId);
            if (startTime != null) {
                double usageHours = (currentTime - startTime) / (1000.0 * 60 * 60);
                device.addUsageTime(usageHours);
                
                // 更新今日记录
                updateTodayRecord(deviceId, device.getTodayEnergyKWh());
                
                deviceStartTimeMap.remove(deviceId);
                Log.d(TAG, "设备停止: " + device.getDeviceName() + ", 使用时长: " + 
                        String.format("%.2f", usageHours) + "小时");
            }
            device.setRunning(false);
        }
        
        saveData();
    }

    /**
     * 更新今日能耗记录
     */
    private void updateTodayRecord(String deviceId, double energyKWh) {
        String today = getTodayDateString();
        DailyEnergyRecord todayRecord = null;
        
        for (DailyEnergyRecord record : dailyRecords) {
            if (record.getDate().equals(today)) {
                todayRecord = record;
                break;
            }
        }
        
        if (todayRecord == null) {
            todayRecord = new DailyEnergyRecord(today, System.currentTimeMillis());
            dailyRecords.add(todayRecord);
        }
        
        todayRecord.setDeviceEnergy(deviceId, energyKWh);
    }

    /**
     * 获取最近7天的能耗记录
     */
    public List<DailyEnergyRecord> getRecent7DaysRecords() {
        ensureRecentRecords();
        return new ArrayList<>(dailyRecords);
    }

    /**
     * 获取所有设备能耗配置
     */
    public List<DeviceEnergy> getAllDeviceEnergy() {
        return new ArrayList<>(deviceEnergyMap.values());
    }

    /**
     * 获取今日总耗电量
     */
    public double getTodayTotalEnergy() {
        double total = 0;
        for (DeviceEnergy device : deviceEnergyMap.values()) {
            total += device.getTodayEnergyKWh();
        }
        return total;
    }

    /**
     * 获取今日总使用时长
     */
    public double getTodayTotalUsageHours() {
        double total = 0;
        for (DeviceEnergy device : deviceEnergyMap.values()) {
            total += device.getTodayUsageHours();
        }
        return total;
    }

    /**
     * 获取设备能耗配置
     */
    public DeviceEnergy getDeviceEnergy(String deviceId) {
        return deviceEnergyMap.get(deviceId);
    }

    /**
     * 更新设备功率设置
     */
    public void updateDevicePower(String deviceId, double powerWatts) {
        DeviceEnergy device = deviceEnergyMap.get(deviceId);
        if (device != null) {
            device.setPowerWatts(powerWatts);
            saveData();
        }
    }

    /**
     * 获取今日日期字符串
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 加载持久化数据
     */
    private void loadData() {
        // 加载设备能耗配置
        String deviceEnergyJson = sharedPreferences.getString(KEY_DEVICE_ENERGY, "");
        if (!deviceEnergyJson.isEmpty()) {
            Type type = new TypeToken<Map<String, DeviceEnergy>>(){}.getType();
            Map<String, DeviceEnergy> loaded = gson.fromJson(deviceEnergyJson, type);
            if (loaded != null) {
                deviceEnergyMap = loaded;
            }
        }
        
        // 加载每日记录
        String dailyRecordsJson = sharedPreferences.getString(KEY_DAILY_RECORDS, "");
        if (!dailyRecordsJson.isEmpty()) {
            Type type = new TypeToken<List<DailyEnergyRecord>>(){}.getType();
            List<DailyEnergyRecord> loaded = gson.fromJson(dailyRecordsJson, type);
            if (loaded != null) {
                dailyRecords = loaded;
            }
        }
    }

    /**
     * 保存数据到持久化存储
     */
    private void saveData() {
        String deviceEnergyJson = gson.toJson(deviceEnergyMap);
        String dailyRecordsJson = gson.toJson(dailyRecords);
        String startTimesJson = gson.toJson(deviceStartTimeMap);
        
        sharedPreferences.edit()
                .putString(KEY_DEVICE_ENERGY, deviceEnergyJson)
                .putString(KEY_DAILY_RECORDS, dailyRecordsJson)
                .putString(KEY_DEVICE_START_TIMES, startTimesJson)
                .apply();
    }
    
    /**
     * 强制保存数据（用于APP退出时）
     */
    public void forceSave() {
        saveData();
        Log.d(TAG, "强制保存能耗数据");
    }

    /**
     * 初始化能耗数据（仅在首次使用时调用）
     * 历史数据为0，仅今日开始统计
     */
    public void generateDemoData() {
        // 检查是否已初始化，避免重复清空数据
        isInitialized = sharedPreferences.getBoolean(KEY_INITIALIZED, false);
        if (isInitialized) {
            Log.d(TAG, "数据已存在，跳过初始化");
            ensureRecentRecords();
            return;
        }
        
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        dailyRecords.clear();
        
        // 初始化最近7天的记录，历史数据为0
        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            String date = sdf.format(calendar.getTime());
            
            DailyEnergyRecord record = new DailyEnergyRecord(date, calendar.getTimeInMillis());
            dailyRecords.add(record);
        }
        
        // 标记已初始化
        sharedPreferences.edit().putBoolean(KEY_INITIALIZED, true).apply();
        saveData();
        Log.d(TAG, "已初始化能耗数据，历史数据为0");
    }
    
    /**
     * 恢复正在运行的设备状态
     * APP重启后恢复设备运行计时
     */
    private void restoreRunningDevices() {
        // 从持久化存储中恢复设备启动时间
        String startTimesJson = sharedPreferences.getString(KEY_DEVICE_START_TIMES, "");
        if (!startTimesJson.isEmpty()) {
            try {
                Type type = new TypeToken<Map<String, Long>>(){}.getType();
                Map<String, Long> savedStartTimes = gson.fromJson(startTimesJson, type);
                if (savedStartTimes != null) {
                    deviceStartTimeMap.putAll(savedStartTimes);
                    Log.d(TAG, "恢复运行中设备: " + deviceStartTimeMap.size() + "个");
                }
            } catch (Exception e) {
                Log.e(TAG, "恢复设备启动时间失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 更新正在运行设备的实时能耗
     * 用于实时显示当前运行设备的能耗
     */
    public void updateRunningDevicesEnergy() {
        long currentTime = System.currentTimeMillis();
        boolean needSave = false;
        
        for (Map.Entry<String, Long> entry : deviceStartTimeMap.entrySet()) {
            String deviceId = entry.getKey();
            Long startTime = entry.getValue();
            DeviceEnergy device = deviceEnergyMap.get(deviceId);
            
            if (device != null && startTime != null && device.isRunning()) {
                // 计算从启动到现在的临时能耗
                double tempUsageHours = (currentTime - startTime) / (1000.0 * 60 * 60);
                double tempEnergy = device.calculateEnergy(device.getTodayUsageHours() + tempUsageHours);
                
                // 更新今日记录
                updateTodayRecord(deviceId, tempEnergy);
                needSave = true;
            }
        }
        
        if (needSave) {
            saveData();
        }
    }
    
    /**
     * 获取设备当前实时能耗（包含正在运行的时间）
     */
    public double getDeviceRealtimeEnergy(String deviceId) {
        DeviceEnergy device = deviceEnergyMap.get(deviceId);
        if (device == null) return 0;
        
        double baseEnergy = device.getTodayEnergyKWh();
        
        // 如果设备正在运行，加上当前运行时间的能耗
        if (device.isRunning()) {
            Long startTime = deviceStartTimeMap.get(deviceId);
            if (startTime != null) {
                double runningHours = (System.currentTimeMillis() - startTime) / (1000.0 * 60 * 60);
                baseEnergy += device.calculateEnergy(runningHours);
            }
        }
        
        return baseEnergy;
    }
    
    /**
     * 获取设备当前实时使用时长（包含正在运行的时间）
     */
    public double getDeviceRealtimeUsageHours(String deviceId) {
        DeviceEnergy device = deviceEnergyMap.get(deviceId);
        if (device == null) return 0;
        
        double baseHours = device.getTodayUsageHours();
        
        // 如果设备正在运行，加上当前运行时间
        if (device.isRunning()) {
            Long startTime = deviceStartTimeMap.get(deviceId);
            if (startTime != null) {
                baseHours += (System.currentTimeMillis() - startTime) / (1000.0 * 60 * 60);
            }
        }
        
        return baseHours;
    }
    
    /**
     * 获取今日实时总耗电量（包含正在运行的设备）
     */
    public double getTodayRealtimeTotalEnergy() {
        updateRunningDevicesEnergy();
        double total = 0;
        for (String deviceId : deviceEnergyMap.keySet()) {
            total += getDeviceRealtimeEnergy(deviceId);
        }
        return total;
    }
    
    /**
     * 获取今日实时总使用时长（包含正在运行的设备）
     */
    public double getTodayRealtimeTotalUsageHours() {
        double total = 0;
        for (String deviceId : deviceEnergyMap.keySet()) {
            total += getDeviceRealtimeUsageHours(deviceId);
        }
        return total;
    }
}
