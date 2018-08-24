package com.bluecreation.melodysmartandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.inputmethod.InputMethodManager;

import com.bluecreation.melodysmart.BondingListener;
import com.bluecreation.melodysmart.MelodySmartDevice;

/**
 * Created by genis on 27/02/2015.
 */
public class BleActivity extends AppCompatActivity implements BondingListener {

    private AlertDialog bondingAlert = null;

    @Override
    public void onBondingStarted() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BleActivity.this);
        builder.setTitle("Please wait");
        builder.setMessage("Melody Smart is bonding to your phone");
        builder.setCancelable(false);
        bondingAlert = builder.show();
    }

    @Override
    public void onBondingFinished(boolean bonded) {
        if (bondingAlert != null && bondingAlert.isShowing()) {
            bondingAlert.dismiss();
        }
        /* If not bonded, show error and quit */
        if (!bonded) {
            bondingAlert = new AlertDialog.Builder(BleActivity.this)
                    .setTitle("Error")
                    .setMessage("Melody Smart couldn't bond to your phone. " +
                            "Please check it was not previously bonded either in " +
                            "the Android Bluetooth settings menu or on Melody smart")
                    .setCancelable(false)
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }).show();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MelodySmartDevice.getInstance().registerListener(this);
    }

    @Override
    public void onDestroy() {
        MelodySmartDevice.getInstance().unregisterListener(this);
        super.onDestroy();
    }

    protected void hideKeyboard(IBinder token) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
