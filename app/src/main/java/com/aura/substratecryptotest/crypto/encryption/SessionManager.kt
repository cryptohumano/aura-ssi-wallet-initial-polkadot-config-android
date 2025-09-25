package com.aura.substratecryptotest.crypto.encryption

import android.content.Context
import com.aura.substratecryptotest.crypto.kilt.KiltDidManager
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de sesiones de autenticación DID
 * Orquesta el flujo completo de autenticación con servidor
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
    }
    
    private val encryptionKeyManager = EncryptionKeyManager(context)
    private val biometricManager = BiometricManager(context)
    private val kiltDidManager = KiltDidManager()
    private val walletManager = WalletManager(context)
    
    /**
     * Datos de sesión de autenticación
     */
    data class SessionData(
        val sessionId: String,
        val encryptionKey: ByteArray,
        val nonce: ByteArray,
        val salt: ByteArray,
        val didAddress: String,
        val encryptionKeyId: String,
        val challengeNonce: ByteArray? = null  // Nonce usado para encriptar el challenge
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as SessionData
            
            if (sessionId != other.sessionId) return false
            if (!encryptionKey.contentEquals(other.encryptionKey)) return false
            if (!nonce.contentEquals(other.nonce)) return false
            if (!salt.contentEquals(other.salt)) return false
            if (didAddress != other.didAddress) return false
            if (encryptionKeyId != other.encryptionKeyId) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = sessionId.hashCode()
            result = 31 * result + encryptionKey.contentHashCode()
            result = 31 * result + nonce.contentHashCode()
            result = 31 * result + salt.contentHashCode()
            result = 31 * result + didAddress.hashCode()
            result = 31 * result + encryptionKeyId.hashCode()
            return result
        }
    }
    
    /**
     * Resultado de autenticación
     */
    data class AuthenticationResult(
        val success: Boolean,
        val sessionData: SessionData?,
        val encryptedChallenge: ByteArray?,
        val error: String?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as AuthenticationResult
            
            if (success != other.success) return false
            if (sessionData != other.sessionData) return false
            if (!encryptedChallenge.contentEquals(other.encryptedChallenge)) return false
            if (error != other.error) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = success.hashCode()
            result = 31 * result + (sessionData?.hashCode() ?: 0)
            result = 31 * result + (encryptedChallenge?.contentHashCode() ?: 0)
            result = 31 * result + (error?.hashCode() ?: 0)
            return result
        }
    }
    
    /**
     * Inicia el proceso de autenticación con el servidor
     * @param challenge Challenge recibido del servidor
     * @param useBiometric Si usar TouchID/Fingerprint
     * @return AuthenticationResult con el resultado
     */
    suspend fun startAuthentication(
        challenge: String,
        useBiometric: Boolean = true
    ): AuthenticationResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "🚀 Iniciando autenticación DID", "Challenge length: ${challenge.length}")
                
                // 1. Obtener DID actual de la wallet activa
                val kiltDid = walletManager.getCurrentWalletKiltDid()
                if (kiltDid == null) {
                    Logger.error(TAG, "No hay DID disponible", "Derivar DID primero", null)
                    return@withContext AuthenticationResult(
                        success = false,
                        sessionData = null,
                        encryptedChallenge = null,
                        error = "No hay DID disponible"
                    )
                }
                
                // Extraer dirección del DID
                val didAddress = kiltDidManager.extractAddressFromDid(kiltDid)
                Logger.debug(TAG, "DID obtenido", "Address: ${didAddress.take(20)}...")
                
                // 2. Generar datos de sesión
                val sessionData = generateSessionData(didAddress, useBiometric)
                if (sessionData == null) {
                    Logger.error(TAG, "Error generando datos de sesión", "No se pudo crear sesión")
                    return@withContext AuthenticationResult(
                        success = false,
                        sessionData = null,
                        encryptedChallenge = null,
                        error = "Error generando datos de sesión"
                    )
                }
                
                Logger.success(TAG, "Datos de sesión generados", "SessionId: ${sessionData.sessionId.take(10)}...")
                
                // 3. Encriptar challenge
                val encryptionResult = encryptionKeyManager.encryptChallenge(challenge, sessionData.encryptionKey)
                if (encryptionResult == null) {
                    Logger.error(TAG, "Error encriptando challenge", "No se pudo encriptar", null)
                    return@withContext AuthenticationResult(
                        success = false,
                        sessionData = null,
                        encryptedChallenge = null,
                        error = "Error encriptando challenge"
                    )
                }
                
                val (encryptedChallenge, challengeNonce) = encryptionResult
                Logger.success(TAG, "Challenge encriptado", "Encrypted size: ${encryptedChallenge.size} bytes")
                
                // Actualizar sessionData con el nonce del challenge
                val updatedSessionData = sessionData.copy(challengeNonce = challengeNonce)
                
                // 4. Retornar resultado exitoso
                AuthenticationResult(
                    success = true,
                    sessionData = updatedSessionData,
                    encryptedChallenge = encryptedChallenge,
                    error = null
                )
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error en autenticación", e.message ?: "Error desconocido", e)
                AuthenticationResult(
                    success = false,
                    sessionData = null,
                    encryptedChallenge = null,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    /**
     * Verifica un challenge encriptado
     * @param encryptedChallenge Challenge encriptado
     * @param nonce Nonce usado para encriptar
     * @param encryptionKey Clave de encriptación
     * @return String con el challenge desencriptado o null si falla
     */
    suspend fun verifyChallenge(
        encryptedChallenge: ByteArray,
        nonce: ByteArray,
        encryptionKey: ByteArray
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Verificando challenge", "Encrypted size: ${encryptedChallenge.size}")
                
                val challenge = encryptionKeyManager.decryptChallenge(encryptedChallenge, nonce, encryptionKey)
                
                if (challenge != null) {
                    Logger.success(TAG, "Challenge verificado", "Challenge length: ${challenge.length}")
                } else {
                    Logger.error(TAG, "Error verificando challenge", "No se pudo desencriptar")
                }
                
                challenge
            } catch (e: Exception) {
                Logger.error(TAG, "Error verificando challenge", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Genera datos de sesión para autenticación
     * @param didAddress Dirección del DID
     * @param useBiometric Si usar autenticación biométrica
     * @return SessionData o null si falla
     */
    private suspend fun generateSessionData(didAddress: String, useBiometric: Boolean): SessionData? {
        return try {
            // Generar salt único
            val salt = encryptionKeyManager.generateSalt()
            
            // Generar encryption key
            val encryptionKey = if (useBiometric && biometricManager.isBiometricAvailable()) {
                // Usar TouchID/Fingerprint
                val password = biometricManager.authenticateWithBiometric()
                encryptionKeyManager.generateEncryptionKey(password, salt)
            } else {
                // Usar contraseña basada en DID
                val password = biometricManager.generateDidPassword(didAddress)
                encryptionKeyManager.generateEncryptionKey(password, salt)
            }
            
            if (encryptionKey == null) {
                Logger.error(TAG, "Error generando encryption key", "No se pudo generar clave")
                return null
            }
            
            // Generar nonce
            val nonce = encryptionKeyManager.generateNonce()
            
            // Generar session ID
            val sessionId = generateSessionId()
            
            // Crear encryption key ID (usar dirección DID)
            val encryptionKeyId = didAddress
            
            SessionData(
                sessionId = sessionId,
                encryptionKey = encryptionKey,
                nonce = nonce,
                salt = salt,
                didAddress = didAddress,
                encryptionKeyId = encryptionKeyId
            )
            
        } catch (e: Exception) {
            Logger.error(TAG, "Error generando datos de sesión", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Genera un ID único para la sesión
     * @return String con el ID de sesión
     */
    private fun generateSessionId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "kilt_session_${timestamp}_${random}"
    }
    
    /**
     * Limpia los datos de sesión
     * @param sessionData Datos de sesión a limpiar
     */
    fun cleanupSession(sessionData: SessionData?) {
        sessionData?.let {
            Logger.debug(TAG, "Limpiando sesión", "SessionId: ${it.sessionId.take(10)}...")
            // En una implementación real, aquí se limpiarían los datos sensibles
        }
    }
}
