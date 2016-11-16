package com.yeleman.snisrdcsms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


public class ReportActivity extends CheckedFormActivity {

	private final static String TAG = Constants.getLogTag("ReportActivity");
    private final String reportName = Config.get(JSONFormParser.KEY_NAME);
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        setTitle(reportName);

        setupOrRefreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    protected void refreshUI() {
        // enable submit button if credentials are verified and we have at least one value
        submitButton.setEnabled(sharedPrefs.getBoolean(Constants.KEY_CREDENTIALS_OK, false) && DataValue.hasValues());

        // update completion counts
        for (final Section section: Section.getAllSection()) {
            TextView sectionLabelText = (TextView) findViewById(section.getId().intValue() + 100);
            sectionLabelText.setText(section.getLabel());
            TextView sectionProgressText = (TextView) findViewById(section.getId().intValue());
            sectionProgressText.setText(section.getProgression());

            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            TextView  progressionLabelText = (TextView) findViewById(R.id.tv_progression_text);
            progressionLabelText.setText(String.format(getString(R.string.progression_label_text),
                    DataValue.countNonNull(),
                    DataValue.countAll(),
                    Utils.percentFormat(DataValue.getCompletionPercentage(), true)));

            progressBar.setProgress(DataValue.countNonNull().intValue());
            progressBar.setMax(DataValue.countAll().intValue());
        }
        submitButton.setText(getString(R.string.submit_progress_button));
    }

    protected void setupUI() {
        final CheckedFormActivity activity = this;
        ViewGroup parentView = (ViewGroup) findViewById(R.id.ll_layout);
        for (final Section section: Section.getAllSection()) {
            LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View inflatedView = inflater.inflate(R.layout.section_button, parentView);

            LinearLayout sectionLinearLayout = (LinearLayout) inflatedView.findViewById(R.id.ll_section);
            TextView sectionProgressText = (TextView) inflatedView.findViewById(R.id.tv_progression);
            sectionProgressText.setText(section.getProgression());
            TextView sectionLabelText = (TextView) inflatedView.findViewById(R.id.tv_section_label);
            sectionLabelText.setText(section.getLabel());
            sectionProgressText.setId(section.getId().intValue());
            sectionLabelText.setId(section.getId().intValue() + 100);
            sectionLinearLayout.setId(section.getId().intValue() + 200);
            sectionLinearLayout.setTag(section.getId());
            sectionLinearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Long sectionId = (Long) view.getTag();
                    Intent intent = new Intent(getApplicationContext(), SectionActivity.class);
                    intent.putExtra(Constants.KEY_SECTION_ID, sectionId);
                    startActivity(intent);
                }
            });
        }

        submitButton = (Button) findViewById(R.id.btn_submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                if (DataValidation.displayCoherenceErrorsPopup(activity, null)) {
                    requestPasswordAndTransmitSMSWithPermission();
                } else {
                    view.setEnabled(true);
                }
            }
        });
    }

    private void requestPasswordAndTransmitSMSWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, Constants.PERMISSION_REQUEST_SEND_SMS);
        } else {
            Log.d(TAG, "permission already present");
            requestPasswordAndTransmitSMS(this, reportName);
            refreshUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_SEND_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Log.d(TAG, "permission was granted");

                    requestPasswordAndTransmitSMS(this, reportName);
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
                refreshUI();
            }
        }
    }

    public void startViewerActivity(View view) {
        startActivity(new Intent(this, DataViewerActivity.class));
    }
}
