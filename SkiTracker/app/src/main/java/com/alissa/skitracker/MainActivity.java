package com.alissa.skitracker;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://bw373.brighton.domains:50000/");
        } catch (URISyntaxException e) {
            Log.e("OwO an ewwor", e.toString());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSocket.on("confirmConnection", onConfirmConnection);
        mSocket.on("recieveFriendsStatus", onRecieveFriendsStatus);
        mSocket.connect();

    }

    private Emitter.Listener onConfirmConnection = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onConfirmConnection----------------");
            System.out.println(data.toString());

            // the connection has been confirmed so send the id
            String id = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);
            mSocket.emit("recieveDeviceID", id.substring(0, 8)); // send the first half of the device id
        }
    };

    private Emitter.Listener onRecieveFriendsStatus = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("--------------recieveFriendsStatus-----------------");
            System.out.println(data.toString());
            TextView testText = findViewById(R.id.testText);
            testText.setText(data.toString());
        }
    };

    public void testButton(View v){
        JSONArray json = new JSONArray();
        mSocket.emit("requestFriendsStatus");
    }


    @Override
    protected void onStop() {
        super.onStop();
        mSocket.disconnect();
    }
}