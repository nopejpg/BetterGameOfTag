package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

public class ManualTagActivity extends AppCompatActivity {
    Button manualTagPlayButton;
    Button manualTagQuitButton;
    Button manualTagUpdateButton;
    Switch manualTagPod1Switch;
    Switch manualTagPod2Switch;
    Switch manualTagPod3Switch;
    BluetoothServices bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_tag);

        bluetooth = new BluetoothServices(this);

        manualTagPlayButton = (Button) findViewById(R.id.manualTagPlayButton);
        manualTagQuitButton = (Button) findViewById(R.id.manualTagQuitButton);
        manualTagUpdateButton = (Button) findViewById(R.id.manualTagUpdateButton);
        manualTagPod1Switch = (Switch) findViewById(R.id.manualTagPod1Switch);
        manualTagPod2Switch = (Switch) findViewById(R.id.manualTagPod2Switch);
        manualTagPod3Switch = (Switch) findViewById(R.id.manualTagPod3Switch);

        manualTagPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.sendMessage("MAN_TAG");
            }
        });

        manualTagQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetooth.sendMessage("EXIT_GAME");
                Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                bluetooth.disconnectGattServer();
                startActivity(intent);
            }
        });

        manualTagUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String config1 = manualTagPod1Switch.isChecked() ? "S" : "U";
                String config2 = manualTagPod2Switch.isChecked() ? "S" : "U";
                String config3 = manualTagPod3Switch.isChecked() ? "S" : "U";
                String config = "%" + config1 + config2 + config3;

                bluetooth.sendMessage(config);
            }
        });
    }
}
