package com.yeleman.snisrdcsms;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.orm.SugarContext;

public class AboutActivity extends CheckedFormActivity {

    private static final String TAG = Constants.getLogTag("About") ;
    private Button versionButton;
	private Button resetDBButton;
	private Button exportDBButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(getString(R.string.menu_about));
        setupOrRefreshUI();
    }

    protected void setupUI() {
    	versionButton = (Button) findViewById(R.id.btn_version);
        resetDBButton = (Button) findViewById(R.id.btn_resetDB);
        exportDBButton = (Button) findViewById(R.id.btn_exportDB);

        TextView helpTextView = (TextView) findViewById(R.id.tv_help);
        Spanned spannedHelpText;
        if (Build.VERSION.SDK_INT >= 24) {
            spannedHelpText = Html.fromHtml(getString(R.string.help_text), Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            spannedHelpText = Html.fromHtml(getString(R.string.help_text));
        }

        helpTextView.setText(spannedHelpText);

    	versionButton.setText(String.format(
    		getString(R.string.version_button_label),
    		BuildConfig.VERSION_NAME));
    	versionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String market_uri = getString(R.string.app_market_url);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(market_uri));
                startActivity(intent);
            }
        });

        final Activity activity = this;
        resetDBButton.setOnClickListener(new View.OnClickListener() {
            int counter = 3;
            final String databaseName = Constants.getDatabaseName(getApplicationContext());
            @Override
            public void onClick(View v) {
                counter --;
                if (counter > 0) {
                    resetDBButton.setText(String.format(getString(R.string.reset_database_counter_text), counter));
                } else if(counter == 0) {
                    resetDBButton.setText(getString(R.string.reset_confirmation_text));
                } else if (counter == -1) {
                    resetDBButton.setEnabled(false);
                    resetDBButton.setText(R.string.in_progress);
                    resetDatabase(databaseName);

                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), String.format(getString(R.string.reseted_confirmation_text), databaseName), Toast.LENGTH_SHORT).show();
                        }
                    }, 500);
                    activity.finish();
                }
            }
        });

        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDBButton.setEnabled(false);
                exportDBButton.setText(R.string.in_progress);
                exportDatabaseWithPermission();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    Log.d(TAG, "permission was granted");

                    exportDatabase();
                } else {
                    // permission denied
                    Log.d(TAG, "permission denied");

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // show permission explanation dialog
                        Log.d(TAG, "should show permission explanation");

                        Popups.showdisplayPermissionErrorPopup(this,
                                getString(R.string.permission_required),
                                getString(R.string.enable_write_text), false);
                    } else {
                        // Never ask again selected, or device policy prohibits the app from having that permission.
                        Log.d(TAG, "Never ask again selected");

                        Popups.showdisplayPermissionErrorPopup(this,
                                getString(R.string.permission_required),
                                getString(R.string.enable_write_in_settings_text), true);
                    }

                }
                postExportRefreshUI();
            }
        }
    }

    private void exportDatabase() {
        Boolean success = Utils.exportDatabase(getApplicationContext());
        Toast.makeText(getBaseContext(),
                getString(success ? R.string.export_database_succeeded : R.string.export_database_failed),
                Toast.LENGTH_LONG).show();
    }

    private void postExportRefreshUI() {
        exportDBButton.setEnabled(true);
        exportDBButton.setText(R.string.export_database_text);
    }

    private void exportDatabaseWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSION_REQUEST_WRITE_STORAGE);
        } else {
            Log.d(TAG, "permission already present");
            exportDatabase();
            postExportRefreshUI();
        }
    }

    private void resetDatabase (String databaseName) {
        SugarContext.terminate();
        getApplicationContext().deleteDatabase(databaseName);
        SugarContext.init(this);
    }
}
