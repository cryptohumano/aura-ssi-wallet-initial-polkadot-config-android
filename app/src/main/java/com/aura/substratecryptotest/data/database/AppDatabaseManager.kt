package com.aura.substratecryptotest.data.database

import android.content.Context
import com.aura.substratecryptotest.data.mountaineering.MountaineeringRepository
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbookDao
import com.aura.substratecryptotest.data.mountaineering.ExpeditionMilestoneDao
import com.aura.substratecryptotest.data.mountaineering.ExpeditionPhotoDao
import com.aura.substratecryptotest.data.user.UserRepository
import com.aura.substratecryptotest.data.user.UserDao
import com.aura.substratecryptotest.data.user.UserLogbookDao
import com.aura.substratecryptotest.data.user.UserSessionDao
import com.aura.substratecryptotest.data.user.UserManagementService
import com.aura.substratecryptotest.data.user.CompleteLogbookMigrationService

/**
 * Manager para la base de datos principal de la aplicación
 */
class AppDatabaseManager(private val context: Context) {
    
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }
    
    // ===== DAOs =====
    
    private val logbookDao: MountaineeringLogbookDao by lazy { database.logbookDao() }
    private val milestoneDao: ExpeditionMilestoneDao by lazy { database.milestoneDao() }
    private val photoDao: ExpeditionPhotoDao by lazy { database.photoDao() }
    
    private val userDao: UserDao by lazy { database.userDao() }
    private val userLogbookDao: UserLogbookDao by lazy { database.userLogbookDao() }
    private val userSessionDao: UserSessionDao by lazy { database.userSessionDao() }
    
    // ===== REPOSITORIES =====
    
    val mountaineeringRepository: MountaineeringRepository by lazy {
        MountaineeringRepository(logbookDao, milestoneDao, photoDao)
    }
    
    val userRepository: UserRepository by lazy {
        UserRepository(userDao, userLogbookDao, userSessionDao)
    }
    
    val userManagementService: UserManagementService by lazy {
        UserManagementService(userRepository, context)
    }
    
    val migrationService: CompleteLogbookMigrationService by lazy {
        CompleteLogbookMigrationService(context, userManagementService, mountaineeringRepository)
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Cierra la base de datos
     */
    fun close() {
        database.close()
    }
    
    /**
     * Verifica si la base de datos está abierta
     */
    fun isOpen(): Boolean {
        return database.isOpen
    }
    
    /**
     * Obtiene el nombre de la base de datos
     */
    fun getDatabaseName(): String {
        return database.openHelper.databaseName ?: "app_database"
    }
}
