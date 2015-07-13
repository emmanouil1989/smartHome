package com.example.dimitris.send_json;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class Pcode extends AppCompatActivity {

    private Button btnSave;
    private EditText txtParingCode;
    private String mail,pairingcod,data;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcode);
        btnSave = (Button) findViewById(R.id.savepericode);
        txtParingCode = (EditText) findViewById(R.id.pericode);



        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.
                        getDefaultSharedPreferences(Pcode.this);
                mail = preferences.getString("username", "");
                Log.v("mail","mail: "+ mail);
                pairingcod = txtParingCode.getText().toString().trim();
                connect("http://193.63.130.184:8080/verifyPairingCode.php?Email="+mail+"&PairingCode="+pairingcod);


            }
        });
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
                    data = getStringfromInputStream(inputStream);
                    Log.v("JSONExample", "Data: " + data);
                    inputStream.close();


                    conn.disconnect();
                   SaveData();


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
    public void SaveData()
    {
        if (data.equals("Success"))
        {
            SharedPreferences preferences = PreferenceManager.
                    getDefaultSharedPreferences(Pcode.this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("pairingCode", txtParingCode.getText().toString());
            editor.apply();

            showToast("Pairing Code saved succefully");
            Intent i = new Intent(Pcode.this, getDevices.class);
            startActivity(i);
        }
        else
        {
            showToast("Please complete the correct PairingCode");
        }

    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(Pcode.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pcode, menu);
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
