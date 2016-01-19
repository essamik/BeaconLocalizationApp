package ch.swisscom.beaconlocalizationapp.model;

import android.graphics.Point;

public class BeaconCharacteristics {

    private String mUuid;
    private int mMajor;
    private int mMinor;
    private Point mPosition;

    public BeaconCharacteristics(String uuid, int major, int minor, Point position) {
        mUuid = uuid;
        mMajor = major;
        mMinor = minor;
        mPosition = position;
    }

    public String getUuid() {
        return mUuid;
    }

    public void setUuid(String uuuid) {
        this.mUuid = uuuid;
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

    public void setminor(int minor) {
        this.mMinor = minor;
    }

    public Point getPosition() {
        return mPosition;
    }

    public void setPosition(Point position) {
        this.mPosition = position;
    }

    public boolean isEqual(String uuid, int major, int minor) {
        return (mUuid.equals(uuid) && mMajor == major && mMinor == minor);
    }

    public String getMajMin() {
        return this.getMajor() + " " + this.getMinor();
    }

}
