package com.bgot.marccelestini.bgot_mobile;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.content.Intent;
import android.os.AsyncTask;

import java.util.Arrays;
import java.util.UUID;
import java.io.*;


public class BluetoothServices{
    private BluetoothGattCharacteristic commsCharacteristic;
    private BluetoothGattDescriptor commsDescriptor;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothLeScanner btScanner;
    boolean scanning = false;
    public volatile boolean commsReady = false;
    protected Activity activity;
    private BluetoothServicesListener listener;
    boolean lastMessageAcked = false;

    UUID communicationServiceUUID = UARTProfile.msService1;
    UUID communicationCharacteristic = UARTProfile.msService1Characteristic1;
    UUID communicationDiscriptor = UARTProfile.clientCharacteristicConfiguration;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public interface BluetoothServicesListener {
        public void onCommsReady();
    }

    public void setBluetoothServicesListener(BluetoothServicesListener listener) {
        this.listener = listener;
    }

    public BluetoothServices(Activity activity) {
        // grab the correct activity
        this.activity = activity;
        this.listener = null;
        if (!commsReady) {
            disconnectGattServer();
            connectAsCentral();
        }
    }

    public String sendMessage(String mes) {
        //Check for correct messages only
        String[] messages = {
                "MAN_TAG","AUTOMATE_TAG","RL_GL","EXIT_GAME",
                "RUN","WALK","STOP",
                "%SSS","%SSU","%SUS","%SUU",
                "%USS","%USU","%UUS","%UUU",
                };

        if (!Arrays.asList(messages).contains(mes)) {
            Log.e("ERR","Invalid message " + mes);
            return "invalidMessage";
        }

        if (!commsReady) {
            disconnectGattServer();
            Log.d("BLE","Not connected to hub, exiting to main menu");
            return "notConnected";
        }

        String message = UARTProfile.createPacket(mes);

        byte[] messageBytes = new byte[0];
        try {
            messageBytes = message.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            Log.e("ERR", "Failed to convert message string to byte array");
        }

        commsCharacteristic.setValue(messageBytes);
        boolean successfulWrite = mBluetoothGatt.writeCharacteristic(commsCharacteristic);

        if (successfulWrite) {
            Log.d("BLE","Successfully sent " + mes);
            lastMessageAcked = false;
            return "sent";
        } else {
            Log.e("BLE","Unable to send " + mes);
            return "notSent";
        }

    }

    public void connectAsCentral(){

        final BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        btScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ERR","Coarse Location is not enabled");
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d("BLE","Starting BLE Scan");
                scanning = true;
                btScanner.startScan(leScanCallback);
            }
        });
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case PERMISSION_REQUEST_COARSE_LOCATION: {
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    System.out.println("coarse location permission granted");
//                } else {
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//                    builder.setTitle("Cannot connect without location permission granted");;
//                    builder.setPositiveButton(android.R.string.ok, null);
//                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                        }
//
//                    });
//                    builder.show();
//                }
//                return;
//            }
//        }
//    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("BLE Scan", "Name: " +result.getDevice().getName());
            String deviceAddress = result.getDevice().getAddress();

//                if (deviceAddress.equals("20:FA:BB:04:9E:9B")) { // <-- testing
                if (deviceAddress.equals("20:FA:BB:04:9E:B4")) { // <-- the actual hub

                Log.d("BLE","Hub discovered, connecting...");
                mBluetoothGatt = result.getDevice().connectGatt(activity.getApplicationContext(), true, mGattCallback);
                Log.d("BLE","Connection started, waiting for services...");

                if (scanning) {
                    Log.d("BLE","Stopping scan");
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
                Log.d("BLE","Scanning Stopped");
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
//                connectedToPeripheral = true;
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
            Log.d("BLE","Received message: " + messageString);

            //check for pod statuses
            if (messageString.contains("STAT")) {
                String status = messageString.substring(5,8);
                Log.d("SYSTEM", "Received Pod Status: " + status);
                ((MyApplicationBGOT) activity.getApplication()).setPodStatus(status);
            }

            if (messageString.contains("ACK")) {
                Log.d("SYSTEM", "Received ACK");
                lastMessageAcked = true;
            }
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

            boolean descriptorSuccess = gatt.writeDescriptor(commsDescriptor);
            if (descriptorSuccess) {
                Log.d("BLE", "Read notifications enabled");
            }
            commsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            commsReady = gatt.setCharacteristicNotification(commsCharacteristic, true);
            Log.d("BLE","Communications service is ready");

            //update listener if it exists
            if (listener != null) listener.onCommsReady();
        }
    };

    public void disconnectGattServer() {
        //disconnect any lingering ble connection
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
        commsReady = false;
    }
}
