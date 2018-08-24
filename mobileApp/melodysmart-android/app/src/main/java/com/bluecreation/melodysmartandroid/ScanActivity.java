package com.bluecreation.melodysmartandroid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.bluecreation.melodysmart.MelodySmartDevice;

public class ScanActivity extends AppCompatActivity {

    protected static final String TAG = ScanActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private MelodySmartDevice melodySmartDevice;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Refreshing results...", Snackbar.LENGTH_LONG).show();
                scanLeDevice(false);
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
            }
        });

        setTitle(String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME));

        melodySmartDevice = MelodySmartDevice.getInstance();
        melodySmartDevice.init(this);

        mLeDeviceListAdapter = new LeDeviceListAdapter(this);

        ListView listView = findViewById(R.id.list);
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                scanLeDevice(false);

                BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);

                Intent intent = DeviceActivity.getIntent(ScanActivity.this, device.getAddress(), device.getName());
                startActivity(intent);
            }
        });

        CheckBox cbxShowOnlyMelodySmart = findViewById(R.id.cbxShowOnlyMelodySmart);
        cbxShowOnlyMelodySmart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mLeDeviceListAdapter.setShowOnlyMelodySmartDevices(isChecked);
                mLeDeviceListAdapter.clear();
            }
        });

        if (!hasCoarseLocationPermission()) {
            requestCoarseLocationPermission();
        } else {
            scanLeDevice(true);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        melodySmartDevice.disconnect();
        melodySmartDevice.close(this);

        scanLeDevice(false);

        super.onDestroy();
    }

    // Device scan callback.
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Found device " + device.getAddress());
                    mLeDeviceListAdapter.addDevice(new LeDeviceListAdapter.ScanResult(device, scanRecord));
                }
            });
        }
    };

    boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void requestCoarseLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            melodySmartDevice.startLeScan(mLeScanCallback);
        } else {
            melodySmartDevice.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanLeDevice(true);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        new AlertBuilder(this)
                                .setTitle("Permission required")
                                .setMessage("BLE scanning in Android 6.0+ requires access to your coarse location, please approve the request.")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestCoarseLocationPermission();
                                    }
                                })
                                .show();
                    } else {
                        Toast.makeText(this, "This application cannot run without the coarse " +
                                " location permission enabled. Please turn it on in the Settings " +
                                "screen before using the app.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
                break;
            }
        }
    }
}
