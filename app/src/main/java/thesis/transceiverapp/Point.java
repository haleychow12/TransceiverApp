package thesis.transceiverapp;

/**
 * Created by hcc999 on 4/7/17.
 */

public class Point {

    //Cartesian stuff will be done in this class
    double x, y;
    public Point(double xPoint, double yPoint){
        x = xPoint;
        y = yPoint;
    }

    //rotates x and y around 0,0 by theta radians, returns point with new x and y
    public static Point rotate(double x, double y, double T){
        return new Point(x*Math.cos(T) - y*Math.sin(T), x*Math.sin(T) + y*Math.cos(T));

    }

    //rotates x,y about rotate_point_x, rotate_point_y by theta radians
    //returns point with new x and y
    public static Point rotate_point(double x, double y, double rotate_point_x,
                             double rotate_point_y, double T){
        Point prime = rotate(x-rotate_point_x, y-rotate_point_y, T);

        return new Point(prime.x + rotate_point_x, prime.y + rotate_point_y);
    }

    //calculates and returns the distance between two points
    public static double distance(Point a, Point b){
        return (Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y)));
    }

    public static void crawlingAlgorithm(){

    }
}
