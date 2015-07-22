package com.example.dimitris.send_json;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.mqttexample.ICallback;
import com.mqttexample.ISendMsg;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;


public class smartLights extends AppCompatActivity {

    private Button btnTurnOnASpecificLight,btnTurnOnOffAllLights,btnGiveCommand;
    private SharedPreferences preferences;
    private String DeviceId,name,state;
    private final int REQ_CODE_SPEECH_INPUT = 1;

    ISendMsg service;
    SendMsgServiceConnection connection;
    class SendMsgServiceConnection implements ServiceConnection
    {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            service = ISendMsg.Stub.asInterface((IBinder) iBinder);
            Log.v("ARK", "Service Connected");
            try
            {

                service.registerCallBack(mCallback);
            }
            catch (Exception e)
            {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            try
            {

                service.unregisterCallBack(mCallback);
            }
            catch (Exception e)
            {
            }
            service = null;
            Log.v("ARK", "Service Disconnected");

        }
    }

    public void initService()
    {
        connection = new SendMsgServiceConnection();
        Intent i = new Intent();
        i.setClassName("com.example.dimitris.send_json", com.example.dimitris.send_json.MQTTService.class.getName());
        boolean ret = bindService(i, connection, Context.BIND_AUTO_CREATE);
        Log.v("ARK", "Am I connected? " + ret);
    }

    public void releaseService()
    {
        unbindService(connection);
        connection = null;
        Log.v("ARK", "Released Service");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_lights);


        initService();
        btnGiveCommand = (Button) findViewById(R.id.givecommand);


        btnGiveCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
    }



    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(smartLights.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Select an application");    // user hint
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);    // setting recognition model, optimized for short phrases – search queries

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && null != data) {


            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            String test = results.get(0).replace(" the ", " ").replace(" find "," fan ").replace(" fun ", " fan ").replace("%"," percent ").replace(" oil "," all ")
                    .replace(" City"," heating").replace("im ","i am ").replace(" phone", " fan");


            if(!(test.contains(" percent"))) {
                test = test.replace("percent", " percent");
            }


            if(test.contains("turn on all")){
                turnAllLights();
            }else if(test.contains("turn off all")) {
                turnoffAllLights();

            }else {
                try {
                    determineSenario(test);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void determineSenario(String text) throws RemoteException {
        showToast(text);


        if (text.contains("turn on") && !text.contains("percent") && !text.contains("heating") )
        {

            name =  text.substring(text.indexOf("on")+3);

            turnOn(text,name);

        }else {
            if (text.contains("turn off")&& !text.contains("heating")) {
                name = text.substring(text.indexOf("off") + 4);
                turnOff(text, name);
            } else if (text.contains("turn on") && text.contains("percent")) {

                showToast(text);
                String[] words = text.split("\\s+");
                state = words[words.length - 2];

                name = text.substring(text.indexOf("on") + 3, text.indexOf(state) - 1);
                turnOn(text, name);


            }else if(text.contains("turn on heating"))
            {
                turnOnHeating();
            }else if (text.contains("turn off heating"))
            {
                turnOffHeating();
            }else if(text.contains("set temperature"))
            {
                String[] words = text.split("\\s+");
               String temp = words[words.length -1];
                int result = Integer.parseInt(temp);
                if(result < 9 || result > 32)
                {
                    showToast("Set a value between 9 or 32");
                }else
                {
                    setOnHeating(temp);
                }

            }else if (text.contains("i am coming home"))
            {




                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                final EditText input = new EditText (this);
                preferences = PreferenceManager.getDefaultSharedPreferences(smartLights.this);
                String address = preferences.getString("address", "");
                input.setText(address);

                alert.setTitle("Title");
                alert.setMessage("Give me your home address");


                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        String saveAddress = input.getText().toString();
                        preferences = PreferenceManager.getDefaultSharedPreferences(smartLights.this);

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("address", saveAddress);
                        editor.apply();
                        showToast("your address saved succefully");
                        comingHome(input.getText().toString());

                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();



            }

        }

    }

    public void comingHome (String address)
    {
        connect("https://maps.googleapis.com/maps/api/directions/json?origin=treforest&destination=" + address + "&mode=transit&key=AIzaSyAjTJQlFKLXA4gpKYTEglcbSDPAf-3P2dI");
    }
    public void setOnHeating(String temperature)
    {

        try {
            service.SendTemp("moconetlabs@gmail.com", "M0C0N3tM0C0N3t", temperature);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    public void turnOnHeating() throws RemoteException {
        service.SendTemp("moconetlabs@gmail.com", "M0C0N3tM0C0N3t", "22");
    }

    public void turnOffHeating() throws RemoteException {
        service.SendTemp("moconetlabs@gmail.com", "M0C0N3tM0C0N3t", "9");
    }


    public void turnOn(String sentence,String sentenceName)
    {
        Set<String> data;
        preferences = PreferenceManager.
                getDefaultSharedPreferences(smartLights.this);
        data = preferences.getStringSet("DevicesSet",null);


        if(null != data)
        {
            //Log.v("setdata", SetData);
            for(String SetData : data) {
                try {
                    JSONObject reader = new JSONObject(SetData);
                    findKeys(reader);
                    String labelId = reader.getString("DeviceId");
                    String labelName = reader.getString("DeviceName");
                    String labelType = reader.getString("DeviceType");
                    labelName = labelName.toLowerCase();

                    if (labelName.equals(sentenceName)) {
                        if (labelType.contains("Binary")) {
                            DeviceId = labelId;
                            // createLink(DeviceId,"37","1");
                            service.SendMsg(DeviceId, labelName, "ON", "0");
                        } else if (labelType.contains("Multilevel")) {
                            String[] words = sentence.split("\\s+");
                            state = words[words.length - 2];

                            // showToast(replaceNumbers(state));
                            DeviceId = labelId;
                            service.SendMsg(DeviceId, labelName, state, "0");
                            //  createLink(DeviceId,"38",state);
                        }
                    } else {
                        // showToast("The is no device with that name");
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void turnOff(String sentence,String sentenceName)
    {
        Set<String> data = new HashSet<String>();
        preferences = PreferenceManager.
                getDefaultSharedPreferences(smartLights.this);
        data = preferences.getStringSet("DevicesSet",null);


        if(data != null)
        {
            for(String SetData : data)
            {
                //Log.v("setdata", SetData);
                try
                {
                    JSONObject reader = new JSONObject(SetData);
                    findKeys(reader);
                    String labelId = reader.getString("DeviceId");
                    String labelName = reader.getString("DeviceName");
                    String labelType = reader.getString("DeviceType");

                    labelName=labelName.toLowerCase();
                    if(labelName.equals(sentenceName))
                    {
                        if (labelType.contains("Binary"))
                        {
                            DeviceId = labelId;
                          //  createLink(DeviceId,"37","0");
                            service.SendMsg(DeviceId, labelName, "OFF","0");
                        }else if (labelType.contains("Multilevel"))
                        {

                            DeviceId = labelId;
                            service.SendMsg(DeviceId, labelName, "0","0");
                          //  createLink(DeviceId,"38","0");
                        }
                    }



                }catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }


            }
        }

    }



    public void turnAllLights()
    {
        Set<String> data = new HashSet<String>();
        preferences = PreferenceManager.
                getDefaultSharedPreferences(smartLights.this);
        data = preferences.getStringSet("DevicesSet",null);


        if(data != null)
        {
            for(String SetData : data)
            {
                //Log.v("setdata", SetData);
                try
                {
                    JSONObject reader = new JSONObject(SetData);
                    findKeys(reader);
                    String labelId = reader.getString("DeviceId");
                    String labelName = reader.getString("DeviceName");
                    String labelType = reader.getString("DeviceType");
                    labelName=labelName.toLowerCase();

                    if(labelName.contains("light"))
                    {
                        if (labelType.contains("Binary"))
                        {
                            DeviceId = labelId;
                           // createLink(DeviceId,"37","1");
                            service.SendMsg(DeviceId, labelName, "ON","0");

                        }else if (labelType.contains("Multilevel"))
                        {

                            state = "99";

                            // showToast(replaceNumbers(state));
                            DeviceId = labelId;
                           // createLink(DeviceId,"38",state);
                            service.SendMsg(DeviceId, labelName, state,"0");
                        }
                    }else
                    {
                        // showToast("The is no device with that name");
                    }



                }catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }


            }
        }
    }

    public void turnoffAllLights()
    {
        Set<String> data = new HashSet<String>();
        preferences = PreferenceManager.
                getDefaultSharedPreferences(smartLights.this);
        data = preferences.getStringSet("DevicesSet",null);


        if(data != null)
        {
            for(String SetData : data)
            {
                //Log.v("setdata", SetData);
                try
                {
                    JSONObject reader = new JSONObject(SetData);
                    findKeys(reader);
                    String labelId = reader.getString("DeviceId");
                    String labelName = reader.getString("DeviceName");
                    String labelType = reader.getString("DeviceType");
                    labelName=labelName.toLowerCase();

                    if(labelName.contains("light"))
                    {
                        if (labelType.contains("Binary"))
                        {
                            DeviceId = labelId;
                          //  createLink(DeviceId,"37","0");
                            service.SendMsg(DeviceId, labelName, "OFF","0");
                        }else if (labelType.contains("Multilevel"))
                        {

                            state = "0";

                            // showToast(replaceNumbers(state));
                            DeviceId = labelId;
                           // createLink(DeviceId,"38",state);
                            service.SendMsg(DeviceId, labelName, state,"0");
                        }
                    }else
                    {
                        // showToast("The is no device with that name");
                    }



                }catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }


            }
        }
    }



    public void connect(final String stringUrl)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    URL url = new URL(stringUrl.replace(" ", "%20"));
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoOutput(false);
                    conn.connect();
                    InputStream inputStream ;

                    int status = conn.getResponseCode();

                    if(status >= HttpStatus.SC_BAD_REQUEST)
                        inputStream = new BufferedInputStream(conn.getErrorStream());
                    else
                        inputStream = new BufferedInputStream(conn.getInputStream());

                    String data = getStringfromInputStream(inputStream);
                    Log.v("JSONExample", "Data: " + data);
                    inputStream.close();


                    conn.disconnect();
                    readAndParseJSON(data);



                }
                catch (Exception e)
                {
                    Log.v("manolis", String.valueOf(e));
                }


            }
        });
        thread.start();

    }

    public String getStringfromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (Exception e) {
            try {
                br.close();
            } catch (Exception e1) {

            }

        }return sb.toString();
    }

    public void findKeys(JSONObject issueObj)
    {
        try
        {
            Iterator iterator = issueObj.keys();

            while(iterator.hasNext())
            {
                String key = (String) iterator.next();

                Log.v("Keys", "Key: " + key);
            }

        }
        catch (Exception e)
        {

        }
    }

    public void readAndParseJSON(String in)
    {
        try
        {
            JSONObject reader = new JSONObject(in);
            findKeys(reader);






        }
        catch (Exception e)
        {
            Log.v("exception", String.valueOf(e));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_smart_lights, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private ICallback mCallback = new ICallback.Stub()
    {
        @Override
        public void CallbackMsg(String type, String value1) throws RemoteException
        {
            Log.v("ARK", type + " " +value1);

            final String gotValue = value1;


            if(type.equalsIgnoreCase("Status"))
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        Toast.makeText(smartLights.this, gotValue, Toast.LENGTH_LONG).show();
                    }
                });

            }
            else if(type.equalsIgnoreCase("Connectivity"))
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {

                        Toast.makeText(smartLights.this, gotValue, Toast.LENGTH_LONG).show();
                    }
                });

            }

        }
    };
}
