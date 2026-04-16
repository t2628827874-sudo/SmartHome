package com.example.smarthome.Activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smarthome.Fragments.HomeFragment;
import com.example.smarthome.Fragments.IntelligentFragment;
import com.example.smarthome.Fragments.MyInfoFragment;
import com.example.smarthome.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private FrameLayout fragment_container;
    private BottomNavigationView bottomNavigationView;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    
    private Fragment homeFragment;
    private Fragment intelligentFragment;
    private Fragment myInfoFragment;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        initPermissionLauncher();
        requestNotificationPermission();

        fragment_container = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        homeFragment = new HomeFragment();
        intelligentFragment = new IntelligentFragment();
        myInfoFragment = new MyInfoFragment();
        
        currentFragment = homeFragment;
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, intelligentFragment, "intelligent")
                .hide(intelligentFragment)
                .add(R.id.fragment_container, myInfoFragment, "my")
                .hide(myInfoFragment)
                .commit();
        
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.Home) {
                    switchFragment(homeFragment);
                } else if (id == R.id.intelligent) {
                    switchFragment(intelligentFragment);
                } else if (id == R.id.My) {
                    switchFragment(myInfoFragment);
                }
                return true;
            }
        });
    }

    private void switchFragment(Fragment targetFragment) {
        if (currentFragment == targetFragment) {
            return;
        }
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(currentFragment);
        transaction.show(targetFragment);
        transaction.commitAllowingStateLoss();
        currentFragment = targetFragment;
    }

    private void initPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "通知权限已授权");
                    } else {
                        Log.w(TAG, "通知权限被拒绝");
                        Toast.makeText(this, "未授权通知权限，后台状态通知功能将无法使用", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "请求通知权限");
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "通知权限已授权");
            }
        }
    }
}
