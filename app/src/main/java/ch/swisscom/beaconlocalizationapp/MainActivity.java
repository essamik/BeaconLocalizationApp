package ch.swisscom.beaconlocalizationapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.ArrayList;
import java.util.List;

import ch.swisscom.beaconlocalizationapp.preferences.PrefsActivity;

public class MainActivity extends AppCompatActivity {

    protected static final Region ALL_BEACONS_REGION = new Region("myregion", null, null, null);
    private static final int REQUEST_ENABLE_BT = 1234;

    private BeaconManager beaconManager;

    private MapFragment mMapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMapFragment = new MapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.content_layout, mMapFragment).commit();

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Note that beacons reported here are already sorted by estimated
                        // distance between device and beacon.
                        if (mMapFragment != null && beacons.size() > 0) {
                            List<MyBeacon> myBeacons = new ArrayList<MyBeacon>();
                            for (Beacon beacon : beacons) {
                                myBeacons.add(MyBeacon.fromEstimote(beacon));
                            }
                            mMapFragment.updateVisibleBeacon(myBeacons);
                        }
                    }
                });
            }
        });

        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToService();
        }
    }

    private void connectToService() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(ALL_BEACONS_REGION);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override protected void onStop() {
        beaconManager.stopRanging(ALL_BEACONS_REGION);
        super.onStop();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View v = findViewById(R.id.action_drag_beacon);

                if (v != null) {
                    v.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            ImageView dragImage = (ImageView) findViewById(R.id.drag);
                            Drawable drawableDrag = getResources().getDrawable(R.drawable.ic_map_beacon);
                            dragImage.setImageDrawable(drawableDrag);
                            dragImage.setVisibility(ImageView.GONE);
                            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(dragImage);

                            mMapFragment.getView().startDrag(ClipData.newPlainText("", ""), shadowBuilder, drawableDrag, 0);

                            return false;
                        }
                    });
                }
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, PrefsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_clear_map) {
            mMapFragment.clearMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
