package com.example.smarthome.Receiver;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.R;
import com.example.smarthome.Service.TimerTaskManager;
import com.example.smarthome.Utils.LocalDeviceManager;

public class TimerTaskReceiver extends BroadcastReceiver {
    private static final String TAG = "TimerTaskReceiver";
    private static final String CHANNEL_ID = "timer_task_channel";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "收到广播: " + action);
        
        if ("START_TASK".equals(action)) {
            String taskId = intent.getStringExtra("taskId");
            String deviceType = intent.getStringExtra("deviceType");
            int durationMinutes = intent.getIntExtra("durationMinutes", 30);
            
            handleStartTask(context, taskId, deviceType, durationMinutes);
        } else if ("STOP_TASK".equals(action)) {
            String taskId = intent.getStringExtra("taskId");
            String deviceType = intent.getStringExtra("deviceType");
            
            handleStopTask(context, taskId, deviceType);
        }
    }
    
    private void handleStartTask(Context context, String taskId, String deviceType, int durationMinutes) {
        Log.d(TAG, "启动定时任务: " + taskId + ", 设备: " + deviceType + ", 时长: " + durationMinutes + "分钟");
        
        LocalDeviceManager deviceManager = LocalDeviceManager.getInstance(context);
        deviceManager.setDeviceState(deviceType, true);
        
        String deviceName = getDeviceDisplayName(deviceType);
        showNotification(context, "定时任务开始", deviceName + "已启动，将在" + durationMinutes + "分钟后自动关闭");
        
        scheduleStopTask(context, taskId, deviceType, durationMinutes);
    }
    
    private void handleStopTask(Context context, String taskId, String deviceType) {
        Log.d(TAG, "停止定时任务: " + taskId + ", 设备: " + deviceType);
        
        LocalDeviceManager deviceManager = LocalDeviceManager.getInstance(context);
        deviceManager.setDeviceState(deviceType, false);
        
        TimerTaskManager taskManager = TimerTaskManager.getInstance(context);
        taskManager.deleteTask(taskId);
        
        String deviceName = getDeviceDisplayName(deviceType);
        showNotification(context, "定时任务完成", deviceName + "已完成工作并自动关闭");
    }
    
    private void scheduleStopTask(Context context, String taskId, String deviceType, int durationMinutes) {
        long stopTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        Intent stopIntent = new Intent(context, TimerTaskReceiver.class);
        stopIntent.setAction("STOP_TASK");
        stopIntent.putExtra("taskId", taskId);
        stopIntent.putExtra("deviceType", deviceType);
        
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                context,
                (taskId + "_stop").hashCode(),
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    stopTime,
                    stopPendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    stopTime,
                    stopPendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    stopTime,
                    stopPendingIntent
            );
        }
        
        Log.d(TAG, "已调度停止任务: " + taskId + ", " + durationMinutes + "分钟后执行");
    }
    
    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "定时任务通知",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("设备定时任务执行通知");
            notificationManager.createNotificationChannel(channel);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    private String getDeviceDisplayName(String deviceType) {
        if (DeviceTimerTask.DEVICE_ROBOT.equals(deviceType)) {
            return "扫地机器人";
        } else if (DeviceTimerTask.DEVICE_DEHUMIDIFIER.equals(deviceType)) {
            return "除湿器";
        }
        return "设备";
    }
}
