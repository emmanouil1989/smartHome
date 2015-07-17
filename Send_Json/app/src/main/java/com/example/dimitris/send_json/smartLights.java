package com.example.dimitris.send_json;

import android.content.ComponentName;
import android.content.Context;
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
import android.widget.Toast;
import com.mqttexample.ICallback;
import com.mqttexample.ISendMsg;
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
        btnTurnOnASpecificLight = (Button) findViewById(R.id.specificlight);
        btnTurnOnOffAllLights =(Button) findViewById(R.id.alllights);

        btnTurnOnOffAllLights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
        btnTurnOnASpecificLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
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
                    .replace(" City"," heating");


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
            }
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
                    URL url = new URL(stringUrl);
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.connect();
                    InputStream inputStream = new BufferedInputStream(conn.getInputStream());
                    String data = getStringfromInputStream(inputStream);
                    Log.v("JSONExample", "Data: " + data);
                    inputStream.close();


                    conn.disconnect();



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
    /*
    public static String replaceNumbers (String input) {
        String result = "";
        String[] decimal = input.split(MAGNITUDES[3]);
        String[] millions = decimal[0].split(MAGNITUDES[2]);

        for (int i = 0; i < millions.length; i++) {
            String[] thousands = millions[i].split(MAGNITUDES[1]);

            for (int j = 0; j < thousands.length; j++) {
                int[] triplet = {0, 0, 0};
                StringTokenizer set = new StringTokenizer(thousands[j]);

                if (set.countTokens() == 1) { //If there is only one token given in triplet
                    String uno = set.nextToken();
                    triplet[0] = 0;
                    for (int k = 0; k < DIGITS.length; k++) {
                        if (uno.equals(DIGITS[k])) {
                            triplet[1] = 0;
                            triplet[2] = k + 1;
                        }
                        if (uno.equals(TENS[k])) {
                            triplet[1] = k + 1;
                            triplet[2] = 0;
                        }
                    }
                }


                else if (set.countTokens() == 2) {  //If there are two tokens given in triplet
                    String uno = set.nextToken();
                    String dos = set.nextToken();
                    if (dos.equals(MAGNITUDES[0])) {  //If one of the two tokens is "hundred"
                        for (int k = 0; k < DIGITS.length; k++) {
                            if (uno.equals(DIGITS[k])) {
                                triplet[0] = k + 1;
                                triplet[1] = 0;
                                triplet[2] = 0;
                            }
                        }
                    }
                    else {
                        triplet[0] = 0;
                        for (int k = 0; k < DIGITS.length; k++) {
                            if (uno.equals(TENS[k])) {
                                triplet[1] = k + 1;
                            }
                            if (dos.equals(DIGITS[k])) {
                                triplet[2] = k + 1;
                            }
                        }
                    }
                }

                else if (set.countTokens() == 3) {  //If there are three tokens given in triplet
                    String uno = set.nextToken();
                    String dos = set.nextToken();
                    String tres = set.nextToken();
                    for (int k = 0; k < DIGITS.length; k++) {
                        if (uno.equals(DIGITS[k])) {
                            triplet[0] = k + 1;
                        }
                        if (tres.equals(DIGITS[k])) {
                            triplet[1] = 0;
                            triplet[2] = k + 1;
                        }
                        if (tres.equals(TENS[k])) {
                            triplet[1] = k + 1;
                            triplet[2] = 0;
                        }
                    }
                }

                else if (set.countTokens() == 4) {  //If there are four tokens given in triplet
                    String uno = set.nextToken();
                    String dos = set.nextToken();
                    String tres = set.nextToken();
                    String cuatro = set.nextToken();
                    for (int k = 0; k < DIGITS.length; k++) {
                        if (uno.equals(DIGITS[k])) {
                            triplet[0] = k + 1;
                        }
                        if (cuatro.equals(DIGITS[k])) {
                            triplet[2] = k + 1;
                        }
                        if (tres.equals(TENS[k])) {
                            triplet[1] = k + 1;
                        }
                    }
                }
                else {
                    triplet[0] = 0;
                    triplet[1] = 0;
                    triplet[2] = 0;
                }

                result = result + Integer.toString(triplet[0]) + Integer.toString(triplet[1]) + Integer.toString(triplet[2]);
            }
        }

        if (decimal.length > 1) {  //The number is a decimal
            StringTokenizer decimalDigits = new StringTokenizer(decimal[1]);
            result = result + ".";
            System.out.println(decimalDigits.countTokens() + " decimal digits");
            while (decimalDigits.hasMoreTokens()) {
                String w = decimalDigits.nextToken();
                System.out.println(w);

                if (w.equals(ZERO[0]) || w.equals(ZERO[1])) {
                    result = result + "0";
                }
                for (int j = 0; j < DIGITS.length; j++) {
                    if (w.equals(DIGITS[j])) {
                        result = result + Integer.toString(j + 1);
                    }
                }

            }
        }

        return result;
    }
    */

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
