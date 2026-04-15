package com.example.smarthome.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smarthome.R;

public class UserInfoActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar userinfo_back;
    private ConstraintLayout myinfo_head;
    private ConstraintLayout gender_layout;
    private ConstraintLayout userinfo_name;
    private TextView tv_gender_value;
    private TextView user_name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);
        //实现左上角返回
        initToolbar();
        initView();

        myinfo_head.setOnClickListener(this);
        gender_layout.setOnClickListener(this);
        userinfo_name.setOnClickListener(this);
        //修改性别
        loadGender();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserName();//重新刷新用户的昵称

    }

    private void loadUserName() {
        SharedPreferences sp = getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        String currentAccount = sp.getString("current_account", "");
        String nicknameKey = "name_" + currentAccount;

        String name = sp.getString(nicknameKey, "");
        if (name.isEmpty()) {
            name = currentAccount;
        }
        user_name.setText(name);
    }

    private void initView() {
        myinfo_head=findViewById(R.id.myinfo_head);
        gender_layout=findViewById(R.id.gender_layout);
        tv_gender_value=findViewById(R.id.tv_gender_value);
        userinfo_name=findViewById(R.id.userinfo_name);
        user_name=findViewById(R.id.user_name);
        //设置默认的昵称
        SharedPreferences sp = getSharedPreferences("userinfo",MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        String currentAccount = sp.getString("current_account","");
        //拼接然后取sp找，没有就默认
        String name="name_"+currentAccount;
        String string = sp.getString(name, "");
        if(string.isEmpty()){
            string=currentAccount;
        }
        user_name.setText(string);
    }

    //实现左上角返回
    private void initToolbar() {
        userinfo_back = findViewById(R.id.userinfo_back);
        // 1. 将 Toolbar 设置为 Activity 的 ActionBar
        setSupportActionBar(userinfo_back);
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
        //toux
        if(v.getId()==R.id.myinfo_head){
            Intent intent=new Intent(this, HeadActivity.class);
            startActivity(intent);
        }
        //点击的是性别
        if(v.getId()==R.id.gender_layout){
            //显示GenderDialog
            showGenderDialog();
        }
        //点击的是昵称
        if(v.getId()==R.id.userinfo_name){
            Intent intent=new Intent(this,NameActivity.class);
            startActivity(intent);
        }
    }
    //显示男女性别的AlertDialog
    //性别弹出列表：在性别这一行下方弹出，点击立即生效
    private void showGenderDialog() {
        final String[] items = {"男", "女"};
        final ListPopupWindow popupWindow = new ListPopupWindow(this);
        popupWindow.setAnchorView(gender_layout);
        popupWindow.setWidth(gender_layout.getWidth());
        popupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        popupWindow.setModal(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        popupWindow.setAdapter(adapter);
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectGender = items[position];
                tv_gender_value.setText(selectGender);
                //
                SharedPreferences sp = getSharedPreferences("userinfo", MODE_PRIVATE);
                String currentAccount = sp.getString("current_account", "");
                String genderKey = "gender_" + currentAccount;
                sp.edit().putString(genderKey, selectGender).apply();
                popupWindow.dismiss();
            }
        });
        popupWindow.show();
    }
    private void loadGender() {
        SharedPreferences sp = getSharedPreferences("userinfo", MODE_PRIVATE);
        String currentAccount = sp.getString("current_account", "");
        String genderKey = "gender_" + currentAccount;

        String gender = sp.getString(genderKey, "");
        if (!gender.isEmpty()) {
            tv_gender_value.setText(gender);
        } else {
            // 可以什么都不设，或者显示“保密”
            // tv_gender_value.setText("保密");
        }
    }
}
