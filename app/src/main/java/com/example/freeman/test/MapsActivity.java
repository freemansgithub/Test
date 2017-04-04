package com.example.freeman.test;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // Get Class Name
    private static String TAG = MapsActivity.class.getName();
    private Context mContext;

    SupportMapFragment mapFragment;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    MqttAndroidClient client;
    String mqttHost;
    String topic = "test/topic";
    IMqttToken token;

    int geocoderMaxResults = 1;
    private float zoom;
    LatLng latlng;
    Double latitude = 53.912954266;
    Double longitude = 27.593078800;
    Float radius;
    String timestamp = null;
    String location;
    String countryName;
    String playerName;
    TextView latitudeView;
    TextView longitudeView;
    TextView timestampView;
    TextView locationView;
    TextView providerView;
    TextView zoomView;
    TextView playerView;
    private Marker playerMarker;
    private Circle playerCircle;
    int playerColor;
    int playerId = 0;
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    ToggleButton btnStart;
    private TimerService timer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        latitudeView = (TextView) findViewById(R.id.latitude);
        longitudeView = (TextView) findViewById(R.id.longitude);
        timestampView = (TextView) findViewById(R.id.timestamp);
        locationView = (TextView) findViewById(R.id.location);
        providerView = (TextView) findViewById(R.id.provider);
        zoomView = (TextView) findViewById(R.id.zoom);
        playerView = (TextView) findViewById(R.id.player);
        btnStart = (ToggleButton) findViewById(R.id.start);
        zoom = 16;
        radius = 50.0f;
        playerColor = Color.BLUE;
        playerName = "Guest";

//        mqttHost = "tcp://79.98.31.32:1883";
        mqttHost = "tcp://192.168.1.6:1883";

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), mqttHost, clientId);
        MqttConnectOptions options = new MqttConnectOptions();

        playerView.setText(playerName);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        try {
            token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    Toast.makeText(MapsActivity.this, "Connected", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");
                    Toast.makeText(MapsActivity.this, "Connection failed", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(MIN_TIME_BW_UPDATES);
        mLocationRequest.setFastestInterval(MIN_TIME_BW_UPDATES);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public List<Address> getGeocoderAddress(Context context) {
        if (mLastLocation != null) {

            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);

            try {
                /**
                 * Geocoder.getFromLocation - Returns an array of Addresses
                 * that are known to describe the area immediately surrounding the given latitude and longitude.
                 */
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, this.geocoderMaxResults);

                return addresses;
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Impossible to connect to Geocoder", e);
            }
        }

        return null;
    }


    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (playerMarker != null) {
            playerMarker.remove();
            playerCircle.remove();
        }
        timestamp = String.format("%s", new Date(location.getTime()));
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        String info = String.format("%s Lat:%.8f Long:%.8f", timestamp, latitude, longitude);
        latitudeView.setText(String.format("%.8f", latitude));
        longitudeView.setText(String.format("%.8f", longitude));
        providerView.setText(location.getProvider());
        this.location = String.format("%s/%s", getCountryName(this), getLocality(this));
        locationView.setText(this.location);
        timestampView.setText(timestamp);
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
        zoomView.setText(String.format("%.1f", zoom));
        //Place current location marker
        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions playserMarkerOptions = new MarkerOptions();
        playserMarkerOptions.position(latLng);
        playserMarkerOptions.title(playerName);
        playserMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        playerMarker = mMap.addMarker(playserMarkerOptions);

        int playerColor50percent = ColorUtils.setAlphaComponent(playerColor, 80);
        playerCircle = mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(radius)
                .strokeColor(playerColor)
                .strokeWidth(2)
                .fillColor(playerColor50percent));

        // push updated date to server
        pushToServer();

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        latitude = 53.912954266;
        longitude = 27.593078800;
    }

    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
//                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //TODO: Do not stop getting of gps info onPause
        //stop location updates when Activity is no longer active
//        if (mGoogleApiClient != null) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
//        mMap.getUiSettings().setZoomControlsEnabled(true);

        View mapView = findViewById(R.id.map);
        View locationButton = mapView.findViewWithTag("GoogleMapMyLocationButton");
        // and next place it, for exemple, on bottom right (as Google Maps app)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 30);

        //Initialize Google Play Services
//        int b = Build.VERSION.SDK_INT;
//        int b2 = Build.VERSION_CODES.M;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
//                buildGoogleApiClient();
                  mMap.setMyLocationEnabled(true);
            }
        }
        else {
//            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    public String getCountryName(Context context) {
        List<Address> addresses = getGeocoderAddress(context);
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String countryName = address.getCountryName();
            return countryName;
        } else {
            return null;
        }
    }

    public String getLocality(Context context) {
        List<Address> addresses = getGeocoderAddress(context);
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String locality = address.getLocality();
            return locality;
        }
        else {
            return null;
        }
    }

    public void onClickLocationSettings(View view) {
        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    public void pushToServer(){
        JSONObject jsonObj = new JSONObject();
        if (timestamp == null) {
            timestamp = "null";
        }
        try {
            jsonObj.put("id", playerId);
            jsonObj.put("name", playerName);
            jsonObj.put("timestamp", timestamp);
            jsonObj.put("lat", latitude);
            jsonObj.put("lng", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String message = jsonObj.toString();
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage.getPayload(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onClickPush(View view){

        JSONObject jsonObj = new JSONObject();
        if (timestamp == null) {
            timestamp = "null";
        }
        try {
            jsonObj.put("id", playerId);
            jsonObj.put("name", playerName);
            jsonObj.put("timestamp", timestamp);
            jsonObj.put("lat", latitude);
            jsonObj.put("lng", longitude);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String message = jsonObj.toString();
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(topic, mqttMessage.getPayload(), 0, false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onClickStart(View view) {
        if (btnStart.getText().equals(btnStart.getTextOff())){
            if (mGoogleApiClient != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }
            // Stop the service
            stopService(new Intent(getBaseContext(), TimerService.class));
        }else {
            if (mGoogleApiClient == null){
                buildGoogleApiClient();
            }
            // Start the service
            startService(new Intent(getBaseContext(), TimerService.class));
        }
    }
}
