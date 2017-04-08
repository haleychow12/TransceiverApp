package thesis.transceiverapp;

import android.location.Location;

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

    public Vector(float direction, double distance, LatLng latLng){
        mAngDegrees = direction;
        mDistCentimeters = distance;
        mLatLng = latLng;
    }

    public float getDirection(){
        return mAngDegrees;
    }

    public double getDistance(){
        return mDistCentimeters;
    }

    public LatLng getLatLng(){
        return mLatLng;
    }

    public Point toPoint(LatLng center){
        double startLat = center.latitude;
        double endLat = mLatLng.latitude;
        double startLng = center.longitude;
        double endLng = mLatLng.longitude;

        float[] results = new float[2];

        Location.distanceBetween(startLat, endLat, startLng, endLng, results);

        //in meters
        double r = results[0];

        //in degrees, initial bearing
        double theta = Math.toRadians(results[1]);

        double x = r*Math.cos(theta);
        double y = r*Math.sin(theta);

        return new Point(x,y);

    }


}
