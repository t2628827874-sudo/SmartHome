package com.example.smarthome.Adapter;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class BannerTransformer implements ViewPager.PageTransformer {
    
    private static final float MIN_SCALE = 0.9f;

    @Override
    public void transformPage(View page, float position) {
        if (position < -1) {
            page.setScaleX(MIN_SCALE);
            page.setScaleY(MIN_SCALE);
        } else if (position <= 1) {
            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position) * 0.1f);
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
        } else {
            page.setScaleX(MIN_SCALE);
            page.setScaleY(MIN_SCALE);
        }
    }
}
