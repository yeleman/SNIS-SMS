package com.yeleman.snisrdcsms;


import android.support.annotation.NonNull;
import com.orm.SugarRecord;

public class OrganisationUnit extends SugarRecord {

    private String dhisId;
    private String label;

    public OrganisationUnit(){
    }

    public static void truncate() {
        SugarRecord.deleteAll(OrganisationUnit.class);
        Utils.truncateTable("ORGANISATION_UNIT");
    }

    private OrganisationUnit(@NonNull String dhisId, @NonNull String label) {
        setDhisId(dhisId);
        setLabel(label);
    }

    public static Long create(String dhisId, String label) {
        OrganisationUnit organisationUnit = new OrganisationUnit(dhisId, label);
        return organisationUnit.save();
    }

    public static void createOrUpdate(String dhisId, String label) {
        if (OrganisationUnit.exists(dhisId)) {
            OrganisationUnit organisationUnit = OrganisationUnit.findByDHISId(dhisId);
            organisationUnit.setDhisId(dhisId);
            organisationUnit.save();
        } else {
            OrganisationUnit.create(dhisId, label);
        }

    }

    public static Boolean exists(String dhisId) {
        String[] params = new String[] { dhisId };
        return OrganisationUnit.count(OrganisationUnit.class, "DHIS_ID = ?", params) > 0;
    }

    public static OrganisationUnit findByDHISId(String dhisId) {
        return OrganisationUnit.find(OrganisationUnit.class, "DHIS_ID = ?", dhisId).get(0);
    }

    private void setDhisId(String dhisId) {
        this.dhisId = dhisId;
    }

    public String getDhisId() {
        return dhisId;
    }

    private void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String getOrNull(String key) {
        if (!Config.exists(key)) {
            return null;
        }
        return Config.get(key);
    }
}
