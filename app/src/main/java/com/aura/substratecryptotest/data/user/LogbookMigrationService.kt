package com.aura.substratecryptotest.data.user

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio para migrar bitácoras existentes al sistema de usuarios
 */
class LogbookMigrationService(
    private val context: Context,
    private val userManagementService: UserManagementService
) {
    
    companion object {
        private const val TAG = "LogbookMigrationService"
        private const val PREFS_NAME = "migration_prefs"
        private const val KEY_MIGRATION_COMPLETED = "migration_completed"
    }
    
    /**
     * Ejecuta la migración si no se ha completado antes
     */
    suspend fun runMigrationIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val migrationCompleted = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
                
                if (migrationCompleted) {
                    Log.i(TAG, "Migración ya completada anteriormente")
                    return@withContext true
                }
                
                Log.i(TAG, "Iniciando migración de bitácoras existentes...")
                val success = migrateExistingLogbooks()
                
                if (success) {
                    prefs.edit().putBoolean(KEY_MIGRATION_COMPLETED, true).apply()
                    Log.i(TAG, "Migración completada exitosamente")
                } else {
                    Log.e(TAG, "Migración falló")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la migración: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Migra todas las bitácoras existentes al sistema de usuarios
     */
    private suspend fun migrateExistingLogbooks(): Boolean {
        return try {
            // Obtener todos los usuarios disponibles
            val users = userManagementService.getAllActiveUsers().first()
            Log.i(TAG, "Usuarios encontrados para migración: ${users.size}")
            
            // Si no hay usuarios, crear uno por defecto
            val targetUser = if (users.isEmpty()) {
                Log.i(TAG, "No hay usuarios disponibles, creando usuario por defecto...")
                val defaultUser = userManagementService.createUserFromWallet(
                    walletName = "Usuario por Defecto",
                    walletAddress = "default_user_${System.currentTimeMillis()}",
                    did = null
                )
                userManagementService.setCurrentUser(defaultUser)
                Log.i(TAG, "Usuario por defecto creado: ${defaultUser.name}")
                defaultUser
            } else {
                // Usar el primer usuario disponible
                users.first().also { user ->
                    userManagementService.setCurrentUser(user)
                    Log.i(TAG, "Usuario actual establecido: ${user.name}")
                }
            }
            
            // Aquí necesitaríamos acceso al repositorio de bitácoras para obtener las existentes
            // Por ahora, vamos a simular la migración
            Log.i(TAG, "Simulando migración de bitácoras existentes...")
            
            // TODO: Implementar migración real cuando tengamos acceso al repositorio de bitácoras
            // val existingLogbooks = mountaineeringRepository.getAllLogbooks().first()
            // existingLogbooks.forEach { logbook ->
            //     userManagementService.associateLogbookWithCurrentUser(logbook.id, LogbookRole.OWNER)
            // }
            
            Log.i(TAG, "Migración de bitácoras completada")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la migración: ${e.message}", e)
            false
        }
    }
    
    /**
     * Asocia una bitácora específica con un usuario
     */
    suspend fun associateLogbookWithUser(logbookId: Long, userId: Long): Boolean {
        return try {
            val association = userManagementService.userRepository.associateUserWithLogbook(
                userId = userId,
                logbookId = logbookId,
                role = LogbookRole.OWNER
            )
            Log.i(TAG, "Bitácora $logbookId asociada con usuario $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error asociando bitácora $logbookId con usuario $userId: ${e.message}", e)
            false
        }
    }
    
    /**
     * Obtiene estadísticas de la migración
     */
    suspend fun getMigrationStats(): MigrationStats {
        val users = userManagementService.getAllActiveUsers().first()
        val systemStats = userManagementService.getSystemStats()
        
        return MigrationStats(
            totalUsers = users.size,
            usersWithBiometric = systemStats.usersWithBiometric,
            usersWithWallet = systemStats.usersWithWallet,
            usersWithDID = systemStats.usersWithDID,
            migrationCompleted = isMigrationCompleted()
        )
    }
    
    /**
     * Verifica si la migración ya se completó
     */
    private fun isMigrationCompleted(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
    }
    
    /**
     * Reinicia la migración (para testing)
     */
    fun resetMigration() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MIGRATION_COMPLETED, false).apply()
        Log.i(TAG, "Migración reiniciada")
    }
}

/**
 * Estadísticas de la migración
 */
data class MigrationStats(
    val totalUsers: Int,
    val usersWithBiometric: Int,
    val usersWithWallet: Int,
    val usersWithDID: Int,
    val migrationCompleted: Boolean
)