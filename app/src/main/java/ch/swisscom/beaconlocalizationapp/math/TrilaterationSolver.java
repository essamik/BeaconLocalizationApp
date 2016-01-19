package ch.swisscom.beaconlocalizationapp.math;

import android.graphics.Point;

public class TrilaterationSolver {

    public static Point solve(Point position1, Point position2, Point position3,double d1, double d2, double d3) {

        int xa = position1.x;
        int ya = position1.y;
        int xb = position2.x;
        int yb = position2.y;
        int xc = position3.x;
        int yc = position3.y;
        double ra = d1;
        double rb = d2;
        double rc = d3;

        double S = (Math.pow(xc, 2.) - Math.pow(xb, 2.) + Math.pow(yc, 2.) - Math.pow(yb, 2.) + Math.pow(rb, 2.) - Math.pow(rc, 2.)) / 2.0;
        double T = (Math.pow(xa, 2.) - Math.pow(xb, 2.) + Math.pow(ya, 2.) - Math.pow(yb, 2.) + Math.pow(rb, 2.) - Math.pow(ra, 2.)) / 2.0;
        double y = ((T * (xb - xc)) - (S * (xb - xa))) / (((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa)));
        double x = ((y * (ya - yb)) - T) / (xb - xa);

        return new Point((int)x,(int)y);
    }
}
