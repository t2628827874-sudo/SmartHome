package com.example.smarthome.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.smarthome.R;

import java.io.File;

/**
 * 用户个人信息Activity
 * 
 * 功能说明：
 * - 显示和编辑用户头像、昵称、性别
 * - 头像点击跳转到HeadActivity进行更换
 * - 昵称点击跳转到NameActivity进行修改
 * - 性别通过弹出列表选择
 * 
 * 数据同步：
 * - 在onResume中重新加载头像和昵称，确保数据同步
 */
public class UserInfoActivity extends AppCompatActivity implements View.OnClickListener {
    private Toolbar userinfo_back;
    private ConstraintLayout myinfo_head;
    private ConstraintLayout gender_layout;
    private ConstraintLayout userinfo_name;
    private TextView tv_gender_value;
    private TextView user_name;
    private ImageView iv_avatar;
    
    private SharedPreferences sharedPreferences;
    private String currentAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);
        
        initSharedPreferences();
        initToolbar();
        initView();
        loadGender();

        myinfo_head.setOnClickListener(this);
        gender_layout.setOnClickListener(this);
        userinfo_name.setOnClickListener(this);
    }

    /**
     * 初始化SharedPreferences
     */
    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        currentAccount = sharedPreferences.getString("current_account", "default");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserName();
        loadAvatar();
    }

    /**
     * 加载用户昵称
     */
    private void loadUserName() {
        String nicknameKey = "name_" + currentAccount;
        String name = sharedPreferences.getString(nicknameKey, "");
        if (name.isEmpty()) {
            name = currentAccount;
        }
        user_name.setText(name);
    }

    /**
     * 加载用户头像
     * 
     * 从SharedPreferences读取头像路径并显示
     * 如果没有自定义头像，显示默认头像
     */
    private void loadAvatar() {
        String avatarPath = sharedPreferences.getString("avatar_path_" + currentAccount, "");
        if (!avatarPath.isEmpty()) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarPath);
                if (bitmap != null) {
                    iv_avatar.setImageBitmap(bitmap);
                    return;
                }
            }
        }
        // 没有自定义头像，显示默认头像
        iv_avatar.setImageResource(R.drawable.headpicture);
    }

    private void initView() {
        myinfo_head = findViewById(R.id.myinfo_head);
        gender_layout = findViewById(R.id.gender_layout);
        tv_gender_value = findViewById(R.id.tv_gender_value);
        userinfo_name = findViewById(R.id.userinfo_name);
        user_name = findViewById(R.id.user_name);
        iv_avatar = findViewById(R.id.iv_avatar);
        
        // 设置默认昵称
        String nicknameKey = "name_" + currentAccount;
        String name = sharedPreferences.getString(nicknameKey, "");
        if (name.isEmpty()) {
            name = currentAccount;
        }
        user_name.setText(name);
    }

    /**
     * 初始化工具栏
     */
    private void initToolbar() {
        userinfo_back = findViewById(R.id.userinfo_back);
        setSupportActionBar(userinfo_back);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onClick(View v) {
        // 头像点击
        if (v.getId() == R.id.myinfo_head) {
            Intent intent = new Intent(this, HeadActivity.class);
            startActivity(intent);
        }
        // 性别点击
        if (v.getId() == R.id.gender_layout) {
            showGenderDialog();
        }
        // 昵称点击
        if (v.getId() == R.id.userinfo_name) {
            Intent intent = new Intent(this, NameActivity.class);
            startActivity(intent);
        }
    }

    /**
     * 显示性别选择弹窗
     */
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
                String genderKey = "gender_" + currentAccount;
                sharedPreferences.edit().putString(genderKey, selectGender).apply();
                popupWindow.dismiss();
            }
        });
        popupWindow.show();
    }

    /**
     * 加载性别
     */
    private void loadGender() {
        String genderKey = "gender_" + currentAccount;
        String gender = sharedPreferences.getString(genderKey, "");
        if (!gender.isEmpty()) {
            tv_gender_value.setText(gender);
        }
    }
}
