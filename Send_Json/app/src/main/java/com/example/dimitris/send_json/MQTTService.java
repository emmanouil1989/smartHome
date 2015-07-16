package com.example.dimitris.send_json;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.mqttexample.ICallback;
import com.mqttexample.ISendMsg;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;
import org.json.JSONObject;

import java.util.Iterator;

public class MQTTService extends Service implements MqttCallback
{
    MqttClient client;
    PowerManager.WakeLock wakeLock;
    public static RemoteCallbackList<ICallback> mCallbacks = new RemoteCallbackList<ICallback>();

    String paring = "395683343";



    public MQTTService()
    {
        Log.v("ARK", "In constructor");

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.v("ARK", "In Bind");


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, "My wakelock");
        wakeLock.acquire();



        reconnect();



        return new ISendMsg.Stub()
        {

            @Override
            public void SendMsg(String deviceNumber, String deviceName, String state, String instance) throws RemoteException
            {
                Log.v("ARK","In SendMsg:  " + state);

                try
                {
                    MqttMessage message = new MqttMessage();
                    JSONObject json = new JSONObject();
                    JSONObject mPayload = new JSONObject();

                    mPayload.put("devicenumber",deviceNumber);
                    mPayload.put("devicename",deviceName);
                    mPayload.put("devicestate",state);
                    mPayload.put("instance",instance);

                    json.put("control",mPayload);
                    message.setPayload(json.toString().getBytes());


                    //String myTopic = "978686423"; //Gabriel's Pairing Code
                    //String myTopic = "111111111";
                    String myTopic = paring;
                    MqttTopic topic = client.getTopic(myTopic);
                    topic.publish(message);
                }
                catch (Exception e)
                {
                    Log.v("ARK", "SendMsg Exception: " + e.toString());
                }

            }

            @Override
            public void SendTemp(String username, String password, String temp) throws RemoteException
            {
                Log.v("ARK","In SendTemp:  " + temp);

                try
                {
                    MqttMessage message = new MqttMessage();
                    JSONObject json = new JSONObject();
                    JSONObject mPayload = new JSONObject();

                    mPayload.put("deviceaccount",username);
                    mPayload.put("devicepassword",password);
                    mPayload.put("devicetemp",temp);


                    json.put("nest",mPayload);
                    message.setPayload(json.toString().getBytes());


                    //String myTopic = "978686423"; //Gabriel's Pairing Code
                    //String myTopic = "111111111";
                    String myTopic = paring;
                    MqttTopic topic = client.getTopic(myTopic);
                    topic.publish(message);
                }
                catch (Exception e)
                {
                    Log.v("ARK", "SendMsg Exception: " + e.toString());
                }
            }

            @Override
            public void sendScenes(String getjson) throws RemoteException
            {
                try {
                    MqttMessage message = new MqttMessage();
                    message.setPayload(getjson.toString().getBytes());
                    String myTopic = paring;
                    MqttTopic topic = client.getTopic(myTopic);
                    topic.publish(message);
                }
                catch (Exception e)
                {

                }


            }

            @Override
            public void registerCallBack(ICallback cb) throws RemoteException
            {
                if(cb!=null)
                {
                    Log.v("ARK", "Register Callback in Service");
                    mCallbacks.register(cb);
                }

            }

            @Override
            public void unregisterCallBack(ICallback cb) throws RemoteException
            {
                if(cb!=null)
                {
                    Log.v("ARK", "Unregister Callback in Service");
                    boolean flag = mCallbacks.unregister(cb);
                    Log.v("ARK", "Is unregister Callback in Service Successful: " + flag);
                }
            }


        };


    }

    public void reconnect()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                    String imei = telephonyManager.getDeviceId();

                    Log.v("ARK","IMEI: " + imei);

                    Log.v("ARK","In reconnect");
                    MemoryPersistence persistence = new MemoryPersistence();
                    client = new MqttClient("tcp://193.63.130.184:8090", imei, persistence); //replace 1234 with IMEI number
                    client.connect();
                    client.setCallback(MQTTService.this);
                    client.subscribe(paring); //Office Pi Pairing Code
                    //client.subscribe("111111111");



                }
                catch (Exception e)
                {
                    Log.v("ARK","Exception in Service " + e.toString());

                    int N = mCallbacks.beginBroadcast();

                    for(int i=0;i<N;i++)
                    {
                        try
                        {
                            mCallbacks.getBroadcastItem(i).CallbackMsg("Connectivity","Connection Lost. Attempting to reconnect in 10 seconds.");
                        } catch (RemoteException e1)
                        {
                            e.printStackTrace();
                        }
                    }
                    mCallbacks.finishBroadcast();



                    try
                    {
                        Thread.sleep(10000);
                    }
                    catch (Exception e1)
                    {

                    }
                    reconnect();
                }
            }
        });

        thread.start();

    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
        //wakeLock.release();
    }


    @Override
    public void connectionLost(Throwable throwable)
    {
        Log.v("ARK","Connection Lost");
        reconnect();

    }

    @Override
    public void messageArrived(MqttTopic mqttTopic, MqttMessage mqttMessage) throws MqttException
    {
        Log.v("ARK", "Message Arrived: " + new String(mqttMessage.getPayload()));

        String payload = new String(mqttMessage.getPayload());

        try
        {

            JSONObject reader = new JSONObject(payload);
            Iterator iterator = reader.keys();
            String key="";
            while (iterator.hasNext())
            {
                key = (String) iterator.next();
                //JSONObject issue = issueObj.getJSONObject(key);
                Log.v("ARK", "Key: " + key);
            }

            if(key.equalsIgnoreCase("status"))
            {
                try {
                    String devicename = reader.getJSONObject("status").getString("devicename");
                    String devicestate = reader.getJSONObject("status").getString("devicestate");
                    Log.v("ARK", "Device Name: " + devicename + " " + devicestate);

                    int N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; i++) {
                        mCallbacks.getBroadcastItem(i).CallbackMsg("Status", devicename + " is set to " + devicestate);

                    }
                    mCallbacks.finishBroadcast();
                }
                catch (Exception e)
                {
                }
            }

            else if(key.equalsIgnoreCase("neststatus"))
            {
                try {
                    String msg = reader.getJSONObject("neststatus").getString("msg");
                    Log.v("ARK", "Nest Message: " + msg);

                    int N = mCallbacks.beginBroadcast();

                    for (int i = 0; i < N; i++) {
                        mCallbacks.getBroadcastItem(i).CallbackMsg("Status", msg);

                    }
                    mCallbacks.finishBroadcast();
                }
                catch (Exception e)
                {

                }

            }


        }
        catch (Exception e)
        {

        }






        if(new String(mqttMessage.getPayload()).contains("Alert")) {

            final Notification.Builder builder = new Notification.Builder(this);
            builder.setStyle(new Notification.BigTextStyle(builder)
                    .bigText(new String(mqttMessage.getPayload()))
                    .setBigContentTitle("HEMS"))
                    .setContentTitle("HEMS")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon);

            final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(0, builder.build());
        }


    }

    @Override
    public void deliveryComplete(MqttDeliveryToken mqttDeliveryToken)
    {
        Log.v("ARK","Delivery complete");

    }
}
