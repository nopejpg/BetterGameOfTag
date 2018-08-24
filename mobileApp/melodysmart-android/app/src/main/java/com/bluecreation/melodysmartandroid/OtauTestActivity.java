package com.bluecreation.melodysmartandroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bluecreation.melodysmart.DeviceDatabase;
import com.bluecreation.melodysmart.ImageManager;
import com.bluecreation.melodysmart.ImageManagerListener;
import com.bluecreation.melodysmart.MelodySmartDevice;
import com.bluecreation.melodysmart.OtauListener;
import com.bluecreation.melodysmart.OtauRelease;
import com.bluecreation.melodysmart.OtauStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by genis on 17/10/2014.
 */
public class OtauTestActivity extends BleActivity implements ImageManagerListener, OtauListener {

    static public final String EXTRAS_IS_RECOVER_OTA = "com.bluecreation.testapp.isRecoverOta";
    static public final String EXTRAS_DEVICE_DATA = "com.bluecreation.testapp.deviceData";
    private static String TAG = OtauTestActivity.class.getSimpleName();
    private MelodySmartDevice device;
    private ImageManager imageManager;
    private OtauRelease otauRelease;
    /* UI */
    private Button startButton;
    private ProgressBar progressBar;
    private TextView progressLabel;
    private AlertDialog dialog;
    /* Array with all the available Melody Smart images */
    private ArrayList<String> listData = new ArrayList<>();
    private byte[] fwData;
    private boolean mIsRecoverOta;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ota_activity);
        /* Get the instance of the Library */
        device = MelodySmartDevice.getInstance();

        /* Get UI elements */
        startButton = (Button) findViewById(R.id.start_button);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressLabel = (TextView) findViewById(R.id.progress_label);

        /* Listen to changers on the OTAU protocol */
        device.registerListener((OtauListener) this);


        /* Start downloading the list of available Melody Smart images */
        imageManager = new ImageManager();
        imageManager.registerListener(this);

        mIsRecoverOta = getIntent().getBooleanExtra(EXTRAS_IS_RECOVER_OTA, false);

        if (!mIsRecoverOta) {
            device.readDeviceVersion();
        } else {
            startUI();
        }
    }

    @Override
    public void onDestroy() {
        imageManager.unregisterListener(this);
        device.unregisterListener((OtauListener) this);
        super.onDestroy();
    }

    @Override
    public void onListDownloaded(final List<OtauRelease> list) {

        for (OtauRelease release : list) {
            listData.add(release.toItemString());
        }
        /* Show list and let user select one */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(OtauTestActivity.this);
                builder.setTitle("Select image");
                builder.setItems(listData.toArray(new String[listData.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, String.format("Selected image from the list (%d/%d)", i, list.size()));
                        ((TextView) findViewById(R.id.online_version_text)).setText(list.get(i).getVersion());
                        otauRelease = list.get(i);
                        startButton.setEnabled(true);
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public void onOtauProgressChanged(final int percentage) {
        // if(success) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    /* First time progress changes we're showing the "Image downloading" dialog. Dissmis it */
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                progressBar.setProgress(percentage);
                progressLabel.setText(String.format("%d%%", percentage));
            }
        });
        //}
    }

    @Override
    public void onOtauFinished(final OtauStatus.Result result, final OtauStatus.Error error) {
        switch (result) {
            case COMPLETED:
                Log.d(TAG, "Preparing for reset");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Button button = (Button) findViewById(R.id.start_button);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                finishWithOk();
                            }
                        });

                        button.setText("Finish");
                        progressLabel.setText("Completed");
                        button.setEnabled(true);
                    }
                });
                break;

            case FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(OtauTestActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage(String.format("There has been an error during the over-the-air upgrade: %s",
                                error.getMessage()));
                        builder.setCancelable(false);
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialog.dismiss();
                                if (error.needsReconnection()) {
                                    finishWithOk();
                                } else {
                                    startUI();
                                }
                            }
                        });
                        dialog = builder.show();
                    }
                });


                break;

        }
    }

    @Override
    public void onDeviceVersionRead(int major, int minor, int patch) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startUI();

            }
        });
    }


    private void startUI() {

        /* Get the remote device Melody Smart version if it was already retrieved to the device */
        String version = device.getDeviceVersion();
        if (version != null) {
            ((TextView) findViewById(R.id.device_version_text)).setText(version);
        } else {
            ((TextView) findViewById(R.id.device_version_text)).setText("N/A");
        }

        /* Disable the OTA start button until device is ready for OTA */
        startButton.setEnabled(false);
        startButton.setText("Start OTAU");

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                /* Show message */
                AlertDialog.Builder builder = new AlertDialog.Builder(OtauTestActivity.this);
                builder.setTitle("Downloading image");
                builder.setMessage("Please wait while the image is downloaded");
                builder.setCancelable(false);
                dialog = builder.show();

                /* disable start button */
                startButton.setEnabled(false);
                device.startOtau(otauRelease);
            }
        });


        if (mIsRecoverOta) {
            DeviceDatabase.DeviceData deviceData = (DeviceDatabase.DeviceData) getIntent().getSerializableExtra(EXTRAS_DEVICE_DATA);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            String message = String.format("Recovering a Melody Smart device stuck in OTAU mode. The values below will be written on the image. If any of those " +
                    "wouldn't match the expected ones, you'll need to OTAU once more in order to read the correct values from the device and write them on the image: \n\n" +
                    "\t- BT address: %s\n\n\t- XtalTrim: %d", MelodySmartDevice.toString(deviceData.getBluetoothAddress()), deviceData.getXtaltrim());

            builder.setMessage(message);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    imageManager.downloadList();
                }
            });
            dialog = builder.show();

        } else {
            imageManager.downloadList();
        }
    }

    private void finishWithOk() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBondingStarted() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onBondingStarted();
    }
}
