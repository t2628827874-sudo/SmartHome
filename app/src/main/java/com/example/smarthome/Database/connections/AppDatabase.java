package com.example.smarthome.Database.connections;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

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
import com.example.smarthome.Database.utils.Converters;

@Database(
    entities = {
        UserConfigEntity.class,
        DeviceEnergyEntity.class,
        DailyEnergyRecordEntity.class,
        DeviceTimerTaskEntity.class,
        ModeConfigEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "smart_home_db";
    private static volatile AppDatabase instance;
    
    public abstract UserConfigDao userConfigDao();
    public abstract DeviceEnergyDao deviceEnergyDao();
    public abstract DailyEnergyRecordDao dailyEnergyRecordDao();
    public abstract DeviceTimerTaskDao deviceTimerTaskDao();
    public abstract ModeConfigDao modeConfigDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = createDatabase(context);
                }
            }
        }
        return instance;
    }
    
    private static AppDatabase createDatabase(Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
    }
    
    public static void destroyInstance() {
        if (instance != null) {
            instance = null;
        }
    }
}
