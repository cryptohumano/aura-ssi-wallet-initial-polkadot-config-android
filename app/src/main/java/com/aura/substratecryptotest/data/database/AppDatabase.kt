package com.aura.substratecryptotest.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook
import com.aura.substratecryptotest.data.mountaineering.ExpeditionMilestone
import com.aura.substratecryptotest.data.mountaineering.ExpeditionPhoto
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbookDao
import com.aura.substratecryptotest.data.mountaineering.ExpeditionMilestoneDao
import com.aura.substratecryptotest.data.mountaineering.ExpeditionPhotoDao
import com.aura.substratecryptotest.data.user.User
import com.aura.substratecryptotest.data.user.UserLogbook
import com.aura.substratecryptotest.data.user.UserSession
import com.aura.substratecryptotest.data.user.UserDao
import com.aura.substratecryptotest.data.user.UserLogbookDao
import com.aura.substratecryptotest.data.user.UserSessionDao
import com.aura.substratecryptotest.data.mountaineering.converters.DateConverter
import com.aura.substratecryptotest.data.mountaineering.converters.StringListConverter

/**
 * Base de datos principal que incluye tanto montañismo como gestión de usuarios
 */
@Database(
    entities = [
        // Entidades de montañismo
        MountaineeringLogbook::class,
        ExpeditionMilestone::class,
        ExpeditionPhoto::class,
        // Entidades de usuario
        User::class,
        UserLogbook::class,
        UserSession::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class, StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAOs de montañismo
    abstract fun logbookDao(): MountaineeringLogbookDao
    abstract fun milestoneDao(): ExpeditionMilestoneDao
    abstract fun photoDao(): ExpeditionPhotoDao
    
    // DAOs de usuario
    abstract fun userDao(): UserDao
    abstract fun userLogbookDao(): UserLogbookDao
    abstract fun userSessionDao(): UserSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(
                    MIGRATION_3_4,
                    MIGRATION_4_5
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migración de la versión 3 a 4 (agregar tablas de usuario)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla de usuarios
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT,
                        walletAddress TEXT,
                        did TEXT,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        biometricEnabled INTEGER NOT NULL DEFAULT 0,
                        lastLogin INTEGER
                    )
                """)
                
                // Crear tabla de asociaciones usuario-bitácora
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_logbooks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        logbookId INTEGER NOT NULL,
                        role TEXT NOT NULL DEFAULT 'OWNER',
                        createdAt INTEGER NOT NULL,
                        canEdit INTEGER NOT NULL DEFAULT 1,
                        canDelete INTEGER NOT NULL DEFAULT 0,
                        canExport INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(logbookId) REFERENCES mountaineering_logbooks(id) ON DELETE CASCADE
                    )
                """)
                
                // Crear tabla de sesiones de usuario
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        sessionToken TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        biometricVerified INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                    )
                """)
                
                // Crear índices para mejorar rendimiento
                database.execSQL("CREATE INDEX IF NOT EXISTS index_users_walletAddress ON users(walletAddress)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_users_did ON users(did)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_logbooks_userId ON user_logbooks(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_logbooks_logbookId ON user_logbooks(logbookId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_sessions_userId ON user_sessions(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_sessions_token ON user_sessions(sessionToken)")
            }
        }
        
        // Migración de la versión 4 a 5 (agregar campo profileImagePath)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar campo profileImagePath a la tabla users
                database.execSQL("ALTER TABLE users ADD COLUMN profileImagePath TEXT")
            }
        }
    }
}
