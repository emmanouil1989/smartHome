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
import java.net.URLConnection;


public class MainActivity extends AppCompatActivity {
    private Button btnLogin;
    private EditText txtUser,txtPass;
    private String username,password,data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogin = (Button) findViewById(R.id.login);
        txtUser =(EditText) findViewById(R.id.username);
        txtPass =(EditText) findViewById(R.id.password);
        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(MainActivity.this);
       String mail = preferences.getString("username", "");
        txtUser.setText(mail);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = txtUser.getText().toString().trim();
                password = txtPass.getText().toString().trim();
                if (username.length() == 0 || password.length() == 0)
                {
                    Toast.makeText(MainActivity.this, "Complete all the fields", Toast.LENGTH_LONG)
                            .show();

                }else
                {

                        connect("http://193.63.130.184:8080/logIn.php?Email=" + username + "&Password=" + password);
                }
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

    public String getStringfromInputStream(InputStream is)
    {
        BufferedReader br=null;
        StringBuilder sb = new StringBuilder();
        String line;

        try
        {
            br = new BufferedReader(new InputStreamReader(is));
            while((line=br.readLine())!=null)
            {
                sb.append(line);
            }

        }
        catch (Exception e)
        {
            try
            {
                br.close();
            }
            catch (Exception e1)
            {

            }

        }return sb.toString();
    }
    public void SaveData()
    {
        if (data.equals("Success"))
        {
            SharedPreferences preferences = PreferenceManager.
                    getDefaultSharedPreferences(MainActivity.this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("username", txtUser.getText().toString());
            editor.apply();


            Intent i = new Intent(MainActivity.this, Pcode.class);
            startActivity(i);
        }
        else
        {
            showToast("Please complete the correct details");
        }

    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
