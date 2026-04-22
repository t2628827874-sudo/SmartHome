package com.example.smarthome.Database.transactions;

import android.content.Context;
import android.util.Log;

import com.example.smarthome.Database.connections.AppDatabase;
import com.example.smarthome.Database.models.DailyEnergyRecordEntity;
import com.example.smarthome.Database.models.DeviceEnergyEntity;
import com.example.smarthome.Database.models.DeviceTimerTaskEntity;
import com.example.smarthome.Database.models.ModeConfigEntity;
import com.example.smarthome.Database.models.UserConfigEntity;
import com.example.smarthome.Database.queries.DailyEnergyRecordDao;
import com.example.smarthome.Database.queries.DeviceEnergyDao;
import com.example.smarthome.Database.queries.DeviceTimerTaskDao;
import com.example.smarthome.Database.queries.ModeConfigDao;
import com.example.smarthome.Database.queries.UserConfigDao;
import com.example.smarthome.Database.utils.DataMigrationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartHomeRepository {
    
    private static final String TAG = "SmartHomeRepository";
    
    private final AppDatabase database;
    private final UserConfigDao userConfigDao;
    private final DeviceEnergyDao deviceEnergyDao;
    private final DailyEnergyRecordDao dailyEnergyRecordDao;
    private final DeviceTimerTaskDao deviceTimerTaskDao;
    private final ModeConfigDao modeConfigDao;
    private final ExecutorService executorService;
    
    private static SmartHomeRepository instance;
    
    public static synchronized SmartHomeRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SmartHomeRepository(context);
        }
        return instance;
    }
    
    private SmartHomeRepository(Context context) {
        database = AppDatabase.getInstance(context);
        userConfigDao = database.userConfigDao();
        deviceEnergyDao = database.deviceEnergyDao();
        dailyEnergyRecordDao = database.dailyEnergyRecordDao();
        deviceTimerTaskDao = database.deviceTimerTaskDao();
        modeConfigDao = database.modeConfigDao();
        executorService = Executors.newFixedThreadPool(4);
        
        performMigration(context);
    }
    
    private void performMigration(Context context) {
        DataMigrationHelper migrationHelper = new DataMigrationHelper(context, database);
        migrationHelper.migrateIfNeeded(success -> {
            if (success) {
                Log.d(TAG, "数据迁移成功");
            } else {
                Log.e(TAG, "数据迁移失败");
            }
        });
    }
    
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
    
    public void getUserConfig(DataCallback<UserConfigEntity> callback) {
        executorService.execute(() -> {
            try {
                UserConfigEntity config = userConfigDao.getFirst();
                notifySuccess(callback, config);
            } catch (Exception e) {
                Log.e(TAG, "获取用户配置失败: " + e.getMessage(), e);
                notifyError(callback, "获取用户配置失败: " + e.getMessage());
            }
        });
    }
    
    public void saveUserConfig(UserConfigEntity config, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = userConfigDao.insert(config);
                notifySuccess(callback, id);
            } catch (Exception e) {
                Log.e(TAG, "保存用户配置失败: " + e.getMessage(), e);
                notifyError(callback, "保存用户配置失败: " + e.getMessage());
            }
        });
    }
    
    public void getAllDevices(DataCallback<List<DeviceEnergyEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<DeviceEnergyEntity> devices = deviceEnergyDao.getAll();
                notifySuccess(callback, devices);
            } catch (Exception e) {
                Log.e(TAG, "获取设备列表失败: " + e.getMessage(), e);
                notifyError(callback, "获取设备列表失败: " + e.getMessage());
            }
        });
    }
    
    public void getDeviceByDeviceId(String deviceId, DataCallback<DeviceEnergyEntity> callback) {
        executorService.execute(() -> {
            try {
                DeviceEnergyEntity device = deviceEnergyDao.getByDeviceId(deviceId);
                notifySuccess(callback, device);
            } catch (Exception e) {
                Log.e(TAG, "获取设备失败: " + e.getMessage(), e);
                notifyError(callback, "获取设备失败: " + e.getMessage());
            }
        });
    }
    
    public void saveDevice(DeviceEnergyEntity device, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = deviceEnergyDao.insert(device);
                notifySuccess(callback, id);
            } catch (Exception e) {
                Log.e(TAG, "保存设备失败: " + e.getMessage(), e);
                notifyError(callback, "保存设备失败: " + e.getMessage());
            }
        });
    }
    
    public void updateDevice(DeviceEnergyEntity device, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = deviceEnergyDao.update(device);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "更新设备失败: " + e.getMessage(), e);
                notifyError(callback, "更新设备失败: " + e.getMessage());
            }
        });
    }
    
    public void deleteDevice(String deviceId, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = deviceEnergyDao.deleteByDeviceId(deviceId);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "删除设备失败: " + e.getMessage(), e);
                notifyError(callback, "删除设备失败: " + e.getMessage());
            }
        });
    }
    
    public void getDevicesOrderByEnergy(DataCallback<List<DeviceEnergyEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<DeviceEnergyEntity> devices = deviceEnergyDao.getAllOrderByEnergyDesc();
                notifySuccess(callback, devices);
            } catch (Exception e) {
                Log.e(TAG, "获取设备列表失败: " + e.getMessage(), e);
                notifyError(callback, "获取设备列表失败: " + e.getMessage());
            }
        });
    }
    
    public void getRunningDevices(DataCallback<List<DeviceEnergyEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<DeviceEnergyEntity> devices = deviceEnergyDao.getAllRunning();
                notifySuccess(callback, devices);
            } catch (Exception e) {
                Log.e(TAG, "获取运行设备失败: " + e.getMessage(), e);
                notifyError(callback, "获取运行设备失败: " + e.getMessage());
            }
        });
    }
    
    public void updateDeviceRunningStatus(String deviceId, boolean running, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                long startTime = running ? System.currentTimeMillis() : 0;
                int rows = deviceEnergyDao.updateRunningStatus(deviceId, running, startTime);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "更新设备状态失败: " + e.getMessage(), e);
                notifyError(callback, "更新设备状态失败: " + e.getMessage());
            }
        });
    }
    
    public void addDeviceEnergy(String deviceId, double energy, long runningTime, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = deviceEnergyDao.addEnergyAndTime(deviceId, energy, runningTime);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "添加能耗数据失败: " + e.getMessage(), e);
                notifyError(callback, "添加能耗数据失败: " + e.getMessage());
            }
        });
    }
    
    public void getTodayEnergyRecord(DataCallback<DailyEnergyRecordEntity> callback) {
        executorService.execute(() -> {
            try {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                DailyEnergyRecordEntity record = dailyEnergyRecordDao.getByDate(today);
                notifySuccess(callback, record);
            } catch (Exception e) {
                Log.e(TAG, "获取今日能耗记录失败: " + e.getMessage(), e);
                notifyError(callback, "获取今日能耗记录失败: " + e.getMessage());
            }
        });
    }
    
    public void saveDailyEnergyRecord(DailyEnergyRecordEntity record, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = dailyEnergyRecordDao.insert(record);
                notifySuccess(callback, id);
            } catch (Exception e) {
                Log.e(TAG, "保存每日能耗记录失败: " + e.getMessage(), e);
                notifyError(callback, "保存每日能耗记录失败: " + e.getMessage());
            }
        });
    }
    
    public void getEnergyRecordsByDateRange(String startDate, String endDate, DataCallback<List<DailyEnergyRecordEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<DailyEnergyRecordEntity> records = dailyEnergyRecordDao.getByDateRange(startDate, endDate);
                notifySuccess(callback, records);
            } catch (Exception e) {
                Log.e(TAG, "获取能耗记录失败: " + e.getMessage(), e);
                notifyError(callback, "获取能耗记录失败: " + e.getMessage());
            }
        });
    }
    
    public void getAllTimerTasks(DataCallback<List<DeviceTimerTaskEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<DeviceTimerTaskEntity> tasks = deviceTimerTaskDao.getAllOrderByTime();
                notifySuccess(callback, tasks);
            } catch (Exception e) {
                Log.e(TAG, "获取定时任务失败: " + e.getMessage(), e);
                notifyError(callback, "获取定时任务失败: " + e.getMessage());
            }
        });
    }
    
    public void getEnabledTimerTasks(DataCallback<List<DeviceTimerTaskEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<DeviceTimerTaskEntity> tasks = deviceTimerTaskDao.getAllEnabled();
                notifySuccess(callback, tasks);
            } catch (Exception e) {
                Log.e(TAG, "获取启用的定时任务失败: " + e.getMessage(), e);
                notifyError(callback, "获取启用的定时任务失败: " + e.getMessage());
            }
        });
    }
    
    public void saveTimerTask(DeviceTimerTaskEntity task, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = deviceTimerTaskDao.insert(task);
                notifySuccess(callback, id);
            } catch (Exception e) {
                Log.e(TAG, "保存定时任务失败: " + e.getMessage(), e);
                notifyError(callback, "保存定时任务失败: " + e.getMessage());
            }
        });
    }
    
    public void deleteTimerTask(String taskId, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = deviceTimerTaskDao.deleteByTaskId(taskId);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "删除定时任务失败: " + e.getMessage(), e);
                notifyError(callback, "删除定时任务失败: " + e.getMessage());
            }
        });
    }
    
    public void updateTimerTaskEnabled(String taskId, boolean enabled, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                int rows = deviceTimerTaskDao.updateEnabled(taskId, enabled);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "更新定时任务状态失败: " + e.getMessage(), e);
                notifyError(callback, "更新定时任务状态失败: " + e.getMessage());
            }
        });
    }
    
    public void getActiveMode(DataCallback<ModeConfigEntity> callback) {
        executorService.execute(() -> {
            try {
                ModeConfigEntity mode = modeConfigDao.getActiveMode();
                notifySuccess(callback, mode);
            } catch (Exception e) {
                Log.e(TAG, "获取活动模式失败: " + e.getMessage(), e);
                notifyError(callback, "获取活动模式失败: " + e.getMessage());
            }
        });
    }
    
    public void getModeByName(String modeName, DataCallback<ModeConfigEntity> callback) {
        executorService.execute(() -> {
            try {
                ModeConfigEntity mode = modeConfigDao.getByModeName(modeName);
                notifySuccess(callback, mode);
            } catch (Exception e) {
                Log.e(TAG, "获取模式失败: " + e.getMessage(), e);
                notifyError(callback, "获取模式失败: " + e.getMessage());
            }
        });
    }
    
    public void saveModeConfig(ModeConfigEntity mode, DataCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = modeConfigDao.insert(mode);
                notifySuccess(callback, id);
            } catch (Exception e) {
                Log.e(TAG, "保存模式配置失败: " + e.getMessage(), e);
                notifyError(callback, "保存模式配置失败: " + e.getMessage());
            }
        });
    }
    
    public void activateMode(String modeName, DataCallback<Integer> callback) {
        executorService.execute(() -> {
            try {
                modeConfigDao.deactivateAll();
                int rows = modeConfigDao.activateMode(modeName);
                notifySuccess(callback, rows);
            } catch (Exception e) {
                Log.e(TAG, "激活模式失败: " + e.getMessage(), e);
                notifyError(callback, "激活模式失败: " + e.getMessage());
            }
        });
    }
    
    private <T> void notifySuccess(DataCallback<T> callback, T data) {
        if (callback != null) {
            callback.onSuccess(data);
        }
    }
    
    private void notifyError(DataCallback callback, String error) {
        if (callback != null) {
            callback.onError(error);
        }
    }
    
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
