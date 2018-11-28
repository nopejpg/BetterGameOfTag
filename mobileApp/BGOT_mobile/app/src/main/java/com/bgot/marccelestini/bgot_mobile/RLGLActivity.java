package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

public class RLGLActivity extends AppCompatActivity {
    RadioButton rlglRedRadio;
    RadioButton rlglYellowRadio;
    RadioButton rlglGreenRadio;
    Button rlglQuitButton;
    Button rlglPlayButton;
    BluetoothServices bluetooth;

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
                bluetooth.sendMessage("RL_GL");
            }
        });

        rlglQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.sendMessage("EXIT_GAME");
                Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                bluetooth.disconnectGattServer();
                startActivity(intent);
            }
        });

        rlglGreenRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglRedRadio.setChecked(false);
                rlglYellowRadio.setChecked(false);
                bluetooth.sendMessage("RUN");
            }
        });

        rlglYellowRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglRedRadio.setChecked(false);
                rlglGreenRadio.setChecked(false);
                bluetooth.sendMessage("WALK");
            }
        });

        rlglRedRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglGreenRadio.setChecked(false);
                rlglYellowRadio.setChecked(false);
                bluetooth.sendMessage("STOP");
            }
        });


    }
}
