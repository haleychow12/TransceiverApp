package thesis.transceiverapp;

import android.content.Intent;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by hcc999 on 11/15/16.
 */

public class SensorActivity extends  FragmentActivity {

    private SensorManager mSensorManager;
    private SensorEventListener r;
    private SensorEventListener a;
    private SensorEventListener g;
    private SensorEventListener m;

    private Sensor accelSensor;
    private Sensor gyroSensor;
    private Sensor rotationSensor;
    private Sensor magneticSensor;


    private TextView maccelXValues;
    private TextView maccelYValues;
    private TextView maccelZValues;
    private TextView mgyroXValues;
    private TextView mgyroYValues;
    private TextView mgyroZValues;
    private TextView mmagXValues;
    private TextView mmagYValues;
    private TextView mmagZValues;
    private TextView mrotationSensorValues;
    private TextView mDistText;

    private ImageView mArrowImage;

    private int mArrow;
    private double mDistEst;

    private String[] mDir= {"left", "diagonal left", "straight", "diagonal right", "right"};
    private float[] mDegrees = {-90, -45, 0, 45, 90};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        /*Intent intent = getIntent();
        mArrow = intent.getIntExtra("arrow", -1);
        mDistEst = intent.getDoubleExtra("distEst", 0);

        mDistText = (TextView) findViewById(R.id.textView4);
        mArrowImage = (ImageView) findViewById(R.id.imageView);
        mDistText.setText(String.format("Distance Estimate: %.3fm", mDistEst));
        mArrowImage.setRotation(mDegrees[mArrow]);*/



        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        r = new SensorActivity.RotationSensorEventListener();
        a = new SensorActivity.AccelSensorEventListener();
        g = new SensorActivity.GyroSensorEventListener();
        m = new SensorActivity.MagneticSensorEventListener();

        mSensorManager.registerListener(r, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(a, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(g, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(m, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);

        maccelXValues = (TextView) findViewById(R.id.accelX_values);
        maccelYValues = (TextView) findViewById(R.id.accelY_values);
        maccelZValues = (TextView) findViewById(R.id.accelZ_values);

        mgyroXValues = (TextView) findViewById(R.id.gyroX_values);
        mgyroYValues = (TextView) findViewById(R.id.gyroY_values);
        mgyroZValues = (TextView) findViewById(R.id.gyroZ_values);

        mmagXValues = (TextView) findViewById(R.id.magX_values);
        mmagYValues = (TextView) findViewById(R.id.magY_values);
        mmagZValues = (TextView) findViewById(R.id.magZ_values);

        mrotationSensorValues = (TextView) findViewById(R.id.rotation_values);


    }


    class RotationSensorEventListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            if (se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                //se.values[0] contains next value
                mrotationSensorValues.setText(Double.toString(se.values[0]));
                //Log.v("Sensor Activity:",Double.toString(se.values[0]));


            }
        }
    }

    class AccelSensorEventListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                //se.values[0] contains next value
                maccelXValues.setText(Double.toString(se.values[0]));
                maccelYValues.setText(Double.toString(se.values[1]));
                maccelZValues.setText(Double.toString(se.values[2]));
                //Log.v("Sensor Activity:",Double.toString(se.values[0]));


            }
        }
    }

    class MagneticSensorEventListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            if (se.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                //se.values[0] contains next value
                mmagXValues.setText(Double.toString(se.values[0]));
                mmagYValues.setText(Double.toString(se.values[1]));
                mmagZValues.setText(Double.toString(se.values[2]));
                //Log.v("Sensor Activity:",Double.toString(se.values[0]));


            }
        }
    }


    class GyroSensorEventListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            if (se.sensor.getType() == Sensor.TYPE_GYROSCOPE){
                //se.values[0] contains next value
                mgyroXValues.setText(Double.toString(se.values[0]));
                mgyroYValues.setText(Double.toString(se.values[1]));
                mgyroZValues.setText(Double.toString(se.values[2]));
                //Log.v("Sensor Activity:",Double.toString(se.values[0]));


            }
        }
    }

    @Override
    public void onPause(){
        mSensorManager.unregisterListener(r);
        mSensorManager.unregisterListener(a);
        mSensorManager.unregisterListener(g);
        mSensorManager.unregisterListener(m);
        super.onPause();
    }

    @Override
    public void onResume(){
        mSensorManager.registerListener(r, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(a, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(g, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(m, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

}

