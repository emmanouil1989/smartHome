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

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class getDevices extends AppCompatActivity {

    private Button btnGetDevices;
    private String pairingcode,data,two,three,eleven,twelve;
    private ArrayList<String> devicesNumber = new ArrayList<>();
    private HashSet<String> DevicesDataJsonpairs = new HashSet<String>();
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_devices);

        btnGetDevices =(Button) findViewById(R.id.getdevices);


        btnGetDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.
                        getDefaultSharedPreferences(getDevices.this);
                pairingcode = preferences.getString("pairingCode", "");


                getDevicesJson("http://193.63.130.184:8080/getDevices.php?pairingCode=" + pairingcode);
            }
        });


    }

    public void getDevicesJson(final String stringUrl)
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
                    data = getStringfromInputStream(inputStream);
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

    public void readAndParseJSON(String in)
    {
        try
        {
            JSONObject reader = new JSONObject(in);
            findKeys(reader);

            Iterator<String> DevicesNumberListItarator = devicesNumber.iterator();

            while (DevicesNumberListItarator.hasNext()) {
                String devices = (String) DevicesNumberListItarator.next();
              //  Log.v("numbers",devices);
                JSONObject deviceValues = reader.getJSONObject(devices);

                String DevideTypes = deviceValues.getString("DeviceType");

                if (DevideTypes.contains("Switch"))
                {
                    switch (devices) {
                        case "3":
                            three = "{\n" +
                                    "      \"PairingCode\":\"395683343\",\n" +
                                    "      \"DeviceId\":\"3\",\n" +
                                    "      \"DeviceName\":\"Fan\",\n" +
                                    "      \"DeviceType\":\"Routing Multilevel Switch\",\n" +
                                    "      \"LastReported\":\"2015-07-08 16:42:41\",\n" +
                                    "      \"Battery\":\"no battery\",\n" +
                                    "      \"Instances\":\"0\"\n" +
                                    "   }";
                            DevicesDataJsonpairs.add(three);

                            break;
                        case "2":
                            two = "{\n" +
                                    "      \"PairingCode\":\"395683343\",\n" +
                                    "      \"DeviceId\":\"2\",\n" +
                                    "      \"DeviceName\":\"Hallway\",\n" +
                                    "      \"DeviceType\":\"Binary Power Switch\",\n" +
                                    "      \"LastReported\":\"2015-07-08 16:42:31\",\n" +
                                    "      \"Battery\":\"no battery\",\n" +
                                    "      \"Instances\":\"0\"\n" +
                                    "   }";
                            DevicesDataJsonpairs.add(two);

                            break;
                        case "11":
                            eleven = "{\n" +
                                    "      \"PairingCode\":\"395683343\",\n" +
                                    "      \"DeviceId\":\"11\",\n" +
                                    "      \"DeviceName\":\"Living Room\",\n" +
                                    "      \"DeviceType\":\"Binary Power Switch\",\n" +
                                    "      \"LastReported\":\"2015-07-08 16:43:02\",\n" +
                                    "      \"Battery\":\"no battery\",\n" +
                                    "      \"Instances\":\"0\"\n" +
                                    "   }";
                            DevicesDataJsonpairs.add(eleven);
                            break;
                        case "12":
                            twelve = "{\n" +
                                    "      \"PairingCode\":\"395683343\",\n" +
                                    "      \"DeviceId\":\"12\",\n" +
                                    "      \"DeviceName\":\"\",\n" +
                                    "      \"DeviceType\":\"Routing Multilevel Switch\",\n" +
                                    "      \"LastReported\":\"0000-00-00 00:00:00\",\n" +
                                    "      \"Battery\":\"no battery\",\n" +
                                    "      \"Instances\":\"0\"\n" +
                                    "   }";
                            DevicesDataJsonpairs.add(twelve);
                            break;
                    }
                   //    Log.v("types",devices);
                    //Log.v("types",DevideTypes);

                }

            }

            preferences = PreferenceManager.
                    getDefaultSharedPreferences(getDevices.this);
            editor = preferences.edit();
            editor.putStringSet("DevicesSet",DevicesDataJsonpairs);
            editor.commit();


            Intent i = new Intent(getDevices.this, smartLights.class);
            startActivity(i);


        }
        catch (Exception e)
        {
             Log.v("exception", String.valueOf(e));
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
                devicesNumber.add(key);
                Log.v("JSONExample", "Key: " + key);
            }

        }
        catch (Exception e)
        {

        }
    }

    public String getsaveData()
        {
            Set<String> data = new HashSet<String>();
                    data = preferences.getStringSet("DevicesSet",null);
            if(data != null)
            {
                for(String SetData : data)
                {
                    Log.v("setdata",SetData);
                    return SetData;
                }
            }
            return " ";


        }




    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getDevices.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_get_devices, menu);
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
