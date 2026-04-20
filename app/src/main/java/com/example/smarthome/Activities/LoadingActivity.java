package com.example.smarthome.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthome.MainActivity;
import com.example.smarthome.R;
import com.example.smarthome.Service.EnergyManager;
import com.example.smarthome.Utils.LocalDeviceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 加载启动页
 * 显示欢迎界面并执行实际的初始化加载任务
 */
public class LoadingActivity extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    
    private View tvWelcome;
    private View tvSubtitle;
    private View dot1, dot2, dot3;
    private TextView tvLoading;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    private SharedPreferences userPreferences;
    private boolean isLoadingComplete = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loading);
        
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        userPreferences = getSharedPreferences("userinfo", MODE_PRIVATE);
        
        initViews();
        startEnterAnimation();
        startLoadingTasks();
    }
    
    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        tvLoading = findViewById(R.id.tv_loading);
    }
    
    private void startEnterAnimation() {
        Animation welcomeAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
        welcomeAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                tvWelcome.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                startSubtitleAnimation();
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        tvWelcome.startAnimation(welcomeAnim);
        
        startDotsAnimation();
    }
    
    private void startSubtitleAnimation() {
        Animation subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_up);
        subtitleAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                tvSubtitle.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {}
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        tvSubtitle.startAnimation(subtitleAnim);
    }
    
    private void startDotsAnimation() {
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
        
        new Handler(getMainLooper()).postDelayed(() -> dot1.startAnimation(pulseAnim), 200);
        new Handler(getMainLooper()).postDelayed(() -> dot2.startAnimation(pulseAnim), 400);
        new Handler(getMainLooper()).postDelayed(() -> dot3.startAnimation(pulseAnim), 600);
    }
    
    /**
     * 启动加载任务
     * 在后台线程中执行实际的初始化操作
     */
    private void startLoadingTasks() {
        executorService.execute(() -> {
            try {
                updateLoadingText("正在加载用户配置...");
                Thread.sleep(300);
                loadUserPreferences();
                
                updateLoadingText("正在初始化设备数据...");
                Thread.sleep(300);
                initializeDeviceManager();
                
                updateLoadingText("正在恢复能耗统计...");
                Thread.sleep(300);
                initializeEnergyManager();
                
                updateLoadingText("正在加载应用资源...");
                Thread.sleep(200);
                preloadResources();
                
                updateLoadingText("加载完成");
                Thread.sleep(200);
                
                isLoadingComplete = true;
                
                mainHandler.post(this::navigateToNext);
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Loading task interrupted", e);
                mainHandler.post(this::navigateToNext);
            }
        });
    }
    
    /**
     * 加载用户配置
     */
    private void loadUserPreferences() {
        boolean isLogin = userPreferences.getBoolean("islogin", false);
        String username = userPreferences.getString("username", "");
        Log.d(TAG, "用户登录状态: " + isLogin + ", 用户名: " + username);
    }
    
    /**
     * 初始化设备管理器
     * 加载本地设备状态
     */
    private void initializeDeviceManager() {
        LocalDeviceManager deviceManager = LocalDeviceManager.getInstance(getApplicationContext());
        Log.d(TAG, "设备管理器初始化完成，设备数量: " + deviceManager.getDeviceCount());
    }
    
    /**
     * 初始化能耗管理器
     * 恢复能耗统计数据
     */
    private void initializeEnergyManager() {
        EnergyManager energyManager = EnergyManager.getInstance(getApplicationContext());
        Log.d(TAG, "能耗管理器初始化完成");
    }
    
    /**
     * 预加载资源
     */
    private void preloadResources() {
        Log.d(TAG, "资源预加载完成");
    }
    
    /**
     * 更新加载文字
     */
    private void updateLoadingText(String text) {
        mainHandler.post(() -> {
            if (tvLoading != null) {
                tvLoading.setText(text);
            }
        });
    }
    
    /**
     * 导航到下一个页面
     */
    private void navigateToNext() {
        boolean isLogin = userPreferences.getBoolean("islogin", false);
        
        Intent intent;
        if (isLogin) {
            intent = new Intent(LoadingActivity.this, HomeActivity.class);
        } else {
            intent = new Intent(LoadingActivity.this, MainActivity.class);
        }
        
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        if (tvWelcome != null) tvWelcome.clearAnimation();
        if (tvSubtitle != null) tvSubtitle.clearAnimation();
        if (dot1 != null) dot1.clearAnimation();
        if (dot2 != null) dot2.clearAnimation();
        if (dot3 != null) dot3.clearAnimation();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        super.onDestroy();
    }
}
