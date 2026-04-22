package com.example.smarthome.Database.queries;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smarthome.Database.models.DeviceTimerTaskEntity;

import java.util.List;

@Dao
public interface DeviceTimerTaskDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DeviceTimerTaskEntity task);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DeviceTimerTaskEntity> tasks);
    
    @Update
    int update(DeviceTimerTaskEntity task);
    
    @Delete
    int delete(DeviceTimerTaskEntity task);
    
    @Query("DELETE FROM device_timer_task WHERE taskId = :taskId")
    int deleteByTaskId(String taskId);
    
    @Query("DELETE FROM device_timer_task WHERE deviceId = :deviceId")
    int deleteByDeviceId(String deviceId);
    
    @Query("DELETE FROM device_timer_task")
    int deleteAll();
    
    @Query("SELECT * FROM device_timer_task WHERE id = :id")
    DeviceTimerTaskEntity getById(long id);
    
    @Query("SELECT * FROM device_timer_task WHERE taskId = :taskId")
    DeviceTimerTaskEntity getByTaskId(String taskId);
    
    @Query("SELECT * FROM device_timer_task WHERE deviceId = :deviceId")
    List<DeviceTimerTaskEntity> getByDeviceId(String deviceId);
    
    @Query("SELECT * FROM device_timer_task WHERE enabled = 1 ORDER BY executionTime ASC")
    List<DeviceTimerTaskEntity> getAllEnabled();
    
    @Query("SELECT * FROM device_timer_task ORDER BY executionTime ASC")
    List<DeviceTimerTaskEntity> getAllOrderByTime();
    
    @Query("SELECT * FROM device_timer_task")
    List<DeviceTimerTaskEntity> getAll();
    
    @Query("SELECT COUNT(*) FROM device_timer_task")
    int getCount();
    
    @Query("SELECT COUNT(*) FROM device_timer_task WHERE enabled = 1")
    int getEnabledCount();
    
    @Query("SELECT COUNT(*) FROM device_timer_task WHERE deviceId = :deviceId")
    int getCountByDeviceId(String deviceId);
    
    @Query("UPDATE device_timer_task SET enabled = :enabled WHERE taskId = :taskId")
    int updateEnabled(String taskId, boolean enabled);
    
    @Query("SELECT EXISTS(SELECT 1 FROM device_timer_task WHERE taskId = :taskId)")
    boolean existsByTaskId(String taskId);
    
    @Query("SELECT * FROM device_timer_task WHERE executionTime = :time AND enabled = 1")
    List<DeviceTimerTaskEntity> getTasksByExecutionTime(String time);
}
