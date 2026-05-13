package com.klentahn.plexyaudiobooks.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [BookEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context, settingsManager: SettingsManager): AppDatabase {
            return Instance ?: synchronized(this) {
                val factory = SupportOpenHelperFactory(settingsManager.getDatabasePassphrase())
                Room.databaseBuilder(context, AppDatabase::class.java, "plexy_database")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
