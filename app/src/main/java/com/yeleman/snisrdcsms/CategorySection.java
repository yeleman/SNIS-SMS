package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;

import com.orm.SugarRecord;

import java.util.List;

public class CategorySection extends SugarRecord {

    private Long categoryId;
    private Long sectionId;

    public CategorySection(){
    }

    public static void truncate() {
        SugarRecord.deleteAll(CategorySection.class);
        Utils.truncateTable("CATEGORY_SECTION");
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
        return Category.findWithQuery(
                Category.class,
                "SELECT * FROM CATEGORY WHERE ID IN " +
                "(SELECT CATEGORY_ID FROM CATEGORY_SECTION WHERE SECTION_ID=?);",
                String.valueOf(sectionId));
    }

    private void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    private void setSectionId(Long sectionId) {
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
