package com.example.smarthome;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.smarthome.Database.connections.AppDatabase;
import com.example.smarthome.Database.models.DailyEnergyRecordEntity;
import com.example.smarthome.Database.models.DeviceEnergyEntity;
import com.example.smarthome.Database.models.DeviceTimerTaskEntity;
import com.example.smarthome.Database.models.ModeConfigEntity;
import com.example.smarthome.Database.models.UserConfigEntity;
import com.example.smarthome.Database.queries.DailyEnergyRecordDao;
import com.example.smarthome.Database.queries.DeviceEnergyDao;
import com.example.smarthome.Database.queries.DeviceTimerTaskDao;
import com.example.smarthome.Database.queries.ModeConfigDao;
import com.example.smarthome.Database.queries.UserConfigDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    
    private AppDatabase database;
    private UserConfigDao userConfigDao;
    private DeviceEnergyDao deviceEnergyDao;
    private DailyEnergyRecordDao dailyEnergyRecordDao;
    private DeviceTimerTaskDao deviceTimerTaskDao;
    private ModeConfigDao modeConfigDao;
    
    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        userConfigDao = database.userConfigDao();
        deviceEnergyDao = database.deviceEnergyDao();
        dailyEnergyRecordDao = database.dailyEnergyRecordDao();
        deviceTimerTaskDao = database.deviceTimerTaskDao();
        modeConfigDao = database.modeConfigDao();
    }
    
    @After
    public void closeDb() {
        database.close();
    }
    
    @Test
    public void testUserConfigInsert() {
        UserConfigEntity config = new UserConfigEntity();
        config.setUserName("测试用户");
        config.setHomeName("测试家庭");
        config.setThemeMode("dark");
        
        long id = userConfigDao.insert(config);
        assertTrue(id > 0);
        
        UserConfigEntity retrieved = userConfigDao.getById(id);
        assertNotNull(retrieved);
        assertEquals("测试用户", retrieved.getUserName());
        assertEquals("测试家庭", retrieved.getHomeName());
        assertEquals("dark", retrieved.getThemeMode());
    }
    
    @Test
    public void testUserConfigUpdate() {
        UserConfigEntity config = new UserConfigEntity();
        config.setUserName("原始用户");
        long id = userConfigDao.insert(config);
        
        UserConfigEntity retrieved = userConfigDao.getById(id);
        retrieved.setUserName("更新用户");
        int rows = userConfigDao.update(retrieved);
        
        assertEquals(1, rows);
        UserConfigEntity updated = userConfigDao.getById(id);
        assertEquals("更新用户", updated.getUserName());
    }
    
    @Test
    public void testDeviceEnergyInsert() {
        DeviceEnergyEntity device = new DeviceEnergyEntity();
        device.setDeviceId("test_device_1");
        device.setDeviceName("测试设备");
        device.setPowerRating(60.0);
        device.setTotalEnergy(1.5);
        
        long id = deviceEnergyDao.insert(device);
        assertTrue(id > 0);
        
        DeviceEnergyEntity retrieved = deviceEnergyDao.getByDeviceId("test_device_1");
        assertNotNull(retrieved);
        assertEquals("测试设备", retrieved.getDeviceName());
        assertEquals(60.0, retrieved.getPowerRating(), 0.01);
        assertEquals(1.5, retrieved.getTotalEnergy(), 0.01);
    }
    
    @Test
    public void testDeviceEnergyUpdate() {
        DeviceEnergyEntity device = new DeviceEnergyEntity();
        device.setDeviceId("test_device_2");
        device.setDeviceName("测试设备2");
        device.setTotalEnergy(0.0);
        deviceEnergyDao.insert(device);
        
        int rows = deviceEnergyDao.addEnergyAndTime("test_device_2", 0.5, 3600000);
        assertEquals(1, rows);
        
        DeviceEnergyEntity updated = deviceEnergyDao.getByDeviceId("test_device_2");
        assertEquals(0.5, updated.getTotalEnergy(), 0.01);
        assertEquals(3600000, updated.getTotalRunningTime());
    }
    
    @Test
    public void testDeviceEnergyDelete() {
        DeviceEnergyEntity device = new DeviceEnergyEntity();
        device.setDeviceId("test_device_3");
        device.setDeviceName("待删除设备");
        deviceEnergyDao.insert(device);
        
        int rows = deviceEnergyDao.deleteByDeviceId("test_device_3");
        assertEquals(1, rows);
        
        DeviceEnergyEntity deleted = deviceEnergyDao.getByDeviceId("test_device_3");
        assertNull(deleted);
    }
    
    @Test
    public void testDeviceEnergyQuery() {
        DeviceEnergyEntity device1 = new DeviceEnergyEntity();
        device1.setDeviceId("device_1");
        device1.setTotalEnergy(2.0);
        deviceEnergyDao.insert(device1);
        
        DeviceEnergyEntity device2 = new DeviceEnergyEntity();
        device2.setDeviceId("device_2");
        device2.setTotalEnergy(1.0);
        deviceEnergyDao.insert(device2);
        
        List<DeviceEnergyEntity> sortedList = deviceEnergyDao.getAllOrderByEnergyDesc();
        assertEquals(2, sortedList.size());
        assertEquals("device_1", sortedList.get(0).getDeviceId());
    }
    
    @Test
    public void testDailyEnergyRecordInsert() {
        DailyEnergyRecordEntity record = new DailyEnergyRecordEntity();
        record.setRecordDate("2024-04-22");
        record.setTotalEnergy(3.5);
        
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("device_1", 2.0);
        breakdown.put("device_2", 1.5);
        record.setDeviceBreakdown(breakdown);
        
        long id = dailyEnergyRecordDao.insert(record);
        assertTrue(id > 0);
        
        DailyEnergyRecordEntity retrieved = dailyEnergyRecordDao.getByDate("2024-04-22");
        assertNotNull(retrieved);
        assertEquals(3.5, retrieved.getTotalEnergy(), 0.01);
        assertNotNull(retrieved.getDeviceBreakdown());
        assertEquals(2, retrieved.getDeviceBreakdown().size());
    }
    
    @Test
    public void testTimerTaskInsert() {
        DeviceEnergyEntity device = new DeviceEnergyEntity();
        device.setDeviceId("timer_device");
        device.setDeviceName("定时设备");
        deviceEnergyDao.insert(device);
        
        DeviceTimerTaskEntity task = new DeviceTimerTaskEntity();
        task.setTaskId("task_1");
        task.setDeviceId("timer_device");
        task.setTaskName("测试任务");
        task.setAction("on");
        task.setExecutionTime("18:30");
        task.setEnabled(true);
        
        long id = deviceTimerTaskDao.insert(task);
        assertTrue(id > 0);
        
        DeviceTimerTaskEntity retrieved = deviceTimerTaskDao.getByTaskId("task_1");
        assertNotNull(retrieved);
        assertEquals("测试任务", retrieved.getTaskName());
        assertEquals("on", retrieved.getAction());
        assertTrue(retrieved.isEnabled());
    }
    
    @Test
    public void testModeConfigInsert() {
        ModeConfigEntity mode = new ModeConfigEntity();
        mode.setModeName("回家模式");
        mode.setActive(true);
        
        Map<String, String> deviceConfig = new HashMap<>();
        deviceConfig.put("light_1", "on");
        deviceConfig.put("ac_1", "on");
        mode.setDeviceConfiguration(deviceConfig);
        
        long id = modeConfigDao.insert(mode);
        assertTrue(id > 0);
        
        ModeConfigEntity retrieved = modeConfigDao.getByModeName("回家模式");
        assertNotNull(retrieved);
        assertTrue(retrieved.isActive());
        assertNotNull(retrieved.getDeviceConfiguration());
        assertEquals(2, retrieved.getDeviceConfiguration().size());
    }
    
    @Test
    public void testModeActivation() {
        ModeConfigEntity mode1 = new ModeConfigEntity();
        mode1.setModeName("模式A");
        mode1.setActive(true);
        modeConfigDao.insert(mode1);
        
        ModeConfigEntity mode2 = new ModeConfigEntity();
        mode2.setModeName("模式B");
        mode2.setActive(false);
        modeConfigDao.insert(mode2);
        
        modeConfigDao.deactivateAll();
        int rows = modeConfigDao.activateMode("模式B");
        assertEquals(1, rows);
        
        ModeConfigEntity activeMode = modeConfigDao.getActiveMode();
        assertNotNull(activeMode);
        assertEquals("模式B", activeMode.getModeName());
    }
    
    @Test
    public void testDateRangeQuery() {
        DailyEnergyRecordEntity record1 = new DailyEnergyRecordEntity();
        record1.setRecordDate("2024-04-20");
        record1.setTotalEnergy(2.0);
        dailyEnergyRecordDao.insert(record1);
        
        DailyEnergyRecordEntity record2 = new DailyEnergyRecordEntity();
        record2.setRecordDate("2024-04-21");
        record2.setTotalEnergy(3.0);
        dailyEnergyRecordDao.insert(record2);
        
        DailyEnergyRecordEntity record3 = new DailyEnergyRecordEntity();
        record3.setRecordDate("2024-04-22");
        record3.setTotalEnergy(4.0);
        dailyEnergyRecordDao.insert(record3);
        
        List<DailyEnergyRecordEntity> records = dailyEnergyRecordDao.getByDateRange("2024-04-20", "2024-04-22");
        assertEquals(3, records.size());
        
        double totalEnergy = dailyEnergyRecordDao.getTotalEnergyByDateRange("2024-04-20", "2024-04-22");
        assertEquals(9.0, totalEnergy, 0.01);
    }
}
