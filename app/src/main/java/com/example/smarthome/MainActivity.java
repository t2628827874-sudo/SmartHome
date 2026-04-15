package com.example.smarthome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smarthome.Activities.HomeActivity;

public class MainActivity extends AppCompatActivity {
    private TextView login_to_register;
    private EditText login_et_account;
    private EditText login_et_pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //初始化
        initView();


    }
    //登录按钮，实现SP存储的函数
    public void login(View v){
        String account=login_et_account.getText().toString();
        String password=login_et_pwd.getText().toString();
        SharedPreferences sp = getSharedPreferences("userinfo", MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        String str = sp.getString(account, "");

        if(str.equals(password)){
            //标记当前是已经登录的状态
            edit.putBoolean("islogin",true);
            //记录当前登录的账号是哪个
            edit.putString("current_account",account);
            edit.apply();
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            //跳转
            Intent intent=new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        }else{
            Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
            return;
        }

    }


    public void initView(){
        login_to_register=findViewById(R.id.login_to_register);
        login_et_account=findViewById(R.id.login_et_account);
        login_et_pwd=findViewById(R.id.login_et_pwd);
    }
    public void ToRegister(View v){
        Intent intent=new Intent(MainActivity.this,RegisterActivity.class);
        startActivity(intent);

    }
}
