package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.List;

public class CategorySection extends SugarRecord {

    private Long categoryId;
    private Long sectionId;

    public CategorySection(){
    }

    public CategorySection(@NonNull Long categoryId, @NonNull Long sectionId) {
        setCategoryId(categoryId);
        setSectionId(sectionId);
    }

    public static Long create(Long categoryId, Long sectionId) {
        CategorySection categorySection = new CategorySection(categoryId, sectionId);
        return categorySection.save();
    }

    public static List<Category> getCategoriesFor(Long sectionId) {
        List<Category> categories = new ArrayList<>();
        for (CategorySection categorySection: Select.from(CategorySection.class).where(Condition.prop("SECTION_ID").eq(sectionId)).list()) {
            categories.add(Category.findById(Category.class, categorySection.getCategoryId()));
        }
        return categories;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    Category getCategory() {
        return Category.findById(Category.class, categoryId);
    }

    Section getSection() {
        return Section.findById(Section.class, sectionId);
    }
}
