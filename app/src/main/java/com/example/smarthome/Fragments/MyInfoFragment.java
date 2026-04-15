package com.example.smarthome.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.List;

public class MyInfoFragment extends Fragment implements View.OnClickListener {
    private Button btn_logout;
    private TextView myinfo_tv1;
    private ConstraintLayout myinfo_head;
    private ViewPager myinfo_vp;
    private BannerPagerAdpater bannerPagerAdpater;
    private List<ImageView> imageViewList;
    private LinearLayout banner_dots;
    private List<ImageView> dotList = new ArrayList<>();
    private int currentIndex = 0;

    //代码在这里写
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //初始化视图
        initView(view);
        //设置监听给logout
        btn_logout.setOnClickListener(this);
        myinfo_head.setOnClickListener(this);
        //设置轮播的函数,初始化小圆点
        startBannerScroll();

    }

    private void startBannerScroll() {
        // 1. 初始化图片
        initimgData();
        // 2. 设置适配器
        bannerPagerAdpater = new BannerPagerAdpater(imageViewList);
        myinfo_vp.setAdapter(bannerPagerAdpater);
        // 3. 初始化下面的小圆点
        initDots();
        // 4. 监听页面切换，更新小圆点
        myinfo_vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override public void onPageScrollStateChanged(int state) {}
        });
    }

    private void initDots() {
        dotList.clear();
        banner_dots.removeAllViews();

        int count = imageViewList.size();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getActivity());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0); // 点之间的间距
            dot.setLayoutParams(params);
            dot.setImageResource(i == 0 ? R.drawable.dots_selected : R.drawable.dots_normal);
            banner_dots.addView(dot);
            dotList.add(dot);
        }
        currentIndex = 0;
    }

    private void updateDots(int position) {
        if (position == currentIndex) return;
        dotList.get(currentIndex).setImageResource(R.drawable.dots_normal);
        dotList.get(position).setImageResource(R.drawable.dots_selected);
        currentIndex = position;
    }

    private void initimgData() {
        ImageView imageView=new ImageView(getActivity());
        imageView.setImageResource(R.drawable.banner1);

        ImageView imageView2=new ImageView(getActivity());
        imageView2.setImageResource(R.drawable.banner2);

        ImageView imageView3=new ImageView(getActivity());
        imageView3.setImageResource(R.drawable.banner3);

        ImageView imageView4=new ImageView(getActivity());
        imageView4.setImageResource(R.drawable.banner4);

        ImageView imageView5=new ImageView(getActivity());
        imageView5.setImageResource(R.drawable.banner5);

        imageViewList=new ArrayList<>();
        imageViewList.add(imageView);
        imageViewList.add(imageView2);
        imageViewList.add(imageView3);
        imageViewList.add(imageView4);
        imageViewList.add(imageView5);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserName();
    }
    private void loadUserName() {
        SharedPreferences sp = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        String currentAccount = sp.getString("current_account", "");
        String nicknameKey = "name_" + currentAccount;

        String name = sp.getString(nicknameKey, "");
        if (name.isEmpty()) {
            name = currentAccount;
        }
        myinfo_tv1.setText(name);
    }

    private void initView(View view) {
        btn_logout= view.findViewById(R.id.btn_logout);
        myinfo_tv1=view.findViewById(R.id.myinfo_tv1);
        myinfo_head=view.findViewById(R.id.myinfo_head);
        myinfo_vp=view.findViewById(R.id.myinfo_vp);
        banner_dots=view.findViewById(R.id.banner_dots);
        //设置昵称
        SharedPreferences sp = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        String currentAccount = sp.getString("current_account","");
        //拼接然后取sp找，没有就默认
        String name="name_"+currentAccount;
        String string = sp.getString(name, "");
        if(string.isEmpty()){
            string=currentAccount;
        }
        myinfo_tv1.setText(string);

    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.btn_logout){
            // 确保 Fragment 仍然附加在 Activity 上，避免空指针异常
            if(getActivity()!=null){
                SharedPreferences sp = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.putBoolean("islogin",false);
                edit.apply();
                //创建跳转到MainActivity
                Intent intent=new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                Toast.makeText(getActivity(), "退出成功", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
        //头部个人信息head
        if (v.getId()==R.id.myinfo_head){
            Intent intent=new Intent(getActivity(), UserInfoActivity.class);
            startActivity(intent);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_info, container, false);
    }


}