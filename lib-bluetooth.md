# Android项目架构探索 

## lib-bluetooth
在工作中，老大让我负责蓝牙相关功能模块的开发，所以就有了这么一个lib。这不是一个标准的蓝牙库，只是在大佬开源的[BluetoothKit](https://github.com/dingjikerbo/Android-BluetoothKit)
基础上做了二次封装，更加方便使用，同时和Lifecycle做了结合，防止内存泄漏。

## 使用
**1.建议在使用前，先学习[BluetoothKit](https://github.com/dingjikerbo/Android-BluetoothKit)**    
    
**2.初始化。**
该lib中使用到的实例BluetoothClient已经初始化，不用重复初始化。代码如下：

```java
public class BluetoothStore implements AppLifecycle {
    private static BluetoothClient client;
    @Override
    public void attachBaseContext(@NonNull Context base) {

    }

    @Override
    public void onCreate(@NonNull Application application) {
        client=new BluetoothClient(application);
    }

    @Override
    public void onTerminate(@NonNull Application application) {

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

    }
    public static BluetoothClient getClient(){
        return client;
    }
}
```
如果有想单独使用该lib，而不想依赖lib-core的朋友，可以如下这样下，并在Application中初始化。
```java
public class BluetoothStore {
    private static BluetoothClient client;

    public static void init(Application application) {
        if (client == null) {
            client = new BluetoothClient(application);
        }
    }

    public static BluetoothClient getClient() {
        if (client == null) {
            throw new NullPointerException("Please init BluetoothStore in Application");
        }
        return client;
    }
}
```
**3.搜索**   
可以同时搜索多个蓝牙name:BLe1、BLe2...
```java
BluetoothSearchHelper bluetoothSearchHelper = new BluetoothSearchHelper();
//每次8秒，搜索3次
bluetoothSearchHelper.searchBle(8000, 3, new SearchListener() {
    @Override
    public void onSearching(boolean isOn) {
        //isOn=true:正在搜索，isOn=false:停止搜索
    }

    @Override
    public void onNewDeviceFinded(BluetoothDevice newDevice) {
        //搜索到新设备
    }
    @Override
    public void obtainDevice(BluetoothDevice device) {
        //搜索到目标设备
    }

    @Override
    public void noneFind() {
        //没有搜索到目标设备
    }
    },"Ble1","Ble2");
```
停止搜索:
```java
bluetoothSearchHelper.stop();
```
一定记得在onDestroy中或者onStop中，释放资源。
```java
bluetoothSearchHelper.clear();
```
**4.连接**
```java
BluetoothConnectHelper bluetoothConnectHelper=new BluetoothConnectHelper();
bluetoothConnectHelper.connect("1B:F4:E2:8A:85:AB", new ConnectListener() {
    @Override
    public void success(BluetoothDevice device) {
        //连接成功
    }
    @Override
    public void failed() {
        //连接失败
    }
    @Override
    public void disConnect(String address) {
        //断开连接
    }
});
```
主动断开连接：
```java
bluetoothConnectHelper.disConnect();
```
一定记得在onDestroy或者onStop中
```java
bluetoothConnectHelper.clear();
```
之所以把搜索和连接分开来时，是为了在实际业务中更加灵活。如果想更加简洁，可以像如下实现：
```java
public class BluetoothPresenter extends BaseBluetooth{

    public BluetoothPresenter(IBluetoothView owner) {
        super(owner);

        //直接连接，跳过搜索过程
        start(BluetoothType.BLUETOOTH_TYPE_BLE,"1B:F4:E2:8A:85:AB");
        //或者
        start(BluetoothType.BLUETOOTH_TYPE_BLE,null,"BLE1","BLE2");
        //或者
        start(BluetoothType.BLUETOOTH_TYPE_CLASSIC,"1B:F4:E2:8A:85:AB");
        //或者
        start(BluetoothType.BLUETOOTH_TYPE_CLASSIC,null,"BLE1","BLE2");
    }

    /**
     * 当搜索到设备都会回调
     * @param device
     */
    @Override
    protected void newDeviceFinded(BluetoothDevice device) {

    }

    /**
     * 指定时间内没有发现目标设备
     */
    @Override
    protected void noneFind() {

    }

    /**
     * 连接成功
     * @param name 连接设备的蓝牙名字
     * @param address 连接设备的mac地址
     */
    @Override
    protected void connectSuccessed(String name, String address) {

    }

    /**
     * 连接失败
     */
    @Override
    protected void connectFailed() {

    }

    /**
     * 连接断开
     * @param address
     */
    @Override
    protected void disConnected(String address) {

    }
}
```
将搜索和连接合二为一，由start()方法启动，同时适配了蓝牙在Android6.0以上权限的问题。当搜索到设定的目标设备中的一个，则会立马进行连接。
剩下的工作即是在连接成功后，进行数据的notify、read、write等一系列操作。如下：
```java
//notify
 BluetoothStore.getClient().notify(address, UUID.fromString(""), UUID.fromString(""), new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                
            }

            @Override
            public void onResponse(int code) {

            }
        });
//write
 BluetoothStore.getClient().write(address, UUID.fromString(""), UUID.fromString(""), new byte[]{0x00,0x00}, new BleWriteResponse() {
             @Override
             public void onResponse(int code) {
                 
             }
         });       
```

## 小技巧
- 蓝牙比较多的环境中，不建议每次连接都去搜索。应该在连接成功一次后，将mac地址在本地缓存，下次再连接的时候，直连mac地址即可，
省去搜索的过程----即“设备绑定”
- [BluetoothKit](https://github.com/dingjikerbo/Android-BluetoothKit)内部仍然使用的是Service进行搜索操作的，我们都
知道在Service中进行耗时操作可能会卡主线程，所以建议搜索应“短时多次”,即每一次搜索的时间不宜过长
- 低功耗蓝牙虽然相比传统蓝牙耗电低了许多，单蓝牙数据通信仍然是高功耗的操作，建议在搜索到目标蓝牙的时候就停止搜索；
在数据传输完成的时候主动断开连接
- 如果是在Fragment中进行蓝牙操作，建议在onStop()中就进行资源回收。因为蓝牙断开、蓝牙开启都是耗时操作，如果该Fragment
在极短的时间内被重用，onDestroy()根本不会被回调，导致资源不被释放，造成内存泄漏。总之，在Fragment中进行蓝牙操作，
资源释放宜早不宜迟
- Android坑比较多，建议朋友们“切走切小心”

## 推荐
[FastBle](https://github.com/Jasonchenlijian/FastBle)     
这个库也挺火的，建议有快速开发蓝牙功能的朋友看看。

## 参考和感谢
[BluetoothKit](https://github.com/dingjikerbo/Android-BluetoothKit)
