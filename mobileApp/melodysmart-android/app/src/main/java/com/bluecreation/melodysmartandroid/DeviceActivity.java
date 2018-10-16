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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bluecreation.melodysmart.BLEError;
import com.bluecreation.melodysmart.DataService;
import com.bluecreation.melodysmart.DeviceDatabase;
import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmart.MelodySmartListener;
import com.bluecreation.melodysmartandroid.databinding.ActivityDeviceBinding;

import static com.bluecreation.melodysmartandroid.R.*;

import java.util.Locale;

public class DeviceActivity extends BleActivity implements View.OnClickListener,MelodySmartListener {

    private static final int REQUEST_CODE_START_OTAU = 0;
    private static final int REQUEST_CODE_START_I2C = 1;
    private static final int REQUEST_CODE_START_REMOTE_COMMANDS = 2;

    private static String EXTRA_DEVICE_ADDRESS = "getDeviceAddress";
    private static String EXTRA_DEVICE_NAME = "deviceName";

    private String TAG = DeviceActivity.class.getSimpleName();

    private TextView mText;
    private Button mStartButton;
    private Button mGame1Button;
    private Button mGame2Button;
    private Button mExitButton;

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

    public void onClick(View v) {
        String bleMessage = "";
        if (v.getId() == id.buttonStart) {
            mText.setText("Start Button Clicked");
            //bleMessage = "˝MAN_TAG1-";
            //bleMessage = hexToAscii("FD074D414E5F544147312D");
            bleMessage = createPacket("MAN_TAG");
        }
        else if (v.getId() == id.buttonGame1) {
            mText.setText("Game Config 1 Clicked");
            //bleMessage = "˝%SUS";
            //bleMessage = hexToAscii("FD04255355530CBC");
            bleMessage = createPacket("%SUS");
        }
        else if (v.getId() == id.buttonGame2) {
            mText.setText("Game Config 2 Clicked");
            //bleMessage = "˝%USU3[";
            //bleMessage = hexToAscii("FD0425555355335B");
            bleMessage = createPacket("%USU");
        }
        else if (v.getId() == id.buttonExit) {
            mText.setText("Exit Button Clicked");
            //bleMessage = "˝ EXIT_GAMEN";
            //bleMessage = hexToAscii("FD09455849545F47414D454E01");
            bleMessage = createPacket("EXIT_GAME");
        }
        sendMessage(bleMessage);
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i=0; i< hexStr.length() ; i+=2) {
            String str = hexStr.substring(i,i+2);
            output.append((char) Integer.parseInt(str,16));
        }

        return output.toString();
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

    private void sendMessage(String bleMessage) {
//        String bleName = "BC49EBC";
//        String bleAddr = "20FABB049EBC";
//        //addresses below for debugging - Marc
//        String bleName = "BC49E7B";
//        String bleAddr = "20:FA:BB:04:9E:7B";

        try {
            device.getDataService().send(bleMessage.getBytes("ISO-8859-1"));
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            mText.setText("Message Failure");
        }
    }

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

        mText = (TextView) findViewById(id.status);
        mStartButton = (Button) findViewById(id.buttonStart);
        mGame1Button = (Button) findViewById(id.buttonGame1);
        mGame2Button = (Button) findViewById(id.buttonGame2);
        mExitButton = (Button) findViewById(id.buttonExit);

        mStartButton.setOnClickListener(this);
        mGame1Button.setOnClickListener(this);
        mGame2Button.setOnClickListener(this);
        mExitButton.setOnClickListener(this);

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
