package com.bgot.marccelestini.bgot_mobile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import java.util.UUID;

public class UARTProfile {
    //Service UUID to expose our UART characteristics
    public static UUID bleId = UUID.fromString("886c6b6b-1974-4506-a569-80fc63642dba");

    //RX, Write characteristic currently  not needed

    //TX READ Notify
    public static UUID safeId = UUID.fromString("886c6b6c-1974-4506-a569-80fc63642dba");
    public static UUID TX_READ_CHAR_DESC = UUID.fromString("886c6b6d-1974-4506-a569-80fc63642dba");
    public final static int DESCRIPTOR_PERMISSION = BluetoothGattDescriptor.PERMISSION_WRITE;

    public static String getStateDescription(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return "Unknown State "+state;
        }
    }


    public static String getStatusDescription(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            default:
                return "Unknown Status "+status;
        }
    }


}
