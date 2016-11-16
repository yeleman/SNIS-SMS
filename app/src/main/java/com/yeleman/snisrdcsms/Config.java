package com.yeleman.snisrdcsms;

import android.support.annotation.NonNull;

import com.orm.SugarRecord;
import com.orm.query.Select;

public class Config extends SugarRecord {

    private String key;
    private String value;

    public Config(){}

    public Config(@NonNull String key, @NonNull String value) {
        setKey(key);
        setValue(value);
    }

    public static Boolean exists(String key) {
        String[] params = new String[]{key};
        return Config.count(Config.class, "KEY = ? ", params) > 0;
    }

    private static Long create(String key, String value) {
        Config config = new Config(key, value);
        return config.save();
    }

    private void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    private void setValue(String value) {
        this.value = value;
    }

    private String getValue() {
        return value;
    }

    private static Config getFor(String key) {
        String[] params = new String[]{key};
        return Select.from(Config.class).where("KEY = ?", params).limit("1").first();
    }

    public static String get(String key) {
        return Config.getFor(key).getValue();
    }

    public static String getOrNull(String key) {
        if (!Config.exists(key)) {
            return null;
        }
        return Config.get(key);
    }

    public static void set(String key, String value) {
        if (Config.exists(key)) {
            Config config = Config.getFor(key);
            config.setValue(value);
            config.save();
        } else {
            Config.create(key, value);
        }

    }
}
