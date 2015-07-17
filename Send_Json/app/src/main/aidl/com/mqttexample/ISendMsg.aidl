// ISendMsg.aidl
package com.mqttexample;

import com.mqttexample.ICallback;


interface ISendMsg
{

    void SendMsg(String deviceNumber, String deviceName, String state, String instance);
    void SendTemp(String username, String password, String temp);
    void sendScenes(String getjson);
    void registerCallBack(ICallback cb);
    void unregisterCallBack(ICallback cb);

}