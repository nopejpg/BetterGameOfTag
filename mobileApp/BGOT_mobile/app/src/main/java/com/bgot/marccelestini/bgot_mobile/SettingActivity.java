package com.bgot.marccelestini.bgot_mobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingActivity extends AppCompatActivity {
    BluetoothServices bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        bluetooth=new BluetoothServices(this);

        String status = ((MyApplicationBGOT) this.getApplication()).getPodStatus();
    }
}
