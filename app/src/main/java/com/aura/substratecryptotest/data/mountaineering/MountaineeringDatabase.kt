package com.aura.substratecryptotest.data.mountaineering

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aura.substratecryptotest.data.mountaineering.converters.DateConverter
import com.aura.substratecryptotest.data.mountaineering.converters.StringListConverter

/**
 * Base de datos Room para el módulo de alpinismo
 */
@Database(
    entities = [
        MountaineeringLogbook::class,
        ExpeditionMilestone::class,
        ExpeditionPhoto::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class, StringListConverter::class)
abstract class MountaineeringDatabase : RoomDatabase() {
    
    abstract fun logbookDao(): MountaineeringLogbookDao
    abstract fun milestoneDao(): ExpeditionMilestoneDao
    abstract fun photoDao(): ExpeditionPhotoDao
    
    companion object {
        @Volatile
        private var INSTANCE: MountaineeringDatabase? = null
        
        // Migración de versión 1 a 2: agregar campo isDraft
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Verificar si la columna ya existe antes de agregarla
                val cursor = database.query("PRAGMA table_info(expedition_milestones)")
                var columnExists = false
                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (columnName == "isDraft") {
                        columnExists = true
                        break
                    }
                }
                cursor.close()
                
                if (!columnExists) {
                    database.execSQL("ALTER TABLE expedition_milestones ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
                }
            }
        }
        
        // Migración de versión 2 a 3: no hacer nada (la columna ya existe)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No hacer nada, la columna isDraft ya existe
            }
        }
        
        fun getDatabase(context: Context): MountaineeringDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MountaineeringDatabase::class.java,
                    "mountaineering_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
