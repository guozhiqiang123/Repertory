package com.gzq.lib_share_umeng.share;

import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;

public  abstract class ShareListener implements UMShareListener {
    @Override
    public void onStart(SHARE_MEDIA share_media) {

    }

    @Override
    public abstract void onResult(SHARE_MEDIA share_media);

    @Override
    public abstract void onError(SHARE_MEDIA share_media, Throwable throwable);

    @Override
    public void onCancel(SHARE_MEDIA share_media) {

    }
}
