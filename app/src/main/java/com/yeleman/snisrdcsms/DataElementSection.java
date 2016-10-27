package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.List;

public class DataElementSection extends SugarRecord {

    private Long dataElementId;
    private Long sectionId;

    public DataElementSection(){
    }

    public DataElementSection(@NonNull Long dataElementId, @NonNull Long sectionId) {
        setDataElementId(dataElementId);
        setSectionId(sectionId);
    }

    public static Long create(Long dataElementId, Long sectionId) {
        DataElementSection dataElementSection = new DataElementSection(dataElementId, sectionId);
        return dataElementSection.save();
    }

    public static List<DataElement> getDataElementsFor(Long sectionId) {
        List<DataElement> dataElements = new ArrayList<>();
        for (DataElementSection dataElementSection: Select.from(DataElementSection.class).where(Condition.prop("SECTION_ID").eq(sectionId)).list()) {
            dataElements.add(DataElement.findById(DataElement.class, dataElementSection.getDataElementId()));
        }
        return dataElements;
    }

    public void setDataElementId(Long categoryId) {
        this.dataElementId = categoryId;
    }

    public Long getDataElementId() {
        return dataElementId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    Category getDataElement() {
        return Category.findById(Category.class, dataElementId);
    }

    Section getSection() {
        return Section.findById(Section.class, sectionId);
    }
}
