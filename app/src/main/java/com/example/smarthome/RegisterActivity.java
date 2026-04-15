package com.example.smarthome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {
    private TextView login_to_register;
    private Toolbar register_toolbar;
    private EditText register_et_account;
    private EditText register_et_pwd;
    private EditText register_et_pwd2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initToolbar();
        initView();

    }
    //实现注册信息存储到本地SP里面
    public void saveUserInfo(View v) {
        // 你已经获取了账号和两个密码
        String account = register_et_account.getText().toString().trim(); // 建议加上 trim() 去掉首尾空格
        String pwd1 = register_et_pwd.getText().toString().trim();
        String pwd2 = register_et_pwd2.getText().toString().trim();
        // 1. 先判断输入是否为空 (健壮性检查)
        if (account.isEmpty() || pwd1.isEmpty() || pwd2.isEmpty()) {
            // 提示用户输入不能为空
            Toast.makeText(this, "账号或密码不能为空", Toast.LENGTH_SHORT).show();
            return; // 提前结束方法
        }
        // 2. 使用 .equals() 方法对比两个密码字符串
        if (pwd1.equals(pwd2)) {
            // 如果密码相同，执行注册成功的逻辑
            SharedPreferences sp=getSharedPreferences("userinfo",MODE_PRIVATE);
            SharedPreferences.Editor edit = sp.edit();
            //键值对存储
            edit.putString(account,pwd1);
            edit.apply();

            Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();
            Intent intent=new Intent(this,MainActivity.class);
            startActivity(intent);

        } else {
            Toast.makeText(this, "两次输入的密码不一致，请重新输入", Toast.LENGTH_SHORT).show();
            //清空密码输入框，方便用户重新输入
            register_et_pwd.setText("");
            register_et_pwd2.setText("");
            register_et_pwd.requestFocus(); // 让光标聚焦到第一个密码框
    }
}

    public void initView(){
        login_to_register=findViewById(R.id.login_to_register);
        register_et_account=findViewById(R.id.register_et_account);
        register_et_pwd=findViewById(R.id.register_et_pwd);
        register_et_pwd2=findViewById(R.id.register_et_pwd2);

    }

    //初始化Actionbar，来实现左上角返回按钮
    private void initToolbar() {
        register_toolbar = findViewById(R.id.register_toolbar);
        // 1. 将 Toolbar 设置为 Activity 的 ActionBar
        setSupportActionBar(register_toolbar);
        // 2. 获取 ActionBar 并进行设置
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // 启用返回按钮
            actionBar.setDisplayShowTitleEnabled(false); // 隐藏标题
        }
    }

    public void ToLogin(View v){
        Intent intent=new Intent(RegisterActivity.this,MainActivity.class);
        startActivity(intent);
    }

    // 3. 重写此方法来处理返回箭头的点击事件
    @Override
    public boolean onSupportNavigateUp() {
        // 当用户点击返回箭头时，关闭当前 Activity
        finish();
        return true;
    }
}