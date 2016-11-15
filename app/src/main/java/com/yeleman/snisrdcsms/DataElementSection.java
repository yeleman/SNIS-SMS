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

    public static void truncate() {
        SugarRecord.deleteAll(DataElementSection.class);
        Utils.truncateTable("DATA_ELEMENT_SECTION");
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
        return DataElement.findWithQuery(
                DataElement.class,
                "SELECT * FROM DATA_ELEMENT WHERE ID IN "+
                "(SELECT DATA_ELEMENT_ID FROM DATA_ELEMENT_SECTION WHERE SECTION_ID=?);",
                String.valueOf(sectionId));
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
