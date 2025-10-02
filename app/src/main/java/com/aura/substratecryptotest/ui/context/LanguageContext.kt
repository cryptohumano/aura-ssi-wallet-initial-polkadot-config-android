package com.aura.substratecryptotest.ui.context

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aura.substratecryptotest.utils.LanguageManager

/**
 * Context de idioma para toda la aplicación
 * Permite que la UI se recomponga cuando cambie el idioma
 */
@Composable
fun rememberLanguageManager(): LanguageManager {
    val context = LocalContext.current
    return remember { LanguageManager.getInstance(context) }
}

/**
 * Hook para observar cambios de idioma y forzar recomposición
 */
@Composable
fun rememberLanguageState(): State<String> {
    val languageManager = rememberLanguageManager()
    val currentLanguage by languageManager.currentLanguage.collectAsState()
    
    // Forzar recomposición cuando cambie el idioma
    DisposableEffect(currentLanguage) {
        onDispose { }
    }
    
    return rememberUpdatedState(currentLanguage)
}

/**
 * Composable que se recompone cuando cambia el idioma
 */
@Composable
fun LanguageAware(
    content: @Composable () -> Unit
) {
    val languageManager = rememberLanguageManager()
    val currentLanguage by languageManager.currentLanguage.collectAsState()
    
    // Forzar recomposición cuando cambie el idioma
    LaunchedEffect(currentLanguage) {
        // Aplicar el idioma cuando cambie
        languageManager.applyLanguage(currentLanguage)
    }
    
    key(currentLanguage) {
        content()
    }
}
