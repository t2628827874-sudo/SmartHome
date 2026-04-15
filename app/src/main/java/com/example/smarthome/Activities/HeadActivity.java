package com.example.smarthome.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smarthome.R;

public class HeadActivity extends AppCompatActivity {
    private Toolbar head_back;
    private Button btn_avatar;
    private ImageView iv_head;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_head);

        iv_head=findViewById(R.id.iv_head);
        btn_avatar=findViewById(R.id.btn_avatar);
        btn_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences("userinfo", MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                


            }
        });

        initToolbar();

    }
    private void initToolbar() {
        head_back = findViewById(R.id.head_back);
        // 1. 将 Toolbar 设置为 Activity 的 ActionBar
        setSupportActionBar(head_back);
        // 2. 获取 ActionBar 并进行设置
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // 启用返回按钮
            actionBar.setDisplayShowTitleEnabled(false); // 隐藏标题
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}