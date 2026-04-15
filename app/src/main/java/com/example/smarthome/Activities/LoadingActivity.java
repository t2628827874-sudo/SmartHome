package com.example.smarthome.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smarthome.MainActivity;
import com.example.smarthome.R;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loading);

        SharedPreferences sp = getSharedPreferences("userinfo", MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        boolean islogin = sp.getBoolean("islogin", false);

        //用Handler来实现延时
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 这里是延时之后要执行的代码
                //判断进入哪个
                if (islogin==true){
                    Intent intent=new Intent(LoadingActivity.this, HomeActivity.class);
                    startActivity(intent);
                }else {
                    Intent intent=new Intent(LoadingActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                //跳转完了之后直接销毁
                finish();
            }
        }, 2000);


    }
}