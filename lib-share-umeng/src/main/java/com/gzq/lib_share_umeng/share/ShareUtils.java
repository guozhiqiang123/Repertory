package com.gzq.lib_share_umeng.share;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import com.gzq.lib_core.utils.ActivityUtils;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.utils.ShareBoardlistener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ShareUtils implements LifecycleObserver {
    private static volatile ShareUtils instance = null;
    private static Activity activity;
    private LifecycleOwner owner;
    private ShareAction shareAction;
    private List<ShareAction> shareActions = new ArrayList<>();
    private ShareBoardlistener shareBoardlistener;
    private ShareListener shareListener;

    public static ShareUtils getInstance() {
        if (instance == null) {
            synchronized (ShareUtils.class) {
                if (instance == null) {
                    instance = new ShareUtils();
                }
            }
        }
        if (activity == null) {
            activity = instance.getCurrentActivity();
        }
        return instance;
    }

    private Activity getCurrentActivity() {
        Activity activity = ActivityUtils.currentActivity();
        if (activity != null) {
            owner = ((LifecycleOwner) activity);
            owner.getLifecycle().addObserver(instance);
        }
        return activity;
    }

    public void shareBoard(ShareBoardlistener shareBoardlistener) {
        this.shareBoardlistener = shareBoardlistener;
        if (shareAction == null) {
            shareAction = new ShareAction(activity)
                    .setDisplayList(SHARE_MEDIA.QQ, SHARE_MEDIA.QZONE, SHARE_MEDIA.WEIXIN, SHARE_MEDIA.WEIXIN_CIRCLE, SHARE_MEDIA.SINA)
                    .setShareboardclickCallback(this.shareBoardlistener);
        }
        shareAction.open();
    }

    public void share2QQWithText(String msg) {
        //分享纯文字到qq好友
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "纯文字分享");
        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(new ComponentName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity"));
        activity.startActivity(intent);
    }

    public void shareImage(UMImage image, SHARE_MEDIA share_media, ShareListener shareListener) {
        this.shareListener = shareListener;
        if (!checkIsInstall(share_media)) {
            this.shareListener.onError(share_media, new Throwable("未安装该应用"));
            return;
        }
        ShareAction share = new ShareAction(activity)
                .withMedia(image)
                .setPlatform(share_media)
                .setCallback(this.shareListener);
        if (!shareActions.contains(shareAction)) {
            shareActions.add(shareAction);
        }
        share.share();
    }

    private boolean checkIsInstall(SHARE_MEDIA share_media) {
        return UMShareAPI.get(activity).isInstall(activity, share_media);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(LifecycleOwner ow) {
        Timber.i("ShareUtils>>>>>>=====>>>>onDestroy()");
        UMShareAPI.get(activity).release();
        shareActions.clear();
        if (owner != null) {
            owner.getLifecycle().removeObserver(instance);
        }
        if (shareAction != null) {
            shareAction.close();
        }
        owner = null;
        instance = null;
        shareAction = null;
        activity = null;
        shareListener = null;
        shareBoardlistener = null;
    }

}
