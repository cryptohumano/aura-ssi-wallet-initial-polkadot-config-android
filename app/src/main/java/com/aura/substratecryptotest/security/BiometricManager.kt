package com.aura.substratecryptotest.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.util.Log

/**
 * Manager para autenticación biométrica
 */
class BiometricManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BiometricManager"
    }
    
    /**
     * Verifica si la biometría está disponible en el dispositivo
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "No hay hardware biométrico disponible")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Hardware biométrico no disponible")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No hay biometría registrada")
                false
            }
            else -> false
        }
    }
    
    /**
     * Muestra el prompt biométrico para autenticación
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Autenticación Biométrica",
        subtitle: String = "Usa tu huella dactilar o reconocimiento facial",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (!isBiometricAvailable()) {
            onError("Biometría no disponible en este dispositivo")
            return
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(TAG, "Autenticación biométrica exitosa")
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "Error en autenticación biométrica: $errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                    else -> onError(errString.toString())
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Autenticación biométrica falló")
                onError("Autenticación falló. Intenta nuevamente.")
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Obtiene el tipo de biometría disponible
     */
    fun getAvailableBiometricType(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Huella dactilar o reconocimiento facial"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No disponible"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware no disponible"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No configurado"
            else -> "Desconocido"
        }
    }
}
