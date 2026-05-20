package com.example.promptbooks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Record::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE records ADD COLUMN type TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN currency TEXT DEFAULT 'AED'")
                database.execSQL("ALTER TABLE records ADD COLUMN counterpartyName TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN counterpartyType TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN paymentMode TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE records ADD COLUMN referenceNumber TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN vatApplicable INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE records ADD COLUMN vatRate REAL")
                database.execSQL("ALTER TABLE records ADD COLUMN vatAmount REAL")
                database.execSQL("ALTER TABLE records ADD COLUMN taxCode TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN createdAt TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN updatedAt TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN source TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN location TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN notes TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN attachmentUri TEXT")
                database.execSQL("ALTER TABLE records ADD COLUMN attachmentType TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "promptbooks_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
