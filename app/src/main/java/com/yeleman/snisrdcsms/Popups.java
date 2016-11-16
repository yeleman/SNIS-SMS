package com.yeleman.snisrdcsms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

class Popups {

    public static void showdisplayPermissionErrorPopup(final Context context, String title, String message, Boolean goToSettings) {
        //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
        helpBuilder.setTitle(title);
        helpBuilder.setMessage(message);
        helpBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        if (goToSettings) {
            helpBuilder.setPositiveButton(context.getString(R.string.open_settings_button),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                            intent.setData(uri);
                            context.startActivity(intent);
                        }
                    });
        } else {
            helpBuilder.setPositiveButton(context.getString(R.string.standard_dialog_ok),
                    new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {}});
        }

        // Remember, create doesn't show the dialog
        final AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
        updatePopupForStatus(helpDialog, Constants.SMS_ERROR);
    }

    public static AlertDialog.Builder getDialogBuilder(Context context,
                                                       String title,
                                                       String message,
                                                       boolean cancelable) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setCancelable(cancelable);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        dialogBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        return dialogBuilder;
    }

    public static AlertDialog getStandardDialog(final Activity activity,
                                                String title,
                                                String message,
                                                boolean cancelable,
                                                final boolean shouldFinishActivity) {
        AlertDialog.Builder dialogBuilder = getDialogBuilder(activity, title, message, cancelable);
        dialogBuilder.setPositiveButton(
                activity.getString(R.string.standard_dialog_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // close the dialog (auto)
                        if (shouldFinishActivity) {
                            activity.finish();
                        }
                    }
                });
        return dialogBuilder.create();
    }

    public static void updatePopupForStatus(AlertDialog dialog, int status) {
        int textColor = Constants.getColorForStatus(status);
        if (textColor != -1) {
            // title
            int textViewId = dialog.getContext().getResources().getIdentifier("android:id/alertTitle", null, null);
            TextView tv = (TextView) dialog.findViewById(textViewId);
            tv.setTextColor(textColor);
            // divider
            int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
            View divider = dialog.findViewById(dividerId);
            if (divider != null) {
                divider.setBackgroundColor(textColor);
            }
        }
    }

    public static DialogInterface.OnClickListener getBlankClickListener() {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        };
    }

    public static ProgressDialog getStandardProgressDialog(Context context,
                                                           String title,
                                                           String message,
                                                           boolean cancelable) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setIcon(R.mipmap.ic_launcher);
        progressDialog.setCancelable(cancelable);
        return progressDialog;
    }
}
