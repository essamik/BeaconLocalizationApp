package ch.swisscom.beaconlocalizationapp;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.List;

import ch.swisscom.beacondistanceestimator.AverageDistanceEstimator;
import ch.swisscom.beaconlocalizationapp.trilateration.MyTrilaterationCalculator;
import ch.swisscom.beaconlocalizationapp.trilateration.NonLinearLeastSquaresSolver;
import ch.swisscom.beaconlocalizationapp.trilateration.TrilaterationFunction;

public class MapFragment extends Fragment implements MapView.OnBeaconTouchedListener {

    private MapView mMap;
    private double mScale = 1;
    private BeaconInventory mBeaconInventory;
    private MyBeacon mClosestBeacon;
    private List<AverageDistanceEstimator> mListCalculator;

    //1m on the map equals that much pixels
    //private final static double P31 ROOM 7 SCALE = 156.25 px;
    //private final static double MULLERSTRASSE FLOOR 4 SCALE = 33.3 px;

    public MapFragment() {
        mBeaconInventory = new BeaconInventory();
        mListCalculator = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mMap = (MapView) view.findViewById(R.id.image_map);
        //mMap.setImageResource(MAP_SRC);
        mMap.setListener(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Device drop
        if (getView() == null) return;

        getView().setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                final int action = event.getAction();
                Log.d(MapFragment.class.getSimpleName(), "Pos : " + event.getX() + " ; " + event.getY());

                switch (action) {

                    case DragEvent.ACTION_DROP: {
                        Point mapDropPoint = new Point((int) event.getX(), (int) event.getY());
                        mMap.addBeacon(new BeaconMapObject(mapDropPoint));
                    }
                    break;

                    case DragEvent.ACTION_DRAG_ENDED: {
                        ImageView dragImage = (ImageView) getView().findViewById(R.id.drag);
                        dragImage.setVisibility(ImageView.INVISIBLE);
                    }
                    break;
                }
                return true;
            }
        });
    }

    private Point calculateCentroidMultilateration(double[] distances, List<BeaconCharacteristics> listBeaconsOnMap) {
        int rows = listBeaconsOnMap.size();
        int cols = 2;
        double[][] positions = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            positions[i][0] = listBeaconsOnMap.get(i).getPosition().x;
            positions[i][1] = listBeaconsOnMap.get(i).getPosition().y;
        }

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        // the answer
        double[] centroid = optimum.getPoint().toArray();

        return new Point((int) centroid[0], (int) centroid[1]);

        // error and geometry information; may throw SingularMatrixException depending the threshold argument provided
    }

    public void setClosestBeacon(MyBeacon beacon) {
        if (beacon != null) {
            mClosestBeacon = beacon;
        }
    }

    public void updateVisibleBeacon(List<MyBeacon> myBeacons) {
        //Save the closest beacon for pairing
        if (myBeacons.size() > 0) mClosestBeacon = myBeacons.get(0);

        double[] distances = new double[mBeaconInventory.getListBeacon().size()];

        for (int i = 0; i < mBeaconInventory.getListBeacon().size(); i++) {
            for (MyBeacon myBeacon : myBeacons) {
                BeaconCharacteristics beacon = mBeaconInventory.getListBeacon().get(i);
                if (beacon.isEqual(myBeacon.getUuid(), myBeacon.getMajor(), myBeacon.getMinor())) {
                    if (mListCalculator.size() <= i) {
                        mListCalculator.add(i, new AverageDistanceEstimator(getActivity(), myBeacon.getCalibrationVal()));
                    }

                    mListCalculator.get(i).addRSSI(myBeacon.getRssi());
                    double distance = mListCalculator.get(i).getAveragedDistance();
                    //distance = myBeacon.getDistance();

                    mMap.updateBeaconDistanceByPosition(beacon.getPosition(), distance);

                    double scaledDistance = distance * getScale();

                    distances[i] =  scaledDistance;

                    break;
                }
            }
        }

        //Need at leat 2 beacons
        if (distances.length >= 2) {
            Point centroid = calculateCentroidMultilateration(distances, mBeaconInventory.getListBeacon());
            mMap.setCentroidMultilateration(centroid);
        }

        if (distances.length >= 3) {
            Point ptCenter = MyTrilaterationCalculator.solve(mBeaconInventory.getBeaconPosition().get(0),
                    mBeaconInventory.getBeaconPosition().get(1),
                    mBeaconInventory.getBeaconPosition().get(2),
                    distances[0], distances[1], distances[2]);
            mMap.setCentroidTrilateration(ptCenter);
        }

        mMap.invalidate();
    }

    public double getScale() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        mScale = Double.valueOf(settings.getString(Constants.PREFS_SCALE, "1"));
        return mScale;
    }

    @Override
    public void onBeaconTouched(final BeaconMapObject beaconMapObj) {
        if (mClosestBeacon != null) {
            BeaconCharacteristics existingBeacon = mBeaconInventory.getBeaconByPosition(beaconMapObj.getPosition());
            if (existingBeacon != null) {
                removeBeaconPairing(existingBeacon, beaconMapObj);
            } else {
                pairBeacon(beaconMapObj);
            }
        }
    }

    private void pairBeacon(final BeaconMapObject beaconMapObj) {
        final MyBeacon closestBeacon = mClosestBeacon;
        final String beaconIds = closestBeacon.getMajor() + " " + closestBeacon.getMinor();
        Snackbar snackbar = Snackbar.make(getView(), "Pair with closest beacon ( "+ beaconIds +" )?", Snackbar.LENGTH_LONG)
                .setAction("Pair", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BeaconCharacteristics myBeacon = new BeaconCharacteristics("", closestBeacon.getUuid(),
                                closestBeacon.getMajor(), closestBeacon.getMinor(), beaconMapObj.getPosition());

                        String feedBack = "";
                        if (mBeaconInventory.addBeacon(myBeacon)) {
                            feedBack = "Paired with " + beaconIds;
                            beaconMapObj.setPairing(true);
                            beaconMapObj.setDistance(closestBeacon.getDistance());
                            mMap.invalidate();
                        } else {
                            feedBack = "Something went wrong";
                        }
                        Snackbar.make(getView(), feedBack, Snackbar.LENGTH_SHORT).show();
                    }
                });
        snackbar.show();
    }

    private void removeBeaconPairing(final BeaconCharacteristics pairedBeacon, final BeaconMapObject beaconMapObj) {
        final String beaconIds = pairedBeacon.getMajor() + " " + pairedBeacon.getMinor();
        Snackbar snackbar = Snackbar.make(getView(), "Remove beacon pairing ? ( "+ beaconIds +" )?", Snackbar.LENGTH_LONG)
                .setAction("UnPair", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String feedBack = "";
                        if (mBeaconInventory.removeBeacon(pairedBeacon)) {
                            feedBack = "Unaired from " + beaconIds;
                            beaconMapObj.setPairing(false);
                            beaconMapObj.setDistance(0);
                            mMap.invalidate();

                        } else {
                            feedBack = "Something went wrong";
                        }

                        Snackbar.make(getView(), feedBack, Snackbar.LENGTH_SHORT).show();
                    }
                });
        snackbar.show();
    }

    public void clearMap() {
        mBeaconInventory.removeAllBeacons();
        mMap.clearMap();
    }
}
