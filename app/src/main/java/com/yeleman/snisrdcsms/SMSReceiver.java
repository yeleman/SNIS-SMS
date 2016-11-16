package com.yeleman.snisrdcsms;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver
{
    private static final String TAG = Constants.getLogTag("SMSReceiver");

    private SMSUpdater mSmsUpdater;

    public SMSReceiver(SMSUpdater u)
    {
        super();
        mSmsUpdater = u;
    }

    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "OnReceive called.");

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String serverNumber = sharedPrefs.getString(Constants.KEY_SERVER_PHONE_NUMBER, Constants.SERVER_PHONE_NUMBER);
        String cleanedServerNumber = Utils.cleanMsisdn(serverNumber);

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.e(TAG, "SMSReceiver with null Bundle");
            return;
        }

        // Retrieve the SMS Messages received
        SmsMessage[] parts;
        if (Build.VERSION.SDK_INT >= 19) {
            parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } else {
            Object[] messages = (Object[]) bundle.get("pdus");
            parts = new SmsMessage[messages != null ? messages.length : 0];
            for (int i = 0; i< (messages != null ? messages.length : 0); i++) {
                //noinspection deprecation
                parts[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
            }
        }

        // For every SMS message received
        String body = "";
        String from = null;
        Long timestamp = 0L;
        for (SmsMessage part: parts) {
            String partOriginatingAddress = Utils.cleanMsisdn(part.getOriginatingAddress());
            // skip messages not from our server
            if (!partOriginatingAddress.equals(cleanedServerNumber)) {
                // Log.d(TAG, "received from non-server number: " + cleanedServerNumber + " -- " + partOriginatingAddress);
                continue;
            }

            // message timestamp from first part
            if (timestamp == 0L) {
                timestamp = part.getTimestampMillis();
            }

            // message sender from first part. fail on different sender
            if (from == null) {
                from = part.getOriginatingAddress();
            } else if (!from.equals(part.getOriginatingAddress())) {
                Log.e(TAG, "Received multipart SMS with multiple senders alltogether");
            }
            // message body is concatenation of all parts
            body += part.getMessageBody();
        }
        if (body.length() > 0) {
            mSmsUpdater.gotSms(from, timestamp, body);
        }
    }
}


