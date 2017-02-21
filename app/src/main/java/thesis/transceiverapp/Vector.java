package thesis.transceiverapp;

import com.google.android.gms.maps.model.LatLng;

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
    /*need to determine when to check the vectors against the magnetic field "in theory"
    on location changed? but after a certain number of steps have been taken or a certain
    amount of distance has been covered*/

    /*this method takes a vectorList (points with vectors) and samples a list of evenly spaced
    LatLngs to find which LatLng calculated magnetic field lines up the best with the observed
    field (represented by the list of points*/
    public static ArrayList<Vector> analyzeMagneticField(ArrayList<Vector> vectorList,
                                                         ArrayList<LatLng> samples){

        for (int i = 0; i < samples.size(); i++){
            //for each sample, take every latlng in the vector list and throw it in the
            //magnetic field equation. find the average difference between the calculated field
            //magnitude in meters (and if I can figure out direction) and the distance in the
            // vector list
        }
        return vectorList;

    }

}
