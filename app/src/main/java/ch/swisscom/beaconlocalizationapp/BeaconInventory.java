package ch.swisscom.beaconlocalizationapp;

import android.graphics.Point;
import android.util.Log;

import com.estimote.sdk.Beacon;

import java.util.ArrayList;
import java.util.List;

public class BeaconInventory {

    private List<BeaconCharacteristics> mListBeacon;

    public BeaconInventory() {
        mListBeacon = new ArrayList<>();
    }

    public boolean addBeacon(BeaconCharacteristics beacon) {
        boolean isExisting = false;
        for (BeaconCharacteristics myBeacon : mListBeacon) {
            if (myBeacon.isEqual(beacon.getUuid(), beacon.getMajor(), beacon.getMinor())) {
                isExisting = true;
            }
        }

        if(!isExisting) mListBeacon.add(beacon);
        else Log.e(getClass().getSimpleName(), "Beacon already in the inventory or placed on the map");

        return !isExisting;
    }

    public boolean removeBeacon(BeaconCharacteristics beacon) {
        boolean isRemoved = false;
        for (BeaconCharacteristics myBeacon : mListBeacon) {
            if (myBeacon.isEqual(beacon.getUuid(), beacon.getMajor(), beacon.getMinor())) {
                mListBeacon.remove(myBeacon);
                isRemoved = true;
                break;
            } else {
                Log.e(getClass().getSimpleName(), "Beacon not found in the inventory or in the map");
            }
        }
        return isRemoved;
    }

    public List<BeaconCharacteristics> getListBeacon() {
        return mListBeacon;
    }

    public List<Point> getBeaconPosition() {
        List<Point> beaconsPosition = new ArrayList<>();
        for (BeaconCharacteristics beacon : mListBeacon) {
            beaconsPosition.add(beacon.getPosition());
        }

        return beaconsPosition;
    }

    public BeaconCharacteristics getBeaconByPosition(Point pos) {
        BeaconCharacteristics beaconFound = null;

        for (BeaconCharacteristics myBeacon : mListBeacon) {
            if (pos.equals(myBeacon.getPosition())) {
                beaconFound = myBeacon;
                break;
            }
        }

        return beaconFound;
    }

    public void removeAllBeacons() {
        mListBeacon = new ArrayList<>();
    }
}
