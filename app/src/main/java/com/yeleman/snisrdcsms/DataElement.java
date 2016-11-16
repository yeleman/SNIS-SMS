package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;

public class DataElement extends SugarRecord {

    private String dhisId;
    private String label;
    private Integer m_order;

    public DataElement(){
    }

    public static void truncate() {
        SugarRecord.deleteAll(DataElement.class);
        Utils.truncateTable("DATA_ELEMENT");
    }

    public DataElement(@NonNull String dhisId, @NonNull String label, @NonNull Integer order) {
        setDhisId(dhisId);
        setLabel(label);
        setOrder(order);
    }

    public static Boolean exists(String dhisId) {
        String[] params = new String[] { dhisId };
        return Category.count(DataElement.class, "DHIS_ID = ? ", params) > 0;
    }

    public static DataElement findByDHISId(String dhisId) {
        try {
            return DataElement.find(DataElement.class, "DHIS_ID = ?", dhisId).get(0);
        } catch (IndexOutOfBoundsException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Long create(String dhisId, String label, Integer order) {
        DataElement dataElement = new DataElement(dhisId, label, order);
        return dataElement.save();
    }

    private void setDhisId(String dhisId) {
        this.dhisId = dhisId;
    }

    public String getDhisId() {
        return dhisId;
    }

    public String getStringId() {
        return String.valueOf(getId());
    }

    private void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private void setOrder(Integer order) {
        this.m_order = order;
    }

    public Integer getOrder() {
        return m_order;
    }
}
