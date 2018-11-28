package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;


public class AutomatedTagActivity extends AppCompatActivity {
    Button autoTagPlayButton;
    Button autoTagQuitButton;
    BluetoothServices bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automated_tag);
        autoTagPlayButton = findViewById(R.id.autoTagPlayButton);
        autoTagQuitButton = findViewById(R.id.autoTagQuitButton);

        bluetooth = new BluetoothServices(this);

        autoTagPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.sendMessage("AUTOMATE_TAG");
            }
        });

        autoTagQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.sendMessage("EXIT_GAME");
                Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                bluetooth.disconnectGattServer();
                startActivity(intent);
            }
        });
    }
}
