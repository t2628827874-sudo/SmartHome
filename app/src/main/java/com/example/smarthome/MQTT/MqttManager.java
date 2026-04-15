package com.example.smarthome.MQTT;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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
    private MqttAndroidClient client;
    private final String serverUri = "tcp://bj-2-mqtt.iot-api.com:1883";
    private boolean isConnected = false;
    private Boolean lastState = null;
    private final String username;
    private final String password;
    private final String statusKey;
    private final String clientId = "Android_SmartHome_App_" + System.currentTimeMillis();
    private final Context context;
    
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
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
        createClient();
    }

    private void createClient() {
        client = new MqttAndroidClient(context.getApplicationContext(), serverUri, clientId);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                isConnected = true;
                cancelTimeout();
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
                if (connectionListener != null) {
                    handler.post(() -> connectionListener.onDisconnected());
                }
                scheduleReconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
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
                        if (stateChangeListener != null) {
                            Boolean finalNewState = newState;
                            handler.post(() -> stateChangeListener.onStateChanged(finalNewState));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void requestInitialState() {
        if (!isConnected || client == null) {
            return;
        }
        try {
            String topic = "attributes/get/1000";
            String payload = "{\"keys\":[\"" + statusKey + "\"]}";
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(0);
            client.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public Boolean getLastState() {
        return lastState;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect() {
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
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    isConnected = false;
                    cancelTimeout();
                    if (connectionListener != null) {
                        String error = exception != null ? exception.getMessage() : "Unknown error";
                        handler.post(() -> connectionListener.onConnectionFailed(error));
                    }
                    scheduleReconnect();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            if (connectionListener != null) {
                handler.post(() -> connectionListener.onConnectionFailed(e.getMessage()));
            }
        }
    }

    private void startTimeout() {
        cancelTimeout();
        timeoutRunnable = () -> {
            if (!isConnected && connectionListener != null) {
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

        if (property != null && property.equals(statusKey) && value instanceof Boolean) {
            lastState = (Boolean) value;
        }

        if (!isConnected || client == null) {
            return;
        }

        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            client.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        cancelReconnect();
        cancelTimeout();
        if (client != null && isConnected) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        isConnected = false;
    }
}
