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
    boolean advertising;
    boolean gattInitialized = false;


    //create pages
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BluetoothAdapter.getDefaultAdapter().setName("BGOT");
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_scan);
        mText = findViewById(id.text);
        advertising = false;
        mAdvertiseButton = findViewById(id.advertise_btn);
        mAdvertiseButton.setOnClickListener(this);
    }

    //when the button is clicked, begin advertise
    @Override
    public void onClick(View v) {
        if (v.getId() == id.advertise_btn) {
            if (advertising == false) {
                advertise();
            } else {
                Log.d("BLE", "Already advertising");
            }

        }
    }
    //Initalize server by setting up UART  server
    private void initServer() {
//        BluetoothGattService UART_SERVICE = new BluetoothGattService(UARTProfile.bleId,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);

//        BluetoothGattCharacteristic TX_READ_CHAR =
//                new BluetoothGattCharacteristic(UARTProfile.safeId,
//                        //Read-only characteristic, supports notifications
//                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
//                        BluetoothGattCharacteristic.PERMISSION_READ);
//
//        //Descriptor for read notifications
//        BluetoothGattDescriptor TX_READ_CHAR_DESC = new BluetoothGattDescriptor(UARTProfile.TX_READ_CHAR_DESC,
//                UARTProfile.DESCRIPTOR_PERMISSION);
//        TX_READ_CHAR.addDescriptor(TX_READ_CHAR_DESC);
//
//
//        UART_SERVICE.addCharacteristic(TX_READ_CHAR);
//        mGattServer.addService(UART_SERVICE);
        BluetoothGattService msService1 = new BluetoothGattService(UARTProfile.msService1,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic msService1Char1 =
                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic1,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService1Char2 =
                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic2,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService1Char3 =
                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic3,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                        BluetoothGattCharacteristic.PERMISSION_READ|
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService1Char4 =
                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic4,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService1Char5 =
                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic5,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService1Char6 =
                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic6,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        msService1.addCharacteristic(msService1Char1);
        msService1.addCharacteristic(msService1Char2);
        msService1.addCharacteristic(msService1Char3);
        msService1.addCharacteristic(msService1Char4);
        msService1.addCharacteristic(msService1Char5);
        msService1.addCharacteristic(msService1Char6);
        mGattServer.addService(msService1);

        BluetoothGattService msService2 = new BluetoothGattService(UARTProfile.msService2,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic msService2Char1 =
                new BluetoothGattCharacteristic(UARTProfile.msService2Characteristic,
                        BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService2Char2 =
                new BluetoothGattCharacteristic(UARTProfile.msService2Characteristic,
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic msService2Char3 =
                new BluetoothGattCharacteristic(UARTProfile.msService2Characteristic,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY|
                BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        msService2.addCharacteristic(msService2Char1);
        msService2.addCharacteristic(msService2Char2);
        msService2.addCharacteristic(msService2Char3);
        mGattServer.addService(msService2);

        BluetoothGattService genericAttributeService = new BluetoothGattService(UARTProfile.genericAttribute,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic serviceChanged = new BluetoothGattCharacteristic(UARTProfile.serviceChanged,
                BluetoothGattCharacteristic.PROPERTY_INDICATE |
        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        genericAttributeService.addCharacteristic(serviceChanged);
        mGattServer.addService(genericAttributeService);

        BluetoothGattService genericAccessService = new BluetoothGattService(UARTProfile.genericAccess,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic deviceName = new BluetoothGattCharacteristic(UARTProfile.deviceName,
                BluetoothGattCharacteristic.PROPERTY_READ |
        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic appearance = new BluetoothGattCharacteristic(UARTProfile.appearance,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic peripheralPreferredConnectionParameters = new BluetoothGattCharacteristic(UARTProfile.peripheralPreferredConnecectionParameters,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        genericAccessService.addCharacteristic(deviceName);
        genericAccessService.addCharacteristic(appearance);
        genericAccessService.addCharacteristic(peripheralPreferredConnectionParameters);
        mGattServer.addService(genericAccessService);

        BluetoothGattService deviceInformationService = new BluetoothGattService(UARTProfile.deviceInformation,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic serialNumberString = new BluetoothGattCharacteristic(UARTProfile.serialNumberString,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic modelNumberString = new BluetoothGattCharacteristic(UARTProfile.modelNumberString,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic systemID = new BluetoothGattCharacteristic(UARTProfile.systemID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic hardwareRevisionString = new BluetoothGattCharacteristic(UARTProfile.hardwareRevisionString,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic firmwareRevisionString = new BluetoothGattCharacteristic(UARTProfile.firmwareRevisionString,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic softwareRevisionString = new BluetoothGattCharacteristic(UARTProfile.softwareRevisionString,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic manufacturerNameString = new BluetoothGattCharacteristic(UARTProfile.manufacturerNameString,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattCharacteristic pnpID = new BluetoothGattCharacteristic(UARTProfile.pnpID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        deviceInformationService.addCharacteristic(serialNumberString);
        deviceInformationService.addCharacteristic(modelNumberString);
        deviceInformationService.addCharacteristic(systemID);
        deviceInformationService.addCharacteristic(hardwareRevisionString);
        deviceInformationService.addCharacteristic(firmwareRevisionString);
        deviceInformationService.addCharacteristic(softwareRevisionString);
        deviceInformationService.addCharacteristic(manufacturerNameString);
        deviceInformationService.addCharacteristic(pnpID);
        mGattServer.addService(deviceInformationService);

        BluetoothGattService batteryService = new BluetoothGattService(UARTProfile.batteryService,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic batteryLevel = new BluetoothGattCharacteristic(UARTProfile.batteryLevel,
                BluetoothGattCharacteristic.PROPERTY_READ |
        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        batteryService.addCharacteristic(batteryLevel);
        mGattServer.addService(batteryService);

        BluetoothGattService linkLossService = new BluetoothGattService(UARTProfile.linkLoss,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic alertLevel1 = new BluetoothGattCharacteristic(UARTProfile.alertLevel,
                BluetoothGattCharacteristic.PROPERTY_READ |
        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        linkLossService.addCharacteristic(alertLevel1);
        mGattServer.addService(linkLossService);

        BluetoothGattService immediateAlertService = new BluetoothGattService(UARTProfile.immediateAlert,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic alertLevel2 = new BluetoothGattCharacteristic(UARTProfile.alertLevel,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        immediateAlertService.addCharacteristic(alertLevel2);
        mGattServer.addService(immediateAlertService);

        BluetoothGattService txPowerService = new BluetoothGattService(UARTProfile.txPower,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic txPowerLevel = new BluetoothGattCharacteristic(UARTProfile.txPowerLevel,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ |
        BluetoothGattCharacteristic.PERMISSION_WRITE);

        txPowerService.addCharacteristic(txPowerLevel);
        mGattServer.addService(txPowerService);

        gattInitialized = true;
    }

    //advertise set-up; however, we need to remove the button at somepoint and add this whole section to onCreate!
    private void advertise() {
        final BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        //Set up the settings for the advertise packet
        AdvertiseSettings advertisementSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        //make the uuid into proper bit size then set it to the data for advertising
        //ParcelUuid pUuid = new ParcelUuid(UARTProfile.bleId);
        ParcelUuid pUuid = new ParcelUuid(UARTProfile.msService1);
        //create data for advertise packet

        AdvertiseData advertisingData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(pUuid)
                .build();

        //create data for packet in response to being scanned
        AdvertiseData ScanResponseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(pUuid)
                .build();

        //advertise callback to make sure its advertisement setup worked
        final AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d("BLE", "Advertising onStartSuccess");
                advertising = true;
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e("BLE", "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
                advertising = false;
            }
        };
        //Gattserver call back to make account for connection changes and requests!
        BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
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
        mGattServer = mBluetoothManager.openGattServer(this, gattServerCallback);
        if (gattInitialized == false) {
            initServer();
        }
        //begin to advertise
        advertiser.startAdvertising(advertisementSettings, advertisingData, ScanResponseData, advertisingCallback);
    }


    }
