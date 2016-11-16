package com.yeleman.snisrdcsms;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

class Constants {

    private final static String TAG = Constants.getLogTag("Constants");

    public static final int SMS_SUCCESS = 0;
    public static final int SMS_WARNING = 1;
    public static final int SMS_ERROR = 2;
    public static final int SMS_UNKNOWN = 3;

    public static final String SERVER_PHONE_NUMBER = "851779857";
    public static final int PIN_CODE_NUM_CHARS = 4;
    public static final int MIN_CHARS_PASSWORD = 4;
    public static final int MIN_CHARS_USERNAME = 3;

    public static final String SMS_SENT_INTENT = "com.yeleman.snisrdcsms.SMS_SENT";
    public static final String SMS_DELIVERED_INTENT = "com.yeleman.snisrdcsms.SMS_DELIVERED";

    public static final int NB_SECONDS_WAIT_FOR_REPLY = 180;
    public static final int MAX_CHARS_SINGLE_SMS = 160;

    public static final String KEY_CREDENTIALS_OK = "credentials_verified";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PIN_CODE = "pin";
    public static final String KEY_SERVER_PHONE_NUMBER = "server_number";
    public static final String KEY_ORGANISATION_UNIT = "organisationUnit";
    public static final String KEY_SECTION_ID = "sectionId";

    public static final String PHONE_PREFIX = "+243";
    public static final String MISSING_VALUE = "-";
    public static final String LABEL_SEPARATOR = "=";

    public static final String[] PLAIN_CHARACTERS = "abcdefghijklmnopqrstuvwxyz:1234567890_-".split("");
    public static final String[] TRANSLATED_CHARACTERS = "f12_8sy3bco47gnadxmqtij6:wz-09phe5krlvu".split("");
    public static final int PERMISSION_REQUEST_SEND_SMS = 1;
    public static final int PERMISSION_REQUEST_WRITE_STORAGE = 2;

    public static String getLogTag(String activity) {
    	return String.format("SNISSMS-%s", activity);
    }

    static String getDatabaseName(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("DATABASE");
        } catch (PackageManager.NameNotFoundException ex) {
            return null;
        }
    }

    public static String[] splitSMSText(String smsText) {
        String[] parts = {null, null};
        if (!(smsText.contains("[") && smsText.contains("]") && smsText.contains(LABEL_SEPARATOR))) {
            parts[1] = smsText;
            return parts;
        }

        int separatorIndex = smsText.indexOf(LABEL_SEPARATOR);
        int endingBracketIndex = smsText.indexOf("]");
        if (separatorIndex < 0 || endingBracketIndex < 0) {
            parts[1] = smsText;
            return parts;
        }

        if (endingBracketIndex < separatorIndex + 1 || smsText.length() < endingBracketIndex + 1) {
            parts[1] = smsText;
            return parts;
        }

        parts[0] = smsText.substring(separatorIndex + 1, endingBracketIndex).trim();
        parts[1] = smsText.substring(endingBracketIndex + 1).trim();

        return parts;
    }

    public static String getSMSContent(String smsText) {
        return splitSMSText(smsText)[1];
    }

    public static int getSMSStatus(String smsText) {
    	String smsCode = splitSMSText(smsText)[0];
        if (smsCode == null) {
            return SMS_UNKNOWN;
        }
        switch(smsCode) {
            case "OK":
                return SMS_SUCCESS;
            case "/!\\":
                return SMS_WARNING;
            case "ECHEC":
                return SMS_ERROR;
            default:
                return SMS_UNKNOWN;
        }
    }

    public static int getColorForStatus(int status) {
        switch (status) {
            case SMS_SUCCESS:
                return Color.rgb(71, 166, 42); // green
            case SMS_WARNING:
                return Color.rgb(208, 113, 42); // orange
            case SMS_ERROR:
                return Color.RED;
            case SMS_UNKNOWN:
            default:
                return -1;
        }
    }
   public static String getCompleteStatus(Boolean status) {
       if (status) {
           return "X";
       } else {
           return "  ";
       }
   }

    public static void updateButtonCompletion(Button button, boolean is_complete) {
        int color = getColorForStatus((is_complete) ? SMS_SUCCESS : SMS_UNKNOWN);
        if (color != -1) {
            button.setTextColor(color);
        }
    }

    public static String stringFromFloat(float data) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(2);
        return nf.format(data);
    }

    public static String stringFromInteger(int data) {
        return String.valueOf(data);
    }

    public static CompoundButton.OnCheckedChangeListener getResetTextViewCheckListener(final TextView textView) {
        Log.d(TAG, "recorded check listener");
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                textView.setError(null);
            }
        };
    }
}
