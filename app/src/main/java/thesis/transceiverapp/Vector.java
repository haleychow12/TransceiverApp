package thesis.transceiverapp;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by hcc999 on 2/20/17.
 */

/*class that contains the degrees and length of a vector*/
public class Vector {
    private float mAngDegrees;
    private double mDistCentimeters;
    private LatLng mLatLng;
    public static final String TAG = Vector.class.getSimpleName();

    public Vector(float direction, double distance, LatLng latLng){
        mAngDegrees = direction;
        mDistCentimeters = distance;
        mLatLng = latLng;
    }

    //converts the Latlng that calls this method into a point on the cartesian plane.
    //creates an x,y point that is the same distance in meters away
    public Point toPoint(LatLng center){
        double startLat = center.latitude;
        double endLat = mLatLng.latitude;
        double startLng = center.longitude;
        double endLng = mLatLng.longitude;

        float[] results = new float[2];

        Location.distanceBetween(startLat, startLng, endLat, endLng, results);

        //in meters
        double r = results[0];
        Log.v(TAG, String.format("r: %.4f", r));

        //in degrees, initial bearing
        double theta = Math.toRadians(results[1]);
        Log.v(TAG, String.format("theta: %.4f", theta));

        double x = r*Math.cos(theta);
        double y = r*Math.sin(theta);

        Log.v(TAG, String.format("Point (x,y): (%.4f, %.4f)", x, y));
        return new Point(x,y);


    }


}
