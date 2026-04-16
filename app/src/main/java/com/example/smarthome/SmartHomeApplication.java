package com.example.smarthome;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.example.smarthome.Service.HomeStatusService;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

public class SmartHomeApplication extends Application {

    private static final String TAG = "SmartHomeApplication";

    private AtomicInteger activityCount = new AtomicInteger(0);
    private WeakReference<Activity> currentActivityRef;
    private boolean isInBackground = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate");

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.d(TAG, "onActivityCreated: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.d(TAG, "onActivityStarted: " + activity.getClass().getSimpleName());
                int count = activityCount.incrementAndGet();
                if (count == 1) {
                    onAppForeground();
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d(TAG, "onActivityResumed: " + activity.getClass().getSimpleName());
                currentActivityRef = new WeakReference<>(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d(TAG, "onActivityPaused: " + activity.getClass().getSimpleName());
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d(TAG, "onActivityStopped: " + activity.getClass().getSimpleName());
                int count = activityCount.decrementAndGet();
                if (count == 0) {
                    onAppBackground();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "onActivityDestroyed: " + activity.getClass().getSimpleName());
            }
        });
    }

    private void onAppForeground() {
        Log.d(TAG, "应用进入前台");
        isInBackground = false;
        HomeStatusService.stopService(this);
    }

    private void onAppBackground() {
        Log.d(TAG, "应用进入后台");
        isInBackground = true;
        HomeStatusService.startService(this);
    }

    public boolean isInBackground() {
        return isInBackground;
    }

    public Activity getCurrentActivity() {
        return currentActivityRef != null ? currentActivityRef.get() : null;
    }
}
