package com.example.smarthome.Database.queries;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smarthome.Database.models.ModeConfigEntity;

import java.util.List;

@Dao
public interface ModeConfigDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ModeConfigEntity modeConfig);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ModeConfigEntity> modeConfigs);
    
    @Update
    int update(ModeConfigEntity modeConfig);
    
    @Delete
    int delete(ModeConfigEntity modeConfig);
    
    @Query("DELETE FROM mode_config WHERE modeName = :modeName")
    int deleteByModeName(String modeName);
    
    @Query("DELETE FROM mode_config")
    int deleteAll();
    
    @Query("SELECT * FROM mode_config WHERE id = :id")
    ModeConfigEntity getById(long id);
    
    @Query("SELECT * FROM mode_config WHERE modeName = :modeName")
    ModeConfigEntity getByModeName(String modeName);
    
    @Query("SELECT * FROM mode_config WHERE active = 1 LIMIT 1")
    ModeConfigEntity getActiveMode();
    
    @Query("SELECT * FROM mode_config ORDER BY createdAt DESC")
    List<ModeConfigEntity> getAllOrderByCreatedDesc();
    
    @Query("SELECT * FROM mode_config")
    List<ModeConfigEntity> getAll();
    
    @Query("SELECT COUNT(*) FROM mode_config")
    int getCount();
    
    @Query("SELECT COUNT(*) FROM mode_config WHERE active = 1")
    int getActiveCount();
    
    @Query("UPDATE mode_config SET active = 0")
    int deactivateAll();
    
    @Query("UPDATE mode_config SET active = 1 WHERE modeName = :modeName")
    int activateMode(String modeName);
    
    @Query("SELECT EXISTS(SELECT 1 FROM mode_config WHERE modeName = :modeName)")
    boolean existsByModeName(String modeName);
}
