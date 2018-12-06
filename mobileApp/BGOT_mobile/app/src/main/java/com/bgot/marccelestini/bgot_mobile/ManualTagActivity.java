package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ManualTagActivity extends AppCompatActivity {
    Button manualTagQuitButton;
    Button manualTagUpdateButton;
    Switch manualTagPod1Switch;
    Switch manualTagPod2Switch;
    Switch manualTagPod3Switch;
    TextView manTagPod1Display;
    TextView manTagPod2Display;
    TextView manTagPod3Display;
    TextView manTagPod1Label;
    TextView manTagPod2Label;
    TextView manTagPod3Label;
    BluetoothServices bluetooth;
    int duration = Toast.LENGTH_SHORT;
    String messageStatus = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_tag);
        manualTagQuitButton = (Button) findViewById(R.id.manualTagQuitButton);
        manualTagUpdateButton = (Button) findViewById(R.id.manualTagUpdateButton);
        manualTagPod1Switch = (Switch) findViewById(R.id.manualTagPod1Switch);
        manualTagPod2Switch = (Switch) findViewById(R.id.manualTagPod2Switch);
        manualTagPod3Switch = (Switch) findViewById(R.id.manualTagPod3Switch);

        manTagPod1Display = findViewById(R.id.manTagPod1Display);
        manTagPod2Display = findViewById(R.id.manTagPod2Display);
        manTagPod3Display = findViewById(R.id.manTagPod3Display);
        manTagPod1Label = findViewById(R.id.manTagPod1Label);
        manTagPod2Label = findViewById(R.id.manTagPod2Label);
        manTagPod3Label = findViewById(R.id.manTagPod3Label);

        manTagPod1Display.setVisibility(View.INVISIBLE);
        manTagPod2Display.setVisibility(View.INVISIBLE);
        manTagPod3Display.setVisibility(View.INVISIBLE);
        manTagPod1Label.setVisibility(View.INVISIBLE);
        manTagPod2Label.setVisibility(View.INVISIBLE);
        manTagPod3Label.setVisibility(View.INVISIBLE);

        manualTagQuitButton.setEnabled(false);
        manualTagUpdateButton.setEnabled(false);

        Toast.makeText(getApplicationContext(),"Connecting to hub...",duration).show();
        bluetooth = new BluetoothServices(this);

        bluetooth.setBluetoothServicesListener(new BluetoothServices.BluetoothServicesListener() {
            @Override
            public void onCommsReady() {
                if (bluetooth.commsReady) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("BLE","Communications ready");
                            Toast.makeText(getApplicationContext(),"Connected! Starting Manual Tag...", duration).show();
                            manualTagQuitButton.setEnabled(true);
                            manualTagUpdateButton.setEnabled(true);
                            manTagPod1Display.setVisibility(View.VISIBLE);
                            manTagPod2Display.setVisibility(View.VISIBLE);
                            manTagPod3Display.setVisibility(View.VISIBLE);
                            manTagPod1Label.setVisibility(View.VISIBLE);
                            manTagPod2Label.setVisibility(View.VISIBLE);
                            manTagPod3Label.setVisibility(View.VISIBLE);
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    messageStatus = bluetooth.sendMessage("MAN_TAG");
                                    processMessageStatus(messageStatus, "MAN_TAG");
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

                        setText(pod1Status,manTagPod1Display);
                        setText(pod2Status,manTagPod2Display);
                        setText(pod3Status,manTagPod3Display);
                    }
                });
            }
        });

        manualTagQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageStatus = bluetooth.sendMessage("EXIT_GAME");
                processMessageStatus(messageStatus,"EXIT_GAME");
            }
        });

        manualTagUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String config1 = manualTagPod1Switch.isChecked() ? "S" : "U";
                String config2 = manualTagPod2Switch.isChecked() ? "S" : "U";
                String config3 = manualTagPod3Switch.isChecked() ? "S" : "U";
                String config = "%" + config1 + config2 + config3;

                messageStatus = bluetooth.sendMessage(config);
                processMessageStatus(messageStatus,config);
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
