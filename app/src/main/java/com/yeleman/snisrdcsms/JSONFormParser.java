package com.yeleman.snisrdcsms;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.orm.SugarRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

interface JSONFormParserVersion {
    void removePreviousData();
    void readAndProcessForm(JSONObject jsonObject) throws JSONException;
}

class JSONFormParserMixin {
    public static final String KEY_DB_INITIALIZED = "db_initialized";
    public static final String KEY_SMS_FORMAT = "smsFormat";
    public static final String KEY_KEYWORD = "keyword";
    public static final String KEY_NAME = "name";
    public static final String KEY_VERSION = "version";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_CATEGORIES = "categories";
    public static final String KEY_ID = "id";
    public static final String KEY_LABEL = "label";
    public static final String KEY_DATA_ELEMENTS = "dataElements";
    public static final String KEY_ORGANISATION_UNITS = "organisationUnits";

    public static final String SMS_SPACER = " ";

    public static String getString(JSONObject obj, String key) {
        try {
            return obj.getString(key);
        } catch (JSONException ex) {
            return null;
        }
    }

    public static Integer getInt(JSONObject obj, String key) {
        try {
            return obj.getInt(key);
        } catch (JSONException ex) {
            return null;
        }
    }

    public static JSONArray getJSONArray(JSONObject obj, String key) {
        try {
            return obj.getJSONArray(key);
        } catch (JSONException ex) {
            return null;
        }
    }
}

public class JSONFormParser extends JSONFormParserMixin {

    public static final String TAG = Constants.getLogTag("JSONFormParser");
    public static final HashMap<Integer,JSONFormParserVersion> VERSIONS = new HashMap<>();
    static{ VERSIONS.put(2, new JSONFormVersion2()); }


    public static void removePreviousData(int upToVersion) {
        for (Map.Entry<Integer, JSONFormParserVersion> entry : VERSIONS.entrySet()) {
            Integer version = entry.getKey();
            JSONFormParserVersion parser = entry.getValue();
            if (version > upToVersion) {
                continue;
            }
            parser.removePreviousData();
        }
    }

    public static void readAndProcessForm(int version, JSONObject jsonObject) throws JSONException {
        JSONFormParserVersion parser = VERSIONS.get(version);
        parser.readAndProcessForm(jsonObject);
    }

    public static Boolean initializeFormFromJSON(Context context, String fileName) throws JSONException {
        String filePath = String.format("sms/%s", fileName);
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(Utils.readAssetFile(context, filePath));
        } catch (JSONException ex) {
            return false;
        }
        // SMS format version
        Integer version = getInt(jsonObject, KEY_VERSION);
        if (version == null) {
            return false;
        }

        if (Config.getOrNull(KEY_DB_INITIALIZED) != null && version.toString().equals(Config.get(KEY_VERSION))) {
            return false;
        }

        // empty common Config model
        SugarRecord.deleteAll(Config.class);

        // fill config values first
        Config.set(
                KEY_SMS_FORMAT,
                TextUtils.join(SMS_SPACER, Utils.toStringArray(getJSONArray(jsonObject, KEY_SMS_FORMAT))));
        Config.set(KEY_KEYWORD, getString(jsonObject, KEY_KEYWORD));
        Config.set(KEY_NAME, getString(jsonObject, KEY_NAME));
        Config.set(KEY_VERSION, String.valueOf(version));

        // remove data from all models
        removePreviousData(version);

        // actually parse form content and fill database
        readAndProcessForm(version, jsonObject);

        // mark process as complete
        Config.set(KEY_DB_INITIALIZED, "yes");

        return true;
    }
}


class JSONFormVersion2 extends JSONFormParserMixin implements JSONFormParserVersion {

    public static final String TAG = Constants.getLogTag("JSONFormVersion2");

    public void removePreviousData() {
        Log.d(TAG, "removePreviousData");
        // empty all models
        SugarRecord.deleteAll(Category.class);
        SugarRecord.deleteAll(CategorySection.class);
        SugarRecord.deleteAll(DataElement.class);
        SugarRecord.deleteAll(DataElementSection.class);
        SugarRecord.deleteAll(DataValue.class);
        SugarRecord.deleteAll(OrganisationUnit.class);
        SugarRecord.deleteAll(Section.class);
    }

    public void readAndProcessForm(JSONObject jsonObject) throws JSONException {
        Log.d(TAG, "readAndProcessForm");

        // record expected organisationUnits
        JSONArray organisationUnits = getJSONArray(jsonObject, KEY_ORGANISATION_UNITS);
        for (int l = 0; l< (organisationUnits != null ? organisationUnits.length() : 0); l++) {
            Log.d(TAG, "orgunit("+l+")");
            JSONObject organisationUnit = organisationUnits.getJSONObject(l);
            Long organisationUnitId = OrganisationUnit.create(
                    getString(organisationUnit, KEY_ID),
                    getString(organisationUnit, KEY_LABEL));
            Log.d(TAG, "Created organisation unit with id="+organisationUnitId);
        }

        // loops on groups
        JSONArray groups = getJSONArray(jsonObject, "sections");
        for(int i = 0; i< (groups != null ? groups.length() : 0); i++){
            JSONObject group = groups.getJSONObject(i);
            Log.d(TAG, "section("+i+")");

            // record Group
            String categoryType;
            if (group.has(KEY_CATEGORY) || group.has(KEY_CATEGORIES)) {
                categoryType = group.has(KEY_CATEGORY) ? Section.SINGLE_CATEGORY : Section.MULTIPLE_CATEGORIES;
            } else {
                categoryType = Section.NO_CATEGORY;
            }
            Long sectionId = Section.create(
                    getString(group, KEY_ID),
                    getString(group, KEY_NAME), categoryType, i);
            Log.d(TAG, "Created section with id="+sectionId.toString()+": "+getString(group, KEY_NAME));

            // record Category (single or multiple)
            if (group.has(KEY_CATEGORY)) {
                JSONObject category = group.getJSONObject(KEY_CATEGORY);
                if (!Category.exists(getString(category, KEY_ID))) {
                    Long categoryId = Category.create(
                            getString(category, KEY_ID),
                            getString(category, KEY_LABEL), 0);
                    Log.d(TAG, "Created Category with id="+categoryId.toString()+": "+getString(category, KEY_LABEL));
                    CategorySection.create(categoryId, sectionId);
                    Log.d(TAG, "Created CategorySection with ids: "+categoryId.toString()+", "+sectionId);
                }
            } else if (group.has(KEY_CATEGORIES)) {
                JSONArray categories = getJSONArray(group, KEY_CATEGORIES);
                for (int j = 0; j< (categories != null ? categories.length() : 0); j++) {
                    Log.d(TAG, "categories("+j+")");
                    JSONObject category = categories.getJSONObject(j);
                    Long categoryId;
                    if (!Category.exists(getString(category, KEY_ID))) {
                        categoryId = Category.create(
                                getString(category, KEY_ID),
                                getString(category, KEY_LABEL), j);
                        Log.d(TAG, "Created Category with id="+categoryId.toString()+": "+getString(category, KEY_LABEL));
                    } else {
                        categoryId = Category.findByDHISId(getString(category, KEY_ID)).getId();
                    }
                    CategorySection.create(categoryId, sectionId);
                    Log.d(TAG, "Created CategorySection with ids: "+categoryId.toString()+", "+sectionId);
                }
            }

            // record DataElement
            JSONArray dataElements = getJSONArray(group, KEY_DATA_ELEMENTS);
            for (int k = 0; k< (dataElements != null ? dataElements.length() : 0); k++) {
                Log.d(TAG, "dataelements("+k+")");
                JSONObject dataElement = dataElements.getJSONObject(k);

                Long dataElementId;
                if (!DataElement.exists(getString(dataElement, KEY_ID))) {
                    dataElementId = DataElement.create(
                            getString(dataElement, KEY_ID),
                            getString(dataElement, KEY_LABEL), k);
                    Log.d(TAG, "Created DataElement with id="+dataElementId.toString()+": "+getString(dataElement, KEY_LABEL));
                }
                dataElementId = DataElement.findByDHISId(getString(dataElement, KEY_ID)).getId();
                DataElementSection.create(dataElementId, sectionId);
                Log.d(TAG, "Created DataElementGroup with ids: "+dataElementId.toString()+", "+sectionId);
            }
        }

        // record expected DataValue
        for (DataElementSection dataElementSection: DataElementSection.listAll(DataElementSection.class)) {
            String[] params = new String[] {String.valueOf(dataElementSection.getSectionId())};
            for (CategorySection categorySection: CategorySection.find(CategorySection.class, "SECTION_ID = ?", params)) {
                DataValue.create(dataElementSection.getDataElementId(), categorySection.getCategoryId(), null);
                Log.d(TAG, "Created DataValue with id="+dataElementSection.getDataElementId().toString()+": "+categorySection.getCategoryId().toString());
            }
            if (dataElementSection.getSection().getCategoryType().equals(Section.NO_CATEGORY)) {
                DataValue.create(dataElementSection.getDataElementId(), null, null);
                Log.d(TAG, "Created DataValue with id="+dataElementSection.getDataElementId().toString()+": null");
            }
        }
    }
}
