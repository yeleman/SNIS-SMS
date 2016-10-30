package com.yeleman.snisrdcsms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CheckedFormActivity extends Activity implements SMSUpdater {

    private final static String TAG = Constants.getLogTag("CheckedFormActivity");

    protected Boolean ui_ready = false;
    protected SharedPreferences sharedPrefs;

    /* progress dialog */
    protected ProgressDialog progressDialog;

    /* Username & Password for transmission */
    protected String username = "-";
    protected String password = "-";

    /* SMS Receiver */
    protected SMSReceiver mSmsReceiver = null;
    protected SMSSentReceiver mSmsSentReceiver = null;
    protected SMSDeliveredReceiver mSmsDeliveredReceiver = null;

    /* Keep an internal state of input validation for each fields */
	protected LinkedHashMap<Integer, Boolean> checkedFields = new LinkedHashMap<>();

	protected void updateFieldCheckedStatus(EditText editText) {
		updateFieldCheckedStatus(editText, false);
	}

	protected void updateFieldCheckedStatus(EditText editText, Boolean status) {
		checkedFields.put(editText.getId(), status);
	}

    /* Username & Password accessors */
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	/* Abstract methods */
	protected void setupInvalidInputChecks() {}
	protected boolean ensureDataCoherence() { return false; }
	protected String buildSMSText() { return ""; }
	protected void storeReportData() {}
	protected void restoreReportData() {}
    protected void resetReportData() {
        Log.i(TAG, "resetReportData orig");
    }

	/* Visual feedback for invalid and incorect data */
    protected void addErrorToField(Spinner spinner, String message) {
        TextView errorText = (TextView)spinner.getSelectedView();
        errorText.setError("");
        errorText.setTextColor(Color.RED);//just to highlight that this is an error
        errorText.setText(message);//changes the selected item text to this
    }
	protected void addErrorToField(EditText editText, String message) {
		editText.setError(message);
		// editText.requestFocus();
	}

    protected boolean doCheckAndProceed(boolean test, String error_msg, Spinner spinner) {
        if (test) {
            addErrorToField(spinner, error_msg);
            return false;
        }
        return true;
    }

	protected boolean doCheckAndProceed(boolean test, String error_msg, EditText editText) {
		if (test) {
            addErrorToField(editText, error_msg);
			return false;
		} else {
			addErrorToField(editText, null);
		}
		return true;
	}

	public void fireErrorDialog(Activity activity, String errorMsg, final EditText fieldToReturnTo) {
        AlertDialog.Builder errorDialogBuilder = Popups.getDialogBuilder(
                activity, getString(R.string.error_dialog_title),
                errorMsg, false);
        errorDialogBuilder.setPositiveButton(R.string.error_dialog_button_text,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // close the dialog and focus on the requested field
                    if (fieldToReturnTo != null) {
                        fieldToReturnTo.requestFocus();
                    }
                }
            });
        AlertDialog errorDialog = errorDialogBuilder.create();
        errorDialog.show();
        // update color of popup to show it's an error.
        Popups.updatePopupForStatus(errorDialog, Constants.SMS_ERROR);
    }

	/* General checking methods */
	protected boolean ensureValidInputs(boolean focusOnFailing) {
    	for (Map.Entry<Integer, Boolean> entry : checkedFields.entrySet()) {
 			if (!entry.getValue()) {
 				if (focusOnFailing) {
 					EditText field = (EditText) findViewById(entry.getKey());
                    field.setText(field.getText());
 					field.requestFocus();
 				}
 				return false;
 			}
		}
        return true;
    }

    protected void removeFocusFromFields() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    protected boolean checkInputsAndCoherence() {
        // remove focus so to remove
        removeFocusFromFields();

    	if (!ensureValidInputs(true)) {
    		Log.d(TAG, "Invalid inputs");
    		return false;
    	}
    	if (!ensureDataCoherence()) {
    		Log.d(TAG, "Not coherent inputs");
    		return false;
    	}
    	Log.i(TAG, "data looks good");
    	return true;
    }

	/* Input Validation Checks (standalone functions) */
	protected boolean assertNotEmpty(EditText editText) {
		boolean test = (editText.getText().toString().trim().length() == 0);
		String error_msg = getString(R.string.error_field_empty);
		return doCheckAndProceed(test, error_msg, editText);
	}

	protected boolean assertAtLeastThisLong(EditText editText, int min_chars) {
		boolean test = (editText.getText().toString().trim().length() < min_chars);
		String error_msg = String.format(getString(R.string.error_field_min_chars),
                String.valueOf(min_chars));
		return doCheckAndProceed(test, error_msg, editText);
	}

    protected boolean assertPasswordAlike(EditText editText) {
        String text = stringFromField(editText);
        boolean test = (text.contains(" ") || text.length() < Constants.MIN_CHARS_PASSWORD);
        String error_msg = String.format(getString(R.string.error_field_nopasswd),
                String.valueOf(Constants.MIN_CHARS_PASSWORD));
        return doCheckAndProceed(test, error_msg, editText);
    }

    protected boolean assertOrganisationUnitSelected(Spinner spinner, HashMap<String, String> spinnerMap) {
        String selectedLabel = spinner.getSelectedItem().toString();
        String dhisId = spinnerMap.get(selectedLabel);
        boolean test = true;
        try {
            test = !OrganisationUnit.exists(dhisId);
        } catch(Exception ex) {}
        String error_msg = getString(R.string.error_no_orgunit_selected);
        return doCheckAndProceed(test, error_msg, spinner);
    }

    protected boolean assertPINCodeAlike(EditText editText) {
        String text = stringFromField(editText);
        boolean test = (text.contains(" ") || text.length() != Constants.PIN_CODE_NUM_CHARS);
        String error_msg = String.format(
                getString(R.string.error_field_no_pin),
                Constants.PIN_CODE_NUM_CHARS);
        return doCheckAndProceed(test, error_msg, editText);
    }

    protected boolean assertPINCodeOK(EditText editText, String pinCode) {
        String text = editText.getText().toString();
        boolean test = !text.equals(pinCode);

        String error_msg = getString(R.string.error_pin_code);
        return doCheckAndProceed(test, error_msg, editText);
    }

    protected boolean assertUsernameAlike(EditText editText) {
        String text = stringFromField(editText);
        boolean test = (text.contains(" ") || text.length() < Constants.MIN_CHARS_USERNAME);
        String error_msg = String.format(getString(R.string.error_field_nopasswd),
                String.valueOf(Constants.MIN_CHARS_USERNAME));
        return doCheckAndProceed(test, error_msg, editText);
    }

    protected boolean assertPositiveIntegerOrNull(EditText editText) {
        boolean test = stringFromField(editText).length() != 0 && (integerFromField(editText, -1) < 0);
        String error_msg = getString(R.string.error_field_positive_integer);
        return doCheckAndProceed(test, error_msg, editText);
    }

	protected boolean assertPositiveInteger(EditText editText) {
		boolean test = (integerFromField(editText, -1) < 0);
		String error_msg = getString(R.string.error_field_positive_integer);
		return doCheckAndProceed(test, error_msg, editText);
	}

	protected boolean assertPositiveFloat(EditText editText) {
		boolean test = (floatFromField(editText, -1) < 0);
		String error_msg = getString(R.string.error_field_positive_integer);
		return doCheckAndProceed(test, error_msg, editText);
	}

	/* Input Validation Checks (EventListener creators) */
	protected void setAssertNotEmpty(final EditText editText) {
    	updateFieldCheckedStatus(editText, false);
    	editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            	updateFieldCheckedStatus(editText, assertNotEmpty(editText));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }


    protected void setAssertPositiveInteger(final EditText editText) {
    	updateFieldCheckedStatus(editText, false);
    	editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            	updateFieldCheckedStatus(editText, assertPositiveInteger(editText));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    protected void setAssertPositiveIntegerOrNull(final EditText editText) {
        updateFieldCheckedStatus(editText, true);
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateFieldCheckedStatus(editText, assertPositiveIntegerOrNull(editText));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    protected void setAssertPositiveFloat(final EditText editText) {

    	updateFieldCheckedStatus(editText, false);
    	editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            	updateFieldCheckedStatus(editText, assertPositiveFloat(editText));
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    protected void setAssertPINAlike(final EditText editText) {
        updateFieldCheckedStatus(editText, false);
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateFieldCheckedStatus(editText, assertPINCodeAlike(editText));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    protected void setAssertPINCodeOK(final EditText editText, final String pinCode) {
        updateFieldCheckedStatus(editText, false);
        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateFieldCheckedStatus(editText, assertPINCodeOK(editText, pinCode));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    /* Data Coherence helpers */
    protected boolean mustBeInferior(EditText fieldToReturnTo, EditText fieldA, EditText fieldB) {
    	int valueA = integerFromField(fieldA);
    	int valueB = integerFromField(fieldB);
    	String errorMsg = String.format(getString(R.string.error_must_be_inferior),
    									fieldA.getHint(), valueA,
    									fieldB.getHint(), valueB);
    	if (valueA >= valueB) {
    		fireErrorDialog(this, errorMsg, fieldToReturnTo);
            return false;
    	}
        return true;
    }
    protected static boolean checkDateIsFriday(DatePicker widget) {
        GregorianCalendar adate = new GregorianCalendar(widget.getYear(), widget.getMonth(), widget.getDayOfMonth());
        return adate.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
    }

    protected boolean mustBeInferiorOrEqual(EditText fieldToReturnTo, EditText fieldA, EditText fieldB) {
        int valueA = integerFromField(fieldA);
        int valueB = integerFromField(fieldB);
        String errorMsg = String.format(getString(R.string.error_must_be_inferior_or_equal),
                fieldA.getHint(), valueA,
                fieldB.getHint(), valueB);
        if (valueA > valueB) {
            fireErrorDialog(this, errorMsg, fieldToReturnTo);
            return false;
        }
        return true;
    }

    protected boolean allIsNotZero(EditText fielA, EditText fieldB, EditText fieldC) {
        if (integerFromField(fielA) == 0 &&
                integerFromField(fieldB) == 0 &&
                integerFromField(fieldC) == 0){
            fireErrorDialog(this, "Tout est à zero", null);
            return false;
        }
        return true;
    }

    protected boolean mustBeEqual(EditText fieldToReturnTo, EditText fieldA, EditText fieldB) {
        int valueA = integerFromField(fieldA);
        int valueB = integerFromField(fieldB);
        String errorMsg = String.format(getString(R.string.error_must_be_equal),
                fieldA.getHint(), valueA,
                fieldB.getHint(), valueB);
        if (valueA != valueB) {
            fireErrorDialog(this, errorMsg, fieldToReturnTo);
            return false;
        }
        return true;
    }

    /* Bundled data-ok callbacks */
    protected void checkAndFinish() {
		if (!checkInputsAndCoherence()) { return; }
		finish();
	}

	protected void checkAndLogSMS() {
		if (!checkInputsAndCoherence()) { return; }

		String sms_text = buildSMSText();
        Log.d(TAG, sms_text);

		finish();
	}

    protected void checkAndSubmitSMSAction() {
    	if (!checkInputsAndCoherence()) { return; }

        String sms_text = buildSMSText();
        Log.d(TAG, sms_text);

		boolean succeeded = transmitSMSForReply(sms_text);
		if (!succeeded) {
			Log.e(TAG, "Unable to send SMS (exception on send command).");
		}
    }

    /* Input CleanUp/convertions */
    protected String stringFromField(EditText editText) {
        return editText.getText().toString().trim();
    }

    protected String stringOrNullFromField(EditText editText) {
        String content = editText.getText().toString().trim();
        return (content.length() == 0) ? null : content;
    }

    protected int integerFromField(EditText editText) {
        return integerFromField(editText, -1);
    }
    protected int integerFromField(EditText editText, int fallback) {
        String text = stringFromField(editText);
        if (text.length() > 0) {
            try {
                return Integer.parseInt(text);
            } catch (Exception e){
                Log.d(TAG, e.toString());
            }
        }
        return fallback;
    }

    protected float floatFromField(EditText editText) {
        return floatFromField(editText, -1);
    }
    protected float floatFromField(EditText editText, int fallback) {
        String text = stringFromField(editText);
        if (text.length() > 0) {
            return Float.parseFloat(text);
        }
        return fallback;
    }
    protected void setTextOnField(EditText editText, Object value) {
        String default_str = "";
        String value_str;
        try {
            value_str = String.valueOf(value);
        } catch (Exception e) {
            value_str = default_str;
        }
        editText.setText(value_str);
    }

    /* SMS Submission Helper */
    protected void requestPasswordAndTransmitSMS(final CheckedFormActivity activity,
                                                 String reportName) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String prefUsername = sharedPrefs.getString(Constants.KEY_USERNAME, "");
        final String prefPINCode = sharedPrefs.getString(Constants.KEY_PIN_CODE, "");
        AlertDialog.Builder identDialogBuilder = Popups.getDialogBuilder(
                activity, getString(R.string.password_dialog_title),
                String.format(getString(R.string.password_dialog_text), reportName),
                false);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.password_dialog, null);
        final EditText usernameField = (EditText) view.findViewById(R.id.usernameField);
        usernameField.setEnabled(false);
        usernameField.setText(prefUsername);

        final ArrayList<OrganisationUnit> organisationUnits = getUserOrganisationUnits();
        if (organisationUnits.isEmpty()) {
            Log.e(TAG, "improperly configured: missing organisationUnit");
            return;
        }

        // display a spinner with available OrganisationUnits if multiple present
        final Spinner orgUnitsField = (Spinner) view.findViewById(R.id.spn_orgUnits);
        // setting up a HashMap for matching labels to dhisId
        final HashMap<String, String> spinnerMap = new HashMap<>();

        // display a list of OrganisationUnit based on numbers in prefs
        if (organisationUnits.size() > 1) {
            // setting up a list of OrgUnit labels for the Spinner
            ArrayList<String> organisationLabels = new ArrayList<>();

            // default value for both (no choice selected)
            spinnerMap.put(getString(R.string.organisation_unit_select_none), Constants.MISSING_VALUE);
            organisationLabels.add(getString(R.string.organisation_unit_select_none));

            // feeding HashMap and labels
            for (OrganisationUnit organisationUnit : Utils.deserializeOrganisationUnits(sharedPrefs.getString(Constants.KEY_ORGANISATION_UNIT, null))) {
                spinnerMap.put(organisationUnit.getLabel(), organisationUnit.getDhisId());
                organisationLabels.add(organisationUnit.getLabel());
            }

            // Adapter uses labels
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, organisationLabels);
            orgUnitsField.setAdapter(adapter);
            orgUnitsField.setVisibility(View.VISIBLE);
        }

        // display a spinner with available periods
        final Spinner periodsField = (Spinner) view.findViewById(R.id.spn_Periods);
        // setting up a HashMap for matching labels to dhisId
        final ArrayList<ArrayList<String>> periodsArrayLists = Utils.getPeriodsArrayLists();
        ArrayList<String> periodLabels = periodsArrayLists.get(1);
        ArrayList<String> periodCodes = periodsArrayLists.get(0);
        final HashMap<String, String> periodsMap = Utils.getHashMapFor(periodLabels, periodCodes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, periodLabels);
        periodsField.setAdapter(adapter);

        identDialogBuilder.setView(view);
        identDialogBuilder.setPositiveButton(R.string.submit, Popups.getBlankClickListener());
        identDialogBuilder.setNegativeButton(R.string.cancel, Popups.getBlankClickListener());
        final AlertDialog identDialog = identDialogBuilder.create();
        identDialog.show();
        identDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                EditText pinField = (EditText) view.findViewById(R.id.pinField);
                pinField.setError(null);

                String selectedOrganisation;
                if (organisationUnits.size() > 1) {
                    if (!assertOrganisationUnitSelected(orgUnitsField, spinnerMap)) {
                        orgUnitsField.requestFocus();
                        return;
                    }
                    String selectedLabel = orgUnitsField.getSelectedItem().toString();
                    selectedOrganisation = spinnerMap.get(selectedLabel);
//                    OrganisationUnit organisationUnit = OrganisationUnit.findByDHISId(dhisId);
//                    Log.d(TAG, "SELECTED ORG: " + organisationUnit.getLabel());
                } else {
                    selectedOrganisation = organisationUnits.get(0).getDhisId();
                }

                if (!assertPINCodeAlike(pinField)) {
                    pinField.requestFocus();
                    return;
                }

                if (!assertPINCodeOK(pinField, prefPINCode)) {
                    pinField.requestFocus();
                    return;
                }

                // period from spinner
                String selectedPeriodLabel = periodsField.getSelectedItem().toString();
                String selectedPeriod = periodsMap.get(selectedPeriodLabel);

                String completeSMSText = Utils.buildCompleteSMSText(activity, selectedOrganisation, selectedPeriod);
                Log.d(TAG, completeSMSText);
                transmitSMSForReply(completeSMSText);
                identDialog.dismiss();
            }
        });
    }

    /* SMS Submission Code */
    protected boolean transmitSMS(String message) {
		// retrieve server number
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String serverNumber = sharedPrefs.getString(Constants.KEY_SERVER_PHONE_NUMBER, Constants.SERVER_PHONE_NUMBER);
        Log.d(TAG, "transmitSMS to: " + serverNumber + " text: " + message);
        try {
            // Find out how many parts required
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            final int numParts = parts.size();

            // Send regular SMS if only one part
            if (numParts == 1) {
                PendingIntent piSent = PendingIntent.getBroadcast(
                        this, 0, new Intent(Constants.SMS_SENT_INTENT), 0);
                PendingIntent piDelivered = PendingIntent.getBroadcast(
                        this, 0, new Intent(Constants.SMS_DELIVERED_INTENT), 0);
                sms.sendTextMessage(serverNumber, null, message, piSent, piDelivered);
            } else {
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

                for (int i = 0; i < numParts; i++) {
                    sentIntents.add(PendingIntent.getBroadcast(
                            this, 0, new Intent(Constants.SMS_SENT_INTENT), 0));
                    deliveredIntents.add(PendingIntent.getBroadcast(
                            this, 0, new Intent(Constants.SMS_DELIVERED_INTENT), 0));
                }
                sms.sendMultipartTextMessage(serverNumber, null, parts, sentIntents, deliveredIntents);
            }
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
    }

    public boolean transmitSMSForReply(String message) {
        // display loading message
        progressDialog = Popups.getStandardProgressDialog(
                this, getString(R.string.sending_sms_report_title),
                getString(R.string.sending_sms_report_body), false);
        progressDialog.show();
        setProgressTimeOut(Constants.NB_SECONDS_WAIT_FOR_REPLY * 1000);

        // send message
        return transmitSMS(message);
    }

    protected boolean closeProgressDialogIfShowing() {
    	boolean wasShowing = false;
    	if (progressDialog != null) {
            try {
                wasShowing = progressDialog.isShowing();
                if (wasShowing) {
                    progressDialog.dismiss();
                }
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
        return wasShowing;
    }

    protected void setProgressTimeOut(long time) {
        final Activity activity = this;
        new Handler().postDelayed(new Runnable() {
            public void run() {
            	// if dialog was one, that's an error and we need to
            	// display timeout message
				if (!closeProgressDialogIfShowing()) {
					return;
				}

                // display popup to warn user
                AlertDialog dialog = Popups.getStandardDialog(
                        activity, getString(R.string.sms_timeout_dialog_title),
                        getString(R.string.sms_timeout_dialog_body), false, false);
                dialog.show();
                Popups.updatePopupForStatus(dialog, Constants.SMS_WARNING);
            }
        }, time);
    }

    protected void disableSMSReceiving() {
        unRegisterSMSReceiver();
        mSmsReceiver = null;
        mSmsSentReceiver = null;
        mSmsDeliveredReceiver = null;
        progressDialog = null;
    }

    protected void registerSMSReceiver() {
        if (mSmsReceiver != null) {
            Log.d(TAG, this.getLocalClassName() + " registering SMSReceiver");
            IntentFilter filter = new IntentFilter();
            // must be set high so that we have priority over other SMS Apps.
            // doesn't matter as we don't discard SMS.
            filter.setPriority(1000);
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
            registerReceiver(mSmsReceiver, filter);
        }

        if (mSmsSentReceiver != null) {
            registerReceiver(mSmsSentReceiver, new IntentFilter(Constants.SMS_SENT_INTENT));
            Log.d(TAG, this.getLocalClassName() + " Registering BroadcastReceiver SMS_SENT");
        }

        if (mSmsDeliveredReceiver != null) {
            registerReceiver(mSmsDeliveredReceiver, new IntentFilter(Constants.SMS_DELIVERED_INTENT));
            Log.d(TAG, this.getLocalClassName() + " Registering BroadcastReceiver SMS_DELIVERED");
        }
    }

    protected void unRegisterSMSReceiver() {
        if (mSmsReceiver != null) {
            Log.d(TAG, this.getLocalClassName() + " unregistering SMSReceiver");
            unregisterReceiver(mSmsReceiver);
        }

        if (mSmsSentReceiver != null) {
            Log.d(TAG, this.getLocalClassName() + " unregistering SMSSentReceiver");
            unregisterReceiver(mSmsSentReceiver);
        }

        if (mSmsDeliveredReceiver != null) {
            Log.d(TAG, this.getLocalClassName() + " unregistering SMSDeliveredReceiver");
            unregisterReceiver(mSmsDeliveredReceiver);
        }
    }

    protected void setupSMSReceiver() {
        Log.d(TAG, "setupSMSReceiver - CheckedFormActivity");
        progressDialog = null; //new ProgressDialog(this);
        // SMS Replies from server
        mSmsReceiver = new SMSReceiver(this);
        // SMS sent feedback
        mSmsSentReceiver = new SMSSentReceiver(this);
        // SMS delivery reports
        mSmsDeliveredReceiver = new SMSDeliveredReceiver(this);
        //registerSMSReceiver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setupSMSReceiver();
        registerSMSReceiver();
    }

    @Override
    protected void onDestroy() {
        unRegisterSMSReceiver();
        super.onDestroy();
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
        // int textColor = Constants.getColorForStatus(status);
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

        // display SMS message
        // int textColor = Constants.getColorForStatus(finalResponseStatus);
        AlertDialog smsMessageDialog = Popups.getStandardDialog(
                this, getString(R.string.server_sms_received_title),
                smsContent,
                false,
                (finalResponseStatus == Constants.SMS_SUCCESS));
        smsMessageDialog.show();
        // change color
        Popups.updatePopupForStatus(smsMessageDialog, finalResponseStatus);
    }

    protected void setIntegerOnField(EditText edittext, Object obj)
    {
        if (Integer.parseInt(String.valueOf(obj)) != -1)
        {
            setTextOnField(edittext, obj);
        }
    }
    protected String stringFromInteger(int data) {
        return Constants.stringFromInteger(data);
    }

    protected String stringFromFloat(float data) {
        return Constants.stringFromFloat(data);
    }

    protected float floatFormat(float value){

        DecimalFormat df = new DecimalFormat("########.00");
        String str = df.format(value);
        return Float.parseFloat(str.replace(',', '.'));
    }

    ArrayList<OrganisationUnit> getUserOrganisationUnits() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return Utils.deserializeOrganisationUnits(sharedPrefs.getString(Constants.KEY_ORGANISATION_UNIT, ""));
    }

    protected void markUIReady() {
        this.ui_ready = true;
    }
    protected Boolean isUIReady() {
        return this.ui_ready;
    }

    protected void setupUI() {}
    protected void refreshUI() {}

    protected void setupUIIfNotReady() {
        if (!isUIReady()) {
            setupUI();
            markUIReady();
        }
    }

    protected void setupOrRefreshUI() {
        if (!isUIReady()) {
            setupUI();
            markUIReady();
        } else {
            refreshUI();
        }
    }
}
