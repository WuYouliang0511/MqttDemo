package com.hikvision.mqtt;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by siboasi on 2021/7/17.
 */

public class MyMqttService extends Service {


    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public final   String             TAG            = "MyMqttService";
    private static MqttAsyncClient    mMqttAsyncClient;
    private        MqttConnectOptions mMqttConnectOptions;
    public         String             HOST           = "tcp://ne0c0100.cn-hangzhou.emqx.cloud:15247";//服务器地址（协议+地址+端口号）
    public         String             USERNAME       = "15355464308";//用户名
    public         String             PASSWORD       = "Wyl19970511../";//密码
    public static  String             PUBLISH_TOPIC  = "发布主题";//发布主题
    public static  String             RESPONSE_TOPIC = "abc";//响应主题
    public         String             userid         = "android redmi note 10"; //用户ID，唯一标识符

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            init();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 开启服务
     */
    public static void startService(Context mContext) {
        mContext.startService(new Intent(mContext, MyMqttService.class));
    }

    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
    public static void publish(String message) {
        String topic = "a";
        int qos = 2;
        boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mMqttAsyncClient.publish(topic, message.trim().getBytes(), qos, retained);
            Log.d("tag", PUBLISH_TOPIC + ":" + message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 响应 （收到其他客户端的消息后，响应给对方告知消息已到达或者消息有问题等）
     *
     * @param message 消息
     */
    public void response(String message) {
        String topic = RESPONSE_TOPIC;
        int qos = 0;
        boolean retained = false;
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mMqttAsyncClient.publish(topic, message.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init() throws MqttException {
        String serverURI = HOST; //服务器地址（协议+地址+端口号）
        mMqttAsyncClient = new MqttAsyncClient(serverURI, userid, new MemoryPersistence());
        mMqttAsyncClient.setCallback(mMqttCallback); //设置监听订阅消息的回调
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(60); //设置超时时间，单位：秒
        mMqttConnectOptions.setKeepAliveInterval(60); //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions.setUserName(USERNAME); //设置用户名
        mMqttConnectOptions.setPassword(PASSWORD.toCharArray()); //设置密码
        // last will message
        boolean doConnect = true;
        String disconnect = "{\"disconnect\":\"" + userid + "\"}";
        String topic = "disconnect";

        int qos = 2;//最好不要为0  可以是1 2
        boolean retained = false;
        // 断开上一次的连接。没有的话  可以去掉
        try {
            //设置遗嘱消息（当客户端断开连接时，会发送给服务端）
            //1.服务端发生I/O错误，或者网络失败
            //2.客户端在定义的心跳时期失联
            //3.客户端在发送下线包之前关闭网络连接
            //4.服务端在收到下线包之前关闭网络连接
            //注意：遗嘱消息在客户端正常调用disconnect方法之后并不会被发送
            mMqttConnectOptions.setWill(topic, disconnect.getBytes(), qos, retained);
        } catch (Exception e) {
            Log.d(TAG, "Exception Occured", e);
            doConnect = false;
            mMqttActionListener.onFailure(null, e);
        }
        if (doConnect) {
            doClientConnection();
        }
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mMqttAsyncClient.isConnected() && isConnectIsNomarl()) {
            try {
                Log.d(TAG, "正在连接");
                mMqttAsyncClient.connect(mMqttConnectOptions, null, mMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.d(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.d(TAG, "没有可用网络");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            new Handler().postDelayed(this::doClientConnection, 3000);
            return false;
        }
    }

    //MQTT是否连接成功的监听
    private final IMqttActionListener mMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken token) {
            Log.d(TAG, "-----------连接成功 ");

            Log.d(TAG, "isComplete: " + token.isComplete());
            Log.d(TAG, "Client: " + (token.getClient() == mMqttAsyncClient));
//            try {
//                mMqttAsyncClient.subscribe("abc", 1);//订阅主题，参数：主题、服务质量  qos最好不要为0
//                Log.d(TAG, "--------------订阅成功 ");
            publish("我是啊");
//            } catch (MqttException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            Log.d(TAG, "连接失败 ");
        }
    };

    //订阅主题的回调
    private final MqttCallback mMqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, @NonNull MqttMessage message) {
            Log.d(TAG, "收到消息： " + new String(message.getPayload()));
            Log.d(TAG, "messageArrived: " + message);
        }

        @Override
        public void deliveryComplete(@NonNull IMqttDeliveryToken token) {
            Log.d(TAG, "消息发送完成: ");
            try {
                if (token.getMessage() != null) {

                    Log.d(TAG, new String(token.getMessage().getPayload()));
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void connectionLost(@NonNull Throwable arg0) {
            Log.d(TAG, "连接断开: " + arg0);
            //因为是在service中，不是UI主线程
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "连接断开", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    public void onDestroy() {
        try {
            mMqttAsyncClient.disconnect(); //断开连接
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}