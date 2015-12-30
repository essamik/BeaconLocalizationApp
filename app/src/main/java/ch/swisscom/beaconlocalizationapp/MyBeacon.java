package ch.swisscom.beaconlocalizationapp;


import android.graphics.Point;

import com.estimote.sdk.Utils;

import java.io.Serializable;

public class MyBeacon implements Serializable {

    private String mUuid;
    private int mMajor;
    private int mMinor;
    private String mAdress;
    private double mRssi;
    private double mDistance;
    private int mTxPower;
    private double mCalibrationVal;
    private Point mPosition;

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuid) {
        this.mUuid = uuid;
    }

    public int getMajor() {
        return mMajor;
    }

    public void setMajor(int major) {
        this.mMajor = major;
    }

    public int getMinor() {
        return mMinor;
    }

    public void setMinor(int minor) {
        this.mMinor = minor;
    }

    public String getAddress() {
        return mAdress;
    }

    public void setAddress(String address) {
        this.mAdress = address;
    }

    public double getRssi() {
        return mRssi;
    }

    public void setRssi(double rssi) {
        this.mRssi = rssi;
    }

    public double getDistance() {
        return Math.round(mDistance * 100.0) / 100.0;
    }

    public void setDistance(double distance) {
        this.mDistance = distance;
    }

    public double getTxPower() {
        return mTxPower;
    }

    public void setTxPower(int txPower) {
        this.mTxPower = txPower;
    }

    public double getCalibrationVal() {
        return mCalibrationVal;
    }

    public static MyBeacon fromEstimote(com.estimote.sdk.Beacon baseBeacon) {
        MyBeacon myBeacon = new MyBeacon();
        myBeacon.mUuid = baseBeacon.getProximityUUID().toString();
        myBeacon.mMajor = baseBeacon.getMajor();
        myBeacon.mMinor = baseBeacon.getMinor();
        myBeacon.mAdress = baseBeacon.getMacAddress().toStandardString();
        myBeacon.mDistance =  Utils.computeAccuracy(baseBeacon);
        myBeacon.mRssi = baseBeacon.getRssi();
        myBeacon.mCalibrationVal = baseBeacon.getMeasuredPower();

        return myBeacon;
    }

    public Point getPosition() {
        return mPosition;
    }

    public void setPosition(Point position) {
        mPosition = position;
    }
}

