package com.sch.share;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by StoneHui on 2018/12/12.
 * <p>
 * Activity 生命周期回调适配器。
 */
public class ActivityLifecycleCallbacksAdapter implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

}