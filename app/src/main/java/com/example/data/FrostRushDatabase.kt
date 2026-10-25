package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PlayerProfileEntity::class], version = 1, exportSchema = false)
abstract class FrostRushDatabase : RoomDatabase() {
    abstract fun playerProfileDao(): PlayerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: FrostRushDatabase? = null

        fun getInstance(context: Context): FrostRushDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FrostRushDatabase::class.java,
                    "frost_rush_3d.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
