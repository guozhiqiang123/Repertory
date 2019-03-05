package com.gzq.lib_pay.wechat;

import android.content.Context;

import com.google.gson.Gson;
import com.gzq.lib_core.base.Box;
import com.gzq.lib_core.utils.ToastUtils;
import com.gzq.lib_pay.OnSuccessAndErrorListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;
import me.jessyan.retrofiturlmanager.RetrofitUrlManager;


public class WechatPayUtils {
    //微信统一下单接口
    public static final String WX_TOTAL_ORDER = "https://api.mch.weixin.qq.com/";

    /**
     * 商户发起生成预付单请求
     * 为了安全，本操作应该放在服务器端进行。如果后台人员太懒，也只能放在客户端进行
     * @param mContext
     * @param appid
     * @param mch_id 微信支付分配的商户号
     * @param wx_private_key 私钥
     * @param wechatModel
     * @param notifyUrl 支付结果异步通知地址（接收微信支付异步通知回调地址，通知url必须为直接可访问的url，不能携带参数。）
     * @param OnSuccessAndErrorListener
     */
    public static void wechatPayUnifyOrder(final Context mContext, final String appid, final String mch_id, final String wx_private_key, WechatModel wechatModel,String notifyUrl, final OnSuccessAndErrorListener OnSuccessAndErrorListener) {
        //随机码
        String nonce_str = getRandomStringByLength(8);
        //商品描述
        String body = wechatModel.getDetail();
        //商品订单号
        String out_trade_no = wechatModel.getOut_trade_no();
        //商品编号
        String product_id = wechatModel.getOut_trade_no();
        //总金额 分
        String total_fee = wechatModel.getMoney();
        //交易起始时间(订单生成时间非必须)
        String time_start = getCurrTime();
        //App支付
        String trade_type = "APP";

        //TODO:接收微信支付异步通知回调地址，通知url必须为直接可访问的url，不能携带参数。
        String notify_url = notifyUrl;
        SortedMap<String, String> params = new TreeMap<String, String>();
        //申请的appid
        params.put("appid", appid);
        //商户号
        params.put("mch_id", mch_id);
        //设备号 终端设备号(门店号或收银设备ID)，默认请传"WEB"
        params.put("device_info", "WEB");
        //随机字符串，不长于32位
        params.put("nonce_str", nonce_str);
        //商品描述
        params.put("body", body);
        //商户订单号(一定要唯一)
        params.put("out_trade_no", out_trade_no);
        //商品号
        params.put("product_id", product_id);
        //总金额 单位：分
        params.put("total_fee", total_fee);
        //发起支付时候的时间戳
        params.put("time_start", time_start);
        //支付类型
        params.put("trade_type", trade_type);
        //支付异步回调页面
        params.put("notify_url", notify_url);
        //签名（使用官方的签名工具生成 https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=4_3）
        String sign = getSign(params, wx_private_key);
        //参数xml化
        String xmlParams = parseString2Xml(params, sign);
        //动态切换地址
        RetrofitUrlManager.getInstance().putDomain("weixin", WX_TOTAL_ORDER);
        //TODO:返回类型上可能有很大问题，需要实测
        Box.getRetrofit(WXService.class)
                .totalOrder(xmlParams)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<Object>() {
                    @Override
                    public void onNext(Object object) {
                        if (object == null) {
                            ToastUtils.showShort("发起预付单失败");
                            return;
                        }
                        String resultString = object.toString();
                        if (resultString.contains("FAIL")) {
                            ToastUtils.showShort("发起预付单失败");
                            return;
                        }
                        Map<String, String> mapXml = null;
                        try {
                            mapXml = getMapFromXML(resultString);
                        } catch (ParserConfigurationException | IOException | SAXException e) {
                            e.printStackTrace();
                        }
                        //下单成功之后发起支付
                        String prepayId = mapXml.get("prepay_id");
                        wechatPayApp(mContext, appid, mch_id, wx_private_key, prepayId, OnSuccessAndErrorListener);
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.showShort("发起预付单失败");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 服务器端生成sign
     *
     * @param mContext  上下文
     * @param appId     appId
     * @param partnerId 商户号
     * @param prepayId  预支付交易会话ID
     * @param noncestr  不超32位的随机字符串
     * @param timestamp 发起支付的时间戳
     * @param sign      签名
     * @param onRxHttp
     */
    public static void wechatPayApp(Context mContext, String appId, String partnerId, String prepayId, String noncestr, String timestamp, String sign, OnSuccessAndErrorListener onRxHttp) {

        WechatPayModel wechatPayModel = new WechatPayModel(
                appId,
                partnerId,
                prepayId,
                "Sign=WXPay",
                noncestr,
                timestamp,
                sign);

        String pay_param=Box.getGson().toJson(wechatPayModel);
        WechatPayUtils.doWXPay(mContext, appId, pay_param, onRxHttp);
    }

    /**
     * 自己生成sign
     *
     * @param mContext     上下文
     * @param appId        appid
     * @param partnerId    商户号
     * @param wxPrivateKey 私钥
     * @param prepayId     预支付交易会话ID
     * @param onRxHttp
     */
    public static void wechatPayApp(Context mContext, String appId, String partnerId, String wxPrivateKey, String prepayId, OnSuccessAndErrorListener onRxHttp) {
        SortedMap<String, String> params = new TreeMap<String, String>();
        params.put("appid", appId);
        params.put("noncestr", getRandomStringByLength(8).toUpperCase());
        params.put("package", "Sign=WXPay");
        params.put("partnerid", partnerId);
        params.put("prepayid", prepayId);
        params.put("timestamp", getCurrTime());
        String sign = getSign(params, wxPrivateKey);
        WechatPayModel wechatPayModel = new WechatPayModel(
                appId,
                partnerId,
                prepayId,
                params.get("package"),
                params.get("noncestr"),
                params.get("timestamp"),
                sign);
        String pay_param = new Gson().toJson(wechatPayModel);
        WechatPayUtils.doWXPay(mContext, appId, pay_param, onRxHttp);
    }

    /**
     * 参数进行XML化
     *
     * @param map,sign
     * @return
     */
    private static String parseString2Xml(Map<String, String> map, String sign) {
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        Set es = map.entrySet();
        Iterator iterator = es.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            sb.append("<" + k + ">" + v + "</" + k + ">");
        }
        sb.append("<sign>" + sign + "</sign>");
        sb.append("</xml>");
        return sb.toString();
    }


    /**
     * 获取签名 md5加密(微信支付必须用MD5加密)
     * 获取支付签名
     *
     * @param params
     * @return
     */
    private static String getSign(SortedMap<String, String> params, String wxPrivateKey) {
        String sign = null;
        StringBuffer sb = new StringBuffer();
        Set es = params.entrySet();
        Iterator iterator = es.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String k = (String) entry.getKey();
            String v = (String) entry.getValue();
            if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k + "=" + v + "&");
            }
        }
        sb.append("key=" + wxPrivateKey);
        sign = MD5.getMessageDigest(sb.toString().getBytes()).toUpperCase();
        return sign;
    }

    /**
     * 获取一定长度的随机字符串
     *
     * @param length 指定字符串长度
     * @return 一定长度的字符串
     */
    private static String getRandomStringByLength(int length) {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    /**
     * 获取当前时间 yyyyMMddHHmmss
     *
     * @return String
     */
    private static String getCurrTime() {
        Date now = new Date();
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String s = outFormat.format(now);
        return s;
    }

    //xml解析
    private static Map<String, String> getMapFromXML(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        //这里用Dom的方式解析回包的最主要目的是防止API新增回包字段
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = new ByteArrayInputStream(xmlString.getBytes());
        Document document = builder.parse(is);
        //获取到document里面的全部结点
        NodeList allNodes = document.getFirstChild().getChildNodes();
        Node node;
        Map<String, String> map = new HashMap<String, String>();
        int i = 0;
        while (i < allNodes.getLength()) {
            node = allNodes.item(i);
            if (node instanceof Element) {
                map.put(node.getNodeName(), node.getTextContent());
            }
            i++;
        }
        return map;
    }


    private static void doWXPay(Context mContext, String wx_appid, String pay_param, final OnSuccessAndErrorListener onRxHttpString) {
        //要在支付前调用
        WechatPay.init(mContext, wx_appid);
        WechatPay.getInstance().doPay(pay_param, new WechatPay.WXPayResultCallBack() {
            @Override
            public void onSuccess() {
                ToastUtils.showShort("微信支付成功");
                onRxHttpString.onSuccess("微信支付成功");
            }

            @Override
            public void onError(int error_code) {
                switch (error_code) {
                    case WechatPay.NO_OR_LOW_WX:
                        ToastUtils.showShort("未安装微信或微信版本过低");
                        onRxHttpString.onError("未安装微信或微信版本过低");
                        break;

                    case WechatPay.ERROR_PAY_PARAM:
                        ToastUtils.showShort("参数错误");
                        onRxHttpString.onError("参数错误");
                        break;

                    case WechatPay.ERROR_PAY:
                        ToastUtils.showShort("支付失败");
                        onRxHttpString.onError("支付失败");
                        break;
                }
            }

            @Override
            public void onCancel() {
                ToastUtils.showShort("支付取消");
                onRxHttpString.onError("支付取消");
            }
        });
    }
}
