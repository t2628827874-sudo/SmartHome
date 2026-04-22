package com.example.smarthome.Database.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_config", indices = {@Index(value = "userName", unique = true)})
public class UserConfigEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String userName;
    
    private String homeName;
    
    private String themeMode;
    
    private String language;
    
    private boolean notificationEnabled;
    
    private boolean autoSync;
    
    private long createdAt;
    
    private long updatedAt;
    
    public UserConfigEntity() {
        this.themeMode = "light";
        this.language = "zh";
        this.notificationEnabled = true;
        this.autoSync = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getHomeName() {
        return homeName;
    }
    
    public void setHomeName(String homeName) {
        this.homeName = homeName;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getThemeMode() {
        return themeMode;
    }
    
    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }
    
    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isAutoSync() {
        return autoSync;
    }
    
    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
