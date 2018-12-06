package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;


public class AutomatedTagActivity extends AppCompatActivity {
    Button autoTagQuitButton;
    TextView autoTagPod1Display;
    TextView autoTagPod2Display;
    TextView autoTagPod3Display;
    TextView autoTagPod1Label;
    TextView autoTagPod2Label;
    TextView autoTagPod3Label;
    BluetoothServices bluetooth;
    String messageStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automated_tag);
        autoTagQuitButton = (Button) findViewById(R.id.autoTagQuitButton);
        autoTagQuitButton.setEnabled(false);

        autoTagPod1Display = findViewById(R.id.autoTagPod1Display);
        autoTagPod2Display = findViewById(R.id.autoTagPod2Display);
        autoTagPod3Display = findViewById(R.id.autoTagPod3Display);
        autoTagPod1Label = findViewById(R.id.autoTagPod1Label);
        autoTagPod2Label = findViewById(R.id.autoTagPod2Label);
        autoTagPod3Label = findViewById(R.id.autoTagPod3Label);

        autoTagPod1Display.setVisibility(View.INVISIBLE);
        autoTagPod2Display.setVisibility(View.INVISIBLE);
        autoTagPod3Display.setVisibility(View.INVISIBLE);
        autoTagPod1Label.setVisibility(View.INVISIBLE);
        autoTagPod2Label.setVisibility(View.INVISIBLE);
        autoTagPod3Label.setVisibility(View.INVISIBLE);

        Toast.makeText(getApplicationContext(),"Connecting to hub...",Toast.LENGTH_LONG).show();
        bluetooth = new BluetoothServices(this);

        bluetooth.setBluetoothServicesListener(new BluetoothServices.BluetoothServicesListener() {
            @Override
            public void onCommsReady() {
                if (bluetooth.commsReady) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        Log.d("BLE","Comms ready callback");
                        Toast.makeText(getApplicationContext(),"Connected! Starting Game...", Toast.LENGTH_LONG).show();
                        autoTagQuitButton.setEnabled(true);
                            autoTagPod1Display.setVisibility(View.VISIBLE);
                            autoTagPod2Display.setVisibility(View.VISIBLE);
                            autoTagPod3Display.setVisibility(View.VISIBLE);
                            autoTagPod1Label.setVisibility(View.VISIBLE);
                            autoTagPod2Label.setVisibility(View.VISIBLE);
                            autoTagPod3Label.setVisibility(View.VISIBLE);

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                messageStatus = bluetooth.sendMessage("AUTOMATE_TAG");
                                processMessageStatus(messageStatus, "AUTOMATE_TAG");
                            }
                        }, 1000);
                        }
                    });
                }
            }

            @Override
            public void onSystemHealth(final String status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boolean pod1Status = status.substring(0,1).equals("1");
                        boolean pod2Status = status.substring(1,2).equals("1");
                        boolean pod3Status = status.substring(2,3).equals("1");

                        setText(pod1Status,autoTagPod1Display);
                        setText(pod2Status,autoTagPod2Display);
                        setText(pod3Status,autoTagPod3Display);
                    }
                });
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
            Toast.makeText(getApplicationContext(), "Invalid Message!!!", Toast.LENGTH_LONG).show();
        }
        if (status.equals("notSent") || status.equals("notConnected")) {
            Toast.makeText(getApplicationContext(), "Out of sync with hub, resetting to main menu", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getApplicationContext(),"message received by hub",Toast.LENGTH_LONG).show();
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

        Toast.makeText(getApplicationContext(),"Unable to send message after retries, exiting to main menu", Toast.LENGTH_LONG).show();
        exitToMainMenu();
    }

    void setText(boolean status, TextView podDisplay) {
        if (status) {
            podDisplay.setText("Connected");
            podDisplay.setTextColor(getResources().getColor(R.color.connected));
        } else {
            podDisplay.setText("Not Connected");
            podDisplay.setTextColor(getResources().getColor(R.color.disconnected));
        }
    }
}
