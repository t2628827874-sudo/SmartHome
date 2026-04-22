package com.example.smarthome.Database.queries;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smarthome.Database.models.DailyEnergyRecordEntity;

import java.util.List;

@Dao
public interface DailyEnergyRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DailyEnergyRecordEntity record);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DailyEnergyRecordEntity> records);
    
    @Update
    int update(DailyEnergyRecordEntity record);
    
    @Delete
    int delete(DailyEnergyRecordEntity record);
    
    @Query("DELETE FROM daily_energy_record WHERE recordDate = :date")
    int deleteByDate(String date);
    
    @Query("DELETE FROM daily_energy_record")
    int deleteAll();
    
    @Query("SELECT * FROM daily_energy_record WHERE id = :id")
    DailyEnergyRecordEntity getById(long id);
    
    @Query("SELECT * FROM daily_energy_record WHERE recordDate = :date")
    DailyEnergyRecordEntity getByDate(String date);
    
    @Query("SELECT * FROM daily_energy_record ORDER BY recordDate DESC")
    List<DailyEnergyRecordEntity> getAllOrderByDateDesc();
    
    @Query("SELECT * FROM daily_energy_record WHERE recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC")
    List<DailyEnergyRecordEntity> getByDateRange(String startDate, String endDate);
    
    @Query("SELECT * FROM daily_energy_record")
    List<DailyEnergyRecordEntity> getAll();
    
    @Query("SELECT COUNT(*) FROM daily_energy_record")
    int getCount();
    
    @Query("SELECT SUM(totalEnergy) FROM daily_energy_record WHERE recordDate BETWEEN :startDate AND :endDate")
    double getTotalEnergyByDateRange(String startDate, String endDate);
    
    @Query("SELECT AVG(totalEnergy) FROM daily_energy_record")
    double getAverageEnergy();
    
    @Query("SELECT EXISTS(SELECT 1 FROM daily_energy_record WHERE recordDate = :date)")
    boolean existsByDate(String date);
    
    @Query("SELECT MAX(totalEnergy) FROM daily_energy_record")
    double getMaxEnergy();
    
    @Query("SELECT MIN(totalEnergy) FROM daily_energy_record WHERE totalEnergy > 0")
    double getMinEnergy();
}
