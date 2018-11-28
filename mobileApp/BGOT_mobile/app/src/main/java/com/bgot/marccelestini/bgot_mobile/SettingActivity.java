package com.bgot.marccelestini.bgot_mobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SettingActivity extends AppCompatActivity {
    TextView settingsPod1Display;
    TextView settingsPod2Display;
    TextView settingsPod3Display;
    Button settingsReturn;
    BluetoothServices bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        bluetooth=new BluetoothServices(this);

        settingsPod1Display = findViewById(R.id.settingsPod1Display);
        settingsPod2Display = findViewById(R.id.settingsPod2Display);
        settingsPod3Display = findViewById(R.id.settingsPod3Display);
        settingsReturn = findViewById(R.id.settingsReturn);

        settingsReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                bluetooth.disconnectGattServer();
                startActivity(intent);
            }
        });

        String status = ((MyApplicationBGOT) this.getApplication()).getPodStatus();

        if (status.equals("~~~")) {
            return;
        }

        boolean pod1Status = status.substring(0,1).equals("1");
        boolean pod2Status = status.substring(1,2).equals("1");
        boolean pod3Status = status.substring(2,3).equals("1");

        setText(pod1Status,settingsPod1Display);
        setText(pod2Status,settingsPod2Display);
        setText(pod3Status,settingsPod3Display);
    }

    void setText(boolean status, TextView podDisplay) {
        if (status) {
            podDisplay.setText("Connected");
        } else {
            podDisplay.setText("Not Connected");
        }
    }
}
