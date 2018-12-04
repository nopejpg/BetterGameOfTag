package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;


public class AutomatedTagActivity extends AppCompatActivity {
    Button autoTagPlayButton;
    Button autoTagQuitButton;
    BluetoothServices bluetooth;
    String messageStatus = "";
    int duration = Toast.LENGTH_SHORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automated_tag);
        autoTagPlayButton = findViewById(R.id.autoTagPlayButton);
        autoTagQuitButton = findViewById(R.id.autoTagQuitButton);
        autoTagPlayButton.setEnabled(false);
        autoTagQuitButton.setEnabled(false);

        Toast.makeText(getApplicationContext(),"Connecting to hub...",duration).show();
        bluetooth = new BluetoothServices(this);

        bluetooth.setBluetoothServicesListener(new BluetoothServices.BluetoothServicesListener() {
            @Override
            public void onCommsReady() {
                if (bluetooth.commsReady) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("BLE","Comms ready callback");
                            Toast.makeText(getApplicationContext(),"Connected!", duration).show();
                            autoTagPlayButton.setEnabled(true);
                            autoTagQuitButton.setEnabled(true);
                        }
                    });
                }
            }
        });

        autoTagPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageStatus = bluetooth.sendMessage("AUTOMATE_TAG");
                processMessageStatus(messageStatus, "AUTOMATE_TAG");
            }
        });

        autoTagQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageStatus = bluetooth.sendMessage("EXIT_GAME");
                processMessageStatus(messageStatus,"EXIT_GAME");
            }
        });
    }

    void processMessageStatus(String status, String message) {
        if (status.equals("invalidMessage")) {
            Toast.makeText(getApplicationContext(), "Invalid Message!!!", duration).show();
        }
        if (status.equals("notSent") || status.equals("notConnected")) {
            Toast.makeText(getApplicationContext(), "Out of sync with hub, resetting to main menu", duration).show();
            exitToMainMenu();
        }
        if (status.equals("sent")) {
            if (!bluetooth.lastMessageAcked) {
                waitForAck(message);
            }
        }
    }

    void exitToMainMenu() {
        Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
        bluetooth.disconnectGattServer();
        startActivity(intent);
    }

    void waitForAck(String message) {
        int retry = 0;

        while (retry < 3) {
            try {
                Thread.sleep(3000);
                if (bluetooth.lastMessageAcked) {
                    Toast.makeText(getApplicationContext(),"message received by hub",duration).show();
                    if (message.equals("EXIT_GAME")) {
                        exitToMainMenu();
                    }
                    return;
                }
                bluetooth.sendMessage(message);
                retry++;
            } catch (InterruptedException ie) {
                 Log.e("sleep","Failed thread sleep");
            }
        }

        Toast.makeText(getApplicationContext(),"Unable to send message after retries, exiting to main menu", duration).show();
        exitToMainMenu();
    }
}
