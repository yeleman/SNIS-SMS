package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.Iterator;
import java.util.List;

public class DataValue extends SugarRecord {

    private Long dataElementId;
    private Long categoryId;
    private String value;

    public DataValue(){
    }

    public static void truncate() {
        SugarRecord.deleteAll(DataValue.class);
        Utils.truncateTable("DATA_VALUE");
    }

    public DataValue(@NonNull Long dataElement, Long category, String value) {
        setDataElementId(dataElement);
        setCategoryId(category);
        setValue(value);
    }

    public static Long create(Long dataElement, Long category, String value) {
        DataValue dataValue = new DataValue(dataElement, category, value);
        return dataValue.save();
    }

    public static Long create(DataElement dataElement, Category category, String value) {
        Long categoryId = null;
        if (category != null) {
            categoryId = category.getId();
        }
        return create(dataElement.getId(), categoryId, value);
    }

    public static DataValue findWith(Long dataElementId, Long categoryId) {
//        return findAllWith(dataElementId, categoryId).get(0);
        if (categoryId == null) {
            return Select.from(DataValue.class).where(Condition.prop("DATA_ELEMENT_ID").eq(dataElementId)).first();
        } else {
            return Select.from(DataValue.class).where(Condition.prop("DATA_ELEMENT_ID").eq(dataElementId), Condition.prop("CATEGORY_ID").eq(categoryId)).first();
        }
    }

    public static List<DataValue> findAllWith(Long dataElementId) {
        return Select.from(DataValue.class).where(Condition.prop("DATA_ELEMENT_ID").eq(dataElementId)).list();
    }

    private void setDataElementId(Long dataElementId) { this.dataElementId = dataElementId; }

    public Long getDataElementId() {
        return dataElementId;
    }

    public DataElement getActualDataElement() {
        return DataElement.findById(DataElement.class, getDataElementId());
    }

    public Category getActualCategory() {
        return Category.findById(Category.class, getCategoryId());
    }

    private void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public Integer getIntegerValue() {
        return (getValue() == null) ? null : Integer.valueOf(getValue());
    }

    public String getFormattedValue() {
        return getValue() == null ? Constants.MISSING_VALUE : getValue();
    }

    public String getDisplayValue() {
        return getValue() == null ? Constants.MISSING_VALUE : Utils.numberFormat(Integer.valueOf(getValue()));
    }

    private void resetValue() { setValue(null); save(); }

    private HumanId getHumanId() {
        return new HumanId(this);
    }

    public String getHuman() {
        return getHumanId().getHuman();
    }

    public static void resetAll() {
        Iterator<DataValue> allDataValues = DataValue.findAll(DataValue.class);
        while (allDataValues.hasNext()) {
            allDataValues.next().resetValue();
        }
    }

    public static List<DataValue> listAllFilled() {
        return Select.from(DataValue.class).where(Condition.prop("VALUE").isNotNull()).list();
    }

    public static Long countAll() { return DataValue.count(DataValue.class); }

    public static Long countNonNull() {
        return Select.from(DataValue.class).where(Condition.prop("VALUE").isNotNull()).count();
    }

    public static Boolean hasValues() {
        return countNonNull() > 0;
    }

    public static Double getCompletionPercentage() {
        return Double.valueOf(countNonNull()) / Double.valueOf(countAll());
    }

    public boolean hasCategory() {
        return getCategoryId() != null;
    }

    public String getLabel() {
        return getActualDataElement().getLabel();
    }

    public String getFullLabel() {
        return String.format("%1$s %2$s", getActualDataElement().getLabel(), getActualCategory().getLabel());
    }
}
