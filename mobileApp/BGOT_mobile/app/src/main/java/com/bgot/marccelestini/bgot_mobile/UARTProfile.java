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

    public static UUID clientCharacteristicConfiguration = convertFromInteger(0x2902);

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

    public static String createPacket(String message) {
        int messageLength = message.length();
        int dataStartIndex = 2;
        char[] buffer = new char[20];

        buffer[0] = (char) Integer.parseInt("FD",16);
        buffer[1] = (char) messageLength;

        for (int i = 0; i < messageLength; i++) {
            int index = dataStartIndex + i;
            buffer[index] = (char) message.charAt(i);
        }

        int CRCStartIndex = dataStartIndex + messageLength;
        String CRCString = Integer.toHexString(createCRC(message, messageLength));
        buffer[CRCStartIndex] = (char) Integer.parseInt(CRCString.substring(6,8),16);
        buffer[CRCStartIndex + 1] = (char) Integer.parseInt(CRCString.substring(4,6),16);

        int packetLength = CRCStartIndex + 2;

        return String.valueOf(buffer).substring(0,packetLength);
    }

    public static int createCRC(String data, int length) {
        int[] crcArray = new int[] {
                0x0000, 0x1189, 0x2312, 0x329B,
                0x4624, 0x57AD, 0x6536, 0x74BF,
                0x8C48, 0x9DC1, 0xAF5A, 0xBED3,
                0xCA6C, 0xDBE5, 0xE97E, 0xF8F7,
                0x1081, 0x0108, 0x3393, 0x221A,
                0x56A5, 0x472C, 0x75B7, 0x643E,
                0x9CC9, 0x8D40, 0xBFDB, 0xAE52,
                0xDAED, 0xCB64, 0xF9FF, 0xE876,
                0x2102, 0x308B, 0x0210, 0x1399,
                0x6726, 0x76AF, 0x4434, 0x55BD,
                0xAD4A, 0xBCC3, 0x8E58, 0x9FD1,
                0xEB6E, 0xFAE7, 0xC87C, 0xD9F5,
                0x3183, 0x200A, 0x1291, 0x0318,
                0x77A7, 0x662E, 0x54B5, 0x453C,
                0xBDCB, 0xAC42, 0x9ED9, 0x8F50,
                0xFBEF, 0xEA66, 0xD8FD, 0xC974,
                0x4204, 0x538D, 0x6116, 0x709F,
                0x0420, 0x15A9, 0x2732, 0x36BB,
                0xCE4C, 0xDFC5, 0xED5E, 0xFCD7,
                0x8868, 0x99E1, 0xAB7A, 0xBAF3,
                0x5285, 0x430C, 0x7197, 0x601E,
                0x14A1, 0x0528, 0x37B3, 0x263A,
                0xDECD, 0xCF44, 0xFDDF, 0xEC56,
                0x98E9, 0x8960, 0xBBFB, 0xAA72,
                0x6306, 0x728F, 0x4014, 0x519D,
                0x2522, 0x34AB, 0x0630, 0x17B9,
                0xEF4E, 0xFEC7, 0xCC5C, 0xDDD5,
                0xA96A, 0xB8E3, 0x8A78, 0x9BF1,
                0x7387, 0x620E, 0x5095, 0x411C,
                0x35A3, 0x242A, 0x16B1, 0x0738,
                0xFFCF, 0xEE46, 0xDCDD, 0xCD54,
                0xB9EB, 0xA862, 0x9AF9, 0x8B70,
                0x8408, 0x9581, 0xA71A, 0xB693,
                0xC22C, 0xD3A5, 0xE13E, 0xF0B7,
                0x0840, 0x19C9, 0x2B52, 0x3ADB,
                0x4E64, 0x5FED, 0x6D76, 0x7CFF,
                0x9489, 0x8500, 0xB79B, 0xA612,
                0xD2AD, 0xC324, 0xF1BF, 0xE036,
                0x18C1, 0x0948, 0x3BD3, 0x2A5A,
                0x5EE5, 0x4F6C, 0x7DF7, 0x6C7E,
                0xA50A, 0xB483, 0x8618, 0x9791,
                0xE32E, 0xF2A7, 0xC03C, 0xD1B5,
                0x2942, 0x38CB, 0x0A50, 0x1BD9,
                0x6F66, 0x7EEF, 0x4C74, 0x5DFD,
                0xB58B, 0xA402, 0x9699, 0x8710,
                0xF3AF, 0xE226, 0xD0BD, 0xC134,
                0x39C3, 0x284A, 0x1AD1, 0x0B58,
                0x7FE7, 0x6E6E, 0x5CF5, 0x4D7C,
                0xC60C, 0xD785, 0xE51E, 0xF497,
                0x8028, 0x91A1, 0xA33A, 0xB2B3,
                0x4A44, 0x5BCD, 0x6956, 0x78DF,
                0x0C60, 0x1DE9, 0x2F72, 0x3EFB,
                0xD68D, 0xC704, 0xF59F, 0xE416,
                0x90A9, 0x8120, 0xB3BB, 0xA232,
                0x5AC5, 0x4B4C, 0x79D7, 0x685E,
                0x1CE1, 0x0D68, 0x3FF3, 0x2E7A,
                0xE70E, 0xF687, 0xC41C, 0xD595,
                0xA12A, 0xB0A3, 0x8238, 0x93B1,
                0x6B46, 0x7ACF, 0x4854, 0x59DD,
                0x2D62, 0x3CEB, 0x0E70, 0x1FF9,
                0xF78F, 0xE606, 0xD49D, 0xC514,
                0xB1AB, 0xA022, 0x92B9, 0x8330,
                0x7BC7, 0x6A4E, 0x58D5, 0x495C,
                0x3DE3, 0x2C6A, 0x1EF1, 0x0F78
        };

        int crc = 0xFFFF;

        for (int j = 0; j < length; j++) {
            crc = crcArray[(crc ^ data.charAt(j)) & 0xFF] ^ (crc >> 8);
        }
        crc = ~(crc);
        return crc;
    }


}
