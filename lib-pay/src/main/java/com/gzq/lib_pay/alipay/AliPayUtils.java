package com.gzq.lib_pay.alipay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alipay.sdk.app.PayTask;
import com.gzq.lib_pay.OnSuccessAndErrorListener;

import java.util.Map;


public class AliPayUtils {

    private static final int SDK_PAY_FLAG = 1;

    private static OnSuccessAndErrorListener sOnSuccessAndErrorListener;
    @SuppressLint("HandlerLeak")
    private static Handler mHandler = new Handler() {
        @Override
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);

                    //对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                    // 同步返回需要验证的信息
                    String resultInfo = payResult.getResult();
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        sOnSuccessAndErrorListener.onSuccess(resultStatus);
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        //8000正在处理中；4000订单支付失败；5000重复请求；6001取消支付；6002网络出错
                        sOnSuccessAndErrorListener.onError(resultStatus);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 服务器端生成订单字符串
     *
     * @param activity
     * @param orderInfo 服务器端生成的订单字符串
     * @param onRxHttp1
     */
    public static void aliPay(final Activity activity, String orderInfo, OnSuccessAndErrorListener onRxHttp1) {
        sOnSuccessAndErrorListener = onRxHttp1;
        toPay(activity, orderInfo);
    }

    /**
     * 在客服端进行签名
     *
     * @param activity
     * @param appid
     * @param isRsa2             是否RSA2算法进行签名
     * @param alipay_rsa_private 私钥
     * @param aliPayModel        支付实体
     * @param onRxHttp1
     */
    public static void aliPay(final Activity activity, String appid, boolean isRsa2, String alipay_rsa_private, AliPayModel aliPayModel,String notifyUrl, OnSuccessAndErrorListener onRxHttp1) {
        sOnSuccessAndErrorListener = onRxHttp1;
        Map<String, String> params = AliPayOrderUtils.buildOrderParamMap(
                appid,
                isRsa2,
                aliPayModel.getOutTradeNo(),
                aliPayModel.getTitle(),
                aliPayModel.getMoney(),
                aliPayModel.getDetail(),
                notifyUrl);

        String orderParam = AliPayOrderUtils.buildOrderParam(params);

        String privateKey = alipay_rsa_private;
        String sign = AliPayOrderUtils.getSign(params, privateKey, isRsa2);
        final String orderInfo = orderParam + "&" + sign;

        toPay(activity, orderInfo);
    }

    private static void toPay(final Activity activity, final String orderInfo) {
        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask alipay = new PayTask(activity);
                Map<String, String> result = alipay.payV2(orderInfo, true);
                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

}
