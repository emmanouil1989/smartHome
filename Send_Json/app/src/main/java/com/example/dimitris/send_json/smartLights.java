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


public class smartLights extends AppCompatActivity {

    private Button btnTurnOnASpecificLight,btnTurnOnOffAllLights;
    private SharedPreferences preferences;
    private String DeviceId,DeviceType,set,name,action,state;

    private final int REQ_CODE_SPEECH_INPUT = 1000;

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
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
    }

    public void determineSenario(String text)
    {
        showToast(text);
        Set<String> data = new HashSet<String>();
        preferences = PreferenceManager.
                getDefaultSharedPreferences(smartLights.this);
        data = preferences.getStringSet("DevicesSet",null);
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

        if (text.contains("turn on") || text.contains("%"))
        {


        }
        if (text.contains("turn on") && text.contains("hallway"))
        {
            showToast(text);
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
                        String labelName = reader.getString("DeviceType");



                    }catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            }
        }
    }




    public void findKeys(JSONObject issueObj)
    {
        try
        {
            Iterator iterator = issueObj.keys();

            while(iterator.hasNext())
            {
                String key = (String) iterator.next();

                Log.v("JSONExample", "Key: " + key);
            }

        }
        catch (Exception e)
        {

        }
    }
    public void createLink (String id, String type, String funciton)
    {
     connect("http://192.168.1.100:8083/ZWaveAPI/Run/devices["+id+"].instances[0].commandClasses["+type+"].Set("+funciton+")");

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
