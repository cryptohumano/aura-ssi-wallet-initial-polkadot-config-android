package com.aura.substratecryptotest.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de wallet seguro que integra KeyStore y Biometría
 * Maneja la creación, almacenamiento y recuperación segura de wallets
 */
class SecureWalletManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureWalletManager"
    }
    
    private val keyStoreManager = KeyStoreManager(context)
    private val biometricManager = SecureBiometricManager(context)
    
    /**
     * Crea una nueva wallet de forma segura
     * Requiere autenticación biométrica
     */
    suspend fun createSecureWallet(
        activity: FragmentActivity,
        mnemonic: String,
        seed: ByteArray
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando wallet segura", "Mnemonic: ${mnemonic.length} palabras, Seed: ${seed.size} bytes")
                
                // 1. Autenticar con biometría (en hilo principal)
                val authenticated = withContext(Dispatchers.Main) {
                    biometricManager.authenticateForWalletCreation(activity)
                }
                if (!authenticated) {
                    Logger.error(TAG, "Autenticación biométrica fallida", "No se puede crear wallet", null)
                    return@withContext false
                }
                
                // 2. Almacenar mnemonic de forma segura
                val mnemonicStored = keyStoreManager.storeMnemonic(mnemonic)
                if (!mnemonicStored) {
                    Logger.error(TAG, "Error almacenando mnemonic", "No se puede crear wallet", null)
                    return@withContext false
                }
                
                // 3. Almacenar seed de forma segura
                val seedStored = keyStoreManager.storeSeed(seed)
                if (!seedStored) {
                    Logger.error(TAG, "Error almacenando seed", "No se puede crear wallet", null)
                    return@withContext false
                }
                
                Logger.success(TAG, "Wallet segura creada exitosamente", "Mnemonic y seed almacenados en KeyStore")
                true
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando wallet segura", e.message ?: "Error desconocido", e)
                false
            }
        }
    }
    
    /**
     * Recupera el mnemonic de forma segura
     * Requiere autenticación biométrica
     */
    suspend fun retrieveMnemonic(activity: FragmentActivity): String? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Recuperando mnemonic", "Solicitando acceso a datos críticos")
                
                // 1. Autenticar con biometría (en hilo principal)
                val authenticated = withContext(Dispatchers.Main) {
                    biometricManager.authenticateForCriticalData(activity)
                }
                if (!authenticated) {
                    Logger.error(TAG, "Autenticación biométrica fallida", "No se puede acceder a mnemonic", null)
                    return@withContext null
                }
                
                // 2. Recuperar mnemonic
                val mnemonic = keyStoreManager.retrieveMnemonic()
                if (mnemonic != null) {
                    Logger.success(TAG, "Mnemonic recuperado exitosamente", "Length: ${mnemonic.length}")
                } else {
                    Logger.error(TAG, "Mnemonic no encontrado", "No hay datos almacenados", null)
                }
                
                mnemonic
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error recuperando mnemonic", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Recupera el seed de forma segura
     * Requiere autenticación biométrica
     */
    suspend fun retrieveSeed(activity: FragmentActivity): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Recuperando seed", "Solicitando acceso a datos críticos")
                
                // 1. Autenticar con biometría (en hilo principal)
                val authenticated = withContext(Dispatchers.Main) {
                    biometricManager.authenticateForCriticalData(activity)
                }
                if (!authenticated) {
                    Logger.error(TAG, "Autenticación biométrica fallida", "No se puede acceder a seed", null)
                    return@withContext null
                }
                
                // 2. Recuperar seed
                val seed = keyStoreManager.retrieveSeed()
                if (seed != null) {
                    Logger.success(TAG, "Seed recuperado exitosamente", "Size: ${seed.size} bytes")
                } else {
                    Logger.error(TAG, "Seed no encontrado", "No hay datos almacenados", null)
                }
                
                seed
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error recuperando seed", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Verifica si hay una wallet almacenada
     */
    fun hasStoredWallet(): Boolean {
        return keyStoreManager.hasStoredData()
    }
    
    /**
     * Verifica si la biometría está disponible
     */
    fun isBiometricAvailable(): Boolean {
        return biometricManager.isBiometricAvailable()
    }
    
    /**
     * Obtiene el estado de la biometría
     */
    fun getBiometricStatus(): SecureBiometricManager.BiometricStatus {
        return biometricManager.getBiometricStatus()
    }
    
    /**
     * Limpia todos los datos de la wallet
     * Requiere autenticación biométrica
     */
    suspend fun clearWallet(activity: FragmentActivity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Limpiando wallet", "Solicitando confirmación biométrica")
                
                // 1. Autenticar con biometría (en hilo principal)
                val authenticated = withContext(Dispatchers.Main) {
                    biometricManager.authenticateForWalletOperation(
                        activity,
                        "Eliminar Wallet",
                        "Confirmar eliminación",
                        "Esta acción eliminará permanentemente tu wallet"
                    )
                }
                if (!authenticated) {
                    Logger.error(TAG, "Autenticación biométrica fallida", "No se puede eliminar wallet", null)
                    return@withContext false
                }
                
                // 2. Limpiar datos
                val cleared = keyStoreManager.clearAllData()
                if (cleared) {
                    Logger.success(TAG, "Wallet eliminada exitosamente", "Todos los datos limpiados")
                } else {
                    Logger.error(TAG, "Error eliminando wallet", "No se pudieron limpiar todos los datos", null)
                }
                
                cleared
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error eliminando wallet", e.message ?: "Error desconocido", e)
                false
            }
        }
    }
    
    /**
     * Exporta la wallet de forma segura
     * Requiere autenticación biométrica
     */
    suspend fun exportWallet(activity: FragmentActivity): WalletExport? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Exportando wallet", "Solicitando acceso a datos críticos")
                
                // 1. Autenticar con biometría (en hilo principal)
                val authenticated = withContext(Dispatchers.Main) {
                    biometricManager.authenticateForCriticalData(activity)
                }
                if (!authenticated) {
                    Logger.error(TAG, "Autenticación biométrica fallida", "No se puede exportar wallet", null)
                    return@withContext null
                }
                
                // 2. Recuperar datos
                val mnemonic = keyStoreManager.retrieveMnemonic()
                val seed = keyStoreManager.retrieveSeed()
                
                if (mnemonic != null && seed != null) {
                    val export = WalletExport(
                        mnemonic = mnemonic,
                        seed = seed,
                        exportedAt = System.currentTimeMillis()
                    )
                    
                    Logger.success(TAG, "Wallet exportada exitosamente", "Datos recuperados para exportación")
                    export
                } else {
                    Logger.error(TAG, "Datos incompletos", "No se puede exportar wallet", null)
                    null
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error exportando wallet", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Datos de exportación de wallet
     */
    data class WalletExport(
        val mnemonic: String,
        val seed: ByteArray,
        val exportedAt: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as WalletExport
            
            if (mnemonic != other.mnemonic) return false
            if (!seed.contentEquals(other.seed)) return false
            if (exportedAt != other.exportedAt) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = mnemonic.hashCode()
            result = 31 * result + seed.contentHashCode()
            result = 31 * result + exportedAt.hashCode()
            return result
        }
    }
}
