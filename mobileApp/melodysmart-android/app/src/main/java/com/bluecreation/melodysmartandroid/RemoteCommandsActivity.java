package com.bluecreation.melodysmartandroid;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmart.RemoteCommandsService;
import com.bluecreation.melodysmartandroid.databinding.ActivityRemoteCommandsBinding;

public class RemoteCommandsActivity extends BleActivity implements RemoteCommandsService.Listener {

    private static final String TAG = RemoteCommandsActivity.class.getSimpleName();

    ActivityRemoteCommandsBinding binding;
    private RemoteCommandsService remoteCommandsService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_remote_commands);

        setSupportActionBar(binding.toolbar);

        binding.remoteCommandSendEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                remoteCommandsService.send(binding.remoteCommandSendEditText.getText().toString());
                binding.remoteCommandSendEditText.setText("");
                return true;
            }
        });

        remoteCommandsService = MelodySmartDevice.getInstance().getRemoteCommandsService();
        remoteCommandsService.registerListener(this);
        remoteCommandsService.enableNotifications(true);
    }

    @Override
    public void onDestroy() {
        remoteCommandsService.unregisterListener(this);
        remoteCommandsService.enableNotifications(false);
        super.onDestroy();
    }

    @Override
    public void handleReply(final byte[] reply) {
        Log.d(TAG, "Got command response : " + new String(reply));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.remoteCommandResponseEditText.append(new String(reply) + "\n");
            }
        });
    }

    @Override
    public void onNotificationsEnabled(boolean state) {
    }
}
