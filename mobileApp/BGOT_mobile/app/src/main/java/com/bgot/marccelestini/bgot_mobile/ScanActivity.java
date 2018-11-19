package com.bgot.marccelestini.bgot_mobile;

import android.Manifest;
import android.app.ListActivity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
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
import android.location.Location;
import android.location.LocationManager;

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
import java.io.*;

import android.content.Intent;

import static com.bgot.marccelestini.bgot_mobile.R.*;

public class ScanActivity extends AppCompatActivity {
    //variables for xml to edit
    private TextView mText;
    private Button mAdvertiseButton;
    private Button advertiseSkipButton;
    private Button connectButton;
    private Button testAutoTag;
    private Button testQuitGame;
    private Button testManTag;
    private Button testRLGL;
    private Button testSUS;
    private Button testUSU;
    private TextView testOutputText;
    //UUIDs "They're also in the strings.xml. 'm having a hard time using R.strings  command... So they're hardcoded" **now theyre in UARTProfile.java
    private BluetoothGattServer mGattServer;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattCharacteristic commsCharacteristic;
    private BluetoothGattDescriptor commsDescriptor;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothLeScanner btScanner;
    boolean advertising;
    boolean gattInitialized = false;
    boolean connectedToPeripheral = false;
    boolean scanning = false;
    boolean commsReady = false;
    boolean hubIsReady = false;

    UUID communicationServiceUUID = UARTProfile.msService1;
    UUID communicationCharacteristic = UARTProfile.msService1Characteristic1;
    UUID communicationDiscriptor = UARTProfile.clientCharacteristicConfiguration;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    //create pages
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BluetoothAdapter.getDefaultAdapter().setName("BGOT");
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_scan);
        mText = findViewById(id.text);
        advertising = false;


        //advertiseSkipButton = findViewById(id.advertiseSkipButton);
//        advertiseSkipButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent mainMenuIntent = new Intent(v.getContext(), MainMenuActivity.class);
//                startActivity(mainMenuIntent);
//            }
//        });

        connectButton = findViewById(id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!connectedToPeripheral) {
                    connectAsCentral();
                }
            }
        });

        testManTag = findViewById(id.testManTag);
        testManTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("MAN_TAG");
            }
        });

        testAutoTag = findViewById(id.testAutoTag);
        testAutoTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("AUTOMATE_TAG");
            }
        });

        testRLGL = findViewById(id.testRLGL);
        testRLGL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("RL_GL");
            }
        });

        testSUS = findViewById(id.testManTagSUS);
        testSUS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("%SUS");
            }
        });

        testUSU = findViewById(id.testManTagUSU);
        testUSU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("%USU");
            }
        });

        testQuitGame = findViewById(id.testQuitGame);
        testQuitGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("EXIT_GAME");
            }
        });
    }

    private void sendMessage(String mes) {
        if (!commsReady) {
//            testOutputText.setText("Communication service not connected");
            connectAsCentral();
            return;
        }

//         if (!hubIsReady) {
//             testOutputText.setText("Hub not ready to receive data");
//             return;
//         }

        String message = UARTProfile.createPacket(mes);

        byte[] messageBytes = new byte[0];
        try {
            messageBytes = message.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            Log.e("ERR", "Failed to convert message string to byte array");
        }

        commsCharacteristic.setValue(messageBytes);
        boolean success = mBluetoothGatt.writeCharacteristic(commsCharacteristic);
//        if (success) {
//            testOutputText.setText("successful write");
//        } else {
//            testOutputText.setText("UNsuccessful write");
//        }
    }

    private void connectAsCentral() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        btScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

//        testOutputText.setText("Scanning...");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanning = true;
                btScanner.startScan(leScanCallback);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Cannot connect without location permission granted");;
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("BLE Scan", "Name: " +result.getDevice().getName());
            String deviceAddress = result.getDevice().getAddress();

//            if (deviceAddress.equals("20:FA:BB:04:9E:7B") && !connectedToPeripheral) { //<--Marc's BLE
            if (deviceAddress.equals("20:FA:BB:04:9E:BC") && !connectedToPeripheral) { //<--Forrest's BLE
//                testOutputText.setText("Hub Discovered");

                mBluetoothGatt = result.getDevice().connectGatt(getApplicationContext(), true, mGattCallback);

                if (scanning) {
                    stopScanning();
                }
            }
        }
    };

    private void stopScanning() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanning = false;
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            }
            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedToPeripheral = true;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] messageBytes = characteristic.getValue();
            String messageString = null;
            try {
                messageString = new String(messageBytes, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                Log.e("ERR", "Unable to convert message bytes to string");
            }
            Log.d("DBG","Received message: " + messageString);
//            if (messageString.contains("SEND_OK")) {
//                hubIsReady = true;
//            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }

            commsCharacteristic = mBluetoothGatt.getService(communicationServiceUUID)
                        .getCharacteristic(communicationCharacteristic);
            commsDescriptor = commsCharacteristic.getDescriptor(communicationDiscriptor);
            commsDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            //commsDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            boolean descriptorSuccess = gatt.writeDescriptor(commsDescriptor);
            if (descriptorSuccess) {
                Log.d("BLE Services", "read notifications enabled");
            }
            commsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            commsReady = gatt.setCharacteristicNotification(commsCharacteristic, true);
            Log.d("BLE","Communications service is ready");
            //testOutputText.setText("Communication service ready");
//            hubIsReady = false;
        }
    };

    public void disconnectGattServer() {
        connectedToPeripheral = false;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
//        hubIsReady = false;
        commsReady = false;

    }


    //Initalize server by setting up UART  server
//    private void initServer() {
////        BluetoothGattService UART_SERVICE = new BluetoothGattService(UARTProfile.bleId,
////                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
////        BluetoothGattCharacteristic TX_READ_CHAR =
////                new BluetoothGattCharacteristic(UARTProfile.safeId,
////                        //Read-only characteristic, supports notifications
////                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
////                        BluetoothGattCharacteristic.PERMISSION_READ);
////
////        //Descriptor for read notifications
////        BluetoothGattDescriptor TX_READ_CHAR_DESC = new BluetoothGattDescriptor(UARTProfile.TX_READ_CHAR_DESC,
////                UARTProfile.DESCRIPTOR_PERMISSION);
////        TX_READ_CHAR.addDescriptor(TX_READ_CHAR_DESC);
////
////
////        UART_SERVICE.addCharacteristic(TX_READ_CHAR);
////        mGattServer.addService(UART_SERVICE);
//        BluetoothGattService msService1 = new BluetoothGattService(UARTProfile.msService1,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic msService1Char1 =
//                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic1,
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
//                BluetoothGattCharacteristic.PROPERTY_WRITE |
//                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService1Char2 =
//                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic2,
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
//                BluetoothGattCharacteristic.PROPERTY_WRITE,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService1Char3 =
//                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic3,
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
//                BluetoothGattCharacteristic.PROPERTY_READ |
//                BluetoothGattCharacteristic.PROPERTY_WRITE |
//                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
//                        BluetoothGattCharacteristic.PERMISSION_READ|
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService1Char4 =
//                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic4,
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
//                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService1Char5 =
//                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic5,
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY |
//                BluetoothGattCharacteristic.PROPERTY_READ |
//                BluetoothGattCharacteristic.PROPERTY_WRITE,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService1Char6 =
//                new BluetoothGattCharacteristic(UARTProfile.msService1Characteristic6,
//                    BluetoothGattCharacteristic.PROPERTY_NOTIFY |
//                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
//                    BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        msService1.addCharacteristic(msService1Char1);
//        msService1.addCharacteristic(msService1Char2);
//        msService1.addCharacteristic(msService1Char3);
//        msService1.addCharacteristic(msService1Char4);
//        msService1.addCharacteristic(msService1Char5);
//        msService1.addCharacteristic(msService1Char6);
//        mGattServer.addService(msService1);
//
//        BluetoothGattService msService2 = new BluetoothGattService(UARTProfile.msService2,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic msService2Char1 =
//                new BluetoothGattCharacteristic(UARTProfile.msService2Characteristic,
//                        BluetoothGattCharacteristic.PROPERTY_READ |
//                BluetoothGattCharacteristic.PROPERTY_WRITE,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService2Char2 =
//                new BluetoothGattCharacteristic(UARTProfile.msService2Characteristic,
//                        BluetoothGattCharacteristic.PROPERTY_WRITE,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic msService2Char3 =
//                new BluetoothGattCharacteristic(UARTProfile.msService2Characteristic,
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY|
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                        BluetoothGattCharacteristic.PERMISSION_READ |
//                BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        msService2.addCharacteristic(msService2Char1);
//        msService2.addCharacteristic(msService2Char2);
//        msService2.addCharacteristic(msService2Char3);
//        mGattServer.addService(msService2);
//
//        BluetoothGattService genericAttributeService = new BluetoothGattService(UARTProfile.genericAttribute,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic serviceChanged = new BluetoothGattCharacteristic(UARTProfile.serviceChanged,
//                BluetoothGattCharacteristic.PROPERTY_INDICATE |
//        BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        genericAttributeService.addCharacteristic(serviceChanged);
//        mGattServer.addService(genericAttributeService);
//
//        BluetoothGattService genericAccessService = new BluetoothGattService(UARTProfile.genericAccess,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic deviceName = new BluetoothGattCharacteristic(UARTProfile.deviceName,
//                BluetoothGattCharacteristic.PROPERTY_READ |
//        BluetoothGattCharacteristic.PROPERTY_WRITE,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic appearance = new BluetoothGattCharacteristic(UARTProfile.appearance,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic peripheralPreferredConnectionParameters = new BluetoothGattCharacteristic(UARTProfile.peripheralPreferredConnecectionParameters,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        genericAccessService.addCharacteristic(deviceName);
//        genericAccessService.addCharacteristic(appearance);
//        genericAccessService.addCharacteristic(peripheralPreferredConnectionParameters);
//        mGattServer.addService(genericAccessService);
//
//        BluetoothGattService deviceInformationService = new BluetoothGattService(UARTProfile.deviceInformation,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic serialNumberString = new BluetoothGattCharacteristic(UARTProfile.serialNumberString,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic modelNumberString = new BluetoothGattCharacteristic(UARTProfile.modelNumberString,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic systemID = new BluetoothGattCharacteristic(UARTProfile.systemID,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic hardwareRevisionString = new BluetoothGattCharacteristic(UARTProfile.hardwareRevisionString,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic firmwareRevisionString = new BluetoothGattCharacteristic(UARTProfile.firmwareRevisionString,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic softwareRevisionString = new BluetoothGattCharacteristic(UARTProfile.softwareRevisionString,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic manufacturerNameString = new BluetoothGattCharacteristic(UARTProfile.manufacturerNameString,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        BluetoothGattCharacteristic pnpID = new BluetoothGattCharacteristic(UARTProfile.pnpID,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        deviceInformationService.addCharacteristic(serialNumberString);
//        deviceInformationService.addCharacteristic(modelNumberString);
//        deviceInformationService.addCharacteristic(systemID);
//        deviceInformationService.addCharacteristic(hardwareRevisionString);
//        deviceInformationService.addCharacteristic(firmwareRevisionString);
//        deviceInformationService.addCharacteristic(softwareRevisionString);
//        deviceInformationService.addCharacteristic(manufacturerNameString);
//        deviceInformationService.addCharacteristic(pnpID);
//        mGattServer.addService(deviceInformationService);
//
//        BluetoothGattService batteryService = new BluetoothGattService(UARTProfile.batteryService,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic batteryLevel = new BluetoothGattCharacteristic(UARTProfile.batteryLevel,
//                BluetoothGattCharacteristic.PROPERTY_READ |
//        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        batteryService.addCharacteristic(batteryLevel);
//        mGattServer.addService(batteryService);
//
//        BluetoothGattService linkLossService = new BluetoothGattService(UARTProfile.linkLoss,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic alertLevel1 = new BluetoothGattCharacteristic(UARTProfile.alertLevel,
//                BluetoothGattCharacteristic.PROPERTY_READ |
//        BluetoothGattCharacteristic.PROPERTY_WRITE,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        linkLossService.addCharacteristic(alertLevel1);
//        mGattServer.addService(linkLossService);
//
//        BluetoothGattService immediateAlertService = new BluetoothGattService(UARTProfile.immediateAlert,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic alertLevel2 = new BluetoothGattCharacteristic(UARTProfile.alertLevel,
//                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        immediateAlertService.addCharacteristic(alertLevel2);
//        mGattServer.addService(immediateAlertService);
//
//        BluetoothGattService txPowerService = new BluetoothGattService(UARTProfile.txPower,
//                BluetoothGattService.SERVICE_TYPE_PRIMARY);
//
//        BluetoothGattCharacteristic txPowerLevel = new BluetoothGattCharacteristic(UARTProfile.txPowerLevel,
//                BluetoothGattCharacteristic.PROPERTY_READ,
//                BluetoothGattCharacteristic.PERMISSION_READ |
//        BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//        txPowerService.addCharacteristic(txPowerLevel);
//        mGattServer.addService(txPowerService);
//
//        gattInitialized = true;
//    }

    //advertise set-up; however, we need to remove the button at somepoint and add this whole section to onCreate!
//    private void advertise() {
//        final BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
//        //Set up the settings for the advertise packet
//        AdvertiseSettings advertisementSettings = new AdvertiseSettings.Builder()
//                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
//                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
//                .setConnectable(true)
//                .setTimeout(0)
//                .build();
//
//        //make the uuid into proper bit size then set it to the data for advertising
//        //ParcelUuid pUuid = new ParcelUuid(UARTProfile.bleId);
//        ParcelUuid pUuid = new ParcelUuid(UARTProfile.msService1);
//        //create data for advertise packet
//
//        AdvertiseData advertisingData = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(false)
//                .addServiceUuid(pUuid)
//                .build();
//
//        //create data for packet in response to being scanned
//        AdvertiseData ScanResponseData = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true)
//                .setIncludeTxPowerLevel(false)
//                .addServiceUuid(pUuid)
//                .build();
//
//        //advertise callback to make sure its advertisement setup worked
//        final AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
//            @Override
//            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
//                super.onStartSuccess(settingsInEffect);
//                Log.d("BLE", "Advertising onStartSuccess");
//                advertising = true;
//            }
//
//            @Override
//            public void onStartFailure(int errorCode) {
//                Log.e("BLE", "Advertising onStartFailure: " + errorCode);
//                super.onStartFailure(errorCode);
//                advertising = false;
//            }
//        };
//        //Gattserver call back to make account for connection changes and requests!
//        BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
//            @Override
//            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
//                super.onConnectionStateChange(device, status, newState);
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    if (newState == BluetoothGatt.STATE_CONNECTED) {
//                        Log.d("BLE", "onConnectionStateChange: State Connected");
//                        // Bluetooth LE peripheral stop advertising on connect with Bluetooth LE central device
//
//                        Log.v("BLE", "Connected to device: " + device.getAddress());
//
//                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
//                        Log.v("BLE", "Disconnected from device");
//                    }
//
//                }
//            }
//
//            @Override
//            public void onServiceAdded(int status, BluetoothGattService service) {
//                super.onServiceAdded(status, service);
//            }
//
//            @Override
//            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
//                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
//            }
//
//            @Override
//            public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
//                                                            int offset, BluetoothGattDescriptor descriptor) {
//                Log.d("HELLO", "Our gatt server descriptor was read.");
//                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
//                Log.d("DONE", "Our gatt server descriptor was read.");
//            }
//            @Override
//            public void onNotificationSent(BluetoothDevice device, int status) {
//                super.onNotificationSent(device, status);
//            }
//
//            @Override
//            public void onMtuChanged(BluetoothDevice device, int mtu) {
//                super.onMtuChanged(device, mtu);
//            }
//        };
//        //after setting up data packets for advertisement, set gat
//        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
//        mGattServer = mBluetoothManager.openGattServer(this, gattServerCallback);
//        if (gattInitialized == false) {
//            initServer();
//        }
//        //begin to advertise
//        advertiser.startAdvertising(advertisementSettings, advertisingData, ScanResponseData, advertisingCallback);
//    }


    }
