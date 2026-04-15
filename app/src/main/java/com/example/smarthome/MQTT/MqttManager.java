package com.example.smarthome.MQTT;

import android.content.Context;

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
    private MqttAndroidClient client;//客户端
    //private static MqttManager instance;//单例
    private final String serverUri="tcp://bj-2-mqtt.iot-api.com:1883";
    private boolean isConnected = false;//判断是否连接上tingscloud
    private Boolean lastState = null;
    private final String username;
    private final String password;
    private final String satusKey;//状态属性名字，不同的标识符不一样
    private final String clientId = "Android_SmartHome_App_" + System.currentTimeMillis();// APP名称 + 随机数或时间戳

    public MqttManager(Context context,String username,String password,String satusKey) {
        this.username = username;
        this.password = password;
        this.satusKey=satusKey;
        //创建客户端对象
        client=new MqttAndroidClient(context.getApplicationContext(),serverUri,clientId);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                //连接成功，发送订阅
                isConnected = true;
                subscribe("attributes/push");
                subscribe("attributes/get/response/+");
                //主动向 ThingsCloud 请求一次 state ,用来初始化开关
                requestInitialState();
            }
            //连接丢失
            @Override
            public void connectionLost(Throwable cause) {
                //连接失败
                isConnected=false;
            }
            //有数据到达客户端，解析，客户端收到消息
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload=new String(message.getPayload());
                JSONObject object=new JSONObject(payload);
                try {
                    if(object.has(satusKey)){
                        lastState=object.getBoolean(satusKey);
                    } else if (object.has("attributes")) {
                        // attributes/get 响应里，state 在 attributes 里面
                        JSONObject attrs = object.getJSONObject("attributes");
                        if (attrs.has(satusKey)) {
                            lastState = attrs.getBoolean(satusKey);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            //消息发送完成
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    private void subscribe(String topic){
        try {
            client.subscribe(topic,0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    //主动发一次请求
    private void requestInitialState(){
        if (!isConnected || client == null) {
            return;
        }
        try {
            String topic = "attributes/get/1000";
            String payload = "{\"keys\":[\"" + satusKey + "\"]}";
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(0);
            client.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
//获取最后的state
    public Boolean getLastState(){
        return lastState;
    }


    //获取单例，全应用只有一个MQTT连接
//    public static synchronized MqttManager getInstance(Context context){
//        if(instance==null){
//            instance=new MqttManager(context);
//        }
//        return instance;
//    }
    //执行连接
    public void connect(){
        MqttConnectOptions options=new MqttConnectOptions();

        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(false);//- - 断线后客户端会自动尝试重连。
        options.setCleanSession(false);

        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // 这里才算真正连上，可以订阅或者上报数据
                    isConnected=true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // 连接失败，看看 exception
                    isConnected=false;
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }
    // 发送指令，客户端给服务器发送
    public void publish(String property, Object value) {
        String topic = "attributes";
        String payload = "{\"" + property + "\":" + value + "}";

        // 如果是 satusKey 并且是 Boolean，本地也先记一下
        if (property != null && property.equals(satusKey) && value instanceof Boolean) {
            lastState = (Boolean) value;
        }

        // 如果还没连上服务器，就先不发，避免消息丢失
        if (!isConnected || client == null) {
            return;
        }

        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1); // 至少一次送达
            client.publish(topic, msg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
