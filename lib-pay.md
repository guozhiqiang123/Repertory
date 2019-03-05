# Android项目架构探索 

## lib-pay
支付涉及到公司财产，所以在开发过程中，安全性、信息完整性、实时性都很重要。虽然支付宝和微信都支持在移动端签名，如果
不是和后台开发水火不容，一定要劝后台开发这一步让他来做。不是为了省事，而是为了安全！！！

## 使用
使用该lib的时候，去官网查一查官方的引用或者jar版本号，看看有没有更新，如果有，建议替换成最新的。
```java
//支付宝
AliPayUtils.aliPay(this, "后台获取的订单信息", new OnSuccessAndErrorListener() {
            @Override
            public void onSuccess(String s) {

            }

            @Override
            public void onError(String s) {

            }
        });
//微信
WechatPayUtils.wechatPayApp(this, "AppId", "商户号", "私钥",
                "预支付交易会话ID", "发起支付的时间戳", "签名", new OnSuccessAndErrorListener() {
                    @Override
                    public void onSuccess(String s) {
                        
                    }

                    @Override
                    public void onError(String s) {

                    }
                });
```