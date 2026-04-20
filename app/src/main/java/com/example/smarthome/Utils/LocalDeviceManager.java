package com.example.smarthome.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.smarthome.Service.EnergyManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地设备管理器
 * 管理本地模拟设备的状态，并与能耗统计系统集成
 */
public class LocalDeviceManager {
    private static final String TAG = "LocalDeviceManager";
    private static final String PREF_NAME = "local_device_states";
    private static LocalDeviceManager instance;
    private SharedPreferences preferences;
    private Map<String, DeviceState> deviceStates;
    private OnDeviceStateChangeListener stateChangeListener;
    private EnergyManager energyManager;

    public interface OnDeviceStateChangeListener {
        void onDeviceStateChanged(String deviceId, boolean newState);
    }

    public static class DeviceState {
        public String deviceId;
        public String deviceName;
        public String room;
        public boolean isOn;
        public long lastUpdateTime;

        public DeviceState(String deviceId, String deviceName, String room, boolean isOn) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.room = room;
            this.isOn = isOn;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    private LocalDeviceManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        deviceStates = new HashMap<>();
        energyManager = EnergyManager.getInstance(context);
        loadDeviceStates();
    }

    public static synchronized LocalDeviceManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDeviceManager(context);
        }
        return instance;
    }

    private void loadDeviceStates() {
        String json = preferences.getString("devices", "");
        if (!json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    String room = obj.getString("room");
                    boolean isOn = obj.getBoolean("isOn");
                    deviceStates.put(id, new DeviceState(id, name, room, isOn));
                }
                Log.d(TAG, "Loaded " + deviceStates.size() + " device states");
            } catch (JSONException e) {
                Log.e(TAG, "Error loading device states: " + e.getMessage());
                initDefaultDevices();
            }
        } else {
            initDefaultDevices();
        }
    }

    private void initDefaultDevices() {
        deviceStates.put("dining_fridge", new DeviceState("dining_fridge", "冰箱", "dining", false));
        deviceStates.put("dining_aircon", new DeviceState("dining_aircon", "空调", "dining", false));
        deviceStates.put("bedroom_curtain", new DeviceState("bedroom_curtain", "窗帘", "bedroom", false));
        deviceStates.put("bedroom_aircon", new DeviceState("bedroom_aircon", "空调", "bedroom", false));
        deviceStates.put("bedroom_dehumidifier", new DeviceState("bedroom_dehumidifier", "除湿器", "bedroom", false));
        deviceStates.put("bedroom_projector", new DeviceState("bedroom_projector", "投影仪", "bedroom", false));
        deviceStates.put("bedroom_speaker", new DeviceState("bedroom_speaker", "音响", "bedroom", false));
        deviceStates.put("bedroom_robot", new DeviceState("bedroom_robot", "扫地机器人", "bedroom", false));
        saveDeviceStates();
        Log.d(TAG, "Initialized default devices");
    }

    private void saveDeviceStates() {
        try {
            JSONArray array = new JSONArray();
            for (DeviceState state : deviceStates.values()) {
                JSONObject obj = new JSONObject();
                obj.put("id", state.deviceId);
                obj.put("name", state.deviceName);
                obj.put("room", state.room);
                obj.put("isOn", state.isOn);
                array.put(obj);
            }
            preferences.edit().putString("devices", array.toString()).apply();
            Log.d(TAG, "Saved " + deviceStates.size() + " device states");
        } catch (JSONException e) {
            Log.e(TAG, "Error saving device states: " + e.getMessage());
        }
    }

    public boolean getDeviceState(String deviceId) {
        DeviceState state = deviceStates.get(deviceId);
        return state != null && state.isOn;
    }

    public void setDeviceState(String deviceId, boolean isOn) {
        DeviceState state = deviceStates.get(deviceId);
        if (state != null) {
            state.isOn = isOn;
            state.lastUpdateTime = System.currentTimeMillis();
            saveDeviceStates();
            
            // 通知能耗管理器记录设备状态变化
            if (energyManager != null) {
                energyManager.onDeviceStateChanged(deviceId, isOn);
            }
            
            if (stateChangeListener != null) {
                stateChangeListener.onDeviceStateChanged(deviceId, isOn);
            }
            Log.d(TAG, "Device " + deviceId + " set to " + isOn);
        }
    }

    public int getOnCountByRoom(String room) {
        int count = 0;
        for (DeviceState state : deviceStates.values()) {
            if (state.room.equals(room) && state.isOn) {
                count++;
            }
        }
        return count;
    }

    public int getTotalOnCount() {
        int count = 0;
        for (DeviceState state : deviceStates.values()) {
            if (state.isOn) {
                count++;
            }
        }
        return count;
    }

    public int getDeviceCount() {
        return deviceStates.size();
    }

    public DeviceState getDevice(String deviceId) {
        return deviceStates.get(deviceId);
    }

    public void setOnDeviceStateChangeListener(OnDeviceStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public String getDeviceSubtitle(String deviceId) {
        DeviceState state = deviceStates.get(deviceId);
        if (state == null) return "未知状态";
        return state.isOn ? "已开启" : "已关闭";
    }

    public String getDeviceSubtitleByType(String deviceType) {
        int count = 0;
        for (DeviceState state : deviceStates.values()) {
            if (state.deviceName.equals(deviceType) && state.isOn) {
                count++;
            }
        }
        if (count == 0) {
            return "没有" + deviceType + "开启";
        } else if (count == 1) {
            return "一个" + deviceType + "开启";
        } else {
            return count + "个" + deviceType + "开启";
        }
    }
}
