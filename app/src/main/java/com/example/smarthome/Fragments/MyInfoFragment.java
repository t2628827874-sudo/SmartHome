package com.example.smarthome.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smarthome.Activities.UserInfoActivity;
import com.example.smarthome.Adapter.BannerPagerAdpater;
import com.example.smarthome.MainActivity;
import com.example.smarthome.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 我的信息Fragment
 * 
 * 功能说明：
 * - 显示用户昵称和头像
 * - 轮播图展示
 * - 退出登录功能
 * 
 * 数据同步：
 * - 在onResume中重新加载头像和昵称，确保数据同步
 */
public class MyInfoFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MyInfoFragment";
    
    private Button btn_logout;
    private TextView myinfo_tv1;
    private ConstraintLayout myinfo_head;
    private ViewPager myinfo_vp;
    private BannerPagerAdpater bannerPagerAdpater;
    private List<ImageView> imageViewList;
    private LinearLayout banner_dots;
    private List<ImageView> dotList = new ArrayList<>();
    private int currentIndex = 0;
    
    private ImageView iv_avatar;
    private SharedPreferences sharedPreferences;
    private String currentAccount;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initSharedPreferences();
        initView(view);
        
        btn_logout.setOnClickListener(this);
        myinfo_head.setOnClickListener(this);
        
        startBannerScroll();
    }

    /**
     * 初始化SharedPreferences
     */
    private void initSharedPreferences() {
        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
            currentAccount = sharedPreferences.getString("current_account", "default");
        }
    }

    /**
     * 启动轮播图
     */
    private void startBannerScroll() {
        initimgData();
        bannerPagerAdpater = new BannerPagerAdpater(imageViewList);
        myinfo_vp.setAdapter(bannerPagerAdpater);
        initDots();
        myinfo_vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override public void onPageScrollStateChanged(int state) {}
        });
    }

    /**
     * 初始化小圆点
     */
    private void initDots() {
        if (getActivity() == null) return;
        
        dotList.clear();
        banner_dots.removeAllViews();

        int count = imageViewList.size();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(i == 0 ? R.drawable.dots_selected : R.drawable.dots_normal);
            banner_dots.addView(dot);
            dotList.add(dot);
        }
        currentIndex = 0;
    }

    /**
     * 更新小圆点状态
     */
    private void updateDots(int position) {
        if (position == currentIndex) return;
        dotList.get(currentIndex).setImageResource(R.drawable.dots_normal);
        dotList.get(position).setImageResource(R.drawable.dots_selected);
        currentIndex = position;
    }

    /**
     * 初始化轮播图片数据
     */
    private void initimgData() {
        if (getActivity() == null) return;
        
        imageViewList = new ArrayList<>();
        
        ImageView imageView = new ImageView(getActivity());
        imageView.setImageResource(R.drawable.banner1);
        imageViewList.add(imageView);

        ImageView imageView2 = new ImageView(getActivity());
        imageView2.setImageResource(R.drawable.banner2);
        imageViewList.add(imageView2);

        ImageView imageView3 = new ImageView(getActivity());
        imageView3.setImageResource(R.drawable.banner3);
        imageViewList.add(imageView3);

        ImageView imageView4 = new ImageView(getActivity());
        imageView4.setImageResource(R.drawable.banner4);
        imageViewList.add(imageView4);

        ImageView imageView5 = new ImageView(getActivity());
        imageView5.setImageResource(R.drawable.banner5);
        imageViewList.add(imageView5);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserName();
        loadAvatar();
    }

    /**
     * 加载用户昵称
     */
    private void loadUserName() {
        if (getActivity() == null || sharedPreferences == null) return;
        
        String nicknameKey = "name_" + currentAccount;
        String name = sharedPreferences.getString(nicknameKey, "");
        if (name.isEmpty()) {
            name = currentAccount;
        }
        if (myinfo_tv1 != null) {
            myinfo_tv1.setText(name);
        }
    }

    /**
     * 加载用户头像
     * 
     * 从SharedPreferences读取头像路径并显示
     * 如果没有自定义头像，显示默认头像
     */
    private void loadAvatar() {
        if (getActivity() == null || sharedPreferences == null || iv_avatar == null) return;
        
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

    /**
     * 初始化视图组件
     */
    private void initView(View view) {
        btn_logout = view.findViewById(R.id.btn_logout);
        myinfo_tv1 = view.findViewById(R.id.myinfo_tv1);
        myinfo_head = view.findViewById(R.id.myinfo_head);
        myinfo_vp = view.findViewById(R.id.myinfo_vp);
        banner_dots = view.findViewById(R.id.banner_dots);
        iv_avatar = view.findViewById(R.id.iv_avatar);
        
        // 设置默认昵称
        if (getActivity() != null && sharedPreferences != null) {
            String nicknameKey = "name_" + currentAccount;
            String name = sharedPreferences.getString(nicknameKey, "");
            if (name.isEmpty()) {
                name = currentAccount;
            }
            myinfo_tv1.setText(name);
        }
        
        // 加载头像
        loadAvatar();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_logout) {
            if (getActivity() != null) {
                SharedPreferences sp = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
                sp.edit().putBoolean("islogin", false).apply();
                
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                Toast.makeText(getActivity(), "退出成功", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
        
        if (v.getId() == R.id.myinfo_head) {
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_info, container, false);
    }
}
