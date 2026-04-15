package com.example.smarthome.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ModeManager {
    private static final String TAG = "ModeManager";
    private static final String PREF_NAME = "mode_prefs";
    private static final String KEY_CURRENT_MODE = "current_mode";
    
    public static final int MODE_NONE = 0;
    public static final int MODE_HOME = 1;
    public static final int MODE_AWAY = 2;
    
    private static ModeManager instance;
    private SharedPreferences preferences;
    private int currentMode;
    private OnModeChangeListener modeChangeListener;
    
    public interface OnModeChangeListener {
        void onModeChanged(int newMode);
    }
    
    private ModeManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentMode = preferences.getInt(KEY_CURRENT_MODE, MODE_NONE);
        Log.d(TAG, "初始化模式管理器，当前模式: " + getModeName(currentMode));
    }
    
    public static synchronized ModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModeManager(context);
        }
        return instance;
    }
    
    public int getCurrentMode() {
        return currentMode;
    }
    
    public void setMode(int mode) {
        if (mode == currentMode) {
            Log.d(TAG, "模式未变化: " + getModeName(mode));
            return;
        }
        
        int oldMode = currentMode;
        currentMode = mode;
        preferences.edit().putInt(KEY_CURRENT_MODE, mode).apply();
        
        Log.d(TAG, "模式切换: " + getModeName(oldMode) + " -> " + getModeName(mode));
        
        if (modeChangeListener != null) {
            modeChangeListener.onModeChanged(mode);
        }
    }
    
    public void clearMode() {
        setMode(MODE_NONE);
    }
    
    public String getModeName(int mode) {
        switch (mode) {
            case MODE_HOME:
                return "回家模式";
            case MODE_AWAY:
                return "离家模式";
            default:
                return "无模式";
        }
    }
    
    public String getCurrentModeName() {
        return getModeName(currentMode);
    }
    
    public String getModeDescription(int mode) {
        switch (mode) {
            case MODE_HOME:
                return "已开启客厅灯光";
            case MODE_AWAY:
                return "已关闭所有电器，开启监控";
            default:
                return "暂无激活的场景";
        }
    }
    
    public void setOnModeChangeListener(OnModeChangeListener listener) {
        this.modeChangeListener = listener;
    }
    
    public boolean isHomeMode() {
        return currentMode == MODE_HOME;
    }
    
    public boolean isAwayMode() {
        return currentMode == MODE_AWAY;
    }
    
    public boolean hasActiveMode() {
        return currentMode != MODE_NONE;
    }
}
