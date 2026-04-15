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

public class MqttManager {
    private static final String TAG = "MqttManager";
    private MqttAndroidClient client;
    private final String serverUri = "tcp://bj-2-mqtt.iot-api.com:1883";
    private boolean isConnected = false;
    private Boolean lastState = null;
    private final String username;
    private final String password;
    private final String statusKey;
    private final String clientId;
    private final Context context;
    
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable reconnectRunnable;
    private Runnable timeoutRunnable;
    private OnStateChangeListener stateChangeListener;
    private OnConnectionListener connectionListener;

    public interface OnStateChangeListener {
        void onStateChanged(Boolean newState);
    }

    public interface OnConnectionListener {
        void onConnected();
        void onDisconnected();
        void onConnectionFailed(String error);
    }

    public MqttManager(Context context, String username, String password, String statusKey) {
        this.context = context;
        this.username = username;
        this.password = password;
        this.statusKey = statusKey;
        this.clientId = "Android_" + username + "_" + System.currentTimeMillis();
        Log.d(TAG, "创建MqttManager: username=" + username + ", statusKey=" + statusKey);
        createClient();
    }

    private void createClient() {
        client = new MqttAndroidClient(context.getApplicationContext(), serverUri, clientId);
        Log.d(TAG, "创建MQTT客户端: clientId=" + clientId);
        
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                isConnected = true;
                cancelTimeout();
                Log.d(TAG, "[" + username + "] 连接成功! reconnect=" + reconnect);
                subscribe("attributes/push");
                subscribe("attributes/get/response/+");
                requestInitialState();
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
                Log.d(TAG, "[" + username + "] 收到消息: topic=" + topic + ", payload=" + payload);
                
                JSONObject object = new JSONObject(payload);
                try {
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
                            Boolean finalNewState = newState;
                            handler.post(() -> stateChangeListener.onStateChanged(finalNewState));
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

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.stateChangeListener = listener;
    }

    public void setOnConnectionListener(OnConnectionListener listener) {
        this.connectionListener = listener;
    }

    private void subscribe(String topic) {
        try {
            client.subscribe(topic, 0);
            Log.d(TAG, "[" + username + "] 订阅主题: " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "[" + username + "] 订阅失败: " + e.getMessage());
        }
    }

    private void requestInitialState() {
        if (!isConnected || client == null) {
            Log.w(TAG, "[" + username + "] 无法请求初始状态: 未连接");
            return;
        }
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

    public Boolean getLastState() {
        return lastState;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect() {
        Log.d(TAG, "[" + username + "] 开始连接...");
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);

        startTimeout();

        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    isConnected = true;
                    Log.d(TAG, "[" + username + "] 连接请求成功");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    isConnected = false;
                    cancelTimeout();
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
            if (connectionListener != null) {
                handler.post(() -> connectionListener.onConnectionFailed(e.getMessage()));
            }
        }
    }

    private void startTimeout() {
        cancelTimeout();
        timeoutRunnable = () -> {
            if (!isConnected && connectionListener != null) {
                Log.w(TAG, "[" + username + "] 连接超时");
                connectionListener.onConnectionFailed("Connection timeout");
            }
        };
        handler.postDelayed(timeoutRunnable, CONNECTION_TIMEOUT_MS);
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    private void scheduleReconnect() {
        cancelReconnect();
        reconnectRunnable = () -> {
            if (!isConnected) {
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

    public void publish(String property, Object value) {
        String topic = "attributes";
        String payload = "{\"" + property + "\":" + value + "}";
        
        Log.d(TAG, "[" + username + "] 发布消息: " + payload);

        if (property != null && property.equals(statusKey) && value instanceof Boolean) {
            lastState = (Boolean) value;
        }

        if (!isConnected || client == null) {
            Log.w(TAG, "[" + username + "] 无法发布: 未连接");
            return;
        }

        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            client.publish(topic, msg);
        } catch (MqttException e) {
            Log.e(TAG, "[" + username + "] 发布失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        Log.d(TAG, "[" + username + "] 断开连接");
        cancelReconnect();
        cancelTimeout();
        if (client != null && isConnected) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                Log.e(TAG, "[" + username + "] 断开连接失败: " + e.getMessage());
            }
        }
        isConnected = false;
    }
}
