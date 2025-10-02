package com.aura.substratecryptotest.data.user

import android.content.Context
import android.util.Log
import com.aura.substratecryptotest.data.mountaineering.MountaineeringRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio completo para migrar bitácoras existentes al sistema de usuarios
 */
class CompleteLogbookMigrationService(
    private val context: Context,
    private val userManagementService: UserManagementService,
    private val mountaineeringRepository: MountaineeringRepository
) {
    
    companion object {
        private const val TAG = "CompleteLogbookMigrationService"
        private const val PREFS_NAME = "complete_migration_prefs"
        private const val KEY_MIGRATION_COMPLETED = "complete_migration_completed"
    }
    
    /**
     * Ejecuta la migración completa si no se ha completado antes
     */
    suspend fun runCompleteMigrationIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val migrationCompleted = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
                
                if (migrationCompleted) {
                    Log.i(TAG, "Migración completa ya realizada anteriormente")
                    return@withContext true
                }
                
                Log.i(TAG, "Iniciando migración completa de bitácoras existentes...")
                val success = migrateAllExistingLogbooks()
                
                if (success) {
                    prefs.edit().putBoolean(KEY_MIGRATION_COMPLETED, true).apply()
                    Log.i(TAG, "Migración completa exitosa")
                } else {
                    Log.e(TAG, "Migración completa falló")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la migración completa: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Migra todas las bitácoras existentes al sistema de usuarios
     */
    private suspend fun migrateAllExistingLogbooks(): Boolean {
        return try {
            // Obtener todas las bitácoras existentes
            val existingLogbooks = mountaineeringRepository.getAllLogbooks().first()
            Log.i(TAG, "Bitácoras existentes encontradas: ${existingLogbooks.size}")
            
            if (existingLogbooks.isEmpty()) {
                Log.i(TAG, "No hay bitácoras existentes para migrar")
                return true
            }
            
            // Obtener todos los usuarios disponibles
            val users = userManagementService.getAllActiveUsers().first()
            Log.i(TAG, "Usuarios disponibles: ${users.size}")
            
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
            
            // Asociar cada bitácora existente con el usuario objetivo
            var successCount = 0
            var errorCount = 0
            
            existingLogbooks.forEach { logbook ->
                try {
                    val success = userManagementService.associateLogbookWithCurrentUser(
                        logbookId = logbook.id,
                        role = LogbookRole.OWNER
                    )
                    
                    if (success) {
                        successCount++
                        Log.d(TAG, "Bitácora '${logbook.name}' (ID: ${logbook.id}) asociada con usuario ${targetUser.name}")
                    } else {
                        errorCount++
                        Log.w(TAG, "No se pudo asociar bitácora '${logbook.name}' (ID: ${logbook.id})")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Error asociando bitácora '${logbook.name}' (ID: ${logbook.id}): ${e.message}", e)
                }
            }
            
            Log.i(TAG, "Migración completada: $successCount exitosas, $errorCount errores")
            
            // Considerar exitosa si al menos la mitad de las bitácoras se migraron
            val totalLogbooks = existingLogbooks.size
            val successRate = successCount.toFloat() / totalLogbooks
            val isSuccessful = successRate >= 0.5f
            
            if (isSuccessful) {
                Log.i(TAG, "Migración exitosa (${(successRate * 100).toInt()}% de bitácoras migradas)")
            } else {
                Log.w(TAG, "Migración parcialmente exitosa (${(successRate * 100).toInt()}% de bitácoras migradas)")
            }
            
            isSuccessful
            
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la migración de bitácoras: ${e.message}", e)
            false
        }
    }
    
    /**
     * Obtiene estadísticas detalladas de la migración
     */
    suspend fun getDetailedMigrationStats(): DetailedMigrationStats {
        val users = userManagementService.getAllActiveUsers().first()
        val systemStats = userManagementService.getSystemStats()
        val existingLogbooks = mountaineeringRepository.getAllLogbooks().first()
        
        // Contar bitácoras asociadas
        var associatedLogbooks = 0
        users.forEach { user ->
            val userLogbooks = userManagementService.userRepository.getLogbooksByUser(user.id).first()
            associatedLogbooks += userLogbooks.size
        }
        
        return DetailedMigrationStats(
            totalUsers = users.size,
            usersWithBiometric = systemStats.usersWithBiometric,
            usersWithWallet = systemStats.usersWithWallet,
            usersWithDID = systemStats.usersWithDID,
            totalLogbooks = existingLogbooks.size,
            associatedLogbooks = associatedLogbooks,
            unassociatedLogbooks = existingLogbooks.size - associatedLogbooks,
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
        Log.i(TAG, "Migración completa reiniciada")
    }
    
    /**
     * Asocia bitácoras no asociadas con el usuario actual
     */
    suspend fun associateUnassociatedLogbooks(): Boolean {
        return try {
            val currentUser = userManagementService.getCurrentUser()
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario actual para asociar bitácoras")
                return false
            }
            
            val allLogbooks = mountaineeringRepository.getAllLogbooks().first()
            val userLogbooks = userManagementService.userRepository.getLogbooksByUser(currentUser.id).first()
            val userLogbookIds = userLogbooks.map { it.logbookId }.toSet()
            
            val unassociatedLogbooks = allLogbooks.filter { it.id !in userLogbookIds }
            Log.i(TAG, "Bitácoras no asociadas encontradas: ${unassociatedLogbooks.size}")
            
            var successCount = 0
            unassociatedLogbooks.forEach { logbook ->
                try {
                    val success = userManagementService.associateLogbookWithCurrentUser(
                        logbookId = logbook.id,
                        role = LogbookRole.OWNER
                    )
                    if (success) {
                        successCount++
                        Log.d(TAG, "Bitácora '${logbook.name}' asociada con usuario ${currentUser.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error asociando bitácora '${logbook.name}': ${e.message}", e)
                }
            }
            
            Log.i(TAG, "Asociación completada: $successCount bitácoras asociadas")
            successCount > 0
            
        } catch (e: Exception) {
            Log.e(TAG, "Error asociando bitácoras no asociadas: ${e.message}", e)
            false
        }
    }
}

/**
 * Estadísticas detalladas de la migración
 */
data class DetailedMigrationStats(
    val totalUsers: Int,
    val usersWithBiometric: Int,
    val usersWithWallet: Int,
    val usersWithDID: Int,
    val totalLogbooks: Int,
    val associatedLogbooks: Int,
    val unassociatedLogbooks: Int,
    val migrationCompleted: Boolean
)
