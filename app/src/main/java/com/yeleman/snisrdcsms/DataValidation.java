package com.yeleman.snisrdcsms;


import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.query.Condition;
import com.orm.query.Select;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DataValidation extends SugarRecord {

    @Ignore
    public static final String EQUAL_TO = "eq";
    @Ignore
    public static final String NOT_EQUAL_TO = "neq";
    @Ignore
    public static final String LESS_THAN_OR_EQUAL_TO = "lte";
    @Ignore
    public static final String LESS_THAN = "lt";
    @Ignore
    public static final String GREATER_THAN_OR_EQUAL_TO = "gte";
    @Ignore
    public static final String GREATER_THAN = "gt";

    @Ignore
    public static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(new String[] {
            EQUAL_TO, NOT_EQUAL_TO, LESS_THAN, LESS_THAN_OR_EQUAL_TO, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO }));

    @Ignore
    public static final HashMap<String, String> OPERATORS_SYMBOLS = new HashMap<>();
    private static final int INFO_ICON_COLOR = Color.rgb(73, 116, 180);
    private static final int INFO_ICON_DELAY = 4;
    private static final int DISABLED_INFO_ICON_COLOR = Color.GRAY;

    static {
        OPERATORS_SYMBOLS.put(EQUAL_TO, "=");
        OPERATORS_SYMBOLS.put(NOT_EQUAL_TO, "!=");
        OPERATORS_SYMBOLS.put(LESS_THAN, "<");
        OPERATORS_SYMBOLS.put(LESS_THAN_OR_EQUAL_TO, "<=");
        OPERATORS_SYMBOLS.put(GREATER_THAN, ">");
        OPERATORS_SYMBOLS.put(GREATER_THAN_OR_EQUAL_TO, ">=");
    }

    @Ignore
    public static final HashMap<String, String> REVERSED_OPERATORS_SYMBOLS = new HashMap<>();
    static {
        REVERSED_OPERATORS_SYMBOLS.put(EQUAL_TO, "=");
        REVERSED_OPERATORS_SYMBOLS.put(NOT_EQUAL_TO, "!=");
        REVERSED_OPERATORS_SYMBOLS.put(LESS_THAN, ">");
        REVERSED_OPERATORS_SYMBOLS.put(LESS_THAN_OR_EQUAL_TO, ">=");
        REVERSED_OPERATORS_SYMBOLS.put(GREATER_THAN, "<");
        REVERSED_OPERATORS_SYMBOLS.put(GREATER_THAN_OR_EQUAL_TO, "<=");
    }

    private Long sectionId;
    private String operator;
    private String left;
    private String right;

    public DataValidation() {
    }

    public static void truncate() {
        SugarRecord.deleteAll(DataValidation.class);
        Utils.truncateTable("DATA_VALIDATION");
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setOperator(String operator) {
        if (!OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("Not a valid operator");
        }
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getLeft() {
        return left;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public String getRight() {
        return right;
    }


    public DataValidation(Long sectionId, @NonNull String operator, @NonNull String left, @NonNull String right) {
        setSectionId(sectionId);
        setOperator(operator);
        setLeft(left);
        setRight(right);
    }

    public static Long create(Long sectionId, @NonNull String operator, @NonNull String left, @NonNull String right) {
        DataValidation dataValidation = new DataValidation(sectionId, operator, left, right);
        return dataValidation.save();
    }

    public static List<DataValidation> getForSection(Long sectionId) {
        return Select.from(DataValidation.class).where(Condition.prop("SECTION_ID").eq(sectionId)).list();
    }

    public static List<DataValidation> getFailingForSection(CheckedFormActivity activity, Long sectionId) {
        List<DataValidation> failingValidations = new ArrayList<>();
        for (DataValidation dataValidation: DataValidation.getForSection(sectionId)) {
            if (!dataValidation.isValid(activity)) {
                failingValidations.add(dataValidation);
            }
        }
        return failingValidations;
    }

    public static List<DataValidation> getCrossSectionsFailing() {
        List<DataValidation> failingValidations = new ArrayList<>();
        for (DataValidation dataValidation: DataValidation.getForSection(null)) {
            if (!dataValidation.isValid()) {
                failingValidations.add(dataValidation);
            }
        }
        return failingValidations;
    }

    public static List<DataValidation> getForSectionAndOperator(Long sectionId, String operator) {
        return Select.from(DataValidation.class).where(Condition.prop("SECTION_ID").eq(sectionId))
                     .and(Condition.prop("OPERATOR").eq(operator)).list();
    }

    public static HashMap<String, List<DataValidation>> getGroupedForSection(Long sectionId) {
        List<DataValidation> validations = DataValidation.getForSection(sectionId);
        HashMap<String, List<DataValidation>> hashMap = new HashMap<>();
        for (DataValidation dataValidation: validations) {
            String operator = dataValidation.getOperator();
            List<DataValidation> operatorValidations =
                    (hashMap.containsKey(operator)) ? hashMap.get(operator) : new ArrayList<DataValidation>();
            operatorValidations.add(dataValidation);
            hashMap.put(operator, operatorValidations);
        }
        return hashMap;
    }

    public static HashMap<String, List<DataValidation>> getGroupedCrossSectionsFailing() {
        List<DataValidation> validations = DataValidation.getCrossSectionsFailing();
        HashMap<String, List<DataValidation>> hashMap = new HashMap<>();
        for (DataValidation dataValidation: validations) {
            String operator = dataValidation.getOperator();
            List<DataValidation> operatorValidations =
                    (hashMap.containsKey(operator)) ? hashMap.get(operator) : new ArrayList<DataValidation>();
            operatorValidations.add(dataValidation);
            hashMap.put(operator, operatorValidations);
        }
        return hashMap;
    }

    public static HashMap<String, List<DataValidation>> getGroupedFailingForSection(CheckedFormActivity activity, Long sectionId) {
        List<DataValidation> validations = DataValidation.getFailingForSection(activity, sectionId);
        HashMap<String, List<DataValidation>> hashMap = new HashMap<>();
        for (DataValidation dataValidation: validations) {
            String operator = dataValidation.getOperator();
            List<DataValidation> operatorValidations =
                    (hashMap.containsKey(operator)) ? hashMap.get(operator) : new ArrayList<DataValidation>();
            operatorValidations.add(dataValidation);
            hashMap.put(operator, operatorValidations);
        }
        return hashMap;
    }

    public Section getSection() {
        return Section.findById(Section.class, getSectionId());
    }

    public DataValue getLeftDataValue() {
        HumanId humanId = new HumanId(getLeft());
        return humanId.getDataValue();
    }

    public DataValue getRightDataValue() {
        HumanId humanId = new HumanId(getRight());
        return humanId.getDataValue();
    }

    public EditText getLeftField(CheckedFormActivity activity) {
        return (EditText) activity.findViewById(getLeftDataValue().getId().intValue()+SectionActivity.INPUT_INCR);
    }

    public EditText getRightField(CheckedFormActivity activity) {
        return (EditText) activity.findViewById(getRightDataValue().getId().intValue()+SectionActivity.INPUT_INCR);
    }

    public TextInputLayout getLeftInputLayout(CheckedFormActivity activity) {
        return (TextInputLayout) activity.findViewById(getLeftDataValue().getId().intValue()+SectionActivity.LAYOUT_INCR);
    }

    public TextInputLayout getRightInputLayout(CheckedFormActivity activity) {
        return (TextInputLayout) activity.findViewById(getRightDataValue().getId().intValue()+SectionActivity.LAYOUT_INCR);
    }

    public void setEntryCheck(CheckedFormActivity activity) {
        EditText left = getLeftField(activity);
        EditText right = getRightField(activity);
        activity.setAssertLessThanOrEqualTo(left, right, getLeftDataValue(), getRightDataValue());
    }

    public void setTextInputEntryCheck(CheckedFormActivity activity) {
        TextInputLayout left = getLeftInputLayout(activity);
        TextInputLayout right = getRightInputLayout(activity);
        activity.setAssertLessThanOrEqualTo(left, right, getLeftDataValue(), getRightDataValue());
    }

    public boolean isValid() {
        Integer leftValue = getLeftDataValue().getIntegerValue();
        Integer rightValue = getRightDataValue().getIntegerValue();
        if (leftValue == null && rightValue == null) {
            return true;
        } else if (leftValue == null || rightValue == null) {
            return false;
        }
        switch(getOperator()) {
            case LESS_THAN:
                return leftValue < rightValue;
            case LESS_THAN_OR_EQUAL_TO:
                return leftValue <= rightValue;
            case EQUAL_TO:
                return leftValue == rightValue;
            case NOT_EQUAL_TO:
                return leftValue != rightValue;
            case GREATER_THAN:
                return leftValue > rightValue;
            case GREATER_THAN_OR_EQUAL_TO:
                return leftValue >= rightValue;
        }
        Log.e(Utils.TAG, "Unknown operator type: " + getOperator());
        return false;
    }

    public boolean isValid(CheckedFormActivity activity) {
        EditText left = getLeftField(activity);
        EditText right = getRightField(activity);
        switch(getOperator()) {
            case LESS_THAN:
                return activity.isLessThan(left, right);
            case LESS_THAN_OR_EQUAL_TO:
                return activity.isLessThanOrEqual(left, right);
            case EQUAL_TO:
                return activity.isEqual(left, right);
            case NOT_EQUAL_TO:
                return activity.isNotEqual(left, right);
            case GREATER_THAN:
                return activity.isGreaterThan(left, right);
            case GREATER_THAN_OR_EQUAL_TO:
                return activity.isGreaterThanOrEqual(left, right);
        }
        Log.e(Utils.TAG, "Unknown operator type: " + getOperator());
        return false;
    }

    public boolean isLeft(String dhisId) {
        return dhisId.equals(left);
    }

    public boolean isRight(String dhisId) {
        return dhisId.equals(right);
    }

    public String getLeftLabel(boolean withCategory) {
        if (withCategory) {
            return getLeftDataValue().getFullLabel();
        }
        return getLeftDataValue().getLabel();
    }
    public String getLeftLabel() {
        return getLeftLabel(false);
    }

    public String getRightLabel(boolean withCategory) {
        if (withCategory) {
            return getRightDataValue().getFullLabel();
        }
        return getRightDataValue().getLabel();
    }
    public String getRightLabel() {
        return getRightLabel(false);
    }

    public String getOperatorSymbol(boolean reversed) {
        return getOperatorSymbolFor(getOperator(), reversed);
    }

    public static String getOperatorSymbolFor(String operator, boolean reversed) {
        HashMap<String, String> hashMap = reversed ? REVERSED_OPERATORS_SYMBOLS : OPERATORS_SYMBOLS;
        String symbol = hashMap.get(operator);
        if (symbol == null) {
            symbol = operator;
        }
        return symbol;
    }

    public String getLeftHint(boolean withCategory) {
        return String.format("%1$s %2$s", getOperatorSymbol(false), getRightLabel(withCategory));
    }

    public String getRightHint(boolean withCategory) {
        return String.format("%1$s %2$s", getOperatorSymbol(true), getLeftLabel(withCategory));
    }

    public String getHintFor(DataValue dataValue, boolean withCategory) {
        if (isLeft(dataValue.getHuman())) {
            return getLeftHint(withCategory);
        } else if (isRight(dataValue.getHuman())) {
            return getRightHint(withCategory);
        }
        return null;
    }

    public static List<DataValidation> getAllFor(Long dataValueId) {
        DataValue dataValue = DataValue.findById(DataValue.class, dataValueId);
        HumanId humanId = new HumanId(dataValue);
        if (humanId.getSection() == null) {
            return DataValidation.find(DataValidation.class,
                    "SECTION_ID IS NULL and (LEFT=? or RIGHT=?)",
                    humanId.getHuman(), humanId.getHuman());
        } else {
            return DataValidation.find(DataValidation.class,
                    "SECTION_ID = ? and (LEFT=? or RIGHT=?)",
                    humanId.getSection().getStringId(), humanId.getHuman(), humanId.getHuman());
        }
    }

    public static List<String> getHintsFor(List<DataValidation> dataValidations, DataValue dataValue, boolean withCategory) {
        List<String> hints = new ArrayList<>();
        for (DataValidation dataValidation: dataValidations) {
            hints.add(dataValidation.getHintFor(dataValue, withCategory));
        }
        Set<String> depdupeHints = new LinkedHashSet<>(hints);
        hints.clear();
        hints.addAll(depdupeHints);
        return hints;
    }

    public static String getCombinedHintsFor(DataValue dataValue, boolean withCategory) {
        return StringUtils.join(getHintsFor(getAllFor(dataValue.getId()), dataValue, withCategory));
    }

    public static boolean displayCoherenceErrorsPopup(CheckedFormActivity activity, Long sectionId) {
        boolean isCrossSection = sectionId == null;
        HashMap<String, List<DataValidation>> validationsMap = (isCrossSection) ? DataValidation.getGroupedCrossSectionsFailing() : DataValidation.getGroupedFailingForSection(activity, sectionId);
        if (validationsMap.values().isEmpty()) {
            return true;
        }

        AlertDialog.Builder errorDialogBuilder = Popups.getDialogBuilder(activity,
                activity.getString(R.string.validation_dialog_title), null, false);

        // base layout
        LayoutInflater inflater = activity.getLayoutInflater();
        final View baseLayout = inflater.inflate(R.layout.data_validation_dialog, null);
        final LinearLayout mainLayout = (LinearLayout) baseLayout.findViewById(R.id.ll_layout);

        View lastSeparator = null;

        for (String operator: validationsMap.keySet()) {
            // operator line
            final View operatorLayout = inflater.inflate(R.layout.data_validation_dialog_operator, null);
            TextView operatorTextView = (TextView) operatorLayout.findViewById(R.id.tv_operator);
            operatorTextView.setText(DataValidation.getOperatorSymbolFor(operator, false));
            mainLayout.addView(operatorLayout);

            // separator
            final View operatorSeparatorLine = inflater.inflate(R.layout.horizontal_line, null);
            mainLayout.addView(operatorSeparatorLine);

            for (DataValidation dataValidation: validationsMap.get(operator)) {

                Integer leftValue;
                Integer rightValue;
                if (sectionId == null) {
                    leftValue = dataValidation.getLeftDataValue().getIntegerValue();
                    rightValue = dataValidation.getRightDataValue().getIntegerValue();
                } else {
                    leftValue = activity.integerFromField(dataValidation.getLeftField(activity), null);
                    rightValue = activity.integerFromField(dataValidation.getRightField(activity), null);
                }

                final View leftLineLayout = inflater.inflate(R.layout.data_validation_dialog_line, null);
                final View rightLineLayout = inflater.inflate(R.layout.data_validation_dialog_line, null);

                // left
                TextView leftLetter = (TextView) leftLineLayout.findViewById(R.id.tv_letter);
                leftLetter.setText("A");
                final TextView leftLabel = (TextView) leftLineLayout.findViewById(R.id.tv_label);
                final String leftDataElementLabel = dataValidation.getLeftLabel();
                final String leftSectionLabel = Section.getFor(
                        dataValidation.getLeftDataValue().getDataElementId(),
                        dataValidation.getLeftDataValue().getCategoryId()).getLabel();
                leftLabel.setText(leftDataElementLabel);
                if (dataValidation.getSection() == null || dataValidation.getSection().hasMultipleCategories()) {
                    if (dataValidation.getLeftDataValue().hasCategory()) {
                        View leftCategoryDivider = leftLineLayout.findViewById(R.id.dv_category);
                        leftCategoryDivider.setVisibility(View.VISIBLE);
                        TextView leftCategory = (TextView) leftLineLayout.findViewById(R.id.tv_category);
                        leftCategory.setVisibility(View.VISIBLE);
                        leftCategory.setText(dataValidation.getLeftDataValue().getActualCategory().getLabel());

                        // display info icon with switch to show section name
                        if (isCrossSection) {
                            final ImageView leftInfoIcon = (ImageView) leftLineLayout.findViewById(R.id.iv_info);
                            leftInfoIcon.setVisibility(View.VISIBLE);

                            final Handler handler = new Handler() {
                                @Override
                                public void handleMessage(final Message msg) {
                                    super.handleMessage(msg);
                                    leftLabel.setText(leftDataElementLabel);
                                    leftInfoIcon.setEnabled(true);
                                    leftInfoIcon.setColorFilter(DataValidation.INFO_ICON_COLOR);

                                }
                            };
                            class MyRunnable implements Runnable {
                                @Override
                                public void run() { handler.sendEmptyMessage(0); }
                            }

                            leftInfoIcon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    leftInfoIcon.setEnabled(false);
                                    leftInfoIcon.setColorFilter(DataValidation.DISABLED_INFO_ICON_COLOR);
                                    leftLabel.setText(leftSectionLabel);
                                    handler.postDelayed(new MyRunnable(), DataValidation.INFO_ICON_DELAY * 1000);
                                }
                            });
                        }
                    }
                }
                TextView leftValueView = (TextView) leftLineLayout.findViewById(R.id.tv_value);
                leftValueView.setText(Utils.numberFormat(leftValue));

                // separator
                final View fieldsSeparatorLine = inflater.inflate(R.layout.horizontal_line, null);

                // right
                TextView rightLetter = (TextView) rightLineLayout.findViewById(R.id.tv_letter);
                rightLetter.setText("B");
                final TextView rightLabel = (TextView) rightLineLayout.findViewById(R.id.tv_label);
                final String rightDataElementLabel = dataValidation.getRightLabel();
                final String rightSectionLabel = Section.getFor(
                        dataValidation.getRightDataValue().getDataElementId(),
                        dataValidation.getRightDataValue().getCategoryId()).getLabel();
                rightLabel.setText(rightDataElementLabel);
                if (dataValidation.getSection() == null || dataValidation.getSection().hasMultipleCategories()) {
                    if (dataValidation.getRightDataValue().hasCategory()) {
                        View rightCategoryDivider = rightLineLayout.findViewById(R.id.dv_category);
                        rightCategoryDivider.setVisibility(View.VISIBLE);
                        TextView rightCategory = (TextView) rightLineLayout.findViewById(R.id.tv_category);
                        rightCategory.setVisibility(View.VISIBLE);
                        rightCategory.setText(dataValidation.getRightDataValue().getActualCategory().getLabel());

                        // display info icon with switch to show section name
                        if (isCrossSection) {
                            final ImageView rightInfoIcon = (ImageView) rightLineLayout.findViewById(R.id.iv_info);
                            rightInfoIcon.setVisibility(View.VISIBLE);

                            final Handler handler = new Handler() {
                                @Override
                                public void handleMessage(final Message msg) {
                                    super.handleMessage(msg);
                                    rightLabel.setText(rightDataElementLabel);
                                    rightInfoIcon.setEnabled(true);
                                    rightInfoIcon.setColorFilter(DataValidation.INFO_ICON_COLOR);
                                }
                            };
                            class MyRunnable implements Runnable {
                                @Override
                                public void run() { handler.sendEmptyMessage(0); }
                            }

                            rightInfoIcon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    rightInfoIcon.setEnabled(false);
                                    rightInfoIcon.setColorFilter(DataValidation.DISABLED_INFO_ICON_COLOR);
                                    rightLabel.setText(rightSectionLabel);
                                    handler.postDelayed(new MyRunnable(), DataValidation.INFO_ICON_DELAY * 1000);
                                }
                            });
                        }
                    }
                }
                TextView rightValueView = (TextView) rightLineLayout.findViewById(R.id.tv_value);
                rightValueView.setText(Utils.numberFormat(rightValue));

                // separator
                final View validationSeparatorLine = inflater.inflate(R.layout.thick_horizontal_line, null);

                mainLayout.addView(leftLineLayout);
                mainLayout.addView(fieldsSeparatorLine);
                mainLayout.addView(rightLineLayout);
                mainLayout.addView(validationSeparatorLine);

                lastSeparator = validationSeparatorLine;
            }
        }

        // make bottom line thiner than regular separator
        if (lastSeparator != null) {
            mainLayout.removeView(lastSeparator);
            View lastSeparatorLine = inflater.inflate(R.layout.horizontal_line, null);
            mainLayout.addView(lastSeparatorLine);
        }

        errorDialogBuilder.setView(baseLayout);
        errorDialogBuilder.setPositiveButton(R.string.validation_dialog_button, Popups.getBlankClickListener());
        final AlertDialog validationsDialog = errorDialogBuilder.create();
        validationsDialog.show();
        return false;
    }

}