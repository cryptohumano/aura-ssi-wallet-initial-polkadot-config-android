package com.aura.substratecryptotest.data.services

import android.content.Context
import android.content.SharedPreferences
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.UserWallet
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Servicio para migrar usuarios desde el sistema anterior (SecureKeyStorage)
 * al nuevo sistema (SecureUserRepository + UserManager)
 */
class LegacyUserMigrationService(private val context: Context) {
    
    companion object {
        private const val TAG = "LegacyUserMigrationService"
        
        // Claves del sistema anterior
        private const val LEGACY_USER_PREFS = "secure_user_data"
        private const val LEGACY_WALLET_PREFS = "wallet_data"
        private const val LEGACY_DID_PREFS = "did_data"
        
        // Claves específicas que podrían existir
        private const val LEGACY_USER_NAME_KEY = "user_name"
        private const val LEGACY_USER_ID_KEY = "user_id"
        private const val LEGACY_WALLET_ADDRESS_KEY = "wallet_address"
        private const val LEGACY_DID_KEY = "did"
        private const val LEGACY_MNEMONIC_KEY = "mnemonic"
        private const val LEGACY_PRIVATE_KEY_KEY = "private_key"
    }
    
    private val secureUserRepository = SecureUserRepository.getInstance(context)
    private val userManager = UserManager(context)
    
    /**
     * Busca y migra datos de usuario del sistema anterior
     */
    suspend fun migrateLegacyUserData(): MigrationResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Iniciando migración de datos de usuario anterior", "")
                
                // 1. Buscar datos en SharedPreferences del sistema anterior
                val legacyData = findLegacyUserData()
                
                if (legacyData == null) {
                    Logger.debug(TAG, "No se encontraron datos de usuario anterior", "")
                    return@withContext MigrationResult.NoDataFound
                }
                
                Logger.success(TAG, "Datos de usuario anterior encontrados", "Nombre: ${legacyData.name}")
                
                // 2. Crear usuario en el nuevo sistema
                val newUserResult = createUserInNewSystem(legacyData)
                
                if (newUserResult !is UserManager.UserAuthResult.Success) {
                    Logger.error(TAG, "Error creando usuario en nuevo sistema", newUserResult.toString(), null)
                    return@withContext MigrationResult.Error("Error creando usuario: ${newUserResult}")
                }
                
                val newUser = newUserResult.user
                Logger.success(TAG, "Usuario migrado exitosamente", "ID: ${newUser.id}")
                
                // 3. Migrar datos de wallet si existen
                if (legacyData.walletAddress != null) {
                    migrateWalletData(newUser.id, legacyData)
                }
                
                // 4. Migrar datos de DID si existen
                if (legacyData.did != null) {
                    migrateDIDData(newUser.id, legacyData)
                }
                
                // 5. Marcar migración como completada
                markMigrationCompleted()
                
                MigrationResult.Success(
                    message = "Usuario migrado exitosamente: ${legacyData.name}",
                    migratedUser = newUser,
                    migratedData = legacyData
                )
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error durante migración", e.message ?: "Error desconocido", e)
                MigrationResult.Error("Error en migración: ${e.message}")
            }
        }
    }
    
    /**
     * Busca datos de usuario en el sistema anterior
     */
    private fun findLegacyUserData(): LegacyUserData? {
        try {
            // Buscar en diferentes archivos de preferencias
            val prefsNames = listOf(
                "substrate_crypto_prefs", // StorageManager - archivo principal
                "wallet_storage", // WalletRepository - datos de wallet
                "secure_storage_default", // KeyStoreManager - usuario por defecto
                LEGACY_USER_PREFS,
                LEGACY_WALLET_PREFS,
                LEGACY_DID_PREFS,
                "user_preferences",
                "secure_preferences",
                "wallet_preferences"
            )
            
            for (prefsName in prefsNames) {
                val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val userData = extractUserDataFromPrefs(prefs)
                if (userData != null) {
                    Logger.debug(TAG, "Datos encontrados en preferencias", "Archivo: $prefsName")
                    return userData
                }
            }
            
            Logger.debug(TAG, "No se encontraron datos en preferencias", "")
            return null
            
        } catch (e: Exception) {
            Logger.error(TAG, "Error buscando datos anteriores", e.message ?: "Error desconocido", e)
            return null
        }
    }
    
    /**
     * Extrae datos de usuario de un archivo de preferencias
     */
    private fun extractUserDataFromPrefs(prefs: SharedPreferences): LegacyUserData? {
        val allEntries = prefs.all
        
        if (allEntries.isEmpty()) {
            return null
        }
        
        Logger.debug(TAG, "Analizando preferencias", "Claves: ${allEntries.keys.joinToString(", ")}")
        
        // Buscar patrones comunes de datos de usuario
        val userName = allEntries.entries.find { 
            it.key.contains("name", ignoreCase = true) || 
            it.key.contains("user", ignoreCase = true) ||
            it.key.contains("username", ignoreCase = true)
        }?.value?.toString()
        
        val walletAddress = allEntries.entries.find { 
            it.key.contains("address", ignoreCase = true) || 
            it.key.contains("wallet", ignoreCase = true) ||
            it.key.contains("public", ignoreCase = true)
        }?.value?.toString()
        
        val did = allEntries.entries.find { 
            it.key.contains("did", ignoreCase = true) || 
            it.key.contains("identity", ignoreCase = true)
        }?.value?.toString()
        
        val mnemonic = allEntries.entries.find { 
            it.key.contains("mnemonic", ignoreCase = true) || 
            it.key.contains("seed", ignoreCase = true) ||
            it.key.contains("phrase", ignoreCase = true)
        }?.value?.toString()
        
        val privateKey = allEntries.entries.find { 
            it.key.contains("private", ignoreCase = true) || 
            it.key.contains("secret", ignoreCase = true) ||
            it.key.contains("key", ignoreCase = true)
        }?.value?.toString()
        
        // Si encontramos al menos un dato relevante, crear el objeto
        if (userName != null || walletAddress != null || did != null || mnemonic != null || privateKey != null) {
            return LegacyUserData(
                name = userName ?: "Usuario Migrado",
                walletAddress = walletAddress,
                did = did,
                mnemonic = mnemonic,
                privateKey = privateKey,
                sourcePreferences = "legacy_preferences"
            )
        }
        
        return null
    }
    
    /**
     * Crea usuario en el nuevo sistema
     */
    private suspend fun createUserInNewSystem(legacyData: LegacyUserData): UserManager.UserAuthResult {
        return try {
            userManager.registerNewUser(
                userName = legacyData.name,
                requireBiometric = false // No requerir biometría para migración
            )
        } catch (e: Exception) {
            Logger.error(TAG, "Error creando usuario", e.message ?: "Error desconocido", e)
            UserManager.UserAuthResult.Error("Error creando usuario: ${e.message}")
        }
    }
    
    /**
     * Migra datos de wallet
     */
    private suspend fun migrateWalletData(userId: String, legacyData: LegacyUserData) {
        try {
            if (legacyData.walletAddress != null) {
                val walletResult = secureUserRepository.createUserWallet(
                    walletName = "Wallet Migrada - ${legacyData.name}",
                    mnemonic = legacyData.mnemonic ?: "",
                    publicKey = legacyData.walletAddress.toByteArray(), // Convertir a ByteArray
                    privateKey = legacyData.privateKey?.toByteArray() ?: byteArrayOf(), // Convertir a ByteArray
                    address = legacyData.walletAddress,
                    cryptoType = "polkadot", // Asumir Polkadot por defecto
                    derivationPath = "//0", // Path por defecto
                    requireBiometric = false // No requerir biometría para migración
                )
                
                if (walletResult.isSuccess) {
                    Logger.success(TAG, "Wallet migrada exitosamente", "Address: ${legacyData.walletAddress}")
                } else {
                    Logger.warning(TAG, "Error migrando wallet", walletResult.exceptionOrNull()?.message ?: "Error desconocido")
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error migrando datos de wallet", e.message ?: "Error desconocido", e)
        }
    }
    
    /**
     * Migra datos de DID
     */
    private suspend fun migrateDIDData(userId: String, legacyData: LegacyUserData) {
        try {
            if (legacyData.did != null) {
                // Crear documento DID en el sistema seguro
                val documentResult = secureUserRepository.createUserDocument(
                    walletId = "migrated_wallet_${System.currentTimeMillis()}",
                    documentHash = "did_${legacyData.did}",
                    documentType = "did_document",
                    blockchainTimestamp = null,
                    metadata = mapOf(
                        "did" to legacyData.did,
                        "migration_source" to "legacy_system",
                        "original_name" to legacyData.name,
                        "migration_timestamp" to System.currentTimeMillis()
                    )
                )
                
                if (documentResult.isSuccess) {
                    Logger.success(TAG, "DID migrado exitosamente", "DID: ${legacyData.did}")
                } else {
                    Logger.warning(TAG, "Error migrando DID", documentResult.exceptionOrNull()?.message ?: "Error desconocido")
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error migrando datos de DID", e.message ?: "Error desconocido", e)
        }
    }
    
    /**
     * Marca la migración como completada
     */
    private fun markMigrationCompleted() {
        val prefs = context.getSharedPreferences("migration_status", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("legacy_user_migrated", true)
            .putLong("migration_timestamp", System.currentTimeMillis())
            .apply()
        
        Logger.success(TAG, "Migración marcada como completada", "")
    }
    
    /**
     * Verifica si ya se realizó la migración
     */
    fun isMigrationCompleted(): Boolean {
        val prefs = context.getSharedPreferences("migration_status", Context.MODE_PRIVATE)
        return prefs.getBoolean("legacy_user_migrated", false)
    }
    
    /**
     * Datos del usuario del sistema anterior
     */
    data class LegacyUserData(
        val name: String,
        val walletAddress: String?,
        val did: String?,
        val mnemonic: String?,
        val privateKey: String?,
        val sourcePreferences: String
    )
    
    /**
     * Resultado de migración
     */
    sealed class MigrationResult {
        data class Success(
            val message: String,
            val migratedUser: UserManager.User,
            val migratedData: LegacyUserData
        ) : MigrationResult()
        
        data class Error(val message: String) : MigrationResult()
        object NoDataFound : MigrationResult()
    }
}
