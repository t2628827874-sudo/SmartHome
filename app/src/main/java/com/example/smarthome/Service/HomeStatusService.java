package com.example.smarthome.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.example.smarthome.Activities.HomeActivity;
import com.example.smarthome.R;
import com.example.smarthome.Utils.LocalDeviceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HomeStatusService extends Service {

    private static final String TAG = "HomeStatusService";
    private static final String CHANNEL_ID = "home_status_channel";
    private static final String CHANNEL_NAME = "智能家居状态";
    private static final int NOTIFICATION_ID = 1001;

    private static final long UPDATE_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long MIN_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30);

    private static boolean isRunning = false;

    private NotificationManager notificationManager;
    private LocalDeviceManager deviceManager;
    private Handler handler;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateNotification();
            scheduleNextUpdate();
        }
    };

    public static void startService(Context context) {
        if (isRunning) {
            Log.d(TAG, "Service already running");
            return;
        }
        Intent intent = new Intent(context, HomeStatusService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, HomeStatusService.class);
        context.stopService(intent);
    }

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        isRunning = true;
        handler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        deviceManager = LocalDeviceManager.getInstance(this);

        createNotificationChannel();
        startForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        updateNotification();
        scheduleNextUpdate();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        isRunning = false;
        handler.removeCallbacks(updateRunnable);
        releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("智能家居设备状态通知");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startForeground() {
        Notification notification = buildNotification(getDeviceStatusData());
        int foregroundServiceType = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void scheduleNextUpdate() {
        handler.removeCallbacks(updateRunnable);

        boolean isScreenOn = powerManager.isInteractive();
        long interval = isScreenOn ? MIN_UPDATE_INTERVAL_MS : UPDATE_INTERVAL_MS;

        Log.d(TAG, "下次更新间隔: " + (interval / 1000) + "秒, 屏幕状态: " + (isScreenOn ? "亮" : "灭"));
        handler.postDelayed(updateRunnable, interval);
    }

    private void updateNotification() {
        try {
            DeviceStatusData data = getDeviceStatusData();
            Notification notification = buildNotification(data);
            notificationManager.notify(NOTIFICATION_ID, notification);
            Log.d(TAG, "通知已更新: " + data.totalOnCount + "个设备开启");
        } catch (Exception e) {
            Log.e(TAG, "更新通知失败: " + e.getMessage(), e);
            showErrorNotification();
        }
    }

    private DeviceStatusData getDeviceStatusData() {
        DeviceStatusData data = new DeviceStatusData();

        try {
            data.totalOnCount = deviceManager.getTotalOnCount();

            data.devicesByRoom = new HashMap<>();
            data.devicesByRoom.put("客厅", getOnDevicesByRoom("dining"));
            data.devicesByRoom.put("卧室", getOnDevicesByRoom("bedroom"));

            data.deviceTypeCounts = new HashMap<>();
            data.deviceTypeCounts.put("空调", getOnCountByDeviceName("空调"));
            data.deviceTypeCounts.put("灯", getOnCountByDeviceName("灯"));

        } catch (Exception e) {
            Log.e(TAG, "获取设备状态失败: " + e.getMessage(), e);
        }

        return data;
    }

    private List<String> getOnDevicesByRoom(String room) {
        List<String> devices = new ArrayList<>();
        try {
            Map<String, LocalDeviceManager.DeviceState> allDevices = getAllDeviceStates();
            for (LocalDeviceManager.DeviceState state : allDevices.values()) {
                if (state.room.equals(room) && state.isOn) {
                    devices.add(state.deviceName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取房间设备失败: " + e.getMessage());
        }
        return devices;
    }

    private int getOnCountByDeviceName(String deviceName) {
        int count = 0;
        try {
            Map<String, LocalDeviceManager.DeviceState> allDevices = getAllDeviceStates();
            for (LocalDeviceManager.DeviceState state : allDevices.values()) {
                if (state.deviceName.equals(deviceName) && state.isOn) {
                    count++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取设备类型数量失败: " + e.getMessage());
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private Map<String, LocalDeviceManager.DeviceState> getAllDeviceStates() {
        try {
            java.lang.reflect.Field field = LocalDeviceManager.class.getDeclaredField("deviceStates");
            field.setAccessible(true);
            return (Map<String, LocalDeviceManager.DeviceState>) field.get(deviceManager);
        } catch (Exception e) {
            Log.e(TAG, "获取设备状态Map失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Notification buildNotification(DeviceStatusData data) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        String contentTitle = "智能家居状态";
        String contentText = buildContentText(data);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(contentTitle);
        bigTextStyle.bigText(buildBigText(data));
        builder.setStyle(bigTextStyle);

        return builder.build();
    }

    private String buildContentText(DeviceStatusData data) {
        if (data.totalOnCount == 0) {
            return "所有设备已关闭";
        }
        return data.totalOnCount + "个设备运行中";
    }

    private String buildBigText(DeviceStatusData data) {
        StringBuilder sb = new StringBuilder();

        if (data.totalOnCount == 0) {
            sb.append("所有设备已关闭");
        } else {
            sb.append("共 ").append(data.totalOnCount).append(" 个设备运行中\n\n");

            if (data.devicesByRoom != null) {
                for (Map.Entry<String, List<String>> entry : data.devicesByRoom.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        sb.append("【").append(entry.getKey()).append("】\n");
                        for (String device : entry.getValue()) {
                            sb.append("  • ").append(device).append("\n");
                        }
                    }
                }
            }

            if (data.deviceTypeCounts != null && !data.deviceTypeCounts.isEmpty()) {
                sb.append("\n【设备统计】\n");
                for (Map.Entry<String, Integer> entry : data.deviceTypeCounts.entrySet()) {
                    if (entry.getValue() > 0) {
                        sb.append("  • ").append(entry.getKey()).append(": ")
                                .append(entry.getValue()).append("个\n");
                    }
                }
            }
        }

        return sb.toString().trim();
    }

    private void showErrorNotification() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home)
                .setContentTitle("智能家居状态")
                .setContentText("无法获取设备状态")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmartHome:HomeStatusService"
            );
            wakeLock.setReferenceCounted(false);
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(TimeUnit.MINUTES.toMillis(1));
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private static class DeviceStatusData {
        int totalOnCount;
        Map<String, List<String>> devicesByRoom;
        Map<String, Integer> deviceTypeCounts;
    }
}
