package com.aura.substratecryptotest.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de idiomas para la aplicación
 * Maneja el cambio de idioma y la persistencia de preferencias
 */
class LanguageManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "language_preferences"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_FIRST_TIME = "first_time"
        
        // Idiomas soportados
        const val LANGUAGE_SPANISH = "es"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_AUTO = "auto"
        
        @Volatile
        private var INSTANCE: LanguageManager? = null
        
        fun getInstance(context: Context): LanguageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanguageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Estado reactivo del idioma
    private val _currentLanguage = MutableStateFlow(getCurrentLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    // Estado para primera vez
    private val _isFirstTime = MutableStateFlow(isFirstTime())
    val isFirstTime: StateFlow<Boolean> = _isFirstTime.asStateFlow()
    
    /**
     * Obtiene el idioma actual
     */
    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_AUTO) ?: LANGUAGE_AUTO
    }
    
    /**
     * Verifica si es la primera vez que se abre la app
     */
    fun isFirstTime(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }
    
    /**
     * Marca que ya no es la primera vez
     */
    fun setFirstTimeCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_TIME, false).apply()
        _isFirstTime.value = false
    }
    
    /**
     * Establece el idioma de la aplicación
     */
    fun setLanguage(language: String) {
        android.util.Log.d("LanguageManager", "setLanguage: $language")
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
        _currentLanguage.value = language
        applyLanguage(language)
    }
    
    /**
     * Aplica el idioma a la configuración del sistema
     */
    fun applyLanguage(language: String) {
        android.util.Log.d("LanguageManager", "applyLanguage: $language")
        val locale = when (language) {
            LANGUAGE_SPANISH -> Locale("es")
            LANGUAGE_ENGLISH -> Locale("en")
            LANGUAGE_AUTO -> getSystemLocale()
            else -> getSystemLocale()
        }
        
        android.util.Log.d("LanguageManager", "Locale: $locale")
        Locale.setDefault(locale)
        
        // Para Android 7.0+ (API 24+), usar createConfigurationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            android.util.Log.d("LanguageManager", "Android 7.0+: Config setLocale to $locale")
            // No actualizar la configuración directamente, solo establecer el locale
        } else {
            @Suppress("DEPRECATION")
            val config = Configuration(context.resources.configuration)
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            android.util.Log.d("LanguageManager", "Android <7.0: Updated configuration")
        }
        
        // Notificar cambio de idioma para recomposición de UI
        _currentLanguage.value = language
        android.util.Log.d("LanguageManager", "Language applied successfully")
    }
    
    /**
     * Obtiene el idioma del sistema
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * Obtiene el nombre del idioma para mostrar en la UI
     */
    fun getLanguageDisplayName(language: String): String {
        return when (language) {
            LANGUAGE_SPANISH -> "Español"
            LANGUAGE_ENGLISH -> "English"
            LANGUAGE_AUTO -> "Automático"
            else -> "Automático"
        }
    }
    
    /**
     * Obtiene la lista de idiomas disponibles
     */
    fun getAvailableLanguages(): List<String> {
        return listOf(LANGUAGE_AUTO, LANGUAGE_SPANISH, LANGUAGE_ENGLISH)
    }
    
    /**
     * Inicializa el idioma al iniciar la aplicación
     */
    fun initializeLanguage() {
        val currentLanguage = getCurrentLanguage()
        applyLanguage(currentLanguage)
    }
}
