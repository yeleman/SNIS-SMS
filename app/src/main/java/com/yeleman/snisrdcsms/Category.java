package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;

public class Category extends SugarRecord {

    private String dhisId;
    private String label;
    private Integer m_order;

    public Category(){
    }

    public Category(@NonNull String dhisId, @NonNull String label, @NonNull Integer order) {
        setDhisId(dhisId);
        setLabel(label);
        setOrder(order);
    }

    public static Long create(String dhisId, String label, Integer order) {
        Category category = new Category(dhisId, label, order);
        return category.save();
    }

    public static Boolean exists(String dhisId) {
        String[] params = new String[1];
        params[0] = dhisId;
        return Category.count(Category.class, "DHIS_ID = ? ", params) > 0;
    }

    public static Category findByDHISId(String dhisId) {
        return Category.find(Category.class, "DHIS_ID = ?", dhisId).get(0);
    }

    void setDhisId(String dhisId) {
        this.dhisId = dhisId;
    }

    public String getDhisId() {
        return dhisId;
    }

    void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    void setOrder(Integer order) {
        this.m_order = order;
    }

    public Integer getOrder() {
        return m_order;
    }
}
