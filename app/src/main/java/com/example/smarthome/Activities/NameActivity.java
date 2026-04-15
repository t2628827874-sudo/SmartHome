package com.example.smarthome.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smarthome.R;

public class NameActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText user_name;
    private Toolbar back;
    private Button btn_save_name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_name);

        user_name=findViewById(R.id.user_name);
        btn_save_name=findViewById(R.id.btn_save_name);

        //设置默认的昵称
        SharedPreferences sp = getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        String currentAccount = sp.getString("current_account","");
        //拼接然后取sp找，没有就默认
        String name="name_"+currentAccount;
        String string = sp.getString(name, "");
        if(string.isEmpty()){
            string=currentAccount;
        }
        user_name.setText(string);

        //修改名称
        btn_save_name.setOnClickListener(this);
        //设置回退
        initToolbar();

    }
    private void initToolbar() {
        back = findViewById(R.id.back);
        // 1. 将 Toolbar 设置为 Activity 的 ActionBar
        setSupportActionBar(back);
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

    @Override
    public void onClick(View v) {
        //点击确认修改名字的逻辑
        if(v.getId()==R.id.btn_save_name){
            String editex=user_name.getText().toString();

            SharedPreferences sp = getSharedPreferences("userinfo", MODE_PRIVATE);
            SharedPreferences.Editor edit = sp.edit();
            String currentAccount = sp.getString("current_account", "");
            String nameKey="name_"+currentAccount;//用name_account来实现不同账号存储
            if(editex.isEmpty()){
                Toast.makeText(this, "不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            //把修稿后的名字放入sp中，
            edit.putString(nameKey,editex);
            edit.apply();
            finish();
        }
    }
}