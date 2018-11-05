package com.bgot.marccelestini.bgot_mobile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

public class AutomatedTagActivity extends AppCompatActivity {
    RadioButton autoTagRadioEasy;
    RadioButton autoTagRadioMedium;
    RadioButton autoTagRadioHard;
    Button autoTagPlayButton;
    Button autoTagQuitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automated_tag);

        autoTagRadioEasy = findViewById(R.id.autoTagRadioEasy);
        autoTagRadioMedium = findViewById(R.id.autoTagRadioMedium);
        autoTagRadioHard = findViewById(R.id.autoTagRadioHard);
        autoTagPlayButton = findViewById(R.id.autoTagPlayButton);
        autoTagQuitButton = findViewById(R.id.autoTagQuitButton);

        autoTagRadioEasy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoTagRadioMedium.setChecked(false);
                autoTagRadioHard.setChecked(false);
            }
        });

        autoTagRadioMedium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoTagRadioEasy.setChecked(false);
                autoTagRadioHard.setChecked(false);
            }
        });

        autoTagRadioHard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoTagRadioEasy.setChecked(false);
                autoTagRadioMedium.setChecked(false);
            }
        });

    }
}
