package com.yeleman.snisrdcsms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SMSSentReceiver extends BroadcastReceiver {

    private static final String TAG = Constants.getLogTag("SMSSentReceiver");
    private SMSUpdater mSmsUpdater;

    public SMSSentReceiver(SMSUpdater u)
    {
        super();
        mSmsUpdater = u;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive SMS_SENT");
        int feedback_status = Constants.SMS_UNKNOWN;
        String feedback_message = "";
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                feedback_status = Constants.SMS_SUCCESS;
                feedback_message = context.getString(R.string.status_SENT_OK);
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                feedback_status = Constants.SMS_ERROR;
                feedback_message = context.getString(R.string.status_ERROR_GENERIC_FAILURE);
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                feedback_status = Constants.SMS_ERROR;
                feedback_message = context.getString(R.string.status_ERROR_NO_SERVICE);
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                feedback_status = Constants.SMS_ERROR;
                feedback_message = context.getString(R.string.status_ERROR_NULL_PDU);
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                feedback_status = Constants.SMS_ERROR;
                feedback_message = context.getString(R.string.status_ERROR_RADIO_OFF);
                break;
            default:
                break;
        }
        if (feedback_status == Constants.SMS_SUCCESS) {
            Toast.makeText(context, feedback_message, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, feedback_message);

        mSmsUpdater.gotSMSStatusUpdate(feedback_status, feedback_message);
    }

}
