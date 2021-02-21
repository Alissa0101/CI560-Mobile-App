package com.alissa.skitracker;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    //private SocketService socketService;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private String friendCode;

    final ArrayList<Friend> friends = new ArrayList<Friend>();

    private FriendAdapter friendAdapter;

    private Socket mSocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            mSocket = IO.socket("http://bw373.brighton.domains:50000/");
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG + " OwO an ewwor", e.toString());
        }


        mSocket.on("confirmConnection", onConfirmConnection);
        mSocket.on("recieveFriendsStatus", onRecieveFriendsStatus);
        mSocket.on("onRecieveMyFriendCode", onRecieveMyFriendCode);
        mSocket.on("onRecieveAddFriendSuccessCode", onRecieveAddFriendSuccessCode);
        mSocket.connect();
        friendAdapter = new FriendAdapter(this, friends);
        ListView listView = (ListView) findViewById(R.id.lv_friendList);
        listView.setAdapter(friendAdapter);


    }

    /**
     * The server sends the client their friend code so it can be displayed
     */
    private Emitter.Listener onRecieveMyFriendCode = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onRecieveMyFriendCode - FRIENDS----------------");
            System.out.println(data.toString());

            TextView myFriendCodeText = findViewById(R.id.tv_myFriendCode);
            try{
                myFriendCodeText.setText("Your Friend Code: " + data.getString("code"));
                EditText et_enterName = findViewById(R.id.et_enterName);
                et_enterName.setText(data.getString("name"));
                friendCode = data.getString("code");
            } catch (JSONException e){
                Log.e(LOG_TAG, e.toString());
            }


        }
    };

    /**
     * Confirm that the connection has been made and send the device code
     */
    private Emitter.Listener onConfirmConnection = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onConfirmConnection - FRIENDS----------------");
            System.out.println(data.toString());

            // the connection has been confirmed so send the id
            String id = Settings.Secure.getString(getApplicationContext().getContentResolver(),Settings.Secure.ANDROID_ID);
            mSocket.emit("recieveDeviceID", id.substring(0, 8)); // send the first half of the device id
            mSocket.emit("recieveCanBeJoined", false);
        }
    };

    /**
     * recieve a list of all friends with their name and status
     */
    private Emitter.Listener onRecieveFriendsStatus = new Emitter.Listener() {


        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("--------------recieveFriendsStatus - FRIENDS-----------------");
            System.out.println(data.toString());
            Iterator<String> keysIterator = data.keys();
            ArrayList<String> keys = new ArrayList<String>();
            keysIterator.forEachRemaining(keys::add);

            friends.clear();

            for(int i = 0; i < keys.size(); i++){
                String key = keys.get(i);
                try{
                    JSONObject userData = data.getJSONObject(key);
                    String name = userData.getString("name");
                    boolean online = userData.getBoolean("online");
                    boolean canBeJoined = userData.getBoolean("canBeJoined");
                    friends.add(new Friend(name, key, online, canBeJoined));
                } catch (JSONException e){
                    Log.e(LOG_TAG, e.toString());
                }
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    friendAdapter.notifyDataSetChanged();
                }
            });


        }

    };

    /**
     * When trying to add a friend the server sends back if it was successful or not
     * if it was the friend has been added and if not then an error has occured or the code is wrong
     * if the code is wrong then turn the text red
     */
    private Emitter.Listener onRecieveAddFriendSuccessCode = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("--------------onRecieveAddFriendSuccessCode - FRIENDS-----------------");
            System.out.println(data.toString());

            EditText et_addFriend = findViewById(R.id.et_addFriend);

            try {
                if (data.getBoolean("success") == false) {
                    //make the text box red
                    et_addFriend.setTextColor(Color.RED);
                } else if (data.getBoolean("success") == true) {
                    //success make the text box default color
                    et_addFriend.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                }
            } catch (JSONException e){
                Log.e(LOG_TAG, e.toString());
            }



        }
    };

    /**
     * send an updated name to the server
     * @param v
     */
    public void updateName(View v){

        EditText et_enterText = findViewById(R.id.et_enterName);
        String name = et_enterText.getText().toString();

        JSONArray json = new JSONArray();
        json.put(name);
        mSocket.emit("recieveUpdateName", json);
    }


    /**
     * send a friend code to the server to be added to this users friends list
     * @param v
     */
    public void addFriend(View v){
        EditText et_addFriend = findViewById(R.id.et_addFriend);
        String friendsCode = et_addFriend.getText().toString();

        JSONArray json = new JSONArray();
        json.put(friendsCode);
        mSocket.emit("recieveAddFriend", json);
    }


    /**
     * Press the join button next to a friend to join them
     * @param v
     */
    public void joinButtonPressed(View v){

        View parentRow = (View) v.getParent();
        ListView listView = (ListView) parentRow.getParent();
        final int position = listView.getPositionForView(parentRow);

        Friend friend = friends.get(position);
        System.out.println(friend.getName() + " " + friend.getCode());

        if(friend.getcanBeJoined() == true){
            //send the friend code to the server to start watching them


            //go to the map view in watching mode
            Intent mapIntent = new Intent(MainActivity.this, MapActivity.class);

            mapIntent.putExtra("watching", true);
            mapIntent.putExtra("watchingCode", friend.getCode());
            startActivity(mapIntent);

        }

    }

    /**
     * press the map button to open the map view
     * this makes the user joinable
     */
    public void openMap(View v){

        Intent mapIntent = new Intent(MainActivity.this, MapActivity.class);
        mapIntent.putExtra("watching", false);
        startActivity(mapIntent);
    }


    /**
     * when the app is closed the socket connection is closed
     * this runs code on the server to tell this users friends that they are offline
     */
    @Override
    protected void onStop() {
        super.onStop();
        mSocket.disconnect();
        mSocket.close();
    }

}