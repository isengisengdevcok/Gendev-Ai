package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        UsageRecordEntity::class,
        PromptPresetEntity::class,
        ProjectFileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GenDevDatabase : RoomDatabase() {
    abstract fun genDevDao(): GenDevDao

    companion object {
        @Volatile
        private var INSTANCE: GenDevDatabase? = null

        fun getInstance(context: Context): GenDevDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GenDevDatabase::class.java,
                    "gendev_ai_lab.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
