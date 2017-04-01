package thesis.transceiverapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    public static final String TAG = MapsActivity.class.getSimpleName();
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private ArrayList<LatLng> mPoints; //list of points where the user has stepped
    private ArrayList<LatLng> mNearPoints;
    private ArrayList<LatLng> mMidPoints;
    private ArrayList<LatLng> mFarPoints;

    //Polyline line; //line that indicates where the user is
    private int penColor = Color.BLUE;

    private TextView maccuracyView; //GPS accuracy text view
    private final static double ACCURACY_THRESHOLD = 6; // GPS accuracy threshold in meters
    private TextView mdistView; //Distance from the transceiver text view
    private Button mButton; //God mode button
    private boolean mThreadReset = false; //boolean that resets the "God mode" thread


    private ArrayList<Vector> mVectors; //array list of vectors

    private Location mCurrentLoc; //current location

    private SensorManager mSensorManager;
    private SensorEventListener r;
    private Sensor rotationSensor;
    private float mDeclination;

    //private SupportMapFragment mMapFragment;
    private ImageView mArrowImage;
    private float mAngle = 0; //current angle of rotation the android tablet is at
    private double currDistance = 0; //current Distance to a transceiver
    private float currAngle = 0; //current angle the arrow is rotated at

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    private IntentFilter filter;
    private int mBaudRate = 115200; //baud rate of the serial device
    private boolean mSerialPortConnected;
    private Button mStartButton;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    final static int DIRECTION_DATA = 123; //unique int that identifies Direction data to the handler
    final static int DISTANCE_DATA = 456; //unique int that identifies Distance data to the handler

    private double mTransceiverDistance = -1; //double that holds transceiver Distance in meters
    private float mTransceiverDirection = 360; //float that holds transceiver direction in degrees

    //default camera position
    public static final CameraPosition PRINCETON =
            new CameraPosition.Builder().target(new LatLng(40.34780, -74.65316))
                    .zoom(15f)
                    .bearing(0)
                    .build();

    /*thread handler that takes the msg and translates it to distance or direction data*/
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            float dir;
            double dist;
            switch (msg.what) {
                case DIRECTION_DATA:
                    dir = ((Float)msg.obj).floatValue();
                    if (dir > -45 && dir < 45) { //the transceiver and tablet don't rotate independently
                        mArrowImage.setRotation(dir);
                        currAngle = dir;
                    }
                    break;
                case DISTANCE_DATA:
                    dist = ((Double)msg.obj).doubleValue();
                    if (dist < 0){
                        mdistView.setText("Dist: NA");
                    }
                    else {
                        mdistView.setText(String.format("Dist: %.2fm", dist));
                        currDistance = dist;
                    }
                    //Log.v(TAG, "Check");
                    break;
            }
        }
    };
    /*Defining a Callback which triggers whenever data is read.*/
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");

                if (data != null){
                    String[] parts = data.split(",");
                    //check if the data is good
                    if (parts[0].equals("A") && parts.length > 2) {
                        //change from centidegrees to degrees
                        mTransceiverDirection = Integer.parseInt(parts[1]);
                        mTransceiverDirection /= 100.0;

                        //centimeters to meters
                        mTransceiverDistance = Integer.parseInt(parts[2]) / 100.0;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    /*Broadcast Receiver to automatically start and stop the Serial connection.*/
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            mSerialPortConnected = true;
                            serialPort.setBaudRate(mBaudRate);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(mStartButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serialPort.close();
                mSerialPortConnected = false;
                unregisterReceiver(broadcastReceiver);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // 10 seconds, in milliseconds (interval betw active location updates)
                .setFastestInterval(1000); // 1 second, in milliseconds

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mPoints = new ArrayList<>();
        mNearPoints = new ArrayList<>();
        mMidPoints = new ArrayList<>();;
        mFarPoints = new ArrayList<>();;

        mVectors = new ArrayList<>();


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

        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        r = new MapsActivity.RotationSensorEventListener();

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        mStartButton = (Button) findViewById(R.id.startButton);
        mSerialPortConnected = false;

        filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }

    /* Behavior after clicking the start button. Connects the USB Serial Port
    with an Arduino device and begins Serial communication*/
    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
        Toast.makeText(this, "no usb device", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //sets default camera location to Princeton
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(PRINCETON));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location Services Connected");

        //check permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mCurrentLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.v(TAG, "setting up locationListener");

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);

        mMap.getUiSettings().setCompassEnabled(true);

        //Thread with simulated and real info from Avalanche Transceiver
        new Thread (new Runnable() {
            Location newLoc;
            float arrowDirection;
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
                        arrowDirection = mCurrentLoc.bearingTo(newLoc) % 360;
                        Log.v(TAG,Double.toString(arrowDirection));


                        distEstimate = mCurrentLoc.distanceTo(newLoc);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTopBarVariables(arrowDirection, distEstimate);
                                MarkerOptions markOptions = new MarkerOptions()
                                        .position(new LatLng(newLoc.getLatitude(), newLoc.getLongitude()))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                mMap.addMarker(markOptions);
                            }
                        });

                    }

                    else{
                        //No "God mode," values are from avalanche transceiver
                        Message m = Message.obtain(mHandler, DIRECTION_DATA);
                        m.obj = Float.valueOf(mTransceiverDirection);
                        mHandler.sendMessage(m);

                        m = Message.obtain(mHandler, DISTANCE_DATA);
                        m.obj = Double.valueOf(mTransceiverDistance);
                        mHandler.sendMessage(m);

                        Log.v(TAG,"done");

                        //TODO do the calculation and simulation here

                    }

                    SystemClock.sleep(500); //good value to update ui
                }
            }
        }).start();

    }
    /*used in "God mode" to set the variables on the top bar to simulated
    transceiver values. Arrow is the direction to the transceiver in degrees,
    distEstimate is the estimated location in meters.*/
    private void setTopBarVariables(float arrow, double distEstimate){
        currAngle = arrow;
        currDistance = distEstimate;
        mArrowImage.setRotation(arrow - mAngle);
        mdistView.setText(String.format("Dist: %.2fm", distEstimate));
    }

    /*uses the location here as reference and returns a random location that is
    within [AccuracyThreshold, AccuracyThreshold + 30] meters from location here*/
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

    /*redraws the polyline on the map and places a marker at the current LatLng*/
    private void redrawLine(LatLng lat) {
        mMap.clear();

        PolylineOptions nearLine = new PolylineOptions().color(Color.GREEN).geodesic(true);
        PolylineOptions midLine = new PolylineOptions().color(Color.YELLOW).geodesic(true);
        PolylineOptions farLine = new PolylineOptions().color(Color.RED).geodesic(true);

        PolylineOptions options = new PolylineOptions().color(penColor).geodesic(true);
        for (int i = 0; i < mPoints.size(); i++) {
            //adjust based on distance
            LatLng point = mPoints.get(i);
            options.add(point);
        }
        for (int i = 0; i < mNearPoints.size(); i++) {
            //adjust based on distance
            LatLng point = mNearPoints.get(i);
            nearLine.add(point);
        }
        for (int i = 0; i < mMidPoints.size(); i++) {
            //adjust based on distance
            LatLng point = mMidPoints.get(i);
            midLine.add(point);
        }
        for (int i = 0; i < mFarPoints.size(); i++) {
            //adjust based on distance
            LatLng point = mFarPoints.get(i);
            farLine.add(point);
        }

        MarkerOptions markOptions = new MarkerOptions().position(lat).title("This is me!");
        mMap.addMarker(markOptions);
        mMap.addPolyline(options); //add Polyline
        mMap.addPolyline(nearLine); //add Polyline
        mMap.addPolyline(midLine); //add Polyline
        mMap.addPolyline(farLine); //add Polyline
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
        registerReceiver(broadcastReceiver, filter);
        super.onResume();
    }

    @Override
    public void onPause(){
        if (mGoogleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mSensorManager.unregisterListener(r);
        mPoints.clear();
        mVectors.clear();

        //remove all serial port connections
        if (mSerialPortConnected) {
            serialPort.close();
            mSerialPortConnected = false;
        }
        //check some condition?
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /*adjusts the magnetic field and calls the new location handler*/
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

    /*formats the input float accuracy into a string that can be displayed*/
    private String formatAccuracy(Float accuracy) {
        String accuracyViewString = String.format("Accuracy: %.2fm", accuracy);
        return accuracyViewString;
    }

    /*new location handler that checks the GPS accuracy and if it is below the
    threshold, creates a new point and redraws the polygon. Returns a latlng
    with the current location*/
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

            if (currDistance > 0 && currDistance <= 5)
                mNearPoints.add(latLng);
            else if (currDistance >  5 && currDistance <= 15)
                mMidPoints.add(latLng);
            else if (currDistance > 15)
                mFarPoints.add(latLng);
            else
                mPoints.add(latLng);

            mVectors.add(new Vector(currAngle, currDistance, latLng));
            redrawLine(latLng);
            updateCameraLocation(latLng);
            mCurrentLoc = location;
        }

        return latLng;

    }


    /*updates the camera's bearing based on the float bearing*/
    private void updateCameraBearing(float bearing) {
        if (mMap == null) return;

        CameraPosition pos = CameraPosition
                .builder(mMap.getCameraPosition())
                .bearing(bearing)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    /*updates and centers the camera on latLng*/
    private void updateCameraLocation(LatLng latLng) {
        if (mMap == null) return;

        CameraPosition pos = CameraPosition
                .builder(mMap.getCameraPosition())
                .target(latLng)
                .zoom(20f)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));

    }

    /*returns a random integer between 0 and max*/
    private int randInt(int max){
        return (int)(Math.random()*max);
    }

    class RotationSensorEventListener implements SensorEventListener{
        private double rAngle; //rotation angle
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            if(se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] mRotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(
                        mRotationMatrix , se.values);
                float[] orientation = new float[3];
                SensorManager.getOrientation(mRotationMatrix, orientation);
                if (Math.abs(Math.toDegrees(orientation[0]) - rAngle) > 0.8) {
                    float bearing = (float) Math.toDegrees(orientation[0]) + mDeclination;
                    updateCameraBearing(bearing);
                }
                rAngle = Math.toDegrees(orientation[0]); //0 is north
                mAngle = (float) rAngle; //set global angle of rotation variable
            }
        }
    }
}