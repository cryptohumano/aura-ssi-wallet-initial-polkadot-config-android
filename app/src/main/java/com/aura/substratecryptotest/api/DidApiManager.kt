package com.aura.substratecryptotest.api

import android.content.Context
import com.aura.substratecryptotest.api.models.*
import com.aura.substratecryptotest.crypto.encryption.EncryptionKeyManager
import com.aura.substratecryptotest.crypto.encryption.SessionManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manager que integra el API Client con el EncryptionKeyManager
 * Maneja la autenticación DID completa y la comunicación con el servidor
 */
class DidApiManager(private val context: Context, private val walletManager: com.aura.substratecryptotest.wallet.WalletManager) {
    
    companion object {
        private const val TAG = "DidApiManager"
    }
    
    private val apiClient = DidApiClient(context)
    private val encryptionKeyManager = EncryptionKeyManager(context)
    private val sessionManager = SessionManager(context, walletManager)
    
    // Estado de la sesión
    var currentSessionData: SessionManager.SessionData? = null
        private set
    private var authToken: String? = null
    
    /**
     * Inicializa la conexión con el servidor
     */
    suspend fun initialize(): Result<HealthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Inicializando conexión con servidor DID", "Verificando estado del servidor")
                
                val healthResult = apiClient.checkHealth()
                if (healthResult.isSuccess) {
                    Logger.success(TAG, "Conexión con servidor establecida", "Servidor disponible")
                } else {
                    Logger.error(TAG, "Error conectando con servidor", "Servidor no disponible", null)
                }
                
                healthResult
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción inicializando conexión", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Inicia sesión de autenticación DID
     */
    suspend fun startDidAuthentication(
        challenge: String,
        useBiometric: Boolean = true
    ): Result<AuthenticationResult> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Iniciando autenticación DID", "Challenge length: ${challenge.length}")
                
                // 1. Iniciar sesión local con EncryptionKeyManager
                val sessionResult = sessionManager.startAuthentication(challenge, useBiometric)
                
                if (sessionResult.success && sessionResult.sessionData != null) {
                    currentSessionData = sessionResult.sessionData
                    
                    // 2. Generar token de autenticación (simulado)
                    // En una implementación real, esto vendría del servidor
                    val simulatedToken = generateSimulatedAuthToken(sessionResult.sessionData)
                    authToken = simulatedToken
                    
                    // 3. Configurar token en el API client
                    apiClient.setAuthToken(simulatedToken)
                    
                    Logger.success(TAG, "Autenticación DID iniciada", "SessionId: ${sessionResult.sessionData.sessionId.take(10)}...")
                    
                    Result.success(AuthenticationResult(
                        success = true,
                        sessionData = sessionResult.sessionData,
                        authToken = simulatedToken,
                        encryptedChallenge = sessionResult.encryptedChallenge,
                        nonce = sessionResult.sessionData.challengeNonce,
                        error = null
                    ))
                } else {
                    Logger.error(TAG, "Error iniciando autenticación DID", sessionResult.error ?: "Error desconocido", null)
                    Result.failure(Exception(sessionResult.error ?: "Error iniciando autenticación"))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción iniciando autenticación DID", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verifica un challenge encriptado
     */
    suspend fun verifyChallenge(
        encryptedChallenge: ByteArray,
        nonce: ByteArray,
        encryptionKey: ByteArray
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Verificando challenge encriptado", "Encrypted size: ${encryptedChallenge.size}")
                
                val challenge = sessionManager.verifyChallenge(encryptedChallenge, nonce, encryptionKey)
                
                if (challenge != null) {
                    Logger.success(TAG, "Challenge verificado exitosamente", "Challenge length: ${challenge.length}")
                    Result.success(challenge)
                } else {
                    Logger.error(TAG, "Error verificando challenge", "No se pudo desencriptar", null)
                    Result.failure(Exception("Error verificando challenge"))
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción verificando challenge", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Almacena una firma DID en el servidor
     */
    suspend fun storeDidSignature(
        contractId: String,
        userDid: String,
        signatureData: ByteArray,
        fileName: String
    ): Result<StoreSignatureResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Almacenando firma DID en servidor", "Contract: $contractId, File: $fileName")
                
                // Crear archivo temporal
                val tempFile = createTempSignatureFile(signatureData, fileName)
                
                val result = apiClient.storeSignature(contractId, userDid, tempFile, fileName)
                
                // Limpiar archivo temporal
                tempFile.delete()
                
                if (result.isSuccess) {
                    Logger.success(TAG, "Firma DID almacenada en servidor", "Contract completed: ${result.getOrNull()?.contractCompleted}")
                } else {
                    Logger.error(TAG, "Error almacenando firma DID en servidor", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                }
                
                result
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción almacenando firma DID", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verifica una firma DID en el servidor
     */
    suspend fun verifyDidSignature(
        signatureData: ByteArray,
        fileName: String
    ): Result<VerifySignatureResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Verificando firma DID en servidor", "File: $fileName")
                
                // Crear archivo temporal
                val tempFile = createTempSignatureFile(signatureData, fileName)
                
                val result = apiClient.verifySignature(tempFile)
                
                // Limpiar archivo temporal
                tempFile.delete()
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    Logger.success(TAG, "Firma DID verificada en servidor", "Valid: ${response?.isValid}, DID: ${response?.details?.did}")
                } else {
                    Logger.error(TAG, "Error verificando firma DID en servidor", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                }
                
                result
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción verificando firma DID", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene estadísticas de firmas del usuario
     */
    suspend fun getUserSignatureStats(): Result<SignatureStatsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Obteniendo estadísticas de firmas del usuario", "GET /stats")
                
                val result = apiClient.getSignatureStats()
                
                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    Logger.success(TAG, "Estadísticas obtenidas", "Contracts: ${stats?.contractsWithDidSignatures}, Signatures: ${stats?.userDidSignatures}")
                } else {
                    Logger.error(TAG, "Error obteniendo estadísticas", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                }
                
                result
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción obteniendo estadísticas", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Genera timestamp blockchain para un documento
     */
    suspend fun generateBlockchainTimestamp(
        contractId: String,
        userDid: String,
        documentHash: String
    ): Result<TimestampResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Generando timestamp blockchain", "Contract: $contractId, Hash: ${documentHash.take(20)}...")
                
                val result = apiClient.generateTimestamp(contractId, userDid, documentHash)
                
                if (result.isSuccess) {
                    val timestamp = result.getOrNull()
                    Logger.success(TAG, "Timestamp blockchain generado", "Timestamp: ${timestamp?.timestamp}")
                } else {
                    Logger.error(TAG, "Error generando timestamp blockchain", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                }
                
                result
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción generando timestamp blockchain", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cierra la sesión de autenticación
     */
    fun closeSession() {
        Logger.debug(TAG, "Cerrando sesión de autenticación", "Limpiando datos de sesión")
        
        currentSessionData = null
        authToken = null
        apiClient.clearAuthToken()
        
        Logger.success(TAG, "Sesión cerrada", "Datos de sesión limpiados")
    }
    
    /**
     * Verifica si hay una sesión activa
     */
    fun hasActiveSession(): Boolean {
        return currentSessionData != null && authToken != null
    }
    
    /**
     * Obtiene el token de autenticación actual
     */
    fun getCurrentAuthToken(): String? {
        return authToken
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Genera un token de autenticación simulado
     * En una implementación real, esto vendría del servidor
     */
    private fun generateSimulatedAuthToken(sessionData: SessionManager.SessionData): String {
        val timestamp = System.currentTimeMillis()
        val tokenData = "${sessionData.sessionId}_${sessionData.didAddress}_${timestamp}"
        return "simulated_jwt_${tokenData.hashCode().toString().replace("-", "")}"
    }
    
    /**
     * Crea un archivo temporal para la firma
     */
    private fun createTempSignatureFile(signatureData: ByteArray, fileName: String): File {
        val tempFile = File(context.cacheDir, "temp_signature_$fileName")
        FileOutputStream(tempFile).use { fos ->
            fos.write(signatureData)
        }
        return tempFile
    }
    
    // ===== DATA CLASSES =====
    
    /**
     * Resultado de autenticación DID
     */
    data class AuthenticationResult(
        val success: Boolean,
        val sessionData: SessionManager.SessionData?,
        val authToken: String?,
        val encryptedChallenge: ByteArray?,
        val nonce: ByteArray?,
        val error: String?
    )
}
