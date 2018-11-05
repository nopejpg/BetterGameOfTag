package com.bgot.marccelestini.bgot_mobile;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rlgl);

        rlglGreenRadio = findViewById(R.id.rlglGreenRadio);
        rlglRedRadio = findViewById(R.id.rlglRedRadio);
        rlglYellowRadio = findViewById(R.id.rlglYellowRadio);
        rlglPlayButton = findViewById(R.id.rlglPlayButton);
        rlglQuitButton = findViewById(R.id.rlglQuitButton);

        //initialize to red light on
        rlglRedRadio.setChecked(true);
        rlglYellowRadio.setChecked(false);
        rlglGreenRadio.setChecked(false);

        rlglGreenRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglRedRadio.setChecked(false);
                rlglYellowRadio.setChecked(false);
            }
        });

        rlglYellowRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglRedRadio.setChecked(false);
                rlglGreenRadio.setChecked(false);
            }
        });

        rlglRedRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rlglGreenRadio.setChecked(false);
                rlglYellowRadio.setChecked(false);
            }
        });
    }
}
