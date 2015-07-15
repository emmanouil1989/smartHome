package com.example.dimitris.send_json;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
import java.util.StringTokenizer;


public class smartLights extends AppCompatActivity {

    private Button btnTurnOnASpecificLight,btnTurnOnOffAllLights;
    private SharedPreferences preferences;
    private String DeviceId,DeviceType,DeviceName,set,name,action,state;
    public static final String[] DIGITS = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
    public static final String[] TENS = {null, "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
    public static final String[] TEENS = {"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
    public static final String[] MAGNITUDES = {"hundred", "thousand", "million", "point"};
    public static final String[] ZERO = {"zero", "oh"};

    private final int REQ_CODE_SPEECH_INPUT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_lights);

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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);    // setting recognition model, optimized for short phrases – search queries

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == REQ_CODE_SPEECH_INPUT && resultCode == RESULT_OK && null != data) {


            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String test = results.get(0);
            determineSenario(test);
            // Do something with spokenText
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void determineSenario(String text)
    {
        showToast(text);

// split the string in words "turn on" "name" "stat-on/off/%"
        /*
        String action = "turn on";
        String name = ""fan";
        String state = "50%"
        */

        //based on the string ACTION i will cain function: ON or OFF or %
        //faction--ON  or OFF or %


        /*
        you pass all 3 string
        search in the json for the device type
        search in the json for the name of the device
        get the device id
        create the URL

         */

        if (text.contains("turn on") && !text.contains("percent") )
        {

            //String[] words = text.split("\\s+");
            name =  text.substring(text.indexOf("on")+3);

            turnOn(text,name);

        }else if (text.contains("turn off"))
        {
            name =  text.substring(text.indexOf("off")+4);
            turnOff(text,name);
        }else if (text.contains("turn on") && text.contains("percent"))
        {

            showToast(text);
            String[] words = text.split("\\s+");
            state = words[words.length - 2];

            name =  text.substring(text.indexOf("on") + 3, text.indexOf(state)-1);
            turnOn(text,name);


        }

    }




    public void turnOn(String sentence,String sentenceName)
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
                            createLink(DeviceId,"37","1");
                        }else if (labelType.contains("Multilevel"))
                        {
                            String[] words = sentence.split("\\s+");
                            state = words[words.length - 2];

                          // showToast(replaceNumbers(state));
                            DeviceId = labelId;
                           createLink(DeviceId,"38",state);
                        }
                    }else
                    {
                       // showToast("The is no device with that name");
                    }



                }catch (JSONException e) {
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
                            createLink(DeviceId,"37","0");
                        }else if (labelType.contains("Multilevel"))
                        {

                            DeviceId = labelId;
                            createLink(DeviceId,"38","0");
                        }
                    }



                }catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }

    }

    public void createLink (String id,String classes, String funciton)
    {
     connect("http://192.168.1.100:8083/ZWaveAPI/Run/devices[" + id + "].instances[0].commandClasses["+classes+"].Set(" + funciton + ")");

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
}
