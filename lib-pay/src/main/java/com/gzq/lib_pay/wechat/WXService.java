package com.gzq.lib_pay.wechat;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface WXService {
    /**
     * 微信统一下单接口
     * @return
     */
    @Headers({"Domain-Name: wexin"})
    @POST("pay/unifiedorder")
    Observable<Object> totalOrder(@Body String pa);
}
