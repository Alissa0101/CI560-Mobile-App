package com.alissa.skitracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private Socket mSocket;

    private String friendCode;

    private boolean watching = false;

    private MapView mapView;

    private String mapViewKey = "MapViewBundleKey";

    private GoogleMap map = null;

    private LocationManager locationManager;

    private boolean movedCamera = false;

    //private double lastAlt = 0;

    private String time = "";//data.getString("time"); //dateFormat.parse(data.getString("time"));
    private double lat = 0;//data.getDouble("lat");
    private double lng = 0;//data.getDouble("lng");
    private double alt = 0;//data.getDouble("alt");


    private ArrayList<Long> lastTime = new ArrayList<Long>();
    private ArrayList<Double> lastLAT = new ArrayList<Double>();
    private ArrayList<Double> lastLNG = new ArrayList<Double>();



    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            System.out.println(location.toString());



            JSONObject data = new JSONObject();
            try {

                Date time = Calendar.getInstance().getTime();

                Location loc = location;
                //TextView tv_debug = findViewById(R.id.tv_debug);

                data.put("time", time);
                data.put("lat", loc.getLatitude());
                data.put("lng", loc.getLongitude());
                data.put("alt", loc.getAltitude());


                //tv_debug.setText("Sent: " + data.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(map != null){
                            MarkerOptions marker = new MarkerOptions().position(new LatLng(loc.getLatitude(), loc.getLongitude())).title(time.toString());
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot));
                            map.addMarker(marker);
                            //map.addMarker(new MarkerOptions().position(new LatLng(loc.getLatitude(), loc.getLongitude())).title(time.toString()));
                            if(movedCamera == false){
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 1000));
                                movedCamera = true;
                            }

                        }
                    }
                });
                mSocket.emit("recieveNewLocationData", data);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.toString());
            }


        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        try {
            mSocket = IO.socket("http://62.171.146.191:50000/");
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG + " OwO an ewwor", e.toString());
        }
        watching = getIntent().getBooleanExtra("watching", false);
        friendCode = getIntent().getStringExtra("friendCode");

        //TextView tv_test = findViewById(R.id.tv_test);
        //tv_test.setText("My code: " + friendCode + "\n");

        mSocket.on("confirmConnection", onConfirmConnection);
        mSocket.on("recieveLocationData", onRecieveLocationData);
        mSocket.on("watchRequest", onWatchRequest);


        mSocket.connect();

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(mapViewKey);
        }
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);

        mapView.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //Could not get permission requests to work
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if(watching == false){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                    0.5f, locationListener);
        }

    }

    @SuppressLint("MissingPermission")
    private Location getLocation(){
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //map is ready :D
        map = googleMap;
    }

    private Emitter.Listener onWatchRequest = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onWatchRequest - MAP----------------");
            System.out.println(data.toString());

            //TextView tv_test = findViewById(R.id.tv_test);
            //tv_test.setText(tv_test.getText() + data.toString() + "\n");

            //show a confirm option to accept the watch request
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createConfirmationMenu(data);
                }
            });

        }
    };

    private void createConfirmationMenu(JSONObject data){

        try{

            String name = data.getString("name");
            String code = data.getString("code");
            JSONObject responseData = new JSONObject();

            new AlertDialog.Builder(MapActivity.this)
                    .setTitle("Confirm friend")
                    .setMessage(name + " wants to join you")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {//they have pressed YES
                            try{
                                responseData.put("confirm", true);
                                responseData.put("code", data.get("code"));
                                mSocket.emit("watchRequestResponse", responseData);
                            }catch (JSONException e){Log.e(LOG_TAG, e.toString());}
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {//they have pressed NO
                            try{
                                responseData.put("confirm", false);
                                responseData.put("code", data.get("code"));
                                mSocket.emit("watchRequestResponse", responseData);
                            }catch (JSONException e){Log.e(LOG_TAG, e.toString());}
                        }
                    })
                    .show();



        } catch (JSONException e){
            Log.e(LOG_TAG, e.toString());
        }

    }

    private Emitter.Listener onRecieveLocationData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            System.out.println("---------------onRecieveLocationData - MAP----------------");
            System.out.println(data.toString());

            //TextView tv_test = findViewById(R.id.tv_test);
            //tv_test.setText(tv_test.getText() + data.toString() + "\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");


            try{
                time = data.getString("time");
                lat = data.getDouble("lat");
                lng = data.getDouble("lng");
                alt = data.getDouble("alt");
            } catch (JSONException e){
                Log.e(LOG_TAG, e.toString());
            }

            System.out.println(time + " LAT: " + lat + " LNG: " + lng + " ALT: " + alt);
            if(lastLAT.size() > 2){
                System.out.println(time + " LastLAT: " + lastLAT.get(lastLAT.size()-2) + " LastLNG: " + lastLNG.get(lastLNG.size()-2));
            }

            Long timeMilliseconds = System.currentTimeMillis();


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //try{



                        //TextView tv_debug = findViewById(R.id.tv_debug);
                        //tv_debug.setText("Recieved: " + data.toString());

                        if(map != null){
                            MarkerOptions marker = new MarkerOptions().position(new LatLng(lat, lng)).title(time);
                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.reddot);
                            //if(alt > lastAlt){
                            //     icon = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
                            //}
                            marker.icon(icon);
                            map.addMarker(marker);
                            if(movedCamera == false){
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 1000));
                                movedCamera = true;
                            }


                            if(lastLAT.size() > 2) {
                                float[] distanceResults = {0, 0, 0, 0};
                                Location.distanceBetween(lat, lng, lastLAT.get(lastLAT.size() - 2), lastLNG.get(lastLNG.size()-2), distanceResults);

                                Double timeDiff = (timeMilliseconds.doubleValue() - lastTime.get(lastTime.size() - 2).doubleValue())/1000;

                                Double speed = distanceResults[0]/timeDiff;

                                TextView infoText = (TextView) findViewById(R.id.InfoText);
                                infoText.setText("Altitude: " + Math.round(alt) + "\n" + "Speed: " + Math.round(speed) + "m/s");
                            }



                        }
                        //

                    //} catch (JSONException e){
                    //    Log.e(LOG_TAG, e.toString());
                    //}
                }
            });
            //lastLAT = lat;
            lastLAT.add(lat);
            lastLNG.add(lng);
            lastTime.add(System.currentTimeMillis());

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

    public void closeActivity(View v){
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(watching == true){
            mSocket.emit("recieveStopWatching");
        }
        mSocket.disconnect();
        mSocket.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
