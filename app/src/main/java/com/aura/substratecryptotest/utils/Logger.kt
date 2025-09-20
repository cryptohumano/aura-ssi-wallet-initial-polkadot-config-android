package com.aura.substratecryptotest.utils

import android.util.Log
import com.aura.substratecryptotest.BuildConfig

/**
 * Clase centralizada para el manejo de logs en la aplicación
 * Proporciona un sistema de logging consistente y configurable
 */
object Logger {
    
    private const val DEFAULT_TAG = "SubstrateCrypto"
    
    /**
     * Niveles de logging disponibles
     */
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * Configuración de logging
     */
    private var isLoggingEnabled = BuildConfig.DEBUG_LOGGING
    private var minLevel = if (BuildConfig.DEBUG) Level.DEBUG else Level.WARN
    
    /**
     * Estado actual del logging (para debugging)
     */
    fun isEnabled(): Boolean = isLoggingEnabled
    
    /**
     * Nivel actual del logging (para debugging)
     */
    fun getCurrentLevel(): Level = minLevel
    
    /**
     * Habilita o deshabilita el logging
     */
    fun setLoggingEnabled(enabled: Boolean) {
        isLoggingEnabled = enabled
    }
    
    /**
     * Establece el nivel mínimo de logging
     */
    fun setMinLevel(level: Level) {
        minLevel = level
    }
    
    /**
     * Verifica si un nivel de logging debe ser mostrado
     */
    private fun shouldLog(level: Level): Boolean {
        return isLoggingEnabled && level.ordinal >= minLevel.ordinal
    }
    
    /**
     * Log de nivel VERBOSE
     */
    fun v(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.VERBOSE)) {
            if (throwable != null) {
                Log.v(tag, message, throwable)
            } else {
                Log.v(tag, message)
            }
        }
    }
    
    /**
     * Log de nivel DEBUG
     */
    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.DEBUG)) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }
    
    /**
     * Log de nivel INFO
     */
    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.INFO)) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }
    
    /**
     * Log de nivel WARN
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.WARN)) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * Log de nivel ERROR
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (shouldLog(Level.ERROR)) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * Log específico para KeyPairManager
     */
    fun keyPair(tag: String = "KeyPairManager", message: String, throwable: Throwable? = null) {
        i(tag, message, throwable)
    }
    
    /**
     * Log específico para WalletManager
     */
    fun wallet(tag: String = "WalletManager", message: String, throwable: Throwable? = null) {
        i(tag, message, throwable)
    }
    
    /**
     * Log específico para MnemonicManager
     */
    fun mnemonic(tag: String = "MnemonicManager", message: String, throwable: Throwable? = null) {
        i(tag, message, throwable)
    }
    
    /**
     * Log específico para operaciones de red
     */
    fun network(tag: String = "Network", message: String, throwable: Throwable? = null) {
        d(tag, message, throwable)
    }
    
    /**
     * Log específico para operaciones de base de datos
     */
    fun database(tag: String = "Database", message: String, throwable: Throwable? = null) {
        d(tag, message, throwable)
    }
    
    /**
     * Log específico para operaciones de seguridad
     */
    fun security(tag: String = "Security", message: String, throwable: Throwable? = null) {
        w(tag, message, throwable)
    }
    
    /**
     * Log con formato especial para operaciones exitosas
     */
    fun success(tag: String = DEFAULT_TAG, operation: String, details: String? = null) {
        val message = "✅ $operation${if (details != null) ": $details" else ""}"
        i(tag, message)
    }
    
    /**
     * Log con formato especial para errores
     */
    fun error(tag: String = DEFAULT_TAG, operation: String, error: String, throwable: Throwable? = null) {
        val message = "❌ $operation: $error"
        e(tag, message, throwable)
    }
    
    /**
     * Log con formato especial para advertencias
     */
    fun warning(tag: String = DEFAULT_TAG, operation: String, warning: String) {
        val message = "⚠️ $operation: $warning"
        w(tag, message)
    }
    
    /**
     * Log con formato especial para información de debug
     */
    fun debug(tag: String = DEFAULT_TAG, operation: String, details: String) {
        val message = "🔍 $operation: $details"
        d(tag, message)
    }
    
    /**
     * Métodos de conveniencia para activar/desactivar logging
     */
    fun enableDebugMode() {
        setLoggingEnabled(true)
        setMinLevel(Level.DEBUG)
    }
    
    fun enableVerboseMode() {
        setLoggingEnabled(true)
        setMinLevel(Level.VERBOSE)
    }
    
    fun disableLogging() {
        setLoggingEnabled(false)
    }
    
    fun enableProductionMode() {
        setLoggingEnabled(true)
        setMinLevel(Level.WARN)
    }
    
    /**
     * Log de estado del sistema de logging
     */
    fun logStatus() {
        val status = if (isLoggingEnabled) "HABILITADO" else "DESHABILITADO"
        val level = minLevel.name
        Log.i(DEFAULT_TAG, "📊 Logger: $status (Nivel: $level)")
        Log.i(DEFAULT_TAG, "🔧 Para cambiar: Logger.enableDebugMode() o Logger.disableLogging()")
    }
}
