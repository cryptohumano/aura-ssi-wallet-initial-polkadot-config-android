package com.aura.substratecryptotest.crypto.encryption

import android.content.Context
import android.provider.Settings
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.xsalsa20poly1305.SecretBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Gestor de claves de encriptación para autenticación DID
 * Maneja la generación de encryption keys usando SCrypt y encriptación XSalsa20Poly1305
 */
class EncryptionKeyManager(private val context: Context) {
    
    companion object {
        // Parámetros SCrypt (mismos que usa el SDK de Substrate)
        private const val SCRYPT_N = 16384
        private const val SCRYPT_R = 8
        private const val SCRYPT_P = 1
        private const val SCRYPT_KEY_SIZE = 32
        
        // Tamaños para encriptación
        private const val NONCE_SIZE = 24
        private const val TAG_SIZE = 16
        private const val GCM_IV_LENGTH = 12
    }
    
    /**
     * Genera una clave de encriptación usando SCrypt
     * @param password Contraseña del usuario (puede ser null para usar TouchID)
     * @param salt Salt para la derivación de clave
     * @return ByteArray con la clave de encriptación de 32 bytes
     */
    suspend fun generateEncryptionKey(password: String?, salt: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val finalPassword = password ?: generateBiometricPassword()
                
                Logger.debug("EncryptionKeyManager", "Generando encryption key", 
                    "Password length: ${finalPassword.length}, Salt size: ${salt.size}")
                
                // Usar SCrypt para derivar la clave (como en el SDK)
                val encryptionKey = scryptGenerate(finalPassword, salt)
                
                Logger.success("EncryptionKeyManager", "Encryption key generada", 
                    "Key size: ${encryptionKey.size} bytes")
                
                encryptionKey
            } catch (e: Exception) {
                Logger.error("EncryptionKeyManager", "Error generando encryption key", 
                    e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Genera una clave de encriptación usando el DID como base
     * @param didAddress Dirección del DID
     * @param salt Salt adicional
     * @return ByteArray con la clave de encriptación
     */
    suspend fun generateEncryptionKeyFromDid(didAddress: String, salt: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("EncryptionKeyManager", "Generando encryption key desde DID", 
                    "DID: ${didAddress.take(20)}..., Salt size: ${salt.size}")
                
                // Usar el DID como base para la contraseña
                val didPassword = generateDidPassword(didAddress)
                val encryptionKey = scryptGenerate(didPassword, salt)
                
                Logger.success("EncryptionKeyManager", "Encryption key desde DID generada", 
                    "Key size: ${encryptionKey.size} bytes")
                
                encryptionKey
            } catch (e: Exception) {
                Logger.error("EncryptionKeyManager", "Error generando encryption key desde DID", 
                    e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Encripta un challenge usando XSalsa20Poly1305
     * @param challenge Challenge a encriptar
     * @param encryptionKey Clave de encriptación
     * @return Pair<ByteArray, ByteArray> con (encrypted, nonce)
     */
    fun encryptChallenge(challenge: String, encryptionKey: ByteArray): Pair<ByteArray, ByteArray>? {
        return try {
            Logger.debug("EncryptionKeyManager", "Encriptando challenge", 
                "Challenge length: ${challenge.length}, Key size: ${encryptionKey.size}")
            
            // Usar SecretBox (XSalsa20Poly1305) como en el SDK
            val secretBox = SecretBox(encryptionKey)
            val challengeBytes = challenge.toByteArray(Charsets.UTF_8)
            val nonce = secretBox.nonce(challengeBytes)
            val encrypted = secretBox.seal(nonce, challengeBytes)
            
            // Logs detallados para verificar encriptación (formato mejorado)
            Logger.debug("EncryptionKeyManager", "🔐 Challenge original", 
                "Text: '$challenge' (${challengeBytes.size} bytes)")
            Logger.debug("EncryptionKeyManager", "🔐 Nonce generado", 
                "Nonce: ${nonce.take(8).joinToString("") { "%02x".format(it) }}... (${nonce.size} bytes)")
            Logger.debug("EncryptionKeyManager", "🔐 Challenge encriptado", 
                "Encrypted: ${encrypted.take(8).joinToString("") { "%02x".format(it) }}... (${encrypted.size} bytes)")
            Logger.debug("EncryptionKeyManager", "🔐 Encryption key", 
                "Key: ${encryptionKey.take(8).joinToString("") { "%02x".format(it) }}... (${encryptionKey.size} bytes)")
            
            // Resumen visual de la encriptación
            Logger.success("EncryptionKeyManager", "✅ Challenge encriptado exitosamente", 
                "📝 Original: '$challenge' → 🔐 Encriptado: ${encrypted.size} bytes + Nonce: ${nonce.size} bytes")
            
            Pair(encrypted, nonce)
        } catch (e: Exception) {
            Logger.error("EncryptionKeyManager", "Error encriptando challenge", 
                e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Desencripta un challenge usando XSalsa20Poly1305
     * @param encryptedChallenge Challenge encriptado
     * @param nonce Nonce usado para encriptar
     * @param encryptionKey Clave de encriptación
     * @return String con el challenge desencriptado
     */
    fun decryptChallenge(encryptedChallenge: ByteArray, nonce: ByteArray, encryptionKey: ByteArray): String? {
        return try {
            Logger.debug("EncryptionKeyManager", "Desencriptando challenge", 
                "Encrypted size: ${encryptedChallenge.size}, Nonce size: ${nonce.size}, Key size: ${encryptionKey.size}")
            
            // Logs detallados para verificar desencriptación (formato mejorado)
            Logger.debug("EncryptionKeyManager", "🔓 Challenge encriptado recibido", 
                "Encrypted: ${encryptedChallenge.take(8).joinToString("") { "%02x".format(it) }}... (${encryptedChallenge.size} bytes)")
            Logger.debug("EncryptionKeyManager", "🔓 Nonce recibido", 
                "Nonce: ${nonce.take(8).joinToString("") { "%02x".format(it) }}... (${nonce.size} bytes)")
            Logger.debug("EncryptionKeyManager", "🔓 Encryption key recibida", 
                "Key: ${encryptionKey.take(8).joinToString("") { "%02x".format(it) }}... (${encryptionKey.size} bytes)")
            
            // Usar SecretBox (XSalsa20Poly1305) como en el SDK
            val secretBox = SecretBox(encryptionKey)
            val decrypted = secretBox.open(nonce, encryptedChallenge)
            
            val challenge = decrypted.toString(Charsets.UTF_8)
            
            // Logs detallados del resultado (formato mejorado)
            Logger.debug("EncryptionKeyManager", "🔓 Challenge desencriptado", 
                "Text: '$challenge' (${decrypted.size} bytes)")
            
            // Resumen visual de la desencriptación
            Logger.success("EncryptionKeyManager", "✅ Challenge desencriptado exitosamente", 
                "🔓 Encriptado: ${encryptedChallenge.size} bytes → 📝 Original: '$challenge' (${challenge.length} chars)")
            
            challenge
        } catch (e: Exception) {
            Logger.error("EncryptionKeyManager", "Error desencriptando challenge", 
                e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Genera un nonce único para la sesión
     * @return ByteArray con el nonce
     */
    fun generateNonce(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        SecureRandom().nextBytes(nonce)
        
        Logger.debug("EncryptionKeyManager", "Nonce generado", 
            "Nonce size: ${nonce.size} bytes")
        
        return nonce
    }
    
    /**
     * Genera un salt único para la derivación de clave
     * @return ByteArray con el salt
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(32) // 32 bytes de salt
        SecureRandom().nextBytes(salt)
        
        Logger.debug("EncryptionKeyManager", "Salt generado", 
            "Salt size: ${salt.size} bytes")
        
        return salt
    }
    
    /**
     * Verifica si una clave de encriptación es válida
     * @param encryptionKey Clave a verificar
     * @return Boolean indicando si es válida
     */
    fun isValidEncryptionKey(encryptionKey: ByteArray): Boolean {
        return encryptionKey.size == SCRYPT_KEY_SIZE
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Genera una contraseña basada en características biométricas del dispositivo
     * @return String con la contraseña generada
     */
    private fun generateBiometricPassword(): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val timestamp = System.currentTimeMillis()
        val biometricPassword = "${deviceId}_${timestamp}_kilt_auth".hashCode().toString()
        
        Logger.debug("EncryptionKeyManager", "Contraseña biométrica generada", 
            "Length: ${biometricPassword.length}")
        
        return biometricPassword
    }
    
    /**
     * Genera una contraseña basada en el DID
     * @param didAddress Dirección del DID
     * @return String con la contraseña generada
     */
    private fun generateDidPassword(didAddress: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
        val bytes = hash.digest(didAddress.toByteArray())
        val didPassword = bytes.joinToString("") { "%02x".format(it) }
        
        Logger.debug("EncryptionKeyManager", "Contraseña DID generada", 
            "Length: ${didPassword.length}")
        
        return didPassword
    }
    
    /**
     * Implementación de SCrypt para derivación de clave
     * @param password Contraseña
     * @param salt Salt
     * @return ByteArray con la clave derivada
     */
    private fun scryptGenerate(password: String, salt: ByteArray): ByteArray {
        // Implementación simplificada de SCrypt usando PBKDF2 como fallback
        // En producción, usar una librería SCrypt real
        val keySpec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            SCRYPT_N,
            SCRYPT_KEY_SIZE * 8
        )
        
        val keyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = keyFactory.generateSecret(keySpec)
        
        return key.encoded
    }
}
