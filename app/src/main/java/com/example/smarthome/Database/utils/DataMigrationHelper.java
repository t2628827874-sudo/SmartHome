package com.example.smarthome.Database.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.smarthome.Model.DailyEnergyRecord;
import com.example.smarthome.Model.DeviceEnergy;
import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.Database.connections.AppDatabase;
import com.example.smarthome.Database.models.DailyEnergyRecordEntity;
import com.example.smarthome.Database.models.DeviceEnergyEntity;
import com.example.smarthome.Database.models.DeviceTimerTaskEntity;
import com.example.smarthome.Database.models.ModeConfigEntity;
import com.example.smarthome.Database.models.UserConfigEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataMigrationHelper {
    
    private static final String TAG = "DataMigration";
    private static final String PREF_MIGRATION_DONE = "migration_done";
    
    private final Context context;
    private final AppDatabase database;
    private final Gson gson;
    private final ExecutorService executorService;
    
    public interface MigrationCallback {
        void onMigrationComplete(boolean success);
    }
    
    public DataMigrationHelper(Context context, AppDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.gson = new Gson();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    public void migrateIfNeeded(MigrationCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("migration_status", Context.MODE_PRIVATE);
        boolean migrationDone = prefs.getBoolean(PREF_MIGRATION_DONE, false);
        
        if (migrationDone) {
            Log.d(TAG, "数据迁移已完成，跳过");
            if (callback != null) {
                callback.onMigrationComplete(true);
            }
            return;
        }
        
        Log.d(TAG, "开始数据迁移...");
        executorService.execute(() -> {
            try {
                migrateAllData();
                
                prefs.edit().putBoolean(PREF_MIGRATION_DONE, true).apply();
                
                Log.d(TAG, "数据迁移完成");
                if (callback != null) {
                    callback.onMigrationComplete(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "数据迁移失败: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onMigrationComplete(false);
                }
            }
        });
    }
    
    private void migrateAllData() {
        migrateUserConfig();
        migrateDeviceEnergy();
        migrateDailyEnergyRecords();
        migrateTimerTasks();
        migrateModeConfig();
    }
    
    private void migrateUserConfig() {
        Log.d(TAG, "迁移用户配置数据...");
        
        SharedPreferences prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        
        UserConfigEntity entity = new UserConfigEntity();
        entity.setUserName(prefs.getString("user_name", null));
        entity.setHomeName(prefs.getString("home_name", null));
        entity.setThemeMode(prefs.getString("theme_mode", "light"));
        entity.setLanguage(prefs.getString("language", "zh"));
        entity.setNotificationEnabled(prefs.getBoolean("notification_enabled", true));
        entity.setAutoSync(prefs.getBoolean("auto_sync", false));
        
        if (entity.getUserName() != null || entity.getHomeName() != null) {
            database.userConfigDao().insert(entity);
            Log.d(TAG, "用户配置迁移完成");
        }
    }
    
    private void migrateDeviceEnergy() {
        Log.d(TAG, "迁移设备能耗数据...");
        
        SharedPreferences prefs = context.getSharedPreferences("energy_data", Context.MODE_PRIVATE);
        String deviceEnergyJson = prefs.getString("device_energy", null);
        
        if (deviceEnergyJson != null && !deviceEnergyJson.isEmpty()) {
            try {
                Type type = new TypeToken<Map<String, DeviceEnergy>>() {}.getType();
                Map<String, DeviceEnergy> deviceEnergyMap = gson.fromJson(deviceEnergyJson, type);
                
                if (deviceEnergyMap != null && !deviceEnergyMap.isEmpty()) {
                    List<DeviceEnergyEntity> entities = new ArrayList<>();
                    
                    for (Map.Entry<String, DeviceEnergy> entry : deviceEnergyMap.entrySet()) {
                        DeviceEnergy oldData = entry.getValue();
                        DeviceEnergyEntity entity = convertToEntity(oldData);
                        entities.add(entity);
                    }
                    
                    database.deviceEnergyDao().insertAll(entities);
                    Log.d(TAG, "设备能耗数据迁移完成，共 " + entities.size() + " 条");
                }
            } catch (Exception e) {
                Log.e(TAG, "设备能耗数据迁移失败: " + e.getMessage(), e);
            }
        }
    }
    
    private DeviceEnergyEntity convertToEntity(DeviceEnergy oldData) {
        DeviceEnergyEntity entity = new DeviceEnergyEntity();
        entity.setDeviceId(oldData.getDeviceId());
        entity.setDeviceName(oldData.getDeviceName());
        entity.setDeviceType(oldData.getDeviceType());
        entity.setPowerRating(oldData.getPowerWatts());
        entity.setTotalEnergy(oldData.getTodayEnergyKWh());
        entity.setTotalRunningTime((long)(oldData.getTodayUsageHours() * 3600000));
        entity.setRunning(oldData.isRunning());
        entity.setStartTime(0);
        return entity;
    }
    
    private void migrateDailyEnergyRecords() {
        Log.d(TAG, "迁移每日能耗记录...");
        
        SharedPreferences prefs = context.getSharedPreferences("energy_data", Context.MODE_PRIVATE);
        String dailyRecordsJson = prefs.getString("daily_records", null);
        
        if (dailyRecordsJson != null && !dailyRecordsJson.isEmpty()) {
            try {
                Type type = new TypeToken<List<DailyEnergyRecord>>() {}.getType();
                List<DailyEnergyRecord> records = gson.fromJson(dailyRecordsJson, type);
                
                if (records != null && !records.isEmpty()) {
                    List<DailyEnergyRecordEntity> entities = new ArrayList<>();
                    
                    for (DailyEnergyRecord record : records) {
                        DailyEnergyRecordEntity entity = convertToEntity(record);
                        entities.add(entity);
                    }
                    
                    database.dailyEnergyRecordDao().insertAll(entities);
                    Log.d(TAG, "每日能耗记录迁移完成，共 " + entities.size() + " 条");
                }
            } catch (Exception e) {
                Log.e(TAG, "每日能耗记录迁移失败: " + e.getMessage(), e);
            }
        }
    }
    
    private DailyEnergyRecordEntity convertToEntity(DailyEnergyRecord record) {
        DailyEnergyRecordEntity entity = new DailyEnergyRecordEntity();
        entity.setRecordDate(record.getDate());
        entity.setTotalEnergy(record.getTotalEnergyKWh());
        entity.setDeviceBreakdown(record.getDeviceEnergyMap());
        return entity;
    }
    
    private void migrateTimerTasks() {
        Log.d(TAG, "迁移定时任务数据...");
        
        SharedPreferences prefs = context.getSharedPreferences("timer_tasks", Context.MODE_PRIVATE);
        String tasksJson = prefs.getString("tasks_list", null);
        
        if (tasksJson != null && !tasksJson.isEmpty()) {
            try {
                Type type = new TypeToken<List<DeviceTimerTask>>() {}.getType();
                List<DeviceTimerTask> tasks = gson.fromJson(tasksJson, type);
                
                if (tasks != null && !tasks.isEmpty()) {
                    List<DeviceTimerTaskEntity> entities = new ArrayList<>();
                    
                    for (DeviceTimerTask task : tasks) {
                        DeviceTimerTaskEntity entity = convertToEntity(task);
                        entities.add(entity);
                    }
                    
                    database.deviceTimerTaskDao().insertAll(entities);
                    Log.d(TAG, "定时任务迁移完成，共 " + entities.size() + " 条");
                }
            } catch (Exception e) {
                Log.e(TAG, "定时任务迁移失败: " + e.getMessage(), e);
            }
        }
    }
    
    private DeviceTimerTaskEntity convertToEntity(DeviceTimerTask task) {
        DeviceTimerTaskEntity entity = new DeviceTimerTaskEntity();
        entity.setTaskId(task.getTaskId());
        entity.setDeviceId(task.getDeviceType());
        entity.setTaskName(task.getDeviceName());
        entity.setAction("on");
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        entity.setExecutionTime(sdf.format(new java.util.Date(task.getStartTimeMillis())));
        
        entity.setEnabled(task.isEnabled());
        entity.setRepeatPattern(task.isRepeating() ? "daily" : "once");
        return entity;
    }
    
    private void migrateModeConfig() {
        Log.d(TAG, "迁移模式配置数据...");
        
        SharedPreferences prefs = context.getSharedPreferences("mode_config", Context.MODE_PRIVATE);
        
        String currentMode = prefs.getString("current_mode", null);
        String homeModeDevices = prefs.getString("home_mode_devices", null);
        String awayModeDevices = prefs.getString("away_mode_devices", null);
        
        if (homeModeDevices != null && !homeModeDevices.isEmpty()) {
            try {
                ModeConfigEntity homeMode = new ModeConfigEntity();
                homeMode.setModeName("home");
                homeMode.setActive("home".equals(currentMode));
                
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> deviceConfig = gson.fromJson(homeModeDevices, type);
                homeMode.setDeviceConfiguration(deviceConfig);
                
                database.modeConfigDao().insert(homeMode);
                Log.d(TAG, "回家模式配置迁移完成");
            } catch (Exception e) {
                Log.e(TAG, "回家模式配置迁移失败: " + e.getMessage(), e);
            }
        }
        
        if (awayModeDevices != null && !awayModeDevices.isEmpty()) {
            try {
                ModeConfigEntity awayMode = new ModeConfigEntity();
                awayMode.setModeName("away");
                awayMode.setActive("away".equals(currentMode));
                
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> deviceConfig = gson.fromJson(awayModeDevices, type);
                awayMode.setDeviceConfiguration(deviceConfig);
                
                database.modeConfigDao().insert(awayMode);
                Log.d(TAG, "离家模式配置迁移完成");
            } catch (Exception e) {
                Log.e(TAG, "离家模式配置迁移失败: " + e.getMessage(), e);
            }
        }
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
