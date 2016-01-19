package ch.swisscom.beaconlocalizationapp.model;

import android.graphics.Point;

public class BeaconMapObject {

    private Point mPosition;
    private double mDistance;
    private boolean mIsPaired;

    public BeaconMapObject(Point pos) {
        mPosition = pos;
    }

    public Point getPosition() {
        return mPosition;
    }

    public double getDistance() {
        return mDistance;
    }

    public boolean isPaired() {
        return mIsPaired;
    }

    public void setDistance(double distance) {
        mDistance = distance;
    }

    public void setPairing(boolean isPaired) {
        mIsPaired = isPaired;
    }

    public void setPosition(Point pos) {
        mPosition = pos;
    }
}
