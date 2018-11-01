package com.bgot.marccelestini.bgot_mobile;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import java.util.UUID;

public class UARTProfile {
    //Service UUID to expose our UART characteristics
    //public static UUID bleId = UUID.fromString("886c6b6b-1974-4506-a569-80fc63642dba");

    //RX, Write characteristic currently  not needed

    //TX READ Notify
//    public static UUID safeId = UUID.fromString("886c6b6c-1974-4506-a569-80fc63642dba");
//    public static UUID TX_READ_CHAR_DESC = UUID.fromString("886c6b6d-1974-4506-a569-80fc63642dba");
//    public final static int DESCRIPTOR_PERMISSION = BluetoothGattDescriptor.PERMISSION_WRITE;

    // mocked melody smart
    public static UUID msService1 = UUID.fromString("bc2f4cc6-aaef-4351-9034-d66268e328f0");
    public static UUID msService2 = UUID.fromString("9bc5d610-c57b-11e3-9c1a-0800200c9a66");
    public static UUID msService1Characteristic1 = UUID.fromString("06d1e5e7-79ad-4a71-8faa-373789f7d93c");
    public static UUID msService1Characteristic2 = UUID.fromString("dd89e1a9-b698-4a25-8e6d-7d8fb2ed77ba");
    public static UUID msService1Characteristic3 = UUID.fromString("6f0e9b56-e175-4243-a20a-71ebdb92fe74");
    public static UUID msService1Characteristic4 = UUID.fromString("eb718970-adca-11e3-aca6-425861b86ab6");
    public static UUID msService1Characteristic5 = UUID.fromString("f372624b-6e84-4851-9b5f-272f33506bcd");
    public static UUID msService1Characteristic6 = UUID.fromString("818ae306-9c5b-448d-b51a-7add6a5d314d");
    public static UUID msService2Characteristic = UUID.fromString("9bc5d610-c57b-11e3-9c1a-0800200c9a66");

    //melody smart generic UUIDs
    public static UUID genericAttribute = convertFromInteger(0x1801);
    public static UUID serviceChanged = convertFromInteger(0x2A05);
    public static UUID genericAccess = convertFromInteger(0x1800);
    public static UUID deviceName = convertFromInteger(0x2A00);
    public static UUID appearance = convertFromInteger(0x2A01);
    public static UUID peripheralPreferredConnecectionParameters = convertFromInteger(0x2A04);
    public static UUID deviceInformation = convertFromInteger(0x180A);
    public static UUID serialNumberString = convertFromInteger(0x2A25);
    public static UUID modelNumberString = convertFromInteger(0x2A24);
    public static UUID systemID = convertFromInteger(0x2A23);
    public static UUID hardwareRevisionString = convertFromInteger(0x2A27);
    public static UUID firmwareRevisionString = convertFromInteger(0x2A26);
    public static UUID softwareRevisionString = convertFromInteger(0x2A28);
    public static UUID manufacturerNameString = convertFromInteger(0x2A29);
    public static UUID pnpID = convertFromInteger(0x2A50);
    public static UUID batteryService = convertFromInteger(0x180F);
    public static UUID batteryLevel = convertFromInteger(0x2A19);
    public static UUID linkLoss = convertFromInteger(0x1803);
    public static UUID alertLevel = convertFromInteger(0x2A06);
    public static UUID immediateAlert = convertFromInteger(0x1802);
    public static UUID txPower = convertFromInteger(0x1804);
    public static UUID txPowerLevel = convertFromInteger(0x2A07);

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

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }


}
