package com.bgot.marccelestini.bgot_mobile;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.content.Intent;


public class MainMenuActivity extends AppCompatActivity {
    Button autoTagButton;
    Button manualTagButton;
    Button redLightGreenLightButton;
    ImageButton settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        autoTagButton = (Button) findViewById(R.id.autoTagButton);
        manualTagButton = (Button) findViewById(R.id.manualTagButton);
        redLightGreenLightButton = (Button) findViewById(R.id.redLightGreenLightButton);
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);

        autoTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runAutoTag();
            }
        });

        manualTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runManualTag();
            }
        });

        redLightGreenLightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runRLGL();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSettings();
            }
        });
    }

    public void runAutoTag() {
        Intent intent = new Intent(this, AutomatedTagActivity.class);
        startActivity(intent);
    }
    public void runManualTag() {
        Intent intent = new Intent(this, ManualTagActivity.class);
        startActivity(intent);
    }
    public void runRLGL() {
        Intent intent = new Intent(this, RLGLActivity.class);
        startActivity(intent);
    }
    public void goToSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

}
