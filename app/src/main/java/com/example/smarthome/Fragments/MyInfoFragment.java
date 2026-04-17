package com.example.smarthome.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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

import androidx.appcompat.app.AlertDialog;

import com.example.smarthome.Activities.UserInfoActivity;
import com.example.smarthome.Adapter.BannerPagerAdpater;
import com.example.smarthome.Adapter.BannerTransformer;
import com.example.smarthome.MainActivity;
import com.example.smarthome.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyInfoFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MyInfoFragment";
    private static final long BANNER_INTERVAL_MS = 3000;
    
    private Button btn_logout;
    private TextView myinfo_tv1;
    private ConstraintLayout myinfo_head;
    private ViewPager myinfo_vp;
    private BannerPagerAdpater bannerPagerAdpater;
    private int[] bannerResIds = {R.drawable.banner1, R.drawable.banner2, R.drawable.banner3, R.drawable.banner4, R.drawable.banner5};
    private LinearLayout banner_dots;
    private List<ImageView> dotList = new ArrayList<>();
    private int currentDotIndex = 0;
    
    private ImageView iv_avatar;
    private SharedPreferences sharedPreferences;
    private String currentAccount;
    
    private Handler bannerHandler;
    private boolean isAutoScrollEnabled = true;
    private boolean isUserTouching = false;
    private int currentPosition = 0;
    
    private ExecutorService executorService;
    private Handler uiHandler;
    private volatile boolean isAvatarLoaded = false;
    
    private boolean isViewCreated = false;
    private boolean isFirstVisible = true;
    
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAutoScrollEnabled && !isUserTouching && bannerPagerAdpater != null && myinfo_vp != null) {
                currentPosition++;
                myinfo_vp.setCurrentItem(currentPosition, true);
            }
            if (bannerHandler != null) {
                bannerHandler.postDelayed(this, BANNER_INTERVAL_MS);
            }
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        initSharedPreferences();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bannerHandler = new Handler(Looper.getMainLooper());
        uiHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initView(view);
        btn_logout.setOnClickListener(this);
        myinfo_head.setOnClickListener(this);
        
        isViewCreated = true;
        
        if (isFirstVisible && isResumed()) {
            onFragmentFirstVisible();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isViewCreated) {
            if (isFirstVisible) {
                onFragmentFirstVisible();
            } else {
                loadUserName();
                isAvatarLoaded = false;
                loadAvatarAsync();
                startAutoScroll();
            }
        }
    }

    private void onFragmentFirstVisible() {
        isFirstVisible = false;
        startBannerScroll();
        loadUserName();
        loadAvatarAsync();
        startAutoScroll();
    }

    private void initSharedPreferences() {
        if (getActivity() != null) {
            sharedPreferences = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
            currentAccount = sharedPreferences.getString("current_account", "default");
        }
    }

    private void startBannerScroll() {
        if (getActivity() == null || myinfo_vp == null) return;
        
        bannerPagerAdpater = new BannerPagerAdpater(getActivity(), bannerResIds);
        myinfo_vp.setAdapter(bannerPagerAdpater);
        myinfo_vp.setPageTransformer(true, new BannerTransformer());
        myinfo_vp.setOffscreenPageLimit(1);
        
        int startPosition = Integer.MAX_VALUE / 2;
        startPosition -= startPosition % bannerResIds.length;
        currentPosition = startPosition;
        myinfo_vp.setCurrentItem(currentPosition, false);
        
        initDots();
        
        myinfo_vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                updateDots(position);
            }
            
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
                switch (state) {
                    case ViewPager.SCROLL_STATE_DRAGGING:
                        isUserTouching = true;
                        break;
                    case ViewPager.SCROLL_STATE_IDLE:
                        isUserTouching = false;
                        break;
                }
            }
        });
        
        myinfo_vp.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_MOVE:
                    isUserTouching = true;
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    isUserTouching = false;
                    break;
            }
            return false;
        });
    }

    private void startAutoScroll() {
        isAutoScrollEnabled = true;
        if (bannerHandler != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler.postDelayed(bannerRunnable, BANNER_INTERVAL_MS);
        }
    }

    private void stopAutoScroll() {
        isAutoScrollEnabled = false;
        if (bannerHandler != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }

    private void initDots() {
        if (getActivity() == null || banner_dots == null) return;
        
        dotList.clear();
        banner_dots.removeAllViews();

        for (int i = 0; i < bannerResIds.length; i++) {
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
        currentDotIndex = 0;
    }

    private void updateDots(int position) {
        int realPosition = position % bannerResIds.length;
        if (realPosition == currentDotIndex) return;
        
        if (currentDotIndex >= 0 && currentDotIndex < dotList.size()) {
            dotList.get(currentDotIndex).setImageResource(R.drawable.dots_normal);
        }
        if (realPosition >= 0 && realPosition < dotList.size()) {
            dotList.get(realPosition).setImageResource(R.drawable.dots_selected);
        }
        currentDotIndex = realPosition;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
    }

    @Override
    public void onDestroyView() {
        stopAutoScroll();
        super.onDestroyView();
        isViewCreated = false;
        isFirstVisible = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
            uiHandler = null;
        }
        if (bannerHandler != null) {
            bannerHandler.removeCallbacksAndMessages(null);
            bannerHandler = null;
        }
    }

    private void loadUserName() {
        if (getActivity() == null || sharedPreferences == null || myinfo_tv1 == null) return;
        
        String nicknameKey = "name_" + currentAccount;
        String name = sharedPreferences.getString(nicknameKey, "");
        if (name.isEmpty()) {
            name = currentAccount;
        }
        myinfo_tv1.setText(name);
    }

    private void loadAvatarAsync() {
        if (getActivity() == null || sharedPreferences == null || iv_avatar == null || executorService == null || isAvatarLoaded) {
            return;
        }
        
        final String avatarPath = sharedPreferences.getString("avatar_path_" + currentAccount, "");
        final ImageView avatarView = iv_avatar;
        
        executorService.execute(() -> {
            if (avatarPath.isEmpty()) {
                uiHandler.post(() -> {
                    if (isAdded() && avatarView != null) {
                        avatarView.setImageResource(R.drawable.headpicture);
                        isAvatarLoaded = true;
                    }
                });
                return;
            }
            
            File avatarFile = new File(avatarPath);
            if (!avatarFile.exists()) {
                uiHandler.post(() -> {
                    if (isAdded() && avatarView != null) {
                        avatarView.setImageResource(R.drawable.headpicture);
                        isAvatarLoaded = true;
                    }
                });
                return;
            }
            
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(avatarPath, options);
                
                int targetWidth = 160;
                int targetHeight = 160;
                int scaleFactor = Math.min(
                        options.outWidth / targetWidth,
                        options.outHeight / targetHeight
                );
                scaleFactor = Math.max(1, scaleFactor);
                
                options.inJustDecodeBounds = false;
                options.inSampleSize = scaleFactor;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                
                final Bitmap bitmap = BitmapFactory.decodeFile(avatarPath, options);
                
                uiHandler.post(() -> {
                    if (isAdded() && avatarView != null) {
                        if (bitmap != null && !bitmap.isRecycled()) {
                            avatarView.setImageBitmap(bitmap);
                        } else {
                            avatarView.setImageResource(R.drawable.headpicture);
                        }
                        isAvatarLoaded = true;
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    if (isAdded() && avatarView != null) {
                        avatarView.setImageResource(R.drawable.headpicture);
                        isAvatarLoaded = true;
                    }
                });
            }
        });
    }

    private void initView(View view) {
        btn_logout = view.findViewById(R.id.btn_logout);
        myinfo_tv1 = view.findViewById(R.id.myinfo_tv1);
        myinfo_head = view.findViewById(R.id.myinfo_head);
        myinfo_vp = view.findViewById(R.id.myinfo_vp);
        banner_dots = view.findViewById(R.id.banner_dots);
        iv_avatar = view.findViewById(R.id.iv_avatar);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_logout) {
            showLogoutConfirmDialog();
        }
        
        if (v.getId() == R.id.myinfo_head) {
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            startActivity(intent);
        }
    }
    
    private void showLogoutConfirmDialog() {
        if (getActivity() == null) return;
        
        new AlertDialog.Builder(getActivity())
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
    
    private void performLogout() {
        if (getActivity() == null) return;
        
        SharedPreferences sp = getActivity().getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        sp.edit().putBoolean("islogin", false).apply();
        
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        Toast.makeText(getActivity(), "退出成功", Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }
}
