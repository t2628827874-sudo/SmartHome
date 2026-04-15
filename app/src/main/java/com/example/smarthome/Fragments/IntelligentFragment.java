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
 * - 使用MqttManager控制IoT设备
 * - 使用LocalDeviceManager管理本地模拟设备
 * 
 * @author SmartHome Team
 */
public class IntelligentFragment extends Fragment {
    
    private static final String TAG = "IntelligentFragment";
    
    // ==================== UI组件 ====================
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
    
    // ==================== 业务管理器 ====================
    private ModeManager modeManager;
    private LocalDeviceManager localDeviceManager;
    
    // MQTT设备管理器
    private MqttManager livingLedMqttManager;   // 客厅灯
    private MqttManager diningLedMqttManager;   // 餐厅灯
    private MqttManager cameraMqttManager;      // 摄像头
    
    // ==================== 状态控制标志 ====================
    private boolean isUpdatingSwitches = false;     // 防止开关状态更新触发循环回调
    private boolean isProcessingMode = false;       // 模式处理中标志（防抖）
    private boolean isFragmentActive = false;       // Fragment活跃状态标志
    
    // 待发送的设备命令队列（用于MQTT未连接时缓存命令）
    private static class PendingCommand {
        MqttManager manager;
        String property;
        boolean value;
        String deviceName;
        
        PendingCommand(MqttManager manager, String property, boolean value, String deviceName) {
            this.manager = manager;
            this.property = property;
            this.value = value;
            this.deviceName = deviceName;
        }
    }
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_DELAY_MS = 800;      // 防抖延迟时间
    private static final long MQTT_CONNECT_WAIT_MS = 3000;  // MQTT连接等待时间

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
     * 创建并配置：
     * 1. ModeManager - 模式状态管理
     * 2. LocalDeviceManager - 本地设备状态管理
     * 3. MqttManager - IoT设备MQTT连接管理
     */
    private void initManagers() {
        try {
            modeManager = ModeManager.getInstance(requireContext());
            localDeviceManager = LocalDeviceManager.getInstance(requireContext());
            
            // 创建MQTT管理器并设置连接监听
            livingLedMqttManager = createMqttManager("ch1hpsnpie8hxwuj", "state", "客厅灯");
            diningLedMqttManager = createMqttManager("irp8ltd5thuronmp", "state", "餐厅灯");
            cameraMqttManager = createMqttManager("taclmu1x2gf4s5cx", "camera_status", "摄像头");
            
            // 设置模式变化监听器
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

    /**
     * 创建MQTT管理器
     * 
     * @param username 设备用户名（ThingsCloud设备ID）
     * @param statusKey 状态属性键名
     * @param deviceName 设备名称（用于日志）
     * @return MqttManager实例，创建失败返回null
     */
    private MqttManager createMqttManager(String username, String statusKey, String deviceName) {
        try {
            MqttManager manager = new MqttManager(requireContext(), username, "odoscHX24A", statusKey);
            manager.setOnConnectionListener(new MqttManager.OnConnectionListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, deviceName + " MQTT连接成功");
                }
                @Override
                public void onDisconnected() {
                    Log.w(TAG, deviceName + " MQTT连接断开");
                }
                @Override
                public void onConnectionFailed(String error) {
                    Log.e(TAG, deviceName + " MQTT连接失败: " + error);
                }
            });
            manager.connect();
            return manager;
        } catch (Exception e) {
            Log.e(TAG, "创建" + deviceName + "MqttManager失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 初始化卡片视图组件
     * 
     * 布局结构：
     * - cardCurrentMode: 当前模式显示卡
     * - cardModeHome: 回家模式控制卡
     * - cardModeAway: 离家模式控制卡
     */
    private void initCardViews(View view) {
        if (view == null) return;
        
        try {
            cardCurrentMode = view.findViewById(R.id.card_current_mode);
            cardModeHome = view.findViewById(R.id.card_mode_home);
            cardModeAway = view.findViewById(R.id.card_mode_away);

            // 初始化当前模式显示卡
            if (cardCurrentMode != null) {
                tvCurrentTitle = cardCurrentMode.findViewById(R.id.tv_title);
                tvCurrentSubtitle = cardCurrentMode.findViewById(R.id.tv_subtitle);
                ivCurrentIcon = cardCurrentMode.findViewById(R.id.iv_icon);
                
                if (tvCurrentTitle != null) tvCurrentTitle.setText("当前模式");
                if (ivCurrentIcon != null) ivCurrentIcon.setImageResource(R.drawable.home);
            }

            // 初始化回家模式控制卡
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

            // 初始化离家模式控制卡
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

    /**
     * 初始化开关监听器
     * 
     * 监听回家模式和离家模式开关的状态变化：
     * - 开启时：激活对应模式
     * - 关闭时：取消当前模式
     * 
     * 使用isUpdatingSwitches标志防止循环触发
     */
    private void initSwitchListeners() {
        try {
            // 回家模式开关监听
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

            // 离家模式开关监听
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
     * 
     * 执行流程：
     * 1. 检查防抖标志，防止重复执行
     * 2. 设置模式状态为回家模式
     * 3. 更新UI开关状态
     * 4. 发送设备控制命令（开启客厅灯）
     * 5. 显示激活提示
     * 
     * 设备控制：
     * - 客厅灯：开启
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
            // 步骤1：设置模式状态
            if (modeManager != null) {
                modeManager.setMode(ModeManager.MODE_HOME);
            }
            
            // 步骤2：更新UI开关状态（互斥控制）
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(true);
            if (swAway != null) swAway.setChecked(false);
            isUpdatingSwitches = false;
            
            // 步骤3：发送设备控制命令 - 开启客厅灯
            // 使用可靠发送机制，确保命令能够送达
            publishDeviceCommandReliably(livingLedMqttManager, "state", true, "客厅灯");
            
            // 步骤4：显示激活提示
            showToast("回家模式已激活");
            updateCurrentModeDisplay();
            
        } catch (Exception e) {
            Log.e(TAG, "激活回家模式失败: " + e.getMessage(), e);
            showToastSafe("激活失败，请重试");
            resetSwitchStates();
        } finally {
            // 延迟重置防抖标志
            handler.postDelayed(() -> {
                isProcessingMode = false;
                Log.d(TAG, "回家模式处理完成");
            }, DEBOUNCE_DELAY_MS);
        }
    }

    /**
     * 激活离家模式
     * 
     * 执行流程：
     * 1. 检查防抖标志，防止重复执行
     * 2. 设置模式状态为离家模式
     * 3. 更新UI开关状态
     * 4. 发送设备控制命令
     * 5. 关闭所有本地设备
     * 6. 显示激活提示
     * 
     * 设备控制：
     * - 客厅灯：关闭
     * - 餐厅灯：关闭
     * - 摄像头：开启
     * - 所有本地设备：关闭
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
            // 步骤1：设置模式状态
            if (modeManager != null) {
                modeManager.setMode(ModeManager.MODE_AWAY);
            }
            
            // 步骤2：更新UI开关状态（互斥控制）
            isUpdatingSwitches = true;
            if (swHome != null) swHome.setChecked(false);
            if (swAway != null) swAway.setChecked(true);
            isUpdatingSwitches = false;
            
            // 步骤3：发送MQTT设备控制命令
            publishDeviceCommandReliably(livingLedMqttManager, "state", false, "客厅灯");
            publishDeviceCommandReliably(diningLedMqttManager, "state", false, "餐厅灯");
            publishDeviceCommandReliably(cameraMqttManager, "camera_status", true, "摄像头");
            
            // 步骤4：关闭所有本地设备
            closeAllLocalDevices();
            
            // 步骤5：显示激活提示
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
     * 
     * 本地设备列表：
     * - 餐厅：冰箱、空调
     * - 卧室：空调、除湿器、窗帘、扫地机器人、投影仪、音响
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
     * 重置开关状态到当前模式
     * 
     * 用于在异常情况下恢复UI状态与实际模式一致
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
     * 可靠发送设备控制命令
     * 
     * 发送策略：
     * 1. 如果MQTT已连接，立即发送命令
     * 2. 如果MQTT未连接，等待连接建立后发送
     * 3. 多次重试确保命令送达
     * 
     * @param manager MQTT管理器
     * @param property 属性名称（如"state"）
     * @param value 属性值（true/false）
     * @param deviceName 设备名称（用于日志和提示）
     */
    private void publishDeviceCommandReliably(MqttManager manager, String property, boolean value, String deviceName) {
        if (manager == null) {
            Log.w(TAG, deviceName + " MqttManager为空，跳过");
            return;
        }
        
        Log.d(TAG, "准备发送" + deviceName + "指令: " + property + "=" + value);
        
        // 尝试立即发送
        if (tryPublishCommand(manager, property, value, deviceName)) {
            return; // 发送成功
        }
        
        // MQTT未连接，启动重试机制
        Log.w(TAG, deviceName + " 未连接，启动重试机制");
        retryPublishCommand(manager, property, value, deviceName, 0);
    }

    /**
     * 尝试发送命令
     * 
     * @return true表示发送成功，false表示需要重试
     */
    private boolean tryPublishCommand(MqttManager manager, String property, boolean value, String deviceName) {
        try {
            if (manager.isConnected()) {
                manager.publish(property, value);
                Log.d(TAG, deviceName + " 指令发送成功: " + property + "=" + value);
                return true;
            } else {
                Log.d(TAG, deviceName + " 当前未连接");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, deviceName + " 发送指令异常: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 重试发送命令
     * 
     * 重试策略：
     * - 最多重试3次
     * - 每次间隔1秒
     * - 检查Fragment活跃状态
     * 
     * @param retryCount 当前重试次数
     */
    private void retryPublishCommand(MqttManager manager, String property, boolean value, String deviceName, int retryCount) {
        final int MAX_RETRY = 3;
        final long RETRY_INTERVAL = 1000;
        
        handler.postDelayed(() -> {
            // 检查Fragment是否仍然活跃
            if (!isFragmentActive) {
                Log.w(TAG, "Fragment已销毁，取消" + deviceName + "重试");
                return;
            }
            
            // 检查管理器是否有效
            if (manager == null) {
                Log.e(TAG, deviceName + " MqttManager已失效");
                return;
            }
            
            // 尝试发送
            if (tryPublishCommand(manager, property, value, deviceName)) {
                Log.d(TAG, deviceName + " 重试发送成功（第" + (retryCount + 1) + "次）");
                return;
            }
            
            // 检查是否还有重试机会
            if (retryCount < MAX_RETRY - 1) {
                Log.w(TAG, deviceName + " 重试发送失败，准备第" + (retryCount + 2) + "次重试");
                retryPublishCommand(manager, property, value, deviceName, retryCount + 1);
            } else {
                Log.e(TAG, deviceName + " 重试次数已达上限，发送失败");
                showToastSafe(deviceName + "连接失败，请检查网络");
            }
        }, RETRY_INTERVAL);
    }

    /**
     * 取消当前模式
     */
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

    /**
     * 更新当前模式显示
     */
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

    /**
     * 更新卡片高亮状态
     * 
     * 激活的模式卡片显示浅蓝色背景
     */
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

    /**
     * 显示Toast提示（主线程安全）
     */
    private void showToast(String message) {
        if (getContext() != null && isFragmentActive) {
            try {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "显示Toast失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 显示Toast提示（线程安全，可在任意线程调用）
     */
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

    /**
     * 初始化用户名称显示
     */
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
