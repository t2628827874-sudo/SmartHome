package com.example.smarthome.Database.queries;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smarthome.Database.models.UserConfigEntity;

import java.util.List;

@Dao
public interface UserConfigDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserConfigEntity userConfig);
    
    @Update
    int update(UserConfigEntity userConfig);
    
    @Delete
    int delete(UserConfigEntity userConfig);
    
    @Query("DELETE FROM user_config")
    int deleteAll();
    
    @Query("SELECT * FROM user_config WHERE id = :id")
    UserConfigEntity getById(long id);
    
    @Query("SELECT * FROM user_config WHERE userName = :userName")
    UserConfigEntity getByUserName(String userName);
    
    @Query("SELECT * FROM user_config LIMIT 1")
    UserConfigEntity getFirst();
    
    @Query("SELECT * FROM user_config")
    List<UserConfigEntity> getAll();
    
    @Query("SELECT COUNT(*) FROM user_config")
    int getCount();
    
    @Query("UPDATE user_config SET themeMode = :themeMode WHERE id = :id")
    int updateThemeMode(long id, String themeMode);
    
    @Query("UPDATE user_config SET notificationEnabled = :enabled WHERE id = :id")
    int updateNotificationEnabled(long id, boolean enabled);
    
    @Query("UPDATE user_config SET language = :language WHERE id = :id")
    int updateLanguage(long id, String language);
    
    @Query("UPDATE user_config SET homeName = :homeName WHERE id = :id")
    int updateHomeName(long id, String homeName);
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_config WHERE id = :id)")
    boolean existsById(long id);
}
