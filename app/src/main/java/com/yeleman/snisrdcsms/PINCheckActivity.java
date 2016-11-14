package com.yeleman.snisrdcsms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.provider.Settings;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PINCheckActivity extends CheckedFormActivity {

    private final static String TAG = Constants.getLogTag("PINCheck");
    private TextInputLayout pinField;
    private Button createButton;
    private Button backButton;
    private Button verifyButton;
    private Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableSMSReceiving();
        setTitle(getString(R.string.settings_pin));
        setupOrRefreshUI();
    }

    protected void setupUI() {
        if (sharedPrefs.getString(Constants.KEY_PIN_CODE, null) == null) {
            setupUIForCreate();
        } else {
            setupUIForCheck();
        }
    }

    protected void setupUIForCheck() {
        setContentView(R.layout.pincheck);

        final Activity activity = this;
        pinField = (TextInputLayout) findViewById(R.id.et_pin);
        setAssertPINCodeOK(pinField, sharedPrefs.getString(Constants.KEY_PIN_CODE, null));
        pinField.requestFocus();

        backButton = (Button) findViewById(R.id.btn_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.finish();
            }
        });

        resetButton = (Button) findViewById(R.id.btn_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
                prefsEditor.putString(Constants.KEY_SERVER_PHONE_NUMBER, null);
                prefsEditor.putString(Constants.KEY_USERNAME, null);
                prefsEditor.putString(Constants.KEY_PASSWORD, null);
                prefsEditor.putString(Constants.KEY_PIN_CODE, null);
                prefsEditor.apply();
                setupUI();
            }
        });

        verifyButton = (Button) findViewById(R.id.btn_verify);
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkInputsAndCoherence()) { return; }

                startActivity(new Intent(activity, SettingsActivity.class));
                activity.finish();
            }
        });
    }

    protected void setupUIForCreate() {
        setContentView(R.layout.pincheck_create);

        final Activity activity = this;
        pinField = (TextInputLayout) findViewById(R.id.et_pin);
        setAssertPINAlike(pinField);
        pinField.requestFocus();

        backButton = (Button) findViewById(R.id.btn_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.finish();
            }
        });

        createButton = (Button) findViewById(R.id.btn_create);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkInputsAndCoherence()) {
                    return;
                }

                final SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
                prefsEditor.putString(Constants.KEY_PIN_CODE, pinField.getEditText().getText().toString());
                prefsEditor.apply();

                startActivity(new Intent(activity, SettingsActivity.class));
                activity.finish();
            }
        });
    }

    protected boolean ensureDataCoherence() {
        return true;
    }
}