package com.example.smarthome.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smarthome.Adapter.RecyclerViewAdapter;
import com.example.smarthome.Dialog.TimerTaskDialog;
import com.example.smarthome.Dialog.TimerTaskListDialog;
import com.example.smarthome.MQTT.MqttConnectionManager;
import com.example.smarthome.MQTT.MqttManager;
import com.example.smarthome.Model.DeviceCardModel;
import com.example.smarthome.Model.DeviceTimerTask;
import com.example.smarthome.R;
import com.example.smarthome.Utils.LocalDeviceManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

/**
 * 首页Fragment
 * 
 * 功能说明：
 * - 显示全屋设备概览和各房间设备控制
 * - 支持客厅、餐厅、卧室三个房间的设备控制
 * - 与IntelligentFragment共享设备状态（通过MqttConnectionManager单例）
 * 
 * 设备控制：
 * - 客厅：灯光、摄像头（MQTT设备）
 * - 餐厅：灯光（MQTT）、冰箱、空调（本地设备）
 * - 卧室：除湿器、空调、窗帘、扫地机器人、投影仪、音响（本地设备）
 * 
 * 流控优化：
 * - 使用MqttConnectionManager单例管理MQTT连接
 * - 确保每个设备只有一个连接，避免重复连接触发平台流控
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    
    private TextView home_username;
    private ImageButton home_btn_img;
    private TabLayout home_tablayout;
    private FrameLayout home_scroll_content;
    
    private MqttConnectionManager.MqttManagerWrapper livingLedManager;
    private MqttConnectionManager.MqttManagerWrapper livingCameraManager;
    private MqttConnectionManager.MqttManagerWrapper diningLedManager;
    private LocalDeviceManager localDeviceManager;
    private MqttConnectionManager mqttConnectionManager;

    private RecyclerViewAdapter allTabAdapter;
    private int currentTabPosition = 0;
    
    private SwitchMaterial livingLedSwitch;
    private SwitchMaterial livingCameraSwitch;
    private SwitchMaterial diningLedSwitch;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);
        setUserName();
        initManagers();

        home_btn_img.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(requireContext(), v);
            popupMenu.getMenuInflater().inflate(R.menu.home_add, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_add_device) {
                    Toast.makeText(requireContext(), "添加设备", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popupMenu.show();
        });
        
        initTablayout();
        
        home_tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                switchContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        
        switchContent(0);
    }

    /**
     * 初始化管理器
     * 
     * 使用MqttConnectionManager单例获取MQTT连接，确保每个设备只有一个连接
     */
    private void initManagers() {
        localDeviceManager = LocalDeviceManager.getInstance(requireContext());
        mqttConnectionManager = MqttConnectionManager.getInstance(requireContext());
        
        // 客厅灯 - 使用单例管理器
        livingLedManager = mqttConnectionManager.getOrCreateManager(
                "ch1hpsnpie8hxwuj", "odoscHX24A", "state");
        livingLedManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "客厅灯连接成功");
            }
            @Override
            public void onDisconnected() {
                Log.w(TAG, "客厅灯连接断开");
            }
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "客厅灯连接失败: " + error);
            }
        });
        livingLedManager.setOnStateChangeListener(newState -> {
            Log.d(TAG, "客厅灯状态变化: " + newState);
            if (livingLedSwitch != null && getActivity() != null && newState != null) {
                getActivity().runOnUiThread(() -> livingLedSwitch.setChecked(newState));
            }
            if (currentTabPosition == 0 && allTabAdapter != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> updateAllTabStats());
            }
        });

        // 摄像头 - 使用单例管理器
        livingCameraManager = mqttConnectionManager.getOrCreateManager(
                "taclmu1x2gf4s5cx", "odoscHX24A", "camera_status");
        livingCameraManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "摄像头连接成功");
            }
            @Override
            public void onDisconnected() {
                Log.w(TAG, "摄像头连接断开");
            }
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "摄像头连接失败: " + error);
            }
        });
        livingCameraManager.setOnStateChangeListener(newState -> {
            Log.d(TAG, "摄像头状态变化: " + newState);
            if (livingCameraSwitch != null && getActivity() != null && newState != null) {
                getActivity().runOnUiThread(() -> livingCameraSwitch.setChecked(newState));
            }
            if (currentTabPosition == 0 && allTabAdapter != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> updateAllTabStats());
            }
        });

        // 餐厅灯 - 使用单例管理器
        diningLedManager = mqttConnectionManager.getOrCreateManager(
                "irp8ltd5thuronmp", "odoscHX24A", "state");
        diningLedManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "餐厅灯连接成功");
            }
            @Override
            public void onDisconnected() {
                Log.w(TAG, "餐厅灯连接断开");
            }
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "餐厅灯连接失败: " + error);
            }
        });
        diningLedManager.setOnStateChangeListener(newState -> {
            Log.d(TAG, "餐厅灯状态变化: " + newState);
            if (diningLedSwitch != null && getActivity() != null && newState != null) {
                getActivity().runOnUiThread(() -> diningLedSwitch.setChecked(newState));
            }
            if (currentTabPosition == 0 && allTabAdapter != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> updateAllTabStats());
            }
        });
        
        // 本地设备状态变化监听器
        localDeviceManager.setOnDeviceStateChangeListener((deviceId, newState) -> {
            if (currentTabPosition == 0 && allTabAdapter != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> updateAllTabStats());
            }
        });
    }

    @SuppressLint("MissingInflatedId")
    private void switchContent(int position) {
        Log.d(TAG, "switchContent position=" + position);
        if (home_scroll_content == null) return;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        home_scroll_content.removeAllViews();

        int layoutId;
        if (position == 0) {
            layoutId = R.layout.home_tab_all;
        } else if (position == 1) {
            layoutId = R.layout.home_tab_living;
        } else if (position == 2) {
            layoutId = R.layout.home_tab_dining;
        } else {
            layoutId = R.layout.home_tab_bedroom;
        }
        
        Log.d(TAG, "inflate layoutId=" + layoutId);
        View root = inflater.inflate(layoutId, home_scroll_content, false);
        home_scroll_content.addView(root);
        
        if (position == 0) {
            setupAllTab(root);
        } else if (position == 1) {
            setupLivingTab(root);
        } else if (position == 2) {
            setupDiningTab(root);
        } else if (position == 3) {
            setupBedroomTab(root);
        }
    }

    private void setupAllTab(View root) {
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        ArrayList<DeviceCardModel> recyclerViewList = new ArrayList<>();
        
        int lightOnCount = getLightOnCount();
        recyclerViewList.add(new DeviceCardModel(formatLightSubtitle(lightOnCount), "灯泡", R.drawable.light));
        
        int curtainOnCount = localDeviceManager.getDeviceState("bedroom_curtain") ? 1 : 0;
        recyclerViewList.add(new DeviceCardModel(formatCurtainSubtitle(curtainOnCount), "窗帘", R.drawable.curtain));
        
        recyclerViewList.add(new DeviceCardModel("温度湿度", "环境", R.drawable.circumstance));
        
        String robotStatus = localDeviceManager.getDeviceState("bedroom_robot") ? "正在工作" : "已停止";
        recyclerViewList.add(new DeviceCardModel(robotStatus, "扫地机器人", R.drawable.robot));
        
        int airconOnCount = getAirconOnCount();
        recyclerViewList.add(new DeviceCardModel(formatAirconSubtitle(airconOnCount), "空调", R.drawable.ic_aircon));
        
        int cameraOnCount = getCameraOnCount();
        recyclerViewList.add(new DeviceCardModel(formatCameraSubtitle(cameraOnCount), "摄像头", R.drawable.camera));
        
        String fridgeStatus = localDeviceManager.getDeviceState("dining_fridge") ? "运行中" : "已关闭";
        recyclerViewList.add(new DeviceCardModel(fridgeStatus, "冰箱", R.drawable.ic_fridge));
        
        String dehumidifierStatus = localDeviceManager.getDeviceState("bedroom_dehumidifier") ? "运行中" : "已关闭";
        recyclerViewList.add(new DeviceCardModel(dehumidifierStatus, "除湿器", R.drawable.ic_dehumidifier));
        
        String projectorStatus = localDeviceManager.getDeviceState("bedroom_projector") ? "运行中" : "已关闭";
        recyclerViewList.add(new DeviceCardModel(projectorStatus, "投影仪", R.drawable.ic_projector));
        
        String speakerStatus = localDeviceManager.getDeviceState("bedroom_speaker") ? "播放中" : "已暂停";
        recyclerViewList.add(new DeviceCardModel(speakerStatus, "音响", R.drawable.ic_speaker));

        allTabAdapter = new RecyclerViewAdapter(requireContext(), recyclerViewList);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(allTabAdapter);
    }

    private void updateAllTabStats() {
        if (allTabAdapter != null) {
            int lightOnCount = getLightOnCount();
            allTabAdapter.updateItem(0, formatLightSubtitle(lightOnCount));
            
            int curtainOnCount = localDeviceManager.getDeviceState("bedroom_curtain") ? 1 : 0;
            allTabAdapter.updateItem(1, formatCurtainSubtitle(curtainOnCount));
            
            String robotStatus = localDeviceManager.getDeviceState("bedroom_robot") ? "正在工作" : "已停止";
            allTabAdapter.updateItem(3, robotStatus);
            
            int airconOnCount = getAirconOnCount();
            allTabAdapter.updateItem(4, formatAirconSubtitle(airconOnCount));
            
            int cameraOnCount = getCameraOnCount();
            allTabAdapter.updateItem(5, formatCameraSubtitle(cameraOnCount));
            
            String fridgeStatus = localDeviceManager.getDeviceState("dining_fridge") ? "运行中" : "已关闭";
            allTabAdapter.updateItem(6, fridgeStatus);
            
            String dehumidifierStatus = localDeviceManager.getDeviceState("bedroom_dehumidifier") ? "运行中" : "已关闭";
            allTabAdapter.updateItem(7, dehumidifierStatus);
            
            String projectorStatus = localDeviceManager.getDeviceState("bedroom_projector") ? "运行中" : "已关闭";
            allTabAdapter.updateItem(8, projectorStatus);
            
            String speakerStatus = localDeviceManager.getDeviceState("bedroom_speaker") ? "播放中" : "已暂停";
            allTabAdapter.updateItem(9, speakerStatus);
        }
    }

    /**
     * 设置客厅标签页
     */
    private void setupLivingTab(View root) {
        livingLedSwitch = root.findViewById(R.id.sw_toggle);
        livingCameraSwitch = root.findViewById(R.id.sw_toggle2);
        
        if (livingLedSwitch != null && livingLedManager != null) {
            Boolean lastState = livingLedManager.getLastState();
            if (lastState != null) {
                livingLedSwitch.setChecked(lastState);
            }
            livingLedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "客厅灯开关手动切换: " + isChecked);
                if (livingLedManager != null) {
                    livingLedManager.publish("state", isChecked);
                }
                if (currentTabPosition == 0) {
                    updateAllTabStats();
                }
            });
        }
        
        if (livingCameraSwitch != null && livingCameraManager != null) {
            Boolean lastCameraState = livingCameraManager.getLastState();
            if (lastCameraState != null) {
                livingCameraSwitch.setChecked(lastCameraState);
            }
            livingCameraSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "摄像头开关手动切换: " + isChecked);
                if (livingCameraManager != null) {
                    livingCameraManager.publish("camera_status", isChecked);
                }
            });
        }
    }

    /**
     * 设置餐厅标签页
     */
    private void setupDiningTab(View root) {
        diningLedSwitch = root.findViewById(R.id.sw_toggle);
        SwitchMaterial swFridge = root.findViewById(R.id.sw_toggle2);
        SwitchMaterial swAircon = root.findViewById(R.id.sw_toggle3);
        
        if (diningLedSwitch != null && diningLedManager != null) {
            Boolean lastState = diningLedManager.getLastState();
            if (lastState != null) {
                diningLedSwitch.setChecked(lastState);
            }
            diningLedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "餐厅灯开关手动切换: " + isChecked);
                if (diningLedManager != null) {
                    diningLedManager.publish("state", isChecked);
                }
                if (currentTabPosition == 0) {
                    updateAllTabStats();
                }
            });
        }
        
        if (swFridge != null) {
            swFridge.setChecked(localDeviceManager.getDeviceState("dining_fridge"));
            swFridge.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("dining_fridge", isChecked);
            });
        }
        
        if (swAircon != null) {
            swAircon.setChecked(localDeviceManager.getDeviceState("dining_aircon"));
            swAircon.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("dining_aircon", isChecked);
            });
        }
    }

    /**
     * 设置卧室标签页
     */
    private void setupBedroomTab(View root) {
        SwitchMaterial swDehumidifier = root.findViewById(R.id.sw_toggle);
        SwitchMaterial swAircon = root.findViewById(R.id.sw_toggle3);
        SwitchMaterial swCurtain = root.findViewById(R.id.sw_toggle4);
        SwitchMaterial swRobot = root.findViewById(R.id.sw_toggle5);
        SwitchMaterial swProjector = root.findViewById(R.id.sw_toggle6);
        SwitchMaterial swSpeaker = root.findViewById(R.id.sw_toggle7);
        
        ImageButton btnTimerDehumidifier = root.findViewById(R.id.btn_timer_dehumidifier);
        ImageButton btnTimerRobot = root.findViewById(R.id.btn_timer_robot);
        
        if (btnTimerDehumidifier != null) {
            btnTimerDehumidifier.setOnClickListener(v -> {
                showTimerDialog(DeviceTimerTask.DEVICE_DEHUMIDIFIER, "除湿器");
            });
        }
        
        if (btnTimerRobot != null) {
            btnTimerRobot.setOnClickListener(v -> {
                showTimerDialog(DeviceTimerTask.DEVICE_ROBOT, "扫地机器人");
            });
        }
        
        if (swDehumidifier != null) {
            swDehumidifier.setChecked(localDeviceManager.getDeviceState("bedroom_dehumidifier"));
            swDehumidifier.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("bedroom_dehumidifier", isChecked);
            });
        }
        
        if (swAircon != null) {
            swAircon.setChecked(localDeviceManager.getDeviceState("bedroom_aircon"));
            swAircon.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("bedroom_aircon", isChecked);
            });
        }
        
        if (swCurtain != null) {
            swCurtain.setChecked(localDeviceManager.getDeviceState("bedroom_curtain"));
            swCurtain.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("bedroom_curtain", isChecked);
            });
        }
        
        if (swRobot != null) {
            swRobot.setChecked(localDeviceManager.getDeviceState("bedroom_robot"));
            swRobot.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("bedroom_robot", isChecked);
            });
        }
        
        if (swProjector != null) {
            swProjector.setChecked(localDeviceManager.getDeviceState("bedroom_projector"));
            swProjector.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("bedroom_projector", isChecked);
            });
        }
        
        if (swSpeaker != null) {
            swSpeaker.setChecked(localDeviceManager.getDeviceState("bedroom_speaker"));
            swSpeaker.setOnCheckedChangeListener((buttonView, isChecked) -> {
                localDeviceManager.setDeviceState("bedroom_speaker", isChecked);
            });
        }
    }
    
    private void showTimerDialog(String deviceType, String deviceName) {
        TimerTaskListDialog dialog = new TimerTaskListDialog(requireContext());
        dialog.show();
    }

    private int getLightOnCount() {
        int count = 0;
        if (livingLedManager != null && Boolean.TRUE.equals(livingLedManager.getLastState())) {
            count++;
        }
        if (diningLedManager != null && Boolean.TRUE.equals(diningLedManager.getLastState())) {
            count++;
        }
        return count;
    }

    private int getCameraOnCount() {
        if (livingCameraManager != null && Boolean.TRUE.equals(livingCameraManager.getLastState())) {
            return 1;
        }
        return 0;
    }

    private int getAirconOnCount() {
        int count = 0;
        if (localDeviceManager.getDeviceState("dining_aircon")) {
            count++;
        }
        if (localDeviceManager.getDeviceState("bedroom_aircon")) {
            count++;
        }
        return count;
    }

    private String formatLightSubtitle(int count) {
        if (count <= 0) return "没有灯泡亮";
        else if (count == 1) return "一个灯泡亮";
        else return count + "个灯泡亮";
    }

    private String formatCameraSubtitle(int count) {
        if (count <= 0) return "没有摄像头工作";
        else if (count == 1) return "一个正在工作";
        else return count + "个正在工作";
    }

    private String formatAirconSubtitle(int count) {
        if (count <= 0) return "没有空调运行";
        else if (count == 1) return "一个空调运行";
        else return count + "个空调运行";
    }

    private String formatCurtainSubtitle(int count) {
        if (count <= 0) return "没有窗帘打开";
        else if (count == 1) return "一个窗帘打开";
        else return count + "个窗帘打开";
    }

    private void initTablayout() {
        home_tablayout.removeAllTabs();
        String[] titles = {"全屋", "客厅", "餐厅", "卧室"};
        for (String title : titles) {
            home_tablayout.addTab(home_tablayout.newTab().setText(title));
        }
    }

    private void setUserName() {
        SharedPreferences sp = getActivity().getSharedPreferences("userinfo", MODE_PRIVATE);
        String currentAccount = sp.getString("current_account", "");
        String name = "name_" + currentAccount;
        String string = sp.getString(name, "");
        if (string.isEmpty()) {
            string = currentAccount;
        }
        String result = string + "的家";
        home_username.setText(result);
    }

    private void initView(View view) {
        home_username = view.findViewById(R.id.home_username);
        home_btn_img = view.findViewById(R.id.home_btn_img);
        home_tablayout = view.findViewById(R.id.home_tablayout);
        home_scroll_content = view.findViewById(R.id.home_scroll_content);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUserName();
        if (currentTabPosition == 0 && allTabAdapter != null) {
            updateAllTabStats();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 释放MQTT连接引用（不立即断开，由单例管理）
        if (mqttConnectionManager != null) {
            mqttConnectionManager.releaseManager("ch1hpsnpie8hxwuj", "state");
            mqttConnectionManager.releaseManager("taclmu1x2gf4s5cx", "camera_status");
            mqttConnectionManager.releaseManager("irp8ltd5thuronmp", "state");
        }
    }
}
