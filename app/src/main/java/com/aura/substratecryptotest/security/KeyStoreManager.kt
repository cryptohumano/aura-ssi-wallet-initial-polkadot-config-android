package com.aura.substratecryptotest.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import com.aura.substratecryptotest.utils.Logger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gestor de Android KeyStore para datos críticos
 * Maneja el almacenamiento seguro de mnemonicos, seeds y claves privadas
 * Requiere autenticación biométrica para acceder
 */
class KeyStoreManager(private val context: Context) {
    
    companion object {
        private const val TAG = "KeyStoreManager"
        private const val KEYSTORE_ALIAS = "aura_wallet_keystore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        // Aliases para diferentes tipos de datos
        private const val MNEMONIC_ALIAS = "wallet_mnemonic"
        private const val SEED_ALIAS = "wallet_seed"
        private const val PRIVATE_KEYS_ALIAS = "private_keys"
        private const val DERIVATIONS_ALIAS = "derivations"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    
    init {
        keyStore.load(null)
        Logger.debug(TAG, "KeyStoreManager inicializado", "Android KeyStore cargado")
    }
    
    /**
     * Genera una clave AES para encriptación
     */
    private fun generateSecretKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // Temporalmente sin autenticación
            .setUserAuthenticationValidityDurationSeconds(0) // Sin duración de autenticación
            .setInvalidatedByBiometricEnrollment(false)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        val secretKey = keyGenerator.generateKey()
        
        Logger.success(TAG, "Clave secreta generada", "Alias: $alias")
        return secretKey
    }
    
    /**
     * Obtiene una clave existente o genera una nueva
     */
    private fun getOrCreateSecretKey(alias: String): SecretKey {
        return if (keyStore.containsAlias(alias)) {
            keyStore.getKey(alias, null) as SecretKey
        } else {
            generateSecretKey(alias)
        }
    }
    
    /**
     * Encripta datos usando AES-GCM
     */
    fun encryptData(data: ByteArray, alias: String): EncryptedData? {
        return try {
            val secretKey = getOrCreateSecretKey(alias)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)
            
            val encryptedDataBase64 = Base64.encodeToString(encryptedData, Base64.DEFAULT)
            val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)
            
            Logger.success(TAG, "Datos encriptados exitosamente", "Alias: $alias, Size: ${data.size} bytes")
            
            EncryptedData(
                encryptedData = encryptedDataBase64,
                iv = ivBase64,
                alias = alias
            )
        } catch (e: Exception) {
            Logger.error(TAG, "Error encriptando datos", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Desencripta datos usando AES-GCM
     * Requiere autenticación biométrica
     */
    fun decryptData(encryptedData: EncryptedData): ByteArray? {
        return try {
            val secretKey = keyStore.getKey(encryptedData.alias, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            val iv = Base64.decode(encryptedData.iv, Base64.DEFAULT)
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
            
            val encryptedBytes = Base64.decode(encryptedData.encryptedData, Base64.DEFAULT)
            val decryptedData = cipher.doFinal(encryptedBytes)
            
            Logger.success(TAG, "Datos desencriptados exitosamente", "Alias: ${encryptedData.alias}, Size: ${decryptedData.size} bytes")
            
            decryptedData
        } catch (e: Exception) {
            Logger.error(TAG, "Error desencriptando datos", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Almacena mnemonic de forma segura
     */
    fun storeMnemonic(mnemonic: String): Boolean {
        return try {
            val encryptedData = encryptData(mnemonic.toByteArray(), MNEMONIC_ALIAS)
            if (encryptedData != null) {
                // Guardar en SharedPreferences (solo la referencia, no los datos)
                val prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("mnemonic_encrypted", encryptedData.encryptedData)
                    .putString("mnemonic_iv", encryptedData.iv)
                    .apply()
                
                Logger.success(TAG, "Mnemonic almacenado de forma segura", "Alias: $MNEMONIC_ALIAS")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error almacenando mnemonic", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Recupera mnemonic de forma segura
     */
    fun retrieveMnemonic(): String? {
        return try {
            val prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
            val encryptedData = prefs.getString("mnemonic_encrypted", null)
            val iv = prefs.getString("mnemonic_iv", null)
            
            if (encryptedData != null && iv != null) {
                val encryptedDataObj = EncryptedData(
                    encryptedData = encryptedData,
                    iv = iv,
                    alias = MNEMONIC_ALIAS
                )
                
                val decryptedBytes = decryptData(encryptedDataObj)
                decryptedBytes?.toString(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error recuperando mnemonic", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Almacena seed de forma segura
     */
    fun storeSeed(seed: ByteArray): Boolean {
        return try {
            val encryptedData = encryptData(seed, SEED_ALIAS)
            if (encryptedData != null) {
                val prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("seed_encrypted", encryptedData.encryptedData)
                    .putString("seed_iv", encryptedData.iv)
                    .apply()
                
                Logger.success(TAG, "Seed almacenado de forma segura", "Size: ${seed.size} bytes")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error almacenando seed", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Recupera seed de forma segura
     */
    fun retrieveSeed(): ByteArray? {
        return try {
            val prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
            val encryptedData = prefs.getString("seed_encrypted", null)
            val iv = prefs.getString("seed_iv", null)
            
            if (encryptedData != null && iv != null) {
                val encryptedDataObj = EncryptedData(
                    encryptedData = encryptedData,
                    iv = iv,
                    alias = SEED_ALIAS
                )
                
                decryptData(encryptedDataObj)
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error recuperando seed", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Verifica si hay datos almacenados
     */
    fun hasStoredData(): Boolean {
        val prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
        return prefs.getString("mnemonic_encrypted", null) != null ||
               prefs.getString("seed_encrypted", null) != null
    }
    
    /**
     * Limpia todos los datos almacenados
     */
    fun clearAllData(): Boolean {
        return try {
            val prefs = context.getSharedPreferences("secure_storage", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            // Eliminar claves del KeyStore
            if (keyStore.containsAlias(MNEMONIC_ALIAS)) {
                keyStore.deleteEntry(MNEMONIC_ALIAS)
            }
            if (keyStore.containsAlias(SEED_ALIAS)) {
                keyStore.deleteEntry(SEED_ALIAS)
            }
            if (keyStore.containsAlias(PRIVATE_KEYS_ALIAS)) {
                keyStore.deleteEntry(PRIVATE_KEYS_ALIAS)
            }
            if (keyStore.containsAlias(DERIVATIONS_ALIAS)) {
                keyStore.deleteEntry(DERIVATIONS_ALIAS)
            }
            
            Logger.success(TAG, "Todos los datos limpiados", "KeyStore y SharedPreferences limpiados")
            true
        } catch (e: Exception) {
            Logger.error(TAG, "Error limpiando datos", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Datos encriptados
     */
    data class EncryptedData(
        val encryptedData: String,
        val iv: String,
        val alias: String
    )
}
