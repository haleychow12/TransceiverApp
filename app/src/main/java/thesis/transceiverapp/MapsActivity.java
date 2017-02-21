package thesis.transceiverapp;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private TextView maccuracyView;
    private TextView mdistView;
    private Button mButton;
    private boolean mThreadReset = false;
    private ArrayList<LatLng> mPoints; //added
    private ArrayList<Vector> mVectors;
    Polyline line; //added
    private Location mCurrentLoc;

    private SensorManager mSensorManager;
    private SensorEventListener r;
    private Sensor rotationSensor;
    private float mDeclination;
    private int penColor = Color.RED;


    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private SupportMapFragment mMapFragment;

    private final static double ACCURACY_THRESHOLD = 12; //accuracy threshold in meters
    private ImageView mArrowImage;
    private float mAngle = 0;
    private double currDistance = 0;
    private float currAngle = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mPoints = new ArrayList<LatLng>();
        mVectors = new ArrayList<Vector>();
        // Create the LocationRequest object

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // 10 seconds, in milliseconds (interval betw active location updates)
                .setFastestInterval(1000); // 1 second, in milliseconds

        mdistView = (TextView) findViewById(R.id.distView);
        mArrowImage = (ImageView) findViewById(R.id.arrow);
        maccuracyView = (TextView) findViewById(R.id.accuracyView);
        mButton = (Button) findViewById(R.id.button2);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mThreadReset = true;
            }
        });
        /*maccuracyView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), SensorActivity.class);
                startActivity(i);
            }
        });*/


        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        r = new MapsActivity.RotationSensorEventListener();
        //boolean t = mSensorManager.registerListener(r, rotationSensor, SensorManager.SENSOR_STATUS_ACCURACY_LOW);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location Services Connected");
        LatLng start = null;
        //check permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.v(TAG, "setting up locationListener");

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);

        mMap.getUiSettings().setCompassEnabled(true);
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        initCamera(new LatLng(lat,lng));

        //Thread with simulated info from Avalanche Transceiver
        new Thread (new Runnable() {
            Location newLoc;
            float arrow;
            double distEstimate;
            public void run(){
                while(mGoogleApiClient.isConnected()){
                    if(mThreadReset){
                        mThreadReset = false;
                        //create new location
                        newLoc = getRandomNewLocation(mCurrentLoc);

                    }
                    if (newLoc != null) {
                        //set arrow picture rotation
                        arrow = mCurrentLoc.bearingTo(newLoc) % 360;
                        Log.v(TAG,Double.toString(arrow));
                        if (distEstimate < 15){
                            penColor = Color.YELLOW;
                        }
                        if (distEstimate < 5){
                            penColor = Color.GREEN;
                        }
                        if (distEstimate > 15){
                            penColor = Color.RED;
                        }

                        distEstimate = mCurrentLoc.distanceTo(newLoc);
                        //String s = "Dist: " + Double.toString(distEstimate) + " " + dir[arrow];
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTopBarVariables(arrow, distEstimate);
                                MarkerOptions markOptions = new MarkerOptions()
                                        .position(new LatLng(newLoc.getLatitude(), newLoc.getLongitude()))
                                        .title("New one to find!");
                                mMap.addMarker(markOptions);
                            }
                        });

                    }

                    //Log.d(TAG, s);*/
                    SystemClock.sleep(1000);
                }
            }
        }).start();

        /*mMapFragment = (SupportMapFragment) (getSupportFragmentManager()
                .findFragmentById(R.id.map));
        ViewGroup.LayoutParams params = mMapFragment.getView().getLayoutParams();
        params.height = 900;
        mMapFragment.getView().setLayoutParams(params);*/
    }
    private void setTopBarVariables(float arrow, double distEstimate){
        currAngle = arrow;
        currDistance = distEstimate;
        mArrowImage.setRotation(arrow - mAngle);
        mdistView.setText(String.format("Dist: %.2fm", distEstimate));
    }

    private Location getRandomNewLocation(Location here){
        double d = (Math.random()*30 + ACCURACY_THRESHOLD)/1000; //rand # range [.01,.04 km]
        double brng = Math.toRadians(randInt(360));

        double R = 6378.1; //radius of Earth

        double lat1 = Math.toRadians(here.getLatitude());
        double lon1 = Math.toRadians(here.getLongitude());

        double lat2 = Math.asin(Math.sin(lat1)*Math.cos(d/R) +
                Math.cos(lat1)*Math.sin(d/R)*Math.cos(brng));

        double lon2 = lon1 + Math.atan2(Math.sin(brng)* Math.sin(d/R)*Math.cos(lat1),
                Math.cos(d/R)- Math.sin(lat1)*Math.sin(lat2));

        lat2 = Math.toDegrees(lat2);
        lon2 = Math.toDegrees(lon2);

        Location there = new Location(LocationManager.GPS_PROVIDER);
        there.setLatitude(lat2);
        there.setLongitude(lon2);
        return there;
    }


    private void redrawLine(LatLng lat) {
        mMap.clear();
        PolylineOptions options = new PolylineOptions().width(5).color(penColor).geodesic(true);
        for (int i = 0; i < mPoints.size(); i++) {
            //adjust based on distance
            LatLng point = mPoints.get(i);
            options.add(point);
        }
        MarkerOptions markOptions = new MarkerOptions().position(lat).title("This is me!");
        mMap.addMarker(markOptions);
        line = mMap.addPolyline(options); //add Polyline
    }

    public void initCamera(LatLng latLng){
        mMap.getUiSettings().setZoomControlsEnabled(true);
        CameraPosition position = CameraPosition.builder()
                .target(latLng)
                .zoom(18f)
                .build();

        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), null);
        Log.v(TAG, "set up Camera");
        //mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //mMap.getUiSettings().setZoomControlsEnabled(true);

    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location Services Suspended, please reconnect");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onResume(){
        mGoogleApiClient.connect();
        mSensorManager.registerListener(r, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    public void onPause(){
        if (mGoogleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mSensorManager.unregisterListener(r);
        mPoints.clear();
        mVectors.clear();
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
        GeomagneticField field = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis()
        );

        // getDeclination returns degrees
        mDeclination = field.getDeclination();

    }

    private String formatAccuracy(Float accuracy) {
        String accuracyViewString = String.format("Accuracy: %.2fm", accuracy);
        return accuracyViewString;
    }

    private LatLng handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
        //Log.d(TAG, Float.toString(location.getBearing()));
        Float accuracyMeters = location.getAccuracy();
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);


        maccuracyView.setText(formatAccuracy(accuracyMeters));

        //if accuracy < 15 meters redraw line and update camera
        if(accuracyMeters < ACCURACY_THRESHOLD) {
            mPoints.add(latLng);
            mVectors.add(new Vector(currAngle, currDistance, latLng));
            redrawLine(latLng);
            updateCameraLocation(latLng);
            mCurrentLoc = location;
        }

        return latLng;

    }
    private void updateCameraBearing(float bearing) {
        if (mMap == null) return;

        CameraPosition pos = CameraPosition
                .builder(mMap.getCameraPosition())
                .bearing(bearing)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    private void updateCameraLocation(LatLng latLng) {
        if (mMap == null) return;

        CameraPosition pos = CameraPosition
                .builder(mMap.getCameraPosition())
                .target(latLng)
                .zoom(20f)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));

    }

    private int randInt(int max){
        return (int)(Math.random()*max);
    }

    class RotationSensorEventListener implements SensorEventListener{
        private double angle;
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            //Log.v(TAG, "rotation sensor!");
            if(se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] mRotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(
                        mRotationMatrix , se.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(mRotationMatrix, orientation);
                if (Math.abs(Math.toDegrees(orientation[0]) - angle) > 0.8) {
                    float bearing = (float) Math.toDegrees(orientation[0]) + mDeclination;
                    updateCameraBearing(bearing);
                }
                angle = Math.toDegrees(orientation[0]); //0 is north
                mAngle = (float) angle;
                //Log.v(TAG, "Angle: " + Double.toString(angle));
            }
        }
    }
}
