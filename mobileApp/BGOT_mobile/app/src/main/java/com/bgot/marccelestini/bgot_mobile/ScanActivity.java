package com.bgot.marccelestini.bgot_mobile;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.os.AsyncTask;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.bgot.marccelestini.bgot_mobile.R.*;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {
    //variables for xml to edit
    private TextView mText;
    private Button mAdvertiseButton;
    //UUIDs "They're also in the strings.xml. I'm having a hard time using R.strings  command... So they're hardcoded" **now theyre in UARTProfile.java
    private BluetoothGattServer mGattServer;
    private BluetoothManager mBluetoothManager;


    //create pages
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_scan);
        mText = findViewById(id.text);
        mAdvertiseButton = findViewById(id.advertise_btn);
        mAdvertiseButton.setOnClickListener(this);
    }

    //when the button is clicked, begin advertise
    @Override
    public void onClick(View v) {
        if (v.getId() == id.advertise_btn) {
            advertise();
        }
    }
    //Initalize server by setting up UART  server
    private void initServer() {
        BluetoothGattService UART_SERVICE = new BluetoothGattService(UARTProfile.bleId,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic TX_READ_CHAR =
                new BluetoothGattCharacteristic(UARTProfile.safeId,
                        //Read-only characteristic, supports notifications
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        //Descriptor for read notifications
        BluetoothGattDescriptor TX_READ_CHAR_DESC = new BluetoothGattDescriptor(UARTProfile.TX_READ_CHAR_DESC,
                UARTProfile.DESCRIPTOR_PERMISSION);
        TX_READ_CHAR.addDescriptor(TX_READ_CHAR_DESC);


        UART_SERVICE.addCharacteristic(TX_READ_CHAR);
        mGattServer.addService(UART_SERVICE);
    }

    //advertise set-up; however, we need to remove the button at somepoint and add this whole section to onCreate!
    private void advertise() {
        final BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        //Set up the settings for the advertise packet
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(120000)
                .build();

        //make the uuid into proper bit size then set it to the data for advertising
        ParcelUuid pUuid = new ParcelUuid(UARTProfile.bleId);
        //create data for advertise packet
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .build();

        //create data for packet in response to being scanned
        AdvertiseData ScanResponseData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(pUuid)
                .build();

        //advertise callback to make sure its advertisement setup worked
        final AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d("BLE", "Advertising success");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e("BLE", "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
            }
        };
        //Gattserver call back to make account for connection changes and requests!
        BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        Log.d("BLE", "onConnectionStateChange: State Connected");
                        // Bluetooth LE peripheral stop advertising on connect with Bluetooth LE central device

                        Log.v("BLE", "Connected to device: " + device.getAddress());

                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        Log.v("BLE", "Disconnected from device");
                    }

                }
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                                            int offset, BluetoothGattDescriptor descriptor) {
                Log.d("HELLO", "Our gatt server descriptor was read.");
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                Log.d("DONE", "Our gatt server descriptor was read.");
            }
            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                super.onMtuChanged(device, mtu);
            }
        };
        //after setting up data packets for advertisement, set gat
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mGattServer = mBluetoothManager.openGattServer(this, callback);
        initServer();
        //begin to advertise
        advertiser.startAdvertising(settings, data, ScanResponseData, advertisingCallback);
    }


    }
