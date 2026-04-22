package com.example.smarthome.Database.utils;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class Converters {
    
    private static final Gson gson = new Gson();
    
    @TypeConverter
    public static Map<String, Double> fromStringToDoubleMap(String value) {
        if (value == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, Double>>() {}.getType();
        return gson.fromJson(value, mapType);
    }
    
    @TypeConverter
    public static String fromDoubleMapToString(Map<String, Double> map) {
        if (map == null) {
            return null;
        }
        return gson.toJson(map);
    }
    
    @TypeConverter
    public static Map<String, String> fromStringToStringMap(String value) {
        if (value == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(value, mapType);
    }
    
    @TypeConverter
    public static String fromStringMapToString(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        return gson.toJson(map);
    }
}
