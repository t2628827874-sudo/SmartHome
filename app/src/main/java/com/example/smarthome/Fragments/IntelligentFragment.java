package com.example.smarthome.Fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.example.smarthome.MQTT.MqttManager;
import com.example.smarthome.R;
import com.example.smarthome.Utils.LocalDeviceManager;
import com.example.smarthome.Utils.ModeManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

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
    private MqttManager livingLedMqttManager;
    private MqttManager diningLedMqttManager;
    private MqttManager cameraMqttManager;
    
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

    private void initManagers() {
        try {
            modeManager = ModeManager.getInstance(requireContext());
            localDeviceManager = LocalDeviceManager.getInstance(requireContext());
            
            livingLedMqttManager = createMqttManager("ch1hpsnpie8hxwuj", "state", "客厅灯");
            diningLedMqttManager = createMqttManager("irp8ltd5thuronmp", "state", "餐厅灯");
            cameraMqttManager = createMqttManager("taclmu1x2gf4s5cx", "camera_status", "摄像头");
            
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

    private MqttManager createMqttManager(String username, String statusKey, String deviceName) {
        try {
            MqttManager manager = new MqttManager(requireContext(), username, "odoscHX24A", statusKey);
            manager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, deviceName + "MQTT连接成功");
                }
                @Override
                public void onDisconnected() {
                    Log.w(TAG, deviceName + "MQTT连接断开");
                }
                @Override
                public void onConnectionFailed(String error) {
                    Log.e(TAG, deviceName + "MQTT连接失败: " + error);
                }
            });
            manager.connect();
            return manager;
        } catch (Exception e) {
            Log.e(TAG, "创建" + deviceName + "MqttManager失败: " + e.getMessage(), e);
            return null;
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
            
            publishDeviceCommand(livingLedMqttManager, "state", true, "客厅灯");
            
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
            
            publishDeviceCommand(livingLedMqttManager, "state", false, "客厅灯");
            publishDeviceCommand(diningLedMqttManager, "state", false, "餐厅灯");
            publishDeviceCommand(cameraMqttManager, "camera_status", true, "摄像头");
            
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

    private void publishDeviceCommand(MqttManager manager, String property, boolean value, String deviceName) {
        if (manager == null) {
            Log.w(TAG, deviceName + " MqttManager为空，跳过");
            return;
        }
        
        try {
            Log.d(TAG, "发送" + deviceName + "指令: " + property + "=" + value + ", 连接状态=" + manager.isConnected());
            
            if (manager.isConnected()) {
                manager.publish(property, value);
                Log.d(TAG, deviceName + " 指令已发送");
            } else {
                Log.w(TAG, deviceName + " 未连接，尝试延迟发送");
                handler.postDelayed(() -> {
                    if (!isFragmentActive) {
                        Log.w(TAG, "Fragment已销毁，取消延迟发送");
                        return;
                    }
                    
                    try {
                        if (manager.isConnected()) {
                            manager.publish(property, value);
                            Log.d(TAG, deviceName + " 延迟指令已发送");
                        } else {
                            Log.e(TAG, deviceName + " 连接超时");
                            showToastSafe(deviceName + "连接失败");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, deviceName + " 延迟发送失败: " + e.getMessage(), e);
                    }
                }, 2000);
            }
        } catch (Exception e) {
            Log.e(TAG, "发送" + deviceName + "指令失败: " + e.getMessage(), e);
        }
    }

    private void deactivateCurrentMode() {
        if (isProcessingMode || !isFragmentActive) {
            return;
        }
        
        Log.d(TAG, "取消当前模式");
        
        try {
            if (modeManager != null) {
                modeManager.clearMode();
            }
            
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(false);
            if (swAway != null) swAway.setChecked(false);
            isUpdatingSwitches = false;
            
            showToast("已取消模式");
            updateCurrentModeDisplay();
        } catch (Exception e) {
            Log.e(TAG, "取消模式失败: " + e.getMessage(), e);
        }
    }

    private void updateCurrentModeDisplay() {
        if (!isFragmentActive || modeManager == null) return;
        
        try {
            if (tvCurrentTitle != null && tvCurrentSubtitle != null) {
                String modeName = modeManager.getCurrentModeName();
                String displayText = "当前模式：" + modeName;
                tvCurrentSubtitle.setText(displayText);
                Log.d(TAG, "更新模式显示: " + displayText);
            }
            
            updateCardHighlight();
        } catch (Exception e) {
            Log.e(TAG, "更新模式显示失败: " + e.getMessage(), e);
        }
    }

    private void updateCardHighlight() {
        if (!isFragmentActive || modeManager == null) return;
        
        try {
            int activeColor = Color.parseColor("#E3F2FD");
            int normalColor = Color.parseColor("#FFFFFF");
            
            if (cardModeHome != null) {
                cardModeHome.setBackgroundColor(modeManager.isHomeMode() ? activeColor : normalColor);
            }
            if (cardModeAway != null) {
                cardModeAway.setBackgroundColor(modeManager.isAwayMode() ? activeColor : normalColor);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新卡片高亮失败: " + e.getMessage(), e);
        }
    }

    private void showToast(String message) {
        if (getContext() != null && isFragmentActive) {
            try {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "显示Toast失败: " + e.getMessage(), e);
            }
        }
    }

    private void showToastSafe(String message) {
        if (getActivity() != null && isFragmentActive) {
            getActivity().runOnUiThread(() -> {
                if (getContext() != null && isFragmentActive) {
                    try {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "显示Toast失败: " + e.getMessage(), e);
                    }
                }
            });
        }
    }

    private void initName() {
        if (getActivity() == null) return;
        
        try {
            SharedPreferences sp = getActivity().getSharedPreferences("userinfo", MODE_PRIVATE);
            String currentAccount = sp.getString("current_account", "");
            String name = "name_" + currentAccount;
            String string = sp.getString(name, "");
            if (string.isEmpty()) {
                string = currentAccount;
            }
            String result = string + "的家";
            if (it_tv != null) {
                it_tv.setText(result);
            }
        } catch (Exception e) {
            Log.e(TAG, "initName失败: " + e.getMessage(), e);
        }
    }

    private void initView(View view) {
        if (view == null) return;
        it_tv = view.findViewById(R.id.it_tv);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intelligent, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        resetSwitchStates();
        updateCurrentModeDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
    }

    @Override
    public void onDestroyView() {
        isFragmentActive = false;
        isProcessingMode = false;
        handler.removeCallbacksAndMessages(null);
        
        try {
            if (livingLedMqttManager != null) {
                livingLedMqttManager.disconnect();
            }
            if (diningLedMqttManager != null) {
                diningLedMqttManager.disconnect();
            }
            if (cameraMqttManager != null) {
                cameraMqttManager.disconnect();
            }
        } catch (Exception e) {
            Log.e(TAG, "断开MQTT连接失败: " + e.getMessage(), e);
        }
        
        super.onDestroyView();
    }
}
