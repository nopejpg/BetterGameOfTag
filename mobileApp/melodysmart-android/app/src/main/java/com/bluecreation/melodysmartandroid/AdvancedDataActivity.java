package com.bluecreation.melodysmartandroid;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bluecreation.melodysmart.DataService;
import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmartandroid.databinding.ActivityAdvancedDataBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdvancedDataActivity extends BleActivity implements DataService.Listener {

    private ActivityAdvancedDataBinding binding;
    private DataService dataService;
    private final List<Byte> receivedData = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_advanced_data);

        setSupportActionBar(binding.toolbar);

        dataService = MelodySmartDevice.getInstance().getDataService();
        dataService.registerListener(this);
        dataService.enableNotifications(true);
    }

    @Override
    public void onDestroy() {
        dataService.unregisterListener(this);

        super.onDestroy();
    }

    @Override
    public void onReceived(byte[] bytes) {
        if (binding.swEcho.isChecked()) {
            dataService.send(bytes);
        }

        if (binding.swLogging.isChecked()) {
            synchronized (receivedData) {
                for (byte b : bytes) {
                    receivedData.add(b);
                }
            }

            updateUi();
        }
    }

    private void updateUi() {
        binding.tvLoggedDataSize.post(new Runnable() {
            @Override
            public void run() {
                String text = String.format(Locale.getDefault(), "Logged data size: %db", receivedData.size());
                binding.tvLoggedDataSize.setText(text);
            }
        });
    }

    @Override
    public void onNotificationsEnabled(boolean b) {

    }

    public void sendDataOverEmail(View view) {
        File outputDir = getExternalCacheDir();
        try {
            File file = File.createTempFile("melodysmart_data_", ".bin", outputDir);
            FileOutputStream fs = new FileOutputStream(file);

            byte[] data = byteListToByteArray(receivedData);
            fs.write(data);
            fs.close();

            sendEmail("Your MelodySmart data", "Here's your MelodySmart data", file);
        } catch (IOException e) {
            Toast.makeText(this, "Couldn't write the data to disk!", Toast.LENGTH_SHORT).show();
        }
    }

    byte[] byteListToByteArray(List<Byte> byteList) {
        byte data[] = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            data[i] = receivedData.get(i);
        }

        return data;
    }

    public void sendDataOverBle(View view) {
        synchronized (receivedData) {
            int index = 0;


            while (index < receivedData.size()) {
                int tx = Math.min(receivedData.size() - index, dataService.getMtu() - 3);

                byte[] data = byteListToByteArray(receivedData.subList(index, index + tx));
                dataService.send(data);

                index += tx;

                updateUi();
            }

            receivedData.clear();
        }

        updateUi();
    }

    public void clearData(View view) {
        synchronized (receivedData) {
            receivedData.clear();
        }

        updateUi();
    }

    void sendEmail(String subject, String body, File attachment)
    {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("application/octet-stream");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachment));

        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }
}
