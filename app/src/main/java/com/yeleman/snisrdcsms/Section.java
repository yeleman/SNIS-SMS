package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Section extends SugarRecord {

    @Ignore
    public static final String NO_CATEGORY = "no_category";
    @Ignore
    public static final String SINGLE_CATEGORY = "single_category";
    @Ignore
    public static final String MULTIPLE_CATEGORIES = "multiple_categories";
    @Ignore
    public static final Set<String> CATEGORY_TYPE = new HashSet<>(Arrays.asList(new String[] { NO_CATEGORY, SINGLE_CATEGORY, MULTIPLE_CATEGORIES }));

    private String slug;
    private String label;
    private String categoryType;
    private Integer m_order;

    public Section(){
    }

    public Section(@NonNull String slug, @NonNull String label, @NonNull String categoryType, @NonNull Integer order) {
        setSlug(slug);
        setLabel(label);
        setCategoryType(categoryType);
        setOrder(order);
    }

    public static Long create(String slug, String label, String categoryType, Integer order) {
        Section section = new Section(slug, label, categoryType, order);
        return section.save();
    }

    public static List<Section> getAllSection() {
        return Select.from(Section.class).orderBy("MORDER ASC").list();
    }

    public String getStringId() {
        return String.valueOf(getId());
    }

    void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCategoryLabel() {
        if (hasSingleCategory()) {
            return getActualCategory().getLabel();
        }
        return getLabel();
    }

    public Category getActualCategory() {
        if (hasSingleCategory()) {
            return Category.findWithQuery(
                    Category.class,
                    "SELECT * FROM CATEGORY WHERE ID IN "+
                    "(SELECT CATEGORY_ID FROM CATEGORY_SECTION WHERE SECTION_ID=?)",
                    getStringId()).get(0);
        }
        return null;
    }

    void setCategoryType(String categoryType) {
        if (!CATEGORY_TYPE.contains(categoryType)) {
            throw new IllegalArgumentException("Invalid category type");
        }
        this.categoryType = categoryType;
    }

    public String getCategoryType() {
        return categoryType;
    }

    void setOrder(Integer order) {
        this.m_order = order;
    }

    public Integer getOrder() {
        return m_order;
    }

    public Boolean hasMultipleCategories() { return getCategoryType().equals(MULTIPLE_CATEGORIES); }
    public Boolean hasSingleCategory() { return getCategoryType().equals(SINGLE_CATEGORY); }
    public Boolean hasNoCategory() { return getCategoryType().equals(NO_CATEGORY); }

    public Long getFilledDataValuesNumber() {
        if (hasNoCategory()) {
            return (long) DataValue.findWithQuery(
                    DataValue.class,
                    "SELECT * FROM DATA_VALUE WHERE VALUE IS NOT NULL AND DATA_ELEMENT_ID IN " +
                            "(SELECT DATA_ELEMENT_ID FROM DATA_ELEMENT_SECTION WHERE SECTION_ID=?);",
                    getStringId()).size();
        } else {
            return (long) DataValue.findWithQuery(
                    DataValue.class,
                    "SELECT * FROM DATA_VALUE WHERE VALUE IS NOT NULL AND DATA_ELEMENT_ID IN " +
                            "(SELECT DATA_ELEMENT_ID FROM DATA_ELEMENT_SECTION WHERE SECTION_ID=?) " +
                            "AND CATEGORY_ID IN (SELECT CATEGORY_ID FROM CATEGORY_SECTION WHERE SECTION_ID = ?);",
                    getStringId(), getStringId()).size();
        }
    }

    public List<DataValue> getExpectedDataValues() {
        if (hasMultipleCategories()) {
            return DataValue.findWithQuery(
                    DataValue.class,
                    "SELECT * FROM DATA_VALUE WHERE DATA_ELEMENT_ID IN " +
                            "(SELECT DATA_ELEMENT_ID FROM DATA_ELEMENT_SECTION WHERE SECTION_ID=?);",
                    String.valueOf(getId()));
        } else if (hasSingleCategory()) {
            return DataValue.findWithQuery(
                    DataValue.class,
                    "SELECT * FROM DATA_VALUE WHERE DATA_ELEMENT_ID IN " +
                    "(SELECT DATA_ELEMENT_ID FROM DATA_ELEMENT_SECTION WHERE SECTION_ID=?) " +
                    "AND CATEGORY_ID IN (SELECT CATEGORY_ID FROM CATEGORY_SECTION WHERE SECTION_ID=?);",
                    getStringId(), getStringId());
        } else {
            return DataValue.findWithQuery(
                    DataValue.class,
                    "SELECT * FROM DATA_VALUE WHERE DATA_ELEMENT_ID IN " +
                    "(SELECT DATA_ELEMENT_ID FROM DATA_ELEMENT_SECTION WHERE SECTION_ID = ?) AND CATEGORY_ID IS NULL;",
                    getStringId());
        }
    }

    public Long getExpectedValuesNumber() {
        return (long) getExpectedDataValues().size();
    }
    public String getLabelWithProgression() {
        return String.format(Locale.FRANCE, "%s [%d/%d]", getLabel(), getFilledDataValuesNumber(), getExpectedValuesNumber());
    }

    public String getProgression() {
        return String.format(Locale.FRANCE, "%d/%d", getFilledDataValuesNumber(), getExpectedValuesNumber());
    }
}
