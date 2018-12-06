package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class RLGLActivity extends AppCompatActivity {
    RadioButton rlglRedRadio;
    RadioButton rlglYellowRadio;
    RadioButton rlglGreenRadio;
    Button rlglQuitButton;

    TextView rlglPod1Display;
    TextView rlglPod2Display;
    TextView rlglPod3Display;
    TextView rlglPod1Label;
    TextView rlglPod2Label;
    TextView rlglPod3Label;

    BluetoothServices bluetooth;
    String messageStatus = "";
    int duration = Toast.LENGTH_SHORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rlgl);

        bluetooth=new BluetoothServices(this);

        rlglGreenRadio = findViewById(R.id.rlglGreenRadio);
        rlglRedRadio = findViewById(R.id.rlglRedRadio);
        rlglYellowRadio = findViewById(R.id.rlglYellowRadio);
        rlglQuitButton = findViewById(R.id.rlglQuitButton);

        rlglPod1Display = findViewById(R.id.rlglPod1Display);
        rlglPod2Display = findViewById(R.id.rlglPod2Display);
        rlglPod3Display = findViewById(R.id.rlglPod3Display);
        rlglPod1Label = findViewById(R.id.rlglPod1Label);
        rlglPod2Label = findViewById(R.id.rlglPod2Label);
        rlglPod3Label = findViewById(R.id.rlglPod3Label);

        rlglPod1Display.setVisibility(View.INVISIBLE);
        rlglPod2Display.setVisibility(View.INVISIBLE);
        rlglPod3Display.setVisibility(View.INVISIBLE);
        rlglPod1Label.setVisibility(View.INVISIBLE);
        rlglPod2Label.setVisibility(View.INVISIBLE);
        rlglPod3Label.setVisibility(View.INVISIBLE);

        //initialize to red light on
        rlglRedRadio.setChecked(false);
        rlglYellowRadio.setChecked(false);
        rlglGreenRadio.setChecked(false);

        rlglQuitButton.setEnabled(false);
        rlglRedRadio.setEnabled(false);
        rlglYellowRadio.setEnabled(false);
        rlglGreenRadio.setEnabled(false);

        Toast.makeText(getApplicationContext(),"Connecting to hub...", duration).show();

        bluetooth.setBluetoothServicesListener(new BluetoothServices.BluetoothServicesListener() {
            @Override
            public void onCommsReady() {
                if (bluetooth.commsReady) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("BLE","Communications ready");
                            Toast.makeText(getApplicationContext(),"Connected!", duration).show();
                            rlglQuitButton.setEnabled(true);
                            rlglRedRadio.setEnabled(true);
                            rlglYellowRadio.setEnabled(true);
                            rlglGreenRadio.setEnabled(true);

                            rlglPod1Display.setVisibility(View.VISIBLE);
                            rlglPod2Display.setVisibility(View.VISIBLE);
                            rlglPod3Display.setVisibility(View.VISIBLE);
                            rlglPod1Label.setVisibility(View.VISIBLE);
                            rlglPod2Label.setVisibility(View.VISIBLE);
                            rlglPod3Label.setVisibility(View.VISIBLE);

    //                        Toast.makeText(getApplicationContext(),"Starting Automated Tag!", duration).show();

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    messageStatus = bluetooth.sendMessage("RL_GL");
                                    processMessageStatus(messageStatus,"RL_GL");
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

                        setText(pod1Status,rlglPod1Display);
                        setText(pod2Status,rlglPod2Display);
                        setText(pod3Status,rlglPod3Display);
                    }
                });
            }
        });

        rlglQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageStatus = bluetooth.sendMessage("EXIT_GAME");
                processMessageStatus(messageStatus,"EXIT_GAME");
            }
        });

        rlglGreenRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglRedRadio.setChecked(false);
                rlglYellowRadio.setChecked(false);
                messageStatus = bluetooth.sendMessage("RUN");
                processMessageStatus(messageStatus,"RUN");
            }
        });

        rlglYellowRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglRedRadio.setChecked(false);
                rlglGreenRadio.setChecked(false);
                messageStatus = bluetooth.sendMessage("WALK");
                processMessageStatus(messageStatus,"WALK");
            }
        });

        rlglRedRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglGreenRadio.setChecked(false);
                rlglYellowRadio.setChecked(false);
                messageStatus = bluetooth.sendMessage("STOP");
                processMessageStatus(messageStatus,"STOP");
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
