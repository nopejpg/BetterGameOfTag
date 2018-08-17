package com.bluecreation.melodysmartandroid;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bluecreation.melodysmart.I2CService;
import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmartandroid.databinding.ActivityI2cBinding;

public class I2cActivity extends BleActivity implements I2CService.Listener {
    ActivityI2cBinding binding;


    private static final String TAG = I2cActivity.class.getSimpleName();

    private I2CService i2CService;

    private static byte parseHexByte(String s) {
        return (byte) (0xff & Integer.parseInt(s, 16));
    }

    private static byte[] parseHexBytes(String s) {
        if (s.length() % 2 != 0) {
            s = "0" + s;
        }
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = parseHexByte(s.substring(2 * i, 2 * i + 2));
        }
        return bytes;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_i2c);

        setSupportActionBar(binding.toolbar);

        i2CService = MelodySmartDevice.getInstance().getI2CService();
        i2CService.registerListener(this);
        i2CService.enableNotifications(true);
    }

    @Override
    public void onDestroy() {
        i2CService.enableNotifications(false);
        i2CService.unregisterListener(this);

        super.onDestroy();
    }

    byte[] getRegisterAddress() {
        return parseHexBytes(binding.registerEditText.getText().toString());
    }

    byte[] getOutgoingData() {
        return parseHexBytes(binding.outgoingDataTextEdit.getText().toString());
    }

    byte getDeviceAddress() {
        return parseHexByte(binding.deviceEditText.getText().toString());
    }

    public void writeData(View view) {
        try {
            byte[] register = getRegisterAddress();
            byte[] data = getOutgoingData();
            byte[] fullData = new byte[register.length + data.length];
            System.arraycopy(register, 0, fullData, 0, register.length);
            System.arraycopy(data, 0, fullData, register.length, data.length);
            i2CService.writeData(fullData, getDeviceAddress());
        } catch (Exception e) {
            Toast.makeText(this, "Invalid parameters!", Toast.LENGTH_SHORT).show();
        }
    }

    public void readData(View view) {
        try {
            byte[] writePortion = getRegisterAddress();
            i2CService.readData(writePortion, getDeviceAddress(), (byte) 16);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid parameters!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void handleReply(final boolean success, byte[] data) {
        StringBuilder i2cDataString = new StringBuilder();
        for (byte b : data) {
            i2cDataString.append(String.format("0x%02x ", b));
        }

        final String result = i2cDataString.toString();
        Log.d(TAG, "Got I2C data : " + result);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (success) {
                    binding.incomingDataTextEdit.setText(result);
                } else {
                    Toast.makeText(I2cActivity.this, "Error happened", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onNotificationsEnabled(boolean state) {

    }
}
