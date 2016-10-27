package com.yeleman.snisrdcsms;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;


public class SettingsActivity extends CheckedFormActivity {

    private Button checkButton;
    private EditText usernameField;
    private EditText pinField;
    private EditText serverPhoneNumberField;
    private EditText passwordField;

    String prefServerNumber;
    String prefUsername;
    String prefPassword;
    String prefPinCode;
    private final static String TAG = Constants.getLogTag("Settings");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setupSMSReceiver();
        setContentView(R.layout.settings);
        setupOrRefreshUI();
    }

    protected void setupUI() {

        serverPhoneNumberField = (EditText) findViewById(R.id.et_server_number);
        usernameField = (EditText) findViewById(R.id.et_username);
        pinField = (EditText) findViewById(R.id.et_pin);
        passwordField = (EditText) findViewById(R.id.et_password);
        checkButton = (Button) findViewById(R.id.btn_check);

        loadPreferenceValues();

        // mark field requirements
        setAssertNotEmpty(serverPhoneNumberField);
        setAssertNotEmpty(usernameField);
        setAssertNotEmpty(passwordField);
        setAssertPINAlike(pinField);

        serverPhoneNumberField.setText(prefServerNumber);
        usernameField.setText(prefUsername);
        passwordField.setText(prefPassword);
        pinField.setText(prefPinCode);

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkInputsAndCoherence()) { return; }

                String serverNumber = stringFromField(serverPhoneNumberField);
                String username = stringFromField(usernameField);
                String password = stringFromField(passwordField);
                String pinCode = stringFromField(pinField);

                Boolean anyCredentialChanged = !(serverNumber.equals(prefServerNumber)
                                                 && username.equals(prefUsername)
                                                 && password.equals(prefPassword));
                Boolean pinChanged = !pinCode.equals(prefPinCode);
                Boolean alreadyVerified = sharedPrefs.getBoolean(Constants.KEY_CREDENTIALS_OK, false);


                // save changes anyway
                saveToPrefs();

                if (pinChanged && !anyCredentialChanged) {
                    // PIN change only
                    // no need for SMS
                    finish();
                } else if (anyCredentialChanged) {
                    // server, username or password change. we should SMS
                    updateCredentialStatus(false);
                    checkCredentialsViaSMSWithPermission();
                } else if (!alreadyVerified) {
                    // we might not have updated info
                    // but we are not verified yet so SMS
                    checkCredentialsViaSMSWithPermission();
                }
            }
        });
    }

    protected boolean ensureDataCoherence() {
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_SEND_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Log.d(TAG, "permission was granted");

                    checkCredentialsViaSMS();
                } else {
                    // permission denied
                    Log.d(TAG, "permission denied");

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                        // show permission explanation dialog
                        Log.d(TAG, "should show permission explanation");

                        Popups.showdisplayPermissionErrorPopup(this,
                                getString(R.string.permission_required),
                                getString(R.string.enable_sms_text), false);
                    } else {
                        // Never ask again selected, or device policy prohibits the app from having that permission.
                        Log.d(TAG, "Never ask again selected");

                        Popups.showdisplayPermissionErrorPopup(this,
                                getString(R.string.permission_required),
                                getString(R.string.enable_sms_in_settings_text), true);
                    }
                }
            }
        }
    }

    private void checkCredentialsViaSMSWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, Constants.PERMISSION_REQUEST_SEND_SMS);
        } else {
            Log.d(TAG, "permission already present");
            checkCredentialsViaSMS();
        }
    }

    private boolean checkCredentialsViaSMS() {
        Log.d(TAG, "checkCredentialsViaSMS");
        String message = Utils.buildDHISCheckSMS(this);
        if (message == null) {
            Log.e(TAG, "credentials not set");
            return false;
        }
        boolean succeeded = transmitSMSForReply(message);
        if (!succeeded) {
            Log.e(TAG, "Unable to send SMS (exception on send command).");
            return true;
        }
        return true;
    }

    public void gotSMSStatusUpdate(int status, String message) {
        // SMS is in good way, let's keep evertything in place
        if (status == Constants.SMS_SUCCESS) {
            return;
        }

        // remove progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        // display error message
        AlertDialog smsMessageDialog = Popups.getStandardDialog(
                this, getString(R.string.error_sms_not_sent_title),
                String.format(getString(R.string.error_sms_not_sent_body), message),
                false, false);
        smsMessageDialog.show();
        // change color
        Popups.updatePopupForStatus(smsMessageDialog, status);
    }

    public void gotSms(String from, Long timestamp, String body) {
        // remove progress dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        final int finalResponseStatus = Constants.getSMSStatus(body);
        String smsContent = Constants.getSMSContent(body);

        // server said credentials are OK and sent at least one OrganisationUnit
        if (finalResponseStatus == Constants.SMS_SUCCESS) {
            updateCredentialStatus(true);
        }

        // record sent OrganisationUnits to prefs
        String[] organisationUnitStrings = smsContent.trim().split("\\|");
        ArrayList<OrganisationUnit> validOrganisationUnits = new ArrayList<>();
        for (String organisationUnitString : organisationUnitStrings) {
            String[] orgUnitParts = organisationUnitString.trim().split("=");
            String organisationUnitDhisId = orgUnitParts[0];
            if (OrganisationUnit.exists(organisationUnitDhisId)) {
                validOrganisationUnits.add(OrganisationUnit.findByDHISId(organisationUnitDhisId));
            }
        }
        if (validOrganisationUnits.isEmpty()) {
            Log.e(TAG, "no valid organisation units!");
        } else {
            Utils.updateSharedPreferences(
                    this, Constants.KEY_ORGANISATION_UNIT,
                    Utils.serializeOrganisationUnits(validOrganisationUnits));
        }

        // display SMS message
        AlertDialog smsMessageDialog = Popups.getStandardDialog(
                this, getString(R.string.server_sms_received_title),
                smsContent,
                false,
                (finalResponseStatus == Constants.SMS_SUCCESS));
        smsMessageDialog.show();
        // change color
        Popups.updatePopupForStatus(smsMessageDialog, finalResponseStatus);
    }

    void loadPreferenceValues() {
        prefServerNumber = sharedPrefs.getString(Constants.KEY_SERVER_PHONE_NUMBER, Constants.SERVER_PHONE_NUMBER);
        prefUsername = sharedPrefs.getString(Constants.KEY_USERNAME, null);
        prefPassword = sharedPrefs.getString(Constants.KEY_PASSWORD, null);
        prefPinCode = sharedPrefs.getString(Constants.KEY_PIN_CODE, null);
    }

    private void saveToPrefs() {
        final SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
        prefsEditor.putString(Constants.KEY_SERVER_PHONE_NUMBER, stringFromField(serverPhoneNumberField));
        prefsEditor.putString(Constants.KEY_USERNAME, stringFromField(usernameField));
        prefsEditor.putString(Constants.KEY_PASSWORD, stringFromField(passwordField));
        prefsEditor.putString(Constants.KEY_PIN_CODE, stringFromField(pinField));
        prefsEditor.apply();
        loadPreferenceValues();
    }

    private void updateCredentialStatus(boolean verified) {
        final SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
        prefsEditor.putBoolean(Constants.KEY_CREDENTIALS_OK, verified);
        prefsEditor.apply();
    }

    public void addContact(View view) {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.rdcflag);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] bita = bos.toByteArray();

        ArrayList<ContentValues> data = new ArrayList<>();

        ContentValues row = new ContentValues();
        row.put(Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
        row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bita);
        data.add(row);

        ContentValues phoneRow = new ContentValues();
        phoneRow.put(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        phoneRow.put(CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.TYPE_MAIN);
        phoneRow.put(CommonDataKinds.Phone.LABEL, "SMS");
        phoneRow.put(CommonDataKinds.Phone.NUMBER, prefServerNumber);
        data.add(phoneRow);

        //Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME, "SNIS SMS SERVER");

        startActivity(intent);
    }

}
