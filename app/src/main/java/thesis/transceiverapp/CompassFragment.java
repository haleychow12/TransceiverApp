package thesis.transceiverapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by hcc999 on 11/13/16.
 */

public class CompassFragment extends Fragment {

    private SensorManager mSensorManager;
    private SensorEventListener r;
    private Sensor rotationSensor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) this.getActivity().getSystemService(SENSOR_SERVICE);

        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        r = new RotationSensorEventListener();
        mSensorManager.registerListener(r, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_compass, parent, false);

        return v;
    }

    class RotationSensorEventListener implements SensorEventListener{
        @Override
        public void onAccuracyChanged(Sensor s, int i){}

        @Override
        public void onSensorChanged(SensorEvent se){
            if (se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
                //se.values[0] contains next value
                Log.v("CompassFragment:",Double.toString(se.values[0]));
            }
        }
    }

    @Override
    public void onPause(){
        mSensorManager.unregisterListener(r);
        super.onPause();
    }

    @Override
    public void onResume(){
        mSensorManager.registerListener(r, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }
}
