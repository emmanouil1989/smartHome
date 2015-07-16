// ICallback.aidl
package com.mqttexample;

// Declare any non-default types here with import statements

interface ICallback
{
    void CallbackMsg(String type, String value1);
}
