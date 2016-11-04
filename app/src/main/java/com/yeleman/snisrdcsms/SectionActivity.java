package com.yeleman.snisrdcsms;


import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends CheckedFormActivity {

    public static final String TAG = Constants.getLogTag("SectionActivity");
    Section section = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.section);
        section = Section.findById(Section.class, getIntent().getExtras().getLong(Constants.KEY_SECTION_ID));
        Log.d(TAG, section.getLabel());
        setTitle(section.getLabel());

        if (section != null) {
            setupOrRefreshUI();
        }
    }

    protected void setupUI() {
        ViewGroup parentView = (ViewGroup) findViewById(R.id.ll_layout);

        EditText lastField = null;
        final List<EditText> allFields = new ArrayList<>();

        ViewGroup.LayoutParams defaultLayoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // retrieve list of corresponding categories for Section
        List<Category> categories = CategorySection.getCategoriesFor(section.getId());
        if (categories.isEmpty()) {
            categories.add(null);
        }

        // display a list of Category labels on top
        if (section.hasMultipleCategories()) {
            // display header layout
            RelativeLayout headerView = (RelativeLayout) findViewById(R.id.rl_header);
            headerView.setVisibility(View.VISIBLE);

            ScrollView mainView = (ScrollView) findViewById(R.id.sv_main);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mainView.getLayoutParams();
            int margin = 60;
            layoutParams.topMargin = margin;
            layoutParams.setMargins(0, margin, 0, 0);
            mainView.setLayoutParams(layoutParams);

            Double weight = 1.0 / categories.size();
            LinearLayout.LayoutParams headerLineLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    weight.floatValue());

            LinearLayout headerLinearLayout = new LinearLayout(this);
            headerLinearLayout.setLayoutParams(defaultLayoutParams);
            headerLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerView.addView(headerLinearLayout);

            for (Category category: categories) {
                TextView tv_header = new TextView(this);
                tv_header.setLayoutParams(headerLineLayoutParams);
                tv_header.setGravity(Gravity.CENTER);
                tv_header.setTextSize(20);
                headerLinearLayout.addView(tv_header);
                tv_header.setText(category.getLabel());
            }
        }

        for (DataElement dataElement: DataElementSection.getDataElementsFor(section.getId())) {

            // build list of DataValues for DataElement (via Category)
            List<DataValue> dataValues = new ArrayList<>();
            for (DataValue dataValue: DataValue.findAllWith(dataElement.getId())) {
                dataValues.add(dataValue);
            }

            // add TextView any way
            TextView tv_label = new TextView(this);
            tv_label.setLayoutParams(defaultLayoutParams);
            parentView.addView(tv_label);
            tv_label.setId(dataElement.getId().intValue()+200); // set arbitrary id to diff from et_field
            tv_label.setText(dataElement.getLabel());

            // add single field if no-or-single cat
            if (section.hasNoCategory() || section.hasSingleCategory()) {

                DataValue dataValue = dataValues.get(0); // no category scenario
                if (section.hasSingleCategory()) {
                    for (DataValue dv : dataValues) {
                        if (dv.getActualCategory() != null && dv.getActualCategory().getId().equals(categories.get(0).getId())) {
                            dataValue = dv;
                            break;
                        }
                    }
                }

                //DataValue dataValue = dataValues.get(0);
                EditText et_field = new EditText(this);
                et_field.setLayoutParams(defaultLayoutParams);
                parentView.addView(et_field);

                et_field.setId(dataValue.getId().intValue());
                et_field.setText(dataValue.getValue());
                et_field.setTag(dataValue.getId());
                et_field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                et_field.setInputType(InputType.TYPE_CLASS_NUMBER);
                setAssertPositiveIntegerOrNull(et_field);

                // record reference to field in list
                allFields.add(et_field);

                // store field as the latest one for Ime tweak
                lastField = et_field;
            } else {
                Double weight = 1.0 / dataValues.size();
                LinearLayout.LayoutParams sharedLineLayoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        weight.floatValue());
                LinearLayout lineLinearLayout = new LinearLayout(this);
                lineLinearLayout.setLayoutParams(defaultLayoutParams);
                lineLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                parentView.addView(lineLinearLayout);

                // loop on DataValues and set different weights
                for (DataValue dataValue: dataValues) {
                    //Log.e(TAG, dataElement.getLabel() + ": "+ dataValue.getId() + "  :: " + dataValue.getCategory());
                    EditText et_field = new EditText(this);
                    et_field.setLayoutParams(sharedLineLayoutParams);
                    lineLinearLayout.addView(et_field);

                    et_field.setId(dataValue.getId().intValue());
                    et_field.setText(dataValue.getValue());
                    et_field.setTag(dataValue.getId());
                    et_field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    et_field.setInputType(InputType.TYPE_CLASS_NUMBER);
                    // et_field.setHint(dataValue.getActualCategory().getLabel());
                    setAssertPositiveIntegerOrNull(et_field);

                    // record reference to field in list
                    allFields.add(et_field);

                    // store field as the latest one for Ime tweak
                    lastField = et_field;
                }
            }
        }

        if (lastField != null) {
            lastField.setNextFocusDownId(R.id.btn_save);
            lastField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }

        Button saveButton = (Button) findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkInputsAndCoherence()) { return; }

                for (EditText editText: allFields) {
                    DataValue dataValue = DataValue.findById(DataValue.class, (Long) editText.getTag());
                    dataValue.setValue(stringOrNullFromField(editText));
                    dataValue.save();
                }
                Toast.makeText(getApplicationContext(), getString(R.string.saved_section_toast), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    protected boolean ensureDataCoherence() {
        return true;
    }

}
