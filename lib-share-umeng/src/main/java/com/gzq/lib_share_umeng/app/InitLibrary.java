package com.gzq.lib_share_umeng.app;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import com.gzq.lib_core.base.delegate.AppLifecycle;
import com.gzq.lib_core.utils.ActivityUtils;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.bean.SHARE_MEDIA;

import timber.log.Timber;

/**
 * copyright：杭州国辰迈联机器人科技有限公司
 * version: V1.3.0
 * created on 2018/10/24 17:01
 * created by: gzq
 * description: 初始化友盟
 */
public class InitLibrary implements AppLifecycle {
    /**
     * 友盟TEST_UMENG这个应用的appkey
     */
    public static final String APP_KEY = "5bdc121cb465f5f2470001cb";
    private static final String TAG = "InitLibrary";
    //TODO:高版本上需要动态申请权限
    String[] umengPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_LOGS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SET_DEBUG_APP,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.WRITE_APN_SETTINGS};

    @Override
    public void attachBaseContext(@NonNull Context base) {

    }

    @Override
    public void onCreate(@NonNull Application application) {
        Timber.i("lib-share-umeng---->onCreate");
        UMConfigure.init(application, APP_KEY, "U-meng", UMConfigure.DEVICE_TYPE_PHONE, "");
        UMConfigure.setLogEnabled(true);


        //appid,secretkey
        PlatformConfig.setWeixin("", "");
        //appid,secretkey,回调地址
        PlatformConfig.setSinaWeibo("", "", "");
        //appid,secretkey
        PlatformConfig.setQQZone("1107947564", "wacz83yDRreZKHA0");
    }

    @Override
    public void onTerminate(@NonNull Application application) {

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

    }
}
