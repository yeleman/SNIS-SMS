package com.yeleman.snisrdcsms;


import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends CheckedFormActivity {

    public static final int LAYOUT_INCR = 100;
    public static final int INPUT_INCR = 200;
    public static final int LABEL_INCR = 300;
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
        LayoutInflater inflater = this.getLayoutInflater();

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

        // loop on dataElements for section
        for (DataElement dataElement: DataElementSection.getDataElementsFor(section.getId())) {

            // if multi-cat, add a title for dataElement
            if (section.hasMultipleCategories()) {
                TextView elementTitle = (TextView) inflater.inflate(R.layout.multicat_element_title, null);
                elementTitle.setText(dataElement.getLabel());
                parentView.addView(elementTitle);
            }

            // loop on dataValues for said dataElement and possibly categories
            for (DataValue dataValue: DataValue.findAllWith(dataElement.getId())) {
                if (section.hasSingleCategory()) {
                    if (dataValue.getActualCategory() == null ||
                        !dataValue.getActualCategory().getId().equals(categories.get(0).getId())) {
                            continue;
                    }
                }

                // TextInputLayout
                TextInputLayout textInputLayout = new TextInputLayout(this);
                textInputLayout.setId(dataValue.getId().intValue()+SectionActivity.LAYOUT_INCR);
                textInputLayout.setLayoutParams(defaultLayoutParams);
                parentView.addView(textInputLayout);

                TextInputEditText et_field = new TextInputEditText(this);
                et_field.setId(dataValue.getId().intValue()+SectionActivity.INPUT_INCR);
                et_field.setText(dataValue.getValue());
                et_field.setTag(dataValue.getId());
                et_field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                et_field.setInputType(InputType.TYPE_CLASS_NUMBER);
                textInputLayout.addView(et_field);

                // TextView (label)
                TextView tv_label = new TextView(this);
                tv_label.setLayoutParams(defaultLayoutParams);
                tv_label.setId(dataValue.getId().intValue()+SectionActivity.LABEL_INCR);
                if (section.hasMultipleCategories()) {
                    tv_label.setText(dataValue.getFullLabel());
                } else {
                    tv_label.setText(dataValue.getLabel());
                }
                textInputLayout.addView(tv_label);

                // basic validation on layout+field
                setAssertPositiveIntegerOrNull(textInputLayout);

                // Hints for checks
                boolean has_validations = DataValidation.getAllFor(dataValue.getId()).size() > 0;
                textInputLayout.setHintEnabled(has_validations);
                if (has_validations) {
                    textInputLayout.setHint(DataValidation.getCombinedHintsFor(dataValue, section.hasMultipleCategories()));
                }

                // record reference to field in list
                allFields.add(et_field);

                // store field as the latest one for Ime tweak
                lastField = et_field;
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
        return DataValidation.displayCoherenceErrorsPopup(this, section.getId());
    }

}
