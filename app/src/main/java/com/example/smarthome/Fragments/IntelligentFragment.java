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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smarthome.Model.WeatherModel;
import com.example.smarthome.MQTT.MqttConnectionManager;
import com.example.smarthome.MQTT.MqttManager;
import com.example.smarthome.R;
import com.example.smarthome.Service.WeatherService;
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
 * - 天气显示：显示沧州市实时天气信息
 * 
 * 架构设计：
 * - 使用ModeManager管理模式状态（支持持久化）
 * - 使用MqttConnectionManager单例控制IoT设备（避免重复连接）
 * - 使用LocalDeviceManager管理本地模拟设备
 * - 使用WeatherService获取天气数据
 */
public class IntelligentFragment extends Fragment {
    
    private static final String TAG = "IntelligentFragment";
    private static final String DEFAULT_CITY = "沧州";
    
    private TextView it_tv;
    private View cardCurrentMode;
    private View cardModeHome;
    private View cardModeAway;
    private View cardWeather;
    
    private TextView tvCurrentTitle;
    private TextView tvCurrentSubtitle;
    private ImageView ivCurrentIcon;
    
    private SwitchMaterial swHome;
    private SwitchMaterial swAway;
    private TextView tvHomeTitle;
    private TextView tvHomeSubtitle;
    private TextView tvAwayTitle;
    private TextView tvAwaySubtitle;
    
    // 天气UI组件
    private TextView tvWeatherCity;
    private TextView tvWeatherUpdateTime;
    private TextView tvWeatherTemp;
    private TextView tvWeatherDesc;
    private TextView tvWeatherTempHigh;
    private TextView tvWeatherTempLow;
    private TextView tvWeatherWind;
    private TextView tvWeatherHumidity;
    private TextView tvWeatherAir;
    private ProgressBar progressWeather;
    private TextView tvWeatherError;
    
    private ModeManager modeManager;
    private LocalDeviceManager localDeviceManager;
    private MqttConnectionManager mqttConnectionManager;
    private WeatherService weatherService;
    
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
            fetchWeather();
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated初始化失败: " + e.getMessage(), e);
            showToastSafe("初始化失败，请重试");
        }
    }

    /**
     * 初始化业务管理器
     */
    private void initManagers() {
        try {
            modeManager = ModeManager.getInstance(requireContext());
            localDeviceManager = LocalDeviceManager.getInstance(requireContext());
            mqttConnectionManager = MqttConnectionManager.getInstance(requireContext());
            weatherService = WeatherService.getInstance();
            
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
            cardWeather = view.findViewById(R.id.card_weather);

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
            
            initWeatherCard();
        } catch (Exception e) {
            Log.e(TAG, "initCardViews失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化天气卡片
     */
    private void initWeatherCard() {
        if (cardWeather == null) return;
        
        tvWeatherCity = cardWeather.findViewById(R.id.tv_weather_city);
        tvWeatherUpdateTime = cardWeather.findViewById(R.id.tv_weather_update_time);
        tvWeatherTemp = cardWeather.findViewById(R.id.tv_weather_temp);
        tvWeatherDesc = cardWeather.findViewById(R.id.tv_weather_desc);
        tvWeatherTempHigh = cardWeather.findViewById(R.id.tv_weather_temp_high);
        tvWeatherTempLow = cardWeather.findViewById(R.id.tv_weather_temp_low);
        tvWeatherWind = cardWeather.findViewById(R.id.tv_weather_wind);
        tvWeatherHumidity = cardWeather.findViewById(R.id.tv_weather_humidity);
        tvWeatherAir = cardWeather.findViewById(R.id.tv_weather_air);
        progressWeather = cardWeather.findViewById(R.id.progress_weather);
        tvWeatherError = cardWeather.findViewById(R.id.tv_weather_error);
        
        // 点击错误提示可重试
        if (tvWeatherError != null) {
            tvWeatherError.setOnClickListener(v -> fetchWeather());
        }
    }
    
    /**
     * 获取天气数据
     */
    private void fetchWeather() {
        if (weatherService == null) return;
        
        showWeatherLoading(true);
        
        weatherService.getWeather(DEFAULT_CITY, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherModel weather) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showWeatherLoading(false);
                        updateWeatherDisplay(weather);
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (isFragmentActive && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showWeatherLoading(false);
                        showWeatherError(errorMessage);
                    });
                }
            }
        });
    }
    
    /**
     * 显示天气加载状态
     */
    private void showWeatherLoading(boolean loading) {
        if (progressWeather != null) {
            progressWeather.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (tvWeatherError != null) {
            tvWeatherError.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示天气错误
     */
    private void showWeatherError(String message) {
        if (tvWeatherError != null) {
            tvWeatherError.setText("加载失败，点击重试");
            tvWeatherError.setVisibility(View.VISIBLE);
        }
        Log.e(TAG, "天气加载失败: " + message);
    }
    
    /**
     * 更新天气显示
     * 
     * API返回扁平结构数据，直接从WeatherModel获取
     */
    private void updateWeatherDisplay(WeatherModel weather) {
        if (weather == null || cardWeather == null) {
            showWeatherError("天气数据为空");
            return;
        }
        
        try {
            // 城市名称
            if (tvWeatherCity != null) {
                String cityName = weather.getCity();
                if (cityName != null && !cityName.isEmpty()) {
                    tvWeatherCity.setText(cityName + "天气");
                } else {
                    tvWeatherCity.setText(DEFAULT_CITY + "天气");
                }
            }
            
            // 更新时间
            if (tvWeatherUpdateTime != null) {
                String updateTime = weather.getUpdateTime();
                if (updateTime != null && !updateTime.isEmpty()) {
                    tvWeatherUpdateTime.setText(updateTime + " 更新");
                } else {
                    tvWeatherUpdateTime.setText("刚刚更新");
                }
            }
            
            // 温度 - 使用格式化方法确保数字格式
            if (tvWeatherTemp != null) {
                tvWeatherTemp.setText(weather.getFormattedTemperature());
            }
            
            // 天气描述
            if (tvWeatherDesc != null) {
                String weatherDesc = weather.getWeather();
                if (weatherDesc != null && !weatherDesc.isEmpty()) {
                    tvWeatherDesc.setText(weatherDesc);
                } else {
                    tvWeatherDesc.setText("--");
                }
            }
            
            // 最高温度
            if (tvWeatherTempHigh != null) {
                tvWeatherTempHigh.setText(weather.getFormattedTemperatureDay());
            }
            
            // 最低温度
            if (tvWeatherTempLow != null) {
                tvWeatherTempLow.setText(weather.getFormattedTemperatureNight());
            }
            
            // 风力
            if (tvWeatherWind != null) {
                String wind = weather.getWind();
                String windSpeed = weather.getWindSpeed();
                StringBuilder windText = new StringBuilder();
                if (wind != null && !wind.isEmpty()) {
                    windText.append(wind);
                }
                if (windSpeed != null && !windSpeed.isEmpty()) {
                    if (windText.length() > 0) {
                        windText.append(" ");
                    }
                    windText.append(windSpeed);
                }
                if (windText.length() > 0) {
                    tvWeatherWind.setText(windText.toString());
                } else {
                    tvWeatherWind.setText("--");
                }
            }
            
            // 湿度 - 使用格式化方法
            if (tvWeatherHumidity != null) {
                tvWeatherHumidity.setText(weather.getFormattedHumidity());
            }
            
            // 空气质量
            if (tvWeatherAir != null) {
                String air = weather.getAir();
                String airLevel = weather.getAirQualityLevel();
                if (air != null && !air.isEmpty()) {
                    tvWeatherAir.setText(airLevel + " " + air);
                    tvWeatherAir.setTextColor(weather.getAirQualityColor());
                } else {
                    tvWeatherAir.setText("--");
                    tvWeatherAir.setTextColor(0xFF666666);
                }
            }
            
            Log.d(TAG, "天气数据更新成功: " + weather.getCity() + " " + weather.getFormattedTemperature() + " " + weather.getWeather());
            
        } catch (Exception e) {
            Log.e(TAG, "更新天气显示失败: " + e.getMessage(), e);
            showWeatherError("显示更新失败");
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
        // 刷新天气数据
        if (weatherService != null) {
            fetchWeather();
        }
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
