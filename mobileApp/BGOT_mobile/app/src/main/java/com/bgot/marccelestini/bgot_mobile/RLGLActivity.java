package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

public class RLGLActivity extends AppCompatActivity {
    RadioButton rlglRedRadio;
    RadioButton rlglYellowRadio;
    RadioButton rlglGreenRadio;
    Button rlglQuitButton;
    Button rlglPlayButton;
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
        rlglPlayButton = findViewById(R.id.rlglPlayButton);
        rlglQuitButton = findViewById(R.id.rlglQuitButton);

        //initialize to red light on
        rlglRedRadio.setChecked(true);
        rlglYellowRadio.setChecked(false);
        rlglGreenRadio.setChecked(false);

        rlglPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageStatus = bluetooth.sendMessage("RL_GL");
                processMessageStatus(messageStatus,"RL_GL");
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
}
