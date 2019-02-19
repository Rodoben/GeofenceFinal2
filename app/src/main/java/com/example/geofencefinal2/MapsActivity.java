package com.example.geofencefinal2;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Map;

import static com.example.geofencefinal2.ConstantData.BAY_AREA_LANDMARKS;

public class MapsActivity extends FragmentActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, OnMapReadyCallback {

    private GoogleMap mMap;


    protected ArrayList<Geofence> mGeofenceList;
    protected GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;



    private GeofencingRequest geofencingRequest;


    private boolean isMonitoring = false;

    private MarkerOptions markerOptions;

    private Marker currentLocationMarker;
    private PendingIntent pendingIntent;


//compass
//ImageView compass_img;
TextView txt_compass;
    int mAzimuth;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        mGeofenceList = new ArrayList<Geofence>();
        buildGoogleApiClient();
        populateGeofenceList();


        //compass

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //compass_img = (ImageView) findViewById(R.id.img_compass);
        txt_compass = (TextView) findViewById(R.id.txt_azimuth);

        start();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        startgeofencing();
        addMarker();



//startLocationMonitor();
    }
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

//   startLocationMonitor();
    }


    @Override
    protected void onStop() {
        super.onStop();

        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        isMonitoring = true;
       startgeofencing();
        startLocationMonitor();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        isMonitoring = false;
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            Toast.makeText(
                    this,
                    "Geofences Added",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
        }

    }
    public void populateGeofenceList() {
        for (Map.Entry<String, ConstantData> entry : BAY_AREA_LANDMARKS.entrySet()) {

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            entry.getValue().radius
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        Toast.makeText(getApplicationContext(),"api client build",Toast.LENGTH_LONG).show();
    }
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }
    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addgeoFences()
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void startLocationMonitor() {
        Log.d(TAG, "start location monitor");
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                    }
                    markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                    markerOptions.title("Current Location");
                    currentLocationMarker = mMap.addMarker(markerOptions);
                    Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                }
            });
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        }

    }
public void  startgeofencing(){
 geofencingRequest=getGeofencingRequest();
 pendingIntent=getGeofencePendingIntent();
    if (!mGoogleApiClient.isConnected()) {
        Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
        return;
    }

    try {
        Toast.makeText(this,"adding geofences", Toast.LENGTH_SHORT).show();
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                // The GeofenceRequest object.
                geofencingRequest,

                // A pending intent that that is reused when calling removeGeofences(). This
                // pending intent is used to generate an intent when a matched geofence
                // transition is observed.
            pendingIntent
        ).setResultCallback(this); // Result processed in onResult().
    } catch (SecurityException securityException) {
        // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
    }
    isMonitoring=true;
    startLocationMonitor();
    invalidateOptionsMenu();
}


    private void addMarker() {
//audiblock

        ConstantData aa=BAY_AREA_LANDMARKS.get("audienter");
        LatLng a=new LatLng(aa.getLatitude(),aa.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(a));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(a)
                .radius(aa.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));


//puc building
        ConstantData puc=BAY_AREA_LANDMARKS.get("puc");
        LatLng puclat=new LatLng(puc.getLatitude(),puc.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(puclat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(puclat)
                .radius(puc.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));

//block3
        ConstantData block3=BAY_AREA_LANDMARKS.get("block3");
        LatLng block3lat=new LatLng(block3.getLatitude(),block3.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(block3lat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(block3lat)
                .radius(block3.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));


        //park

        ConstantData park=BAY_AREA_LANDMARKS.get("park");
        LatLng parklat=new LatLng(park.getLatitude(),park.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(parklat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(parklat)
                .radius(park.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));

        ConstantData central=BAY_AREA_LANDMARKS.get("central");
        LatLng centrallat=new LatLng(central.getLatitude(),central.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(centrallat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(centrallat)
                .radius(central.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));

        ConstantData block1=BAY_AREA_LANDMARKS.get("block1");
        LatLng block1lat=new LatLng(block1.getLatitude(),block1.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(block1lat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(block1lat)
                .radius(block1.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));

        ConstantData block2=BAY_AREA_LANDMARKS.get("block2");
        LatLng block2lat=new LatLng(block2.getLatitude(),block2.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(block2lat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(block2lat)
                .radius(block2.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));

        ConstantData block4=BAY_AREA_LANDMARKS.get("block4");
        LatLng block4lat=new LatLng(block4.getLatitude(),block4.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(block4lat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(block4lat)
                .radius(block4.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));
        ConstantData block2nd=BAY_AREA_LANDMARKS.get("block2nd");
        LatLng block2ndlat=new LatLng(block2nd.getLatitude(),block2nd.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(block2ndlat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(block2ndlat)
                .radius(block2nd.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));
        ConstantData home=BAY_AREA_LANDMARKS.get("home");
        LatLng homelat=new LatLng(home.getLatitude(),home.getLongitude());
        mMap.addMarker(new MarkerOptions()
                // .title("G:" + String)
                .snippet("Click here if you want delete this geofence")
                .position(homelat));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(a));
        mMap.addCircle(new CircleOptions()
                .center(homelat)
                .radius(home.radius)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));

    }
    @Override
    protected void onResume() {
        super.onResume();
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapsActivity.this);
        if (response != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Service Not Available");
            GoogleApiAvailability.getInstance().getErrorDialog(MapsActivity.this, response, 1).show();
        } else {
            Log.d(TAG, "Google play service available");
        }

        start();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        // compass_img.setRotation(-mAzimuth);

        String where = "NW";

        if (mAzimuth >= 350 || mAzimuth <= 10)
            where = "N";
        if (mAzimuth < 350 && mAzimuth > 280)
            where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260)
            where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190)
            where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170)
            where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100)
            where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80)
            where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10)
            where = "NE";


        txt_compass.setText(mAzimuth + "° " + where);


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void start() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                noSensorsAlert();
            }
            else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        else{
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void noSensorsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device doesn't support the Compass.")
                .setCancelable(false)
                .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }
    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    public void stop() {
        if (haveSensor) {
            mSensorManager.unregisterListener(this, mRotationV);
        }
        else {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        }
    }
}
