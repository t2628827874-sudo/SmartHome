package com.example.smarthome.Database.queries;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smarthome.Database.models.DeviceEnergyEntity;

import java.util.List;

@Dao
public interface DeviceEnergyDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DeviceEnergyEntity deviceEnergy);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DeviceEnergyEntity> deviceEnergies);
    
    @Update
    int update(DeviceEnergyEntity deviceEnergy);
    
    @Delete
    int delete(DeviceEnergyEntity deviceEnergy);
    
    @Query("DELETE FROM device_energy WHERE deviceId = :deviceId")
    int deleteByDeviceId(String deviceId);
    
    @Query("DELETE FROM device_energy")
    int deleteAll();
    
    @Query("SELECT * FROM device_energy WHERE id = :id")
    DeviceEnergyEntity getById(long id);
    
    @Query("SELECT * FROM device_energy WHERE deviceId = :deviceId")
    DeviceEnergyEntity getByDeviceId(String deviceId);
    
    @Query("SELECT * FROM device_energy ORDER BY totalEnergy DESC")
    List<DeviceEnergyEntity> getAllOrderByEnergyDesc();
    
    @Query("SELECT * FROM device_energy WHERE running = 1")
    List<DeviceEnergyEntity> getAllRunning();
    
    @Query("SELECT * FROM device_energy")
    List<DeviceEnergyEntity> getAll();
    
    @Query("SELECT COUNT(*) FROM device_energy")
    int getCount();
    
    @Query("SELECT COUNT(*) FROM device_energy WHERE running = 1")
    int getRunningCount();
    
    @Query("SELECT SUM(totalEnergy) FROM device_energy")
    double getTotalEnergy();
    
    @Query("UPDATE device_energy SET running = :running, startTime = :startTime WHERE deviceId = :deviceId")
    int updateRunningStatus(String deviceId, boolean running, long startTime);
    
    @Query("UPDATE device_energy SET totalEnergy = totalEnergy + :energy, totalRunningTime = totalRunningTime + :runningTime WHERE deviceId = :deviceId")
    int addEnergyAndTime(String deviceId, double energy, long runningTime);
    
    @Query("SELECT EXISTS(SELECT 1 FROM device_energy WHERE deviceId = :deviceId)")
    boolean existsByDeviceId(String deviceId);
}
