package com.example.smarthome.Adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class BannerPagerAdpater extends PagerAdapter {

    private Context context;
    private int[] imageResIds;
    private static final int FAKE_BANNER_SIZE = Integer.MAX_VALUE;
    private List<ImageView> cachedViews = new ArrayList<>();

    public BannerPagerAdpater(Context context, int[] imageResIds) {
        this.context = context;
        this.imageResIds = imageResIds;
        
        for (int i = 0; i < imageResIds.length; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setImageResource(imageResIds[i]);
            cachedViews.add(imageView);
        }
    }

    @Override
    public int getCount() {
        return imageResIds == null ? 0 : FAKE_BANNER_SIZE;
    }

    public int getRealCount() {
        return imageResIds == null ? 0 : imageResIds.length;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        int realPosition = position % getRealCount();
        ImageView imageView = cachedViews.get(realPosition);
        
        if (imageView.getParent() != null) {
            ((ViewGroup) imageView.getParent()).removeView(imageView);
        }
        
        container.addView(imageView);
        return imageView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof View) {
            container.removeView((View) object);
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
