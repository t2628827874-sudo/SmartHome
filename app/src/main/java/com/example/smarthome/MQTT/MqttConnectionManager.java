package com.example.smarthome.MQTT;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT连接管理器（单例模式）
 * 
 * 功能说明：
 * - 管理所有MQTT设备连接，确保每个设备只有一个连接实例
 * - 避免重复连接导致的平台流控问题
 * - 提供连接状态监听和设备状态监听
 * 
 * 使用方式：
 * - 通过 getInstance() 获取单例实例
 * - 通过 getOrCreateManager() 获取或创建设备连接
 * - 通过 releaseManager() 释放不再使用的连接
 * 
 * 流控优化：
 * - 单例模式确保每个设备只有一个连接
 * - 增加重连间隔（30秒）
 * - 连接成功后延迟请求初始状态（2秒）
 * - 移除Paho库的自动重连，使用自定义重连机制
 */
public class MqttConnectionManager {
    private static final String TAG = "MqttConnectionManager";
    
    private static MqttConnectionManager instance;
    private final Context applicationContext;
    private final ConcurrentHashMap<String, MqttManagerWrapper> managers = new ConcurrentHashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private static final String SERVER_URI = "tcp://bj-2-mqtt.iot-api.com:1883";
    private static final int RECONNECT_DELAY_MS = 30000;     // 30秒重连间隔
    private static final int INITIAL_STATE_DELAY_MS = 2000;  // 连接成功后2秒再请求状态
    
    private MqttConnectionManager(Context context) {
        this.applicationContext = context.getApplicationContext();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized MqttConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttConnectionManager(context);
        }
        return instance;
    }
    
    /**
     * 获取或创建设备MQTT管理器
     * 
     * @param username 设备用户名（ThingsCloud设备ID）
     * @param password 设备密码
     * @param statusKey 状态属性键名
     * @return MqttManagerWrapper包装器
     */
    public MqttManagerWrapper getOrCreateManager(String username, String password, String statusKey) {
        String key = username + "_" + statusKey;
        
        MqttManagerWrapper wrapper = managers.get(key);
        if (wrapper != null) {
            Log.d(TAG, "复用现有连接: " + key);
            wrapper.retain();
            return wrapper;
        }
        
        Log.d(TAG, "创建新连接: " + key);
        wrapper = new MqttManagerWrapper(applicationContext, username, password, statusKey);
        managers.put(key, wrapper);
        wrapper.connect();
        
        return wrapper;
    }
    
    /**
     * 释放设备MQTT管理器
     * 
     * 当引用计数为0时才真正断开连接
     */
    public void releaseManager(String username, String statusKey) {
        String key = username + "_" + statusKey;
        MqttManagerWrapper wrapper = managers.get(key);
        if (wrapper != null) {
            if (wrapper.release()) {
                Log.d(TAG, "断开连接: " + key);
                managers.remove(key);
                wrapper.disconnect();
            }
        }
    }
    
    /**
     * MQTT管理器包装类
     * 
     * 实现引用计数，支持多个组件共享同一连接
     */
    public class MqttManagerWrapper {
        private final String username;
        private final String password;
        private final String statusKey;
        private final String clientId;
        private MqttAndroidClient client;
        private boolean isConnected = false;
        private Boolean lastState = null;
        private int referenceCount = 1;
        
        private MqttManager.OnStateChangeListener stateChangeListener;
        private MqttManager.OnConnectionListener connectionListener;
        private Runnable reconnectRunnable;
        private Runnable initialStateRunnable;
        
        private MqttManagerWrapper(Context context, String username, String password, String statusKey) {
            this.username = username;
            this.password = password;
            this.statusKey = statusKey;
            // 使用固定clientId，避免重复连接
            this.clientId = "Android_" + username;
            createClient(context);
        }
        
        private void createClient(Context context) {
            client = new MqttAndroidClient(context, SERVER_URI, clientId);
            Log.d(TAG, "[" + username + "] 创建MQTT客户端: " + clientId);
            
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    isConnected = true;
                    cancelReconnect();
                    Log.d(TAG, "[" + username + "] 连接成功! reconnect=" + reconnect);
                    
                    subscribe("attributes/push");
                    subscribe("attributes/get/response/+");
                    
                    // 延迟请求初始状态，避免触发流控
                    scheduleInitialStateRequest();
                    
                    if (connectionListener != null) {
                        handler.post(() -> connectionListener.onConnected());
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    isConnected = false;
                    Log.w(TAG, "[" + username + "] 连接丢失: " + (cause != null ? cause.getMessage() : "unknown"));
                    if (connectionListener != null) {
                        handler.post(() -> connectionListener.onDisconnected());
                    }
                    scheduleReconnect();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "[" + username + "] 收到消息: " + payload);
                    
                    try {
                        JSONObject object = new JSONObject(payload);
                        Boolean newState = null;
                        
                        if (object.has(statusKey)) {
                            newState = object.getBoolean(statusKey);
                        } else if (object.has("attributes")) {
                            JSONObject attrs = object.getJSONObject("attributes");
                            if (attrs.has(statusKey)) {
                                newState = attrs.getBoolean(statusKey);
                            }
                        }
                        
                        if (newState != null) {
                            lastState = newState;
                            Log.d(TAG, "[" + username + "] 状态更新: " + statusKey + "=" + newState);
                            if (stateChangeListener != null) {
                                Boolean finalState = newState;
                                handler.post(() -> stateChangeListener.onStateChanged(finalState));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[" + username + "] 解析消息失败: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
        }
        
        private void subscribe(String topic) {
            try {
                client.subscribe(topic, 0);
                Log.d(TAG, "[" + username + "] 订阅主题: " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "[" + username + "] 订阅失败: " + e.getMessage());
            }
        }
        
        /**
         * 延迟请求初始状态
         * 
         * 避免连接成功后立即请求导致流控
         */
        private void scheduleInitialStateRequest() {
            cancelInitialStateRequest();
            initialStateRunnable = () -> {
                if (isConnected && client != null) {
                    try {
                        String topic = "attributes/get/1000";
                        String payload = "{\"keys\":[\"" + statusKey + "\"]}";
                        Log.d(TAG, "[" + username + "] 请求初始状态: " + payload);
                        MqttMessage msg = new MqttMessage(payload.getBytes());
                        msg.setQos(0);
                        client.publish(topic, msg);
                    } catch (MqttException e) {
                        Log.e(TAG, "[" + username + "] 请求初始状态失败: " + e.getMessage());
                    }
                }
            };
            handler.postDelayed(initialStateRunnable, INITIAL_STATE_DELAY_MS);
        }
        
        private void cancelInitialStateRequest() {
            if (initialStateRunnable != null) {
                handler.removeCallbacks(initialStateRunnable);
                initialStateRunnable = null;
            }
        }
        
        public void connect() {
            Log.d(TAG, "[" + username + "] 开始连接...");
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            // 关闭Paho库的自动重连，使用自定义重连机制
            options.setAutomaticReconnect(false);
            options.setCleanSession(false);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);

            try {
                client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "[" + username + "] 连接请求成功");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        String error = exception != null ? exception.getMessage() : "Unknown error";
                        Log.e(TAG, "[" + username + "] 连接失败: " + error);
                        if (connectionListener != null) {
                            handler.post(() -> connectionListener.onConnectionFailed(error));
                        }
                        scheduleReconnect();
                    }
                });
            } catch (MqttException e) {
                Log.e(TAG, "[" + username + "] 连接异常: " + e.getMessage());
            }
        }
        
        /**
         * 调度重连
         * 
         * 使用较长的重连间隔（30秒），避免频繁重连触发流控
         */
        private void scheduleReconnect() {
            cancelReconnect();
            reconnectRunnable = () -> {
                if (!isConnected && referenceCount > 0) {
                    Log.d(TAG, "[" + username + "] 尝试重新连接...");
                    connect();
                }
            };
            handler.postDelayed(reconnectRunnable, RECONNECT_DELAY_MS);
        }
        
        private void cancelReconnect() {
            if (reconnectRunnable != null) {
                handler.removeCallbacks(reconnectRunnable);
                reconnectRunnable = null;
            }
        }
        
        public void disconnect() {
            Log.d(TAG, "[" + username + "] 断开连接");
            cancelReconnect();
            cancelInitialStateRequest();
            
            if (client != null && isConnected) {
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    Log.e(TAG, "[" + username + "] 断开连接失败: " + e.getMessage());
                }
            }
            isConnected = false;
        }
        
        public void publish(String property, Object value) {
            if (!isConnected || client == null) {
                Log.w(TAG, "[" + username + "] 无法发布: 未连接");
                return;
            }
            
            String payload = "{\"" + property + "\":" + value + "}";
            Log.d(TAG, "[" + username + "] 发布消息: " + payload);
            
            if (property != null && property.equals(statusKey) && value instanceof Boolean) {
                lastState = (Boolean) value;
            }
            
            try {
                MqttMessage msg = new MqttMessage(payload.getBytes());
                msg.setQos(1);
                client.publish("attributes", msg);
            } catch (MqttException e) {
                Log.e(TAG, "[" + username + "] 发布失败: " + e.getMessage());
            }
        }
        
        public Boolean getLastState() {
            return lastState;
        }
        
        public boolean isConnected() {
            return isConnected;
        }
        
        public void setOnStateChangeListener(MqttManager.OnStateChangeListener listener) {
            this.stateChangeListener = listener;
        }
        
        public void setOnConnectionListener(MqttManager.OnConnectionListener listener) {
            this.connectionListener = listener;
        }
        
        /**
         * 增加引用计数
         */
        private void retain() {
            referenceCount++;
            Log.d(TAG, "[" + username + "] 引用计数: " + referenceCount);
        }
        
        /**
         * 减少引用计数
         * @return true表示引用计数为0，可以断开连接
         */
        private boolean release() {
            referenceCount--;
            Log.d(TAG, "[" + username + "] 引用计数: " + referenceCount);
            return referenceCount <= 0;
        }
        
        public String getUsername() {
            return username;
        }
    }
}
