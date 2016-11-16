package com.yeleman.snisrdcsms;

public class HumanId {
    private String dataElementDhisId;
    private String categoryDhisId;

    private Long dataElementId;
    private Long categoryId;
    private Long dataValueId;
    private Long sectionId;

    public HumanId(String dataElementId, String categoryId) {
        setDataElementDhisId(dataElementId);
        setCategoryDhisId(categoryId);
        initializeWithDhisIds();
    }

    public HumanId(String humanId) {
        String[] parts = humanId.split("\\.", 2);
        if (parts.length == 2) {
            setDataElementDhisId(parts[0]);
            setCategoryDhisId(parts[1]);
        } else if (parts.length == 1) {
            setDataElementDhisId(parts[0]);
        } else {
            throw new IllegalArgumentException("Not a DHIS Id");
        }
        initializeWithDhisIds();
    }

    public HumanId(DataValue dataValue) {
        setDataElementDhisId(dataValue.getActualDataElement().getDhisId());
        if (dataValue.getActualCategory() != null) {
            setCategoryDhisId(dataValue.getActualCategory().getDhisId());
        }
        initializeWithDhisIds();
    }

    private void initializeWithDhisIds() {
        // dataElement
        dataElementId = DataElement.findByDHISId(getDataElementDhisId()).getId();

        // category
        if (hasCategory()) {
            categoryId = Category.findByDHISId(getCategoryDhisId()).getId();
        }

        // dataValue
        dataValueId = DataValue.findWith(getDataElementId(), getCategoryId()).getId();

        // section
        sectionId = Section.getFor(getDataElementId(), getCategoryId()).getId();
    }

    private boolean hasCategory() {
        return getCategoryDhisId() != null;
    }

    private void setDataElementDhisId(String dataElementDhisId) {
        this.dataElementDhisId = dataElementDhisId;
    }

    private String getDataElementDhisId() {
        return dataElementDhisId;
    }

    private void setCategoryDhisId(String categoryDhisId) {
        this.categoryDhisId = categoryDhisId;
    }

    private String getCategoryDhisId() {
        return categoryDhisId;
    }

    private Long getDataElementId() {
        return dataElementId;
    }

    public DataElement getDataElement() {
        return DataElement.findById(DataElement.class, getDataElementId());
    }

    private Long getCategoryId() {
        return categoryId;
    }

    public Category getCategory() {
        return Category.findById(Category.class, getCategoryId());
    }

    private Long getDataValueId() {
        return dataValueId;
    }

    public DataValue getDataValue() {
        return DataValue.findById(DataValue.class, getDataValueId());
    }

    private Long getSectionId() {
        return sectionId;
    }

    public Section getSection() {
        return Section.findById(Section.class, getSectionId());
    }

    public String getHuman() {
        if (hasCategory()) {
            return String.format("%1$s.%2$s", getDataElementDhisId(), getCategoryDhisId());
        }
        return getDataElementDhisId();
    }

}
