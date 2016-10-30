package com.yeleman.snisrdcsms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;

public class HomeActivity extends CheckedFormActivity {

    private static final String TAG = Constants.getLogTag("Home");

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            try {
                JSONFormParser.initializeFormFromJSON(getApplicationContext(), "palu-v1.json");
            } catch (JSONException ex) {
                Log.e(TAG, ex.toString());
                ex.printStackTrace();
            } finally {
                name = Config.getOrNull(JSONFormParser.KEY_NAME);
                if (name != null) {
                    toggleReportButton(true);
                    setupOrRefreshUI();
                }
            }
        }
    };
    private class databasePreparationRunnable implements Runnable {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

    private boolean first_run = true;
    String name = null;
    LinearLayout resetLayout;

    Button startButton;
    Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableSMSReceiving();
        setContentView(R.layout.home);
        setTitle(R.string.app_name);
        setupOrRefreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        prepareDatabase();
        updateResetLayoutVisibility();
    }

    protected void toggleReportButton(boolean enable) {
        // toggle reporting button
        startButton.setText(enable ? name : getString(R.string.in_progress));
        startButton.setEnabled(enable);
    }

    protected void prepareDatabase() {
        toggleReportButton(false);
        int delay = 1;
        if (first_run) {
            delay = 1000;
            first_run = false;
        }
        handler.postDelayed(new databasePreparationRunnable(), delay);
    }

    protected void setupUI() {
        resetLayout = (LinearLayout) findViewById(R.id.ll_reset);
        startButton = (Button) findViewById(R.id.btn_start_reporting);
        resetButton = (Button) findViewById(R.id.btn_reset_values);

        // reset data button with confirmation
        resetButton.setOnClickListener(new View.OnClickListener() {
            int counter = 1;
            final String text = resetButton.getText().toString();
            @Override
            public void onClick(View v) {
                counter --;
                if(counter == 0) {
                    resetButton.setText(getString(R.string.reset_confirmation_text));
                } else if (counter == -1) {
                    // temporary state
                    resetButton.setEnabled(false);
                    resetButton.setText(R.string.in_progress);

                    // launch reset (might take several seconds)
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Utils.resetAllEntries();

                            // greet and refresh UI
                            updateResetLayoutVisibility();

                            Toast.makeText(getApplicationContext(), R.string.reset_values_completed, Toast.LENGTH_SHORT).show();

                            // restore button
                            counter = 1;
                            resetButton.setText(text);
                            resetButton.setEnabled(true);
                        }
                    }, 1);
                }
            }
        });
        updateResetLayoutVisibility();
    }

    protected void updateResetLayoutVisibility() {
        if (resetLayout == null) { return; }
        resetLayout.setVisibility(DataValue.hasValues() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch(item.getItemId()) {
            case R.id.about_menuentry:
                intent = new Intent(this, AboutActivity.class);
                break;
            case R.id.settings_menuentry:
                intent = new Intent(this, PINCheckActivity.class);
                break;
        }
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }

    public void startReporting(View view) {
        startActivity(new Intent(this, ReportActivity.class));
    }

}
