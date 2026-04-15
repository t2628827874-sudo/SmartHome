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
import com.example.smarthome.MQTT.MqttManager;
import com.example.smarthome.Model.DeviceCardModel;
import com.example.smarthome.R;
import com.example.smarthome.Utils.LocalDeviceManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private TextView home_username;
    private ImageButton home_btn_img;
    private TabLayout home_tablayout;
    private FrameLayout home_scroll_content;
    
    private MqttManager livingLedMqttManager;
    private MqttManager livingCameraMqttManager;
    private MqttManager diningLedMqttManager;
    private LocalDeviceManager localDeviceManager;

    private static final String TAG = "HomeFragment";
    
    private RecyclerViewAdapter allTabAdapter;
    private int currentTabPosition = 0;

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

    private void initManagers() {
        localDeviceManager = LocalDeviceManager.getInstance(requireContext());
        
        livingLedMqttManager = new MqttManager(
                requireContext(),
                "ch1hpsnpie8hxwuj",
                "odoscHX24A",
                "state"
        );
        livingLedMqttManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
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
        livingLedMqttManager.connect();

        livingCameraMqttManager = new MqttManager(
                requireContext(),
                "taclmu1x2gf4s5cx",
                "odoscHX24A",
                "camera_status"
        );
        livingCameraMqttManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
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
        livingCameraMqttManager.connect();

        diningLedMqttManager = new MqttManager(
                requireContext(),
                "irp8ltd5thuronmp",
                "odoscHX24A",
                "state"
        );
        diningLedMqttManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
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
        diningLedMqttManager.connect();
        
        localDeviceManager.setOnDeviceStateChangeListener((deviceId, newState) -> {
            if (currentTabPosition == 0 && allTabAdapter != null) {
                requireActivity().runOnUiThread(() -> updateAllTabStats());
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

    private void setupLivingTab(View root) {
        SwitchMaterial sw = root.findViewById(R.id.sw_toggle);
        SwitchMaterial sw2 = root.findViewById(R.id.sw_toggle2);
        
        if (sw != null) {
            Boolean lastState = livingLedMqttManager.getLastState();
            if (lastState != null) {
                sw.setChecked(lastState);
            }
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                livingLedMqttManager.publish("state", isChecked);
                if (currentTabPosition == 0) {
                    updateAllTabStats();
                }
            });
            
            livingLedMqttManager.setOnStateChangeListener(newState -> {
                if (sw != null && !sw.isChecked() != (newState != null && !newState)) {
                    if (newState != null) {
                        requireActivity().runOnUiThread(() -> sw.setChecked(newState));
                    }
                }
                if (currentTabPosition == 0) {
                    updateAllTabStats();
                }
            });
        }
        
        if (sw2 != null) {
            Boolean lastCameraState = livingCameraMqttManager.getLastState();
            if (lastCameraState != null) {
                sw2.setChecked(lastCameraState);
            }
            sw2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                livingCameraMqttManager.publish("camera_status", isChecked);
            });
            
            livingCameraMqttManager.setOnStateChangeListener(newState -> {
                if (sw2 != null && newState != null) {
                    requireActivity().runOnUiThread(() -> sw2.setChecked(newState));
                }
            });
        }
    }

    private void setupDiningTab(View root) {
        SwitchMaterial swLed = root.findViewById(R.id.sw_toggle);
        SwitchMaterial swFridge = root.findViewById(R.id.sw_toggle2);
        SwitchMaterial swAircon = root.findViewById(R.id.sw_toggle3);
        
        if (swLed != null) {
            Boolean lastState = diningLedMqttManager.getLastState();
            if (lastState != null) {
                swLed.setChecked(lastState);
            }
            swLed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                diningLedMqttManager.publish("state", isChecked);
                if (currentTabPosition == 0) {
                    updateAllTabStats();
                }
            });
            
            diningLedMqttManager.setOnStateChangeListener(newState -> {
                if (swLed != null && newState != null) {
                    requireActivity().runOnUiThread(() -> swLed.setChecked(newState));
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

    private void setupBedroomTab(View root) {
        SwitchMaterial swDehumidifier = root.findViewById(R.id.sw_toggle);
        SwitchMaterial swAircon = root.findViewById(R.id.sw_toggle3);
        SwitchMaterial swCurtain = root.findViewById(R.id.sw_toggle4);
        SwitchMaterial swRobot = root.findViewById(R.id.sw_toggle5);
        SwitchMaterial swProjector = root.findViewById(R.id.sw_toggle6);
        SwitchMaterial swSpeaker = root.findViewById(R.id.sw_toggle7);
        
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

    private int getLightOnCount() {
        int count = 0;
        if (livingLedMqttManager != null && Boolean.TRUE.equals(livingLedMqttManager.getLastState())) {
            count++;
        }
        if (diningLedMqttManager != null && Boolean.TRUE.equals(diningLedMqttManager.getLastState())) {
            count++;
        }
        return count;
    }

    private int getCameraOnCount() {
        int count = 0;
        if (livingCameraMqttManager != null && Boolean.TRUE.equals(livingCameraMqttManager.getLastState())) {
            count++;
        }
        return count;
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
        if (count <= 0) {
            return "没有灯泡亮";
        } else if (count == 1) {
            return "一个灯泡亮";
        } else {
            return count + "个灯泡亮";
        }
    }

    private String formatCameraSubtitle(int count) {
        if (count <= 0) {
            return "没有摄像头工作";
        } else if (count == 1) {
            return "一个正在工作";
        } else {
            return count + "个正在工作";
        }
    }

    private String formatAirconSubtitle(int count) {
        if (count <= 0) {
            return "没有空调运行";
        } else if (count == 1) {
            return "一个空调运行";
        } else {
            return count + "个空调运行";
        }
    }

    private String formatCurtainSubtitle(int count) {
        if (count <= 0) {
            return "没有窗帘打开";
        } else if (count == 1) {
            return "一个窗帘打开";
        } else {
            return count + "个窗帘打开";
        }
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
    public void onDestroyView() {
        super.onDestroyView();
        if (livingLedMqttManager != null) {
            livingLedMqttManager.disconnect();
        }
        if (livingCameraMqttManager != null) {
            livingCameraMqttManager.disconnect();
        }
        if (diningLedMqttManager != null) {
            diningLedMqttManager.disconnect();
        }
    }

    public void logout(View v) {
    }
}
