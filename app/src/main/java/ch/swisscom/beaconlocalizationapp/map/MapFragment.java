package ch.swisscom.beaconlocalizationapp.map;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.List;

import ch.swisscom.beacondistanceestimator.AverageDistanceEstimator;
import ch.swisscom.beaconlocalizationapp.BeaconInventory;
import ch.swisscom.beaconlocalizationapp.Constants;
import ch.swisscom.beaconlocalizationapp.PersistUtils;
import ch.swisscom.beaconlocalizationapp.R;
import ch.swisscom.beaconlocalizationapp.math.TrilaterationSolver;
import ch.swisscom.beaconlocalizationapp.math.NonLinearLeastSquaresSolver;
import ch.swisscom.beaconlocalizationapp.math.MultilaterationFunction;
import ch.swisscom.beaconlocalizationapp.model.BeaconCharacteristics;
import ch.swisscom.beaconlocalizationapp.model.BeaconMapObject;
import ch.swisscom.beaconlocalizationapp.model.MyBeacon;

public class MapFragment extends Fragment implements MapView.OnBeaconTouchedListener {

    public final static String TAG = MapFragment.class.getSimpleName();

    private MapView mMap;
    private BeaconInventory mBeaconInventory;
    private MyBeacon mClosestBeacon;

    private List<AverageDistanceEstimator> mListCalculator;
    private List<Double> mListDeltaMeasured;

    private boolean mAddUserPositionMode = false;
    private double mScale = 1;

    public MapFragment() {
        mBeaconInventory = new BeaconInventory();
        mListCalculator = new ArrayList<>();
        mListDeltaMeasured = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mMap = (MapView) view.findViewById(R.id.image_map);
        mMap.setListener(this);
        setHasOptionsMenu(true);

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

    public void updateVisibleBeacon(List<MyBeacon> myBeacons) {
        //Save the closest beacon for pairing
        if (myBeacons.size() > 0) mClosestBeacon = myBeacons.get(0);

        List<Double> distances = new ArrayList<>();

        for (int i = 0; i < mBeaconInventory.getListBeacon().size(); i++) {
            for (MyBeacon myBeacon : myBeacons) {
                BeaconCharacteristics beaconOnMap = mBeaconInventory.getListBeacon().get(i);
                if (beaconOnMap.isEqual(myBeacon.getUuid(), myBeacon.getMajor(), myBeacon.getMinor())) {
                    if (mListCalculator.size() <= i) {
                        mListCalculator.add(i, new AverageDistanceEstimator(getActivity(), myBeacon.getCalibrationVal(), false));
                    }

                    mListCalculator.get(i).addRSSI(myBeacon.getRssi() + getRSSICorrection());
                    break;
                }
            }
        }

        for (int i = 0; i < mBeaconInventory.getListBeacon().size(); i++) {
            double distance = mListCalculator.get(i).calculateAveragedDistance();
            BeaconCharacteristics beaconOnMap = mBeaconInventory.getListBeacon().get(i);

            mMap.updateBeaconDistanceByPosition(beaconOnMap.getPosition(), distance);

            double scaledDistance = distance * getScale();
            distances.add(scaledDistance);
        }

        List<BeaconCharacteristics> beaconsInRange = mBeaconInventory.getListBeacon();

        /** Multilateration */
        if (distances.size() >= 2) {
            Point centroid = calculateCentroidMultilateration(distances, beaconsInRange);
            if (centroid != null)  mMap.setCentroidMultilateration(centroid);
            double deltaDistance = mMap.gerErrorDelta();
            if (deltaDistance != -1) saveDelta(deltaDistance);

        }

        /** Trilateration
        if (distances.size() >= 3) {
            Point ptCenter = TrilaterationSolver.solve(mBeaconInventory.getBeaconPosition().get(0),
                                                        mBeaconInventory.getBeaconPosition().get(1),
                                                        mBeaconInventory.getBeaconPosition().get(2),
                                                        distances.get(0), distances.get(1),
                                                        distances.get(2));
            mMap.setCentroidTrilateration(ptCenter);
        }
         */

        mMap.invalidate();

    }

    private void saveDelta(double deltaDistance) {
        Log.d(TAG, "Error Delta : " + deltaDistance + "m");
        mListDeltaMeasured.add(deltaDistance);
        StringBuilder builder = new StringBuilder();

        for (Double delta : mListDeltaMeasured) {
            builder.append(delta + ",");
        }

        PersistUtils.saveMeasure(builder.toString(), mBeaconInventory.getListBeacon().size());
    }


    private Point calculateCentroidMultilateration(List<Double> listDistance, List<BeaconCharacteristics> listBeaconsOnMap) {
        int rows = listBeaconsOnMap.size();
        int cols = 2;
        double[][] positions = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            positions[i][0] = listBeaconsOnMap.get(i).getPosition().x;
            positions[i][1] = listBeaconsOnMap.get(i).getPosition().y;
        }

        double[] distances = new double[listDistance.size()];
        for (int i = 0; i < listDistance.size(); i++) {
            distances[i] = listDistance.get(i).doubleValue();
        }

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new MultilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = null;
        try {
            optimum = solver.solve();
        } catch (Exception e) {
            System.out.println(e);
        }

        if (optimum != null) {
            // the answer
            double[] centroid = optimum.getPoint().toArray();

            return new Point((int) centroid[0], (int) centroid[1]);

            // error and geometry information; may throw SingularMatrixException depending the threshold argument provided

        } else return null;
    }

    public double getScale() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        mScale = Double.valueOf(settings.getString(Constants.PREFS_SCALE, "1"));
        return mScale;
    }

    public double getRSSICorrection() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        double rssiCorrection = Double.valueOf(settings.getString(Constants.PREFS_RSSICORRECTION, "0"));
        return rssiCorrection;
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
                        BeaconCharacteristics myBeacon = new BeaconCharacteristics(closestBeacon.getUuid(),
                                                                                    closestBeacon.getMajor(),
                                                                                    closestBeacon.getMinor(),
                                                                                    beaconMapObj.getPosition());

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

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_mapfragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear_map) {
            this.clearMap();
            return true;
        } else if (id == R.id.action_set_position) {
            mAddUserPositionMode = !mAddUserPositionMode;
            item.getIcon().setAlpha(mAddUserPositionMode ? 100 : 255);
            mMap.setAddUserPositionMode(mAddUserPositionMode);
            if (!mAddUserPositionMode) mListDeltaMeasured = new ArrayList<>();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void clearMap() {
        mBeaconInventory.removeAllBeacons();
        mMap.clearMap();
    }

}
