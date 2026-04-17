package com.example.smarthome.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.Receiver.TimerTaskReceiver;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimerTaskManager {
    private static final String TAG = "TimerTaskManager";
    private static final String PREFS_NAME = "timer_tasks";
    private static final String KEY_TASKS = "tasks";
    private static final int MAX_TASKS = 10;
    
    private static TimerTaskManager instance;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final AlarmManager alarmManager;
    
    private TimerTaskManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    public static synchronized TimerTaskManager getInstance(Context context) {
        if (instance == null) {
            instance = new TimerTaskManager(context);
        }
        return instance;
    }
    
    public List<DeviceTimerTask> getAllTasks() {
        String json = sharedPreferences.getString(KEY_TASKS, "[]");
        Type type = new TypeToken<ArrayList<DeviceTimerTask>>(){}.getType();
        List<DeviceTimerTask> tasks = gson.fromJson(json, type);
        return tasks != null ? tasks : new ArrayList<>();
    }
    
    public List<DeviceTimerTask> getTasksForDevice(String deviceType) {
        List<DeviceTimerTask> allTasks = getAllTasks();
        List<DeviceTimerTask> deviceTasks = new ArrayList<>();
        for (DeviceTimerTask task : allTasks) {
            if (task.getDeviceType().equals(deviceType)) {
                deviceTasks.add(task);
            }
        }
        return deviceTasks;
    }
    
    public boolean addTask(DeviceTimerTask task) {
        List<DeviceTimerTask> tasks = getAllTasks();
        
        int deviceTaskCount = 0;
        for (DeviceTimerTask t : tasks) {
            if (t.getDeviceType().equals(task.getDeviceType())) {
                deviceTaskCount++;
            }
        }
        
        if (deviceTaskCount >= 5) {
            Log.w(TAG, "每个设备最多5个定时任务");
            return false;
        }
        
        if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
            task.setTaskId(UUID.randomUUID().toString());
        }
        
        tasks.add(task);
        saveTasks(tasks);
        scheduleTask(task);
        
        Log.d(TAG, "添加定时任务: " + task.getTaskId() + ", 设备: " + task.getDeviceName());
        return true;
    }
    
    public boolean updateTask(DeviceTimerTask task) {
        List<DeviceTimerTask> tasks = getAllTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskId().equals(task.getTaskId())) {
                cancelTask(tasks.get(i));
                tasks.set(i, task);
                saveTasks(tasks);
                if (task.isEnabled()) {
                    scheduleTask(task);
                }
                Log.d(TAG, "更新定时任务: " + task.getTaskId());
                return true;
            }
        }
        return false;
    }
    
    public boolean deleteTask(String taskId) {
        List<DeviceTimerTask> tasks = getAllTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskId().equals(taskId)) {
                cancelTask(tasks.get(i));
                tasks.remove(i);
                saveTasks(tasks);
                Log.d(TAG, "删除定时任务: " + taskId);
                return true;
            }
        }
        return false;
    }
    
    public void toggleTaskEnabled(String taskId, boolean enabled) {
        List<DeviceTimerTask> tasks = getAllTasks();
        for (DeviceTimerTask task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                task.setEnabled(enabled);
                if (enabled) {
                    scheduleTask(task);
                } else {
                    cancelTask(task);
                }
                saveTasks(tasks);
                Log.d(TAG, "切换任务状态: " + taskId + ", enabled: " + enabled);
                return;
            }
        }
    }
    
    private void saveTasks(List<DeviceTimerTask> tasks) {
        String json = gson.toJson(tasks);
        sharedPreferences.edit().putString(KEY_TASKS, json).apply();
    }
    
    private void scheduleTask(DeviceTimerTask task) {
        if (!task.isEnabled()) {
            return;
        }
        
        long triggerTime = task.getStartTimeMillis();
        
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "定时时间已过，不调度: " + task.getTaskId());
            return;
        }
        
        Intent startIntent = new Intent(context, TimerTaskReceiver.class);
        startIntent.setAction("START_TASK");
        startIntent.putExtra("taskId", task.getTaskId());
        startIntent.putExtra("deviceType", task.getDeviceType());
        startIntent.putExtra("durationMinutes", task.getDurationMinutes());
        
        PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                context,
                task.getTaskId().hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    startPendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    startPendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    startPendingIntent
            );
        }
        
        Log.d(TAG, "已调度启动任务: " + task.getTaskId() + ", 时间: " + task.getFormattedStartTime());
    }
    
    private void cancelTask(DeviceTimerTask task) {
        Intent startIntent = new Intent(context, TimerTaskReceiver.class);
        startIntent.setAction("START_TASK");
        
        PendingIntent startPendingIntent = PendingIntent.getBroadcast(
                context,
                task.getTaskId().hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(startPendingIntent);
        Log.d(TAG, "已取消任务: " + task.getTaskId());
    }
    
    public void rescheduleAllTasks() {
        List<DeviceTimerTask> tasks = getAllTasks();
        for (DeviceTimerTask task : tasks) {
            if (task.isEnabled() && task.getStartTimeMillis() > System.currentTimeMillis()) {
                scheduleTask(task);
            }
        }
        Log.d(TAG, "重新调度所有任务");
    }
    
    public void cleanupExpiredTasks() {
        List<DeviceTimerTask> tasks = getAllTasks();
        List<DeviceTimerTask> validTasks = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (DeviceTimerTask task : tasks) {
            if (task.getEndTimeMillis() > now) {
                validTasks.add(task);
            } else {
                cancelTask(task);
                Log.d(TAG, "清理过期任务: " + task.getTaskId());
            }
        }
        
        if (validTasks.size() != tasks.size()) {
            saveTasks(validTasks);
        }
    }
    
    public DeviceTimerTask getTaskById(String taskId) {
        List<DeviceTimerTask> tasks = getAllTasks();
        for (DeviceTimerTask task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }
}
