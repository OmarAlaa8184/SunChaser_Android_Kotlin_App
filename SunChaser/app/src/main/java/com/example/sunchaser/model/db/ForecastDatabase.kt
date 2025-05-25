package com.example.sunchaser.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sunchaser.model.weatherPojo.Alert
import com.example.sunchaser.model.weatherPojo.ForecastEntity


@Database(entities = [ForecastEntity::class , Alert::class], version = 1,exportSchema = false)
abstract class ForecastDatabase:RoomDatabase()
{
    abstract fun forecastDao():ForecastDao

    abstract fun alertDao():AlertDao

    companion object DatabaseProvider {
        @Volatile
        private var INSTANCE: ForecastDatabase? = null

        fun getInstance(context: Context): ForecastDatabase
        {
            return INSTANCE ?: synchronized(this)
            {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ForecastDatabase::class.java,
                    "forecasts_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}