package com.aura.substratecryptotest.data.services

import android.content.Context
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.data.UserDocument
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Servicio para migrar documentos existentes al sistema de usuarios seguros
 * Convierte bitácoras de montañismo en documentos seguros por usuario
 */
class DocumentMigrationService(private val context: Context) {
    
    companion object {
        private const val TAG = "DocumentMigrationService"
    }
    
    private val secureUserRepository = SecureUserRepository.getInstance(context)
    private val userManager = UserManager(context)
    private val appDatabaseManager = AppDatabaseManager(context)
    
    /**
     * Migra todas las bitácoras existentes a documentos seguros
     * Asocia cada bitácora con un usuario específico
     */
    suspend fun migrateExistingLogbooksToSecureDocuments(): MigrationResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Iniciando migración de bitácoras a documentos seguros", "")
                
                // 1. Obtener todas las bitácoras existentes
                val mountaineeringRepository = appDatabaseManager.mountaineeringRepository
                val allLogbooksFlow = mountaineeringRepository.getAllLogbooks()
                
                // Recopilar las bitácoras del Flow
                var allLogbooks = emptyList<MountaineeringLogbook>()
                allLogbooksFlow.collect { logbooks ->
                    allLogbooks = logbooks
                }
                
                Logger.debug(TAG, "Bitácoras encontradas", "Cantidad: ${allLogbooks.size}")
                
                if (allLogbooks.isEmpty()) {
                    return@withContext MigrationResult.Success(
                        message = "No hay bitácoras para migrar",
                        migratedCount = 0,
                        totalCount = 0
                    )
                }
                
                // 2. Obtener usuarios registrados
                val registeredUsers = userManager.getRegisteredUsers()
                
                if (registeredUsers.isEmpty()) {
                    Logger.warning(TAG, "No hay usuarios registrados", "Creando usuario por defecto")
                    // Crear usuario por defecto para las bitácoras existentes
                    val defaultUserResult = userManager.registerNewUser("Usuario Principal", requireBiometric = false)
                    if (defaultUserResult !is UserManager.UserAuthResult.Success) {
                        return@withContext MigrationResult.Error("No se pudo crear usuario por defecto")
                    }
                }
                
                val users = userManager.getRegisteredUsers()
                val primaryUser = users.firstOrNull()
                
                if (primaryUser == null) {
                    return@withContext MigrationResult.Error("No se pudo obtener usuario principal")
                }
                
                // 3. Migrar cada bitácora a documento seguro
                var migratedCount = 0
                var errorCount = 0
                
                for (logbook in allLogbooks) {
                    try {
                        val migrationResult = migrateLogbookToSecureDocument(logbook, primaryUser.id)
                        if (migrationResult) {
                            migratedCount++
                            Logger.success(TAG, "Bitácora migrada", "ID: ${logbook.id}, Nombre: ${logbook.name}")
                        } else {
                            errorCount++
                            Logger.error(TAG, "Error migrando bitácora", "ID: ${logbook.id}", null)
                        }
                    } catch (e: Exception) {
                        errorCount++
                        Logger.error(TAG, "Excepción migrando bitácora", "ID: ${logbook.id}, Error: ${e.message}", e)
                    }
                }
                
                val result = MigrationResult.Success(
                    message = "Migración completada: $migratedCount exitosas, $errorCount errores",
                    migratedCount = migratedCount,
                    totalCount = allLogbooks.size
                )
                
                Logger.success(TAG, "Migración completada", "Exitosas: $migratedCount, Errores: $errorCount")
                return@withContext result
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error en migración", e.message ?: "Error desconocido", e)
                return@withContext MigrationResult.Error("Error en migración: ${e.message}")
            }
        }
    }
    
    /**
     * Migra una bitácora específica a documento seguro
     */
    private suspend fun migrateLogbookToSecureDocument(
        logbook: MountaineeringLogbook,
        userId: String
    ): Boolean {
        return try {
            // Crear documento seguro
            val documentResult = secureUserRepository.createUserDocument(
                walletId = "migration_wallet", // Wallet temporal para migración
                documentHash = "logbook_${logbook.id}_${System.currentTimeMillis()}",
                documentType = "mountaineering_logbook",
                blockchainTimestamp = null,
                metadata = mapOf(
                    "original_logbook_id" to logbook.id,
                    "logbook_name" to logbook.name,
                    "logbook_observations" to logbook.observations,
                    "is_completed" to logbook.isCompleted,
                    "created_at" to logbook.createdAt,
                    "migration_timestamp" to System.currentTimeMillis(),
                    "migration_source" to "mountaineering_logbook"
                )
            )
            
            documentResult.isSuccess
        } catch (e: Exception) {
            Logger.error(TAG, "Error migrando bitácora individual", "ID: ${logbook.id}, Error: ${e.message}", e)
            false
        }
    }
    
    /**
     * Obtiene estadísticas de migración
     */
    suspend fun getMigrationStats(): MigrationStats {
        return withContext(Dispatchers.IO) {
            try {
                val mountaineeringRepository = appDatabaseManager.mountaineeringRepository
                val allLogbooksFlow = mountaineeringRepository.getAllLogbooks()
                
                // Recopilar las bitácoras del Flow
                var allLogbooks = emptyList<MountaineeringLogbook>()
                allLogbooksFlow.collect { logbooks ->
                    allLogbooks = logbooks
                }
                
                val registeredUsers = userManager.getRegisteredUsers()
                
                // Contar documentos seguros existentes
                var secureDocumentsCount = 0
                for (user in registeredUsers) {
                    val userWallets = secureUserRepository.getUserWallets()
                    // Por ahora usar wallets como proxy, después implementar conteo real de documentos
                    secureDocumentsCount += userWallets.size
                }
                
                MigrationStats(
                    totalLogbooks = allLogbooks.size,
                    totalUsers = registeredUsers.size,
                    secureDocumentsCount = secureDocumentsCount,
                    migrationNeeded = allLogbooks.isNotEmpty() && secureDocumentsCount == 0
                )
            } catch (e: Exception) {
                Logger.error(TAG, "Error obteniendo estadísticas", e.message ?: "Error desconocido", e)
                MigrationStats(0, 0, 0, false)
            }
        }
    }
    
    /**
     * Resultado de migración
     */
    sealed class MigrationResult {
        data class Success(
            val message: String,
            val migratedCount: Int,
            val totalCount: Int
        ) : MigrationResult()
        
        data class Error(
            val message: String
        ) : MigrationResult()
    }
    
    /**
     * Estadísticas de migración
     */
    data class MigrationStats(
        val totalLogbooks: Int,
        val totalUsers: Int,
        val secureDocumentsCount: Int,
        val migrationNeeded: Boolean
    )
}
