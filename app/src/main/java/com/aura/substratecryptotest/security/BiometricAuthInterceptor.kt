package com.aura.substratecryptotest.security

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interceptor de autenticación biométrica para operaciones críticas
 * Requiere TouchID/FaceID antes de realizar operaciones de escritura
 */
class BiometricAuthInterceptor(private val context: Context) {
    
    companion object {
        private const val TAG = "BiometricAuthInterceptor"
    }
    
    private val userManager = UserManager(context)
    
    /**
     * Requiere autenticación biométrica para operaciones críticas
     */
    suspend fun requireBiometricAuth(
        operation: String,
        requireCurrentUser: Boolean = true
    ): BiometricAuthResult {
        return try {
            Logger.debug(TAG, "Solicitando autenticación biométrica", "Operación: $operation")
            
            // Verificar que hay un usuario activo
            if (requireCurrentUser) {
                val currentUser = userManager.getCurrentUser()
                if (currentUser == null) {
                    Logger.error(TAG, "No hay usuario activo", "No se puede realizar operación", null)
                    return BiometricAuthResult.Error("No hay usuario activo")
                }
            }
            
            // Verificar disponibilidad biométrica
            if (!isBiometricAvailable()) {
                Logger.error(TAG, "Biometría no disponible", "No se puede autenticar", null)
                return BiometricAuthResult.Error("Autenticación biométrica no disponible")
            }
            
            // Realizar autenticación biométrica
            val authResult = authenticateWithBiometric(
                title = "Confirmar $operation",
                subtitle = "Usa tu huella para confirmar esta acción crítica"
            )
            
            if (authResult) {
                Logger.success(TAG, "Autenticación biométrica exitosa", "Operación: $operation")
                BiometricAuthResult.Success
            } else {
                Logger.error(TAG, "Autenticación biométrica fallida", "Operación: $operation", null)
                BiometricAuthResult.Error("Autenticación biométrica fallida")
            }
            
        } catch (e: Exception) {
            Logger.error(TAG, "Error en autenticación biométrica", e.message ?: "Error desconocido", e)
            BiometricAuthResult.Error("Error en autenticación: ${e.message}")
        }
    }
    
    /**
     * Requiere autenticación biométrica para acceso a datos sensibles
     */
    suspend fun requireBiometricAuthForSensitiveData(
        dataType: String
    ): BiometricAuthResult {
        return requireBiometricAuth(
            operation = "acceso a $dataType",
            requireCurrentUser = true
        )
    }
    
    /**
     * Requiere autenticación biométrica para operaciones de escritura
     */
    suspend fun requireBiometricAuthForWriteOperation(
        operation: String
    ): BiometricAuthResult {
        return requireBiometricAuth(
            operation = "escritura: $operation",
            requireCurrentUser = true
        )
    }
    
    /**
     * Requiere autenticación biométrica para cambio de usuario
     */
    suspend fun requireBiometricAuthForUserSwitch(): BiometricAuthResult {
        return requireBiometricAuth(
            operation = "cambio de usuario",
            requireCurrentUser = false
        )
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Verifica si la autenticación biométrica está disponible
     */
    private fun isBiometricAvailable(): Boolean {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Autentica con TouchID/FaceID
     */
    private suspend fun authenticateWithBiometric(
        title: String,
        subtitle: String
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(
                    context as FragmentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            Logger.success(TAG, "Autenticación biométrica exitosa", "TouchID/FaceID verificado")
                            continuation.resume(true)
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            Logger.error(TAG, "Error en autenticación biométrica", "Code: $errorCode, Message: $errString")
                            continuation.resume(false)
                        }
                        
                        override fun onAuthenticationFailed() {
                            Logger.warning(TAG, "Autenticación biométrica fallida", "Huella no reconocida")
                            continuation.resume(false)
                        }
                    }
                )
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Cancelar")
                    .setConfirmationRequired(true)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
                continuation.invokeOnCancellation {
                    biometricPrompt.cancelAuthentication()
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error iniciando autenticación biométrica", e.message ?: "Error desconocido", e)
                continuation.resume(false)
            }
        }
    }
}

/**
 * Resultado de autenticación biométrica
 */
sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Error(val message: String) : BiometricAuthResult()
}



