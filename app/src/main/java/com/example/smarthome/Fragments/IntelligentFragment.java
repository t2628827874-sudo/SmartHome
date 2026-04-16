package com.example.smarthome.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smarthome.MQTT.MqttConnectionManager;
import com.example.smarthome.MQTT.MqttManager;
import com.example.smarthome.R;
import com.example.smarthome.Utils.LocalDeviceManager;
import com.example.smarthome.Utils.ModeManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * 智能模式控制Fragment
 * 
 * 功能说明：
 * - 提供回家模式和离家模式的控制界面
 * - 回家模式：自动开启客厅灯光
 * - 离家模式：关闭所有电器，开启摄像头监控
 * 
 * 架构设计：
 * - 使用ModeManager管理模式状态（支持持久化）
 * - 使用MqttConnectionManager单例控制IoT设备（避免重复连接）
 * - 使用LocalDeviceManager管理本地模拟设备
 * 
 * 流控优化：
 * - 使用MqttConnectionManager单例管理MQTT连接
 * - 确保每个设备只有一个连接，避免重复连接触发平台流控
 */
public class IntelligentFragment extends Fragment {
    
    private static final String TAG = "IntelligentFragment";
    
    private TextView it_tv;
    private View cardCurrentMode;
    private View cardModeHome;
    private View cardModeAway;
    
    private TextView tvCurrentTitle;
    private TextView tvCurrentSubtitle;
    private ImageView ivCurrentIcon;
    
    private SwitchMaterial swHome;
    private SwitchMaterial swAway;
    private TextView tvHomeTitle;
    private TextView tvHomeSubtitle;
    private TextView tvAwayTitle;
    private TextView tvAwaySubtitle;
    
    private ModeManager modeManager;
    private LocalDeviceManager localDeviceManager;
    private MqttConnectionManager mqttConnectionManager;
    
    private MqttConnectionManager.MqttManagerWrapper livingLedManager;
    private MqttConnectionManager.MqttManagerWrapper diningLedManager;
    private MqttConnectionManager.MqttManagerWrapper cameraManager;
    
    private boolean isUpdatingSwitches = false;
    private boolean isProcessingMode = false;
    private boolean isFragmentActive = false;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_DELAY_MS = 800;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isFragmentActive = true;

        try {
            initManagers();
            initView(view);
            initName();
            initCardViews(view);
            initSwitchListeners();
            updateCurrentModeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated初始化失败: " + e.getMessage(), e);
            showToastSafe("初始化失败，请重试");
        }
    }

    /**
     * 初始化业务管理器
     * 
     * 使用MqttConnectionManager单例获取MQTT连接，确保每个设备只有一个连接
     */
    private void initManagers() {
        try {
            modeManager = ModeManager.getInstance(requireContext());
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

            // 摄像头 - 使用单例管理器
            cameraManager = mqttConnectionManager.getOrCreateManager(
                    "taclmu1x2gf4s5cx", "odoscHX24A", "camera_status");
            cameraManager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
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
            
            if (modeManager != null) {
                modeManager.setOnModeChangeListener(newMode -> {
                    if (getActivity() != null && isFragmentActive) {
                        getActivity().runOnUiThread(() -> {
                            if (isFragmentActive) {
                                updateCurrentModeDisplay();
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "initManagers失败: " + e.getMessage(), e);
        }
    }

    private void initCardViews(View view) {
        if (view == null) return;
        
        try {
            cardCurrentMode = view.findViewById(R.id.card_current_mode);
            cardModeHome = view.findViewById(R.id.card_mode_home);
            cardModeAway = view.findViewById(R.id.card_mode_away);

            if (cardCurrentMode != null) {
                tvCurrentTitle = cardCurrentMode.findViewById(R.id.tv_title);
                tvCurrentSubtitle = cardCurrentMode.findViewById(R.id.tv_subtitle);
                ivCurrentIcon = cardCurrentMode.findViewById(R.id.iv_icon);
                
                if (tvCurrentTitle != null) tvCurrentTitle.setText("当前模式");
                if (ivCurrentIcon != null) ivCurrentIcon.setImageResource(R.drawable.home);
            }

            if (cardModeHome != null) {
                tvHomeTitle = cardModeHome.findViewById(R.id.tv_title);
                tvHomeSubtitle = cardModeHome.findViewById(R.id.tv_subtitle);
                swHome = cardModeHome.findViewById(R.id.sw_toggle);
                
                if (tvHomeTitle != null) tvHomeTitle.setText("回家模式");
                if (tvHomeSubtitle != null) tvHomeSubtitle.setText("开启客厅灯光");
                if (swHome != null && modeManager != null) {
                    swHome.setChecked(modeManager.isHomeMode());
                }
            }

            if (cardModeAway != null) {
                tvAwayTitle = cardModeAway.findViewById(R.id.tv_title);
                tvAwaySubtitle = cardModeAway.findViewById(R.id.tv_subtitle);
                swAway = cardModeAway.findViewById(R.id.sw_toggle);
                
                if (tvAwayTitle != null) tvAwayTitle.setText("离家模式");
                if (tvAwaySubtitle != null) tvAwaySubtitle.setText("关闭电器，开启监控");
                if (swAway != null && modeManager != null) {
                    swAway.setChecked(modeManager.isAwayMode());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "initCardViews失败: " + e.getMessage(), e);
        }
    }

    private void initSwitchListeners() {
        try {
            if (swHome != null) {
                swHome.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isUpdatingSwitches || !isFragmentActive) return;
                        
                        try {
                            if (isChecked) {
                                if (modeManager != null && !modeManager.isHomeMode()) {
                                    activateHomeMode();
                                }
                            } else {
                                if (modeManager != null && modeManager.isHomeMode()) {
                                    deactivateCurrentMode();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "swHome切换失败: " + e.getMessage(), e);
                            resetSwitchStates();
                        }
                    }
                });
            }

            if (swAway != null) {
                swAway.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isUpdatingSwitches || !isFragmentActive) return;
                        
                        try {
                            if (isChecked) {
                                if (modeManager != null && !modeManager.isAwayMode()) {
                                    activateAwayMode();
                                }
                            } else {
                                if (modeManager != null && modeManager.isAwayMode()) {
                                    deactivateCurrentMode();
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "swAway切换失败: " + e.getMessage(), e);
                            resetSwitchStates();
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "initSwitchListeners失败: " + e.getMessage(), e);
        }
    }

    /**
     * 激活回家模式
     */
    private void activateHomeMode() {
        if (isProcessingMode || !isFragmentActive) {
            Log.w(TAG, "正在处理模式切换或Fragment不活跃，请稍候");
            resetSwitchStates();
            return;
        }
        
        isProcessingMode = true;
        Log.d(TAG, "激活回家模式");
        
        try {
            if (modeManager != null) {
                modeManager.setMode(ModeManager.MODE_HOME);
            }
            
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(true);
            if (swAway != null) swAway.setChecked(false);
            isUpdatingSwitches = false;
            
            // 发送设备控制命令 - 开启客厅灯
            if (livingLedManager != null) {
                livingLedManager.publish("state", true);
                Log.d(TAG, "客厅灯指令发送: state=true");
            }
            
            showToast("回家模式已激活");
            updateCurrentModeDisplay();
            
        } catch (Exception e) {
            Log.e(TAG, "激活回家模式失败: " + e.getMessage(), e);
            showToastSafe("激活失败，请重试");
            resetSwitchStates();
        } finally {
            handler.postDelayed(() -> {
                isProcessingMode = false;
                Log.d(TAG, "回家模式处理完成");
            }, DEBOUNCE_DELAY_MS);
        }
    }

    /**
     * 激活离家模式
     */
    private void activateAwayMode() {
        if (isProcessingMode || !isFragmentActive) {
            Log.w(TAG, "正在处理模式切换或Fragment不活跃，请稍候");
            resetSwitchStates();
            return;
        }
        
        isProcessingMode = true;
        Log.d(TAG, "激活离家模式");
        
        try {
            if (modeManager != null) {
                modeManager.setMode(ModeManager.MODE_AWAY);
            }
            
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(false);
            if (swAway != null) swAway.setChecked(true);
            isUpdatingSwitches = false;
            
            // 发送MQTT设备控制命令
            if (livingLedManager != null) {
                livingLedManager.publish("state", false);
                Log.d(TAG, "客厅灯指令发送: state=false");
            }
            if (diningLedManager != null) {
                diningLedManager.publish("state", false);
                Log.d(TAG, "餐厅灯指令发送: state=false");
            }
            if (cameraManager != null) {
                cameraManager.publish("camera_status", true);
                Log.d(TAG, "摄像头指令发送: camera_status=true");
            }
            
            // 关闭所有本地设备
            closeAllLocalDevices();
            
            showToast("离家模式已激活");
            updateCurrentModeDisplay();
            
        } catch (Exception e) {
            Log.e(TAG, "激活离家模式失败: " + e.getMessage(), e);
            showToastSafe("激活失败，请重试");
            resetSwitchStates();
        } finally {
            handler.postDelayed(() -> {
                isProcessingMode = false;
                Log.d(TAG, "离家模式处理完成");
            }, DEBOUNCE_DELAY_MS);
        }
    }

    /**
     * 关闭所有本地设备
     */
    private void closeAllLocalDevices() {
        if (localDeviceManager == null) {
            Log.w(TAG, "LocalDeviceManager为空，跳过本地设备关闭");
            return;
        }
        
        String[] deviceIds = {
            "dining_fridge", "dining_aircon", "bedroom_aircon",
            "bedroom_dehumidifier", "bedroom_curtain", "bedroom_robot",
            "bedroom_projector", "bedroom_speaker"
        };
        
        for (String deviceId : deviceIds) {
            try {
                localDeviceManager.setDeviceState(deviceId, false);
            } catch (Exception e) {
                Log.e(TAG, "关闭设备" + deviceId + "失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 取消当前模式
     */
    private void deactivateCurrentMode() {
        if (modeManager == null) return;
        
        try {
            modeManager.setMode(ModeManager.MODE_NONE);
            
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(false);
            if (swAway != null) swAway.setChecked(false);
            isUpdatingSwitches = false;
            
            showToast("已取消当前模式");
            updateCurrentModeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "取消模式失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重置开关状态到当前模式
     */
    private void resetSwitchStates() {
        if (!isFragmentActive || modeManager == null) return;
        
        try {
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(modeManager.isHomeMode());
            if (swAway != null) swAway.setChecked(modeManager.isAwayMode());
            isUpdatingSwitches = false;
        } catch (Exception e) {
            Log.e(TAG, "重置开关状态失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新当前模式显示
     */
    private void updateCurrentModeDisplay() {
        if (tvCurrentSubtitle == null || modeManager == null) return;
        
        try {
            int currentMode = modeManager.getCurrentMode();
            String modeText;
            int iconRes;
            
            switch (currentMode) {
                case ModeManager.MODE_HOME:
                    modeText = "回家模式";
                    iconRes = R.drawable.home;
                    break;
                case ModeManager.MODE_AWAY:
                    modeText = "离家模式";
                    iconRes = R.drawable.away;
                    break;
                default:
                    modeText = "无模式";
                    iconRes = R.drawable.home;
                    break;
            }
            
            tvCurrentSubtitle.setText(modeText);
            if (ivCurrentIcon != null) {
                ivCurrentIcon.setImageResource(iconRes);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新模式显示失败: " + e.getMessage(), e);
        }
    }

    private void initView(View view) {
        it_tv = view.findViewById(R.id.it_tv);
    }

    private void initName() {
        SharedPreferences sp = requireActivity().getSharedPreferences("userinfo", MODE_PRIVATE);
        String currentAccount = sp.getString("current_account", "");
        String name = "name_" + currentAccount;
        String string = sp.getString(name, "");
        if (string.isEmpty()) {
            string = currentAccount;
        }
        String result = string + "的智能";
        it_tv.setText(result);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intelligent, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        updateCurrentModeDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        
        // 释放MQTT连接引用（不立即断开，由单例管理）
        if (mqttConnectionManager != null) {
            mqttConnectionManager.releaseManager("ch1hpsnpie8hxwuj", "state");
            mqttConnectionManager.releaseManager("irp8ltd5thuronmp", "state");
            mqttConnectionManager.releaseManager("taclmu1x2gf4s5cx", "camera_status");
        }
    }

    private void showToast(String message) {
        if (getContext() != null && isFragmentActive) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showToastSafe(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
