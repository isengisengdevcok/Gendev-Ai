package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        UsageRecordEntity::class,
        PromptPresetEntity::class,
        ProjectFileEntity::class,
        GeminiModelEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GenDevDatabase : RoomDatabase() {
    abstract fun genDevDao(): GenDevDao

    companion object {
        @Volatile
        private var INSTANCE: GenDevDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `gemini_models` (
                        `id` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `tierName` TEXT NOT NULL,
                        `tagline` TEXT NOT NULL,
                        `contextWindowTokens` INTEGER NOT NULL,
                        `maxOutputTokens` INTEGER NOT NULL,
                        `inputPricePerMillionUsd` REAL NOT NULL,
                        `outputPricePerMillionUsd` REAL NOT NULL,
                        `recommendedFor` TEXT NOT NULL,
                        `isDynamic` INTEGER NOT NULL,
                        `supportsLegacySamplingParams` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): GenDevDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GenDevDatabase::class.java,
                    "gendev_ai_lab.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
