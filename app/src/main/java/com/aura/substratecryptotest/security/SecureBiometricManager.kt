package com.aura.substratecryptotest.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestor de autenticación biométrica seguro
 * Maneja TouchID/Fingerprint para acceso a datos críticos del KeyStore
 */
class SecureBiometricManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureBiometricManager"
    }
    
    /**
     * Verifica si la autenticación biométrica está disponible
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Logger.success(TAG, "Biometría disponible", "TouchID/Fingerprint habilitado")
                true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Logger.error(TAG, "Biometría no disponible", "Hardware no soportado", null)
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Logger.error(TAG, "Biometría no disponible", "Hardware no disponible", null)
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Logger.error(TAG, "Biometría no configurada", "Usuario no ha configurado biometría", null)
                false
            }
            else -> {
                Logger.error(TAG, "Biometría no disponible", "Error desconocido", null)
                false
            }
        }
    }
    
    /**
     * Verifica si la autenticación biométrica fuerte está disponible
     */
    fun isStrongBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Logger.success(TAG, "Biometría fuerte disponible", "Hardware de seguridad disponible")
                true
            }
            else -> {
                Logger.error(TAG, "Biometría fuerte no disponible", "Hardware de seguridad no disponible", null)
                false
            }
        }
    }
    
    /**
     * Autentica al usuario para acceso a datos críticos del KeyStore
     */
    suspend fun authenticateForCriticalData(
        activity: FragmentActivity,
        title: String = "Acceso Seguro",
        subtitle: String = "Usa tu huella digital o Face ID para acceder a tus datos críticos"
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (!isBiometricAvailable()) {
                    Logger.error(TAG, "Biometría no disponible", "No se puede acceder a datos críticos", null)
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }
                
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Logger.success(TAG, "Autenticación biométrica exitosa", "Acceso a datos críticos autorizado")
                        continuation.resume(true)
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Logger.error(TAG, "Error en autenticación biométrica", errString.toString(), null)
                        continuation.resumeWithException(Exception("Error biométrico: $errString"))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Logger.error(TAG, "Autenticación biométrica fallida", "Credenciales no reconocidas", null)
                        continuation.resumeWithException(Exception("Autenticación fallida"))
                    }
                })
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Se requiere autenticación biométrica para acceder a mnemonicos y claves privadas")
                    .setNegativeButtonText("Cancelar")
                    .setConfirmationRequired(true)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
                continuation.invokeOnCancellation {
                    biometricPrompt.cancelAuthentication()
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error iniciando autenticación biométrica", e.message ?: "Error desconocido", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Autentica al usuario para operaciones de wallet
     */
    suspend fun authenticateForWalletOperation(
        activity: FragmentActivity,
        operation: String,
        title: String = "Confirmar Operación",
        subtitle: String = "Confirmar $operation con biometría"
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (!isBiometricAvailable()) {
                    Logger.error(TAG, "Biometría no disponible", "No se puede realizar operación", null)
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }
                
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Logger.success(TAG, "Autenticación biométrica exitosa", "Operación: $operation")
                        continuation.resume(true)
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Logger.error(TAG, "Error en autenticación biométrica", errString.toString(), null)
                        continuation.resumeWithException(Exception("Error biométrico: $errString"))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Logger.error(TAG, "Autenticación biométrica fallida", "Credenciales no reconocidas", null)
                        continuation.resumeWithException(Exception("Autenticación fallida"))
                    }
                })
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Usa tu huella digital o Face ID para confirmar esta operación")
                    .setNegativeButtonText("Cancelar")
                    .setConfirmationRequired(true)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
                continuation.invokeOnCancellation {
                    biometricPrompt.cancelAuthentication()
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error iniciando autenticación biométrica", e.message ?: "Error desconocido", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Autentica al usuario para creación de wallet
     */
    suspend fun authenticateForWalletCreation(
        activity: FragmentActivity,
        title: String = "Crear Wallet Segura",
        subtitle: String = "Confirma la creación de tu wallet con biometría"
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (!isBiometricAvailable()) {
                    Logger.error(TAG, "Biometría no disponible", "No se puede crear wallet segura", null)
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }
                
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Logger.success(TAG, "Autenticación biométrica exitosa", "Wallet creation autorizada")
                        continuation.resume(true)
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Logger.error(TAG, "Error en autenticación biométrica", errString.toString(), null)
                        continuation.resumeWithException(Exception("Error biométrico: $errString"))
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Logger.error(TAG, "Autenticación biométrica fallida", "Credenciales no reconocidas", null)
                        continuation.resumeWithException(Exception("Autenticación fallida"))
                    }
                })
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Se requiere autenticación biométrica para crear una wallet segura")
                    .setNegativeButtonText("Cancelar")
                    .setConfirmationRequired(true)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
                continuation.invokeOnCancellation {
                    biometricPrompt.cancelAuthentication()
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error iniciando autenticación biométrica", e.message ?: "Error desconocido", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Verifica el estado de la biometría
     */
    fun getBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNKNOWN_ERROR
        }
    }
    
    /**
     * Estados de la biometría
     */
    enum class BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NONE_ENROLLED,
        UNKNOWN_ERROR
    }
}
