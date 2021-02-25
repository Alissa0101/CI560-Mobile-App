package com.alissa.skitracker;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private Socket mSocket;

    private String friendCode;

    private boolean watching = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        try {
            mSocket = IO.socket("http://bw373.brighton.domains:50000/");
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG + " OwO an ewwor", e.toString());
        }
        watching = getIntent().getBooleanExtra("watching", false);
        friendCode = getIntent().getStringExtra("friendCode");

        TextView tv_test = findViewById(R.id.tv_test);
        tv_test.setText("My code: " + friendCode + "\n");

        mSocket.on("confirmConnection", onConfirmConnection);
        mSocket.on("recieveLocationData", onRecieveLocationData);
        mSocket.on("watchRequest", onWatchRequest);

        mSocket.connect();



        if(watching == false){
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    JSONObject data = new JSONObject();
                    try{
                        data.put("time", Calendar.getInstance().getTime());
                        mSocket.emit("recieveNewLocationData", data);
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, e.toString());
                    }

                }
            }, 0, 1000);//put here time 1000 milliseconds=1 second

        }

    }

    private Emitter.Listener onWatchRequest = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onWatchRequest - MAP----------------");
            System.out.println(data.toString());

            TextView tv_test = findViewById(R.id.tv_test);
            tv_test.setText(tv_test.getText() + data.toString() + "\n");


            //show a confirm option to accept the watch request
            JSONObject responseData = new JSONObject();
            try{
                responseData.put("confirm", true);
                responseData.put("code", data.get("code"));
                mSocket.emit("watchRequestResponse", responseData);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.toString());
            }


        }
    };

    private Emitter.Listener onRecieveLocationData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onRecieveLocationData - MAP----------------");
            System.out.println(data.toString());

            TextView tv_test = findViewById(R.id.tv_test);
            tv_test.setText(tv_test.getText() + data.toString() + "\n");

        }
    };

    /**
     * Confirm that the connection has been made and send the device code
     */
    private Emitter.Listener onConfirmConnection = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onConfirmConnection - MAP----------------");
            System.out.println(data.toString());

            // the connection has been confirmed so send the id
            String id = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);
            mSocket.emit("recieveDeviceID", id.substring(0, 8)); // send the first half of the device id
            mSocket.emit("recieveCanBeJoined", !watching);
            if(watching == true){
                mSocket.emit("recieveWatchingCode", getIntent().getStringExtra("watchingCode"));
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(watching == true){
            mSocket.emit("recieveStopWatching");
        }
        mSocket.disconnect();
        mSocket.close();
    }


}
