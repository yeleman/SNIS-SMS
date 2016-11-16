package com.yeleman.snisrdcsms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.orm.SugarRecord;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;


import static android.os.Environment.getExternalStorageDirectory;

class ExternalStorage {
    private boolean available = false;
    private boolean writable = false;
    ExternalStorage() {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            available = writable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            available = true;
            writable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            available = writable = false;
        }
    }
    public boolean isAvailable() { return available; }
    public boolean isWritable() { return writable; }
    public File getDirectory() { return getExternalStorageDirectory(); }
}

class Utils {
    public static final String TAG = Constants.getLogTag("Utils");

    private static ExternalStorage getExternalStorage() { return new ExternalStorage(); }

    private static Boolean copyFile(File from, File to) {
        try {
            FileChannel src = new FileInputStream(from).getChannel();
            FileChannel dst = new FileOutputStream(to).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
            return false;
        }
        return true;
    }

    static Boolean exportDatabase(Context context) {
        File localDBFile = context.getDatabasePath(Constants.getDatabaseName(context));
        Log.i(TAG, "DB exists: "+ localDBFile.exists());

        ExternalStorage sd = getExternalStorage();
        if (!sd.isWritable()) {
            Log.e(TAG, "Unable to write to SD card");
            return false;
        }

        File destination = new File(sd.getDirectory(), context.getPackageName());
        if (!destination.exists()) {
            Log.d(TAG, "destination folder does not exist.");
            if (destination.mkdirs()) {
                Log.d(TAG, "destination folder created.");
            } else {
                Log.e(TAG, "Unable to create export folder.");
                return false;
            }
        } else { Log.d(TAG, "destination folder exist."); }

        Date now = new Date();
        String fileName = String.format("backup-%s.db", now.toString());
        File backupFile = new File(destination, fileName);
        try {
            if (!backupFile.createNewFile()) {
                Log.e(TAG, "Unable to write file at "+backupFile.getAbsolutePath());
                return false;
            }
            if (!copyFile(localDBFile, backupFile)) {
                Log.e(TAG, "Unable to copy file from "+ localDBFile.getAbsolutePath() +" to "+backupFile.getAbsolutePath());
                backupFile.delete();
                return false;
            }
            Log.i(TAG, "exported on " + backupFile.getAbsolutePath());
            return true;
        } catch(IOException ex) {
            backupFile.delete();
            Log.e(TAG, ex.toString());
            return false;
        }
    }

    public static String[] toStringArray(JSONArray array) {
        if(array==null)
            return null;

        String[] arr=new String[array.length()];
        for(int i=0; i<arr.length; i++) {
            arr[i]=array.optString(i);
        }
        return arr;
    }

    static void updateSharedPreferences(Context context, String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(key, value);
        prefEditor.apply();
    }

    public static String readAssetFile(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private static String buildIndexedDataSMSPart() {
        String smsContent = "";
        for(DataValue dataValue: DataValue.listAllFilled()) {
            smsContent += String.format("%s=%s", dataValue.getHuman(), dataValue.getValue()) + JSONFormParser.SMS_SPACER;

        }
        return smsContent.trim();
    }

    private static String buildDataSMSPart() {
        String smsFormat = Config.get(JSONFormParser.KEY_SMS_FORMAT);
        String[] codedIds = smsFormat.split(JSONFormParser.SMS_SPACER);
        String smsContent = "";
        for (String codedId : codedIds) {
            String dataElementId;
            String categoryId = null;
            if (codedId.contains(".")) {
                String[] dataValueParts = codedId.split("\\.");
                dataElementId = dataValueParts[0];
                categoryId = dataValueParts[1];
            } else {
                dataElementId = codedId;
            }
            DataElement dataElement = DataElement.findByDHISId(dataElementId);
            DataValue dataValue;
            if (categoryId == null) {
                dataValue = DataValue.findWith(dataElement.getId(), null);
            } else {
                Category category = Category.findByDHISId(categoryId);
                dataValue = DataValue.findWith(dataElement.getId(), category.getId());
            }

            smsContent += dataValue.getFormattedValue() + JSONFormParser.SMS_SPACER;
        }
        return smsContent.trim();
    }

    private static String obfuscate(String plain){
        return StringUtils.replaceEach(plain, Constants.PLAIN_CHARACTERS, Constants.TRANSLATED_CHARACTERS);
    }

    public static String deobfuscate(String encrypted){
        return StringUtils.replaceEach(encrypted, Constants.TRANSLATED_CHARACTERS, Constants.PLAIN_CHARACTERS);
    }

    private static String getObfuscatedCredentials(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String username = sharedPref.getString(Constants.KEY_USERNAME, null);
        String password = sharedPref.getString(Constants.KEY_PASSWORD, null);
        if (username == null || password == null) {
            return null;
        }
        return obfuscate(String.format("%s:%s", username, password));
    }

    public static String buildDHISCheckSMS(Context context) {
        String credentials = getObfuscatedCredentials(context);
        if (credentials == null) {
            return  null;
        }
        return String.format("check %s", credentials);
    }

    public static String buildCompleteSMSText(Context context, String organisationUnitDhisId, String period) {
        String credentials = getObfuscatedCredentials(context);
        if (credentials == null) {
            return  null;
        }

        String body = buildDataSMSPart();
        String header = String.format("%1$s %2$s %3$s %4$s %5$s#",
                Config.get(JSONFormParser.KEY_KEYWORD),
                Config.get(JSONFormParser.KEY_VERSION),
                credentials,
                period,
                organisationUnitDhisId);
        String indexedHeader = "i:" + header;
        int headerLength = indexedHeader.length();
        int availableLength = Constants.MAX_CHARS_SINGLE_SMS - headerLength;
        if (DataValue.countNonNull() * 2 <= availableLength) {
            String indexedBody = buildIndexedDataSMSPart();
            if (indexedBody.length() <= availableLength) {
                return indexedHeader + indexedBody;
            }
        }
        return header + body;
    }

    public static void resetAllEntries() { DataValue.resetAll(); }

    public static String serializeOrganisationUnits(ArrayList<OrganisationUnit> organisationUnits) {
        String serialized = "";
        for (OrganisationUnit organisationUnit: organisationUnits) {
            serialized += organisationUnit.getDhisId();
            serialized += "|";
        }
        return serialized.substring(0, serialized.length() - 1);
    }

    public static ArrayList<OrganisationUnit> deserializeOrganisationUnits(String serialized) {
        ArrayList<OrganisationUnit> organisationUnits = new ArrayList<>();
        if (serialized != null && serialized.length() > 0) {
            String[] parts = serialized.split("\\|");
            for (String part : parts) {
                organisationUnits.add(OrganisationUnit.findByDHISId(part));
            }
        }
        return organisationUnits;
    }

    public static ArrayList<ArrayList<String>> getPeriodsArrayLists() {
        return getPeriodsArrayLists(3);
    }

    private static ArrayList<ArrayList<String>> getPeriodsArrayLists(int previousMonthNumber) {
        SimpleDateFormat codeFormat = new SimpleDateFormat("yyMM", Locale.ENGLISH);
        SimpleDateFormat labelFormat = new SimpleDateFormat("MMMM y", Locale.FRANCE);
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> codes = new ArrayList<>();
        for (int i=0; i<previousMonthNumber; i++) {
            calendar.add(Calendar.MONTH, -1);
            Date monthDate = calendar.getTime();
            codes.add(codeFormat.format(monthDate));
            labels.add(labelFormat.format(monthDate));
        }

        ArrayList<ArrayList<String>> periods = new ArrayList<>();
        periods.add(codes);
        periods.add(labels);

        return periods;
    }

    public static HashMap<String, String> getHashMapFor(ArrayList<String> keys, ArrayList<String> labels) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (keys.size() == labels.size()) {
            for (int i=0; i<keys.size(); i++) {
                hashMap.put(keys.get(i), labels.get(i));
            }
        }
        return hashMap;
    }

    public static String percentFormat(double number, boolean addPercentSign) {
        if (number <= 1 && addPercentSign) {
            number = number * 100;
        }
        String formatted = String.format(Locale.FRANCE, "%,.0f", number);
        if (addPercentSign) {
            return formatted + "%";
        }
        return formatted;
    }

    public static String numberFormat(Integer number) {
        if (number == null) {
            return "?";
        }
        return String.format(Locale.FRANCE, "%,d", number);
    }

    public static String cleanMsisdn(String msisdn) {
        return msisdn.replace(Constants.PHONE_PREFIX, "").replace("+", "");
    }

    public static void truncateTable(String tableName) {
        SugarRecord.executeQuery("DELETE FROM SQLITE_SEQUENCE WHERE NAME = ?;", tableName);
    }
}
