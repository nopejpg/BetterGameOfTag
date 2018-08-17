package com.bluecreation.melodysmartandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bluecreation.melodysmart.BLEError;
import com.bluecreation.melodysmart.DataService;
import com.bluecreation.melodysmart.DeviceDatabase;
import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmart.MelodySmartListener;
import com.bluecreation.melodysmartandroid.databinding.ActivityDeviceBinding;

import java.util.Locale;

public class DeviceActivity extends BleActivity implements MelodySmartListener {

    private static final int REQUEST_CODE_START_OTAU = 0;
    private static final int REQUEST_CODE_START_I2C = 1;
    private static final int REQUEST_CODE_START_REMOTE_COMMANDS = 2;

    private static String EXTRA_DEVICE_ADDRESS = "getDeviceAddress";
    private static String EXTRA_DEVICE_NAME = "deviceName";

    private String TAG = DeviceActivity.class.getSimpleName();

    private ActivityDeviceBinding binding;
    private MelodySmartDevice device;
    private AlertDialog connectionDialog;
    private DataService.Listener dataServiceListener = new DataService.Listener() {
        @Override
        public void onReceived(final byte[] data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.etReceivedData.setText(new String(data));
                }
            });
        }

        @Override
        public void onNotificationsEnabled(boolean state) {
        }
    };

    public static Intent getIntent(Context context, String address, String name) {
        Intent intent = new Intent(context, DeviceActivity.class);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        intent.putExtra(EXTRA_DEVICE_NAME, name);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_device);

        setSupportActionBar(binding.toolbar);

        /* Get the instance of the Melody Smart Android library and initialize it */
        device = MelodySmartDevice.getInstance();
        device.registerListener((MelodySmartListener) this);

        Intent intent = getIntent();
        String deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        String deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);

        connectionDialog = new AlertBuilder(this)
                .setMessage(String.format("Connecting to:\n%s\n(%s)...", deviceName, deviceAddress))
                .setTitle(R.string.app_name)
                .show();

        try {
            device.connect(deviceAddress);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        binding.etDataToSend.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                device.getDataService().send(textView.getText().toString().getBytes());
                return true;
            }
        });
    }

    public void openI2C(View view) {
        Intent intent = new Intent(this, I2cActivity.class);
        startActivityForResult(intent, REQUEST_CODE_START_I2C);
    }

    public void openUpgradeFw(View view) {
        startOtauActivity(false, null);
    }

    public void openAdvancedData(View view) {
        Intent intent = new Intent(this, AdvancedDataActivity.class);
        startActivityForResult(intent, REQUEST_CODE_START_REMOTE_COMMANDS);
    }

    public void openWifiConfiguration(View view) {
        Intent intent = new Intent(this, WifiConfigurationActivity.class);
        startActivityForResult(intent, REQUEST_CODE_START_REMOTE_COMMANDS);
    }

    public void openRemoteCommands(View view) {
        Intent intent = new Intent(this, RemoteCommandsActivity.class);
        startActivityForResult(intent, REQUEST_CODE_START_REMOTE_COMMANDS);
    }

    private void findRemoteCommandsService() {
        final boolean found = device.getRemoteCommandsService().isAvailable();

        Log.d(TAG, (found ? "Connected " : "Not connected  ") + "to remote commands service");

        binding.remoteCommandsButton.setEnabled(found);
    }

    private void findI2CService() {
        final boolean found = device.getI2CService().isAvailable();

        Log.d(TAG, (found ? "Connected" : "Not connected") + " to I2C service");

        binding.i2cButton.setEnabled(found);
    }

    void findDataService() {
        final boolean found = device.getDataService().isAvailable();

        Log.d(TAG, (found ? "Connected" : "Not connected") + "to MelodySmart data service");

        if (found) {
            device.getDataService().registerListener(dataServiceListener);
            device.getDataService().enableNotifications(true);
        } else {
            Toast.makeText(this, "MelodySmart service not found on the remote device.", Toast.LENGTH_LONG).show();
        }

        binding.advancedDataButton.setEnabled(found);
        binding.etDataToSend.setEnabled(found);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_START_OTAU) {
            if (resultCode == RESULT_OK) {
                finish();
            } else {
                device.registerListener((MelodySmartListener) this);
            }
        }
    }

    @Override
    public void onDestroy() {

        device.getDataService().unregisterListener(dataServiceListener);
        device.unregisterListener((MelodySmartListener) this);

        device.disconnect();

        super.onDestroy();
    }


    @Override
    public void onDeviceConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findDataService();
                findI2CService();
                findRemoteCommandsService();

                if (connectionDialog != null && connectionDialog.isShowing()) {
                    connectionDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onDeviceDisconnected(final BLEError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (error.getType() == BLEError.Type.NO_ERROR) {
                    binding.etDataToSend.setEnabled(false);
                    Toast.makeText(DeviceActivity.this, "Disconnected from the device.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    if (connectionDialog != null && connectionDialog.isShowing()) {
                        connectionDialog.dismiss();
                    }
                    connectionDialog = new AlertBuilder(DeviceActivity.this)
                            .setMessage(getDisconnectionMessage(error))
                            .setTitle("Disconnected")
                            .setCancelable(false)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }).show();
                }
            }
        });
    }

    @Override
    public void onOtauAvailable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.otaButton.setEnabled(true);
            }
        });
    }

    @Override
    public void onOtauRecovery(DeviceDatabase.DeviceData deviceData) {
        // Automatically go to OTAU
        startOtauActivity(true, deviceData);
    }

    @Override
    public void onReadRemoteRssi(int rssi) {
    }

    private void startOtauActivity(boolean isRecovery, DeviceDatabase.DeviceData deviceData) {
        device.unregisterListener((MelodySmartListener) this);

        Intent intent = new Intent(this, OtauTestActivity.class);
        intent.putExtra(OtauTestActivity.EXTRAS_IS_RECOVER_OTA, isRecovery);
        intent.putExtra(OtauTestActivity.EXTRAS_DEVICE_DATA, deviceData);
        startActivityForResult(intent, REQUEST_CODE_START_OTAU);
    }

    private String getDisconnectionMessage(BLEError error) {
        String message;
        switch (error.getType()) {
            case AUTHENTICATION_ERROR:
                message = "Authentication error: ";
                if (device.isBonded()) {
                    if (device.removeBond()) {
                        message += " bonding information has been removed on your Android phone. Please remove it on your MelodySmart device if necessary and reconnect.";
                    } else {
                        message += " could not remove bonding information on your Android phone. Please remove it manually on the Bluetooth settings screen, " +
                                "remove it on your MelodySmart device if necessary and reconnect.";
                    }
                } else {
                    message += " please remove bonding information on your MelodySmart device and reconnect.";

                }
                break;

            case REMOTE_DISCONNECTION:
                message = error.getMessage();
                break;

            default:
                message = String.format(Locale.getDefault(),
                        "Disconnected: %s\n\n[error code: %d]", error.getMessage(), error.getCode());
                break;
        }

        return message;
    }
}
