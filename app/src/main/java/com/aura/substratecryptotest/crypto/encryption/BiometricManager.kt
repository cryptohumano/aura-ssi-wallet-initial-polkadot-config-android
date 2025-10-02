package com.aura.substratecryptotest.crypto.encryption

import android.content.Context
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestor de autenticación biométrica (TouchID/Fingerprint)
 * Maneja la autenticación del usuario para generar encryption keys
 */
class BiometricManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BiometricManager"
    }
    
    /**
     * Verifica si la autenticación biométrica está disponible
     * @return Boolean indicando si está disponible
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Logger.debug(TAG, "Autenticación biométrica disponible", "TouchID/Fingerprint habilitado")
                true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Logger.warning(TAG, "Sin hardware biométrico", "Dispositivo no tiene sensor biométrico")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Logger.warning(TAG, "Hardware biométrico no disponible", "Sensor biométrico temporalmente no disponible")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Logger.warning(TAG, "Sin huellas registradas", "Usuario no ha configurado huellas dactilares")
                false
            }
            else -> {
                Logger.warning(TAG, "Autenticación biométrica no disponible", "Estado desconocido")
                false
            }
        }
    }
    
    /**
     * Autentica al usuario usando TouchID/Fingerprint
     * @param title Título del prompt
     * @param subtitle Subtítulo del prompt
     * @return String con la contraseña generada o null si falla
     */
    suspend fun authenticateWithBiometric(
        title: String = "Autenticación KILT",
        subtitle: String = "Usa tu huella para autenticarte con el servidor"
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (!isBiometricAvailable()) {
                    Logger.warning(TAG, "Autenticación biométrica no disponible", "Fallback a contraseña generada")
                    continuation.resume(generateFallbackPassword())
                    return@suspendCancellableCoroutine
                }
                
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(
                    context as FragmentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            Logger.success(TAG, "Autenticación biométrica exitosa", "TouchID/Fingerprint verificado")
                            val password = generateBiometricPassword()
                            continuation.resume(password)
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            Logger.error(TAG, "Error en autenticación biométrica", "Code: $errorCode, Message: $errString")
                            continuation.resumeWithException(Exception("Error biométrico: $errString"))
                        }
                        
                        override fun onAuthenticationFailed() {
                            Logger.warning(TAG, "Autenticación biométrica fallida", "Huella no reconocida")
                            continuation.resumeWithException(Exception("Autenticación fallida"))
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
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Genera una contraseña basada en características biométricas del dispositivo
     * @return String con la contraseña generada
     */
    private fun generateBiometricPassword(): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val timestamp = System.currentTimeMillis()
        val biometricPassword = "${deviceId}_${timestamp}_kilt_biometric".hashCode().toString()
        
        Logger.debug(TAG, "Contraseña biométrica generada", "Length: ${biometricPassword.length}")
        
        return biometricPassword
    }
    
    /**
     * Genera una contraseña de fallback cuando TouchID no está disponible
     * @return String con la contraseña de fallback
     */
    private fun generateFallbackPassword(): String {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val timestamp = System.currentTimeMillis()
        val fallbackPassword = "${deviceId}_${timestamp}_kilt_fallback".hashCode().toString()
        
        Logger.debug(TAG, "Contraseña de fallback generada", "Length: ${fallbackPassword.length}")
        
        return fallbackPassword
    }
    
    /**
     * Genera una contraseña basada en el DID del usuario
     * @param didAddress Dirección del DID
     * @return String con la contraseña generada
     */
    fun generateDidPassword(didAddress: String): String {
        val timestamp = System.currentTimeMillis()
        val didPassword = "${didAddress}_${timestamp}_kilt_did".hashCode().toString()
        
        Logger.debug(TAG, "Contraseña DID generada", "Length: ${didPassword.length}")
        
        return didPassword
    }
}






