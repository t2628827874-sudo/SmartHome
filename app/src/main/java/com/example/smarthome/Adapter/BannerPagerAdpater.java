package com.example.smarthome.Adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.List;

//实现自动轮播的Adapter
public class BannerPagerAdpater extends PagerAdapter {

    private List<ImageView> imageViewList;

    public BannerPagerAdpater(List<ImageView> imageViewList) {
        this.imageViewList = imageViewList;
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ImageView imageView = imageViewList.get(position);
        container.addView(imageView);//将图片添加到container

        return imageView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);//划过的图片销毁

    }

    @Override
    public int getCount() {
        return imageViewList==null ? 0:imageViewList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view==object;
    }
}
